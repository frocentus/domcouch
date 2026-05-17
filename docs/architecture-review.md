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

### Issues

| Concern | Impact |
|---|---|
| **Deep nesting**: `items.NAME[0].values[0]` | Couchbase SDK `getObject()`/`getArray()` throw `ClassCastException` — required custom `get(name)+instanceof` workaround |
| **Type-per-item overhead**: Each field stores `{type, values}` wrapper | ~30% storage overhead vs. direct typed values |
| **Multi-instance items as arrays**: Correct for Domino semantics (multiple Body items) | Makes N1QL queries verbose: `items.BODY[0].values[0]` |

### Recommendation
```json
{
  "_type": "domcouch.document",
  "unid": "...",
  "form": "KanbanBoard",
  "fields": {
    "Title": "My Board",
    "Priority": 3,
    "Readers": ["Alice", "Bob"],
    "Tags": ["java", "couchbase"]
  },
  "_multi": {
    "Body": ["Para 1", "Para 2"]  // only for multi-instance items
  }
}
```
Flat `fields` for 95% of single-value items, `_multi` for the rare multi-instance case. N1QL: `doc.fields.Title`, `doc._multi.Body[0]`. No more `getObject()`/`getArray()` SDK issues.

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

## 5. Batching Logic — ❌ None

### Requirement
> Traditional Domino code loops through a View and calls `getNextDocument()`, fetching documents one by one. Your facade MUST implement lazy loading, internal batch-fetching behind the scenes.

### Current State
- **In-memory navigator**: Builds full index upfront (30s for 50K docs). O(1) `getNext()` but terrible first-access latency.
- **Lazy navigator**: Key-based pagination with page size 200. Sequential walk is μs-fast within a page. But `getNth()` uses slow OFFSET.
- **No batching in `DocumentCollection`**: Iterating `getAllDocuments()` does N+1.

### Recommendation
Add `getNextDocument()` batching to `DocumentCollection`:
```java
public Document getNextDocument() {
    if (buffer.isEmpty() && hasMore) {
        // Fetch next batch of 100 document IDs
        List<String> batch = n1qlPage(cursorKey, 100);
        // Bulk KV fetch
        buffer = collection.getAll(batch);
        cursorKey = batch.get(batch.size() - 1).key;
    }
    return buffer.poll();
}
```

---

## 6. Memory & Recyclability — 🟡

| Requirement | Status |
|---|---|
| `recycle()` clears caches | ✅ Views cleared, navigator index cleared, KV connections released |
| No lingering caches | 🟡 `formulaCache` in CouchbaseDatabase never cleared, `views` map only cleared on `recycle()` |
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
| 3 | Flat `fields` schema | Eliminates all ClassCastException workarounds | High — schema migration |
| 4 | `lotus.domino.*` package | True drop-in compatibility | Low — wrapper classes |
| 5 | Batching in DocumentCollection | Prevents N+1 in legacy code patterns | Medium |
| 6 | `_multi` for multi-instance items | 90% reduction in array nesting | Medium — tied to #3 |
