---
name: couchbase8
description: Use when working with Couchbase 8 in the domcouch project. Covers N1QL patterns, GSI indexes, collection rules, eventual consistency, EXPLAIN analysis, and performance benchmarks.
---

# Couchbase 8 — domcouch Reference

> Covers Couchbase 8.0.1 Community Edition as used in the domcouch project.

## Quick Reference — Common Patterns

### N1QL: nested JSON field access (our multi-instance array schema)
```sql
doc.items.Department[0].`values`[0]
```
Fields are ALWAYS arrays — use `[0]` before `.values[0]`.

### Key-based pagination (preferred)
```sql
-- Page forward:
SELECT unid FROM docs AS doc
WHERE doc._type = 'domcouch.document'
  AND doc.items.KeyCol[0].`values`[0] > $cursor
ORDER BY doc.items.KeyCol[0].`values`[0] LIMIT 200

-- Page backward:
... AND doc.items.KeyCol[0].`values`[0] < $cursor
ORDER BY doc.items.KeyCol[0].`values`[0] DESC LIMIT 200
-- Reverse results in Java
```

### OFFSET — slow, only for rare random access
```sql
LIMIT 1 OFFSET 5000  -- ~1s for 50K docs
```

### COUNT
```sql
SELECT COUNT(*) AS cnt FROM docs AS doc WHERE doc._type = 'domcouch.document'
```

### IN array check (folders)
```sql
'TestInbox' IN doc.folders
```

---

## GSI Indexes — Best Practices for Index Usage

### The Golden Rule
Couchbase uses a GSI index **only when the WHERE clause filters on the index key column**.
The optimizer ignores indexes for ORDER BY unless they fully cover the SELECT.

### Matching index to query — the formula

```
Query:     WHERE doc.items.Department[0].`values`[0] = 'Engineering'
              + ORDER BY doc.items.Department[0].`values`[0]

Index MUST: CREATE INDEX ... ON ...(items.Department[0].`values`[0])
              WHERE _type = 'domcouch.document'

Result:    EXPLAIN → IndexScan3 ✅
```

### Pattern that WORKS (key-based pagination)

```sql
-- 1. Create the index
CREATE INDEX idx_nav_dept ON `domcouch`.`contacts`.`documents`(
  items.Department[0].`values`[0]
) WHERE _type = 'domcouch.document'

-- 2. Query with WHERE on the indexed column
SELECT unid, doc.items.DEPARTMENT[0].`values`[0] AS d
FROM `domcouch`.`contacts`.`documents` AS doc
WHERE doc._type = 'domcouch.document'
  AND doc.items.Department[0].`values`[0] > 'Engineering'  -- ← INDEX USED HERE
ORDER BY doc.items.Department[0].`values`[0]                 -- ← index provides order
LIMIT 200

-- EXPLAIN: IndexScan3 ✅ (not PrimaryScan3 → Order)
```

### Pattern that DOES NOT WORK (full collection scan)

```sql
-- Same index, but query has no key-column filter:
SELECT unid FROM docs AS doc
WHERE doc._type = 'domcouch.document'    -- ← no index key filter
ORDER BY doc.items.Department[0].`values`[0]  -- ← full sort, index ignored
LIMIT 200

-- EXPLAIN: PrimaryScan3 → Fetch → Order ❌ (index ignored!)
```

### Verification checklist — before writing code

1. **Create the index** with the EXACT key expression you'll use in WHERE
2. **EXPLAIN the query** — confirm you see `IndexScan3`, not `PrimaryScan3 → Order`
3. **If PrimaryScan**: check that WHERE clause references the index key column directly
4. **USE INDEX hint** only as fallback (often ignored by optimizer anyway)
5. **Key expression must match**: `items.Department[0].values[0]` in index = `doc.items.Department[0].values[0]` in query (alias prefix is OK)

### When to NOT create an index

- **Full-collection scan** (navigator build): index won't help ORDER BY
- **Document count queries**: primary index handles COUNT(*) efficiently
- **Complex WHERE with multiple conditions**: test with EXPLAIN first

### Our navigator indexes — why they work

The `CouchbaseLazyViewNavigator` uses key-based pagination:
```sql
WHERE keyCol > $cursor ORDER BY keyCol LIMIT pageSize
```
This triggers the GSI index because the WHERE clause filters on the index key.
The in-memory navigator uses `SELECT ... ORDER BY` (no key filter) — index ignored.

### CREATE (index expression must match query)

```sql
CREATE INDEX idx_name ON `bucket`.`scope`.`documents`(
  items.Department[0].`values`[0]
) WHERE _type = 'domcouch.document'
```

### DROP (correct syntax!)

```sql
DROP INDEX idx_name ON `bucket`.`scope`.`documents`
-- NOT: DROP INDEX `bucket`.`scope`.`documents`.`idx_name`   ← SYNTAX ERROR
```

### Index lifecycle (domcouch)
- `TTLViewIndexService` (default): hash-based names, 1h TTL, metadata in `view_index_meta`
- `SimpleViewIndexService`: name-based, explicit drop
- Created on view creation, dropped on `View.recycle()`
- Index name = `idx_nav_{SHA-256(formula+keyColumns)[0:12]}` — shared across views with same formula

### USE INDEX hint — goes AFTER alias, often ignored

```sql
FROM keyspace AS doc USE INDEX (idx_name USING GSI)
```

---

## Collections & Scopes

### Naming rule: NO leading `_` or `%`
```sql
CREATE COLLECTION `bucket`.`scope`.`view_index_meta` IF NOT EXISTS
-- NOT: `_view_index_meta`
```

### Check existence
```sql
SELECT RAW COUNT(*) FROM system:collections
WHERE bucket_id = 'domcouch' AND scope_id = 'contacts' AND name = 'x'
```

---

## Eventual Consistency

- N1QL lags KV writes: **500ms-1s** delay
- KV reads can also return null after immediate write
- For tests: prefer N1QL COUNT over `getDocumentByUNID()` after save
- Use `Thread.sleep(500)` between write and N1QL read

### `REQUEST_PLUS` scan consistency (eliminates drift)

```java
import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.java.query.QueryOptions;

QueryResult result = scope.query(stmt,
    QueryOptions.queryOptions()
        .scanConsistency(QueryScanConsistency.REQUEST_PLUS));
```

- **Cost**: 50-200ms extra per query (waits for all GSI indexes to catch up)
- **Use**: every `scope.query()` call in library code, test cleanup queries
- **Without it**: `doc.remove()` (KV) → N1QL still sees doc for 100ms-2s → ghosts

### N1QL `DELETE` (bulk cleanup — same index as SELECT)

```java
// Clean all domcouch documents from a collection
session.getNativeCluster().query(
    "DELETE FROM `bucket`.`scope`.`collection` AS d WHERE d._type = 'domcouch.document'",
    QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS)
);
```

**Why N1QL DELETE beats KV remove for bulk ops**: N1QL DELETE and N1QL SELECT share the same GSI index. KV remove is invisible to N1QL until the index catches up. Use N1QL DELETE whenever you need the deletion to be reflected in subsequent N1QL queries.

### `rowsAsObject()` — one-shot iterable

```java
// The iterable can only be consumed ONCE. Collect in ArrayList first:
var rows = new ArrayList<JsonObject>();
result.rowsAsObject().forEach(rows::add);
long count = rows.size(); // correct — NOT result.rowsAsObject().size()
```

### Test cleanup pattern

```java
// @BeforeAll — bulk N1QL DELETE by _type
String cp = collectonPath;
session.getNativeCluster().query(
    "DELETE FROM " + cp + " AS d WHERE d._type = 'domcouch.document'",
    QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS)
);

// @AfterAll — per-document cleanup (inner try/catch so one failure doesn't skip others)
for (Document doc : db.getAllDocuments()) {
    try { doc.remove(); } catch (Exception e) { log(e); }
}
```

---

## Couchbase SDK ClassCastException — The Universal Fix

**Couchbase SDK methods `getObject(name)`, `getArray(name)`, and `contentAsObject()` throw**
`ClassCastException: JsonArray cannot be cast to JsonObject` **when the JSON value is the
opposite type. This affects ALL SDK methods that do type-specific access on objects
containing arrays (our multi-instance item schema).**

### Rule: NEVER use getObject/getArray/contentAsObject directly

```java
// WRONG — throws ClassCastException for array values
JsonObject item = itemsObj.getObject(name);
JsonArray arr = itemsObj.getArray(name);
JsonObject doc = result.contentAsObject();

// CORRECT — use generic get() + instanceof
Object val = itemsObj.get(name);
if (val instanceof JsonArray arr) { ... }
else if (val instanceof JsonObject io) { ... }
```

### For KV reads specifically: use RawJsonTranscoder

```java
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.codec.RawJsonTranscoder;

GetResult result = collection.get(unid,
    GetOptions.getOptions().transcoder(RawJsonTranscoder.INSTANCE));
String rawJson = result.contentAs(String.class);
JsonObject json = JsonObject.fromJson(rawJson);
```

### Affected methods (all fixed in domcouch)

| Method | Location | Fix |
|---|---|---|
| `collection.get()` | CouchbaseDatabase | RawJsonTranscoder |
| `items.getObject(name)` | canRead(), loadFromJson() | `get(name)` + instanceof |
| `items.getArray(name)` | loadFromJson() | `get(name)` + instanceof |
| `result.contentAsObject()` | getAllDocuments(), search() | Avoided — use `getDocumentByUNID()` |
| `items.getArray(name)` | CouchbaseViewNavigator, CouchbaseLazyViewNavigator | `get(name)` + instanceof |

---


## EXPLAIN

```sql
EXPLAIN SELECT ... FROM docs AS doc WHERE ...
```

Key operators:
| Operator | Meaning |
|---|---|
| `PrimaryScan3` | Full scan — want to avoid |
| `IndexScan3` | Using GSI index — good |
| `Fetch` | Reading doc body after index |
| `Order` | In-memory sort — expensive |
| `Limit` | Result cap |

---

## Performance (50K docs)

| Operation | Time |
|---|---|
| Full scan + ORDER BY | 29-35s |
| Key-based page (200 rows) | 200-500ms |
| OFFSET 5000 | 1-1.1s |
| COUNT(*) | ~10ms |
| getNth in-memory | 9μs |
| getNext in-memory | 82ns |
| getNext lazy (in-page) | 16μs |
| cat-skip | 364ns |

---

## Known Bugs / Pitfalls

1. **`search()` throws `JsonArray cannot be cast to JsonObject`** — when results have array fields like `folders`. Caused by `rowsAsObject()` on documents with top-level arrays. Fixed: `fetchDocumentsByN1qlIds` now uses `SELECT meta().id` + `getDocumentByUNID()` (RawJsonTranscoder) instead of `SELECT doc FROM ...`.
2. **`removeDocument()` — don't swallow exceptions**: Silent `catch(Exception){}` hides KV deletion failures. Let exceptions propagate or log them. Test cleanup should catch per-document so one failure doesn't skip others.
3. **Collection names can't start with `_`** — error message is obscure.
4. **DROP INDEX syntax**: must use `ON collection`, not collection-qualified path.
5. **USE INDEX ignored**: even with hint, Couchbase may pick PrimaryScan if index doesn't match WHERE.
6. **Community Edition**: no GSI ORDER BY limitations, but 4-core query cap, 5-node limit.

---

## JSON Schema (our format)

```json
{
  "_type": "domcouch.document",
  "unid": "...",
  "form": "Person",
  "items": {
    "FirstName": [{"type": 0, "values": ["John"]}],
    "Salary": [{"type": 1, "values": [95000]}],
    "Body": [
      {"type": 5, "values": ["Para 1"]},
      {"type": 5, "values": ["Para 2"]}
    ]
  },
  "folders": ["Inbox"],
  "_attachments": {...}
}
```

Items are ALWAYS arrays. Single-value names are single-element arrays.
N1QL: `items.FIRSTNAME[0].values[0]`.
