# AGENTS.md — DomCouch Project Decisions & Best Practices

> **Project**: HCL Domino 14.5 Java API emulation on Couchbase  
> **Version**: 0.1.0-SNAPSHOT  
> **Last updated**: 2026-05-11

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
| Items           | Stored in a nested `items` object: `{name: {type, values}}`            |
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
    "FirstName": { "type": 0, "values": ["Alice"] },
    "Salary": { "type": 1, "values": [95000] },
    "Readers": { "type": 4, "values": ["Alice", "Bob"] },
    "Authors": { "type": 3, "values": ["Alice"] }
  },
  "created": "2026-01-01T00:00:00Z",
  "lastModified": "2026-01-01T00:00:00Z",
  "parentUNID": "...",
  "folders": ["Inbox"]
}
```

### 2.3 Item Types

| Constant    | Value | Meaning                             |
| ----------- | ----- | ----------------------------------- |
| `TEXT`      | 0     | Plain text / string values          |
| `NUMBERS`   | 1     | Numeric values                      |
| `DATETIMES` | 2     | Date/time values                    |
| `AUTHORS`   | 3     | Domino Authors field (edit control) |
| `READERS`   | 4     | Domino Readers field (read control) |
| `RICHTEXT`  | 5     | Rich text / MIME (not yet impl.)    |

### 2.4 Formula Translator

Domino `@Functions` in selection formulas are translated to N1QL at query time
(see `CouchbaseDatabase.translateFormula()`). The translator handles `SELECT`,
`& | !` operators, `@Contains`, `@Begins`, `@Today`, `@UserName`, `@If`, and more.

**Limitation**: Complex `@Matches` regex patterns and multi-pass `@Transform`
formulas are not supported. See `docs/api-coverage.md` for the full matrix.

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

### 5.1 Project Layout

```
domcouch/
├── AGENTS.md
├── pom.xml                            (parent Maven POM, Spring Boot 3.4.3)
├── docker-compose.yml                 (Couchbase 7.x)
├── domino-couchbase-lib/
│   └── src/main/java/com/domcouch/
│       ├── api/                       (Interfaces — the Domino contract)
│       │   ├── Session.java
│       │   ├── Database.java
│       │   ├── Document.java
│       │   ├── DocumentCollection.java
│       │   ├── Item.java
│       │   ├── View.java
│       │   ├── ViewEntry.java
│       │   ├── ViewEntryCollection.java
│       │   ├── DateTime.java
│       │   └── NotesException.java
│       └── impl/                      (Couchbase-backed implementations)
│           ├── CouchbaseSession.java
│           ├── CouchbaseDatabase.java
│           ├── CouchbaseDocument.java
│           ├── CouchbaseDocumentCollection.java
│           ├── CouchbaseItem.java
│           ├── CouchbaseView.java
│           ├── CouchbaseViewEntry.java
│           ├── CouchbaseViewEntryCollection.java
│           └── CouchbaseDateTime.java
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

### 6.3 Data Generation

On first startup, `DatabaseInitializer` generates 10,000 fake persons with 20
attributes each. Views are created automatically. Re-initialization clears all
documents and regenerates.

---

## 7. Decision Log

| Date       | Decision                                                            | Rationale                                                     |
| ---------- | ------------------------------------------------------------------- | ------------------------------------------------------------- |
| 2026-05-11 | Reader/Author filtering: application-side (post-query)              | Simpler, more correct; field identified by type not name      |
| 2026-05-11 | `getDocumentCount()` not reader-filtered (known limitation)         | Requires N1QL-level filtering; deferred                       |
| 2026-05-11 | Author enforcement: strict check on `save()` and `remove()`         | Matches Domino behavior; throws `NotesException(4010)`        |
| 2026-05-11 | New doc creation always allowed (author check on save)              | Self-lockout prevention; user must include self               |
| 2026-05-11 | `Item.setReaders()` / `Item.setAuthors()` added to API              | Matches `lotus.domino.Item` methods                           |
| 2026-05-11 | `canRead()` centralized as static method on CouchbaseDatabase       | Prevented reader-check logic drift between doc & view         |
| 2026-05-11 | `FTSearch` uses parameterized N1QL queries (`$q`)                   | Eliminates N1QL injection; user input never concatenated      |
| 2026-05-11 | `getCollectionPath()` extracted; shared by DB + View                | Single source of truth for backtick-escaped path              |
| 2026-05-11 | Reader check in `getDocumentByUNID` BEFORE deserialization          | Avoids full doc construction cost if user can't read          |
| 2026-05-11 | Formula translator trusts developer-controlled input                | `search()` formulas are NOT for end-user input; `FTSearch` is |
| 2026-05-11 | Database = bucket (per-.nsf isolation) for single-arg `getDatabase` | True isolation, testable `copyToDatabase` across buckets      |

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

4. **No attachment support**: `RichTextItem` and `EmbeddedObject` not
   implemented.

5. **No replication**: Couchbase cross-data-center replication (XDCR) exists
   but no Domino-style replication objects.

6. **Formula translator gaps**: `@Matches`, `@Explode`, `@Implode`,
   `@ReplaceSubstring` not implemented. See `docs/api-coverage.md`.

7. **View column extraction naive**: `extractColumnValues` emits all item
   values as columns; no column selection formula support.

---

## 9. Future Directions

- [ ] Push reader filtering to N1QL for better performance on large datasets
- [ ] Implement `RichTextItem` with Couchbase binary attachments
- [ ] Add database-level ACL (`getACL()` / `grantAccess()` / `revokeAccess()`)
- [ ] Complete `@UserRoles` and `@Author` formula support
- [ ] Add proper unit tests for the library
- [ ] Password hashing / encryption for sensitive fields (SSN)
