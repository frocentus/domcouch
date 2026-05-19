# DomCouch Code Review Report

## Executive Summary

DomCouch has a solid overall structure for a Domino-style API on top of Couchbase, with clear separation between interfaces and implementations. The most serious issues are: checked exceptions are being swallowed or converted into runtime failures in the database open path, several N1QL statements are built through string concatenation, and lazy document item loading is not thread-safe. These problems affect API compatibility first, then correctness and security.

## Risk Summary

- **Compatibility risk: HIGH** — `Session.getDatabase()` can wrap `NotesException` in `RuntimeException`, and `getView()` can return a shell view with no persisted definition.
- **Performance risk: MEDIUM** — `isValid()` uses a cluster-wide admin call, and document/view collection methods can trigger large full scans and extra allocations.
- **Security risk: HIGH** — unparameterized N1QL construction exists in response lookup and folder/view DDL paths.

## Compatibility Audit

### Meets current expectations

- Public API surfaces generally mirror the Domino-style interfaces.
- `NotesException` error code usage is mostly consistent with the documented ranges.
- Reader/Author document security logic is implemented and enforced in save/remove paths.
- `getUniversalID()` returns an uppercase 32-character hex value.
- `getParentDocumentUNID()` returns `""` for non-response documents.

### Violations

#### 1) `Session.getDatabase()` swallows checked failures

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseSession.java`

**Problem:** `getDatabase(String)` and `getDatabase(String, String)` use `computeIfAbsent(...)` and wrap failures in `RuntimeException`. That breaks the checked-exception contract implied by `throws NotesException`.

**Fix:** Avoid `computeIfAbsent` for exception-bearing logic. Resolve the database explicitly and rethrow as `NotesException`.

**Regression test:** Force a bucket/scope failure and assert callers receive `NotesException`, not `RuntimeException`.

#### 2) `getDocumentByUNID()` returns `null` for all errors

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java`

**Problem:** The method returns `null` on any exception, making infrastructure/auth/deserialization failures indistinguishable from "not found".

**Fix:** Return `null` only for not-found cases; wrap other failures in `NotesException`.

**Regression test:** Simulate a Couchbase outage or malformed stored JSON and assert an exception is thrown.

#### 3) View definitions are not persisted

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java` / `CouchbaseView.java`

**Problem:** `getView(name)` returns a new `CouchbaseView` with no selection formula if the view was not created in the current JVM session. That silently changes behavior across restarts.

**Fix:** Persist view metadata and reconstruct the view on demand.

**Regression test:** Create a view, restart the session, reopen the view, and verify it still filters correctly.

#### 4) `remove()` leaves the document marked dirty

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDocument.java`

**Problem:** `remove()` sets `dirty = true` after deletion. That is semantically incorrect and can confuse callers.

**Fix:** Track deletion separately or clear dirty state after delete.

**Regression test:** Delete a document and assert it is no longer considered dirty.

#### 5) Missing interface overload for `createView(name, formula, keyItemName)`

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/api/Database.java`

**Problem:** The implementation supports a three-argument string overload, but the public interface does not declare it. That is a compatibility gap for callers typed to `Database`.

**Fix:** Add the overload to the interface or remove the implementation overload if it is not intended to be public.

**Regression test:** Compile a caller against `Database` that uses the overload.

## Performance Review

### 1) `isValid()` is too expensive

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseSession.java`

The current implementation calls `cluster.buckets().getAllBuckets()`, which is an admin-style cluster-wide query. Health checks can become expensive and noisy.

**Recommendation:** Use a lightweight ping/diagnostic call instead.

### 2) Document retrieval uses multi-step query paths

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java`

`fetchDocumentsByN1qlIds(...)` first collects IDs, then issues batched `USE KEYS` lookups. This adds memory churn and extra query round-trips for large result sets.

**Recommendation:** Prefer a single query path when possible, and add pagination/limits for large result sets.

### 3) Item loading creates unnecessary contention risk

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDocument.java`

Lazy item loading is unsynchronized and can repeat work under concurrent access.

**Recommendation:** Protect lazy initialization with a synchronized or volatile double-check pattern.

## Concurrency and Robustness

### 1) Lazy item loading is not thread-safe

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDocument.java`

**Problem:** `ensureItemsLoaded()` checks and mutates `itemsLoaded` and `rawDoc` without synchronization.

**Fix:** Use `volatile` + synchronized double-check or load eagerly on construction.

**Test:** Access the same document from multiple threads and verify items are not duplicated or corrupted.

### 2) Mutable shared state in `FormulaTranslator`

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java`

**Problem:** The database stores a single mutable `formulaTranslator` whose current user can be changed globally. In a multi-user server, one request can affect another.

**Fix:** Make user context request-scoped or pass it explicitly to formula evaluation.

**Test:** Run two concurrent evaluations with different users and assert each sees its own identity.

## Security Review

### 1) N1QL injection in response lookup

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java`

**Problem:** `findByParentUNID(...)` concatenates `parentUnid` directly into the query string.

**Fix:** Use a named N1QL parameter.

**Test:** Supply a malicious value containing quotes or boolean operators and confirm it cannot alter the result set.

### 2) Folder name is embedded into N1QL text

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java`

**Problem:** `createFolder(...)` and `getFolder(...)` build a formula string with manual quote escaping.

**Fix:** Validate folder names strictly, or use parameterized query construction.

**Test:** Try folder names containing quotes/backticks and assert rejection.

### 3) Index DDL is built from caller input

**File:** `domino-couchbase-lib/src/main/java/com/domcouch/impl/CouchbaseDatabase.java`

**Problem:** `createViewIndex(...)` embeds `keyItemName` into DDL text.

**Fix:** Restrict allowed characters to a safe identifier set.

**Test:** Reject unsafe key item names.

## Tests and CI Gaps

Missing high-value coverage:

- API contract tests for checked-exception behavior.
- Restart/persistence tests for views and folders.
- Concurrency stress tests for lazy item loading.
- Injection regression tests for all string-built N1QL.
- Benchmark tests for document retrieval and formula evaluation.

Recommended CI checks:

1. Binary/API compatibility check with a tool such as japicmp.
2. SpotBugs + FindSecBugs for injection and concurrency issues.
3. Dependency vulnerability scanning and secret scanning.

## Prioritized Remediation Plan

1. **Fix checked-exception handling in database open paths** — low effort, high compatibility impact.
2. **Parameterize all N1QL strings built from external values** — low effort, high security impact.
3. **Make lazy document loading thread-safe** — low effort, medium/high robustness impact.
4. **Persist view definitions across sessions** — medium effort, high compatibility impact.
5. **Replace expensive health checks with lightweight diagnostics** — low effort, medium performance impact.

## Recommended Acceptance Criteria

- Public API callers can continue to catch `NotesException` without runtime surprises.
- No user-controlled values are concatenated into N1QL or DDL strings.
- Concurrent access to a shared `Document` does not duplicate or corrupt item state.
- Views continue to behave the same after session restart.
- Health checks complete quickly and do not require cluster-wide enumeration.
