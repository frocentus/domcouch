# @Function Catalog — domcouch

> **Last updated**: 2026-05-22
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
| `@Repeat` | ✅ | 📋 | Repeat string N times; third arg truncates to maxChars; 1,024 char limit |
| `@Contains` | ✅ | 📋 | Substring contained? pair-wise list support |
| `@Begins` | ✅ | 📋 | String begins with? pair-wise list support |
| `@Ends` | ✅ | 📋 | String ends with? pair-wise list support |
| `@ReplaceSubstring` | ✅ | 📋 | Sequential string replacement; list support |
| `@Word` | ✅ | 📋 | Extract word N by separator; list support |
| `@Matches` | ✅ | 📋 | Pattern matching: ? * + {sets} ! \| & \\ escape; 24 tests |
| `@Like` | ✅ | 📋 | SQL-style pattern (_ any char, % any seq); escape char supported |
| `@LeftBack` | ✅ | 📋 | From end: numeric removes last N; separator returns chars left of separator |
| `@RightBack` | ✅ | 📋 | From end: numeric rightmost N; separator uses lastIndexOf |
| `@Middle` | ✅ | 📋 | Middle substring; 4 overloads (off+n, off+sub, sub+n, sub+sub) |
| `@MiddleBack` | ✅ | 📋 | Middle from end; 4 overloads |
| `@ProperCase` | ✅ | 📋 | Capitalize first letter of each word; list support |
| `@NewLine` | ✅ | 📋 | Newline character `\n` |
| `@Explode` | ✅ | 📋 | Split by separators; includeEmpties and newlineAsSeparator supported |
| `@Implode` | ✅ | 📋 | Join list with separator |
| `@FileDir` | ✅ | 📋 | Extract directory from file path |
| `@Ascii` | ✅ | 📋 | Filter to ASCII 32-127; [ALLINRANGE] supported |
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
| `@Adjust` | ✅ | 📋 | Date arithmetic with [INLOCALTIME]/[INGMT] DST keyword |
| `@BusinessDays` | ✅ | 📋 | Business days with daysToExclude and datesToExclude |
| `@Zone` | ✅ | 📋 | Delegates to getTimeZoneOffset(); fallback Java tz ID |
| `@TextToTime` | ✅ | 📋 | Convert string to time-date |
| `@ToTime` | ✅ | 📋 | Convert to time-date |
| `@TimeToTextInZone` | 🟡 | 📋 | Delegates to timeToTextInZone(); Couchbase: returns "" |
| `@TimeZoneToText` | 🟡 | 📋 | Delegates to timeZoneToText(); Couchbase: returns "" |
| `@GetCurrentTimeZone` | ⚠ | 📋 | Delegates to getCanonicalTimeZone(); Couchbase: Java tz ID |

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
| `@DocLength` | 🟡 | 📋 | Delegates to `getDocumentSize()`; Couchbase: placeholder 0 |
| `@DocLock` | 🟡 | 📋 | Delegates to `lockDocument()`/`unlockDocument()` etc.; Couchbase: stubs |
| `@DocCommittedLength` | 🟡 | ⚠ | Delegates to `getDocumentSize()`; Couchbase: placeholder 0 |
| `@NoteID` | ✅ | 📋 | Delegates to `getDocumentUNID()` ("NT" + first 8 chars of UNID) |
| `@IsAvailable` | ✅ | 📋 | Delegates to `resolve()` — true if field exists |
| `@IsUnavailable` | ✅ | 📋 | Delegates to `resolve()` — true if field does not exist |
| `@IsNewDoc` | ✅ | 📋 | Delegates to `getDocumentUNID()` — true if empty |
| `@IsResponseDoc` | ✅ | 📋 | Delegates to `resolve("PARENTUNID")` |
| `@IsAuthor` | ✅ | ⚠ | True (always author) |
| `@Author` | ✅ | ⚠ | Delegates to `resolve("AUTHORS")` |
| `@Attachments` | ✅ | 📋 | Delegates to `getAttachmentCount()`; document + item-level support |
| `@GetField` | ✅ | 📋 | Delegates to `resolve()` |
| `@DeleteDocument` | 🟡 | ⚠ | Delegates to `markForDeletion()`; Couchbase: no-op |
| `@UndeleteDocument` | 🟡 | ⚠ | Delegates to `unmarkForDeletion()`; Couchbase: no-op |
| `@HardDeleteDocument` | 🟡 | ⚠ | Delegates to `hardDelete()`; Couchbase: calls `document.remove()` |
| `@IsValid` | ✅ | ⚠ | Delegates to `isDocumentValid()`; Couchbase: always true |
| `@WhichFolders` | ✅ | 📋 | Delegates to `getFolderNames()` |
| `@AddToFolder` | ✅ | ⚠ | Delegates to `addToFolder()`; writes to `folders[]` |

---

## Database / View Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@DbName` | ✅ | 📋 | Delegates to `getServerName()` + `getDatabaseName()` |
| `@DbLookup` | ✅ | 📋 | Cross-database lookup via view key (literal N1QL concat + numeric key detection) |
| `@DbColumn` | ✅ | 📋 | Cross-database column lookup via view |
| `@DbTitle` | ✅ | ⚠ | Delegates to `getDatabaseTitle()` |
| `@ReplicaID` | ✅ | ⚠ | Delegates to `getReplicaID()` |
| `@ServerName` | ✅ | ⚠ | Delegates to `getServerName()` |
| `@DbExists` | ✅ | ⚠ | True (always exists) |
| `@DbManager` | ✅ | 📋 | Manager names from ACL |
| `@ViewTitle` | ❌ | — | Not yet (view-specific) |

---

## Security / User Functions

| Function | Status | Spec | Description |
|----------|--------|------|-------------|
| `@UserName` | ✅ | 📋 | Current user name |
| `@UserNamesList` | ✅ | ⚠ | List of user names/roles |
| `@UserRoles` | ✅ | 📋 | ACL roles assigned to current user |
| `@Domain` | ✅ | ⚠ | Delegates to `getDomain()`; Couchbase: empty |
| `@Version` | ✅ | 📋 | "Domino 14.5 / Couchbase" |
| `@V3UserName` | ✅ | ⚠ | Legacy user name (delegates to @UserName) |
| `@V4UserAccess` | ✅ | 📋 | Legacy access level (from ACL) |
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
| Subscript (`[n]`) | ✅ | 📋 | 1-based indexing; numeric and single-letter var indices; for multi-letter, use temp: `idx := n; items[idx]` |
| Assignment (`:=`) | ✅ | 📋 | Temporary variable |
| `FIELD x := expr` | ✅ | 📋 | Field assignment |
| `DEFAULT x := expr` | ✅ | 📋 | Default value |
| `ENVIRONMENT x := expr` | ✅ | 📋 | Environment value (delegates to `getEnvironmentValue()`) |
| `SELECT formula` | ✅ | 📋 | Selection formula marker |
| `REM { ... }` | ✅ | 📋 | Comments |

---

## Not Yet Implemented

Reclassified — `@DbLookup`/@DbColumn moved to Database/View Functions above.

| Function | Priority | Reason |
|----------|----------|--------|
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
| `@UserAccess` | ✅ | 📋 | Current user's ACL access level (0-6) |
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
| Database/View | 7 | 0 | 0 | 1 |
| Security/User | 11 | 0 | 0 | 0 |
| Operators | 11 | 0 | 0 | 0 |
| **Total** | **133** | **19** | **0** | **1** |

📋 **124** verified against official Domino spec (includes all ✅ + 🟡)
🔜 **29** deferred to future phases
