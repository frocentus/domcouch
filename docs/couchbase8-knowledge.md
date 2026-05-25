# Couchbase 8 Knowledge — domcouch Patterns & Best Practices

> **Server**: Couchbase 8.0.1 Community Edition (docker-compose)  
> **SDK**: Java SDK 3.7.4  
> **Skill file**: [`.skills/couchbase8/SKILL.md`](../.skills/couchbase8/SKILL.md) — N1QL, GSI indexes, EXPLAIN, performance benchmarks

---

## Couchbase Mapping (Domino → Couchbase)

| Domino Concept | Couchbase Mapping |
|---------------|-------------------|
| Session | `Cluster` connection |
| Database (.nsf) | Bucket (single-arg) or scope within bucket (two-arg) |
| Document | JSON doc with `_type = "domcouch.document"` |
| Universal ID | 32-char hex UUID (field: `unid`) |
| Items | Nested `items` object — JSON arrays per name |
| View | N1QL-backed query with optional GSI index |
| Folders | N1QL `'name' IN doc.folders` |
| ACL | `_type = "domcouch.acl"` document |

### Document types (all in one collection)

| `_type` | Key | Purpose |
|---------|-----|---------|
| `domcouch.document` | 32-char hex UNID | Domain data |
| `domcouch.form` | `form:{name}` | Form definitions |
| `domcouch.acl` | `acl` | Database ACL |
| `domcouch.view` | `view_def:{name}` | View definitions |

---

## N1QL vs KV Consistency

| Layer | Operation | Consistency | Speed |
|-------|-----------|-------------|-------|
| **KV** | `collection.upsert()` / `remove()` / `get()` | Instant | Fastest |
| **N1QL** | `SELECT ... WHERE ...` | **Eventual** (default) | Fast |
| **N1QL** | `SELECT ...` with `REQUEST_PLUS` | **Strong** | 50-200ms slower |

**Golden rule**: KV writes are NOT immediately visible to N1QL. Use `REQUEST_PLUS` scan consistency for correctness.

```java
import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.java.query.QueryOptions;

QueryResult result = scope.query(stmt,
    QueryOptions.queryOptions()
        .scanConsistency(QueryScanConsistency.REQUEST_PLUS));
```

**All `scope.query()` calls in domcouch use REQUEST_PLUS** (CouchbaseView, CouchbaseDatabase).

---

## N1QL DELETE vs KV remove

| Approach | Use case | Consistency |
|----------|----------|-------------|
| `collection.remove(id)` | Single document | KV, instant |
| `DELETE FROM ... WHERE _type = 'domcouch.document'` | **Bulk cleanup** | N1QL, use REQUEST_PLUS |

**Rule**: N1QL DELETE shares the GSI index with N1QL SELECT — both see the same data. Prefer N1QL DELETE for bulk cleanup.

---

## `rowsAsObject()` — One-Shot Iterable

```java
// ❌ WRONG — forEach consumes iterable, then .size() returns 0
result.rowsAsObject().forEach(row -> {});
long count = result.rowsAsObject().size(); // 0!

// ✅ RIGHT — collect in ArrayList first
var rows = new ArrayList<JsonObject>();
result.rowsAsObject().forEach(rows::add);
log("Deleted %d rows", rows.size());
```

---

## Test Cleanup Pattern

```java
// @BeforeAll — bulk N1QL DELETE by _type
String cp = "`bucket`.`scope`.`collection`";
session.getNativeCluster().query(
    "DELETE FROM " + cp + " AS d WHERE d._type = 'domcouch.document'",
    QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS)
);

// @AfterAll — per-document cleanup (inner try/catch)
for (Document doc : db.getAllDocuments()) {
    try { doc.remove(); } catch (Exception e) { /* log */ }
}
```

---

## N1QL Field Paths & Escaping

Our item schema uses arrays:
```json
{
  "items": {
    "Form": [{"type": 0, "values": ["KanbanTask"]}]
  }
}
```

N1QL references:
- `d.items.Form[0].\`values\`[0]` — first item's first value  
  ↳ **`values` is a reserved word** — MUST backtick-escape!
- `d._type = 'domcouch.document'` — type discriminator (always safe)
- `'name' IN doc.folders` — folder check
- `doc.items.NAME[0].\`values\`[0]` — general field access

**Prefer `_type` over nested paths for DELETE/COUNT** — simpler, no escaping.

---

## JSON Schema (all document types)

### domcouch.document
```json
{
  "_type": "domcouch.document",
  "unid": "A1B2C3D4...",
  "form": "Person",
  "items": {
    "FirstName": [{"type": 0, "values": ["Alice"]}],
    "Salary": [{"type": 1, "values": [95000]}],
    "Readers": [{"type": 4, "values": ["Alice", "Bob"]}],
    "Authors": [{"type": 3, "values": ["Alice"]}],
    "Body": [
      {"type": 5, "values": ["Para 1"]},
      {"type": 5, "values": ["Para 2"]}
    ]
  },
  "folders": ["Inbox"],
  "_attachments": {},
  "created": "2026-01-01T00:00:00Z",
  "lastModified": "2026-01-01T00:00:00Z",
  "parentUNID": "..."
}
```

### domcouch.form
```json
{
  "_type": "domcouch.form",
  "name": "Person",
  "fields": [
    {"name": "FirstName", "type": 0},
    {"name": "FullName", "type": 0, "computed": true,
     "formula": "FirstName + \" \" + LastName"}
  ]
}
```

### domcouch.acl
```json
{
  "_type": "domcouch.acl",
  "defaultLevel": 2,
  "roles": ["Sales"],
  "entries": {
    "Alice": {"level": 6, "userType": 0, "privileges": 127, "roles": ["Admin"]},
    "*/West/Acme": {"level": 2, "userType": -1, "privileges": 64, "roles": ["Sales"]}
  }
}
```

---

## Item Types

| Constant | Value | Meaning |
|----------|-------|---------|
| `TEXT` | 0 | Plain text / string values |
| `NUMBERS` | 1 | Numeric values |
| `DATETIMES` | 2 | Date/time values |
| `AUTHORS` | 3 | Domino Authors field (edit control) |
| `READERS` | 4 | Domino Readers field (read control) |
| `RICHTEXT` | 5 | Rich text / MIME (segment-based JSON) |

---

## Security: Readers, Authors, ACL

### Document-level (Readers/Authors items)
- **No reader fields** → document is public
- **No author fields** → editable by everyone
- Reader/Author fields support `[Role]` syntax — resolved via ACL

### Database-level (ACL)
- 7 access levels: NoAccess → Manager
- 9 privilege flags per-entry with level-appropriate defaults
- Wildcard entries: `*/West/Acme` matches any user in that hierarchy
- `getEffectiveLevel(userName)` — explicit → wildcard → default
- Enforcement: `getDocumentByUNID` checks READER, `save()` checks AUTHOR

---

## Couchbase SDK Pitfalls

1. **`getObject()`/`getArray()`/`contentAsObject()` throw ClassCastException** on nested arrays — use `get(name)` + instanceof, and `RawJsonTranscoder` for KV reads
2. **`rowsAsObject()` is one-shot** — collect in ArrayList before counting
3. **`values` is a reserved word** in N1QL — always backtick-escape
4. **Collection names can't start with `_`**
5. **DROP INDEX syntax**: `DROP INDEX name ON collection` (not collection-qualified)
6. **Community Edition**: 4-core query cap, 5-node limit
