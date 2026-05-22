# AGENTS.md — DomCouch Project Decisions & Best Practices

> **Project**: HCL Domino 14.5 Java API emulation on Couchbase  
> **Version**: 0.3.0-SNAPSHOT  
> **Last updated**: 2026-05-19

---

## 1. Project Mission

**"Write once against the Domino API, run forever on Couchbase."**

Provide a Java library (`domino-couchbase-lib`) that mirrors the `lotus.domino`
data-layer API (`Session` → `Database` → `Document` → `View` → `Item`) backed by
Couchbase 7.x. The goal is API-level compatibility: existing Domino Java code can
switch the backend with minimal changes.

---

## 2. Architecture Decisions

### 2.1 Couchbase Mapping

| Domino Concept  | Couchbase Mapping                                                      |
| --------------- | ---------------------------------------------------------------------- |
| Session         | Couchbase `Cluster` connection                                         |
| Database (.nsf) | Couchbase **bucket** (single-arg) or **scope within bucket** (two-arg) |
| Document        | JSON document in the `documents` collection                            |
| Universal ID    | 32-char hex UUID (field: `unid`)                                       |
| Items           | Stored in a nested `items` object as JSON arrays per name              |
| View            | N1QL-backed query with optional index                                  |
| Attachments     | Not yet implemented                                                    |

**Two database-opening patterns:**

| Call                                          | Bucket     | Scope      | Use case                          |
| --------------------------------------------- | ---------- | ---------- | --------------------------------- |
| `session.getDatabase("contacts")`             | `contacts` | `data`     | Default — full isolation per DB   |
| `session.getDatabase("domcouch", "contacts")` | `domcouch` | `contacts` | Legacy — shared bucket, per scope |

The single-arg form auto-creates buckets on first use (100 MB RAM quota by default,
configurable via `Session.createSession(conn, user, pass, ramQuotaMB)`).
The two-arg form requires a pre-existing bucket.

### 2.2 Document JSON Schema

```json
{
  "_type": "domcouch.document",
  "unid": "A1B2C3D4...",
  "form": "Person",
  "items": {
    "FirstName": [{ "type": 0, "values": ["Alice"] }],
    "Salary": [{ "type": 1, "values": [95000] }],
    "Readers": [{ "type": 4, "values": ["Alice", "Bob"] }],
    "Authors": [{ "type": 3, "values": ["Alice"] }],
    "Body": [
      { "type": 5, "values": ["First paragraph"] },
      { "type": 5, "values": ["Second paragraph"] }
    ]
  },
  "_attachments": {
    "report.pdf": {
      "type": "application/pdf",
      "size": 12345,
      "parentUnid": "A1B2C3D4..."
    }
  },
  "created": "2026-01-01T00:00:00Z",
  "lastModified": "2026-01-01T00:00:00Z",
  "parentUNID": "...",
  "folders": ["Inbox"]
}
```

Each item name maps to a JSON **array** of item objects, allowing multiple
items with the same name (e.g. multiple `Body` items — Domino multi-instance
items). Single-value names produce a single-element array.

N1QL field references use `doc.items.NAME[0].values[0]` to access the first
item's first value.

### 2.3 Item Types

| Constant    | Value | Meaning                             |
| ----------- | ----- | ----------------------------------- |
| `TEXT`      | 0     | Plain text / string values          |
| `NUMBERS`   | 1     | Numeric values                      |
| `DATETIMES` | 2     | Date/time values                    |
| `AUTHORS`   | 3     | Domino Authors field (edit control) |
| `READERS`   | 4     | Domino Readers field (read control) |
| `RICHTEXT`  | 5     | Rich text / MIME (not yet impl.)    |

### 2.4 Formula Engine

The formula engine is a complete Lexer → Parser → Evaluator pipeline
in `com.domcouch.formula`. It supports two modes:

| Mode                    | Entry point                    | Use case                          |
| ----------------------- | ------------------------------ | --------------------------------- |
| **Query translation**   | `FormulaTranslator.toN1ql()`   | Selection formulas → N1QL WHERE   |
| **Computed evaluation** | `FormulaTranslator.evaluate()` | Computed fields against documents |

**Key classes**:

| Class                    | Role                                                    |
| ------------------------ | ------------------------------------------------------- |
| `Lexer`                  | Tokenizes formula strings (54 test cases)               |
| `Parser`                 | Pratt-style recursive descent → AST (39 test cases)     |
| `Evaluator`              | Tree-walks AST against `FormulaContext` (57 test cases) |
| `CompiledFormula`        | Pre-parsed AST — evaluate without re-parsing            |
| `DocumentFormulaContext` | Bridges formula engine with `Document` API              |

**Compiled caching**: Formulas can be compiled once and evaluated against
many documents, achieving ~16× speedup by skipping Lexer+Parser stages.
See `CouchbaseDatabase.compileFormula()` / `evaluateCached()`.

**@Functions supported** (Phase 1): 35 functions — all verified against
official Domino specifications. Includes string ops, type conversion,
type checking, list ops, control flow, date extraction, boolean constants,
and side-effects. `@Command` / `@PostedCommand` are no-ops.

Phase 2 (16 functions remaining): `@Explode`, `@Implode`, `@Prompt`,
`@DbLookup`, `@DbColumn`, `@MailSend`, `@Adjust`, `@SetDocField`, and others.

Full language specification: `docs/formula-language-architecture.md`.

---

## 3. Document-Level Security (Readers & Authors)

### 3.1 Domino Semantics We Emulate

#### Reader Fields (type 4)

- **No reader fields** → document is **public** (readable by everyone).
- **One or more reader fields** → only users/roles listed in **at least one**
  reader field can read the document. Users not listed cannot see the document
  at all.

#### Author Fields (type 3)

- **No author fields** → document is **editable by everyone**.
- **One or more author fields** → only users/roles listed in **at least one**
  author field can save/delete the document.
- `save()` and `remove()` throw `NotesException(4010)` when the current user
  is not an author.

### 3.2 Implementation

Reader enforcement is centralized in `CouchbaseDatabase.canRead(JsonObject, String)` — a
single static method that both `CouchbaseDocument.isReadableBy()` and
`CouchbaseView.isReadableRow()` delegate to, preventing logic divergence.

| Layer               | Mechanism                                                         |
| ------------------- | ----------------------------------------------------------------- |
| `CouchbaseDatabase` | `canRead(json, user)` — static, works on raw JSON                 |
| `CouchbaseDocument` | `isReadableBy(user)` / `isEditableBy(user)` — on hydrated items   |
| `CouchbaseView`     | `isReadableRow(row)` → delegates to `CouchbaseDatabase.canRead()` |
| `getDocumentByUNID` | Checks `canRead()` **before** deserializing the document          |
| Enforcement point   | **Application-side** (post-query filtering in Java)               |

### 3.3 N1QL Injection Defenses

- **`FTSearch`**: Uses **parameterized queries** (`$q` + `$limit` via `QueryOptions`).
  No user input is concatenated into N1QL strings.
- **`search()` / formula translator**: String literals from formulas are embedded
  directly. This is **safe** because selection formulas are developer-controlled
  (not end-user input). A `SECURITY NOTE` comment in `translateFormula()` documents
  this assumption. If `search()` ever accepts untrusted input, the translator must
  be refactored to use parameterized literals.

Couchbase/CouchDB have no native per-document reader-field enforcement at the
database engine level. Couchbase N1QL can do `IN` checks on arrays, but the
reader field name is arbitrary (could be named `Readers`, `docReaders`, etc.)
and we identify it by item **type** (4), not name. Post-query filtering is:

- **Correct**: Covers all document-returning methods uniformly.
- **Simple**: One method (`isReadableBy` / `isReadableRow`) handles all cases.
- **Traceable**: Easy to debug and audit.

Future optimization: push reader checks to N1QL for large datasets via a
convention on field naming (`"Readers"`) combined with `IN` array membership.

### 3.4 Known Limitations

1. **Counts are not reader-filtered**: `getDocumentCount()` and
   `getEntryCount()` count **all** documents including those the current user
   cannot read. This reveals document existence. Domino hides these documents
   completely. Requires N1QL-level filtering.

2. **Formula translator trusts input**: `search()` and `translateFormula()`
   embed string literals directly into N1QL. Selection formulas must be
   developer-controlled. User-supplied search must go through `FTSearch`
   (which is parameterized).

---

## 4. API Compatibility Stance

### 4.1 What We Target

- **Core data-layer interfaces** (`Session`, `Database`, `Document`, `View`,
  `ViewEntry`, `ViewEntryCollection`, `Item`, `DateTime`)
- Method signatures match `lotus.domino.*` where possible
- Exception model: `NotesException` with Domino-style error codes

### 4.2 What We Skip (by design)

- **UI/client classes**: `RichTextItem`, `RichTextStyle`, `NotesUIWorkspace`
- **Admin/replication**: `Registration`, `Replication`, `ACL` management
- **Server-side agents**: `Agent`, `AgentContext`
- **DXL**: `DxlExporter`, `DxlImporter`
- **Crypto**: `encrypt()`, `decrypt()`, `sign()`

### 4.3 When to Extend the API

Add new methods to the public interfaces (`com.domcouch.api.*`) **only** when:

1. They are essential for functionality not expressible through the Domino API
2. They fill a gap Couchbase requires (e.g., `getCurrentUserName()`)
3. Prefer adding domcouch-specific methods to the **implementation classes**
   rather than the API interfaces when they're not needed by callers

---

## 5. Code Conventions

### 5.0 Tool Usage

- **String comparison / diffing**: Use `ctx_execute` with Python or JavaScript,
  not manual eyeballing. Example:
  ```python
  a, b = "DOCOMMITTEDLENGTH", "DOCCCOMMITTEDLENGTH"
  diffs = [(i, ca, cb) for i, (ca, cb) in enumerate(zip(a, b)) if ca != cb]
  print(diffs)
  ```
- **String lengths / positions**: Use a script, never count characters manually.
  Example:
  ```python
  s = "North Carolina"
  print(f"len={len(s)}")
  for i, ch in enumerate(s): print(f"  [{i}] = '{ch}'")
  sub = "th"; idx = s.index(sub); print(f"'{sub}' at [{idx}:{idx+len(sub)}]")
  ```
  Manual counting like `"N(1)o(2)r(3)..."` is error-prone and wastes time.
  Script catches typos humans miss. Apply whenever comparing identifiers,
  counting string positions, extracting substrings, or analyzing any text
  longer than ~10 characters.

### 5.1 Project Layout

```
domcouch/
├── AGENTS.md
├── README.md
├── pom.xml                            (parent Maven POM, Spring Boot 3.4.3)
├── docker-compose.yml                 (Couchbase 7.x)
├── docs/
│   ├── api-coverage.md
│   ├── formula-language-architecture.md
│   ├── function-catalog.md
│   ├── notes_formula_documentation.md
│   ├── couchbase8-knowledge.md
│   └── skills/couchbase8/SKILL.md
├── formula-engine/                    (Standalone module — 0 external deps)
│   ├── pom.xml
│   └── src/main/java/com/domcouch/formula/
│       ├── Token.java, TokenType.java
│       ├── Lexer.java
│       ├── Expr.java
│       ├── Parser.java
│       ├── FormulaContext.java
│       ├── Evaluator.java
│       ├── CompiledFormula.java
│       ├── FormulaTranslator.java
│       ├── FormulaParseException.java
│       └── FunctionHandler.java
├── domino-couchbase-lib/              (Depends on formula-engine + Couchbase SDK)
│   ├── pom.xml
│   └── src/main/java/com/domcouch/
│       ├── api/                       (Interfaces — the Domino contract)
│       │   └── ...
│       └── impl/                      (Couchbase-backed implementations)
│           ├── CouchbaseSession.java
│           ├── CouchbaseDatabase.java
│           ├── CouchbaseDocument.java
│           ├── CouchbaseItem.java
│           ├── CouchbaseView.java
│           ├── DocumentFormulaContext.java
│           └── ...
└── springboot-demo/                   (Demo REST app + data generator)
    └── src/main/java/com/domcouch/demo/
        ├── config/                    (Spring beans: Session, Database)
        ├── controller/                (REST endpoints)
        ├── model/                     (Person POJO)
        └── service/                   (Business logic + data gen)
```

### 5.2 Naming

- **API interfaces**: use `lotus.domino` method names exactly (e.g., `getDocumentByUNID`, `replaceItemValue`)
- **Implementation classes**: prefix with `Couchbase` (e.g., `CouchbaseDocument`)
- **Item field names**: PascalCase matching Domino conventions (`FirstName`, `LastName`)
- **UNID format**: 32-char uppercase hex, no dashes

### 5.3 Error Handling

- Use `NotesException` with numeric error codes
- Error code ranges:
  - `4000-4099`: Document operation failures
  - `4000`: save failed
  - `4001`: remove failed
  - `4002`: FTSearch failed
  - `4003`: search failed
  - `4010`: no author access (security)
  - `4500`: Feature not applicable in Couchbase
  - `4501`: Formula parse error — unclosed string
  - `4502`: Formula parse error — unexpected character
  - `4503`: Formula parse error — @ without function name
- Use `FormulaParseException` (extends `RuntimeException`) for parse errors
- Wrap Couchbase SDK exceptions; don't leak them through the API

### 5.4 Thread Safety

- `ConcurrentHashMap` for caches (databases map, view map, items map)
- `CouchbaseSession.databases` cache is concurrent
- Document items map is concurrent (allows concurrent reads during save)

---

## 6. Testing & Demo

### 6.1 Running Locally

```bash
# Start Couchbase
docker compose up -d

# Build
mvn clean package -DskipTests

# Run demo
mvn -pl springboot-demo spring-boot:run
```

### 6.2 Key Endpoints

| Method | Path                               | Purpose            |
| ------ | ---------------------------------- | ------------------ |
| GET    | `/api/persons/info`                | DB stats           |
| GET    | `/api/persons/{unid}`              | Single person      |
| GET    | `/api/persons/view/{name}?key=...` | View lookup        |
| GET    | `/api/persons/search?q=...`        | Full-text search   |
| POST   | `/api/persons/admin/reinitialize`  | Reset + repopulate |

### 6.3 Test Suite

```bash
mvn test -pl domino-couchbase-lib
```

| Test class                  | Tests   | Coverage                                                                                                              |
| --------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------- |
| `LexerTest`                 | 63      | All token types, escapes, numbers, brackets, comments                                                                 |
| `ParserTest`                | 43      | Precedence, operators, FIELD/DEFAULT/ENVIRONMENT, @Functions                                                          |
| `EvaluatorTest`             | 63      | Arithmetic, comparison, coercion, @Functions, assignment                                                              |
| `FormulaExamplesTest`       | 97      | Real Domino spec examples — all formula categories                                                                    |
| `CachedEvaluationTest`      | 8       | Compile-once, evaluate-many; verified against Java values                                                             |
| `PerformanceComparisonTest` | 9       | Throughput, cached vs uncached, pipeline breakdown                                                                    |
| `FormulaTranslatorTest`     | 27      | N1QL translation correctness via AST-based walker                                                                     |
| `StringFunctionsTest`       | 114     | @Contains @Matches @Repeat @ReplaceSubstring @Word @Trim @Case @Length @Left @Right @ProperCase @Explode @Ascii @Char |
| `MathFunctionsTest`         | 33      | @Pi @Power @Sqrt @Exp @Log @Cos @Sin @Tan @Abs @Ln @FloatEq @Max @Min @Sum @Modulo @Sign @ATan @ATan2 @ASin @ACos     |
| `DateTimeFunctionsTest`     | 33      | @Month @Day @Year @Date @Adjust @TimeMerge @Tomorrow @Yesterday @BusinessDays @Today @Now + doc timestamps            |
| `ListFunctionsTest`         | 24      | @IsMember @Replace @Count @Compare @Subset @Unique @Member @Implode @Sort @Transform                                  |
| `ControlFlowTest`           | 18      | @While @For @DoWhile @Set @SetField @Eval @Error @IsError @CheckFormulaSyntax                                         |
| `DocumentFunctionsTest`     | 15      | @DocFields @DocLength @DocLock @DocumentUniqueID lifecycle folders @DeleteField                                       |
| `OperatorsTest`             | 14      | Pair-wise and permuted list operations                                                                                |
| `PatternMatchingTest`       | 27      | @Matches (24) + @Like (6) — all pattern operators                                                                     |
| `DataConversionTest`        | 24      | @Text @TextToNumber @IsNumber @IsTime @TextToTime @ToNumber @ToTime                                                   |
| `ValidationTest`            | 25      | @Success @Failure @IsNull @IsValid @IfError + placeholders + constants                                                |
| **Total**                   | **654** |                                                                                                                       |

### 6.4 Data Generation

On first startup, `DatabaseInitializer` generates 10,000 fake persons with 20
attributes each. Views are created automatically. Re-initialization clears all
documents and regenerates.

---

## 7. Decision Log

| Date       | Decision                                                      | Rationale                                                                                                                                                                                                                                                   |
| ---------- | ------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-05-11 | Reader/Author filtering: application-side (post-query)        | Simpler, more correct; field identified by type not name                                                                                                                                                                                                    |
| 2026-05-11 | `getDocumentCount()` not reader-filtered (known limitation)   | Requires N1QL-level filtering; deferred                                                                                                                                                                                                                     |
| 2026-05-11 | Author enforcement: strict check on `save()` and `remove()`   | Matches Domino behavior; throws `NotesException(4010)`                                                                                                                                                                                                      |
| 2026-05-11 | New doc creation always allowed (author check on save)        | Self-lockout prevention; user must include self                                                                                                                                                                                                             |
| 2026-05-11 | `Item.setReaders()` / `Item.setAuthors()` added to API        | Matches `lotus.domino.Item` methods                                                                                                                                                                                                                         |
| 2026-05-11 | `canRead()` centralized as static method on CouchbaseDatabase | Prevented reader-check logic drift between doc & view                                                                                                                                                                                                       |
| 2026-05-11 | `FTSearch` uses parameterized N1QL queries (`$q`)             | Eliminates N1QL injection; user input never concatenated                                                                                                                                                                                                    |
| 2026-05-11 | `getCollectionPath()` extracted; shared by DB + View          | Single source of truth for backtick-escaped path                                                                                                                                                                                                            |
| 2026-05-11 | Reader check in `getDocumentByUNID` BEFORE deserialization    | Avoids full doc construction cost if user can't read                                                                                                                                                                                                        |
| 2026-05-12 | Formula engine: Lexer → Parser → Evaluator pipeline           | Full Domino formula language support; 654 tests                                                                                                                                                                                                             |
| 2026-05-12 | Compiled formula caching with `compileFormula()`              | 16× speedup for batch document processing                                                                                                                                                                                                                   |
| 2026-05-12 | `DocumentFormulaContext` bridges formula engine with Document | Computed fields evaluated directly against domcouch Documents                                                                                                                                                                                               |
| 2026-05-12 | `@Command` / `@PostedCommand` treated as no-ops               | Matches Domino `NoExternalApps=1`; formulas with UI commands still evaluate                                                                                                                                                                                 |
| 2026-05-13 | 150+ @Functions implemented (132 ✅ + 19 🟡)                  | 654 tests; function-catalog.md with per-function spec verification                                                                                                                                                                                          |
| 2026-05-14 | Extracted `formula-engine` as standalone Maven module         | Zero external deps; 3-module project (formula-engine → domino-couchbase-lib → springboot-demo)                                                                                                                                                              |
| 2026-05-15 | Items stored as JSON arrays `[{type, values}]` per name       | Supports Domino multi-instance items (multiple items with same name); N1QL uses `items.NAME[0].values[0]`; backward-compatible loader handles old object format                                                                                             |
| 2026-05-14 | Pair-wise + permuted list operators                           | All 12 permuted operators, list broadcasting, any-match semantics                                                                                                                                                                                           |
| 2026-05-15 | AST-based N1QL translator replaces regex                      | 71 @Function translations to Couchbase N1QL; formula-column views with computed fields                                                                                                                                                                      |
| 2026-05-15 | ViewNavigator with in-memory categorized index                | Full Domino ViewNavigator API (27 get + 23 goto methods); N1QL ORDER BY + client-side category row insertion; O(1) getNth/gotoPos via flat List<ViewEntry>; hierarchy links (parent/child/sibling)                                                          |
| 2026-05-15 | CouchbaseLazyViewNavigator — key-based pagination             | No full scan on build; WHERE keyCol > $cursorKey ORDER BY keyCol LIMIT pageSize; build 1ms vs 33s; first page visible in ~1s; sequential walk 16μs/entry                                                                                                    |
| 2026-05-15 | Folder support via `doc.folders` array                        | createFolder/getFolder/removeFolder/isFolder on Database; folders are virtual views with N1QL `'name' IN doc.folders`                                                                                                                                       |
| 2026-05-16 | ViewIndexService — pluggable index lifecycle                  | TTLViewIndexService (default): SHA-256 hash-based index names, metadata in view_index_meta collection, 1h TTL with cleanupStale(). SimpleViewIndexService: view-name-based, drop on recycle only. Index lifecycle = view lifecycle, not navigator lifecycle |
| 2026-05-16 | Indexes fixed for multi-instance array schema                 | createViewIndex bugs: (1) exists-check always true, (2) CREATE INDEX used old object format `items.X.values[0]` instead of `items.X[0].values[0]`. Demo views now have 9 idx*view*\* indexes online                                                         |
| 2026-05-17 | Performance: batch document fetching                          | Replaced N+1 KV reads in getAllDocuments/search/findByParentUNID with N1QL USE KEYS batching (100 docs/query). 500× fewer round trips. 50K docs: 30s → ~400ms                                                                                               |
| 2026-05-17 | Couchbase SDK ClassCastException — universal fix              | All `getObject()/getArray()/contentAsObject()` calls throw ClassCastException with nested JSON arrays. Fixed across 6 methods using `get(name)+instanceof` and `RawJsonTranscoder`. Documented in .skills/couchbase8/SKILL.md                               |
| 2026-05-18 | Lazy document item loading                                    | Split loadFromJson into loadMetadata (cheap) + loadItems (expensive, deferred). 70% memory reduction for bulk document operations. Thread-safe via volatile + synchronized                                                                                  |
| 2026-05-19 | Code review remediation                                       | All 9 findings resolved: N1QL injection, thread safety (volatile+synchronized, ThreadLocal), checked exceptions, view persistence, lightweight isValid(), folder name validation. 12 regression tests added                                                 |
| 2026-05-20 | `computeWithForm` — Domino-standard API                       | Added `computeWithForm(boolean, boolean)` (resolves Form from document's Form item). @DbLookup single-element list unwrap in computed fields. Explicit `computeWithForm(Form, ...)` kept as extension                                                   |
| 2026-05-22 | N1QL scan consistency: `REQUEST_PLUS` everywhere              | All `scope.query()` calls in CouchbaseView and CouchbaseDatabase now use `scanConsistency(REQUEST_PLUS)`. KV deletions become immediately visible to N1QL. Eliminates eventual-consistency drift between KV and N1QL layers                               |
| 2026-05-22 | Bulk N1QL `DELETE` for test cleanup                           | N1QL DELETE by `_type = 'domcouch.document'` is reliable for cleanup (shares index with N1QL SELECT). Per-field DELETE (`d.items.Form[0].\`values\`[0]`) failed due to GSI path resolution quirks. Use "prefer N1QL DELETE to KV remove for bulk ops" rule |

---

## 8. Known Limitations

1. **Reader fields ignored in counts**: `getDocumentCount()` and
   `getEntryCount()` return total counts including unreadable documents.

2. **Formula translator**: String literals are embedded directly in N1QL.
   Formulas must be developer-controlled (not end-user input). User search
   should use `FTSearch()` which is parameterized.

3. **No database-level ACL**: The `Database` interface lacks `getACL()`,
   `grantAccess()`, `revokeAccess()`. All access control is per-document via
   Readers/Authors items.

4. **No RichText/MIME support**: `RichTextItem` is the only major item type
   not yet implemented. Simple file attachments (document-level and item-level)
   are supported via `embedObject()` and `getAttachment()`.

5. **No replication**: Couchbase cross-data-center replication (XDCR) exists
   but no Domino-style replication objects.

6. **@DbLookup/@DbColumn limited**: String-key lookup only. No date-range or
   multi-column key support.

7. **copyAllItems() not implemented**: `removeItem()` and `copyItemToDocument()`
   are supported, but bulk copy-across-documents is not.

---

## 9. Future Directions

- [ ] Push reader filtering to N1QL for better performance on large datasets
- [ ] Implement `RichTextItem` with Couchbase binary attachments
- [ ] Add database-level ACL (`getACL()` / `grantAccess()` / `revokeAccess()`)
- [ ] Multi-locale date parsing in time-date constants
- [ ] Permuted operators (`*+`, `*=`, etc.) and full list broadcasting semantics
- [ ] `@DbLookup` / `@DbColumn` date-range and multi-column key support
- [ ] Password hashing / encryption for sensitive fields (SSN)
- [ ] Expose `setCacheSize`/`setBufferMaxEntries`/`setAutoUpdate` on ViewNavigator API
- [ ] `lotus.domino.*` package wrapper for true drop-in compatibility
- [ ] Replace `View.getEntryCount()` with reader-filtered count (currently unfiltered)
