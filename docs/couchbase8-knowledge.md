# Couchbase 8 — Knowledge Base for domcouch

> Collected from the domcouch project (2026-05-15/16). Couchbase 8.0.1 Community Edition.

---

## 1. Setup

### Docker

```yaml
# docker-compose.yml
services:
  couchbase:
    image: couchbase/server:community-8.0.1
    ports:
      - "8091-8096:8091-8096"
      - "11210:11210"
    environment:
      - CLUSTER_NAME=domcouch
      - COUCHBASE_ADMINISTRATOR_USERNAME=Administrator
      - COUCHBASE_ADMINISTRATOR_PASSWORD=password
```

First-time setup after container starts:
1. Open `http://localhost:8091`
2. Accept terms, create cluster
3. Default bucket `domcouch` with 100MB RAM quota (single-arg `getDatabase("name")` auto-creates)
4. Two-arg `getDatabase("bucket", "scope")` requires pre-existing bucket + scope

### SDK Version

```
com.couchbase.client:java-client:3.7.4
```

---

## 2. N1QL Query Patterns

### Document access with nested JSON arrays

Our multi-instance items schema stores fields as arrays:
```json
{"items": {"Department": [{"type": 0, "values": ["Engineering"]}]}}
```

N1QL access pattern:
```sql
doc.items.Department[0].`values`[0]
```

Backtick-quote `values` because it might be reserved in some contexts.
ALWAYS use `[0]` array indexing — fields are ALWAYS arrays in the new schema.

### ORDER BY with nested fields

```sql
SELECT unid, doc.* FROM `domcouch`.`contacts`.`documents` AS doc
WHERE doc._type = 'domcouch.document'
ORDER BY doc.items.Department[0].`values`[0]
LIMIT 200
```

ORDER BY with nested fields does a FULL SCAN + SORT. Couchbase does NOT use GSI indexes
for ORDER BY unless the query is fully covered by the index (all selected fields are in the
index key). Since we SELECT multiple fields, ORDER BY always does a sort.

### Key-based pagination (preferred over OFFSET)

```sql
-- First page
SELECT unid, doc.* FROM ... AS doc
WHERE doc._type = 'domcouch.document'
ORDER BY doc.items.Department[0].`values`[0]
LIMIT 200

-- Next page (key-based)
SELECT unid, doc.* FROM ... AS doc
WHERE doc._type = 'domcouch.document'
  AND doc.items.Department[0].`values`[0] > $cursorKey
ORDER BY doc.items.Department[0].`values`[0]
LIMIT 200

-- Previous page (reverse)
SELECT unid, doc.* FROM ... AS doc
WHERE doc._type = 'domcouch.document'
  AND doc.items.Department[0].`values`[0] < $cursorKey
ORDER BY doc.items.Department[0].`values`[0] DESC
LIMIT 200
-- Then reverse results in Java
```

Key-based pagination USES GSI indexes when the WHERE clause filters on the index key.
The EXPLAIN shows `IndexScan3` instead of `PrimaryScan3 → Order → Limit`.

### OFFSET — slow, avoid for random access

```sql
SELECT unid FROM ... AS doc WHERE ... ORDER BY ... LIMIT 1 OFFSET 5000
```

In Couchbase as in Domino, OFFSET is a full scan. For 50K docs, OFFSET 5000 takes ~1s.
Only use for secondary use cases (rare getNth calls).

### COUNT

```sql
SELECT COUNT(*) AS cnt FROM `domcouch`.`contacts`.`documents` AS doc
WHERE doc._type = 'domcouch.document'
```

### IN array check (folders)

```sql
-- Both work:
'TestInbox' IN doc.folders
ARRAY_CONTAINS(doc.folders, 'TestInbox')
```

### LIKE for prefix matching (categories)

```sql
WHERE doc.items._CATEGORIES[0].values[0] LIKE 'Engineering||%'      -- all descendants
WHERE doc.items._CATEGORIES[0].values[0] NOT LIKE 'Engineering||%||%'  -- direct children
```

### ORDER BY in navigator uses individual key columns, not concatenated _categories

The in-memory navigator builds ORDER BY from individual key column references:
```sql
ORDER BY doc.items.Department[0].`values`[0], doc.items.City[0].`values`[0]
```

NOT from a pre-computed `_categories` field (we tried this — the store-on-write approach
was removed in favor of dynamic concatenation in the N1QL query itself).

---

## 3. GSI Indexes

### Creation

```sql
-- Simple index on a single field
CREATE INDEX idx_name ON `bucket`.`scope`.`documents`(
  items.Department[0].`values`[0]
) WHERE _type = 'domcouch.document'

-- Composite index (multiple key columns)
CREATE INDEX idx_name ON `bucket`.`scope`.`documents`(
  items.Form[0].`values`[0],
  items.Department[0].`values`[0]
) WHERE _type = 'domcouch.document'
```

**Important**: The index key expression MUST match the query WHERE clause exactly.
Couchbase normalizes `items.Department[0].values[0]` to `((((items.Department)[0]).values)[0])`.

### Dropping

```sql
-- Correct syntax:
DROP INDEX idx_name ON `bucket`.`scope`.`documents`

-- WRONG (doesn't work):
DROP INDEX `bucket`.`scope`.`documents`.`idx_name`
```

### When indexes are USED

Indexes are used when the WHERE clause references the index key column:
```sql
-- USES index idx_nav_perf_nav:
SELECT ... WHERE doc._type = 'domcouch.document'
  AND doc.items.Department[0].values[0] > 'Engineering'
ORDER BY doc.items.Department[0].values[0]

-- Does NOT use index (no key column filter):
SELECT ... WHERE doc._type = 'domcouch.document'
ORDER BY doc.items.Department[0].values[0]
```

EXPLAIN shows `IndexScan3` when index is used, `PrimaryScan3 → Order` when not.

### USE INDEX hint — often ignored

```sql
-- CORRECT syntax (after alias):
FROM keyspace AS doc USE INDEX (idx_name USING GSI)

-- WRONG (before alias):
FROM keyspace USE INDEX (...) AS doc
```

Even with USE INDEX, Couchbase may ignore the hint if the index can't help the query
(e.g. index key doesn't match WHERE clause, or SELECT requires fields not in the index).

### Covering indexes for ORDER BY

Couchbase ONLY uses an index for ORDER BY when ALL selected fields are in the index key.
Since our navigator SELECTs `unid` + key columns + display columns, and covering indexes
would need to include everything, we don't create covering indexes. ORDER BY always does a
sort in memory for the navigator's full-collection scan.

### Index lifecycle (domcouch)

We use `TTLViewIndexService` (default):
- Index name = `idx_nav_{SHA-256(formula + keyColumns)[0:12]}`
- Created on first view access
- Metadata stored in `view_index_meta` collection
- `cleanupStale()` drops indexes whose `lastAccessAt` > TTL (1 hour default)
- Called on `Database.recycle()`

The `SimpleViewIndexService` alternative uses view-name-based index names with
explicit drop on `View.recycle()`.

### Checking indexes

```sql
-- All indexes for a scope
SELECT name, state, is_primary, index_key FROM system:indexes
WHERE bucket_id = 'domcouch' AND scope_id = 'contacts'
ORDER BY name

-- Check column existence in system:indexes is unreliable for collection name checks
-- Use system:collections instead
SELECT RAW COUNT(*) FROM system:collections
WHERE bucket_id = 'domcouch' AND scope_id = 'contacts' AND name = 'my_collection'
```

---

## 4. Collections and Scopes

### Collection naming rules

**Collection names can NOT start with `_` or `%`.**

```sql
-- WRONG:
CREATE COLLECTION `bucket`.`scope`.`_view_index_meta`

-- CORRECT:
CREATE COLLECTION `bucket`.`scope`.`view_index_meta`
```

### Creating collections

```sql
-- DDL to create a collection within an existing scope
CREATE COLLECTION `domcouch`.`contacts`.`view_index_meta` IF NOT EXISTS
```

Collections within a scope are NOT auto-created by `scope.collection(name)` in Couchbase 8.
Must use DDL.

### Checking scope existence

```sql
SELECT RAW COUNT(*) FROM system:scopes
WHERE bucket_id = 'domcouch' AND name = 'contacts'
```

---

## 5. Eventual Consistency

### KV vs N1QL timing

Documents saved via `collection.upsert()` may not be immediately visible to N1QL queries.
Sleep 500ms-1s between save and query. KV reads (`collection.get()`) can also return null
immediately after write — may need retry loops.

### For tests

- Prefer N1QL COUNT queries over KV `getDocumentByUNID()` for verifying writes
- Test documents in isolated scopes (not the demo's `contacts` scope)
- Use `Thread.sleep(500)` between write and read

---

## 6. EXPLAIN Analysis

```sql
EXPLAIN SELECT unid FROM `domcouch`.`contacts`.`documents` AS doc
WHERE doc._type = 'domcouch.document'
ORDER BY doc.items.Department[0].`values`[0]
LIMIT 10
```

Key plan operators to watch:
| Operator | Meaning |
|---|---|
| `PrimaryScan3` | Full scan of primary index |
| `IndexScan3` | Using a GSI secondary index |
| `Fetch` | Reading document body after index lookup |
| `Order` | In-memory sort (expensive) |
| `Limit` | Result cap |
| `Filter` | WHERE clause evaluation |

Goal: eliminate `Order` operator by having index handle sorting, or accept it for
full-collection scans.

---

## 7. Performance Numbers (50K docs, domcouch/contacts)

| Operation | Time | Notes |
|---|---|---|
| Full scan + ORDER BY (50K rows) | 29-35s | PrimaryScan + Fetch + Order |
| Key-based page (200 rows) | 200-500ms | IndexScan, WHERE keyCol > $cursor |
| OFFSET 5000 (single row) | 1-1.1s | Full scan up to offset |
| COUNT(*) | ~10ms | Indexed |
| getNth(5000) in-memory | 9μs | O(1) array access |
| getNext() in-memory | 82ns | O(1) |
| getNext() lazy (in-page) | 16μs | Buffer read |
| cat-skip | 364ns | In-memory scan |

---

## 8. Search Function

The `search()` method in CouchbaseDatabase uses `formulaTranslator.toN1ql()`
to convert Domino formulas to N1QL. Current known bug:

```
JsonArray cannot be cast to JsonObject
```

When results contain array fields (like `folders`), `result.rowsAsObject()` fails.
This affects `search()` but NOT `getAllDocuments()` or folder view queries.

---

## 9. Primary Index

Every collection needs a primary index for N1QL to work:

```sql
CREATE PRIMARY INDEX IF NOT EXISTS ON `bucket`.`scope`.`collection`
```

`CouchbaseDatabase.ensurePrimaryIndex()` creates this on construction.
The primary index always has `name = '#primary'` and `is_primary = true` in `system:indexes`.

---

## 10. Key Takeaways

1. **Key-based pagination > OFFSET** — USE `WHERE keyCol > $cursor ORDER BY keyCol LIMIT n`
2. **Indexes help WHERE, not ORDER BY** — unless covering the entire SELECT
3. **Collection names**: no `_` or `%` prefix
4. **Eventual consistency**: N1QL lags behind KV writes (500ms-1s)
5. **EXPLAIN** is your friend — verify index usage with `IndexScan3` vs `PrimaryScan3`
6. **CREATE INDEX syntax** uses `items.X[0].values[0]` for nested arrays
7. **DROP INDEX** uses `ON collection` syntax, not `collection.index` path
8. **Community Edition**: no GSI index ORDER BY limitations, but 4-core query cap, 5-node limit
