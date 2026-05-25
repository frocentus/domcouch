# Domino API Coverage — domcouch v0.3.0

> Updated: 2026-05-22 — computeWithForm, REQUEST_PLUS scan consistency, batch document fetching

## Legend

✅ Implemented & tested 🟡 Stub / limited ❌ Not implemented  
🔵 N/A (UI/client only) 🧪 Formula translator

---

## Core Data Layer

| Interface               | Method                                            | Status | Notes                                                                   |
| ----------------------- | ------------------------------------------------- | ------ | ----------------------------------------------------------------------- |
| **Session**             | `createSession()`                                 | ✅     | Connects to Couchbase cluster                                           |
|                         | `createSession(conn,user,pass,ramMB)` 🆕          | ✅     | Custom bucket RAM quota (domcouch extension)                            |
|                         | `getDatabase(name)`                               | ✅     | Creates bucket per DB, scope `data`                                     |
|                         | `getDatabase(bucket, name)`                       | ✅     | Scope within pre-existing bucket                                        |
|                         | `getUserName()`                                   | ✅     |                                                                         |
|                         | `isValid()`                                       | ✅     | Cluster health check                                                    |
|                         | `recycle()`                                       | ✅     | Disconnects cluster                                                     |
|                         | `getNativeCluster()`                              | ✅     | Internal — Couchbase Cluster handle                                     |
|                         | `createTrustedSession()`                          | ❌     |                                                                         |
|                         | `getDatabase(server, dbFile)`                     | ❌     | Multi-server Notes path                                                 |
|                         | `getAddressBooks()`                               | ❌     |                                                                         |
|                         | `getEnvironmentString/Value()`                    | ❌     |                                                                         |
|                         | `setEnvironmentVar()`                             | ❌     |                                                                         |
|                         | `evaluate(formula)`                               | ❌     | Server-side eval                                                        |
|                         | `getUserGroupNameList()`                          | ❌     |                                                                         |
|                         | `createName() / createDateTime()`                 | ❌     | Factory methods                                                         |
|                         | `getNotesVersion() / getPlatform()`               | ❌     |                                                                         |
|                         | `isOnServer() / isTrustedSession()`               | ❌     |                                                                         |
|                         | `getAgentContext()`                               | ❌     |                                                                         |
| **Database**            | `createDocument()`                                | ✅     |                                                                         |
|                         | `getDocumentByUNID(unid)`                         | ✅     | Enforces Reader fields                                                  |
|                         | `getView(name)`                                   | ✅     | Lazy-create, N1QL-backed                                                |
|                         | `createView(name, formula)`                       | ✅     | Notes-style formula → N1QL                                              |
|                         | `createView(name, formula, keyCol)`               | ✅     | Explicit key column                                                     |
|                         | `FTSearch(query)`                                 | ✅     | Parameterized N1QL (no injection)                                       |
|                         | `FTSearch(query, maxDocs)`                        | ✅     |                                                                         |
|                         | `search(formula)`                                 | ✅     | Formula translator applied                                              |
|                         | `getAllDocuments()`                               | ✅     | Enforces Reader fields                                                  |
|                         | `getDocumentCount()`                              | ✅     | N1QL COUNT (⚠ not reader-filtered)                                      |
|                         | `getCurrentUserName()` 🆕                         | ✅     | domcouch extension — current session user                               |
|                         | `canRead(json, user)` 🆕                          | ✅     | Static utility — centralized reader check                               |
|                         | `getCollectionPath()` 🆕                          | ✅     | domcouch extension — escaped N1QL path                                  |
|                         | `getTitle() / setTitle()`                         | ✅     |                                                                         |
|                         | `getFileName()`                                   | ✅     | Scope name                                                              |
|                         | `isOpen()`                                        | ✅     |                                                                         |
|                         | `recycle()`                                       | ✅     |                                                                         |
|                         | `open(server, dbFile)`                            | ❌     | Static factory                                                          |
|                         | `openByReplicaID()`                               | ❌     |                                                                         |
|                         | `getACL() / grantAccess() / revokeAccess()`       | ✅     | Full ACL API: 7 levels, 9 privileges, roles, wildcards |
|                         | `queryAccessRoles()`                              | ❌     |                                                                         |
|                         | `isFTIndexed() / createFTIndex()`                 | ❌     |                                                                         |
|                         | `getSize() / getCreated() / getLastModified()`    | ❌     |                                                                         |
|                         | `compact() / createCopy() / createReplica()`      | ❌     |                                                                         |
|                         | `getProfileDocument()`                            | ❌     |                                                                         |
|                         | `getServer() / getFilePath()`                     | ❌     |                                                                         |
| **Document**            | `getFirstItem(name)`                              | ✅     |                                                                         |
|                         | `getItems()`                                      | ✅     |                                                                         |
|                         | `hasItem(name)`                                   | ✅     |                                                                         |
|                         | `replaceItemValue(name, val)`                     | ✅     |                                                                         |
|                         | `save()`                                          | ✅     | Upsert + Author field enforcement                                       |
|                         | `remove()`                                        | ✅     | Author field enforcement                                                |
|                         | `getUniversalID()`                                | ✅     | 32-char hex                                                             |
|                         | `getCreated() / getLastModified()`                | ✅     |                                                                         |
|                         | `isDirty()`                                       | ✅     |                                                                         |
|                         | `copyToDatabase(targetDb)`                        | ✅     | Cross-bucket copy (⚠ copies author fields too)                          |
|                         | `makeResponse(parent)`                            | ✅     | Sets parentUNID                                                         |
|                         | `getParentDocumentUNID()`                         | ✅     |                                                                         |
|                         | `getResponses()`                                  | ✅     | Queries by parentUNID                                                   |
|                         | `isResponse()`                                    | ✅     |                                                                         |
|                         | `putInFolder(name)`                               | ✅     | Stored in folders[]                                                     |
|                         | `removeFromFolder(name)`                          | ✅     |                                                                         |
|                         | `getFolderNames()`                                | ✅     |                                                                         |
|                         | `getNoteID()`                                     | ❌     |                                                                         |
|                         | `computeWithForm()`                               | ✅     | Domino-standard + explicit Form variant                                 |
|                         | `encrypt() / decrypt() / sign()`                  | ❌     |                                                                         |
|                         | `getAttachment()`                                 | ✅     |                                                                         |
|                         | `getEmbeddedObjects()`                            | ✅     |                                                                         |
|                         | `copyAllItems() / removeItem()`                   | 🟡     | removeItem ✅, copyAllItems ❌                                          |
|                         | `lock() / unlock() / isLocked()`                  | ❌     |                                                                         |
|                         | `isValid() / isDeleted() / isProfile()`           | ❌     |                                                                         |
| **DocumentCollection**  | `getFirstDocument()`                              | ✅     |                                                                         |
|                         | `getNextDocument()`                               | ✅     |                                                                         |
|                         | `getNthDocument(n)`                               | ✅     |                                                                         |
|                         | `getCount()`                                      | ✅     |                                                                         |
|                         | `reset()`                                         | ✅     |                                                                         |
|                         | `iterator()`                                      | ✅     | for-each compatible                                                     |
|                         | `merge(other)`                                    | ✅     | Set union                                                               |
|                         | `intersect(other)`                                | ✅     | Set intersection                                                        |
|                         | `subtract(other)`                                 | ✅     | Set difference                                                          |
|                         | `stampAll(name, val)`                             | ✅     | Bulk item update                                                        |
| **View**                | `getName()`                                       | ✅     |                                                                         |
|                         | `getAllEntries()`                                 | ✅     | N1QL SELECT                                                             |
|                         | `getAllEntriesByKey(key)`                         | ✅     | Indexed lookup                                                          |
|                         | `getEntryByKey(key)`                              | ✅     |                                                                         |
|                         | `FTSearch(query)`                                 | ✅     |                                                                         |
|                         | `FTSearch(query, maxDocs)`                        | ✅     |                                                                         |
|                         | `getEntryCount()`                                 | ✅     | N1QL COUNT                                                              |
|                         | `refresh()`                                       | 🟡     | N1QL is always fresh                                                    |
|                         | `getAliases()`                                    | ❌     |                                                                         |
|                         | `getColumns() / getColumnNames()`                 | ❌     |                                                                         |
|                         | `getSelectionFormula()`                           | ❌     |                                                                         |
|                         | `isCategorized() / isFolder() / isHierarchical()` | ❌     |                                                                         |
| **ViewEntry**           | `getColumnValues()`                               | ✅     |                                                                         |
|                         | `getColumnValue(i)`                               | ✅     |                                                                         |
|                         | `getDocument()`                                   | ✅     | Fetches by UNID                                                         |
|                         | `getUniversalID()`                                | ✅     |                                                                         |
|                         | `isValid()`                                       | ✅     |                                                                         |
|                         | `getPosition()`                                   | ✅     |                                                                         |
| **ViewEntryCollection** | `getFirstEntry()`                                 | ✅     |                                                                         |
|                         | `getNextEntry()`                                  | ✅     |                                                                         |
|                         | `getNthEntry(n)`                                  | ✅     |                                                                         |
|                         | `getCount()`                                      | ✅     |                                                                         |
|                         | `reset()`                                         | ✅     |                                                                         |
|                         | `iterator()`                                      | ✅     |                                                                         |
| **Item**                | `getName()`                                       | ✅     |                                                                         |
|                         | `getType()`                                       | ✅     | TEXT/NUMBERS/DATETIMES/AUTHORS/READERS/RICHTEXT                         |
|                         | `getValues()`                                     | ✅     |                                                                         |
|                         | `getValueString()`                                | ✅     |                                                                         |
|                         | `getValueInt() / getValueDouble()`                | ✅     |                                                                         |
|                         | `getValueDateTime()`                              | ✅     |                                                                         |
|                         | `getValueCustomData() / setValueCustomData()`     | ✅     |                                                                         |
|                         | `isReaders()`                                     | ✅     | Reader field check                                                      |
|                         | `isAuthors()`                                     | ✅     | Author field check                                                      |
|                         | `setReaders(flag)`                                | ✅     | Mark/unmark as Reader type                                              |
|                         | `setAuthors(flag)`                                | ✅     | Mark/unmark as Author type                                              |
|                         | `readersItem(name, vals)` 🆕factory               | ✅     | domcouch extension — convenience factory                                |
|                         | `authorsItem(name, vals)` 🆕factory               | ✅     | domcouch extension — convenience factory                                |
| **DateTime**            | `getLocalTime()`                                  | ✅     |                                                                         |
|                         | `getGMTTime()`                                    | ✅     |                                                                         |
|                         | `toJavaDate()`                                    | ✅     |                                                                         |
|                         | `timeDifference(other)`                           | ✅     |                                                                         |
|                         | `adjustDay(n) / adjustHour(n)`                    | ✅     |                                                                         |
|                         | `isDateOnly()`                                    | ✅     |                                                                         |
| **NotesException**      | `id` field                                        | ✅     |                                                                         |
|                         | `getMessage()`                                    | ✅     |                                                                         |

| **Name**                | `parse(name)`                                     | ✅     | Canonical + abbreviated format parsing                                  |
|                         | `getCanonical()`                                  | ✅     | "CN=.../OU=.../O=.../C=..."                                           |
|                         | `getAbbreviated()`                                | ✅     | "name/unit/org/country"                                                |
|                         | `getCommon()`                                     | ✅     | Common Name component                                                   |
|                         | `getOrganization()`                               | ✅     | Organization component                                                  |
|                         | `getOrgUnit1-4()`                                 | ✅     | Organizational unit components (1-4)                                   |
|                         | `getCountry() / getGiven() / getSurname()`        | ✅     | Extended components                                                     |
|                         | `isHierarchical()`                                | ✅     | Has OU/O/C components                                                   |
|                         | `getAddr821() / getAddr822()`                     | ✅     | RFC 821/822 internet addresses                                          |
|                         | `getLanguage()`                                   | ✅     | Language code suffix                                                    |

| **ACL**                 | `getEntry(name) / createACLEntry / removeACLEntry`| ✅     | Per-user ACL management                                                 |
|                         | `getRoles() / addRole / removeRole / renameRole`  | ✅     | Role definitions                                                        |
|                         | `LEVEL_* constants (7 levels)`                    | ✅     | NoAccess → Manager                                                      |
|                         | `PRIV_* constants (9 privileges)`                 | ✅     | Per-entry, level-appropriate defaults                                   |
|                         | `getRolesForUser(name)`                           | ✅     | Role resolution with wildcard support                                   |
|                         | `isConsistentACL / getInternetLevel`              | ✅     | Extended settings                                                       |

| **ACLEntry**            | `getLevel() / setLevel()`                         | ✅     | Access level (resets privileges on change)                              |
|                         | `isRoleEnabled / enableRole / disableRole`        | ✅     | Per-entry role assignment                                               |
|                         | `isWildcard() / matchesWildcard()`                | ✅     | */West/Acme pattern matching                                            |
|                         | `isPerson() / isServer() / isGroup()`             | ✅     | User type classification                                                |
|                         | `canCreateDocuments / canDeleteDocuments`         | ✅     | Privilege-based convenience                                             |

---

## Formula Translator (🧪 — Notes → N1QL)

| Feature                            | Status | Example                            |
| ---------------------------------- | ------ | ---------------------------------- |
| `SELECT` keyword                   | ✅     | `SELECT Form = 'Person'`           |
| `&` / `\|` / `!` operators         | ✅     | `A & B`, `A \| B`, `!A`            |
| Field = value                      | ✅     | `Status = 'Active'`                |
| Field IS NOT MISSING               | ✅     | `LastName IS NOT MISSING`          |
| `@All`                             | ✅     | All documents                      |
| `@IsResponseDoc`                   | ✅     |                                    |
| `@Contains(Field; val)`            | ✅     |                                    |
| `@Begins(Field; val)`              | ✅     |                                    |
| `@Ends(Field; val)`                | ✅     |                                    |
| `@IsMember(val; Field)`            | ✅     |                                    |
| `@IsNotMember(val; Field)`         | ✅     |                                    |
| `@LowerCase(Field)`                | ✅     |                                    |
| `@UpperCase(Field)`                | ✅     |                                    |
| `@Trim(Field)`                     | ✅     |                                    |
| `@Length(Field)`                   | ✅     |                                    |
| `@Left(Field; n)`                  | ✅     |                                    |
| `@Right(Field; n)`                 | ✅     |                                    |
| `@Today` / `@Now`                  | ✅     | → `NOW_STR()`                      |
| `@Created` / `@Modified`           | ✅     | → `doc.created`/`doc.lastModified` |
| `@UserName`                        | ✅     | From Session                       |
| `@IsAvailable(Field)`              | ✅     |                                    |
| `@IsNumber(val)`                   | ✅     |                                    |
| `@IsText(val)`                     | ✅     |                                    |
| `@If(cond; a; b)`                  | ✅     | → `CASE WHEN`                      |
| `@Matches(Field; pattern)`         | ❌     | Complex regex mapping              |
| `@Explode` / `@Implode`            | ❌     |                                    |
| `@ReplaceSubstring`                | ❌     |                                    |
| `@Word`                            | ❌     |                                    |
| `@Year` / `@Month` / `@Day`        | ❌     | Date part extraction               |
| `@Adjust(date; y;m;d)`             | ❌     |                                    |
| `@TextToNumber` / `@TextToTime`    | ❌     |                                    |
| `@Environment`                     | ❌     |                                    |
| `@UserRoles`                       | ❌     |                                    |
| `@Elements`                        | ❌     |                                    |
| `@Abstract`                        | ❌     |                                    |
| `@DocChildren` / `@DocDescendants` | ❌     |                                    |
| `@Author`                          | ❌     |                                    |

> **Note**: The formula engine (`com.domcouch.formula`) also supports full computed
> evaluation of 150+ @Functions (not N1QL translation).
> See `docs/function-catalog.md` for the complete matrix.

---

## ViewNavigator

Full Domino ViewNavigator API for categorized views.

| Method                     | Status | Notes                                                |
| -------------------------- | ------ | ---------------------------------------------------- |
| `createViewNav()`          | ✅     | Returns CouchbaseViewNavigator (in-memory index)     |
| `createLazyViewNav()` 🆕   | ✅     | Returns CouchbaseLazyViewNavigator (key-based pages) |
| `getFirst/Last/Next/Prev`  | ✅     |                                                      |
| `getNth(n)`                | ✅     | O(1) in-memory; O(n) lazy                            |
| `getNextCategory/Category` | ✅     | In-memory only; lazy: limited support                |
| `getChild/Parent/Sibling`  | 🟡     | In-memory only; lazy: not supported                  |
| `getPos/gotoPos`           | 🟡     | In-memory only                                       |
| `createViewNavFrom*`       | ✅     | From entry, category, children, descendants          |
| `createViewNavMaxLevel`    | ✅     |                                                      |
| `markAllRead/Unread`       | ✅     | No-op (Couchbase)                                    |
| `setCacheSize(n)`          | ❌     | Implementation exists (pageSize), not exposed on API |
| `setBufferMaxEntries(n)`   | ❌     | Implementation exists (N1QL LIMIT), not exposed      |
| `setAutoUpdate(bool)`      | ❌     | Not implemented                                      |

## RichTextItem

Segment-based rich text with style support. Stored as JSON array in items.

| Method | Status | Notes |
|--------|--------|-------|
| `appendText(text)` | ✅ | Plain text segment |
| `appendDocLink(doc, comment)` | ✅ | Document link segment |
| `addNewLine(count)` | ✅ | 1=single, 2=paragraph |
| `beginSection(title)` / `endSection()` | ✅ | Collapsible sections |
| `beginInsert()` / `endInsert()` | ✅ | Insert-position marker |
| `getNotesFont(name, styleBits)` | ✅ | Named font style |
| `appendStyle(style, text)` | ✅ | Styled text |
| `getSegmentCount()` | ✅ | Segment count |
| `getPlainText()` | ✅ | All text concatenated |
| `isMimeType(mime)` / `getMimeType()` | ✅ | MIME type checks |
| `compact()` | ✅ | Remove empty segments |
| `getContentJSON()` | ✅ | JSON representation |
| `RichTextStyle.create().with(...)` | ✅ | Builder: font-name, size, color, bold, italic, underline, strikethrough |

## Folders

Database-level folder CRUD. Folders are virtual views with N1QL `'name' IN doc.folders`.

| Method                   | Status | Notes                                  |
| ------------------------ | ------ | -------------------------------------- |
| `createFolder(name)`     | ✅     | Creates View with auto-generated N1QL  |
| `getFolder(name)`        | ✅     | Returns cached View                    |
| `getFolderNames()`       | ✅     | Lists all folder names                 |
| `removeFolder(name)`     | ✅     | Removes from folderNames + views cache |
| `isFolder(name)`         | ✅     | Checks folderNames set                 |
| `putInFolder(name)`      | ✅     | Adds to `folders[]` on document        |
| `removeFromFolder(name)` | ✅     | Removes from `folders[]`               |
| `getFolderNames()` (doc) | ✅     | Returns document's `folders[]` list    |

## Forms

Form definitions (design documents). Stored as `_type = "domcouch.form"` in Couchbase.

| Method | Status | Notes |
|--------|--------|-------|
| `Database.createForm(name, fields)` | ✅ | Persists to Couchbase |
| `Database.getForm(name)` | ✅ | Loads from Couchbase |
| `Database.getFormNames()` | ✅ | N1QL query for form docs |
| `Document.computeWithForm(form, all, validate)` | ✅ | Evaluates computed/default/validation formulas |
| `Document.computeWithForm(computeAll, validate)` | ✅ | Domino-standard — resolves Form from doc's Form item |
| `Form.getFields()` | ✅ | Returns List<FieldDefinition> |
| `Form.getField(name)` | ✅ | Lookup by name (case-insensitive) |
| `FieldDefinition: computed/composed/display` | ✅ | Computation modes |
| `FieldDefinition: getFormula/getDefaultFormula` | ✅ | Formula strings |
| `FieldDefinition: getValidationFormula` | ✅ | Validation with error message |
| `FieldDefinition: isMultiValue/isRichText` | ✅ | Type flags |
| `FieldDefinition: getNumberFormat/getDateFormat` | ✅ | Format patterns |

## ViewIndexService

Pluggable N1QL index lifecycle for categorized views.

| Implementation                  | Strategy                                              |
| ------------------------------- | ----------------------------------------------------- |
| `TTLViewIndexService` (default) | Hash-based names, 1h TTL, metadata in view_index_meta |
| `SimpleViewIndexService`        | View-name-based, explicit drop on recycle             |
| `ViewIndexService`              | Interface — swap for custom strategies                |

---

## Major Missing Classes / Interfaces

| Class                         | Priority | Notes                                   |
| ----------------------------- | -------- | --------------------------------------- |
| **EmbeddedObject**            | Medium   | Attachments (partial — getAttachment ✅) |
| **Agent / AgentContext**      | Medium   | Scheduled agents                        |
| **Stream**                    | Low      | Binary I/O                              |
| **Log**                       | Low      | Logging                                 |
| **DxlExporter / DxlImporter** | Low      | DXL format                              |
| **DateRange**                 | Low      | Date ranges                             |
| **International**             | Low      | Locale/timezone                         |
| **Registration**              | Low      | User registration                       |
| **Replication**               | Low      | Replication objects                     |
| **Outline**                   | Low      | Navigator outlines                      |

---

## Totals

| Area                            | ✅  | 🟡  | ❌  |
| ------------------------------- | --- | --- | --- |
| Core Data Layer (interfaces)    | 75  | 1   | ~40 |
| Formula Translator (@Functions) | 19  | 0   | ~15 |
| Missing Classes                 | 0   | 0   | 10  |
| Domcouch Extensions             | 8   | —   | —   |

---

## Domcouch Extensions (not in lotus.domino)

| Class    | Method / Constant                              | Purpose                                            |
| -------- | ---------------------------------------------- | -------------------------------------------------- |
| Session  | `createSession(conn, user, pass, ramQuotaMB)`  | Custom bucket RAM quota for auto-created buckets   |
| Database | `getCurrentUserName()`                         | Current user for Reader/Author enforcement         |
| Database | `canRead(JsonObject, String)` _(static)_       | Centralized reader-field check on raw JSON         |
| Database | `getCollectionPath()`                          | Escaped N1QL collection path                       |
| Database | `DEFAULT_SCOPE` _(constant)_                   | Fixed scope name when database = bucket (`"data"`) |
| Item     | `readersItem(name, values)` _(static factory)_ | Convenience — creates a Readers-type item          |
| Item     | `authorsItem(name, values)` _(static factory)_ | Convenience — creates an Authors-type item         |
