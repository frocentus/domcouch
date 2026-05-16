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

## GSI Indexes

### CREATE
```sql
CREATE INDEX idx_name ON `bucket`.`scope`.`documents`(
  items.Department[0].`values`[0]
) WHERE _type = 'domcouch.document'
```

### DROP (correct syntax!)
```sql
DROP INDEX idx_name ON `bucket`.`scope`.`documents`
-- NOT: DROP INDEX `bucket`.`scope`.`documents`.`idx_name`
```

### When indexes are USED
Only when WHERE clause filters on the index key column:
```sql
-- USES index:
WHERE items.Department[0].values[0] > 'Engineering'  -- IndexScan3 in EXPLAIN

-- Does NOT use index:
WHERE _type = 'domcouch.document'  -- PrimaryScan3 in EXPLAIN
```

### USE INDEX hint — goes AFTER alias
```sql
FROM keyspace AS doc USE INDEX (idx_name USING GSI)
```

### Indexes DON'T help ORDER BY
Unless the index covers ALL selected fields. Since we SELECT multiple fields,
ORDER BY always does a full sort. This is the primary bottleneck for the
in-memory navigator build (~30s for 50K docs).

### Index lifecycle (domcouch)
- `TTLViewIndexService` (default): hash-based names, 1h TTL, metadata in `view_index_meta`
- `SimpleViewIndexService`: name-based, explicit drop
- Created on view creation, dropped on `View.recycle()`

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

1. **`search()` throws `JsonArray cannot be cast to JsonObject`** — when results have array fields like `folders`. Caused by `rowsAsObject()` on documents with top-level arrays.
2. **Collection names can't start with `_`** — error message is obscure.
3. **DROP INDEX syntax**: must use `ON collection`, not collection-qualified path.
4. **USE INDEX ignored**: even with hint, Couchbase may pick PrimaryScan if index doesn't match WHERE.
5. **Community Edition**: no GSI ORDER BY limitations, but 4-core query cap, 5-node limit.

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
