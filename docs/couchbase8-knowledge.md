# Couchbase Knowledge — N1QL Consistency & Best Practices

> **Skill file**: [`.skills/couchbase8/SKILL.md`](.skills/couchbase8/SKILL.md) — runtime patterns, ClassCastException, GSI indexes, EXPLAIN.

---

## N1QL vs KV Consistency

| Layer | Operation | Consistency | Speed |
|-------|-----------|-------------|-------|
| **KV** | `collection.upsert(id, doc)` | Instant | Fastest |
| **KV** | `collection.remove(id)` | Instant | Fastest |
| **KV** | `collection.get(id)` | Instant | Fastest |
| **N1QL** | `SELECT ... WHERE ...` | **Eventual** (default) | Fast |
| **N1QL** | `SELECT ...` with `REQUEST_PLUS` | **Strong** | Slower |

**Golden rule**: KV writes are NOT immediately visible to N1QL queries. The GSI index processes mutations asynchronously. Use `REQUEST_PLUS` to force the query to wait for all indexes.

---

## `REQUEST_PLUS` Scan Consistency

```java
import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.java.query.QueryOptions;

QueryResult result = scope.query(stmt,
    QueryOptions.queryOptions()
        .scanConsistency(QueryScanConsistency.REQUEST_PLUS));
```

Use it on every `scope.query()` call in the library. Without it:
- `doc.remove()` (KV) → N1QL still sees the document for ~100ms-2s
- Test cleanup deletes documents but subsequent queries return them
- Data accumulates across test runs

**Cost**: 50-200ms extra per query (waits for index sync). Acceptable for correctness.

---

## N1QL DELETE vs KV remove

| Approach | Use case | Consistency |
|----------|----------|-------------|
| `collection.remove(id)` | Single document, needs instant delete | KV, instant |
| `DELETE FROM ... WHERE _type = 'domcouch.document'` | **Bulk cleanup**, same index as SELECT | N1QL, use REQUEST_PLUS |

**Rule**: For bulk cleanup, use N1QL DELETE (not KV remove). N1QL DELETE shares the GSI index with N1QL SELECT — both see the same data at the same consistency level. KV remove + N1QL SELECT = drift.

---

## `rowsAsObject()` is One-Shot

```java
// ❌ WRONG — consumes iterable, then .size() returns 0
result.rowsAsObject().forEach(row -> {});
long count = result.rowsAsObject().size(); // 0!

// ✅ RIGHT — collect in ArrayList first
var rows = new ArrayList<JsonObject>();
result.rowsAsObject().forEach(rows::add);
log("Deleted %d rows", rows.size());
```

`QueryResult.rowsAsObject()` returns a **one-shot iterable**. Once consumed (forEach, iterator loop), subsequent calls return empty.

---

## Test Cleanup Pattern

```java
// @BeforeAll cleanup — N1QL DELETE by _type
String cp = "`bucket`.`scope`.`collection`";
session.getNativeCluster().query(
    "DELETE FROM " + cp + " AS d WHERE d._type = 'domcouch.document'",
    QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS)
).rowsAsObject().forEach(row -> {});

// @AfterAll cleanup — same pattern
// Per-document inner try/catch so one failure doesn't skip others
for (Document doc : db.getAllDocuments()) {
    try { doc.remove(); } catch (Exception e) { /* log */ }
}
```

---

## Field Paths for N1QL

Documents use the array-item schema:
```json
{
  "_type": "domcouch.document",
  "items": {
    "Form": [{"type": 0, "values": ["KanbanTask"]}]
  }
}
```

N1QL references:
- `d.items.Form[0].\`values\`[0]` — first item's first value (backtick `values` — reserved word!)
- `d._type = 'domcouch.document'` — type discriminator (always present, reliable)

**Prefer `_type` over nested field paths for DELETE/COUNT** — simpler, no escaping, always matches.
