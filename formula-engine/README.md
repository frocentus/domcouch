# Formula Engine — Domino Formula Language Parser & Evaluator

> **Zero external dependencies.** Lexer → Parser → Evaluator pipeline for
> HCL Domino's `@Formula` language. Part of the DomCouch project.

## Architecture

```
Formula String
    │
┌───▼───┐    List<Token>    ┌────────┐    List<Expr>    ┌──────────┐
│ Lexer │ ────────────────► │ Parser │ ───────────────► │ Evaluator│────► Result
└───────┘                   └────────┘                  └────┬─────┘
                                                            │
                                              ┌─────────────▼──────────────┐
                                              │     FormulaContext          │
                                              │  (provides document fields, │
                                              │   UNID, database name, etc.) │
                                              └────────────────────────────┘
```

The evaluator resolves variables and @Functions against a pluggable
`FormulaContext`. Different implementations (document-backed, map-backed,
test context) satisfy different use cases without changing the evaluator.

## FormulaContext Interface

The evaluator does **not** know about Couchbase, Domino, or any database. All
interaction with the outside world goes through `FormulaContext`:

```java
public interface FormulaContext {

    /** Resolve a variable/field name. Returns null if the field does not exist;
        returns "" for empty fields. */
    Object resolve(String name);

    /** Write a value to a document field. Default: throws UnsupportedOperationException. */
    default void setField(String name, Object value) { throw ...; }

    /** Delete a document field. Default: throws UnsupportedOperationException. */
    default void deleteField(String name) { throw ...; }

    /** Return all field names on the document. Default: empty list. */
    default List<String> getFieldNames() { return List.of(); }

    /** Return the document's universal ID. Default: empty string. */
    default String getDocumentUNID() { return ""; }

    /** Return the current database name. Default: empty string. */
    default String getDatabaseName() { return ""; }
}
```

### Context Properties at a Glance

| Property                   | Return Type    | Purpose                                          | Default                             |
| -------------------------- | -------------- | ------------------------------------------------ | ----------------------------------- |
| `resolve(name)`            | `Object`       | Read a field value (null → "", absent → "")     | —                                   |
| `setField(name, val)`      | `void`         | Write to a document field                        | throws `ContextNotSupportedException` |
| `deleteField(name)`        | `void`         | Remove a document field                          | throws `ContextNotSupportedException` |
| `getFieldNames()`          | `List<String>` | Enumerate all fields                             | throws `ContextNotSupportedException` |
| `getDocumentUNID()`        | `String`       | Document universal ID (32-char hex)              | throws `ContextNotSupportedException` |
| `isDocumentValid()`        | `boolean`      | Whether the document is valid (not deleted)      | throws `ContextNotSupportedException` |
| `getDocumentSize()`        | `long`         | Document size in bytes                           | throws `ContextNotSupportedException` |
| `getAttachmentCount()`     | `int`          | Number of file attachments                       | throws `ContextNotSupportedException` |
| `getFolderNames()`         | `List<String>` | Folders containing this document                 | throws `ContextNotSupportedException` |
| `lockDocument()`           | `boolean`      | Lock the document                                | throws `ContextNotSupportedException` |
| `unlockDocument()`         | `boolean`      | Unlock the document                              | throws `ContextNotSupportedException` |
| `getDocumentLockStatus()`  | `String`       | Lock holder name or ""                           | throws `ContextNotSupportedException` |
| `isDocumentLockingEnabled()`| `boolean`     | Whether document locking is enabled              | throws `ContextNotSupportedException` |
| `getDatabaseName()`        | `String`       | Database file path (e.g. `"mail\\harald.nsf"`)   | throws `ContextNotSupportedException` |
| `getServerName()`          | `String`       | Server name (e.g. `"CN=Server/O=Org"`)           | throws `ContextNotSupportedException` |
| `getDatabaseTitle()`       | `String`       | Database title (e.g. `"Personnel Records"`)       | throws `ContextNotSupportedException` |
| `getReplicaID()`           | `String`       | 16-char hex replica ID                           | throws `ContextNotSupportedException` |
| `getDomain()`              | `String`       | Domino domain (e.g. `"MyOrg"`)                   | throws `ContextNotSupportedException` |
| `getEnvironmentValue(n)`   | `String`       | notes.ini / environment variable value           | throws `ContextNotSupportedException` |
| `markForDeletion()`        | `void`         | Mark document for deletion (soft delete)         | throws `ContextNotSupportedException` |
| `unmarkForDeletion()`      | `void`         | Unmark document for deletion                     | throws `ContextNotSupportedException` |
| `hardDelete()`             | `void`         | Permanently delete the document                  | throws `ContextNotSupportedException` |
| `addToFolder(name)`        | `void`         | Add document to a folder                         | throws `ContextNotSupportedException` |

### Evaluator Internal State (outside FormulaContext)

| State             | Purpose                                  | Set via                |
| ----------------- | ---------------------------------------- | ---------------------- |
| `currentUserName` | @UserName / @V3UserName / @UserNamesList | Constructor            |
| `tempScope`       | `:=` assignments, @Set temp variables    | Per-evaluation HashMap |

### ContextNotSupportedException — Graceful Degradation

Every default method throws `ContextNotSupportedException`. The evaluator
catches this in every @Function handler and returns a sensible default:

| Context method            | When not supported                               | Evaluator default        |
| ------------------------- | ------------------------------------------------ | ------------------------ |
| `setField()`              | @SetField, FIELD `:=`                            | value unchanged (no-op)  |
| `deleteField()`           | @DeleteField, REM {}                             | `""` (no-op)            |
| `getFieldNames()`         | @DocFields                                       | `[]`                     |
| `getDocumentUNID()`       | @DocumentUniqueID, @NoteID, @IsNewDoc            | `""`, `""`, `1.0` (new)  |
| `isDocumentValid()`       | @IsValid                                         | `1.0` (true)             |
| `getDocumentSize()`       | @DocLength, @DocCommittedLength                  | `0.0`                     |
| `getAttachmentCount()`    | @Attachments                                     | `0.0`                     |
| `getFolderNames()`        | @WhichFolders                                    | `[]`                      |
| `lockDocument()`          | @DocLock("LOCK")                                 | `1.0`                     |
| `unlockDocument()`        | @DocLock("UNLOCK")                               | `1.0`                     |
| `getDocumentLockStatus()` | @DocLock("STATUS")                               | `""`                     |
| `isDocumentLockingEnabled()`| @DocLock("LOCKINGENABLED")                      | `0.0`                     |
| `getDatabaseName()`       | @DbName[1] (file path)                           | `""`                      |
| `getServerName()`         | @DbName[0], @ServerName                          | `""`                      |
| `getDatabaseTitle()`      | @DbTitle                                         | `""`                      |
| `getReplicaID()`          | @ReplicaID                                       | `""`                      |
| `getDomain()`             | @Domain                                          | `""`                      |
| `getEnvironmentValue(n)`  | @Environment                                     | `""`                      |
| `markForDeletion()`       | @DeleteDocument                                  | `1.0`                     |
| `unmarkForDeletion()`     | @UndeleteDocument                                | `1.0`                     |
| `hardDelete()`            | @HardDeleteDocument                              | `1.0`                     |
| `addToFolder(name)`       | @AddToFolder                                     | `1.0`                     |

This means a **read-only context with only `resolve()`** works safely with
any formula — @SetField becomes a no-op, @DocumentUniqueID returns `""`, etc.

---

## @Function → Context Property Mapping

This table shows every @Function and which context properties it needs.
Functions not listed are **pure** — they need no context at all.

### Field Resolution (`ctx.resolve`)

These functions read field values from the document context.

| @Function                    | Context Property              | Notes                           |
| ---------------------------- | ----------------------------- | ------------------------------- |
| All `Variable` references    | `resolve(name)`               | Every field access in formulas  |
| `@IsAvailable(name)`         | `resolve(name)`               | Returns 1 if field exists       |
| `@IsUnavailable(name)`       | `resolve(name)`               | Returns 1 if field absent       |
| `@Unavailable(name)`         | `resolve(name)`               | Alias for @IsUnavailable        |
| `@GetField(name)`            | `resolve(name)`               | Returns field value or ""       |
| `@IsResponseDoc`             | `resolve("PARENTUNID")`       | 1 if parent doc exists          |
| `@InheritedDocumentUniqueID` | `resolve("PARENTUNID")`       | Parent UNID or ""               |
| `@Author`                    | `resolve("AUTHORS")`          | Authors field or ""             |
| `@Created`                   | `resolve("CREATED")`          | Creation timestamp              |
| `@Modified`                  | `resolve("MODIFIED")`         | Last-modified timestamp         |
| `@Accessed`                  | `resolve("ACCESSED")`         | Last-accessed timestamp         |
| `@AddedToThisFile`           | `resolve("ADDEDTOTHISFILE")`  | Added-to-db timestamp           |
| `DEFAULT field := expr`      | `resolve(field)` + `setField` | Reads first; writes if absent/0 |

### Field Writing (`ctx.setField`)

These functions modify document fields.

| @Function                 | Context Property      | Notes                          |
| ------------------------- | --------------------- | ------------------------------ |
| `@SetField(field; value)` | `setField(name, val)` | Direct field write             |
| `FIELD name := expr`      | `setField(name, val)` | FIELD keyword assignment       |
| `DEFAULT name := expr`    | `setField(name, val)` | Conditional write (if empty/0) |

### Field Deletion (`ctx.deleteField`)

| @Function            | Context Property    | Notes                    |
| -------------------- | ------------------- | ------------------------ |
| `@DeleteField`       | `deleteField(name)` | Removes field completely |
| `REM { field; ... }` | `deleteField(name)` | Keyword statement        |

### Document Identity (`ctx.getDocumentUNID` / `ctx.getFieldNames`)

| @Function           | Context Property    | Notes                          |
| ------------------- | ------------------- | ------------------------------ |
| `@DocumentUniqueID` | `getDocumentUNID()` | Full 32-char hex UNID          |
| `@NoteID`           | `getDocumentUNID()` | `"NT" + first 8 chars of UNID` |
| `@IsNewDoc`         | `getDocumentUNID()` | 1 if UNID is empty             |
| `@DocFields`        | `getFieldNames()`   | List of all field names        |

### Database Identity (`ctx.getDatabaseName` / `getServerName` / `getDatabaseTitle` / `getReplicaID`)

| @Function     | Context Property      | Notes                              |
| ------------- | --------------------- | ---------------------------------- |
| `@DbName`     | `getServerName()` +   | Returns list: `[serverName,        |
|               | `getDatabaseName()`   |             databaseName]`          |
| `@DbTitle`    | `getDatabaseTitle()`  | Database title (e.g. "Personnel")   |
| `@ReplicaID`  | `getReplicaID()`      | 16-char hex replica ID             |
| `@ServerName` | `getServerName()`     | Server name (e.g. "CN=...")         |

### Document Metadata (`ctx.getDocumentSize` / `getAttachmentCount` / `getFolderNames` / `isDocumentValid`)

| @Function            | Context Property       | Notes                          |
| -------------------- | ---------------------- | ------------------------------ |
| `@DocLength`         | `getDocumentSize()`    | Approximate size in bytes      |
| `@DocCommittedLength`| `getDocumentSize()`    | Size on disk (same as DocLength)|
| `@Attachments`       | `getAttachmentCount()` | Number of file attachments     |
| `@WhichFolders`      | `getFolderNames()`     | List of folder names           |
| `@IsValid`           | `isDocumentValid()`    | 1 if document is valid         |

### Document Locking (`ctx.lockDocument` / `unlockDocument` / `getDocumentLockStatus` / `isDocumentLockingEnabled`)

| @Function                  | Context Property              | Notes                     |
| -------------------------- | ----------------------------- | ------------------------- |
| `@DocLock("LOCK")`         | `lockDocument()`              | Lock the document         |
| `@DocLock("UNLOCK")`       | `unlockDocument()`            | Unlock the document       |
| `@DocLock("STATUS")`       | `getDocumentLockStatus()`     | Lock holder name or ""    |
| `@DocLock("LOCKINGENABLED")`| `isDocumentLockingEnabled()` | 1 if locking is enabled   |

### Session / Environment (`ctx.getDomain` / `getEnvironmentValue`)

| @Function        | Context Property          | Notes                           |
| ---------------- | ------------------------- | ------------------------------- |
| `@Domain`        | `getDomain()`             | Domino domain (e.g. "MyOrg")    |
| `@Environment`   | `getEnvironmentValue(n)`  | notes.ini variable value        |

### Document Lifecycle (`ctx.markForDeletion` / `unmarkForDeletion` / `hardDelete` / `addToFolder`)

| @Function             | Context Property      | Notes                          |
| --------------------- | --------------------- | ------------------------------ |
| `@DeleteDocument`     | `markForDeletion()`   | Soft delete (mark)             |
| `@UndeleteDocument`   | `unmarkForDeletion()` | Undo soft delete               |
| `@HardDeleteDocument` | `hardDelete()`        | Permanent delete               |
| `@AddToFolder`        | `addToFolder(name)`   | Add document to a folder       |

### User Identity (Evaluator `currentUserName`)

| @Function        | Source            | Notes                     |
| ---------------- | ----------------- | ------------------------- |
| `@UserName`      | `currentUserName` | Via Evaluator constructor |
| `@UserNamesList` | `currentUserName` | Single-element list       |
| `@V3UserName`    | `currentUserName` | Alias for @UserName       |

### Temp Variables (Evaluator `tempScope`)

| @Function          | Source      | Notes                    |
| ------------------ | ----------- | ------------------------ |
| `x := expr`        | `tempScope` | Scoped to evaluation run |
| `@Set(var; value)` | `tempScope` | Sets temp variable       |

---

## Pure Functions (No Context Required)

These functions are self-contained — they only operate on their arguments and
built-in state. They work with any `FormulaContext`, including `null`.

### Math (23 functions)

`@Abs`, `@ACos`, `@ASin`, `@ATan`, `@ATan2`, `@Cos`, `@Sin`, `@Tan`,
`@Exp`, `@Log`, `@Ln`, `@Sqrt`, `@Pi`, `@Power`, `@Integer`, `@Round`,
`@Sign`, `@Modulo`, `@FloatEq`, `@Random`, `@Max`, `@Min`, `@Sum`

### String (16)

`@Trim`, `@UpperCase`, `@LowerCase`, `@Length`, `@Left`, `@Right`,
`@Repeat`, `@Matches`, `@Like`, `@Contains`, `@Begins`, `@Ends`,
`@Explode`, `@Implode`, `@ProperCase`, `@NewLine`

### String — Substring (8)

`@Middle`, `@MiddleBack`, `@LeftBack`, `@RightBack`,
`@ReplaceSubstring`, `@Word`, `@FileDir`, `@Ascii`

### List (11)

`@Elements`, `@Count`, `@IsMember`, `@IsNotMember`, `@Member`,
`@Replace`, `@Subset`, `@Unique`, `@Sort`, `@Compare`, `@Transform`\*

> \* `@Transform` creates a wrapper context but only binds one variable;
> all other resolution delegates to the parent context.

### Control Flow (7)

`@If`, `@Do`, `@Return`, `@While`, `@DoWhile`, `@For`, `@Eval`

### Date/Time Construction (5)

`@Date`, `@Time`, `@TimeMerge`, `@Now`, `@Today`

### Date/Time Extraction (9)

`@Month`, `@Day`, `@Year`, `@Weekday`, `@Second`, `@Minute`, `@Hour`,
`@Tomorrow`, `@Yesterday`

### Date/Time Manipulation (3)

`@Adjust`, `@BusinessDays`, `@Zone`

### Type Conversion (5)

`@Text`, `@TextToNumber`, `@ToNumber`, `@TextToTime`, `@ToTime`

### Type Checking (5)

`@IsNumber`, `@IsText`, `@IsTime`, `@IsNull`

### Boolean Constants (8)

`@True`, `@False`, `@All`, `@Yes`, `@No`, `@Nothing`, `@Success`, `@Failure`

### Error Handling (3)

`@Error`, `@IsError`, `@IfError`

### Placeholders & Stubs (21)

`@ClientType`, `@DbExists`, `@GetCurrentTimeZone`, `@LanguagePreference`,
`@Locale`, `@Keywords`, `@ThisName`, `@ThisValue`, `@URQueryString`,
`@V4UserAccess`, `@RegQueryValue`,
`@GetIMContactListGroupNames`, `@UserNameLanguage`, `@UserNameList`,
`@Narrow`, `@Wide`, `@Prompt`, `@Password`,
`@PasswordQuality`, `@VerifyPassword`, `@PickList`

### No-ops (2)

`@Command`, `@PostedCommand` (always return "")

---

## Implementing a FormulaContext

### Minimal Context (read-only, no mutations)

Only `resolve()` is implemented — all other methods throw
`ContextNotSupportedException`. The evaluator handles this gracefully:
`@SetField` returns its argument, `@DocumentUniqueID` returns `""`, etc.

```java
FormulaContext ctx = name -> myFieldMap.get(name);

Evaluator ev = new Evaluator("Alice");
Object result = ev.evalExpr("@UpperCase(FirstName)", ctx);

// Even this works — @SetField is silently ignored
result = ev.evalExpr("@SetField(\"Status\"; \"active\")", ctx);
```

### Full Context (read + write + identity)

```java
FormulaContext ctx = new FormulaContext() {
    @Override
    public Object resolve(String name) {
        return document.get(name);       // or null
    }

    @Override
    public void setField(String name, Object value) {
        document.put(name, value);
    }

    @Override
    public void deleteField(String name) {
        document.remove(name);
    }

    @Override
    public List<String> getFieldNames() {
        return new ArrayList<>(document.keySet());
    }

    @Override
    public String getDocumentUNID() {
        return document.getId();
    }

    @Override
    public String getDatabaseName() {
        return db.getName();
    }
};
```

### Domino-Backed Context

```java
public class DominoFormulaContext implements FormulaContext {
    private final lotus.domino.Document doc;
    private final lotus.domino.Database db;
    private final lotus.domino.Session session;

    public DominoFormulaContext(lotus.domino.Document doc) {
        this.doc = doc;
        this.db = doc.getParentDatabase();
        this.session = db.getParent();
    }

    // ---- Fields ----
    @Override public Object resolve(String name) {
        var item = doc.getFirstItem(name);
        if (item == null) return null;
        var values = item.getValues();
        if (values == null || values.isEmpty()) return "";
        return values.size() == 1 ? values.get(0) : values;
    }
    @Override public void setField(String name, Object value) {
        doc.replaceItemValue(name, value);
    }
    @Override public void deleteField(String name) {
        doc.removeItem(name);
    }
    @Override public List<String> getFieldNames() {
        var names = new ArrayList<String>();
        for (var item : (java.util.Vector<?>) doc.getItems())
            names.add(((lotus.domino.Item) item).getName());
        return names;
    }

    // ---- Document identity ----
    @Override public String getDocumentUNID() {
        return doc.getUniversalID();
    }
    @Override public boolean isDocumentValid() {
        return doc.isValid();
    }

    // ---- Document metadata ----
    @Override public long getDocumentSize() {
        return doc.getSize();
    }
    @Override public int getAttachmentCount() {
        var eo = doc.getEmbeddedObjects();
        return eo != null ? eo.size() : 0;
    }
    @Override public List<String> getFolderNames() {
        var v = doc.getFolderReferences();
        return v != null ? new ArrayList<>((java.util.Vector<String>) v) : List.of();
    }

    // ---- Document locking ----
    @Override public boolean lockDocument() { return doc.lock(); }
    @Override public boolean unlockDocument() { return doc.unlock(); }
    @Override public String getDocumentLockStatus() {
        var holders = doc.getLockHolders();
        return (holders != null && !holders.isEmpty()) ? (String) holders.get(0) : "";
    }
    @Override public boolean isDocumentLockingEnabled() {
        return db.isDocumentLockingEnabled();
    }

    // ---- Database ----
    @Override public String getDatabaseName() {
        try { return db.getFilePath(); } catch (Exception e) { return ""; }
    }
    @Override public String getServerName() {
        try { return db.getServer(); } catch (Exception e) { return ""; }
    }
    @Override public String getDatabaseTitle() {
        try { return db.getTitle(); } catch (Exception e) { return ""; }
    }
    @Override public String getReplicaID() {
        try { return db.getReplicaID(); } catch (Exception e) { return ""; }
    }

    // ---- Session / environment ----
    @Override public String getDomain() {
        try { return session.getDomain(); } catch (Exception e) { return ""; }
    }
    @Override public String getEnvironmentValue(String name) {
        try { return session.getEnvironmentString(name, true); } catch (Exception e) { return ""; }
    }

    // ---- Document lifecycle ----
    @Override public void markForDeletion()  { doc.markForDeletion(); }
    @Override public void unmarkForDeletion() { doc.unmarkForDeletion(); }
    @Override public void hardDelete()        { doc.remove(true); }
    @Override public void addToFolder(String name) { doc.putInFolder(name); }

    // ---- Time zone (Domino canonical format) ----
    @Override public List<Number> getTimeZoneOffset(String timeDate) {
        try {
            java.util.TimeZone tz;
            if (timeDate != null && !timeDate.isEmpty()) {
                var dt = session.createDateTime(timeDate);
                tz = dt.getTimeZone();
            } else {
                tz = java.util.TimeZone.getDefault();
            }
            int offsetMs = tz.getRawOffset();
            int hours = offsetMs / 3600000;
            int minutes = (Math.abs(offsetMs) % 3600000) / 60000;
            int dst = tz.observesDaylightTime() ? 1 : 0;
            return List.of(hours, minutes, dst);
        } catch (Exception e) { return List.of(0, 0, 0); }
    }
    @Override public String getCanonicalTimeZone() {
        try { return session.getCurrentTimeZone().getCanonical(); } catch (Exception e) { return ""; }
    }
    @Override public String timeToTextInZone(String timeDate, String timeZone, String format) {
        try {
            var dt = session.createDateTime(timeDate);
            dt.convertToZone(session.getCurrentTimeZone());
            return dt.getLocalTime();
        } catch (Exception e) { return ""; }
    }
    @Override public String timeZoneToText(String timeZone, String format) {
        try {
            var tz = session.createDateTime("Today");
            // Translate canonical tz to text (simplified)
            return tz.getZoneTime();
        } catch (Exception e) { return ""; }
    }
}
```

### Test Context (Map-backed)

```java
Map<String, Object> fields = Map.of(
    "FirstName", "Alice",
    "Salary", 95000.0
);

// Lambda implements only resolve() — all other methods throw
// ContextNotSupportedException, caught by evaluator.
FormulaContext ctx = fields::get;
```

---

## Compiled Formula Caching

```java
FormulaTranslator translator = new FormulaTranslator("Alice");

// Compile once
CompiledFormula fullName = translator.compile("FirstName + \" \" + LastName");

// Evaluate many times (no Lexer/Parser overhead)
for (Document doc : docs) {
    FormulaContext ctx = new DocumentFormulaContext(doc);
    String name = (String) translator.evaluate(fullName, ctx);
}
```

---

## N1QL Translation Coverage

67 @Functions are translated to Couchbase N1QL expressions by
`N1qlTranslator`. The remaining functions are evaluated via the
Java formula engine (`Evaluator`).

### Translated to N1QL (67)

| Category | @Functions |
|----------|-----------|
| **String** | @UpperCase, @LowerCase, @Trim, @Length, @Left, @Right, @Contains, @Begins, @Ends, @ReplaceSubstring, @Repeat, @ProperCase, @NewLine, @Word, @Middle, @MiddleBack, @Explode, @Implode |
| **Math** | @Abs, @Sqrt, @Power, @Exp, @Log, @Ln, @Cos, @Sin, @Tan, @Pi, @Integer, @Round, @Modulo, @Sign, @Max, @Min |
| **Date** | @Date, @Adjust, @Month, @Day, @Year, @Hour, @Minute, @Second, @Weekday, @Today, @Now, @Tomorrow, @Yesterday, @Created, @Modified |
| **Type** | @Text, @TextToNumber, @IsNumber, @IsText, @IsNull |
| **List** | @Elements, @Count, @IsMember, @IsNotMember, @Explode, @Implode |
| **Logic** | @If, @IsAvailable, @IsUnavailable, @IsNewDoc |
| **Boolean** | @True, @False, @Yes, @No, @All, @Success |
| **Document** | @IsResponseDoc, @UserName |

### Evaluated in Java (remaining)

| Category | @Functions |
|----------|-----------|
| **Complex date** | @BusinessDays, @Time, @TimeMerge, @TimeToTextInZone, @TimeZoneToText |
| **Security** | @UserRoles, @Domain, @IsAuthor, @V4UserAccess |
| **Document** | @DocFields, @DocLength, @DocLock, @DocumentUniqueID, @NoteID, @Author, @Attachments, @DeleteDocument, @UndeleteDocument, @HardDeleteDocument, @AddToFolder, @WhichFolders, @IsValid, @GetField |
| **Control flow** | @While, @For, @DoWhile, @Eval, @Return, @Set, @SetField, @DeleteField, @Do, @Transform |
| **Misc** | @Ascii, @Char, @Compare, @FileDir, @Like, @Matches, @Prompt, @PickList, @Environment, @Random, @CheckFormulaSyntax, @IfError, @Error, @IsError, @Failure, @Unavailable, @IsResponseDoc, @ThisName, @ThisValue, @URLQueryString, @GetCurrentTimeZone, @Locale, @LanguagePreference, @ClientType, @Version, @Keywords, @RegQueryValue, @Narrow, @Wide, @DbExists, @DbName, @DbTitle, @ReplicaID, @ServerName |

