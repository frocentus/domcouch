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

| Property              | Return Type    | Purpose                                     | Default                             |
| --------------------- | -------------- | ------------------------------------------- | ----------------------------------- |
| `resolve(name)`       | `Object`       | Read a field value (null → "", absent → "") | —                                   |
| `setField(name, val)` | `void`         | Write to a document field                   | throws `ContextNotSupportedException` |
| `deleteField(name)`   | `void`         | Remove a document field                     | throws `ContextNotSupportedException` |
| `getFieldNames()`     | `List<String>` | Enumerate all fields                        | throws `ContextNotSupportedException` |
| `getDocumentUNID()`   | `String`       | Document universal ID                       | throws `ContextNotSupportedException` |
| `getDatabaseName()`   | `String`       | Current database name                       | throws `ContextNotSupportedException` |

### Evaluator Internal State (outside FormulaContext)

| State             | Purpose                                  | Set via                |
| ----------------- | ---------------------------------------- | ---------------------- |
| `currentUserName` | @UserName / @V3UserName / @UserNamesList | Constructor            |
| `tempScope`       | `:=` assignments, @Set temp variables    | Per-evaluation HashMap |

### ContextNotSupportedException — Graceful Degradation

Every default method throws `ContextNotSupportedException`. The evaluator
catches this in every @Function handler and returns a sensible default:

| Context method       | When not supported                      | Evaluator default        |
| -------------------- | --------------------------------------- | ------------------------ |
| `setField()`         | @SetField, FIELD `:=`                   | value unchanged (no-op)  |
| `deleteField()`      | @DeleteField, REM {}                    | `""` (no-op)            |
| `getFieldNames()`    | @DocFields                              | `[]`                     |
| `getDocumentUNID()`  | @DocumentUniqueID, @NoteID, @IsNewDoc   | `""`, `""`, `1.0` (new)  |
| `getDatabaseName()`  | @DbName, @DbTitle, @ReplicaID           | `["",""]`, `""`, `""`    |

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

### Database Identity (`ctx.getDatabaseName`)

| @Function    | Context Property    | Notes                        |
| ------------ | ------------------- | ---------------------------- |
| `@DbName`    | `getDatabaseName()` | Returns list: `["", dbName]` |
| `@DbTitle`   | `getDatabaseName()` | Returns db name string       |
| `@ReplicaID` | `getDatabaseName()` | Returns db name string       |

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

`@IsNumber`, `@IsText`, `@IsTime`, `@IsNull`, `@IsValid`

### Boolean Constants (8)

`@True`, `@False`, `@All`, `@Yes`, `@No`, `@Nothing`, `@Success`, `@Failure`

### Error Handling (3)

`@Error`, `@IsError`, `@IfError`

### Placeholders & Stubs (29)

`@ClientType`, `@DbExists`, `@GetCurrentTimeZone`, `@LanguagePreference`,
`@Locale`, `@Keywords`, `@ThisName`, `@ThisValue`, `@URQueryString`,
`@V4UserAccess`, `@Environment`, `@RegQueryValue`,
`@GetIMContactListGroupNames`, `@UserNameLanguage`, `@UserNameList`,
`@DeleteDocument`, `@UndeleteDocument`, `@HardDeleteDocument`,
`@DocCommittedLength`, `@AddToFolder`, `@WhichFolders`, `@Narrow`, `@Wide`,
`@DocLength`, `@DocLock`, `@Attachments`, `@Prompt`, `@Password`,
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

    @Override public Object resolve(String name) {
        var item = doc.getFirstItem(name);
        return item != null ? item.getValues() : null;
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
    @Override public String getDocumentUNID() {
        return doc.getUniversalID();
    }
    @Override public String getDatabaseName() {
        return db.getFilePath();
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
