# DomCouch — Architecture Review vs. Target Vision

> **Date**: 2026-05-17  
> **Version**: domcouch 0.2.0-SNAPSHOT  
> **Purpose**: Compare current implementation against the ideal Domino 14.5 API facade architecture.

---

## 1. Interface Compatibility — ⚠️ Partial

| Requirement | Status | Gap |
|---|---|---|
| Identical `lotus.domino.*` package names | ❌ | Uses `com.domcouch.api.*` — existing Domino code needs import changes |
| Identical method signatures | ✅ | `getFirstItem()`, `replaceItemValue()`, `save()`, etc. match Domino |
| Identical exception handling | ✅ | `NotesException` with numeric error codes |
| Unmappable features stubbed gracefully | 🟡 | `@Command`/`@PostedCommand` no-ops, but `RichTextItem` missing entirely |
| Formula language compatibility | ✅ | Full lexer→parser→evaluator pipeline, 150+ @Functions, N1QL translation |

**Verdict**: The API surface is correct but the package name prevents drop-in replacement. A thin wrapper layer re-exporting under `lotus.domino.*` would close this gap.

---

## 2. NoSQL Schema Design — ⚠️ Nested, Not Flat

### Current Schema (Couchbase JSON)
```json
{
  "_type": "domcouch.document",
  "unid": "...",
  "form": "KanbanBoard",
  "items": {
    "TITLE": [{"type": 0, "values": ["My Board"]}],
    "PRIORITY": [{"type": 1, "values": [3.0]}],
    "READERS": [{"type": 4, "values": ["Alice", "Bob"]}]
  },
  "folders": ["Inbox"],
  "parentUNID": "...",
  "_attachments": {...}
}
```

### Issues — and why the current schema is actually correct

| Concern | Analysis |
|---|---|
| **Deep nesting**: `items.NAME[0].values[0]` triggers Couchbase SDK `ClassCastException` | **Root cause is the SDK, not the schema.** Fixed via `get(name)+instanceof` and `RawJsonTranscoder`. Schema nesting is fine. |
| **Type-per-item overhead**: Each field stores `{type, values}` | ~30% storage overhead, but enables seamless multi-value→multi-instance upgrade. Worth it. |
| **Multi-instance items as arrays**: N1QL queries are verbose | N1QL `items.BODY[0].values[0]` is consistent — all fields use the same path. |

### Why NOT flatten

Consider `Tags` as multi-value: `["java", "couchbase"]`. If it later needs per-instance metadata
(split into two `Tag` items with different formatting), the `{type, values}` schema handles this
transparently — just add a second array element. Callers see the same `getValues()` result.

A flat `"fields": {"Tags": ["java","couchbase"]}` has **no upgrade path** to multi-instance —
you'd need to migrate from `fields.Tags` to `_multi.Tags`, breaking all N1QL queries and
API accessors. The current schema avoids this class of migration entirely.

### Recommendation (revised)

**Keep the current `{type, values}` array schema.** It correctly models Domino's flexible
field semantics where any single-value field can become multi-value, and any multi-value field
can become multi-instance — all without schema migration. The SDK access issues are solved
once (see ClassCastException section below), not repeatedly per field type.

---

## 3. N+1 Query Problem — 🟡 Partial

### Current State

| Operation | Pattern | N+1? |
|---|---|---|
| `getDocumentByUNID()` | RawJsonTranscoder → single KV read | No |
| `getAllDocuments()` | N1QL `SELECT meta().id` → KV fetch per doc | **YES — N+1** |
| `search()` | Same as getAllDocuments | **YES — N+1** |
| `getResponses()`/`findByParentUNID()` | Same pattern | **YES — N+1** |
| Lazy navigator `getNext()` | Key-based pagination, page size 200 | No |
| In-memory navigator `getNext()` | Full scan once, then O(1) | No (after build) |

### The N+1 paths

```java
// For 50K documents: 1 N1QL query + 50,000 KV reads = 50,001 operations
for (JsonObject row : scope.query("SELECT meta().id FROM ...").rowsAsObject()) {
    String unid = row.getString("_id");
    Document doc = getDocumentByUNID(unid);  // KV read per document
    docs.add(doc);
}
```

**Impact**: `getAllDocuments()` on 50K docs takes ~30-60 seconds due to 50K sequential KV reads.

### Recommendation
Use N1QL `SELECT doc.*` directly (if SDK supports it) or batch KV reads:
```java
// Batch KV reads — 50K docs → 500 batches of 100
List<String> ids = n1qlQuery("SELECT meta().id FROM ...");
for (int i = 0; i < ids.size(); i += 100) {
    List<GetResult> batch = collection.getAll(ids.subList(i, min(i+100, ids.size())));
    for (GetResult r : batch) {
        docs.add(parseDocument(r));
    }
}
```
Or use the Couchbase reactive SDK with `flatMap` for concurrent KV fetches.

---

## 4. Heavy Object Instantiation — ❌

### Current: Full document load on every access

```java
// CouchbaseDocument constructor loads ALL items immediately:
public CouchbaseDocument(CouchbaseDatabase database, JsonObject doc) {
    loadFromJson(doc);  // iterates all items, creates CouchbaseItem objects
}
```

Even when the caller only needs `doc.getUniversalID()` or `doc.getFirstItem("Title")`, every item is deserialized. For 50K documents, this means 50K × ~20 items = 1M `CouchbaseItem` objects in memory.

### Recommendation
Lazy item loading:
```java
public Item getFirstItem(String name) {
    if (items == null) loadItems();  // lazy: only load when items are accessed
    return items.get(name.toUpperCase()).get(0);
}
```

---

## 5. Batching Logic — 🟡 Good Pattern, Missing API

### Domino's Built-in Caching

Domino ViewNavigator supports:
- `setCacheSize(int n)` — pre-fetches `n` entries into a local buffer
- `setAutoUpdate(false)` — disables automatic refresh, enables caching
- When cache is exhausted, next batch is pre-fetched automatically

This is the batch-fetch anti-N+1 pattern that the architecture review requires.

### Our Implementation

**`CouchbaseLazyViewNavigator` already implements this pattern** — the `pageSize` parameter IS the
cache size. Each page fetch uses key-based pagination (`WHERE keyCol > $cursor LIMIT n`).
Sequential `getNext()` is μs-fast within the page, and the next page is fetched in one N1QL
query when exhausted. No N+1.

```java
// Lazy navigator — pageSize behaves like Domino's setCacheSize():
var nav = ((CouchbaseView) view).createLazyViewNav(/*maxLevel*/ 0, /*pageSize*/ 200);
```

### Gap: API surface

| Method | Domino has | We have | Maps to |
|---|---|---|---|
| `setCacheSize(int)` | ✅ | ❌ | Lazy nav `pageSize` — number of entries pre-fetched into buffer |
| `getCacheSize()` | ✅ | ❌ | |
| `setBufferMaxEntries(int)` | ✅ | ❌ | LIMIT clause in N1QL — max rows per server request |
| `getBufferMaxEntries()` | ✅ | ❌ | |
| `setAutoUpdate(boolean)` | ✅ | ❌ | Controls whether navigator refreshes on each access |
| `isAutoUpdate()` | ✅ | ❌ | |

In Domino, `setCacheSize` and `setBufferMaxEntries` work together:
- `setBufferMaxEntries(50)` — "don't fetch more than 50 entries per network round-trip"
- `setCacheSize(200)` — "keep up to 200 entries in local memory"

This maps cleanly to our lazy navigator: `pageSize` = buffer max, with potential for a
larger in-memory ring buffer beyond the current page.
Implementation is trivial: `setCacheSize()` changes pageSize and re-fetches the current page.

---

## 6. Memory & Recyclability — 🟡

| Requirement | Status |
|---|---|
| `recycle()` clears caches | ✅ Views cleared, navigator index cleared, KV connections released |
| No lingering caches | ✅ `formulaCache` cleared on `recycle()`, `views` map intentionally long-lived (navigators are transient, views persist) |
| JVM GC handles objects | ✅ No manual memory management |

---

## 7. What We Got Right

| Feature | Implementation |
|---|---|
| **Formula engine** | Zero-dependency lexer→parser→evaluator, 150+ @Functions, N1QL translation for 71 functions |
| **ViewNavigator** | Full Domino API surface (27 get + 23 goto methods), categorized views with hierarchy links |
| **Lazy navigator** | Key-based pagination, 1ms build, μs-fast sequential walk |
| **TTL index lifecycle** | Hash-based index names, metadata persistence, automatic stale cleanup |
| **Folder support** | Virtual views via `'name' IN doc.folders` |
| **Reader/Author security** | Per-document access control with Domino semantics |

---

## 8. Priority Fixes (by impact)

| # | Issue | Impact | Effort |
|---|---|---|---|
| 1 | N+1 in `getAllDocuments()`/`search()` | 30-60s for 50K docs | Medium — batch KV reads |
| 2 | Lazy document loading | 1M unnecessary CouchbaseItem objects for 50K docs | Low — defer `loadFromJson` |
| 3 | `lotus.domino.*` package | True drop-in compatibility | Low — wrapper classes |
| 4 | Add `setCacheSize`/`setAutoUpdate` to ViewNavigator | Matches Domino API surface | Low — expose pageSize as settable |
| 5 | ClassCastException in SDK accessors | **✅ Fixed** — `get(name)+instanceof`, `RawJsonTranscoder` | Done |
| 6 | Schema migration to flat fields | **❌ Rejected** — `{type, values}` schema is correct | N/A |
