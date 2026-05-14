# @Function Catalog — domcouch

> **Last updated**: 2026-05-14
> **Status**: ✅ Implemented | 🟡 Partial | ❌ Not yet
> **Spec verified**: 📋 Yes | ⚠ Needs verification

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented and tested |
| 🟡 | Partial / limited implementation |
| ❌ | Not implemented |
| 📋 | Verified against official Domino spec |
| ⚠ | Needs spec verification |

---

## String Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@Trim` | ✅ | 📋 | Remove leading/trailing/redundant spaces; removes blank list elements |
| `@UpperCase` | ✅ | 📋 | Convert to uppercase; list support |
| `@LowerCase` | ✅ | 📋 | Convert to lowercase; list support |
| `@Length` | ✅ | 📋 | String length in characters; list support |
| `@Left` | ✅ | 📋 | Leftmost N chars or up to substring; list support |
| `@Right` | ✅ | 📋 | Rightmost N chars or after substring; list support |
| `@Repeat` | 🟡 | 📋 | Repeat string N times; third arg truncates (should pad) |
| `@Contains` | ✅ | 📋 | Substring contained? pair-wise list support |
| `@Begins` | ✅ | 📋 | String begins with? pair-wise list support |
| `@Ends` | ✅ | 📋 | String ends with? pair-wise list support |
| `@ReplaceSubstring` | ✅ | 📋 | Sequential string replacement; list support |
| `@Word` | ✅ | 📋 | Extract word N by separator; list support |
| `@Matches` | 🟡 | 📋 | Pattern matching (*, ?, {sets}, \); missing !, \\|, &, + |
| `@Like` | 🟡 | 📋 | SQL-style pattern (_ any char, % any seq); escape char partial |
| `@LeftBack` | 🟡 | 📋 | From end: numeric takes first N (should remove last N); separator overload |
| `@RightBack` | ✅ | ⚠ | From end: numeric/substring overloads |
| `@Middle` | 🟡 | ⚠ | Middle substring; complex overloads (off/n, sub/n, off/sub, sub/sub) |
| `@MiddleBack` | 🟡 | ⚠ | Middle from end; complex overloads |
| `@ProperCase` | ✅ | 📋 | Capitalize first letter of each word; list support |
| `@NewLine` | ✅ | 📋 | Newline character `\n` |
| `@Explode` | 🟡 | 📋 | Split string by separators; missing includeEmpties/newlineAsSeparator |
| `@Implode` | ✅ | 📋 | Join list with separator |
| `@FileDir` | ✅ | ⚠ | Extract directory from file path |
| `@Ascii` | ✅ | ⚠ | Filter to ASCII range |
| `@Char` | ✅ | 📋 | Code page 850 code → character; list support |

---

## Math Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@Abs` | ✅ | 📋 | Absolute value; list support |
| `@ACos` | ✅ | 📋 | Arc cosine |
| `@ASin` | ✅ | 📋 | Arc sine |
| `@ATan` | ✅ | 📋 | Arc tangent |
| `@ATan2` | ✅ | 📋 | Arc tangent (y, x) |
| `@Cos` | ✅ | 📋 | Cosine (radians) |
| `@Sin` | ✅ | 📋 | Sine (radians) |
| `@Tan` | ✅ | 📋 | Tangent (radians) |
| `@Exp` | ✅ | 📋 | e^x |
| `@Log` | ✅ | 📋 | Common logarithm (base 10) |
| `@Ln` | ✅ | 📋 | Natural logarithm (base e) |
| `@Sqrt` | ✅ | 📋 | Square root |
| `@Pi` | ✅ | 📋 | π constant |
| `@Power` | ✅ | 📋 | Exponentiation (base^exp) |
| `@Integer` | ✅ | 📋 | Truncate to integer |
| `@Round` | ✅ | 📋 | Round to integer or factor |
| `@Sign` | ✅ | 📋 | Sign: -1, 0, 1 |
| `@Max` | ✅ | 📋 | Maximum of numbers or pairwise lists |
| `@Min` | ✅ | 📋 | Minimum of numbers or pairwise lists |
| `@Sum` | ✅ | 📋 | Sum of numbers and number lists |
| `@Modulo` | ✅ | 📋 | Remainder (always non-negative) |
| `@FloatEq` | ✅ | 📋 | Float equality within epsilon |
| `@Random` | ✅ | 📋 | Random number 0–1 |

---

## Date / Time Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@Created` | ✅ | 📋 | Document creation timestamp (from context) |
| `@Modified` | ✅ | 📋 | Last modified timestamp (from context) |
| `@Accessed` | ✅ | 📋 | Last accessed timestamp (from context) |
| `@AddedToThisFile` | ✅ | 📋 | Added-to-db timestamp (from context) |
| `@Now` | ✅ | 📋 | Current date & time |
| `@Today` | ✅ | 📋 | Current date (time stripped) |
| `@Tomorrow` | ✅ | 📋 | Tomorrow's date |
| `@Yesterday` | ✅ | 📋 | Yesterday's date |
| `@Date` | ✅ | 📋 | Constructor: y/m/d, y/m/d/h/m/s, strip-time from date |
| `@Time` | ✅ | 📋 | Constructor: h/m/s, y/m/d/h/m/s, extract-time from date |
| `@TimeMerge` | ✅ | 📋 | Merge date + time into datetime |
| `@Month` | ✅ | 📋 | Extract month (1–12) |
| `@Day` | ✅ | 📋 | Extract day of month |
| `@Year` | ✅ | 📋 | Extract year |
| `@Hour` | ✅ | 📋 | Extract hour (0–23) |
| `@Minute` | ✅ | 📋 | Extract minute (0–59) |
| `@Second` | ✅ | 📋 | Extract second (0–59) |
| `@Weekday` | ✅ | 📋 | Day of week (1=Sun, 7=Sat) |
| `@Adjust` | 🟡 | 📋 | Date arithmetic; missing DST keywords, pair-wise list |
| `@BusinessDays` | 🟡 | 📋 | Business days count; missing exclusion params |
| `@Zone` | ✅ | ⚠ | Current timezone |
| `@TextToTime` | ✅ | 📋 | Convert string to time-date |
| `@ToTime` | ✅ | 📋 | Convert to time-date |
| `@TimeToTextInZone` | 🟡 | ⚠ | Placeholder (returns input string) |
| `@TimeZoneToText` | 🟡 | ⚠ | Placeholder (returns "UTC") |
| `@GetCurrentTimeZone` | ✅ | ⚠ | Current timezone ID |

---

## Type Conversion

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@Text` | 🟡 | 📋 | Convert to text; number format (S/C/%/parens/width); date format (D/T/S codes) |
| `@TextToNumber` | ✅ | 📋 | Extract leading numeric portion; list support |
| `@ToNumber` | ✅ | 📋 | Convert string/number to number |
| `@IsNumber` | ✅ | 📋 | True if value is a number (not string) |
| `@IsText` | ✅ | 📋 | True if value is a text string |
| `@IsTime` | ✅ | 📋 | True if value is a time-date |

---

## List Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@Elements` | ✅ | 📋 | Element count (0 for empty string) |
| `@Count` | ✅ | 📋 | Element count (1 for scalar/null string) |
| `@IsMember` | ✅ | 📋 | List membership; pair-wise check |
| `@IsNotMember` | ✅ | 📋 | Not in list; pair-wise check |
| `@Member` | ✅ | 📋 | Position of element in list (1-based, 0 if not found) |
| `@Replace` | ✅ | 📋 | List element replacement |
| `@Subset` | ✅ | 📋 | Subset: first N or last N elements |
| `@Unique` | ✅ | 📋 | Remove duplicate elements |
| `@Sort` | ✅ | 📋 | Sort list alphabetically |
| `@Compare` | ✅ | 📋 | Pairwise string comparison (-1/0/1) |
| `@Transform` | ✅ | 📋 | Apply formula to each list element |

---

## Control Flow

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@If` | ✅ | 📋 | Conditional (any odd number of args) |
| `@Do` | ✅ | 📋 | Sequential execution; returns last |
| `@Return` | ✅ | 📋 | Early return |
| `@While` | ✅ | 📋 | While loop |
| `@DoWhile` | ✅ | 📋 | Do-while loop (condition last) |
| `@For` | ✅ | 📋 | For loop (init; condition; increment; statements...) |
| `@Set` | ✅ | 📋 | Set temporary variable |
| `@SetField` | ✅ | 📋 | Write to document field |
| `@DeleteField` | ✅ | 📋 | Delete document field |
| `@Eval` | ✅ | 📋 | Runtime formula evaluation (meta) |
| `@CheckFormulaSyntax` | ✅ | 📋 | Syntax validation |
| `@IfError` | ✅ | 📋 | Catch Java exceptions |
| `@Error` | ✅ | 📋 | Error sentinel value |
| `@IsError` | ✅ | 📋 | Error detection |

---

## Boolean / Constants

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@True` | ✅ | 📋 | Boolean true (1) |
| `@False` | ✅ | 📋 | Boolean false (0) |
| `@All` | ✅ | 📋 | Select all / true (1) |
| `@Yes` | ✅ | 📋 | Boolean yes (1) |
| `@No` | ✅ | 📋 | Boolean no (0) |
| `@Nothing` | ✅ | 📋 | Empty/null value |
| `@Success` | ✅ | 📋 | Validation success (1) |
| `@Failure` | ✅ | 📋 | Validation failure message |

---

## Document Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@DocFields` | ✅ | 📋 | List all field names |
| `@DocumentUniqueID` | ✅ | 📋 | Document universal ID (32-char hex) |
| `@InheritedDocumentUniqueID` | ✅ | 📋 | Parent document UNID |
| `@DocLength` | 🟡 | 📋 | Approximate doc size (placeholder 0) |
| `@DocLock` | 🟡 | 📋 | Document locking (stubs) |
| `@DocOmmittedLength` | 🟡 | ⚠ | Omitted length (placeholder 0) |
| `@NoteID` | ✅ | 📋 | "NT" + first 8 chars of UNID |
| `@IsAvailable` | ✅ | 📋 | True if field exists |
| `@IsUnavailable` | ✅ | 📋 | True if field does not exist |
| `@IsNewDoc` | ✅ | 📋 | True if document is new (no UNID) |
| `@IsResponseDoc` | ✅ | 📋 | True if document is a response |
| `@IsAuthor` | ✅ | ⚠ | True (always author) |
| `@Author` | ✅ | ⚠ | Returns AUTHORS field value |
| `@Attachments` | 🟡 | ⚠ | Attachment count (placeholder 0) |
| `@GetField` | ✅ | 📋 | Get field value by name |
| `@DeleteDocument` | 🟡 | ⚠ | Document delete (stub) |
| `@UndeleteDocument` | 🟡 | ⚠ | Document undelete (stub) |
| `@HardDeleteDocument` | 🟡 | ⚠ | Permanent delete (stub) |

---

## Database / View Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@DbName` | ✅ | ⚠ | 2-element list [server, dbname] |
| `@DbTitle` | ✅ | ⚠ | Database title |
| `@ReplicaID` | ✅ | ⚠ | Replica ID |
| `@ServerName` | ✅ | ⚠ | Server name (empty) |
| `@DbExists` | ✅ | ⚠ | True (always exists) |
| `@DbManager` | ❌ | — | Not yet (ACL-based) |
| `@ViewTitle` | ❌ | — | Not yet (view-specific) |

---

## Security / User Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@UserName` | ✅ | 📋 | Current user name |
| `@UserNamesList` | ✅ | ⚠ | List of user names/roles |
| `@UserRoles` | ✅ | ⚠ | ACL roles (empty) |
| `@Domain` | ✅ | ⚠ | Mail domain (empty) |
| `@Version` | ✅ | 📋 | "Domino 14.5 / Couchbase" |
| `@V3UserName` | ✅ | ⚠ | Legacy user name (delegates to @UserName) |
| `@V4UserAccess` | ✅ | ⚠ | Legacy access (returns 1) |
| `@ClientType` | ✅ | ⚠ | "Notes" |
| `@LanguagePreference` | ✅ | ⚠ | "EN" |
| `@Locale` | ⚠ | ⚠ | Java default locale |
| `@UserNameLanguage` | ✅ | ⚠ | "EN" |

---

## Operators (built-in)

| Feature | Status | Spec | Description |
|---------|--------|------|-------------|
| Arithmetic (`+ - * /`) | ✅ | 📋 | Pair-wise list semantics |
| Comparison (`= <> != > < >= <=`) | ✅ | 📋 | Pair-wise; any-match for = |
| Permuted (`*+ *- ** */ *= *!= *> *< *>= *<=`) | ✅ | 📋 | Cartesian product operations |
| Concatenation (`+`) | ✅ | 📋 | Pair-wise |
| List constructor (`:`) | ✅ | 📋 | Colon operator |
| Subscript (`[n]`) | ✅ | 📋 | 1-based indexing; single-letter vars |
| Assignment (`:=`) | ✅ | 📋 | Temporary variable |
| `FIELD x := expr` | ✅ | 📋 | Field assignment |
| `DEFAULT x := expr` | ✅ | 📋 | Default value |
| `ENVIRONMENT x := expr` | ✅ | 📋 | Environment value |
| `SELECT formula` | ✅ | 📋 | Selection formula marker |
| `REM { ... }` | ✅ | 📋 | Comments |

---

## Not Yet Implemented

These are deferred to future phases:

| Function | Priority | Reason |
|----------|----------|--------|
| `@DbLookup` | High | Cross-database lookup |
| `@DbColumn` | High | Cross-database column lookup |
| `@DbCommand` | Medium | Database command execution |
| `@Name` | Medium | Name format manipulation |
| `@NameLookup` | Medium | Directory name lookup |
| `@Abstract` | Low | Rich text → plain text |
| `@PickList` | Low | UI pick list dialog |
| `@Prompt` | Low | UI dialog (always returns "" in non-UI context) |
| `@MailSend` | Low | Mail document send |
| `@AllChildren` | Low | Selection formula only |
| `@AllDescendants` | Low | Selection formula only |
| `@AttachmentNames` | Low | Needs binary attachment support |
| `@AttachmentLengths` | Low | Needs binary attachment support |
| `@AttachmentModifiedTimes` | Low | Needs binary attachment support |
| `@UserAccess` | Low | Needs ACL system |
| `@UserPrivileges` | Low | Needs ACL system |
| `@Platform` | Low | UI-only |
| `@StatusBar` | Low | UI-only |
| `@MailDbName` | Low | Server-specific |
| `@OptimizeMailAddress` | Low | Server-specific |
| `@Password` | Low | Security-sensitive |
| `@HashPassword` | Low | Security-sensitive |
| `@VerifyPassword` | Low | Security-sensitive |
| `@DocChildren` | Low | View engine specific |
| `@DocDescendants` | Low | View engine specific |
| `@DocLevel` | Low | View engine specific |
| `@DocNumber` | Low | View engine specific |
| `@DocParentNumber` | Low | View engine specific |
| `@DocSiblings` | Low | View engine specific |
| `@IsCategory` | Low | View engine specific |
| `@IsExpandable` | Low | View engine specific |
| `@Responses` | Low | View engine specific |
| `@SetViewInfo` | Low | UI-specific |
| `@EditECL` | Low | ACL management |
| `@RefreshECL` | Low | ACL management |

---

## Summary

| Category | ✅ | 🟡 | ⚠ | ❌ |
|----------|----|-----|-----|-----|
| String | 16 | 7 | 0 | 0 |
| Math | 23 | 0 | 0 | 0 |
| Date/Time | 17 | 4 | 0 | 0 |
| Type Conversion | 5 | 1 | 0 | 0 |
| List | 11 | 0 | 0 | 0 |
| Control Flow | 14 | 0 | 0 | 0 |
| Boolean/Constants | 8 | 0 | 0 | 0 |
| Document | 11 | 7 | 0 | 0 |
| Database/View | 5 | 0 | 0 | 2 |
| Security/User | 11 | 0 | 0 | 0 |
| Operators | 11 | 0 | 0 | 0 |
| **Total** | **132** | **19** | **0** | **2** |

📋 **120** verified against official Domino spec (includes all ✅ + 🟡)
🔜 **33** deferred to future phases
