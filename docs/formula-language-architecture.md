# Formula Language Architecture — domcouch

> **Status**: Design phase — not yet implemented  
> **Last updated**: 2026-05-11

---

## 1. Overview

Domino formulas are a domain-specific language used in two contexts:

| Context               | Example                                      | What we need                                      |
| --------------------- | -------------------------------------------- | ------------------------------------------------- |
| **Selection** (query) | `SELECT Form = 'Person' & Status = 'Active'` | Translate to N1QL WHERE clause → **already done** |
| **Computed value**    | `LastName + ", " + FirstName`                | Evaluate against a document → **new**             |

The existing `FormulaTranslator.toN1ql()` handles selection via regex substitution.
We now add a proper **lexer → parser → evaluator** pipeline for computed evaluation.

---

## 2. Language Grammar

### 2.1 Token Types

| Token              | Pattern / Description                                                      | Examples                                 |
| ------------------ | -------------------------------------------------------------------------- | ---------------------------------------- |
| **VARIABLE**       | `[$A-Za-z][$A-Za-z0-9_]*` (case-insensitive)                               | `D`, `Subject`, `$TITLE`, `LastName`     |
| **CONST_STRING**   | `"..."` — double-quoted, `\"` escape for `"`, `\\` for `\`             | `", "`, `"Update Complete"`              |
| **CONST_BRACE**    | `{...}` — brace-delimited, `\}` escape for `}`, `\\` for `\`               | `{Hello World}`, `{Tab\t here}`          |
| **CONST_NUMBER**   | `[+-]?(\d+\.?\d*|\d*\.?\d+)([eE][+-]?\d+)?`                              | `42`, `-123.4`, `.123`, `123E-2`, `123.` |
| **CONST_DATETIME** | `[...]` — bracket-enclosed time/date, NOT a known keyword                     | `[5:30]`, `[5:30 PM]`, `[6/15]`, `[6/15 5:30 PM]` |
| **KEYWORD**        | `[...]` — known keyword OR reserved word                                      | `[OK]`, `[CANCEL]`, `SELECT`             |
| **OPERATOR**       | `:=` `+` `-` `*` `/` `=` `>` `<` `>=` `<=` `!=` `<>` `&` `\|` `!` `:` `[]` | `:` = list constructor, `[]` = subscript |
| **AT_FUNCTION**  | `@Name`                                                                    | `@Created`, `@All`, `@Trim`              |
| **LPAREN**       | `(`                                                                        |                                          |
| **RPAREN**       | `)`                                                                        |                                          |
| **SEMICOLON**    | `;`                                                                        | Statement AND argument separator         |
| **ASSIGN**       | `:=`                                                                       |                                          |

### 2.2 Reserved Words (Keywords)

`SELECT`, `FIELD`, `DEFAULT`, `ENVIRONMENT`, `REM`, `DURING`, `THEN`, `ELSE`, `END`

Keywords are case-insensitive (`select` = `SELECT`) but must be followed by at least one space.

### 2.3 Statement Forms

```
Statement  := Assignment | Expression | KeywordStatement | Comment
Assignment := (FIELD | DEFAULT | ENVIRONMENT)? VARIABLE ':=' Expression
Expression := Term (Operator Term)*
Term       := VARIABLE | CONST | AT_FUNCTION_CALL | AT_FUNCTION | '(' Expression ')' | Term '[' Expression ']'
Comment    := REM CONST_STRING | REM CONST_BRACE | REM   // REM; is valid (empty comment)

ListLiteral  := Expression ':' ListLiteral | Expression

AT_FUNCTION_CALL := AT_FUNCTION '(' [ Expression (';' Expression)* ] ')'
AT_FUNCTION_0ARY := AT_FUNCTION   // no parens → implicit 0-arg call
KeywordStatement := KEYWORD Expression
```

### 2.4 Operator Precedence (1 = highest, per Domino spec)

| Lvl | Operators                               | Assoc | Notes                                      |
| --- | --------------------------------------- | ----- | ------------------------------------------ |
| 1   | `[]` (subscript)                        | left  | `list[n]` — highest, binds tightest        |
| 2   | `:` (list constructor)                  | right | `"a" : "b" : "c"` → 3-element list              |
| 3   | `+` `-` (unary sign)                    | right | Positive/negative, not addition             |
| 4   | `*` `/` `*/` `/*` (multiplication/div)   | left  | Permuted variants = same semantics          |
| 5   | `+` `-` `*+` `+*` (addition/subtraction) | left  | `+` also concatenation for strings         |
| 6   | `=` `<>` `!=` `><` `>` `<` `>=` `<=`    | left  | Comparison; permuted variants = same        |
| 7   | `!` `&` `\|` (logical)                   | left  | NOT, AND, OR — all same level              |
| 8   | `:=` (assignment)                       | right | Lowest expression precedence; nests         |

**Critical**: `!` is at the **lowest** level (7), not the highest.
`!a & b` parses as `!(a & b)`, not `(!a) & b`. All three logical operators share
level 7 and are left-associative.

**Permuted operators**: Domino supports alternative spellings of the same operator
(e.g., `*=` same as `=`, `*<` same as `<`). Phase 1 implements only the canonical
forms (`=`, `!=`, `<>`, `*`, `/`, `+`, `-`). Permuted variants are Phase 2.

#### Evaluation order

1. **Parentheses**: `(a)` forces inner evaluation first. `(5 - 3) * (6 - 4)` → `2 * 2` → `4`.
2. **Precedence**: Higher-precedence operators evaluate first. `5 - 3 * 6 - 4` → `5 - 18 - 4` → `-17`.
3. **Left-to-right**: Equal-precedence operators evaluate left to right. `8 / 4 * 2` → `2 * 2` → `4`.

Operands are coerced to matching types before evaluation (see §5.1 Type Coercion).

### 2.5 Complete Examples (from spec)

```
D := @Created                                     → Assign(Var("D"), Call("Created", []))
@Trim(Subject)                                    → Call("Trim", [Var("Subject")])
@Created                                         → Call("Created", [])  // no parens = 0-arg
@Prompt([OK]; "Update Complete"; "Your...")        → Call("Prompt", [Kw("OK"), Const("Update Complete"), Const("Your...")])
SELECT @All                                       → KwStmt("SELECT", Call("All", []))
LastName + ", " + FirstName                        → BinOp(+, BinOp(+, Var, Const), Var)
LastName+", "+FirstName                           → same (spaces optional around operators)
FIELD MyField := @Trim(Topic); @UpperCase(Status)  → [Assign(Var("MyField"), Call("Trim", ...)), Call("UpperCase", ...)]
```

### 2.6 Syntax Rules

#### Semicolons separate statements, not lines

```
FIELD X := FirstName + " " + LastName;   @UpperCase(X)
```

Two statements: an assignment AND a function call whose result is the formula value.

#### Case insensitivity

Case is not significant except inside string constants. The Lexer **upper-cases** all
non-string tokens immediately upon recognition:

- `lastname` and `LastName` → same variable `LASTNAME`
- `@trim` and `@Trim` → same function `TRIM`
- `select` and `SELECT` → same keyword `SELECT`
- `"Hello"` keeps exact case

#### Spaces

Optional around operators and punctuation, ignored between tokens. Keywords MUST
have at least one space (or semicolon) separating them from what follows:

```
SELECT @All              ← valid (space after SELECT)
SELECT@All               ← error: keyword not separated
LastName+", "+FirstName  ← valid (no spaces around operators)
```

#### Operators between values

`LastName FirstName` is NOT valid — values must be separated by an operator.
The parser rejects consecutive terms without an operator.

#### Text constants — quotation marks and braces

Two delimiter styles for string literals:

| Style | Example | Escape `"` | Escape `}` | Escape `\` |
|--------|---------|------------|------------|------------|
| Double-quoted | `"Hello World"` | `\"` | (none needed) | `\\` |
| Brace-delimited | `{Hello World}` | (none needed) | `\}` | `\\` |

Brace delimiters (`{...}`) allow embedding quotes without escaping:

```
{"Hello World"}          ← equivalent to   "\"Hello World\""
{He said "Hi" to me}    ← equivalent to   "He said \"Hi\" to me"
```

Numbers in quotes/braces are treated as text: `"42"` is the string `"42"`, not the number 42.

**Lexer rule**: Both forms produce the same token type internally. The Lexer strips
delimiters, unescapes content, and produces a `CONST_STRING` token with the raw
string value. Compiled formulas always normalize to double-quoted form.

**@Repeat**: `@Repeat("X"; 5)` → `"XXXXX"` (Phase 1).

### 2.7 Fields and Variables

#### FIELD keyword — document mutation vs temporary variables

The `FIELD` keyword distinguishes assignments that modify the document from those that
create temporary variables in the evaluation scope:

```
FIELD Subject := "No Subject"     ← writes to the document field "Subject"
Subject := "No Subject"           ← creates temp variable in evaluation scope
x := FirstName + " " + LastName   ← temp variable x (not written to document)
```

Without `FIELD`, the variable exists only for the duration of formula evaluation.

#### DEFAULT keyword — read with fallback

`DEFAULT` provides a fallback value when a field doesn't exist, without writing to
the document:

```
DEFAULT Subject := "No Subject"
```

- If `Subject` field exists and is non-empty → returns the field's value
- If `Subject` doesn't exist or is null → returns `"No Subject"` (the default)
- Unlike `FIELD`, DEFAULT does NOT write to the document

#### ENVIRONMENT keyword — environment variable

`ENVIRONMENT var := value` writes to a session-level or system environment.
Phase 1: treated as no-op (returns the value, does not persist). Phase 2: may write
to a config store.

#### REM — comments

`REM "text"` or `REM {text}` — documentation-only, produces a `Comment` AST node.
The Evaluator skips comment nodes (no value returned). Comments can only appear
as standalone statements, not mid-expression.

#### Null fields

A null/missing field is equivalent to `""` (empty string). Testing for null:

```
FIELD Subject := @If(Subject = ""; "No Subject"; Subject)
```

Non-text fields should avoid `""` — use default formulas or type checks instead.

#### Deleting fields — @DeleteField

`@DeleteField` is a side-effecting function that removes a field from the document.
It must always be the right-hand side of a `FIELD` assignment:

```
FIELD BodyText := @DeleteField
```

The field name comes from the `FIELD` target, not from an argument.

#### Form fields

Special field names available on all documents:

| Field          | Contents                                    |
| -------------- | ------------------------------------------- |
| `Form`         | Form name used to create the document       |
| `$TITLE`       | Form name (when form stored in document)    |
| `$Info`        | Form info (when form stored in document)    |
| `$WindowTitle` | Window title (when form stored in document) |
| `$Body`        | Form body (when form stored in document)    |

These are resolved like any other field by `DocumentContext` — no special handling needed
in the formula engine beyond treating `$` as a valid identifier character:

```
@If(@IsAvailable(Form); Form; $TITLE)
```

### 2.8 Temporary Variables

A temporary variable exists only within a formula. Its scope is that formula.

**Syntax**: `variableName := value` (no `FIELD` keyword)

```
date := @Created;
month := @Text(@Month(date));
n := 1;
```

**Rules**:

- Takes the type of the assigned value (string, number, boolean, list)
- Can be reassigned (`n := n + 1`) — new with Release 6+
- Boolean values: `1` = True, `0` = False (number semantics, not Java boolean)
- String variables: practical limit ~2,048 characters (documented, not enforced)
- Resolved via the evaluation scope's temp-variable map, NOT from document fields

**List construction with `:` operator**:

```
nMonths := "1" : "2" : "3" : "4" : "5" : "6" : "7" : "8" : "9" : "10" : "11" : "12";
months := "January" : "February" : "March" : "April" : "May" : "June"
        : "July" : "August" : "September" : "October" : "November" : "December"
```

The `:` operator creates a list from individual values. It has higher precedence than `+`
but lower than `*` `/`.

**Subscript access `list[n]`**:

```
Categories[n]           ← nth element (1-based index, Domino convention)
```

Subscript is a postfix operator that indexes into a list. `n` is 1-based in Domino.
The result is the single element at that position, or `""` if out of bounds.

**Complete example** — setting a field from temp variables:

```
date := @Created;
month := @Text(@Month(date));
nMonths := "1" : "2" : "3" : "4" : "5" : "6" : "7" : "8" : "9" : "10" : "11" : "12";
months := "January" : "February" : "March" : "April" : "May" : "June"
        : "July" : "August" : "September" : "October" : "November" : "December";
FIELD MonthName := @Replace(month; nMonths; months)
```

AST for `nMonths` list:

```
BinaryOp(:,
  BinaryOp(:, "1", "2"),
  BinaryOp(:, ..., "12"))
```

AST for `Categories[n]`:

```
BinaryOp([], Var("Categories"), Var("n"))
```

### 2.9 Time-Date Constants

Time-date constants are enclosed in brackets `[...]` and parsed at evaluation time.

**Lexer disambiguation**: After consuming `[...]` content:
- If content is a **known keyword** (`OK`, `CANCEL`, `YES`, `NO`, `OKCANCEL`, etc.) → `KEYWORD`
- Otherwise → `CONST_DATETIME` (stored as raw string, parsed at eval time)

**Time formats**:

| Format | Example | Description |
|--------|---------|-------------|
| 24-hour | `[5:30]` | hh:mm, hours 0–23, seconds optional (default 00) |
| 12-hour | `[5:30 PM]` | hh:mm AM/PM, hours 1–12, seconds optional |
| 24-hour full | `[17:30:00]` | hh:mm:ss |

**Date formats** (OS-locale dependent):

| Locale | Format | Example | Result (2002) |
|--------|--------|---------|---------------|
| US | mm/dd[/yy] | `[6/15]` | June 15, 2002 |
| France | dd/mm[/yy] | `[15/6]` | June 15, 2002 |
| Japan | yyyy/mm/dd | `[2002/06/15]` | June 15, 2002 |

Year is optional and defaults to current year. Two-digit years: `yy >= 50` → 1900+yy, `yy < 50` → 2000+yy.

**Time-date combined**: `[time date]` or `[date time]` — e.g., `[6/15 5:30 PM]`, `[5:30 PM 6/15]`.

**Subtraction**: `[5:30 PM] - [5:30]` → numeric difference in seconds (43200).

The Evaluator converts `CONST_DATETIME` to a `DateTime` object at evaluation time.
Date parsing uses the JVM's default locale unless overridden.

**AST**: `ConstDateTime(String raw)` — a new AST leaf node for time-date constants.

**Phase 1 limitation**: US English locale only (`mm/dd[/yy]`). Multi-locale parsing deferred to Phase 2.

**Time zone functions** (Phase 2):
- `@TimeZoneToText(zoneField)` — converts timezone attribute string to human-readable
- `@TextToTimeInZone(timeValue; zoneField)` — formats a time for a specific zone

---

## 3. Architecture

```
                        ┌──────────┐
   formula string ─────▶│  Lexer   │─────▶ List<Token>
                        └──────────┘
                               │
                        ┌──────────┐
             Tokens ───▶│  Parser  │─────▶ Expr (AST)
                        └──────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
              ┌──────────┐         ┌──────────┐
   AST ──────▶│Evaluator │         │toN1ql()  │  (existing regex path)
              └──────────┘         └──────────┘
                    │
              ┌──────────┐
              │  Result  │  (String | Double | DateTime | List)
              └──────────┘
```

### 3.1 Package

```
com.domcouch.formula/
├── Token.java            (type + lexeme + position)
├── Lexer.java            (CharSequence → List<Token>; uppercases variables/keywords/function names)
├── Expr.java             (sealed interface + record subtypes; names in uppercase)
├── Parser.java           (recursive descent: List<Token> → List<Expr>)
├── Evaluator.java        (Expr × FormulaContext → Object)
├── FormulaContext.java   (resolve(VARIABLE) → Object; expects uppercase names)
├── FormulaTranslator.java (existing — moves to this package, gains evaluate())
└── @Functions.java        (registry keyed by uppercase function name)
```

### 3.2 Expression AST (`Expr.java`)

```java
public sealed interface Expr {
    record Variable(String name)                          implements Expr {}
    record StringConst(String value)                      implements Expr {}
    record NumberConst(double value)                      implements Expr {}
    record DateTimeConst(String raw)                     implements Expr {}  // [5:30 PM] etc.
    record KeywordExpr(String value)                      implements Expr {}
    record FunctionCall(String name, List<Expr> args)     implements Expr {}
    record BinaryOp(Expr left, String op, Expr right)     implements Expr {}
    record Assignment(Expr target, Expr value)            implements Expr {}  // temp var := expr
    record FieldAssign(Expr target, Expr value)           implements Expr {}  // FIELD var := expr
    record DefaultAssign(Expr target, Expr value)         implements Expr {}  // DEFAULT var := expr
    record EnvironmentAssign(Expr target, Expr value)     implements Expr {}  // ENVIRONMENT var := expr
    record KeywordStatement(String keyword, Expr body)    implements Expr {}
    record DeleteField(Expr target)                       implements Expr {}  // @DeleteField result
    record Comment(String text)                           implements Expr {}  // REM "..." or REM {...}
}
```

Key distinction: `Assignment` is a temp variable (`x := expr`), `FieldAssign` is a
document mutation (`FIELD x := expr`). Both are <b>expressions</b> that return their value,
so they can nest inside other expressions:

```
city1Upper := @UpperCase(city1 := "London")
```

AST: `Assignment(Var("city1Upper"), Call("UpperCase", [Assignment(Var("city1"), Const("London"))]))`.
The inner `Assignment` returns `"London"` as its value after storing it in the temp scope.

### 3.3 FormulaContext

```java
public interface FormulaContext {
    /** Resolve a variable name to its value, or null if not found. */
    Object resolve(String name);

    /** Write a value to a document field. Used by FIELD assignments. */
    default void setField(String name, Object value) {
        throw new UnsupportedOperationException("setField not supported in this context");
    }

    /** Delete a field from the document. Used by FIELD x := @DeleteField. */
    default void deleteField(String name) {
        throw new UnsupportedOperationException("deleteField not supported in this context");
    }
}
```

Built-in implementations:

- `DocumentContext(Document)` — resolves field names via `doc.getFirstItem(name)`;
  `setField` calls `doc.replaceItemValue(name, value)`; `deleteField` removes the item
- `MapContext(Map<String,Object>)` — temp variables only; throws on `setField`/`deleteField`
- `StackedContext(ctx, override)` — layers temp-vars over a document scope; delegates
  writes to the underlying context
- `SessionContext(Session)` — for `@UserName`, etc.; read-only

---

## 4. @Function Catalog

Functions are registered in a `Map<String, FunctionHandler>`.

```java
@FunctionalInterface
public interface FunctionHandler {
    Object call(Evaluator eval, List<Expr> args, FormulaContext ctx);
}
```

### 4.1 Functions to implement (Phase 1)

| Function                        | Args | Returns  | Notes                        |
| ------------------------------- | ---- | -------- | ---------------------------- |
| `@Trim(String)`                 | 1    | String   | Whitespace trim              |
| `@UpperCase(String)`            | 1    | String   |                              |
| `@LowerCase(String)`            | 1    | String   |                              |
| `@Length(String)`               | 1    | Number   |                              |
| `@Left(String; n)`              | 2    | String   |                              |
| `@Right(String; n)`             | 2    | String   |                              |
| `@Repeat(String; n)`            | 2    | String   | Repeat string n times        |
| `@Contains(String; sub)`        | 2    | Boolean  | `true` if substring found    |
| `@Begins(String; prefix)`       | 2    | Boolean  |                              |
| `@Ends(String; suffix)`         | 2    | Boolean  |                              |
| `@If(cond; trueVal; falseVal)`  | 3    | Any      | Short-circuit evaluation     |
| `@Do(expr1; expr2; ...)`       | var  | Any      | Sequential; returns last     |
| `@Return(value)`               | 1    | —        | Early termination (exception) |
| `@Created`                      | 0    | DateTime | From document metadata       |
| `@Modified`                     | 0    | DateTime | From document metadata       |
| `@Now`                          | 0    | DateTime | Current time                 |
| `@Today`                        | 0    | Date     | Current date                 |
| `@UserName`                     | 0    | String   | From session                 |
| `@Month(date)`                  | 1    | Number   | Month number (1–12)          |
| `@Day(date)`                    | 1    | Number   | Day of month (1–31)          |
| `@Year(date)`                   | 1    | Number   | 4-digit year                 |
| `@IsAvailable(field)`           | 1    | Boolean  | `true` if field exists       |
| `@IsNumber(val)`                | 1    | Boolean  |                              |
| `@IsText(val)`                  | 1    | Boolean  |                              |
| `@Text(val)`                    | 1    | String   | Convert to string            |
| `@TextToNumber(val)`            | 1    | Number   | Parse string to number       |
| `@Elements(list)`               | 1    | Number   | Count of list items          |
| `@IsMember(val; list)`          | 2    | Boolean  |                              |
| `@IsNotMember(val; list)`       | 2    | Boolean  |                              |
| `@Replace(source; from; to)`    | 3    | List     | Replace values in a list     |
| `@All`                          | 0    | true     | Always true (selection only) |
| `@True`                        | 0    | 1        | Boolean true                 |
| `@False`                       | 0    | 0        | Boolean false                |
| `@DeleteField`                  | 0    | —        | Side-effect: deletes field   |
| `@Abstract([opts];max;ellip;f)` | 4    | String   | Rich text → plain text       |

### 4.2 Functions to implement (Phase 2+)

`@ReplaceSubstring`, `@Word`, `@Explode`, `@Implode`, `@Prompt`, `@Set`, `@SetField`,
`@Environment`, `@DocChildren`, `@DocDescendants`, `@Author`, `@UserRoles`,
`@Matches`, `@While`, `@Adjust`

### 4.3 Side-Effecting Functions

Some @functions perform actions beyond computing a return value. The Evaluator's
tree-walk order (left-to-right, depth-first) naturally respects execution order.

| @Function | Side-effect | Phase |
|-----------|-------------|-------|
| `@DeleteField` | Deletes a document field | 1 |
| `@Prompt` / `@PickList` / `@DialogBox` | Displays a dialog box | 2+ |
| `@Command` / `@PostedCommand` | Executes a Notes® command | 2+ |
| `@MailSend` | Creates and routes a mail memo | 2+ |
| `@DbColumn` / `@DbLookup` / `@DbCommand` | Accesses another database | 2+ |

Side-effecting functions return a value like any other function — the side-effect is
an additional action performed during evaluation.

**@Command / @PostedCommand treatment**: These execute Domino UI operations that have
no equivalent in Couchbase. They are **silently ignored** (return `""`), matching Domino's
`NoExternalApps=1` environment setting. This allows formulas containing @Commands to
compute their non-command portions correctly without throwing errors.

---

## 5. Evaluation Rules

### 5.0 Assignment Semantics

The `:=` operator returns its righthand value, making assignments nestable expressions:

```
city1Upper := @UpperCase(city1 := "London")
```

Evaluation:
1. Evaluate `"London"` → `"London"`
2. Store `"London"` in temp scope under `city1`
3. Return `"London"` (the newly assigned value)
4. `@UpperCase("London")` → `"LONDON"`
5. Store `"LONDON"` under `city1Upper`, return it

**FIELD assignment**: Same semantics but calls `ctx.setField(name, value)` instead of
writing to the temp scope. `FIELD` can nest:

```
FIELD CityUpper := @UpperCase(FIELD City := "London")
```

**DEFAULT and ENVIRONMENT**: Cannot be nested — parser rejects these in expression position.

### 5.1 Type Coercion

Domino is loosely typed. Rules:

| Operation           | Rule                                            |
| ------------------- | ----------------------------------------------- |
| `String + anything` | Concatenation (both operands stringified)       |
| `Number + Number`   | Arithmetic addition                             |
| `Number + String`   | String concatenation (number stringified)       |
| `- * /`             | Both operands coerced to Number                 |
| `= != > < >= <=`    | Numeric if both numbers, else string comparison |
| `&` `\|`            | Both operands coerced to Boolean                |
| `!`                 | Operand coerced to Boolean, negated             |

Boolean coercion: `0`, `""`, null → false; everything else → true.

### 5.2 Missing Values

- A variable that resolves to `null` or doesn't exist → `""` (empty string)
- `""` coerced to `0` in numeric context, `false` in boolean context

### 5.3 List Values

When a field is multi-value (e.g., `categories: ["A", "B", "C"]`):

**Pair-wise operators** (`+` `-` `*` `/` `=` `!=` `>` `<` `>=` `<=`):
- Two lists → element-by-element. Shorter list's last element repeats for
  remaining elements of the longer list.
- `1:2:3 + 10:20` → `11:22:23` (last element `2` of second list repeats)
- List + scalar → scalar paired with each element: `1:2:3 + 10` → `11:12:13`
- Pair-wise equality (`=`): only **one matching pair** needed for True.
  `1:2:3 = 2:3:1` → True (pair `2=2` found)
- ⚠ `A=B` and `A!=B` can both be True for lists. Use `!(A=B)` for proper inverse.

**Permutation operators** (`**` `*/` `*+` `*-` `*>` `*<` `*>=` `*<=` `*=` `*!=`):
- Every possible combination of elements: `m × n` results.
- `1:2 *+ 10:20:30` → `11:21:31:12:22:32`
- Scalar with list: scalar against each element.

**Equality semantics (pair-wise)**:

| Statement | Lists | Result | Why |
|-----------|-------|--------|-----|
| `1:2:3 = 3:2:1` | (1,2,3) vs (3,2,1) | True | Pair `2=2` matches |
| `1:2:3 = 4:5:6` | (1,2,3) vs (4,5,6) | False | No pair matches |
| `1:2:3 != 3:2:1` | (1,2,3) vs (3,2,1) | True | Pair `1≠3` doesn't match |
| `!(1:2:3 = 3:2:1)` | (1,2,3) vs (3,2,1) | False | Negation of True |

**Phase 1**: Treat first value only (`getValueString()` / `getValueDouble()`).
No list broadcasting.

**Phase 2**: Full pair-wise and permutation semantics.

### 5.4 Subscript and List Construction

**Subscript `list[n]`**:

- `n` is 1-based (Domino convention): `list[1]` returns first element
- `n` can be a constant, variable, or expression; decimals are **rounded** to integer
- Out of bounds (`< 1` or `> size`) → `NotesException` "Array index out of bounds"
- Scalar values are treated as a 1-element list (subscript `[1]` returns the value)
- Rich text: only `[1]` is valid, returns value unchanged
- **Read-only**: subscript cannot appear on the left side of `:=`.
  `Categories[2] := "New"` is illegal. Rebuild the list instead:
  ```
  FIELD Categories := Categories[1] : "New" : Categories[3]
  ```

**List constructor `a : b : c`**:

- Right-associative: `"a" : "b" : "c"` = `"a" : ("b" : "c")`
- All elements must be the **same type** (text, number, or date-time)
- Elements can be constants, variables, expressions, or nested lists
- Result is a `List<Object>`
- Scalars in lists are individually typed (string, number, boolean as 1/0)
- **Precedence effect**: `:` is level 2, unary `-` is level 3.
  `-3:4` → `-(3:4)` → `(-3, -4)` — unary minus applies to the whole list.
  Use parens: `(-3):4` → `(-3, 4)` — negate only the first element.
- **Element-wise operators**: `+` `-` `*` `/` on two equal-length lists operate element-wise.
  `(1:2:3:4) + (1:2:(-3):4)` → `(2, 4, 0, 8)`

**@Replace(source; fromList; toList)**:

- For each element in `source`, if it matches an element in `fromList`, replace with the
  corresponding element from `toList`
- If not found in `fromList`, keep original value
- Returns a list the same size as `source`

### 5.5 Control Flow

#### @If — conditional

`@If(cond; trueVal; falseVal)` — evaluates `cond`, then evaluates and returns
only the selected branch (short-circuit).

```
@If(Salary > 100000; "High"; "Standard")
@If(cond; @Do(stmt1; stmt2); defaultValue)
```

#### @Do — sequential execution

`@Do(expr1; expr2; ...; exprN)` — evaluates each expression in order, returns
the value of the **last** expression. Useful as an execution path in `@If`.

All expressions execute regardless of side-effects; only the last value is returned.

#### @Return — early termination

`@Return(value)` — immediately stops formula evaluation and returns `value`.
Implemented via a `ReturnValue` internal exception that unwinds the evaluator:

```java
class ReturnValue extends RuntimeException {
    final Object value;
}
```

The Evaluator's top-level `evaluate()` catches `ReturnValue` and extracts the value.

```
@If(cond; @Return("stopped"); "continue")
```

#### @Command evaluation order

Domino defers some @Commands to execute after all other @Functions. Since our engine
treats ALL `@Command` / `@PostedCommand` as no-ops (return `""`, no side-effects),
evaluation order is irrelevant — they can execute inline.

---

## 6. Two-Mode Design

`FormulaTranslator` remains the single entry point but gains a second mode:

```java
public class FormulaTranslator {

    // ---- Query mode (existing, unchanged) ----
    public String toN1ql(String formula) { ... }  // regex-based

    // ---- Computed mode (new) ----
    public Object evaluate(FormulaContext ctx, String formula) {
        List<Token> tokens = lexer.tokenize(formula);
        List<Expr> stmts = parser.parse(tokens);
        // For a value formula, evaluate the last expression
        return evaluator.eval(stmts.getLast(), ctx);
    }
}
```

The Lexer/Parser/Evaluator are created once in the constructor (stateless, reusable).

---

## 7. Error Handling

| Error                    | Exception                                           | Code |
| ------------------------ | --------------------------------------------------- | ---- |
| Unclosed string          | `FormulaParseException`                             | 4501 |
| Unmatched parenthesis    | `FormulaParseException`                             | 4502 |
| Unknown @Function        | `FormulaParseException`                             | 4503 |
| Wrong argument count     | `FormulaParseException`                             | 4504 |
| Type mismatch at runtime | Returns default value, no throw (Domino convention) |

```java
public class FormulaParseException extends NotesException {
    public final int position;  // character offset where error occurred
}
```

---

## 8. Migration

The existing `com.domcouch.impl.FormulaTranslator` moves to `com.domcouch.formula.FormulaTranslator`.
The existing `toN1ql()` method continues to work identically (regex-based). No breaking changes.

```java
// Old
import com.domcouch.impl.FormulaTranslator;

// New
import com.domcouch.formula.FormulaTranslator;
```

---

## 9. Implementation Roadmap

| Step                                           | Deliverable                | Lines (est.) |
| ---------------------------------------------- | -------------------------- | ------------ |
| 1. Token type + Lexer                          | `Token.java`, `Lexer.java` | ~120         |
| 2. AST types                                   | `Expr.java`                | ~40          |
| 3. Parser (recursive descent)                  | `Parser.java`              | ~150         |
| 4. Evaluator (tree walker)                     | `Evaluator.java`           | ~100         |
| 5. FormulaContext + DocumentContext            | `FormulaContext.java`      | ~60          |
| 6. @Function registry (Phase 1 functions)      | `@Functions.java`          | ~200         |
| 7. FormulaTranslator: add `evaluate()`         | Modify existing            | ~20          |
| 8. Integration: computed field API on Document | `CouchbaseDocument`        | ~30          |
| 9. Tests                                       | Unit + integration         | ~200         |
| **Total**                                      |                            | **~920**     |

---

## 10. Design Decisions

| Decision                                   | Rationale                                                                                       |
| ------------------------------------------ | ----------------------------------------------------------------------------------------------- |
| Sealed interface for AST                   | Exhaustive pattern matching; compiler-enforced completeness                                     |
| Recursive descent parser                   | Simple, readable, no dependencies                                                               |
| FormulaContext as `@FunctionalInterface`   | Callers can pass lambda or named implementation                                                 |
| Keep `toN1ql()` regex-based, not AST-based | Selection formulas are structurally simpler; regex avoids parser overhead for query translation |
| Move to `com.domcouch.formula` package     | Clean separation from `api` and `impl`                                                          |
| Two-mode entry through `FormulaTranslator` | Single class for both modes; users don't pick Lexer/Parser directly                             |
