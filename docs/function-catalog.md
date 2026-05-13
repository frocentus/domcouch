# @Function Catalog — domcouch

> **Last updated**: 2026-05-13  
> **Status**: ✅ Implemented | 🟡 Partial | ❌ Not yet  
> **Spec verified**: 📋 Yes | ⚠ Needs verification

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented and tested |
| 🟡 | Partial / limited implementation |
| ❌ | Not yet implemented |
| 🚫 | Not applicable (no-op in Couchbase) |
| 📋 | Verified against official Domino spec |
| ⚠ | Needs spec verification — may differ from Domino |

---

## String Functions

### @Trim
```
@Trim(string)
```
Removes leading, trailing, and redundant spaces. Collapses consecutive spaces.
If parameter is a list, each element is trimmed and empty elements are removed.
| Arg | Type | Description |
|-----|------|-------------|
| 1 | String | Input string |

| Example | Result |
|---------|--------|
| `@Trim("  hello  ")` | `"hello"` |
| `@Trim(Subject)` | Subject field, trimmed |

Status: ✅ 📋

### @UpperCase
```
@UpperCase(string)
```
Converts to uppercase.
| Arg | Type | Description |
|-----|------|-------------|
| 1 | String | Input string |

| Example | Result |
|---------|--------|
| `@UpperCase("hello")` | `"HELLO"` |
| `@UpperCase(city1 := "London")` | `"LONDON"` |

Status: ✅ 📋

### @LowerCase
```
@LowerCase(string)
```
Converts to lowercase.
| Arg | Type | Description |
|-----|------|-------------|
| 1 | String | Input string |

| Example | Result |
|---------|--------|
| `@LowerCase("HELLO")` | `"hello"` |

Status: ✅ 📋

### @Length
```
@Length(string)
```
Returns the number of characters.
| Arg | Type | Description |
|-----|------|-------------|
| 1 | String | Input string |

| Example | Result |
|---------|--------|
| `@Length("hello")` | `5` |

Status: ✅ 📋

### @Left
```
@Left(string; numberOfChars)
```
Returns the leftmost characters.
| Arg | Type | Description |
|-----|------|-------------|
| 1 | String | Input string |
| 2 | Number | Character count |

| Example | Result |
|---------|--------|
| `@Left("hello"; 2)` | `"he"` |
| `@Left("hello"; 10)` | `"hello"` (no error on overflow) |

Status: ✅ 📋

### @Right
```
@Right(string; numberOfChars)  or  @Right(string; subString)
```
Returns rightmost N characters, or characters right of a substring.
Negative N → whole string. List-aware.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | String to search |
| 2 | Number or Text | Character count, or substring |

| Example | Result |
|---------|--------|
| `@Right("Lennard Wallace"; 3)` | `"ace"` |
| `@Right("Lennard Wallace"; " ")` | `"Wallace"` (right of space) |
| `@Right("Lennard" : "Wallace"; 3)` | `["ard", "ace"]` (list) |
| `@Right("hello"; -1)` | `"hello"` (negative → full) |

Status: ✅ 📋

### @Repeat
```
@Repeat(string; count; maxChars)
```
Repeats the string `count` times. Optional third argument truncates to `maxChars`.
List-aware.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | String to repeat |
| 2 | Number | Repeat count |
| 3 | Number (optional) | Max characters to return |

| Example | Result |
|---------|--------|
| `@Repeat("Hello"; 3)` | `"HelloHelloHello"` |
| `@Repeat("Bye"; 2; 5)` | `"ByeBy"` (truncated) |
| `@Repeat("Hello" : "Bye"; 3)` | `["HelloHelloHello", "ByeByeBye"]` (list) |

Status: ✅ 📋

### @Contains  *(Phase 2)*
```
@Contains(string; substring)
```
Returns True (1) if any element of `string` contains any element of `substring`.
Case-sensitive. If either parameter is a list, all pairs are tested.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | The string(s) to search |
| 2 | Text or text list | The substring(s) to find |

| Example | Result |
|---------|--------|
| `@Contains("Hi There"; "Th")` | `1` |
| `@Contains("Hello"; "hello")` | `0` (case-sensitive) |
| `@Contains("Tom":"Dick":"Harry"; "Harry":"Tom")` | `1` (list in list) |
| `@Contains("Tom"; "Tom":"Dick":"Harry")` | `1` (scalar in list) |

Status: ✅ 📋

### @Begins  *(Phase 2)*
```
@Begins(string; substring)
```
Returns True (1) if any element of `string` starts with any element of `substring`.
Case-sensitive. If either parameter is a list, all pairs are tested.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | The string(s) to check |
| 2 | Text or text list | The prefix(es) to match |

| Example | Result |
|---------|--------|
| `@Begins("Hi There"; "Hi")` | `1` |
| `@Begins("Hi There"; "hi")` | `0` (case-sensitive) |
| `@Begins("Luigi Smith"; "Luigi":"Florence":"Henri")` | `1` (list match) |

Status: ✅ 📋

### @Ends  *(Phase 2)*
```
@Ends(string; substring)
```
Returns True (1) if any element of `string` ends with any element of `substring`.
Case-sensitive. If either parameter is a list, all pairs are tested.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | The string(s) to check |
| 2 | Text or text list | The suffix(es) to match |

| Example | Result |
|---------|--------|
| `@Ends("Hi There"; "re")` | `1` |
| `@Ends("Hi There"; "The")` | `0` |
| `@Ends("Alice Owens"; "Owens":"Irons":"Baker")` | `1` (list match) |

Status: ✅ 📋

### @ReplaceSubstring  *(Phase 2)*
```
@ReplaceSubstring(sourceList; fromList; toList)
```
Replaces values in `sourceList` matching `fromList` with corresponding values
from `toList`. All three parameters accept text or text lists.

**Sequential replacement**: Each `fromList` item is applied to the result of the
previous replacement.
`@ReplaceSubstring("first"; "first":"second"; "second":"third")`
→ `"third"` ("first"→"second", then "second"→"third").

**Extra fromList items**: Replaced with the last value in `toList`.
**Extra toList items**: Ignored.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | Source string(s) |
| 2 | Text or text list | Substrings to find |
| 3 | Text or text list | Replacement values |

| Example | Result |
|---------|--------|
| `@ReplaceSubstring("I like apples"; "like"; "hate")` | `"I hate apples"` |
| `@ReplaceSubstring("I like apples"; "like":"apples"; "hate":"peaches")` | `"I hate peaches"` |
| `@ReplaceSubstring("first"; "first":"second"; "second":"third")` | `"third"` (sequential) |
| `@ReplaceSubstring(Description; @Newline; " ")` | Carriage returns → spaces |
| `@ReplaceSubstring("a":"b":"c"; "a":"b"; "x":"y")` | `["x", "y", "c"]` (list source) |

Status: ✅ 📋

### @Word  *(Phase 2)*
```
@Word(string; separator; number)
```
Returns the nth word from `string`, delimited by `separator`.
Supports negative indices (`-1` = last word) and list sources.
`0` is equivalent to `1` (first word).

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | The string(s) to scan |
| 2 | Text | Word separator character |
| 3 | Number | Position (1-based; negative = from end) |

| Example | Result |
|---------|--------|
| `@Word("Larson, Collins, and Jensen"; " "; 2)` | `"Collins,"` |
| `@Word("Larson,James,M."; ","; 3)` | `"M."` |
| `@Word("James M. Larson"; " "; -1)` | `"Larson"` (last) |
| `@Word("Hello World"; " "; 0)` | `"Hello"` (0 = first) |
| `@Word("a b c" : "x y z"; " "; 2)` | `["b", "y"]` (list source) |

Status: ✅ 📋

### @Matches  *(Phase 2)*
```
@Matches(string; pattern)
```
Tests a string against a wildcard pattern. Case-insensitive for simple
characters; case-sensitive for `{...}` character classes. List-aware.

**Phase 1 wildcards**: `?` (single char), `*` (any string), `{ABC}` / `{A-F}` (character class).
**Phase 2**: `!` (NOT), `|` (OR), `&` (AND), `+` (one-or-more) — not yet.

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text or text list | String to test |
| 2 | Text or text list | Wildcard pattern |

| Example | Result |
|---------|--------|
| `@Matches("abc"; "a?c")` | `1` (? = any single char) |
| `@Matches("Vermont"; "*mont*")` | `1` (* = any string) |
| `@Matches("AB"; "{A-C}{A-C}")` | `1` (character class) |
| `@Matches("abc"; "ABC")` | `1` (case-insensitive) |
| `@Matches("one":"two":"three"; "three":"four":"five")` | `1` (list: any match) |

Status: ✅ 📋

---

## Type Conversion

### @Text
```
@Text(value)
```
Converts any value to a string (numbers lose decimal if whole).
| Arg | Type | Description |
|-----|------|-------------|
| 1 | Any | Value to convert |

| Example | Result |
|---------|--------|
| `@Text(42)` | `"42"` |
| `@Text(@Created)` | `"11/30/2000 02:39:55 PM"` |

Status: ✅ 📋

### @TextToNumber
```
@TextToNumber(string)
```
Parses a string to a number (returns `0` on parse failure).
| Arg | Type | Description |
|-----|------|-------------|
| 1 | String | Numeric string |

| Example | Result |
|---------|--------|
| `@TextToNumber("42")` | `42` |
| `@TextToNumber("abc")` | `0` |

Status: ✅ 📋

---

## Type Checking

### @IsNumber
```
@IsNumber(value)
```
Returns True if the value is a number or a numeric string.
| Example | Result |
|---------|--------|
| `@IsNumber(42)` | `1` |
| `@IsNumber("hello")` | `0` |

Status: ✅ 📋

### @IsText
```
@IsText(value)
```
Returns True if the value is a string.
| Example | Result |
|---------|--------|
| `@IsText("hello")` | `1` |
| `@IsText(42)` | `0` |

Status: ✅ 📋

### @IsAvailable
```
@IsAvailable(fieldName)
```
Returns True if the named field exists and is non-empty.
| Example | Result |
|---------|--------|
| `@IsAvailable(Subject)` | `1` (if Subject exists) |
| `@IsAvailable(Missing)` | `0` |

Status: ✅ 📋

---

## List Functions

### @Elements
```
@Elements(list)
```
Returns the number of elements in a list. Scalars count as 1.
| Example | Result |
|---------|--------|
| `@Elements("a" : "b" : "c")` | `3` |
| `@Elements("hello")` | `1` |

Status: ✅ 📋

### @IsMember
```
@IsMember(value; list)
```
Returns True if `value` is an element of `list`.
| Example | Result |
|---------|--------|
| `@IsMember("b"; "a" : "b" : "c")` | `1` |
| `@IsMember("x"; "a" : "b" : "c")` | `0` |

Status: ✅ 📋

### @IsNotMember
```
@IsNotMember(value; list)
```
Returns True if `value` is NOT an element of `list`.
| Example | Result |
|---------|--------|
| `@IsNotMember("x"; "a" : "b" : "c")` | `1` |

Status: ✅ 📋

### @Replace  *(Phase 2)*
```
@Replace(sourceList; fromList; toList)
```
Element-level list replacement. If an element in `sourceList` matches an element
in `fromList`, it is replaced by the corresponding element from `toList`.
(Not to be confused with `@ReplaceSubstring` which does string-level replacement.)

| Arg | Type | Description |
|-----|------|-------------|
| 1 | Text list | Source list |
| 2 | Text list | Values to find |
| 3 | Text list | Replacement values |

| Example | Result |
|---------|--------|
| `@Replace("Red":"Orange":"Yellow":"Green"; "Orange":"Blue"; "Black":"Brown")` | `["Red", "Black", "Yellow", "Green"]` |

Status: ✅ 📋

---

## Control Flow

### @If
```
@If(condition; trueValue; falseValue)
```
Evaluates `condition`. If truthy, evaluates and returns `trueValue`; otherwise `falseValue`. Short-circuit.
| Example | Result |
|---------|--------|
| `@If(Salary > 100000; "High"; "Standard")` | `"Standard"` (if Salary = 95000) |
| `@If(1; "yes"; "no")` | `"yes"` |

Status: ✅ 📋

### @Do
```
@Do(expr1; expr2; ...)
```
Evaluates all expressions in order. Returns the value of the **last** expression.
| Example | Result |
|---------|--------|
| `@Do("a"; "b"; "c")` | `"c"` |
| `@Do(sum := 1 + 2; sum * 2)` | `6` |

Status: ✅ 📋

### @Return
```
@Return(value)
```
Immediately stops formula evaluation and returns `value`. Implemented via exception unwinding.
| Example | Result |
|---------|--------|
| `@If(cond; @Return("early"); "late")` | `"early"` (if cond is true) |

Status: ✅ 📋

### @While  *(Phase 2)*
```
@While(condition; body; increment)
```
Repeatedly evaluates `body` and `increment` while `condition` is truthy.
| Example | Result |
|---------|--------|
| `sum := 0; i := 1; @While(i <= 5; @Do(sum := sum + i; i := i + 1); 0); sum` | `15` |

Status: ✅ ⚠

### @Set  *(Phase 2)*
```
@Set("variableName"; value)
```
Assigns a value to a temporary variable by name (string). Returns the value.
| Example | Result |
|---------|--------|
| `@Set("x"; 42); x` | `42` |

Status: ✅ ⚠

### @SetField  *(Phase 2)*
```
@SetField("fieldName"; value)
```
Writes a value to a document field by name (string). Returns the value.
| Example | Result |
|---------|--------|
| `@SetField("Status"; "Done")` | `"Done"` (and writes to document) |

Status: ✅ ⚠

---

## Date / Time

### @Created
```
@Created
```
Returns the document's creation timestamp (from `FormulaContext.resolve("CREATED")`).
Status: ✅ ⚠

### @Now
```
@Now
```
Returns the current date and time as a formatted string.
Status: ✅ ⚠

### @Today
```
@Today
```
Returns the current date as a formatted string.
Status: ✅ ⚠

### @Month  *(Phase 2)*
```
@Month(dateValue)
```
Extracts the month (1–12) from a date string. Supports ISO and US formats.
| Example | Result |
|---------|--------|
| `@Month("2024-01-15")` | `1` |
| `@Month("11/30/2000 02:39:55 PM")` | `11` |

Status: ✅ ⚠

### @Day  *(Phase 2)*
```
@Day(dateValue)
```
Extracts the day of month (1–31).
| Example | Result |
|---------|--------|
| `@Day("2024-01-15")` | `15` |

Status: ✅ ⚠

### @Year  *(Phase 2)*
```
@Year(dateValue)
```
Extracts the 4-digit year.
| Example | Result |
|---------|--------|
| `@Year("2024-01-15")` | `2024` |

Status: ✅ ⚠

### @Adjust  *(Phase 2)*
```
@Adjust(date; years; months; days; hours; minutes; seconds)
```
Adjusts a date by the given increments. Not yet implemented.
Status: ❌

### @TimeZoneToText  *(Phase 2)*
Status: ❌

### @TextToTimeInZone  *(Phase 2)*
Status: ❌

---

## Boolean / Constants

### @True
```
@True
```
Returns `1` (Domino boolean True).
Status: ✅ ⚠

### @False
```
@False
```
Returns `0` (Domino boolean False).
Status: ✅ ⚠

### @All
```
@All
```
Returns `1` (True). Used in `SELECT @All` for view selection.
Status: ✅ ⚠

---

## Security / Context

### @UserName
```
@UserName
```
Returns the current user name (from Session).
| Example | Result |
|---------|--------|
| `@UserName` | `"Alice"` |

Status: ✅ ⚠

### @UserRoles  *(Phase 2)*
Status: ❌

### @Author  *(Phase 2)*
Status: ❌

---

## Side-Effecting Functions

### @DeleteField
```
@DeleteField
```
Must be the RHS of a `FIELD` assignment. Deletes the field from the document.
| Example | Effect |
|---------|--------|
| `FIELD BodyText := @DeleteField` | Removes `BodyText` from document |

Status: ✅ ⚠

### @Command / @PostedCommand
```
@Command([commandName]; arg1; ...)
```
Domino UI commands — **no-ops** in domcouch (return `""`). Matches Domino's `NoExternalApps=1`.
Status: 🚫 No-op

---

## Unimplemented (Phase 2+)

| Function | Priority | Notes |
|----------|----------|-------|
| `@Abstract` | Medium | Rich text → plain text |
| `@Adjust` | Medium | Date arithmetic |
| `@Author` | Low | Author access check |
| `@DbColumn` | High | Cross-database column lookup |
| `@DbCommand` | High | Cross-database command |
| `@DbLookup` | High | Cross-database value lookup |
| `@DbTitle` | Low | Database title |
| `@DialogBox` | Low | UI dialog |
| `@DocChildren` | Medium | Response document query |
| `@DocDescendants` | Medium | All descendant documents |
| `@Environment` | Low | Environment variable |
| `@Explode` | Low | String → list |
| `@Implode` | Low | List → string |
| `@MailSend` | Low | Send mail |
| `@Matches` | Medium | Pattern matching with wildcards |
| `@PickList` | Low | UI pick list |
| `@Prompt` | Low | UI dialog |
| `@Replace` | Medium | List value replacement |
| `@UserRoles` | Low | User roles query |

---


## Summary

| Category | ✅ | 📋 | ⚠ | ❌ | 🚫 |
|----------|----|-----|-----|-----|-----|
| String | 11 | 6 | 5 | 1 | 0 |
| Type Conversion | 2 | 0 | 2 | 0 | 0 |
| Type Checking | 3 | 0 | 3 | 0 | 0 |
| List | 3 | 0 | 3 | 1 | 0 |
| Control Flow | 5 | 0 | 5 | 0 | 0 |
| Date / Time | 6 | 0 | 6 | 3 | 0 |
| Boolean | 3 | 0 | 3 | 0 | 0 |
| Security | 1 | 0 | 1 | 2 | 0 |
| Side-Effecting | 1 | 0 | 1 | 0 | 2 |
| Cross-DB | 0 | 0 | 0 | 3 | 0 |
| **Total** | **35** | **6** | **29** | **10** | **2** |

📋 6 verified against official Domino spec  
⚠ 29 need spec verification
