# Notes Formula Language - @Functions Documentation

This document contains comprehensive documentation for all Notes Formula Language @Functions.

Source: HCL Domino Designer 14.5.1 Documentation

---

## @Abs

# @Abs (Formula Language)

Returns the absolute (unsigned) value of a number.

## Syntax

**@Abs(**  *anyNumber*  **)**

## Parameters

*anyNumber*

Number
or number list. Any number valid in Notes/Domino, whether positive
or negative, whole or fractional, integer or real.

## Return value

absoluteValue

Number
or number list. The absolute value of *anyNumber.*

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

When
you use this function as the Input translation formula for a Number
field, you do not have to supply the field with a default value.

You
can enter a field name as the *anyNumber* parameter. If you do,
be sure that the field you reference in an @Abs function:

* Is a number field
* Has a default value of zero

## Examples

1. This example returns 2.16.

   ```
   @Abs(-2.16)
   ```
2. This example returns 2.15 and 2.16 in a list.

   ```
   @Abs(2.15 : (-2.16))
   ```
3. This example returns 2.16 if the number in the field named Net
   is either 2.16 or -2.16.

   ```
   @Abs(Net)
   ```
4. This example returns 25 if Score1 = 50 and Score2 = 75, or if
   Score1 = 75 and Score2 = 50.

   ```
   @Abs(Score1 - Score2)
   ```
5. This formula, for a computed number field called numDays, uses
   @Abs to calculate the number of days between two dates, which are
   stored in time fields dateA and dateB. @Integer(dateA-dateB) returns
   the number of seconds between dateA and dateB, so the formula divides
   by 60\*60\*24 to get days. For example, if dateA is 08/11/95 and dateB
   is 09/22/95, the formula returns: 42.

   ```
   @If( numDays = ""; 0; @Abs( @Integer( dateA - dateB ) / (60 * 60 * 24 ) ) )
   ```

---

## @Abstract

# @Abstract (Formula Language)

Abbreviates the contents of one or more fields by:

* Selecting the most significant words in a body of text
* Abbreviating common words
* Dropping vowels from words
* Removing unnecessary text or characters, such as mail headers
  or white space

Note: This function removes all carriage returns
and linefeeds, regardless of which keywords are selected.

This function
only works with single-byte character sets.

## Syntax

**@Abstract(
[**  *keywords*  **] ;**  *size*  **;**  *beginText*  **;**  *bodyFields* **)**

## Parameters

**[**  *keywords*  **]**

Any
number of keywords that tell Notes/Domino how you want to abbreviate
and sort the text (see list). Keywords are executed in the order in
which you list them. Enclose each keyword in brackets and separate
multiple commands with colons: [DROPVOWELS]:[NOTRIMWHITE]:[ABBREV].

*size*

Number.
The maximum size of the abstracted text. Can be no larger than 64,994
bytes. The number of bytes available for the abstracted text is *size* -
1; one byte is reserved for internal use.

*beginText*

Text.
A comment to insert at the beginning of the returned text, no larger
than 10 characters. The size of *beginText* counts toward the
total *size* of the abstracted text, but its contents are unaffected
by @Abstract commands. Specify an empty string ("") if you do not
want a comment.

*bodyFields*

Text
or text list. Any number of fields containing the text to abstract.
May be text, rich text, or keyword fields. The text within each field
is concatenated with spaces in the order specified. If Notes/Domino
cannot locate a field by name, it uses the string literal instead.
Enclose each field name in quotes and separate multiple names with
colons: "Sales":"Figures".

CAUTION: Rich text fields
are not part of a document until saved. If you want @Abstract to work
on additions and changes to the current document, you must first save
and then recalculate the document. @Abstract cannot convert rich text
to text in a view column.

## Return value

*abstractedText*

Text. The text contained
in each of the body fields, abbreviated and sorted as specified by
the *commands*.

## Keywords

You
can use the following keywords with @Abstract:

**[TEXTONLY]**

Removes
mail headers and punctuation chunks from the text.

**[COUNTWORDS]**

Computes
the significance of each word in the text. A word's significance depends
on the number of times it appears in the text. A word that appears
in the Significant Word file (see "Files") gets its significance boosted.
A word that appears in the Insignificant Word file (see "Files") has
no significance.

**[SAVE]**

Saves the text in its
current state. Saved text can be restored with the [RESTORE] keyword.

**[RESTORE]**

Discards
the current text and restores the last-saved text. You can only restore
saved text one time. If no text has been previously saved, this keyword
has no effect.

**[TRYFIT]**

Takes the current text
and determines if it has become small enough to fit in the specified *size*.
If so, @Abstract returns the current text, with the modifications
applied to this point, and stops, ignoring any remaining commands.
If not, @Abstract continues with the next keyword.

**[SORTCHUNKS]**

Sorts
the text according to significance. The text is divided into chunks,
of which there are three types: text, mail header, and punctuation.

* Text chunks are usually sentences. They may be at the beginning,
  end, or middle of a paragraph.
* Mail header chunks are created according to the contents of the
  Mail Headers file (see "Files").
* Punctuation chunks consist of any text with no letters or digits.

The significance of a chunk depends upon the significances
of the words within it, the number of words in the chunk, and the
type and position of the chunk. To use [SORTCHUNKS], you must also
use [COUNTWORDS].

**[ABBREV]**

Abbreviates the
text. @Abstract uses an Abbreviation Dictionary to substitute abbreviations
for words in the text (see "Files"). You can control other aspects
of the abbreviation process with the following commands (which have
no effect unless followed by the [ABBREV] keyword):

**[USEDICT]**

Specifies
that the Abbreviation Dictionary should be used. This is the default.

**[NODICT]**

Specifies
that the Abbreviation Dictionary should not be used.

**[KEEPVOWELS]**

Keeps
vowels in words. This is the default.

**[DROPVOWELS]**

Removes
vowels from words. The first vowel in a word that begins with a vowel
isn't affected. If you use [DROPVOWELS], you can optionally use one
of the following subcommands.

**[DROPFIRSTVOWEL]**

Drops
vowels from the beginning of words.

**[KEEPFIRSTVOWEL]**

Keeps
vowels at the beginning of words. This is the default.

**[TRIMWHITE]**

Removes
extra white space characters from the text. This is the default.

**[NOTRIMWHITE]**

Retains
extra white space characters in the text.

**[TRIMPUNCT]**

Removes
extra white space characters surrounding punctuation.

**[NOTRIMPUNCT]**

Retains
extra white space characters surrounding punctuation.

**[NOSTOPLIST]**

Disables
the insignificant word list (notestop.txt)

**[NOSIGLIST]**

Disables
the significant word list (notesigl.txt).

## Rules

There
are three built-in programs you can use with @Abstract.

**[RULE1]** consists
of the following commands, executed in this order:

**[TEXTONLY]:[TRYFIT]**

Removes
all mail header and punctuation chunks. If the text fits, the function
is done; otherwise, continues.

**[TRIMPUNCT]:**

Trims
white space around punctuation marks.

**[SAVE]:**

Saves
the current state of the text.

**[ABBREV]:[TRYFIT]:**

Abbreviates
the text. If the text fits, the function is done; otherwise, continues.

**[RESTORE]:**

Restores
the state of the text to what it was prior to abbreviating.

**[SAVE]:**

Saves
the current state of the text.

**[DROPVOWELS]:[ABBREV]:[TRYFIT]:**

Abbreviates
text by dropping vowels. If the text fits, the function is done; otherwise,
continues.

**[RESTORE]:**

Restores the state of
the text to what it was prior to abbreviating.

**[COUNTWORDS]:[SORTCHUNKS]:[ABBREV]**

Counts
words and sorts the chunks. Abbreviates the text and returns it.

If
the removal of mail headers and punctuation allowed the text to fit
into the desired size, then text is returned as is. If the first abbreviation
was enough to make the text fit, the returned text begins with a minus
character ( - ). If the second abbreviation was enough to make the
text fit, the returned text begins with a plus character ( + ). If
the function counted the words and sorted the chunks, the text will
start with an asterisk ( \* ) and the sentences will be separated with
a ( | ) to indicate that they were rearranged.

**[RULE2]** issues
the following commands:

**[TRIMPUNCT]:[ABBREV]**

**[RULE3]** issues
the following commands:

**[TEXTONLY]:[TRYFIT]:**

Removes
all mail header and punctuation chunks. If the text fits, the function
is done; otherwise, continue.

**[TRIMPUNCT]:**

Trims
white space around punctuation marks.

**[SAVE]:**

Saves
the current state of the text.

**[ABBREV]:[TRYFIT]:**

Abbreviates
the text. If the text fits, the function is done; otherwise, continue.

**[RESTORE]:**

Restores
the state of the text to what it was prior to abbreviating.

**[DROPVOWELS]:**

Abbreviates
text by dropping vowels.

**[SAVE]:**

Saves the
current state of the text.

**[ABBREV]:[TRYFIT]:**

If
the text fits, the function is done; otherwise, continue.

**[RESTORE]:**

Restores
the state of the text to what it was prior to abbreviating.

**[COUNTWORDS]:[SORTCHUNKS]:[ABBREV]**

Counts
words and sorts the chunks. Abbreviates the text and returns it.

If
the function counted the words and sorted the chunks, the returned
text begins with an asterisk ( \* ) and the sentences are separated
with a ( | ) to indicate that they were rearranged.

## Files

The
following files are used by @Abstract. You can create all, some, or
none of these text files, depending on how you want to use @Abstract.
Any files you do create must be named as specified and be present
in your Notes/Data file path when you start running the software.

Abbreviation Dictionary (noteabbr.txt)

Each
line of the file should contain two words: the first is the original
word and the second is its abbreviation. An abbreviation must be shorter
than the word it replaces. For example:

```
telephone ph
number no
```

Capitalization works as follows:

* If the abbreviation is specified in uppercase letters, then it
  always appears in uppercase letters.
* If the original word appears in lowercase letters, the abbreviation
  appears as specified in the abbreviation dictionary.
* If the original word appears in uppercase letters or in a mixture
  of uppercase and lowercase letters, the abbreviation appears in uppercase
  letters.
* A lowercase first letter in the abbreviation will be converted
  to uppercase if needed to match the first letter in the original word.
* The remaining letters in the abbreviation will be converted to
  uppercase if needed to match the case of the original word's second
  letter.

The abbreviation is never converted to lowercase, but it
may be converted to uppercase.

| Specified abbreviation | Word being replaced | Resulting abbreviation | Reasoning |
| --- | --- | --- | --- |
| Phone | telephone | Phone | The original word appears in lowercase, so the specified abbreviation's case is used. |
| Phone | TElephone | PHONE | The abbreviation's case is based upon the original word's case. |
| Phone | Telephone | Phone | The abbreviation's case is based upon the original word's case. |
| PHONE | Telephone | PHONE | The abbreviation is specified as uppercase, so it always appears as uppercase. |
| Phone | tElephone | PHONE | The first letter of the abbreviation was already uppercase, so Notes/Domino leaves it alone. The remaining letters of the abbreviation are converted to uppercase to match the second letter of the original word. |

Significant Words (notesigl.txt)

The file should
be a free-form list of significant words, such as "urgent" or "immediately."
When @Abstract computes the significance of text, it boosts the significance
of any words included in the significant word list. For example:

```
client


boss


chocolate
```

Insignificant Words (notestop.txt)

The file
should be a free-form list of words that are always insignificant,
such as "the," "and," and "of." When @Abstract computes the significance
of words, it ignores any words included in this file. For example:

```
the


and


of
```

Mail Headers (notehead.txt)

The file should
be a free-form list of words that indicate mail headers, such as Subject,
From, and To. In order for @Abstract to consider a chunk a mail header,
it must begin with one of the words specified in this file and be
followed immediately by a colon and a space. If you want a mail header
to be considered significant, place an asterisk after the word. For
example:

```
Subject*


From
```

## Examples

1. This formula abbreviates the contents of the description field
   by eliminating vowels.

   ```
   @Abstract( [DROPVOWELS]:[ABBREV]; 200; ""; "description" )
   ```

   If
   the description field contained this text: The kickoff meeting for
   our capital campaign is tomorrow.

   Then the formula returns:
   Th kckff mtng fr r cptl cmpgn is tmrrw.
2. This formula abbreviates the contents of the description field
   by using an Abbreviation Dictionary and eliminating vowels, including
   the vowels that appear as the first letter in a word.

   ```
   @Abstract([USEDICT]:[DROPVOWELS]:[DROPFIRSTVOWEL]:[ABBREV]; 200; ""; "description" )
   ```

   If
   the Abbreviation Dictionary contains the following:

   capital
   cap meeting mtg tomorrow tom

   Then the formula returns: Th
   kckff mtg fr r cap cmpgn s tom**.**
3. This formula shows a misunderstanding in the use of @Abstract.
   It returns the contents of the description field unaltered, since
   the [ABBREV] keyword incorrectly precedes [DROPVOWELS].

   ```
   @Abstract( [ABBREV]:[DROPVOWELS]; 200; ""; "description" )
   ```
4. This formula removes the white spaces from around all punctuation
   and abbreviates the text in the "opinion" field according to the noteabbr.txt
   file, which contains the following:

   following flwg punctuation punc

   ```
   @Abstract([RULE2];300;"Result:";"opinion")
   ```

   If
   the opinion field contains the text: The FOllowing is a list of punctuation
   marks: ! , ; :.

   Then the formula returns: Result:The FLWG is
   a list of punc marks:!<;:.

---

## @AbstractSimple

# @AbstractSimple (Formula Language)

Creates a short abstract of a text or rich text field.
Simpler and more efficient than using @Abstract.

Note: This @function is new with Release 8.

## Syntax

**@AbstractSimple(**  *bodyFields*  **)**

## Parameters

*bodyFields*

Text
or text list. Any number of fields containing the text to abstract.
May be text or rich text fields. If Notes/Domino cannot locate a field
by name, it uses the string literal instead. Enclose each field name
in quotes and separate multiple names with colons: "Sales":"Figures".

CAUTION: Rich text fields are not part of a document until saved.
If you want @AbstractSimple to work on additions and changes to the
current document, you must first save and then recalculate the document.

## Return value

*abstractedText*

Text or Text List.
Returns the first 100 characters or first 2 paragraphs of text, whichever
is smaller. All newline and tab characters are converted into spaces,
and all empty paragraphs (containing only a newline character) are
ignored. If the parameter is a single field, a text value is returned.
If the parameter is a list of field names, then a text list is returned
with each list element containing the abstracted text of the corresponding
field in the parameter list.

If a field parameter is of an
invalid type, or can't be found, the text returned is the string of
the field parameter.

Note: If the behavior of @Abstract is desired, where the result
is a single, space-separated, string of abstracted results, apply
@Implode to the result of @AbstractSimple

## Examples

1. If the field "Verse" contains the rich text:

   ```
   One bright day
   in the middle of
   the night
   ```

   @AbstractSimple("Verse") returns:

   ```
   One bright day in the middle of
   ```
2. If the field "Verse" contains the rich text:

   ```
   One bright day


   in the middle of
   ```

   @AbstractSimple("Verse") returns:

   ```
   One bright day in the middle of
   ```
3. If the following fields are on a document:

   | **Field Name** | **Type** | **Value** |
   | --- | --- | --- |
   | PersonName | Text | ``` Sam Smith ``` |
   | Address | Text | ``` 123 Shady Lane ``` |
   | CityState | Text | ``` Anytown, USA ``` |
   | ZIP | Numeric | ``` 12345.00 ``` |

   @AbstractSimple("PersonName" : "Address" : "CityState"
   : "ZIP") returns:

   ```
   Sam Smith : 123 Shady Lane : Anytown, USA : ZIP
   ```

   Note: The last element is "ZIP" because the field is numeric
   which is invalid for @AbstractSimple
4. If the field "Critics" contains the text:

   ```
   When asked to comment on the movie, the reviewer stated that it was one of the year's best, and certainly would find a place on many award lists.
   ```

   @AbstractSimple("Critics")
   returns:

   ```
   When asked to comment on the movie, the reviewer stated that it was one of the year's best, and cert
   ```

---

## @Accessed

# @Accessed (Formula Language)

Indicates the time and date when the document was last
accessed by a NotesÂ® client,
whether for reading or editing.

## Syntax

**@Accessed**

## Return value

*lastAccessed*

Time-date. The time
and date that the current document was last accessed.

## Usage

@Accessed
is most useful in field formulas, selection formulas, agents, and
actions. Because @Accessed requires some time to compute, it should
not be used in applications where efficiency is critical.

The
value returned by @Accessed is exact only to the day, not the hour.
If the document is edited, the property is always updated. If the
document is read more than once during the same 24-hour period, the
value is only updated the first time accessed.

The last-accessed
value is not replicated; each replica copy of the document maintains
its own value. The value returned by @Accessed represents the last
time the document was accessed in that replica of the database.

If
the database is stored on CD-ROM, @Accessed has no meaning because
read/write access is not controlled by the Notes/Domino editor.

## Usage in workflow applications

This function is useful for determining
whether a document has been "stalled" in a workflow application; for
example, you can run an agent that checks the last-accessed date on
a series of documents and sends out reminders about documents that
should have been read but have not.

@Accessed can also be used
in an agent to determine which documents in a database have not been
accessed within a certain period of time, and archive them.

Note: @Accessed is similar to @Modified, which records the date
the document was last edited and saved.

## Usage in column or selection formulas

Be careful when using @Accessed
in views (in column or selection formulas) because it forces the view
to be refreshed every time it's opened. You can prevent this by selecting
the Manual/Background option for the view refresh frequency. Using
@Accessed in a view will also cause that view to perpetually appear
to need refreshing -- the refresh mark will always display.

## Examples

This formula returns: 06/22/95 10:46:03
AM; if the document was last read or edited on June 22, 1995 at 10:46:03
AM.

```
@Accessed
```

---

## @ACos

# @ACos (Formula Language)

Calculates the arc (inverse) cosine, using the cosine of
an angle.

## Syntax

**@ACos(**  *cosine*  **)**

## Parameters

*cosine*

Number
or number list. A cosine of an angle, from -1 through 1.

## Return value

*angle*

Number
or number list. An angle, in radians, from 0 through pi. This represents
an angle between 0 and 180 degrees.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns pi/2.

   ```
   @ACos( 0 )
   ```
2. This example returns 1.0472 radians (60 degrees).

   ```
   @ACos( 0.5 )
   ```
3. This example returns 1.0472 radians and pi/2 in a list.

   ```
   @ACos( 0 : 0.5 )
   ```

---

## @AddToFolder

# @AddToFolder (Formula Language)

Adds current document to one folder while removing it from
another. NULL string can be substituted for either argument to skip
the action.

Note: This @function is new with Release 5.

## Syntax

@AddToFolder(*foldernameadd* ; *foldernameremove*)

## Parameters

*foldernameadd*

Text.
Name of the folder the document will be added to.

*foldernameremove*

Text.
Name of the folder the document will be removed from.

## Usage

This
formula can be used in toolbar button and agent formulas.

@Command([Folder];
Foldername; MoveOrCopy) works just like @AddToFolder except it moves
a document from the current folder.

## Examples

1. This example adds the currently selected document to the folder
   named Work.

   ```
   @AddToFolder("Work";"")
   ```
2. This example adds the currently selected document to the folder
   named Work and removes it from the folder named Favorites.

   ```
   @AddToFolder("Work";"Favorites")
   ```

---

## @Adjust

# @Adjust (Formula Language)

Adjusts the specified time-date value by the number of
years, months, days, hours, minutes, and/or seconds you specify. The
amount of adjustment may be positive or negative.

## Syntax

**@Adjust(**  *dateToAdjust*  **;**  *years*  **;**  *months*  **;**  *days*  **;**  *hours*  **;**  *minutes*  **;**  *seconds*  **;
[DST]**  **)**

## Parameters

*dateToAdjust*

Time-date
or time-date list. The time-date value you want to increment. This
should be a single date, not a range.

*year*s

Number.
The number of years to increment by.

*month*s

Number.
The number of months to increment by.

*day*s

Number.
The number of days to increment by.

*hour*s

Number.
The number of hours to increment by.

*minute*s

Number.
The number of minutes to increment by.

*second*s

Number.
The number of seconds to increment by.

**[DST]**

Keyword.
Optional. Specify **[INLOCALTIME]** to further adjust the time
for daylight-saving time if the adjustment crosses the boundary and
daylight-saving time is in effect. Specify **[INGMT]** or omit
this parameter to not further adjust the time for daylight-saving
time. The adjustment is such that adding or subtracting in day increments
yields the same time in the new day.

## Return value

*adjustedDate*

Time-date. The date,
incremented by the amount of time you have specified.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

You must include all arguments except the [DST] keyword;
include a zero (0) for parameters you don't want to adjust.

The
arguments are applied from last to first. For instance, @Adjust([2/2/2006];
0; 2; 28; 0; 0; 0) returns [5/2/2006], not [4/30/2006] as you might
expect. This is because @Adjust first adds 28 days, making [3/2/2006],
then adds two months, making [5/2/2006]. To first add two months,
then add 28 days, use @Adjust twice, for instance: @Adjust(@Adjust([02/02/2006];
0; 2; 0; 0; 0; 0); 0; 0; 28; 0; 0; 0)

Tip: To find the difference between two dates, subtract them.
The result is returned in seconds. To adjust the result to days, divide
the result by 86,400 - which is the number of seconds in a day. For
example, if you have two date fields, date1, which contains [07/01/01]
and date2, which contains [07/05/01], use the following formula to
return the number of days between the two dates:

```
(date2-date1)/86400
```

This
code returns 4.

## Calculating due dates

A typical use for @Adjust is calculating a due
date from an entry date, by adjusting only one component of the time-date
value, for example, the month component.

## Examples

1. This example returns 09/2/97.

   ```
   @Adjust([06/30/95];2;2;2;0;0;0)
   ```

   Notes/Domino
   sees 30 in the days portion of the time-date value and adjusts it
   by 2, which increments the month value by 1. Notes/Domino then adjusts
   the month value by 2, and the year value by 2.
2. This example returns 03/20/94.

   ```
   @Adjust([03/30/96];-2;0;-10;0;0;0)
   ```

   Notes/Domino
   returns a date that is 2 years and 10 days before the supplied date.
3. This example returns 09/1/97 and 09/2/97.

   ```
   @Adjust([06/29/95] : [06/30/95]; 2; 2; 2; 0; 0; 0)
   ```
4. This example returns the date one month from the date in the field
   named Date.

   ```
   @Adjust(Date;0;1;0;0;0;0)
   ```
5. This example returns the date one month and one day from the current
   time-date.

   ```
   @Adjust(@Now;0;1;1;0;0;0)
   ```
6. Given a date, this formula calculates the beginning of the week.
   It takes the date stored in the dueDate field, and returns the date
   representing the previous Monday. For example, if dueDate is 06/02/95,
   this formula returns 05/29/95.

   ```
   @Adjust( dueDate; 0; 0; - ( @Weekday( dueDate ) - 2 ); 0; 0; 0 )
   ```
7. This example returns 5/2/2006.

   ```
   @Adjust([2/2/2006]; 0; 2; 28; 0; 0; 0)
   ```
8. This example returns 4/30/2006.

   ```
   @Adjust(@Adjust([02/02/2006]; 0; 2; 0; 0; 0; 0); 0; 0; 28; 0; 0; 0)
   ```

---

## @AdminECLIsLocked

# @AdminECLIsLocked (Formula Language)

Note: This @function is new with Release 7.

Checks the current status of the Administration ECL in the name and address book and returns 1 (True) if the Administration ECL is locked and editing is prevented; otherwise returns 0 (False).

## Syntax

**@AdminECLIsLocked**

## Return value

*flag*

Boolean

* 1 (True) indicates that the Administration ECL is locked and may not be edited
* 0 (False) indicates that the Administration ECL is not locked

## Usage

You cannot use this function in Web applications. In Release 7, this function will be used in pubnames.ntf to determine if AdminECL is locked or not.

---

## @All

# @All (Formula Language)

Returns the value True.

## Syntax

**@All**

## Return value

*flag*

Number.
The number 1 (True).

## Usage

Use
@All in selection formulas, mail agents, paste agents, scheduled agents,
or in any formula requiring a SELECT statement. Notes/Domino appends
SELECT @All to agents in contexts where @All is needed. All views
default to a selection formula of SELECT @All.

## Examples

1. This example selects all documents in the database when used as
   a view selection formula.

   ```
   SELECT @All
   ```
2. This formula, when used in a mail or paste agent, selects all
   documents and sets the Status field to "Open."

   ```
   FIELD Status:="Open";SELECT@All
   ```

---

## @AllChildren

# @AllChildren (Formula Language)

Includes all response documents at all levels for parent
documents that match selection criteria.

## Syntax

**SELECT**  *selectionFormula*  **I
@AllChildren**

## Return value

Selects all the documents that match *selectionFormula* plus
their immediate responses.

## Usage

@AllChildren
can only be used in a view selection or selective replication formula.
It must be appended to the end of a selection formula using the Boolean
OR operator ("|"). Don't use it within complex expressions in a formula.

@AllChildren
allows you to define a view as a set of documents that match a given
formula plus the immediate responses to those documents. It also allows
you to create a selective replication formula to replicate a set of
documents along with the immediate responses.

Selection formulas
that use @AllChildren may provide a significant advantage over formulas
that use @IsResponseDoc. While @IsResponseDoc returns True for anyresponse
document in a database, @AllChildren returns only those responses
that are immediate children of matching documents.

## Examples

1. A response hierarchy contains the following documents.

   1.0 What
   is your favorite color? (Esteban Garcia) 1.1 Blue (Mary Lu)
   1.2 Aqua (Jim Thompson) 1.2.1 Why do you like aqua?
   (Mary Lu) 1.2.2 It reminds me of the ocean (Jim Thompson)
   1.3 I like the color orange (Bill Jones)

   The first SELECT
   statement selects documents 1.2, 1.2.1, and 1.2.2; the second selects
   documents 1.0, 1.1, 1.2, and 1.3; the third selects documents 1.0,
   1.1, 1.2, 1.2.1, 1.2.2, and 1.3; and the fourth selects documents
   1.2.1 and 1.3.

   ```
   SELECT @Author = "Jim Thompson" | @AllChildren
   SELECT @Author = "Esteban Garcia" | @AllChildren
   SELECT @Author = "Esteban Garcia" | @AllDescendants
   SELECT @Contains( Subject; "like" ) | @AllChildren
   ```
2. You have a Flowers discussion database and you want to add a new
   view that will show only those documents having to do with orchids.
   You create an Orchid view, use the View InfoBox to indicate that it
   should show documents in a response hierarchy, and write the following
   selection formula for the view:

   ```
   SELECT @Contains( Subject; "orchid" ) | @IsResponseDoc
   ```

   You
   get this view:

   **Date Topic** 04/08/95 The orchid
   family of flowers (Anne Davis, 2 responses) 04/08/95 Sighting
   of new variation (Brad Sullivan) 04/08/95 The "ghost" orchid
   (Rachel Greenbaum) 04/08/95 Local flower shops that carry orchids
   (Mary Tsen, 1 response) 04/08/95 Try the Blumenhaus (Anne
   Davis)

   The view, however, is selecting *every* response
   document in the entire database, whether or not it has to do with
   orchids. For example, here's what the view looks like when the response
   hierarchy is turned off:

   **Date Topic** 04/08/95
   The orchid family of flowers (AnneDavis) 04/08/95 Sighting of
   new variation (Brad Sullivan) 04/08/95 Special varieties of roses
   (Michael Bowling) 04/08/95 My roses bloomed late this year (Marcel
   DuBois) 04/08/95 Local flower shops that carry orchids (Mary Tsen)
   04/08/95 Try the Blumenhaus (Anne Davis) 04/08/95 The "ghost"
   orchid (Rachel Greenbaum)

   The unneeded documents take up valuable
   space in the view index on the database server. (In addition, if you
   used this same formula for replication, the unneeded documents would
   be replicated).

   You use @AllChildren to rewrite the selection
   formula:

   ```
   SELECT @Contains( Subject; "orchid" ) | @AllChildren
   ```

   This
   formula selects and displays *only* those response documents
   whose parent contains "orchid" in the Subject field. The view does
   not contain any hidden response documents.

   **Date
   Topic** 04/08/95 The orchid family of flowers (Anne Davis, 2
   responses) 04/08/95 Sighting of new variation (Brad Sullivan)
   04/08/95 The "ghost" orchid (Rachel Greenbaum) 04/08/95 Local
   flower shops that carry orchids (Mary Tsen, 1 response) 04/08/95
   Try the Blumenhaus (Anne Davis)
3. Just as you'd hoped, the orchids generate a lively discussion.
   The Main View of the database, which selects all documents, now looks
   like this:

   **Date Topic** 04/08/95 The orchid family
   of flowers (Anne Davis, 7 responses) 04/08/95 Sighting of new
   variation (Brad Sullivan, 2 responses) 04/08/95 What color?
   (Anne Davis) 04/08/95 Please post exact location (Mary Tsen)
   04/08/95 The "ghost" orchid (Rachel Greenbaum, 3 responses)
   04/08/95 Very difficult to see (Brad Sullivan, 1 response)
   04/08/95 Only blooms for an hour or so! (Rachel Greenbaum)
   04/08/95 Some sightings reported in Florida (AnneDavis) 04/08/95
   Roses beginning to bloom (Peter Donovan, 2 responses) 04/08/95
   Special varieties of roses (Michael Bowling) 04/08/95 My
   roses bloomed late this year (Marcel DuBois) 04/08/95 Local flower
   shops that carry orchids (Mary Tsen, 1 response) 04/08/95 Try
   the Blumenhaus (Anne Davis) 04/08/95 Tulip trips to Holland (Mary
   Tsen)

   The Orchid view you just created, however, does not contain
   all the documents you want. @AllChildren only selects the immediate
   children of any parent document(s) that meet the selection criteria:

   **Date
   Topic** 04/08/95 The orchid family of flowers (Anne Davis,
   4 responses) 04/08/95 Sighting of new variation (Brad Sullivan)
   04/08/95 The "ghost" orchid (Rachel Greenbaum, 2 responses)
   04/08/95 Very difficult to see (Brad Sullivan) 04/08/95
   Some sightings reported in Florida (Anne Davis) 04/08/95
   Local flower shops that carry orchids (Mary Tsen, 1 response) 04/08/95
   Try the Blumenhaus (Anne Davis)

   In this case, @AllDescendants
   might provide a better solution. You rewrite the selection formula:

   ```
   SELECT @Contains( Subject; "orchid" ) | @AllDescendants
   ```

   The
   Orchid view now contains entire threads of the orchid discussion:

   **Date
   Topic** 04/08/95 The orchid family of flowers (Anne Davis,
   7 responses) 04/08/95 Sighting of new variation (Brad Sullivan,
   2 responses) 04/08/95 What color? (Anne Davis) 04/08/95
   Please post exact location (Mary Tsen) 04/08/95 The "ghost"
   orchid (Rachel Greenbaum, 3 responses) 04/08/95 Very difficult
   to see (Brad Sullivan, 1 response) 04/08/95 Only blooms
   for an hour or so! (Rachel Greenbaum) 04/08/95 Some sightings
   reported in Florida (Anne Davis) 04/08/95 Local flower shops that
   carry orchids (Mary Tsen, 1 response) 04/08/95 Try the Blumenhaus
   (Anne Davis)

---

## @AllDescendants

# @AllDescendants (Formula Language)

Includes all response and response-to-response documents
for parents that match selection criteria.

## Syntax

**SELECT**  *selectionFormula*  **I
@AllDescendants**

## Return value

Selects all the documents that match *selectionFormula* plus
their responses and responses-to-responses, for as many levels of
response documents as exist.

## Usage

@AllDescendants
can only be used in a view selection or selective replication formula.
It must be appended to the end of a selection formula using the Boolean
OR operator ("|"). Don't use it within complex expressions in a formula.

@AllDescendants
allows you to define a view as a set of documents that match a given
formula plus all the responses to those documents, at any level. It
also allows you to create a selective replication formula to replicate
a set of documents along with all responses.

Selection formulas
that use @AllDescendants may provide a significant advantage to formulas
that use @IsResponseDoc. While @IsResponseDoc returns True for any
response document in a database, @AllDescendants returns only those
responses that are descendants of matching documents.

## Examples

1. A response hierarchy contains the following documents.

   1.0 What
   is your favorite color? (Esteban Garcia) 1.1 Blue (Mary Lu)
   1.2 Aqua (Jim Thompson) 1.2.1 Why do you like aqua?
   (Mary Lu) 1.2.2 It reminds me of the ocean (Jim Thompson)
   1.3 I like the color orange (Bill Jones)

   The first SELECT
   statement selects documents 1.2, 1.2.1, and 1.2.2; the second selects
   documents 1.0, 1.1, 1.2, and 1.3; the third selects documents 1.0,
   1.1, 1.2, 1.2.1, 1.2.2, and 1.3; and the fourth selects documents
   1.2.1 and 1.3.

   ```
   SELECT @Author = "Jim Thompson" | @AllChildren
   SELECT @Author = "Esteban Garcia" | @AllChildren
   SELECT @Author = "Esteban Garcia" | @AllDescendants
   SELECT @Contains( Subject; "like" ) | @AllChildren
   ```
2. You have a Flowers discussion database and you want to add a new
   view that will show only those documents having to do with orchids.
   You create an Orchid view, use the View InfoBox to indicate that it
   should show documents in a response hierarchy, and write the following
   selection formula for the view:

   ```
   SELECT @Contains( Subject; "orchid" ) | @IsResponseDoc
   ```

   You
   get this view:

   **Date Topic** 04/08/95 The orchid
   family of flowers (Anne Davis, 2 responses) 04/08/95 Sighting
   of new variation (Brad Sullivan) 04/08/95 The "ghost" orchid
   (Rachel Greenbaum) 04/08/95 Local flower shops that carry orchids
   (Mary Tsen, 1 response) 04/08/95 Try the Blumenhaus (Anne
   Davis)

   The view, however, is selecting *every* response
   document in the entire database, whether or not it has to do with
   orchids. For example, here's what the view looks like when the response
   hierarchy is turned off:

   **Date Topic** 04/08/95
   The orchid family of flowers (AnneDavis) 04/08/95 Sighting of
   new variation (Brad Sullivan) 04/08/95 Special varieties of roses
   (Michael Bowling) 04/08/95 My roses bloomed late this year (Marcel
   DuBois) 04/08/95 Local flower shops that carry orchids (Mary Tsen)
   04/08/95 Try the Blumenhaus (Anne Davis) 04/08/95 The "ghost"
   orchid (Rachel Greenbaum)

   The unneeded documents take up valuable
   space in the view index on the database server. (In addition, if you
   used this same formula for replication, the unneeded documents would
   be replicated).

   You use @AllChildren to rewrite the selection
   formula:

   ```
   SELECT @Contains( Subject; "orchid" ) | @AllChildren
   ```

   This
   formula selects and displays *only* those response documents
   whose parent contains "orchid" in the Subject field. The view does
   not contain any hidden response documents.

   **Date
   Topic** 04/08/95 The orchid family of flowers (Anne Davis, 2
   responses) 04/08/95 Sighting of new variation (Brad Sullivan)
   04/08/95 The "ghost" orchid (Rachel Greenbaum) 04/08/95 Local
   flower shops that carry orchids (Mary Tsen, 1 response) 04/08/95
   Try the Blumenhaus (Anne Davis)
3. Just as you'd hoped, the orchids generate a lively discussion.
   The Main View of the database, which selects all documents, now looks
   like this:

   **Date Topic** 04/08/95 The orchid family
   of flowers (Anne Davis, 7 responses) 04/08/95 Sighting of new
   variation (Brad Sullivan, 2 responses) 04/08/95 What color?
   (Anne Davis) 04/08/95 Please post exact location (Mary Tsen)
   04/08/95 The "ghost" orchid (Rachel Greenbaum, 3 responses)
   04/08/95 Very difficult to see (Brad Sullivan, 1 response)
   04/08/95 Only blooms for an hour or so! (Rachel Greenbaum)
   04/08/95 Some sightings reported in Florida (AnneDavis) 04/08/95
   Roses beginning to bloom (Peter Donovan, 2 responses) 04/08/95
   Special varieties of roses (Michael Bowling) 04/08/95 My
   roses bloomed late this year (Marcel DuBois) 04/08/95 Local flower
   shops that carry orchids (Mary Tsen, 1 response) 04/08/95 Try
   the Blumenhaus (Anne Davis) 04/08/95 Tulip trips to Holland (Mary
   Tsen)

   The Orchid view you just created, however, does not contain
   all the documents you want. @AllChildren only selects the immediate
   children of any parent document(s) that meet the selection criteria:

   **Date
   Topic** 04/08/95 The orchid family of flowers (Anne Davis,
   4 responses) 04/08/95 Sighting of new variation (Brad Sullivan)
   04/08/95 The "ghost" orchid (Rachel Greenbaum, 2 responses)
   04/08/95 Very difficult to see (Brad Sullivan) 04/08/95
   Some sightings reported in Florida (Anne Davis) 04/08/95
   Local flower shops that carry orchids (Mary Tsen, 1 response) 04/08/95
   Try the Blumenhaus (Anne Davis)

   In this case, @AllDescendants
   might provide a better solution. You rewrite the selection formula:

   ```
   SELECT @Contains( Subject; "orchid" ) | @AllDescendants
   ```

   The
   Orchid view now contains entire threads of the orchid discussion:

   **Date
   Topic** 04/08/95 The orchid family of flowers (Anne Davis,
   7 responses) 04/08/95 Sighting of new variation (Brad Sullivan,
   2 responses) 04/08/95 What color? (Anne Davis) 04/08/95
   Please post exact location (Mary Tsen) 04/08/95 The "ghost"
   orchid (Rachel Greenbaum, 3 responses) 04/08/95 Very difficult
   to see (Brad Sullivan, 1 response) 04/08/95 Only blooms
   for an hour or so! (Rachel Greenbaum) 04/08/95 Some sightings
   reported in Florida (Anne Davis) 04/08/95 Local flower shops that
   carry orchids (Mary Tsen, 1 response) 04/08/95 Try the Blumenhaus
   (Anne Davis)

---

## @Ascii

# @Ascii (Formula Language)

Converts an LMBCS (LotusÂ® Multi-Byte
Character Set) string to an ASCII string.

## Syntax

**@Ascii(**  *string*  **)
@Ascii(**  *string*  **; [ALLINRANGE] )**

## Parameters

*string*

Text
or text list. An LMBCS string.

**[ALLINRANGE]**

Keyword.
Optional. Specifies that @Ascii should return a null string ("") if
any characters in the original *string* cannot be represented
by ASCII codes 32 to 127.

## Return value

*newString*

Text or text list. The
original *string,* with each character converted to an ASCII-compliant
character. Any character that can't be represented by ASCII codes
32 to 127 is replaced with a question mark (?). If you specify [ALLINRANGE]
and there are characters that can't be represented by ASCII codes
32 to 127, returns a null string ("").

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

@Ascii
first converts the string into ASCII-compliant characters, replacing
any unrepresented characters with question marks, and then, if [ALLINRANGE]
is True, checks for question marks within the string. This means that
if the original *string* contains a question mark and [ALLINRANGE]
is specified, a null string is returned even if the entire *string* can
be represented by ASCII codes 32-127.

## Examples

1. This example returns Cue.

   ```
   @Ascii( "ÃÃ¼Ã©" )
   ```
2. This example returns Cue??.

   ```
   @Ascii( "ÃÃ¼Ã©Â£Â¥" )
   ```
3. This example returns Cue and Cue?? In a list.

   ```
   @Ascii( "ÃÃ¼Ã©" : "ÃÃ¼Ã©Â£Â¥" )
   ```
4. This example returns a null string ("") since the last 2 characters
   can't be represented by ASCII codes 32 to 127.

   ```
   @Ascii("ÃÃ¼Ã©Â£Â¥";[ALLINRANGE])
   ```
5. This example returns Cue??; cat if field1 is a field containing
   the text list "ÃÃ¼Ã©Â£Â¥";"cat."

   ```
   @Ascii( field1 )
   ```

---

## @ASin

# @ASin (Formula Language)

Calculates the arc (inverse) sine using the sine of an
angle.

## Syntax

**@ASin(**  *sine*  **)**

## Parameters

*sine*

Number
or number list. A sine of an angle, from -1 through 1.

## Return value

*angle*

Number
or number list. An angle, in radians, from -pi/2 through pi/2. This
represents an angle between -90 and 90 degrees.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns pi/2.

   ```
   @ASin( 1 )
   ```
2. This example returns 0.72082 radians (41.3 degrees).

   ```
   @ASin ( 0.66 )
   ```
3. This example returns pi/2 and 0.72082 radians in a list.

   ```
   @ASin ( 1 : 0.66 )
   ```

---

## @ATan

# @ATan (Formula Language)

Calculates the arc (inverse) tangent using the tangent
of an angle.

## Syntax

**@ATan(**  *tangent*  **)**

## Parameters

*tangent*

Number
or number list. The tangent of an angle.

## Return value

*angle*

Number
or number list. An angle, in radians, from -pi/2 through pi/2. This
represents an angle between -90 and 90 degrees.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns pi/4.

   ```
   @ATan( 1 )
   ```
2. This example returns -pi/4.

   ```
   @ATan( -1 )
   ```
3. This example returns 1.10715 radians (63.4 degrees).

   ```
   @ATan( 2 )
   ```
4. This example returns pi/4, -pi/4, and 1.10715 radians in a list

   ```
   @ATan( 1 : (-1) : 2 )
   ```

---

## @ATan2

# @ATan2 (Formula Language)

Calculates the arc tangent using the tangent y/x of an
angle.

## Syntax

**@ATan2(**  *x*  **;**   *y*  **)**

## Parameters

*x*

Number
or number list. The denominator of the tangent value *y / x.*

y

Number
or number list. The numerator of the tangent value *y / x.*

## Usage

If
either parameter is a list, the function operates pair-wise on the
list elements, and the return value is a list with the number of elements
in the larger list.

## Return value

*angle*

Number. An angle, in radians,
from -pi through pi. This represents an angle between -180 and 180
degrees, depending on the sign of x and y (see the list below).

| If | Then *angle* is in the range |
| --- | --- |
| xis positive  yis positive | 0 to pi/2 (Quadrant I) |
| x is negative  y is positive | pi/2 to pi (Quadrant II) |
| x is negative  y is negative | -pi to -pi/2 (Quadrant III) |
| x is positive  y is negative | -pi/2 to 0 (Quadrant IV) |

## Examples

1. This example returns pi/4.

   ```
   @ATan2( 1; 1 )
   ```
2. This example returns 3pi/4.

   ```
   @ATan2( -1; 1 )
   ```
3. This example returns 1.10715 radians (63.4 degrees).

   ```
   @ATan2 ( 1; 2 )
   ```
4. This example returns pi/4, 3pi/4, and 1.10715 radians in a list.

   ```
   @ATan2 ( 1 : : (-1) : 1; 1 : 1 : 2 )
   ```

---

## @AttachmentLengths

# @AttachmentLengths (Formula Language)

Returns a number or a number list containing the length
of each attachment to the current document. The number(s) returned
are only approximations; the actual size(s) of the attachment(s) may
be slightly different.

## Syntax

**@AttachmentLengths(** *excludeMIMEBody* **)**

## Parameters

*excludeMIMEBody*

Boolean.
Optional.

* Specify True (1) to exclude large MIME parts that are stored as
  attachments (but displayed in-line). This is the default.
* Specify False (0) to include large MIME parts that are stored
  as attachments (but displayed in-line).

## Return value

*sizeInBytes*

Number or number list.

* If the current document contains one attachment, *sizeInBytes* is
  a number representing the size of that attachment in bytes.
* If the current document contains more than one attachment, *sizeInBytes* is
  a number list where each number in the number list is the size of
  one of the attachments, in bytes.

## Usage

The
attachment size is computed based on uncompressed file size (that
is, the number of bytes the attachment would use if you extracted
it); the actual disk storage space required for the file may be somewhat
smaller.

@AttachmentLengths returns an empty list if there
are no attachments. If there is one attachment of length 0, @AttachmentLengths
returns 0.

## Examples

1. This example returns 6102 if that is the approximate size of the
   single, attached file.

   ```
   @AttachmentLengths
   ```
2. This example, given a semicolon as the multi-value separator,
   returns AUTOEXEC.BAt:112;CONFIG.SYS:1549;Q4SALES.WK4:17636 if those
   are the names and lengths of the files attached.

   ```
   @AttachmentNames + ":" + @Text(@AttachmentLengths)
   ```
3. This example returns 0 if there is one attachment of length 0.

   ```
   @AttachmentLengths
   ```
4. This example returns an empty list (no value appears at all) if
   there are no attachments.

   ```
   @AttachmentLengths
   ```
5. This example sums the attachment lengths, checking first to make
   sure there are attachments.

   ```
   @Sum(@Attachments > 0; @AttachmentLengths; 0)
   ```

---

## @AttachmentModifiedTimes

# @AttachmentModifiedTimes (Formula Language)

Returns a datetime that displays the date on which the
file attachment associated with the current document was last modified.
If the document contains more than one file attachment, returns the
modification dates in a datetime list.

Note: This @function is new with Release 6.

## Syntax

**@AttachmentModifiedTimes(** *excludeMIMEBody* **)**

## Parameters

*excludeMIMEBody*

Boolean.
Optional.

* Specify True (1) to exclude large MIME parts that are stored as
  attachments (but displayed in-line). This is the default.
* Specify False (0) to include large MIME parts that are stored
  as attachments (but displayed in-line).

## Return value

*modificationDate*

Datetime or datetime
list.

* If the current document contains one attachment, the *modificationDate* is
  a datetime value representing the date on which the attachment was
  last modified.
* If the current document contains more than one attachment, the *modificationDate* is
  a datetime list value representing the dates on which the attachments
  were last modified. The order in which the dates display in the list
  matches the order in which the file names display in the text list
  returned by @AttachmentNames.
* If the current document has no attachments, returns a null string
  ("").

## Examples

1. For a document that contains a rich text field containing one
   attached file, this code, when added to a computed datetime field,
   returns 09/26/2001, the date on which the attached file was last modified.

   ```
   @AttachmentModifiedTimes
   ```
2. If the document contains a rich text field to which the domino.dtd,
   whitepaper.pdf, and myreport.wk1 files were attached on August 7th,
   the following code, when added to a computed datetime field, returns:
   09/25/2001;05/10/2001;09/26/2001, which are the respective dates on
   which the attached files were last modified.

   ```
   @AttachmentModifiedTimes
   ```
3. If a form contains two rich text fields and you attach the domino.dtd
   file to the first field, save the document, and reopen it, this code,
   in a computed datetime field, displays 09/25/2001.

   ```
   @AttachmentModifiedTimes
   ```

   If
   you then attach the whitepaper.pdf file to the second rich text field
   and refresh the document, the computed field changes to display 09/25/2001;05/10/2001.
   Attaching the myreport.wk1 file to the first rich text field and refreshing
   the document causes the computed field to return 09/25/2001;05/10/2001;09/26/2001.

---

## @AttachmentNames

# @AttachmentNames (Formula Language)

Returns the operating system file names of any files attached
to a document. If there are multiple files attached, the names are
returned as a multiple-value text list.

## Syntax

**@AttachmentNames(** *excludeMIMEBody* **)**

## Parameters

*excludeMIMEBody*

Boolean.
Optional.

* Specify True (1) to exclude large MIME parts that are stored as
  attachments (but displayed in-line). This is the default.
* Specify False (0) to include large MIME parts that are stored
  as attachments (but displayed in-line).

## Return value

*names*

Text or text list.

* If the current document contains one attachment, *names* is
  text representing the file name of that attachment.
* If the current document contains more than one attachment, *names* is
  a text list where each itemis the file name of one of the attachments.

## Examples

1. If a file named SALESQ1.WK4 is attached to the document, this
   example returns: SALESQ1.WK4.

   ```
   @AttachmentNames
   ```
2. Given a semicolon as the multivalue separator, if files named
   SALESQ1.WK4 and ADMIN.DOC are attached to the document, this example
   returns: SALESQ1.WK4; ADMIN.DOC.

   ```
   @AttachmentNames
   ```

---

## @Attachments

# @Attachments (Formula Language)

Returns the number of files attached to a document.

## Syntax

**@Attachments(** *excludeMIMEBody* **)**

## Parameters

*excludeMIMEBody*

Boolean.
Optional.

* Specify True (1) to exclude large MIME parts that are stored as
  attachments (but displayed in-line). This is the default.
* Specify False (0) to include large MIME parts that are stored
  as attachments (but displayed in-line).

## Return value

*numFiles*

Number. The number of files
attached to the current document.

## Usage in a Column Formula

When used in a column formula in a
view or folder, @If(@Attachments;5;0) can be used to display the paper
clip icon if the current document has one or more attachments, or
displays a blank if there are no attachments. This is the formula
used to indicate attachments in the Notes/Domino mail template. For
this formula to work, you must select **Icon** in the Column Definition
dialog box for this column.

## Examples

1. This example returns 3 if there are three files attached to a
   document.

   ```
   @Attachments
   ```
2. This example returns 0 if there are no files attached to a document.

   ```
   @Attachments
   ```

---

## @Author

# @Author (Formula Language)

Returns a text list containing the names of the author(s)
of the current document.

## Syntax

**@Author**

## Return value

*authorList*

Text list. All the authors
of the current document. For authors with hierarchical names, Notes/Domino
returns the abbreviated form of the name (as in Denise Lee/Research/Acme),
rather than the canonical form (CN=Denise Lee/OU=Research/O=Acme).

@Author
uses the following instructions (in the sequence outlined) to find
document author(s) and return the appropriate text list:

1. Search the document for a field of type Authors. If there is one,
   return the name(s) stored there. (If there are multiple Authors fields,
   returns the contents of the first Authors field found in the document.)
2. If there is no Authors field, look for a field called From. If
   there is a From field, look for the field FromDomain.
   * If both fields are found, combine the two fields, separating them
     by an @ sign (as in, Mary Tsen@AcmeWest).
   * Otherwise, return the contents of the From field only.
3. If there is no From field, look for a field named $UpdatedBy.
   If there is one, return the contents of the field.
4. If there is no $UpdatedBy field *and* this is a new document
   (not yet saved), return the current user's name.
5. If none of the above can be found, return the null string ("").

## Usage

@Author
is most useful for documents containing an Author Names or From field.

## Examples

If a document has one Authors field
that contains: Mary Tsen, David Smith, Denise Lee/Research/Acme.
This example returns: Mary Tsen; David Smith; Denise Lee/Research/Acme.

```
@Author
```

---

## @Begins

# @Begins (Formula Language)

Determines whether a particular substring is stored at
the beginning of another string.

## Syntax

**@Begins(**  *string*  **;**  *substring*  **)**

## Parameters

*string*

Text
or text string. Any string.

*substring*

Text
or text string. The string you want to search for at the beginning
of *string.*

## Return value

*flag*

Boolean.

* Returns 1 (True) if *substring* is contained within *string*,
  beginning from the first letter
* Returns 0 (False) if not

## Usage

This
function is case-sensitive.

If the either parameter is a list,
the function tests each element of the second parameter against each
element of the first parameter and returns 1 if any match occurs.

## Examples

1. This example returns 1.

   ```
   @Begins("Hi There";"Hi")
   ```
2. This example returns 0.

   ```
   @Begins("Hi There";"hi")
   ```
3. This example checks the field named Topic; if that field begins
   with the string "All desks memo", returns the string: Junk Mail. Otherwise,
   it returns the string: Read this first.

   ```
   @If(@Begins(Topic;"All desks memo");"Junk Mail"; "Read this first")
   ```
4. This formula checks to see if the beginning of the Signature field
   contains the strings "Luigi" or "Florence" or "Henri." If it does,
   the string Verify Signature is returned; otherwise, the string Don't
   Verify Signature is returned.

   ```
   @If(@Begins(Signature; "Luigi":"Florence":"Henri"); "Verify signature"; "Don't Verify Signature")
   ```

---

## @BrowserInfo

# @BrowserInfo (Formula Language)

Determines the capabilities of a Web client, that is you
can determine the properties of the browser for the current request.

Note: This @function is new with Release 5.

## Syntax

**@BrowserInfo(**  *"propertyname"*  **)**

## Parameters

*propertyname*

Text.
The name of the browser property to be retrieved.

## Return value

The return value type is dependent on the capability. The table shows the current set
of Web browser and NotesÂ® client capabilities
that Notes/Domino supports:

| Property name | Return type | Return value for browsers | Return value for NotesÂ® client |
| --- | --- | --- | --- |
| BrowserType | Text | The type of the browser: "Microsoftâ¢" | "NotesÂ®" |
| Cookies | Boolean | 1 (True) if the browser supports cookies; otherwise 0 (False). | 0 (False) |
| DHTML | Boolean | 1 (True) if the browser supports dynamic HTML; otherwise 0 (False). | 0 (False) |
| FileUpload | Boolean | 1 (True) if the browser supports file upload; otherwise 0 (False). | 0 (False) |
| Frames | Boolean | 1 (True) if the browser supports the HTML <FRAME> tag; otherwise 0 (False). | 1 (True) |
| Javaâ¢ | Boolean | 1 (True) if the browser supports Javaâ¢ applets; otherwise 0 (False). | 1 (True) |
| JavaScriptâ¢ | Boolean | 1 (True) if the browser supports JavaScriptâ¢; otherwise 0 (False). | 1 (True) |
| Iframe | Boolean | 1 (True) if the browser supports the Microsoftâ¢ HTML <IFRAME> tag; otherwise 0 (False). | 0 (False) |
| Platform | Text | The operating system platform of the browser: "Win95," "Win98," "WinNT," "MacOS," or "Unknown." | "Unknown" |
| Robot | Boolean | 1 (True) if the browser is probably a Web robot; otherwise 0 (False). | 0 (False) |
| SSL | Boolean | 1 (True) if the browser supports SSL; otherwise 0 (False). | 0 (False) |
| Tables | Boolean | 1 (True) if the browser supports the HTML <TABLE> tag; otherwise 0 (False). | 1 (True) |
| VBScript | Boolean | 1 (True) if the browser supports VBScript; otherwise 0 (False). | 0 (False) |
| Version | Number | The browser version number, or -1 for unrecognized browsers. | NotesÂ® client build number |

## Usage

@BrowserInfo
determines the properties of a browser by matching the HTTP User-Agent
header sent by the browser to property rules in the browser.cnf file
in the DominoÂ® data directory.
@BrowserInfo also contains hard-coded rules for the NotesÂ® client.

@BrowserInfo can be used
in all types of formulas except view selection and view column formulas.

Pre-5.0 NotesÂ® clients will not be able
to open forms that use @BrowserInfo. The client will display the error
message "Invalid formula: unknown function/operator." To prevent this
error, check the version number of the client in your formulas. Example:

@If(@TextToNumber(@Version) >=
160; @BrowserInfo("BrowserType");"Unknown")

## Examples

This example displays the value in
the field named KeyThought, if the current browser supports JavaScriptâ¢; otherwise the
value in the field Topic is displayed.

```
@If (@BrowserInfo("JavaScript"); KeyThought;Topic)
```

---

## @BusinessDays

# @BusinessDays (Formula Language)

Returns the number of business days in one or more date
ranges.

## Syntax

**@BusinessDays(** *startDates* **;** *endDates* **;** *daysToExclude*  **;** *datesToExclude*  **)**

Note: This @function is new with Release 6.

## Parameters

*startDates*

Time-date
or time-date list. The start of each date range.

*endDates*

Time-date
or time-date list. The end of each date range.

*daysToExclude*

Numer
or number list. Optional. Days of the week not counted as business
days, where 1 is Sunday and 7 is Saturday. Decimal numbers are rounded
to integers. Numbers other than 1-7 are ignored.

*datesToExclude*

Time-date
or time-date list. Optional. Dates not counted as business days.

## Return value

*numberOfDays*

Number or number list.
The number of days from *startDates* to *endDates*, inclusive,
less *daysToExclude* and *datesToExclude* that fall within
the date range.

## Usage

The
operation on *startDates* and *endDates* is a pair-wise
list operation. If they are not the same length, the shorter list
is filled out with the value of the last element.

@BusinessDays
returns -1 if the calculation produces a negative number of days,
an end date precedes a start date, or a time-date value contains only
a time.

## Examples

1. This agent displays the number of days in 2001 excluding Saturdays,
   Sundays, and 10 holidays.

   ```
   @Prompt([OK];
   @Text(
   @BusinessDays([01/01/2001]; [12/31/2001]; 1 : 7;
   [01/01/2001] : [01/15/2001] : [02/16/2001] : [05/28/2001] : [07/04/2001] :
   [09/03/2001] : [10/08/2001] : [11/22/2001] : [11/23/2001] : [12/25/2001])
   );
   "Business days in 2001")
   ```
2. This agent displays the number of days in each quarter of 2001
   excluding Saturdays, Sundays, and 10 holidays.

   ```
   @Prompt([OK];
   @Implode(@Text(
   @BusinessDays([01/01/2001] : [04/01/2001] : [07/01/2001] : [10/01/2001];
   [03/31/2001] : [06/30/2001] : [09/30/2001] : [12/31/2001];
   1 : 7;
   [01/01/2001] : [01/15/2001] : [02/16/2001] : [05/28/2001] : [07/04/2001] :
   [09/03/2001] : [10/08/2001] : [11/22/2001] : [11/23/2001] : [12/25/2001])
   ); "-");
   "Business days in 2001 by quarter")
   ```
3. This field value formula returns the number of days from StartDate
   to EndDate, inclusive, less NonWorkDays and Holidays. StartDate and
   EndDate are time-date fields with scalar values. NonWorkDays is a
   keyword field with alias values of "1" and "7" for Sunday and Saturday.
   Holidays is a time-date field that allows multiple values.

   ```
   @BusinessDays(StartDate; EndDate;
   @TextToNumber(NonWorkDays);
   Holidays)
   ```
4. This code, when added to a view action in a calendar view that
   contains a multiple-day event, displays a dialog box that shows the
   number of business days in the event. For instance, if, in your calendar
   view, you include a Vacation event that lasts for 32 days (startDT
   field is 08/02/2002 and endDT field is 09/02/2002), when a user selects
   the Vacation event from the calendar and clicks on the button, a dialog
   box appears entitled "Business days" that displays 22.

   ```
   @Prompt([OK];"Business days";@Text(@BusinessDays(startDT;endDT;1:7)))
   ```

   To
   account for a holiday on September 2, edit the formula as follows:

   ```
   @Prompt([OK];"Business days";@Text(@BusinessDays(startDT;endDT;1:7;[09/02/2002])))
   ```

---

## @Certificate

# @Certificate (Formula Language)

Extracts information from the Certified Public Key in the DominoÂ® Directory.

## Syntax

**@Certificate(**  **[**  *dataToRetrieve*  **]
; Certificate )**

## Parameters

**[**  *dataToRetrieve*  **]**

Keyword.
Must be enclosed in brackets as shown. Use one of the following keywords:

**[SUBJECT]**

The
name of the certified user ID or server ID.

**[ISSUER]**

The
name of the ID used to issue the certificate.

**[EXPIRATION]**

The
date and time that the North American certificate expires.

**[INTLEXPIRATION]**

The
date and time that the International certificate expires.

**Certificate**

Required.
This specifies the name of the field where the Certified Public Key
information is stored.

## Return value

*dataRetrieved*

Text for the Subject
and Issuer names, and time-date values for the Expiration and IntlExpiration
dates.

## Usage

@Certificate
is useful within a macro or view selection formula for selecting a
list of users whose certificates are about to expire; it is used by
several DominoÂ® Directory
tools.

@Certificate only retrieves data; you cannot use it
to change certificate information (use the appropriate Administration
menus to update certificates). Certified Public Key information is
stored only for users and servers with hierarchical IDs; @Certificate
returns a null string for nonhierarchical IDs.

If you use incorrect
syntax, @Certificate returns a null string and does not generate an
error message.

@Certificate returns a null string (""), instead
of the name of the server ID, when used in a Scheduled agent running
on the server.

You cannot use this function in Web applications.

## Examples

1. This example returns CN=Michael Bowling/OU=R&D/O=WorkSavers/C=US
   for Michael Bowling's hierarchical ID.

   ```
   @Certificate([SUBJECT];Certificate)
   ```
2. This example returns the name of the ID that certified the ID.

   ```
   @Certificate([ISSUER];Certificate)
   ```
3. This example returns the date and time the North American ID expires.

   ```
   @Certificate([EXPIRATION];Certificate)
   ```
4. This example returns the date and time the International ID expires.

   ```
   @Certificate([INTLEXPIRATION];Certificate)
   ```

---

## @Char

# @Char (Formula Language)

Converts an HCL Code Page 850 code number into the
corresponding single character string.

## Syntax

**@Char(**  *codeNumber*  **)**

## Parameters

*codeNumber*

Number
or number list. Any number between 0 and 255. Non-integer numbers
are truncated to integers.

## Return value

*correspondingChar*

Text
or text list. A single character that corresponds to *codeNumber.*

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

@Char(10)
returns a carriage return.

@Char(9) returns a tab.

Note: In the NotesÂ® client,
the codeNumber parameters 0 and 9 do not work in column formulas.

@Char(13)
returns a carriage return when used in an @Prompt formula.

To
add multiple lines to a single column row:

1. In the View Properties box:
   * Change the Lines per row to the number of carriage returns you
     want to include in the row.
   * Select Shrink rows to content.
2. In the Column Properties box:
   * Choose New Line as the Multi-value separator.
   * Deselect the Show multiple values as separate entries checkbox.
3. In the code for the column formula, specify each string or number
   that you want to display on a new line as a separate value. Since
   you set the Multi-value separator to New Line, this inserts a carriage
   return between each value. For example, the following column formula
   vertically lists the content of the FirstName field before the content
   of the LastName field in the column row:

   ```
   first:= FirstName;
   last := LastName;
   @Trim(first : last)
   ```

## Examples

1. This example returns: A.

   ```
   @Char(65)
   ```
2. This example returns: a.

   ```
   @Char(97)
   ```
3. This example returns: 8.

   ```
   @Char(56)
   ```
4. This example returns: A, a, and 8 in a list.

   ```
   @Char(65 : 97 : 56)
   ```
5. This example returns the character in the field named QuestionnaireNumber
   if that field is currently filled in; otherwise, returns a null string.

   ```
   @If(@IsAvailable(QuestionnaireNumber);
        @Char(QuestionnaireNumber);"")
   ```
6. This example uses @Char(13) to insert a carriage return into the
   text of @Prompt.

   ```
   @Prompt([OK]; "Complete"; "The agent has finished." + @Char(13) + "Please exit this document without saving.")
   ```

---

## @CheckAlarms

# @CheckAlarms (Formula Language)

Triggers the alarm daemon to check for new alarms in the
mail file.

## Syntax

**@CheckAlarms**

## Usage

You
use @CheckAlarms whenever you make changes to any scheduling that
involves alarms. This includes creating a new appointment or anniversary
event with an alarm, changing an existing appointment that has an
alarm (because the mailer daemon has to reread the information to
find out when the new alarm should go off), or deleting an appointment
that had an alarm.

---

## @CheckFormulaSyntax

# @CheckFormulaSyntax (Formula Language)

Checks a block of commented out formula language code for
errors.

Note: This @function is new with Release 6.

## Syntax

**@CheckFormulaSyntax**(*formulaText*)

## Parameters

*formulaText*

Text.
The formula code to test for errors, commented out. Enclose the formula
code in braces ({}) to comment out the code.

## Return value

*errorInformation*

Text or textlist.

* Returns "1" if the formula has no errors.
* Returns the text list "*errorMessage*" : "*errorLine*"
  : "*errorColumn*" : "*errorOffset*" : "*errorLength*"
  : "*errorText*" where each list item is defined as follows:

  *errorMessage*:
  Message returned by the compiler.

  *errorLine*: Line where
  the error occurred, beginning with 1, not zero. New lines created
  by wrapped text are not counted.

  *errorColumn*: Number
  of character spaces from the first character in the line where the
  error occurred, beginning with 1.

  *errorOffset*: Number
  of character spaces from the first character in the formulaText block
  where the error occurred, beginning with 1.

  *errorLength*:
  Length of the text making up the error.

  *errorText*:
  Text or token that the compiler processes as the cause of the error.

## Usage

The
formula you are checking is not "commented out". {) are quote characters.
The argument to the function is a string which contains the formula
to be checked, not a comment. It is no different from any other function
that takes a string argument. The reason { } is used in the examples
rather than the more usual double quotes, is because the string you
are quoting is likely to contain double quotes. Using { } avoids "escaping"
the quotes within the text. If the value is not hard coded but read
from a field, as it would be in most applications, this is not an
issue.

This @function reports compile
errors, not run-time errors. A run-time error is generated, for example,
if a function has an insufficient number of arguments. This function
is useful especially if you are using the [@Eval](H_EVAL.html "At run-time, compiles and runs each element in a text expression as a formula. Returns the result of the last formula expression in the list or an error if an error is generated by any of the formula expressions in the list.") function
to execute a text expression at run-time, since you can use it to
first check the syntax of any text you supply to @Eval.

Note: Do not use the expression @CheckFormulaSyntax(...) = "1"
to test that there is not an error, because this will return True
if there is a "1" in any element of the returned error information,
for example, if the error is on line 1.

## Examples

1. This example returns "Unknown @Function":"4":"1":"60":"8":"@MailSnd"
   when used as the default value for a text field.

   ```
   formula := {subject:="test";
   remark:="ok";
   SendTo:="Darrin Dogs/Star";
   @MailSnd(SendTo;"";"";subject;remark;"ID";[Sign]:[Encrypt])};
   @CheckFormulaSyntax(formula);
   ```
2. This code returns "1" when used as the default value for a text
   field.

   ```
   formula := {subject:="test";
   remark:="ok";
   SendTo:="Darrin Dogs/Star";
   @MailSend(SendTo;"";"";subject;remark;"ID";[Sign]:[Encrypt])};
   @CheckFormulaSyntax(formula);
   ```
3. This Input Validation formula checks that the user has entered
   a valid formula in a text field:

   ```
   @If(@ThisValue= ""; 
   @Return(@Failure("You must enter a formula here.")); 
   0);
   -tmp[1] :=  @CheckFormulaSyntax(@ThisValue);
   @If(_tmp[1] = "1";
   = "1"; 
   @Success; 
   @Failure("Invalid formula: " + -tmp[1] + " on line " + -tmp[2]))
   ```

---

## @ClientType

# @ClientType (Formula Language)

Returns a text string to differentiate NotesÂ® clients and World Wide Web browsers.

## Syntax

**@ClientType**

## Return value

*client type*

Text.

* Returns "NotesÂ®" if the
  client type is a NotesÂ® client
* Returns "Web" if the client type is a Web browser

## Usage

@ClientType
is useful within database formulas, form formulas, buttons in forms,
and "hide-when" formulas. Do not use @ClientType in column formulas.

@ClientType
always returns "None" when executed in a server background agent.

## Examples

1. This example returns the client type.

   ```
   @Prompt([OK]; "Client type"; @ClientType)
   ```
2. This example, used in a button, opens a view called "By Category
   - NotesÂ®" if the client type
   is "NotesÂ®," or a view called
   "By Category - Web" otherwise."

   ```
   @If(@ClientType = "Notes"; @Command([OpenView]; 
         "By Category - Notes");
   @Command([OpenView]; "By Category - Web"))
   ```

---

## @Command

# @Command (Formula Language)

Executes a Notes/Domino command. Most of the standard menu
commands can be executed using @Command. In addition, a number of
specialized commands are available. In a formula, any command invoked
using @Command runs in the order you specify in the formula. This
means that any changes made by the command, such as inserting text
into a field, affect the rest of the formula (see exceptions).

## Syntax

**@Command(
[**  *command*  **] ;**  *parameters*  **)**

## Usage

This
function does not work in column, selection, hide-when, section editor,
window title, field or form formulas, scheduled, new and modified
and mail processing agents that run on a server or agents that run
on all documents or selected documents. It's intended for use in toolbar
button, hotspot, and action formulas.

## Exceptions

The
commands listed in the Evaluated after all @functions column in the
table (as follows)*always* execute after all the functions present
in a formula are executed, which means that the action performed by
a command cannot be used by a function that follows it in a formula.
The commands listed in the Evaluated immediately column have the equivalent
functionality to the corresponding Evaluated after all @functions
commands, except they execute as soon as they are encountered in the
formula.

Note: The Evaluated as encountered
commands are new with Release 6.

| Evaluated after all @functions | Evaluated immediately |
| --- | --- |
| [EditClear](H_EDITCLEAR.html "Performs the menu command Edit - Delete.") | [Clear](H_CLEAR_COMMAND.html "Performs the menu command Edit - Delete.") |
| [EditProfile](H_EDITPROFILE.html "Opens a new or existing profile document in Edit mode.") | [EditProfileDocument](H_EDITPROFILEDOCUMENT_COMMAND.html "Creates a new or opens an existing profile document in Edit mode.") |
| [FileCloseWindow](H_FILECLOSEWINDOW.html "Closes the current Notes window. If the document or design element in that window has not been saved, Notes prompts the user to save it before closing.") | [CloseWindow](H_CLOSEWINDOW_COMMAND.html "Same as the File - Close command menu. Closes the current Notes tab, or the window if this was the last tab in the window. If the document or design element in that window has not been saved, Notes prompts the user to save it before closing.") |
| [FileDatabaseDelete](H_FILEDATABASEDELETE.html "Permanently deletes the current database file from the hard disk where it is stored.") | [DatabaseDelete](H_DATABASEDELETE_COMMAND.html "Permanently deletes the current database file from the hard disk where it is stored.") |
| [FileExit](H_FILEEXIT.html "Performs the menu command File - Exit (File - Quit on the Macintosh), which closes Notes/Domino and all its open windows.") | [ExitNotes](H_EXITNOTES_COMMAND.html "Performs the menu command File - Exit (File - Quit on the Macintosh), which closes Notes/Domino and all its open windows.") |
| [Folder](H_FOLDER_COMMAND.html "Moves or copies the selected document to a folder.") | [FolderDocuments](H_FOLDERDOCUMENTS_COMMAND.html "Moves or copies the selected document to a folder.") |
| [NavigateNext](H_NAVIGATENEXT.html "Navigates to the next document in the current view or folder.") | [NavNext](H_NAVNEXT_COMMAND.html "Navigates to the next document in the current view or folder.") |
| [NavigateNextMain](H_NAVIGATENEXTMAIN.html "Navigates to the next main document in the current view.") | [NavNextMain](H_NAVNEXTMAIN_COMMAND.html "Navigates to the next main document in the current view.") |
| [NavigateNextSelected](H_NAVIGATENEXTSELECTED.html "Navigates to the next selected document in the current view or folder.") | [NavNextSelected](H_NAVNEXTSELECTED_COMMAND.html "Navigates to the next selected document in the current view or folder.") |
| [NavigateNextUnread](H_NAVIGATENEXTUNREAD.html "Navigates to the next unread document in the current view or folder.") | [NavNextUnread](H_NAVNEXTUNREAD.html "Navigates to the next unread document in the current view or folder.") |
| [NavigatePrev](H_NAVIGATEPREV.html "Navigates to the previous document in the current view or folder.") | [NavPrev](H_NAVPREV_COMMAND.html "Navigates to the previous document in the current view or folder.") |
| [NavigatePrevMain](H_NAVIGATEPREVMAIN.html "Navigates to the previous main document in the current view or folder.") | [NavPrevMain](H_NAVPREVMAIN_COMMAND.html "Navigates to the previous main document in the current view or folder.") |
| [NavigatePrevSelected](H_NAVIGATEPREVSELECTED.html "Navigates to the previous selected document in the current view or folder.") | [NavPrevSelected](H_NAVPREVSELECTED_COMMAND.html "Navigates to the previous selected document in the current view or folder.") |
| [NavigatePrevUnread](H_NAVIGATEPREVUNREAD.html "Navigates to the previous unread document in the current view or folder.") | [NavPrevUnread](H_NAVPREVUNREAD_COMMAND.html "Navigates to the previous unread document in the current view or folder.") |
| [ReloadWindow](H_RELOADWINDOW_COMMAND.html "Reloads or refreshes the contents of the current window.") | [RefreshWindow](H_REFRESHWINDOW_COMMAND.html "Reloads or refreshes the contents of the current window.") |
| [ToolsRunBackgroundMacros](H_TOOLSRUNBACKGROUNDMACROS.html "Runs all of the database's scheduled agents, regardless of when they are scheduled to run. The agents will then run as usual at their regularly scheduled times.") | [RunScheduledAgents](H_RUNSCHEDULEDAGENTS_COMMAND.html "Runs all of the database's scheduled agents, regardless of when they are scheduled to run. The agents will then run as usual at their regularly scheduled times.") |
| [ToolsRunMacro](H_TOOLSRUNMACRO.html "Executes a specified agent.") | [RunAgent](H_RUNAGENT_COMMAND.html "Executes a specified agent.") |
| [ViewChange](H_VIEWCHANGE.html "Switches to the specified view or folder within the current database or, if a view or folder is not specified, displays the View menu so the user can select a view.") | [SwitchView](H_SWITCHVIEW_COMMAND.html "Switches to the specified view or folder within the current database or, if a view or folder is not specified, displays the View menu so the user can select a view.") |
| [ViewSwitchForm](H_VIEWSWITCHFORM.html "Changes the form used to display the current document.") | [SwitchForm](H_SWITCHFORM_COMMAND.html "Changes the form used to display the current document.") |

---

## @Compare

# @Compare (Formula Language)

Compares the alphabetic order of the elements in two lists
pair-wise.

Note: This @function is new with Release 6.

## Syntax

**@Compare(**  *textlist*  **;**  *textlist*  **;
[**  *options*  **]** **)**

## Parameters

*textlist*

Text
list. The first two parameters are text lists. If one list is shorter,
the last element in the shorter list is repeated until it reaches
the same length as the longer list. The corresponding elements of
each list are compared.

**[**  *options* **]**

Keyword
list. The list can include any of the following keywords. Conflicting
options result in the last specified.

**[CASESENSITIVE]** (default)

**[CASEINSENSITIVE]**

**[ACCENTSENSITIVE]** (default)

**[ACCENTINSENSITIVE]**

**[PITCHSENSITIVE]** (default)

**[PITCHINSENSITIVE]**

## Return value

*result*

Number list. Each element
is the result of comparing the corresponding elements in the text
lists, and is one of three values:

* 0 if the elements in the two lists are equal
* -1 if the element in the first list is less than the element in
  the second list. For example, this is the result if the first list
  contains alice and the second list contains bobby.
* 1 if the element in the first list is greater than the element
  in the second list. For example, this is the result if the first list
  contains bobby and the second list contains alice.

## Usage

The
comparison sequence for the English character set is as follows: the
apostrophe, the dash, the numbers 0-9, the alphabetic characters a-z
and A-Z, and the remaining special characters. The sequence for the
alphabetic characters is in order, lowercase character first: a, A,
b, B, and so on through z, Z. This sequence can lead to some anomalies;
for example, "new york" compares before "New Boston." Use the [CaseInsensitive]
option, or [@UpperCase](H_UPPERCASE.html "Converts the lowercase letters in the specified string to uppercase."), [@LowerCase](H_LOWERCASE.html "Converts the uppercase letters in the specified string to lowercase."), and [@ProperCase](H_PROPERCASE.html "Converts the words in a string to proper-name capitalization: the first letter of each word becomes uppercase, all others become lowercase.") to address this behavior.

If
you set Unicode standard sorting as the sorting option, you cannot
select the following keywords or combinations:

* **[PITCHINSENSITIVE]**
* **[CASESENSITIVE]:[ACCENTINSENSITIVE]**

You specify Unicode standard sorting by setting the notes.ini
variable $CollationType to @UCA, or by selecting the "Unicode standard
sorting" checkbox that displays in the following dialog boxes:

* Sorting dialog box that displays when you choose File - Preferences
  - User Preferences - International - Sorting from the main menu
* Database Properties box\*
* Design Document Properties box\*

\*The Unicode option is disabled in the Database and Design
Document Properties boxes until you select a default sort order.

For
more information on Unicode sorting, see http://oss.software.ibm.com/icu/

## Examples

1. This action compares a list to the value "N" and displays the
   result. Boston and Moscow result in -1 (less than N), Tokyo results
   in 1 (greater than N), and n and N result in 0.

   ```
   list := "Boston" : "Tokyo" : "Moscow" : "N" : "n";
   result := @text(@compare(list; "N"; [CaseInsensitive]));
   @Prompt([OKCANCELLIST] : [NOSORT]; 
   "Result"; ""; ""; list + " (" + result + ")")
   ```
2. This computed field formula compares the two multi-value fields
   Name1 and Name2 and posts the result as its value. Text is substituted
   for the numeric result values.

   ```
   @If(Names1 = "" | Names2 = ""; ""; @do(
   comp1 := @Compare(Names1; Names2;
   [CASEINSENSITIVE] : [ACCENTINSENSITIVE]);
   comp2 := @Replace(@Text(comp1); "-1" : "0" : "1";
   " is less than " : " is equal to " : " is greater than ");
   Names1 + comp2 + Names2))
   ```
3. This computed field formula for a multi-value field named Column2
   compares Column1 with A and Z to see if its values start in the alphabetic
   range. Text is posted to Column2 when the value in Column1 is out
   of range.

   ```
   @If(Column1 = ""; ""; @Do(
   Low1 := @Compare(Column1; "A"; [CASEINSENSITIVE]);
   High1 := @Compare(Column1; "Z"; [CASEINSENSITIVE]);
   Low2 := @Replace(@Text(Low1); "-1" : "0" : "1"; "Does not start with alpha" : "" : "");
   High2 := @Replace(@Text(High1); "-1" : "0" : "1"; "" : "" : "Does not start with alpha");
   Low2 + High2))
   ```
4. This formula retrieves all the elements that begin with a, b,
   or c from the text list in the sailboats field:

   ```
   @For(n:=1;n <= @Elements(sailboats);n := n+1;
   FIELD result := @If(n=1;@If(@Compare(sailboats[n];"d";[CASEINSENSITIVE])=-1;
   sailboats[n];"");@If(@Compare(sailboats[n];"d";[CASEINSENSITIVE])=-1;
   result:(sailboats[n]);result)));
   result
   ```

   If the sailboats field contains "Hunter":"C&C":"Pearson":"Contessa":"Bristol,"
   this formula returns "C&C;Contessa;Bristol."

---

## @ConfigFile

# @ConfigFile (Formula Language)

Returns the file path for the initialization file for NotesÂ® (notes.ini).

Note: This function is new with Release 6.

## Syntax

**@ConfigFile**

## Return value

*notes.ini path*

String. Returns the
file path to the notes.ini initialization file.

## Usage

When
the formula is executed on the NotesÂ® client,
it returns the filename and path of the notes.ini initialization file
for the NotesÂ® client. When
the formula is executed on the server or Web server (when accessed
in a Web page, for example), it returns the filename and path of the
notes.ini initialization file for the server.

## Examples

1. This formula, when added to a computed field on a form and previewed
   in NotesÂ®, returns: C:\Notes\notes.ini
   if the current NotesÂ® client
   was installed in C:\Notes.

   ```
   @ConfigFile
   ```
2. This formula, when added as computed text to a page, returns D:\webapp\notes.ini,
   when the page being previewed by a Web browser resides in a database
   hosted by the webapp server.

   ```
   @ConfigFile
   ```

---

## @Contains

# @Contains (Formula Language)

Determines whether a substring is stored within a string.

## Syntax

**@Contains(**  *string*  **;**  *substring*  **)**

## Parameters

*string*

Text
or text list. The string(s) you want to search.

*substring*

Text
or text list. The string(s) you want to search for in *string.*

## Return value

*flag*

Boolean.

* Returns true (1) if any substring is contained in one of the strings
* Returns false (0) if no substrings are contained in any of the
  strings

  Note: If any element in the substrings is a null
  string(""), this function always returns true.

## Usage

This
function is case-sensitive.

If either parameter is a list,
the function returns 1 if any element of parameter 1 contains any
element of parameter 2.

You cannot use this function to test
for substrings in a rich text field.

Avoid using this function
to test for an exact match (that is, parameter 2 equals parameter
1). The result will be wrong if parameter 1 is not an exact match
but does contain parameter 2. Use the equal operator or [@IsMember](H_ISMEMBER.html "Indicates if a piece of text (or a text list) is contained within another text list. The function is case-sensitive."), which will give the desired
result and are more efficient.

## Examples

1. This example returns 1 to indicate that the substring, "Th," is
   contained in the string, "Hi There."

   ```
   @Contains("Hi There";"Th")
   ```
2. This example returns 1 to indicate that the items in one text
   list are contained in the other text list.

   ```
   @Contains("Tom":"Dick":"Harry";"Harry":"Tom")
   ```
3. This example returns 1 to indicate that the single text item in
   one parameter is present in the text list that makes up the other
   parameter.

   ```
   @Contains("Tom";"Tom":"Dick":"Harry")
   ```
4. This input validation formula for the "RequestShipDate" field
   checks if the date in the field is invalid or if the field named ProductLeadTime
   contains the phrases "weeks" or "month." If either condition is true,
   when the user saves the document a message box displays stating, "You
   must request a valid ship date."

   ```
   @If(@Contains(ProductLeadTime;"weeks":"month"); @If(!@IsTime(RequestedShipDate);
   @Failure("You must request a valid ship date.");@Success;@Success)
   ```
5. This view selection formula creates a new view that includes only
   documents that have a Subject field containing the text "Mary Lamb"
   (in any case).

   ```
   SELECT form = "Memo" & @Contains(@LowerCase(Subject); "mary lamb")
   ```
6. This action formula opens a WebForm instead of a NotesForm if
   the user executing the action is assigned the role of "[WebUser]"
   in the ACL.

   ```
   @Command([Compose]; @If(@Contains(@UserRoles; "WebUser"); "WebForm"; "NotesForm"))
   ```

   Note: For this example to work, the Enforce a consistent ACL
   across all replicas of this database option must be selected.

---

## @Cos

# @Cos (Formula Language)

Given an angle in radians, returns the cosine of the angle.
In a right triangle, the cosine of an acute angle is the ratio of
the length of its adjacent side to the length of the hypotenuse.

## Syntax

**@Cos(**  *angle*  **)**

## Parameters

*angle*

Number
or number list. An angle measured in radians.

## Return value

*cosine*

Number
or number list. The cosine of *angle*, from -1 to 1.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This formula returns 1.

   ```
   @Cos( 2 * @Pi )
   ```
2. This formula returns 1 and -1 in a list.

   ```
   @Cos( (2 * @Pi) : @Pi )
   ```
3. This formula finds the length of side c in triangle ABC. You know
   the value of angle C in radians, and the lengths of sides a and b.
   This formula finds the length of side c.

   ```
   @Sqrt( @Power( sideA; 2 )+@Power( sideB; 2 )- 
        ( 2*sideA*sideB*( @Cos( angleC ) ) ))
   ```

   This formula
   is a version of the law of cosines, which states that for any triangle
   ABC, c2 = a2+b2-2ab(cos(C)).

---

## @Count

# @Count (Formula Language)

Calculates the number of text, number, or time-date values
in a list. This function always returns a number to indicate the number
of entries in the list.

This function is similar to [@Elements](H_ELEMENTS.html "Calculates the number of text, number, or time-date values in a list. This function always returns a number to indicate the number of entries in the list."),
except that it returns the number 1 instead of 0, when the value it
is evaluating is not a list or is a null string.

Note: This
@function is new with Release 6.

## Syntax

**@Count(**  *list*  **)**

## Parameters

*list*

Text
list, number list, or time-date list.

## Return value

*numElements*

Number. The number of
elements in the list. If the value is not a list or is a null string,
@Count(list) returns the number 1.

## Examples

1. This formula returns the value 3 when the stooges field contains
   the text list: "Moe":"Larry":"Curly":

   ```
   @Count(stooges)
   ```
2. This code returns the value 1, even if there are 4 entries under
   the Kevlar category in the Sails view, when the value of the third
   column in the view is determined by the simple function, # in View:

   ```
   @Count(@DbLookup("";"Server/Name/Notes":"Cost\\Materials.nsf";"Sails";"Kevlar";3))
   ```

   This
   formula returns 1 because the list produced by the # in View simple
   function is a list of special text, not a standard number list. If
   @Count were replaced by @Elements, the formula would return 0.
3. This formula returns a list of the largest numbers after performing
   a pair-wise comparison of the elements in the Asia\_total and USA\_total
   fields, which contain number lists representing the monthly sales
   totals for the year in these two regions:

   ```
   tAsia := @Count(Asia_total);
   tUSA := @Count(USA_total);
   dif := (tAsia - tUSA);
   result := @If(@Sign(dif) = -1;@Subset(USA_total;tUSA=@Abs(dif));
   @Subset(Asia_total;(tAsia - dif)));
   @If(@Sign(dif) = -1;@Max(Asia_total;result);@Max(result;USA_total))
   ```

   If
   the USA has not yet posted its fourth quarter sales totals, this formula
   displays only the results of comparing the figures posted for the
   first nine months. It does not follow the default behavior of repeating
   the September sales figure three times to even out the two list lengths.

---

## @Created

# @Created (Formula Language)

Returns the time-date when the document was created.

## Syntax

**@Created**

## Return value

*dateCreated*

Time-date. The date when
the current document was created.

## Usage

@Created
differs from @Now, in that @Created returns a time-date value that
remains constant, while @Now returns a dynamic time-date that changes
with each formula evaluation when it is used in a computed field.

In
a field formula, Notes/Domino takes the value for @Created from the
server clock, unless the database is local.

## Examples

1. This example returns 06/23/95 11:36:50 AM for a document created
   on June 23, 1995, at 11:36:50 A.M.

   ```
   @Created
   ```
2. This example returns 8/4/93 3:10:00 PM for a document created
   on April 4, 1992 at 3:10 P.M.

   ```
   @Adjust(@Created;1;4;0;0;0;0)
   ```

   See [@Adjust](H_ADJUST.html "Adjusts the specified time-date value by the number of years, months, days, hours, minutes, and/or seconds you specify. The amount of adjustment may be positive or negative.") for an explanation of the parameters
   following @Created.
3. This code, when added as the view selection formula, populates
   the view with only those documents created after July 23, 2001.

   ```
   SELECT @Created > [07/23/2001]
   ```
4. If you add the following code as the form formula for a view,
   all documents created before June 1, 2001 display using the "oldFormat"
   form and those created on or after June 1 use the "newFormat" form.

   ```
   @If(@Created >= [06/01/01];"newFormat";"oldFormat")
   ```
5. This view selection formula uses @Created to select only those
   documents created in the current month. To avoid having the view refresh
   indicator display, it uses @TextToTime("Today") instead of @Today.
   Date calculations in views may impact the performance of an application.

   ```
   SELECT ( ( @Year( @Created ) = @Year( @TextToTime( "Today" ) ) ) & ( @Month( @Created ) = @Month( @TextToTime( "Today" ) ) ) )
   ```

---

## @Date

# @Date (Formula Language)

Translates numbers for the various components of time and
date, then returns the time-date value.

## Syntax

**@Date(**  *year*  **;**  *month*  **;**  *day*  **)
@Date(**  *year*  **;**  *month*  **;**  *day*  **;**  *hour*  **;**  *minute*  **;**  *second*  **)
@Date(**  *time-date*  **)**

## Parameters

*year*

Number.
The year that you want to appear in the resulting date. You must specify
an entire four-digit year. (For example, use 1996, not 96).

*month*

Number.
The month that you want to appear in the resulting date. (For example,
use 1 to specify January).

*day*

Number.
The day that you want to appear in the resulting date.

*hour*

Number.
The number of hours. This value will be truncated from the resulting
date.

*minute*

Number. The number of minutes.
This value will be truncated from the resulting date.

*second*

Number.
The number of seconds. This value will be truncated from the resulting
date.

*time-date*

Time-date or time-date list.
For a time-date value such as @Now or [10/31/93 12:00:00], @Date removes
the time portion of the value, leaving only the date.

## Return value

*truncatedTimeDate*

Time-date.
The date corresponding to the parameters that you sent to @Date, minus
any time components.

## Usage

If
the parameter is a date-time list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

Specifying invalid numbers will result in a blank
date.

## Examples

1. This example returns 06/23/95.

   ```
   @Date(1995; 06; 23)
   ```
2. This example returns 06/23/0095.

   ```
   @Date(95; 06; 23)
   ```
3. This example returns 06/23/2095.

   ```
   @Date(2095; 06; 23)
   ```
4. This example returns 06/23/95 if the time-date value in the field
   named ResponseDate is 06/23/95 03:00:01 P.M.

   ```
   @Date(ResponseDate)
   ```
5. This example returns 1/20/93 08:58:12 AM.

   ```
   @Date(1993; 01; 20; 8; 58; 12)
   ```
6. This example returns 11/20/95.

   ```
   @Date([11/20/95 8:58:12])
   ```
7. This example returns 11/20/95 and 11/21/95 in a list.

   ```
   @Date([11/20/95 8:58:12] : [11/21/95 8:58:12])
   ```

---

## @Day

# @Day (Formula Language)

Extracts the day of the month from the specified date.

## Syntax

**@Day(**  *timeDateValue*  **)**

## Parameters

*timeDateValue*

Time-date
or time-date list. The date containing the day value that you want
to extract.

## Return value

*dayOfMonth*

Number
or number list. The number corresponding to the day of the month indicated
by *timeDateValue.* Returns -1 if the time-date provided contains
only a time value and not a date.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 15 if today is July 15, August 15, September
   15, and so on.

   ```
   @Day(@Now)
   ```
2. This example returns 20 and 21 in a list.

   ```
   @Day([11/20/95 8:58:12] : [11/21/95 8:58:12])
   ```
3. This example returns the string "Payment received on or before
   the 15th" if the PaymentReceived field is filled in on or before the
   15th of the month; otherwise, it returns the string "Payment received
   after the 15th."

   ```
   @If(@Day(PaymentReceived)<16;"Payment received on or before the 15th";"Payment received after the 15th")
   ```

---

## @DB2Schema

# @DB2Schema (Formula Language)

Given the name of a database as a text string, returns
a text string containing the DB2Â® schema
of that database if it is a db2nsf database or the empty string if
it is not a db2nsf database.

Note: This @function is new with Release 7.

## Syntax

**@DB2Schema(**  *server
: file*  **)**

**@DB2Schema(**  *server ; replicaID*  **)**

## Parameters

*server*

Text.
The name of the server.

*file*

Text.
The path and file name of the database. Specify the database path
and file name using the appropriate format for the operating system.

*replicaID*

Text.
The replica ID of the database.

## Return value

*schema*

Text. The DB2Â® schema of the nsf database indicated by *server
: file* or *server ; replicaID.* The empty string ("") is
returned if the database is a non-DB2 database. Returns an error via
@Error if:

* The server cannot be reached
* The database specified in *file* or *replicaID* cannot
  be found

## Usage

@DB2Schema
is intended to be used with Query Views, where a DB2Â® SQL query returns a result set for display.
This SQL query is an evaluated formula, and may incorporate @functions
in the query formula, the evaluation of which results in the text
string of the SQL executed in DB2Â®.
To SELECT from a DominoÂ® Access
View (DAV) within the current db2nsf's schema, the DAV table must
be qualified by the schema name. Otherwise, DB2Â® uses the accessing user's name as the schema
name. @DB2Schema allows the schema name to be dynamically specified
within a query formula.

This function also works in all contexts
where @function use is supported, including view selection formulas,
and column formulas.

## Examples

1. This formula returns the schema name FRITES if a database exists
   and is DB2Â® backed or "" if it
   is not DB2Â® backed.

   ```
   @DB2Schema( "Belgium" : "mail\\frites.nsf" )
   ```
2. This formula returns DB2Â® information
   about a database using its replica ID instead of its file name:

   ```
   @DB2Schema("Cheshire";"852556DO:00576146")
   ```
3. These formulas both return DB2Â® information
   about the current database:

   ```
   @DB2Schema(@DbName)
   ```
4. This example uses @DB2Schema in a query to pull firstname and
   lastname information from the dav1 table in the same schema as the
   current database:

   ```
   "SELECT firstname, lastname FROM " + @DB2Schema( @DbName ) + ".dav1"
   ```
5. This example uses @DB2Schema with error handling in a column formula
   to return the schema of the local db2nsf database referenced in the
   dbname field of the document:

   ```
   @If(@IsError(@DB2Schema(@DBName));"Error")
   ```
6. This example first uses @IsDB2 to find out if a connection to DB2Â® is available, and if the local
   database referenced in the dbname field of the document is a DB2Â® database, so that a more meaningful
   error message may be displayed from @DB2Schema:

   ```
   result1 := @IsDB2("":dbname);
   result2 := @DB2Schema(@DBName)
   @If(@IsError(result1);"Unable to find database or lost server connection";
   result1;@If(@IsError(result2);
   "Unable to find database or lost server connection";result2);
   "Not a DB2 database");
   ```

---

## @DbColumn (Domino® data source)

# @DbColumn (DominoÂ® data source) (Formula Language)

Returns a column of values from a view or folder in a DominoÂ® database.

## Syntax

**@DbColumn(**  *class*  **:**  *cache*  **;**  *server*  **:**  *database*  **;**  *view*  **;**  *columnNumber*  **)**

Note: The separator between the class and cache arguments as
well as the server and database arguments is a colon; the rest of
the separators are semi-colons.

## Parameters

*class*

Text.
Indicates what type of database you are accessing. You can indicate
a DominoÂ® database with either
"NotesÂ®" or "" (null string).

*cache*

String
argument. Optional. In the initial lookup, specify either "" or "NoCache."
If the former case, subsequent lookups to the same data source, you
can specify "ReCache."

* **""** (null string) caches the results of the lookup. Each
  subsequent lookup to the same location (within the same DominoÂ® session and so long as the database
  executing this lookup remains open) reuses that data until you specify
  "ReCache." Cached data improves performance and may be a good choice
  for stable data.
* **"ReCache"** refreshes the cache with the latest data from
  the database. If you want to ensure that this lookup gets the latest
  information, specify this option.
* **"NoCache"** gets the results of the lookup from the database;
  no cache is used. If you want to ensure that DominoÂ® retrieves the latest information for
  every lookup, specify this option.

*server*  **:**  *database*

Text list.
The server location and file name of the database. See "Specifying
the server and database."

*view*

Text. The name
of the view in which to search. The view name must exactly match the
full name of the view as specified in the View properties (you can
omit the alias). If the view cascades from another name on the menu,
include that name, too. See "Specifying the view name."

*columnNumber*

Number.
The column number within the view. Because Notes/Domino looks up information
in the view based on column numbers, you can only retrieve data that
actually appears in the view. See "Specifying the column number."

## Return value

*valuesFound*

Text, numbers, date-time,
or text list. The values found in the view column that you indicated.
See "Accessing the values found," later in this chapter. If the column
is re-sortable, results will depend on the current sort order.

## Specifying the server and database

There are several ways to specify
the *server*  **:**  *database* parameter:

* To perform the lookup on the current database (the same database
  in which the formula is being evaluated), specify "" as the entire
  argument to the function. "" means the local DominoÂ® directory where you are executing.
* To perform a lookup on a local database, use "" for the server
  name and specify the database name explicitly, such as "":"DATABASE.NSF."
* To perform a lookup (from the workstation) on a DominoÂ® database that resides on a server,
  include the server plus the path and file name as a text list, as
  in "SERVER":"DATABASE.NSF."
* If there are multiple copies of the database located on various DominoÂ® servers, using the database
  replica ID in place of both the server and database name allows you
  to access a replica copy of that database without having to specify
  either the server name or the database name. For example, if you use
  "85255CEB:0032AC04" (a database replica ID, found in the database
  InfoBox) as the database name, DominoÂ® uses
  a replica of the database to retrieve the information.

DominoÂ® searches
for replicas in this order, using the first replica it encounters:

* Workspace
* If there is one replica on your workspace, DominoÂ® uses it.
* If there are multiple, stacked replicas on your workspace, DominoÂ® uses the first replica
  on the stack.
* If there are multiple, unstacked replicas on your workspace, DominoÂ® looks for an icon matching
  your current server and uses that. If none of the icons matches your
  current server, DominoÂ® uses
  the icon that was added to your workspace first.
* CurrentÂ® server
* Locally (your hard disk)

  Once a replica is located, it's added
  to your workspace to save time on future lookups.

## NotesÂ®

* To avoid typing errors in the replica ID, choose File - Database
  - Design Synopsis and select Replication. You can then copy the replica
  ID from the synopsis and paste it into your formula.
* If your database is located in DOS, put a double backslash between the directory and the
  database name, as in "MAIL\\MINE.NSF", because formulas treat backslashes as "quote"
  characters.

## Specifying a view or folder

You can specify a view (or folder) parameter
using either the full name of the view or its synonym. For example,
if your Last Name view is cascaded from By Author in the View menu,
and has the synonym |LName, the name looks like this in the view InfoBox:

```
By Author\Last Name|LName
```

When
you reference this view with @DbColumn, you can simply use the LName
synonym, enclosed in quotation marks:

```
"LName"
```

If
the view name doesn't have a synonym, you can use the By Author name
plus the Last Name cascade, again enclosed in quotation marks (but
without the synonym). And since the view name is used in a formula,
the "\" must be preceded with an additional "\" to ensure that DominoÂ® interprets it correctly:

```
"By Author\\Last Name"
```

## Specifying the column number

To specify a columnNumber parameter,
you count the view columns, with the first column being column number
1. Because of the way that DominoÂ® indexes
views, however, not every column is counted for the lookup.

Use
the following method to calculate the column number for lookup purposes:

1. Count the columns in the view. Look at the view in design mode
   to make sure that you see all the columns, including columns used
   for sorting or categorizing the view.
2. Discount all columns that display a constant value, such as "Submitted
   by:" or 32. If a column contains a formula that happens to return
   the same result for every document, it is not considered a "constant",
   so be sure to include it in your column count.
3. Discount all columns that consist solely of the following @functions:
   @DocChildren, @DocDescendants, @DocLevel, @DocNumber, @DocParentNumber,
   @DocSiblings, @IsCategory, @IsExpandable.
4. Now recount the columns, working from first to last.

This revised column number is the value to specify in the
lookup formula.

If you specify a non-existent column, you don't
get an error, but rather a null value.

## Accessing the return values

If multiple values are returned by @DbColumn,
they are formatted as a list and are separated with the multivalue
separator designated for the current field in the field InfoBox.

@DbColumn
can return no more than 64K bytes of data. Use the following equations
to determine how much of your data can be returned using @DbColumn.

For
lookups that return text:

2 + (2 \* number of entries returned)
+ total text size of all entries

For lookups that return numbers
or dates:

(10 \* number of entries returned) + 6

## Usage

@DbColumn
is intended mainly for use with keyword formulas. Instead of hard-coding
a list of keywords and then periodically updating that list by re-editing
the form containing the keyword field, @DbColumn allows you to dynamically
retrieve a list of values from a database view or table.

This
function does not work in column or selection formulas, or in mail
agents.

## Server agents and security

Consider the database containing @DbColumn
as the *source* database, and the database being accessed as
the *target* database.

When you use @DbColumn in an agent,
it can access data in a target database that is running on either
the same server as the one hosting the source database or another
server. The agent signer must have at least Reader access to the target
database.

Note: Agents running on R5 or earlier servers
can only access target databases stored on the same server as the
source database. In addition, the agent signer must have at least
Reader access to the target database. The use of a replica id in the
acl is still supported in Release 6 and later. If the agent signer
is not available in the acl of a pre-Release 6 database and the replica
id is, the replica id is used instead. (You grant access to the source
database by adding the replica id of the source database, for example
85255CEB:0032AC04, to the ACL of the target database and assigning
it Reader access or higher.)

## Other agents and security

When @DbColumn is used in any other
type of formula or agent, it has unlimited access to any target database
stored on the user's own workstation. If the target database is stored
on another DominoÂ® server,
the access for @DbColumn is determined by the user's own access level
(based on the user's NotesÂ® ID).

@DbColumn
is subject to the Read Access list for a view.

## Examples

This keyword formula uses @DbColumn.
Whenever a document is composed using the form, DominoÂ® retrieves the list of product names
stored in column 2 of the Inventory On Hand view of the Inventory
database (INVENTRY.NSF). This lookup is used in a purchase requisitions
application to retrieve a current list of products available in inventory.

```
@DbColumn("";"":"INVENTRY.NSF";"Inventory On Hand";2)
```

---

## @DbColumn (ODBC data source)

# @DbColumn (ODBC data source) (Formula Language)

Uses
data source information to activate the appropriate ODBC driver. The
driver then locates the specified DBMS, table, and column, and returns
all values in that column. You can optionally specify whether the
returned list of values is sorted, whether duplicate values are deleted,
and how null values are handled.

Note: @DbColumn can
only retrieve data; it can't add, delete, or modify data.

## Syntax

**@DbColumn(
"ODBC" :**  *cache*  **;**  *data\_source*  **;**  *user\_ID1*  **:**  *user\_ID2*  **;** *password1*  **:**  *password2*  **;**  *table*  **;**  *column*  **:**  *null\_handling*  **;
"Distinct" :**  *sort*  **)**

## Parameters

**"ODBC"**

String
argument. Indicates that you are accessing an ODBC data source.

*cache*

String
argument. Optional. In the initial lookup, specify either "" or "NoCache."
If the former case, subsequent lookups to the same data source, you
can specify "ReCache."

* **""** (null string) caches the results of the lookup. Each
  subsequent lookup to the same location (within the same DominoÂ® session and so long as the database
  executing this lookup remains open) reuses that data until you specify
  "ReCache." Cached data improves performance and may be a good choice
  for stable data.
* **"ReCache"** refreshes the cache with the latest data from
  the database. If you want to ensure that this lookup gets the latest
  information, specify this option.

  Note: "ReCache" is new
  with Release 6.
* **"NoCache"** gets the results of the lookup from the database;
  no cache is used. If you want to ensure that DominoÂ® retrieves the latest information for
  every lookup, specify this option.

*data\_source*

Text. The name of the external
data source being accessed. A data source indicates the location of
one or more database tables.

See "Specifying the data source."

*user\_ID1
: user\_ID2*

Text list. The user IDs needed to connect to
the external database. You may need up to two IDs, depending on the
DBMS being accessed.

See "Specifying IDs and passwords."

*password1
: password2*

Text list. The passwords required by the user
IDs.

See "Specifying IDs and passwords."

*table*

Text.
The name of the database table being accessed.

*column*

Text.
The name of the column from which data is being retrieved.

*null\_handling*

Text.
Specifies how null values are treated when the data is retrieved.

See
"Specifying null handling."

**"Distinct"**

String
argument. Optional. Removes duplicate values from the list before
returning data.

See "Specifying Distinct."

*sort*

String
argument. Specify **"Ascending"** to sort the list of values into
ascending order before it is returned; specify **"Descending"** to
sort the list in descending order.

See "Specifying sort."

## Return value

*valuesFound*

Text, number, date-time,
or a list of these types. The values found in the *column* you
indicated.

See "Accessing the values found," later in this
chapter.

Note: If you use the option button or the
check box user interface for a keywords field, DominoÂ® updates the keyword list only when
the document is composed or is loaded for editing. If you use the
Standard user interface for the list, the keyword list is updated
every time the document is recalculated.

## Specifying the data source

The data source name can contain up to
32 alphanumeric characters.

@DbLookup can access data sources
that have already been registered in the ODBC.INI file (or similar
registry on platforms other than Windowsâ¢).

## Specifying IDs and passwords

You only need these arguments if your
DBMS requires them.

Instead of storing the IDs in the @DbColumn
formula, you can replace them with null strings (""). If an ID is
required, the user will be prompted for it. This is useful when you
do not want other designers to see IDs, or when you want users to
enter their own IDs when accessing external data. However, you must
include IDs and passwords in formulas that run automatically (such
as an agent) because these formulas don't prompt for information.

The
user IDs and passwords for accessing a data source are required only
once per DominoÂ® database
session, as long as that database remains open. If the user opens
another DominoÂ® database and
executes a formula that accesses the same data source, the user ID
and password will be required again.

Password parameters are
necessary only when ID parameters are specified. Like IDs, passwords
can either be stored in the @DbColumn formula, or prompted for by
substituting the null string. If the database password is null, you
can omit it from the formula.

For example, for the full ID/password
specification, enter:

* "";"" (two null strings, separated by a semicolon) to specify
  no ID and password, or to prompt for both
* "user\_ID1";"password1" to specify one user ID and password combination
* "user\_ID1":"user\_ID2";"password1":"password2" to specify two
  user ID and password combinations

## Specifying the table name

You can optionally include the name of the
table's owner to remove ambiguity; use the format "owner\_name.table\_name",
with a period to separate the owner name from the table name. For
example:

```
"dbo.author"
```

*Table* can
also refer to a database view in the DBMS being accessed.

## Specifying null handling

Normally, null values are ignored and the
resulting list is shortened (same as using the Discard option as described
next).

To control how null values are handled,
specify one of the following, appended to the *column* parameter
with a colon:

* "Fail" generates this error message if the column of data contains
  any null values:

  ```
  Null values found - canceling @Db function
  ```

  No
  data is returned with the message.
* "Discard" discards the null values, thus shortening the returned
  list of values. If one or more values are discarded when the @DbColumn
  formula is executed, you see this message on the status bar:

  ```
  Caution: NULL values discarded from @Db list.
  ```
* "*Replacement value*" specifies a replacement value for null
  values. The replacement value must be a quoted string, but if the
  column is numeric or date-time, the string must be convertible to
  that type.

  If your formula includes a sort string argument, the
  list of values to be returned is sorted *before* the replacement
  values are inserted. During sorting, all null values are placed at
  the beginning of the list for an ascending sort; and at the end for
  a descending sort. They are not replaced until sorting is complete.
  This can result in a list that has some values sorted incorrectly.
  For example, if you specify "zzz" as your replacement value, all the
  "zzz" values will appear at the beginning of the list, even if you
  sorted it in ascending order.

  If one or more values are replaced
  when the @DbColumn formula is executed, you see this message on the
  status bar:

  ```
  Caution: NULL value replaced with user-defined value in @Db list
  ```

  Generally,
  the replacement value should be one that is not likely to appear in
  the list as valid data; for example, if the column is text, your replacement
  value might be "\*\*\*" so that you can easily find those values in DominoÂ®.

## Specifying Distinct

The Distinct string argument is similar to @Unique
in DominoÂ®, except that Distinct
ensures that duplicate values are removed *before* the data is
returned. Using Distinct instead of @Unique has two advantages:

* The formula operates more quickly because the additional work
  is performed outside of DominoÂ®.
* You can potentially retrieve a larger amount of useful data into DominoÂ® -- since the duplicate
  values are removed at the back-end, more unique values can be returned
  to DominoÂ®.

Note: Distinct is not supported by all ODBC drivers.
If there are null values in the data and you specify Distinct, one
null is usually returned.

## Specifying sort

If you use the Distinct string argument, you can append
the *sort* parameter to it with a colon. Use one of these keywords
for the *sort* parameter to specify sorting of the return values:

* Ascending sorts the list in ascending order.
* Descending sorts the list in descending order.

If no sort string argument is specified, values are returned
in arbitrary order.

Note: The *sort* keywords
are not supported by all ODBC drivers. If you attempt to use both
Ascending and Descending in your formula, you see an "Invalid argument"
message.

If multiple values are returned, they are formatted
as a list and are separated with the multi-value separator designated
for the current field.

@DbColumn can return no more than 64KB
of data. Use the following equations to determine how much of your
data can be returned with @DbColumn.

* For lookups that return text:

  2 + (2 \* number of entries returned)
  + total text size of all entries

  Each text string is limited
  to 511 bytes; if only one text string is returned, it is limited to
  64KB.
* For lookups that return numbers or dates:

  (10 \* number of entries
  returned) + 6

If the user's NOTES.INI file includes the statement

```
NoExternalApps=1
```

the
@DbColumn formula is disabled. The user will not see an error message;
the formula fails to execute. This applies to @DbColumn only when
you use it with ODBC.

## Usage

@DbColumn
is intended mainly for keyword formulas. Instead of hard-coding a
list of keywords and then periodically updating that list, @DbColumn
allows you to dynamically retrieve a list of values from an external
database table.

This function only works in Web applications
if the remote server hosting the table from which data is being retrieved
exists on the same machine as the DominoÂ® server,
which is rarely the case.

## Examples

1. This formula retrieves from the inventory database the complete
   list of colors in which your company's uniforms are available. The
   data is stored like this:

   | Item | Size | Color |
   | --- | --- | --- |
   | Shirt | Small | Red |
   | Skirt | Small | Green |
   | Sweater | Medium | Red |
   | Trousers | Medium | Yellow |

   Use @DbColumn to retrieve the entire contents of the Color
   column (column 3):

   ```
   @DbColumn("ODBC";"INVENTORY";"";"";"UNIFORMS";"Color")
   ```

   Values in the
   resulting list appear just as they were encountered in the database;
   they are not sorted and duplicate values are retained:

   ```
   Red:Green:Red:Yellow
   ```
2. This example uses the sample "pubs" database that is included
   with Microsoftâ¢ SQL Server.
   The formula uses the ODBC SQL Server driver to access the database,
   locate the table called "authors" that is owned by user "dbo," and
   then retrieve the list of names in the "au\_lname" column. The author
   names are sorted in ascending order; null values are discarded.

   ```
   @DbColumn("ODBC";"PUBLISHERS";"dbo";"vanilla";
   "dbo.authors"; "au_lname":"Discard";"Ascending")
   ```

---

## @DbCommand(Domino® data source)

# @DbCommand (DominoÂ® data source) (Formula Language)

Accesses view and folder information from a DominoÂ® database in Web applications.

Note: This @function is new with Release 6.

## Syntax

**@DbCommand(
"DominoÂ®" ; "ViewNextPage"
)**

**@DbCommand( "DominoÂ®"
; "ViewPreviousPage" )**

**@DbCommand( "DominoÂ®" ; "FolderList" ;**  *promptString*  **;**  *foldersToExclude* **)**

## Parameters

**"DominoÂ®"**

String argument.
Indicates that you want to access a DominoÂ® data
source.

**"ViewNextPage"**

String argument. Displays
the next chunk of documents in an embedded view.

**"ViewPreviousPage"**

String
argument. Displays the previous chunk of documents in an embedded
view.

**"FolderList"**

String argument. Indicates
that you want to display a list of the names of folders in the database
that are accessible from the Web.

*promptString*

String.
Optional. Use only if including **"FolderList"** string argument.
String to display as the first choice in a Listbox field. If you want
the first choice in the list to be "Select a folder," specify:

```
@DbCommand("Domino" ; "FolderList" ; "Select a folder")
```

*foldersToExclude*

Textlist.
Optional. Use only if including **"FolderList"** string argument.
Names of the folders you do not want to display in the listbox field.
If you do not want the "MyStuff" and "Problems" folders to be included
in the list, specify:

```
@DbCommand("Domino" ; "FolderList" ; "Select a folder" ; "MyStuff" : "Problems")
```

## Usage

You
cannot use this function to access a DominoÂ® data
source in the NotesÂ® client.

Use
the FolderList string argument with the @DbCommand in a selection
formula for a Listbox field that is set to Use formula for choices
to display a list of available folders in a Web application. If no
folders exist, the Listbox field is empty when it displays and the *promptString* does
not display in it either.

You can use the [FolderDocuments @command](H_FOLDERDOCUMENTS_COMMAND.html "Moves or copies the selected document to a folder.") with
the FolderList string argument to copy or move a selected document
in an embedded view that has HTML selection enabled into the folder
selected from the Listbox field. To do so, complete these steps:

1. Give the Listbox field that uses the @DbCommand the reserved name
   $$SelectDestFolder.
2. Set the view that is being embedded into the form to Allow selection
   of documents on the Advanced tab of its View Properties box.
3. Edit the EmbeddedView Properties box by setting the Display property
   on the Info tab to Using HTML and selecting Show Selection Margin
   on the Display tab.
4. Add an action button to the form with the following formula: @Command([FolderDocuments];"";"0").
   When clicked, the document currently selected in the embedded view
   is copied to the folder currently selected in the $$SelectDestFolder
   Listbox field. Replace "0" with "1" to move the selected document
   instead of copying it.

The **"ViewNextPage"** and **"ViewPreviousPage"** string
arguments are useful when your form has an embedded view that contains
several documents. By adding Next and Previous actions to the form
that contains @DbCommand functions with these keywords, you can display
the documents in manageable chunks. Set the Embedded View Properties
box options as follows:

1. Set the Web access Display setting to Using HTML.
2. Deselect Use default.
3. Select a number in the Lines to display field.

## Examples

1. This code, when added as the selection formula to a Listbox field
   that is set to Use formula for choices on the Control tab of the Field
   Properties box, displays a list of all the folders in a database.
   -Select a folder- appears as the first option in the resulting Listbox.

   ```
   @DbCommand("Domino";"FolderList";"-Select a folder-")
   ```

   If
   you name the Listbox field $$SelectDestFolder, the following code,
   when added to the "Move the Folder" action button, moves the document
   selected in the embedded HTML view into the folder selected from the
   Listbox field.

   ```
   @Command([FolderDocuments];"";"1")
   ```
2. This code, when added as the selection formula to a Listbox field
   that is set to Use formula for choices on the Control tab of the Field
   Properties box, displays a list of the folders in the database. However,
   it prevents the "Private" and "Manager" folders from displaying in
   the resulting listbox.

   ```
   @DbCommand("Domino;"FolderList";"Choose a folder";"Private":"Manager")
   ```
3. On a form that has an embedded view that contains 50 documents,
   this formula, when added as the code for the Next action button, displays
   documents 11-20 if the Lines to display field on the Info tab of the
   Embedded View Properties box is set to 10.

   ```
   @DbCommand("Domino";"ViewNextPage")
   ```

   You
   can also add an action button called Previous that contains the following
   code. When a user clicks this button, the previous block of pages
   displays in the embedded view.

   ```
   @DbCommand("Domino";"ViewPreviousPage")
   ```

---

## @DbCommand(ODBC data source)

# @DbCommand (ODBC data source) (Formula Language)

Given data source information from the ODBC.INI file (or
equivalent), uses this information to activate the appropriate ODBC
driver. The driver then locates the specified DBMS, passes the specified
command to it for processing, and returns the data retrieved by that
command.

Note: @DbCommand only works with ODBC data sources
and only with SELECT statements. If used with statements that don't
retrieve a result set, @DbCommand simply transmits the statement.
Use the ODBC capabilities of LotusScriptÂ® for
more extensive interaction.

## Syntax

**@DbCommand(
"ODBC" :**  *cache*  **;**  *data\_source*  **;**  *user\_ID1*  **:**  *user\_ID2*  **;** *password1*  **:**  *password2*  **;**  *command\_string*  **:**  *null\_handling* **)**

## Parameters

**"ODBC**"

String
argument. Indicates that you are accessing an ODBC data source.

*cache*

String
argument. Optional. In the initial lookup, specify either "" or "NoCache."
If the former case, subsequent lookups to the same data source, you
can specify "ReCache."

* **""** (null string) caches the results of the lookup. Each
  subsequent lookup to the same location (within the same DominoÂ® session and so long as the database
  executing this lookup remains open) reuses that data until you specify
  "ReCache." Cached data improves performance and may be a good choice
  for stable data.
* **"ReCache"** refreshes the cache with the latest data from
  the database. If you want to ensure that this lookup gets the latest
  information, specify this option.

  Note: "ReCache" is new
  with Release 6.
* **"NoCache"** gets the results of the lookup from the database;
  no cache is used. If you want to ensure that DominoÂ® retrieves the latest information for
  every lookup, specify this option

Text. The name of the external data source being accessed.
A data source indicates the location of one or more database tables.
See "Specifying the data source."

*user\_ID1* : *user\_ID2*

Text
list. The user IDs needed to connect to the external database. You
may need up to two IDs, depending on the DBMS being accessed. See
"Specifying IDs and passwords."

*password1 : password2*

Text
list. The passwords required by the user ID(s). See "Specifying IDs
and passwords."

*command\_string*

Text. An SQL
statement, command statement, or name of a procedure to be executed.
See "Specifying a command string."

*null\_handling*

Text.
Specifies how null values are treated when the data is retrieved.
See "Specifying null handling."

## Return value

*valuesFound*

Text, number, date-time,
or a list of these types. The values returned by the *command\_string*.
See "Accessing values found."

Note: If you use the
option button or the check box user interface for a keywords field, DominoÂ® updates the keyword list
only when the document is composed or is loaded for editing. If you
use the Standard user interface for the list, the keyword list is
updated every time the document is recalculated.

## Specifying the data source

The data source name can contain up to
32 alphanumeric characters.

@DbCommand can access data sources
that have already been registered in the ODBC.INI file (or similar
registry on platforms other than Windowsâ¢).

## Specifying IDs and passwords

You only need these arguments if your
DBMS requires them.

Instead of storing the IDs in the @DbCommand
formula, you can replace them with null strings (""). If an ID is
required, the user will be prompted for it. This is useful when you
do not want other designers to see IDs, or when you want users to
enter their own IDs when accessing external data. However, you must
include IDs and passwords in formulas that will run automatically
(such as an agent) because these formulas don't prompt for information.

The
user IDs and passwords for accessing a data source are required only
once per DominoÂ® database
session as long as that database remains open. If the user opens another DominoÂ® database and executes
a formula that accesses the same data source, the user ID and password
will be required again.

Password parameters are necessary only
when ID parameters are specified. Like IDs, passwords can either be
stored in the @DbColumn formula, or prompted for by substituting the
null string. If the database password is null, you can omit it from
the formula.

For example, for the full ID/password specification,
enter:

* "";"" (two null strings, separated by a semicolon) to specify
  no ID and password, or to prompt for both
* "user\_ID1";"password1" to specify one user ID and password combination
* "user\_ID1":"user\_ID2";"password1":"password2" to specify two
  user ID and password combinations

Note: For complex connections, additional ID and
password parameters may be required to connect to the data source.

## Specifying the command string

The *command\_string* can be any
of the following:

* An SQL statement (it must use the SQL syntax accepted by the back-end
  DBMS).
* A command statement using the back-end DBMS command language.
* The name of a procedure stored within the back-end DBMS (the procedure
  contains one or more command strings that are activated when the procedure
  is called by @DbCommand).

A date-time value must be entered in the format of the database,
not that of DominoÂ®; for example,
use 1996-01-31-12.00.00 for DB2/2, not 1996-01-31-12:00:00.

## Specifying null handling

To control how null values are handled, specify
one of the following, appended to the *command\_string* parameter
with a colon:

* "Fail" generates this error message if the column of data contains
  any null values:

  ```
  Null values found - canceling @Db function
  ```

  No
  data is returned with the message.
* "Discard" discards the null values, thus shortening the returned
  list of values. If one or more values are discarded when the @DbCommand
  formula is executed, you see this message on the status bar:

  ```
  Caution: NULL values discarded from @Db list.
  ```
* "Replacement value" specifies a replacement value for null values.
  The replacement value must be a quoted string, but if the column is
  numeric or date-time, the string must be convertible to that type.
* If your command string includes a sort string argument, the list
  of values to be returned is sorted *before* the replacement values
  are inserted. During sorting, all null values are placed at the beginning
  or end of the list, depending on the driver. They are not replaced
  until sorting is complete. This can result in a list that has some
  values sorted incorrectly.

  If one or more values are replaced when
  the @DbCommand formula is executed, you see this message on the status
  bar:

  ```
  Caution: NULL value replaced with user-defined value in @Db list.
  ```

  Generally,
  the replacement value should be one that is not likely to appear in
  the list as valid data; for example, if the column is text, your replacement
  value might be "\*\*\*" so that you can easily find those values.

## Accessing values found

@DbCommand can return no more than 64KB of
data. Use the following equations to determine how much of your data
can be returned with @DbCommand.

* For lookups that return text:

  2 + (2 \* number of entries returned)
  + total text size of all entries

  Each text string is limited
  to 511 bytes; if only one text string is returned, it is limited to
  64KB.
* For lookups that return numbers or dates:

  (10 \* number of entries
  returned) + 6

If the user's NOTES.INI file includes the statement

```
NoExternalApps=1
```

the
@DbCommand formula is disabled. The user will not see an error message;
the formula fails to execute.

## Usage

@DbCommand
is useful for testing a non-equal relationship (such as less-than),
or for testing multiple conditions at the same time. To use @DBCommands,
pass a command to the back-end database for processing.

For
example, to return data from records where:

```
BALANCE >= 1000.00 and DAYS_OVERDUE > 30
```

Write
the selection statement in SQL, and then use @DbCommand to pass that
statement to the DBMS for processing; @DbCommand then returns the
requested data.

For Web applications, you can use this function
only with the syntax:

```
@DbCommand("Domino";"ViewNextPage")
```

or

```
@DbCommand("Domino";"ViewPreviousPage")
```

to
create a link to the next or previous page in a view. You cannot use
@DbCommand in any other context with Web applications.

Note: In a Web application, this command acts on an embedded
view when it is called from an action on a page or document.

## Examples

This formula uses the sample "pubs"
database that is included with Microsoftâ¢ SQL
Server. The formula uses an ODBC driver to access the data source
called PUBLISHERS, locate the table called "authors" that is owned
by user "dbo," and then retrieve the list of names in the "au\_lname"
column for those authors who live in California and have a contract.
The string CA is enclosed in single quotation marks, since it is already
embedded within a quoted command string.

```
@DbCommand("ODBC";"PUBLISHERS";"dbo":"";"vanilla":"";   
"SELECT au_lname FROM dbo.authors WHERE contract=1 AND state='CA' ")
```

---

## @DbExists

# @DbExists (Formula Language)

Given a server and file name, or replica ID, indicates
whether the specified database exists.

## Syntax

**@DbExists(**  *server*  **:**  *file*  **)**

**@DbExists(**  *server*  **;**  *replicaID*  **)**

## Parameters

*server*

Text.
The name of the server. Use an empty string ("") to indicate the local
computer.

*file*

Text. The path and file name
of the database. Specify the database path and file name using the
appropriate format for the operating system.

*replicationID*

Text.
The replica ID of the database.

## Return value

*flag*

Number.

* Returns 1 (True) if the database exists.
* Returns 0 (False) if it does not exist.

## Usage

This
function does not work in column or selection formulas, or in agents
that run on a server (mail and scheduled agents).

## Examples

1. This formula returns 1 if FRITES.NSF is in the MAIL directory
   on the server Belgium. Otherwise it returns 0.

   ```
   @DbExists( "Belgium" : "mail\\frites.nsf" )
   ```
2. This formula checks if a database exists before opening it on
   the workspace.

   ```
   server := @Subset( @MailDbName; 1 );
   file := "mail\\blah.nsf";
   @If( @DbExists( server : file ) ; @PostedCommand([FileOpenDatabase]; server : file ); @Prompt([OK]; "Sorry"; "The database cannot be located on your home server." ) )
   ```
3. This formula uses a database's replica ID instead of its file
   name:

   ```
   Exists := @DbExists("Cheshire";"852556DO:00576146");
   ```

---

## @DbLookup (Domino® data source)

# @DbLookup (DominoÂ® data source) (Formula Language)

Given a key value, looks in the specified view (or folder)
and finds all documents containing the key value in the first sorted
column within the view. For each selected document, @DbLookup returns
either the contents of a specified column in the view, or the contents
of a specified field.

## Syntax

**@DbLookup(**  *class*  **:**  *cache*  **;**  *server*  **:**  *database*  **;**  *view*  **;**  *key*  **;**  *fieldName*  **;**  *keywords*  **)** or **@DbLookup(**  *class*  **:**  *cache*  **;**  *server*  **:**  *database*  **;**  *view*  **;**  *key*  **;**  *columnNumber*  **;**  *keywords* **)**

Note: The separator between the class and the cache string arguments
as well as the server and database are colons; the rest of the separators
are semicolons.

## Parameters

*class*

Text.
Indicates what type of database you are accessing. You can indicate
a DominoÂ® database with either **""** or **"Notes."**

*cache*

String
argument. Optional. In the initial lookup, specify either "" or "NoCache."
If the former case, subsequent lookups to the same data source, you
can specify "ReCache."

* **""** (null string) caches the results of the lookup. Each
  subsequent lookup to the same location (within the same DominoÂ® session and so long as the database
  executing this lookup remains open) reuses that data until you specify
  "ReCache." Cached data improves performance and may be a good choice
  for stable data.
* **"ReCache"** refreshes the cache with the latest data from
  the database. If you want to ensure that this lookup gets the latest
  information, specify this option.
* **"NoCache"** gets the results of the lookup from the database;
  no cache is used. If you want to ensure that DominoÂ® retrieves the latest information for
  every lookup, specify this option.

Note: "NoCache" ignores the lookup cache. If the
same lookup was already cached, the cache is not updated. "ReCache"
does not use the cached result, it stores its result in the cache
when it is done. If you later do the same lookup with default caching,
you get the result that was stored by "ReCache."

*server* **:**  *database*

Text
list. The server location and file name of the database. See "Specifying
the server and database."

*view*

Text. The name
of the view or folder in which to search. The view name must exactly
match the view's full name as specified in the view InfoBox (you can
omit any synonyms). If the view cascades from another name on the
menu, include that name too. See "Specifying the view."

*key*

Text,
number, date/time, or a list of any of these types. Determines which
document is actually read in order to retrieve a value. A document's
key is the value displayed in the first *sorted* column within
the view. Note that if the column is re-sortable, results will depend
on the current sort order. See "Specifying a key."

*fieldName*

Text.
The name of the field from which the data will be retrieved, once
the correct document has been identified. See "Specifying a field
name."

*columnNumber*

Number. When you use a
column number, DominoÂ® finds
all documents in the view that match the specified key, and returns
whatever value is *displayed* in the indicated column for each
of those documents, regardless of the formula used to display the
data. See "Specifying the column number."

*keywords*

Note: This parameter is new with Release 6.

Keyword.
Optional. Keywords can be concatenated.

* **[FAILSILENT]** returns "" (null string) instead of an error
  if the key cannot be found.
* **[PARTIALMATCH]** returns a match if the key matches the
  beginning characters of the column value.
* **[RETURNDOCUMENTUNIQUEID]** returns the UNID of the document
  instead of a field or column value.

## Return value

*valuesFound*

Text, numbers, date-time,
or text-list. The values found in the *fieldName* or *column* you
indicated, or the UNID of the document. See "Accessing the return
values."

If no documents in the first sorted column match the
key, @DbLookup returns an error, "Entry not found in index", which
you can test for with @IsError or @IfError.

## Specifying the server and database

There are several ways to specify
the *server*  **:**  *database* parameter:

* To perform the lookup on the current database (the same database
  in which the formula is being evaluated), specify "" as the entire
  argument to the function. "" means the local DominoÂ® directory where you are executing.
* To perform a lookup on a local database, use "" for the server
  name and specify the database name explicitly, such as "":"DATABASE.NSF."
* To perform a lookup (from the workstation) on a DominoÂ® database that resides on a server,
  include the server plus the path and file name as a text list, as
  in "SERVER":"DATABASE.NSF."
* If there are multiple copies of the database located on various DominoÂ® servers, using the database
  replica ID in place of both the server and database name lets you
  access a replica copy of that database without having to specify either
  the server name or the database name. For example, if you use "85255CEB:0032AC04"
  (a database replica ID, found in the database InfoBox) as the database
  name, DominoÂ® uses a replica
  of the database to retrieve the information.

  DominoÂ® searches for replicas in this order,
  using the first replica it encounters:

  + Workspace

    If there is one replica on your workspace, DominoÂ® uses it.

    If there
    are multiple, stacked replicas on your workspace, DominoÂ® uses the first replica on the stack.

    If
    there are multiple, unstacked replicas on your workspace, DominoÂ® looks for an icon matching
    your current server and uses that. If none of the icons matches your
    current server, DominoÂ® uses
    the icon that was added to your workspace first.
  + CurrentÂ® server
  + Locally (your hard disk)

  Once a replica is located, it's added to your workspace to
  save time on future lookups.

## NotesÂ®

* To avoid typing errors in the replica ID, choose File - Database
  - Design Synopsis and select Replication. Then copy the replica ID
  from the synopsis and paste it into your formula.
* If your database is located in DOS, put a double backslash between the directory and the
  database name, as in "MAIL\\MINE.NSF" because formulas treat single backslashes as escape
  characters.

## Specifying a view

You can specify a view parameter using either the
full name of the view (or folder) or its synonym. For example, if
your Last Name view is cascaded from By Author in the View menu, and
has the synonym |LName, it looks like this in the view InfoBox:

By
Author\Last Name|LName

When you reference this view with @DbLookup,
you can just use the LName synonym, enclosed in quotation marks:

"LName"

If
the view name doesn't have a synonym, you use the By Author name plus
the Last Name cascade, again enclosed in quotation marks (but without
the synonym). And since the view name is used in a formula, the "\"
must be preceded with an additional "\" to ensure that DominoÂ® interprets it correctly:

**"**By
Author\\Last Name**"**

## Specifying a key

You can only test for values that match the key (equality);
there is no way to specify a different operator such as < (less-than).

In
addition to specifying a constant as the key to be matched, you can
also use the value of an editable field. For example, you could create
a ContactInfo form that contains two fields: a contactName field and
a lookupComments field. You want a user to be able to enter a contact
name in the contactName field and have the lookupComments field display
a list of comments associated with the contact that the user supplied.
To do so, you could make the contactName field an editable text field
(or a choice list field such as a Dialog list field). The lookupComments
field could contain the following code as its Input validation formula:

@DbLookup("":"NoCache";"Sales":"Customers.nsf";"ContactList";contactName;"Comments")

When
a user enters or chooses the customer name, "Susie Queue," for instance,
in the contactName field of the ContactInfo form and presses F9 to
refresh the document, the formula in the lookupComments field performs
these tasks:

* Finds the ContactList view of the NotesÂ® database
  Customers.nsf on the Sales server.
* Locates the first sorted column containing the key "Susie Queue."
* Extracts the text strings displayed in the Comments column for
  each document containing the "Susie Queue" key.
* Returns the extracted list of comments to the lookupComments field.
  If more than one document was accessed, the strings returned are separated
  by a semicolon.

By specifying the field contactName as the key, whenever
the @DbLookup formula is executed, the current value of the contactName
field is used as the lookup criterion.

The view must contain
a sorted column in order for the lookup to work; otherwise a null
value is returned. Results are not accurate for a multi-value field
that is sorted but not categorized. If the column can be re-sorted,
results will depend upon the current sort order.

The type of
the key must match the type of the data in the sorted column, or no
match will be found.

The match between the lookup key and the
value in the sort column must be exact -- capitalization doesn't matter,
but spacing and punctuation must be precise. The match must be complete
unless you specify the [PARTIALMATCH] keyword.

When using a
number or date/time key, the key value must match the column value
exactly. Remember that the column "style" settings can be used to
hide information; so for instance, the column might contain a date
and time, but only display the date. To lookup in that column, you
have to supply an exactly matching date/time value, not just the date
that is visible.

When your key is a list of values, the result
is the list-concatenation of all the matching row entries. For example,
if lookup of key "Sam" returns 7 and "George" returns 19 : 4, then
the key "Sam":"George" would return 7:19:4.

When using a list
of keys, if the first key doesn't match a view entry, the lookup fails,
even if other keys do match. If the first key finds a match, however,
subsequent non-matching keys will just be ignored; they won't cause
an error.

## Specifying a field name

When you use a *fieldName* to perform
a lookup, the value returned is the value that is actually stored
in the field; it may be different from what displays in the view. DominoÂ® can retrieve data from
any field in any document displayed in the specified view, but if
the field isn't displayed as a view column, DominoÂ® must search the entire document to
find the field, which may result in a slower lookup. You cannot retrieve
data from a rich text field using @DbLookup.

Some of the documents
matching the key may not even contain the specified field if they
were created using different forms.

Note: If you supply
a string value to the *fieldname* value, you are first looking
for the *column name* (also known as the *Programmatic Name)* which
you can find on the Advanced tab of the column properties. If there
is no matching column, look for an item with the specified name. If
you use the Advanced tab of the column properties to discover the
name of the column you would like to read in your lookup, you can
use that column name instead of the column number. To get the best
performance from @DbLookup, change the programmatic name of the column
to some unique name that describes its contents, then use that name
instead of the column number as your lookup argument.

## Specifying the column number

Lookups based on view columns are more
efficient than those based on fields not included in the view. For
best results, you should include the desired field in the view.

For
example, if your view is categorized by product ID and you specify
"01776" as the lookup key and 2 as the column, DominoÂ® returns whatever is displayed in column
2 for all documents categorized under product ID 01776.

To
specify a columnNumber parameter, you count the view's columns from
left to right, with the leftmost column being number 1. Because of
the way DominoÂ® indexes views,
however, not every column is counted for the lookup.

Use this
method to calculate the column number for lookup purposes:

1. Count the columns in the view, from left to right.

   Be sure
   you don't miss any columns, for example, a column used for sorting
   or categorizing the view may not show up. Look at the view in design
   mode to make sure you see all its columns.
2. Discount all columns that display a constant value, such as 32
   or "Submitted by: ." If a column contains a formula that happens to
   return the same result for every document, it is not considered a
   "constant" so be sure to include it in your column count.
3. Discount all columns that consist solely of the following @functions:
   @DocChildren, @DocDescendants, @DocLevel, @DocNumber, @DocParentNumber,
   @DocSiblings, @IsCategory, @IsExpandable.
4. Now recount the columns, working from left to right.

   This revised
   column number is the value to specify in the lookup formula.

Note: If you choose to use a column number instead
of a field name in an @DbLookup formula, you can only retrieve data
that actually appears in the view.

## Accessing the return values

If multiple values are returned by @DbLookup,
they are formatted as a list and are separated with the multi-value
separator designated in the current field's InfoBox.

@DbLookup
can return no more than 64KB of data. Use the following equations
to determine how much of your data can be returned with @DbLookup.

For
lookups that return text:

2 + (2 \* number of entries returned)
+ total text size of all entries

For lookups that return numbers
or dates:

(10 \* number of entries returned) + 6

## Usage

This
function does not work in column or selection formulas, or in mail
agents.

## Server agents and security

Consider the database containing @DbLookup
the *source* database, and the database being accessed the *target* database.

When
you use @DbLookup in an agent, it can access data in a target database
that is running on either the same server as the one hosting the source
database or another server. The agent signer must have at least Reader
access to the target database.

Note: Agents running
on R5 or earlier servers can only access target databases stored on
the same server as the source database. In addition, the agent signer
must have at least Reader access to the target database. The use of
a replica id in the acl is still supported in Release 6. If the agent
signer is not available in the acl of a pre-Release 6 database and
the replica id is, the replica id is used instead. (You grant access
to the source database by adding the replica id of the source database,
for example 85255CEB:0032AC04, to the ACL of the target database and
assigning it Reader access or higher.)

## Other agents and security

When @DbLookup is used in any other
type of formula or agent, it has unlimited access to any target database
stored on the user's own workstation. If the target database is stored
on another DominoÂ® server,
@DbLookup's access is determined by the agent signer's access level
(based on the user's NotesÂ® ID).

@DbLookup
is subject to the Read Access list for a view.

## Examples

1. Your organization maintains employee office location and department
   information in the Person documents in the public Name & Address
   Book.

   You might have a Purchasing application where employees fill
   out Purchase Requests for office supplies. You can have your NotesÂ® application look up this
   information and automatically insert it into documents.

   Mary
   Tsen composes a Purchase Order. The P.O. Number, Date, and Requested
   By fields are filled in automatically by NotesÂ®. Mary fills in the details of the purchase
   order: quantity, part number, and so on.

   When Mary saves the
   Purchase Order, the delivery information in the document is calculated
   using a series of @DbLookup formulas to retrieve information about
   that user from the public Name & Address Book:

   This is
   accomplished by using computed fields and writing a lookup formula
   for each field to be retrieved (Location and Telephone). For example,
   the formula for the Location field would be:

   ```
   @DbLookup("";"Purchasing":"Names.NSF";"People"; @Right(RequestedBy; "");"Location")
   ```

   This
   formula instructs DominoÂ® to
   open the Name & Address Book (Names.NSF) on the Purchasing server,
   locate the People view, and then locate the person whose last name
   matches the last name in the purchase order's RequestedBy field. Once
   the correct document has been located, DominoÂ® copies the information from the Person
   document's Location field into the purchase order Location field.

   A
   similar formula then copies Mary's telephone number from the Person
   record OfficePhoneNumber field into the purchase order Phone field.

   Note: For the DeliverTo field, Mary's name is determined when
   the document is composed, using @UserName.
2. Using the Name & Address Book again, you want to retrieve
   a list of office telephone numbers for everyone in the Purchasing
   department.

   You could use @DbLookup with the key "Purchasing" to
   retrieve the OfficePhoneNumber field, and NotesÂ® would return the telephone number for
   every employee with "Purchasing" entered in the Department field of
   their Person record. The phone numbers are returned as a text list,
   using the selected multivalue separator for the field.
3. This formula returns the value stored in the Status field of the
   Virus Check document, which is accessed via the In Progress view of
   the PROJECTS.NSF database stored in the SMITH subdirectory on the
   RESEARCH server. The information will not be cached, so if this formula
   is evaluated again during the same NotesÂ® session,
   a new lookup will be performed to ensure that the status retrieved
   is up to date.

   ```
   @DbLookup("":"NoCache";"RESEARCH":"SMITH\\PROJECTS.NSF"; "In Progress";"Virus Check";"Status")
   ```

---

## @DbLookup (ODBC data source)

# @DbLookup (ODBC data source) (Formula Language)

Uses data source information from the ODBC.INI file to
activate the appropriate ODBC driver. The driver then locates the
specified DBMS, table, and column, and returns only the values in
that column belonging to records whose value in the key column matches
the specified key. You can optionally specify whether the returned
list of values is sorted, whether duplicate values are deleted, and
how null values are handled.

Note: @DbLookup can only retrieve data; it can't
add, delete, or modify data.

## Syntax

**@DbLookup(
"ODBC" :**  *cache*  **; "** *data\_source* **" ;
"** *user\_ID1* **" : "** *user\_ID2* **" ;** **"** *password1* **"
: "** *password2* **" ; "** *table* **" ; "** *column* **"
: "** *null\_handling* **" ; "** *key\_column* **" ;
"** *key* **" ; "Distinct" : "** *sort* **" )**

## Parameters

**"ODBC"**

String
argument. Indicates that you are accessing an ODBC data source.

*cache*

String
argument. Optional. In the initial lookup, specify either "" or "NoCache."
If the former case, subsequent lookups to the same data source, you
can specify "ReCache."

* **""** (null string) caches the results of the lookup. Each
  subsequent lookup to the same location (within the same DominoÂ® session and so long as the database
  executing this lookup remains open) re-uses that data until you specify
  "ReCache." Cached data improves performance and may be a good choice
  for stable data.
* **"ReCache"** refreshes the cache with the latest data from
  the database. If you want to ensure that this lookup gets the latest
  information, specify this option.

  Note: "ReCache" is new
  with Release 6.
* **"NoCache"** gets the results of the lookup from the database;
  no cache is used. If you want to ensure that DominoÂ® retrieves the latest information for
  every lookup, specify this option.

"*data\_source*"

Text. The name of the external
data source being accessed. This name is specified as the dsn (data
source name) in the Data Source Administrator or the odbc.ini file.
A data source indicates the location of one or more database tables.
See "Specifying the data source."

"*user\_ID1*" :
"*user\_ID2*"

Text-list. The user IDs needed to connect
to the external database. You may need up to two IDs, depending on
the DBMS being accessed. See "Specifying IDs and passwords."

"*password1*" *:* "*password2*"

Text
list. The passwords required by the user IDs. See "Specifying IDs
and passwords."

"*table*"

Text. The name of the
database table being accessed.

"*column*"

Text.
The name of the column from which data is being retrieved.

"*null\_handling*"

Text.
Specifies how null values are treated when the data is retrieved.
See "Specifying null handling."

"*key\_column*"

Text.
The name of the column used for key matching.

"*key*"

Text,
number, or date-time, or a list. The value to be looked up in *key\_column*.
Use the NotesÂ® type that agrees
with the type of the key column in the data source.

**"Distinct"**

String
argument. Optional. Removes duplicate values from the list before
returning data. See "Specifying Distinct."

"*sort*"

String
argument. Sorts the list of values into either ascending or descending
order before it is returned. See "Specifying sort."

## Return value

*valuesFound*

Text, number, date-time,
or a list of these types. The values found in the *column* you
indicated. See "Accessing the values found," later in this chapter.

Note: If you use the option button or the check box user interface
for a keywords field, DominoÂ® updates
the keyword list only when the document is composed or opened for
editing. If you use the Standard user interface for the list, the
keyword list is updated every time the document is recalculated.

## Specifying the data source

The data source name can contain up to
32 alphanumeric characters.

## Specifying IDs and passwords

You only need these arguments if your
DBMS requires them.

Instead of storing the IDs in the @DbLookup
formula, you can replace them with null strings (""). If an ID is
required, the user will be prompted for it. This is useful when you
do not want other designers to see IDs, or when you want users to
enter their own IDs when accessing external data. However, you must
include IDs and passwords in formulas that will run automatically
(such as an agent) because those formulas don't prompt for information.

The
user IDs and passwords for accessing a data source are required only
once per DominoÂ® database
session as long as that database remains open. If the user opens another DominoÂ® database and executes
a formula that accesses the same data source, the user ID and password
will be required again.

Password parameters are necessary only
when ID parameters are specified. Like IDs, passwords can either be
stored in the @DbLookup formula, or prompted for by the ODBC driver
by substituting the null string. If the database password is null,
you can omit it from the formula.

For example, for the full
ID/password specification, enter:

* "";"" (two null strings, separated by a semicolon) to specify
  no ID and password, or to prompt for both
* "user\_ID1";"password1" to specify one user ID and password combination
* "user\_ID1":"user\_ID2";"password1":"password2" to specify two user
  ID and password combinations

## Specifying the table name

If the DBMS supports it, you can optionally
include the name of the table's owner to remove ambiguity. Use the
format "owner\_name.table\_name", with a period separating the owner
name and the table name.

For example:

```
"dbo.author"
```

*Table* can
also refer to a database view in the DBMS being accessed.

## Specifying null handling

To control how null values are handled, specify
one of the following, appended to the *column* parameter with
a colon:

* "Fail" generates this error message if the column of data contains
  any null values:

  ```
  Null values found - @Db function
  ```

  No
  data is returned with the message.
* "Discard" discards the null values, thus shortening the returned
  list of values. If one or more values are discarded when the @DbLookup
  formula is executed, DominoÂ® displays
  this message on the status bar:

  ```
  Caution: NULL values discarded from @Db list.
  ```
* "Replacement value" specifies a replacement value for null values.
  The replacement value must be a quoted string, but if the column is
  numeric or date-time, the string must be convertible to that type.

  If
  your formula includes a sort string argument, the list of values to
  be returned is sorted *before* the replacement values are inserted.
  During sorting, all null values are placed at the beginning of the
  list for an ascending sort, and at the end for a descending sort.
  They are not replaced until sorting is complete. This can result in
  a list that has some values sorted incorrectly. For example, if you
  specify "zzz" as your replacement value, all those "zzz" values will
  appear at the beginning of the list even though you sorted it by ascending
  order.

  If one or more values are replaced when the @DbLookup
  formula is executed, DominoÂ® displays
  this message on the status bar:

  ```
  Caution: NULL value replaced with user-defined value in @Db list.
  ```

  Generally,
  the replacement value should be one that is not likely to appear in
  the list as valid data; for example, if the column is text, your replacement
  value might be "\*\*\*" so that you can easily find those values.

## Specifying key\_column and key

Use "key\_column" to indicate which column
to search for the specified "key"; enclose the column name in quotation
marks. If the DBMS product uses case-sensitive column names, be sure
to use the correct capitalization. The values in the key column do *not* have
to be sorted before you retrieve data with @DbLookup.

Specify
a value using the NotesÂ® type
that agrees with the key column in the data source. For example, specify
a number or a number-valued expression when the key column is of any
numeric type, such as integer, real, float, or double. If the key
is a string (text) value, enclose it in quotation marks. A date-time
value must be entered in the format of the database, not that of DominoÂ®; for example, use 1996-01-31-12.00.00
for DB2/2, not 1996-01-31-12:00:00.

Together, the key column
and the key form the "where" clause of a selection statement:

```
"SELECT column WHERE key_column = key"
```

The
ODBC Application Interface always tests for equality and only returns
data from records where the value in the key column exactly matches
the key. To test whether the value in the key column matches one of
several possible values, format the key value as a list, separating
items with colons as in "Red":"Blue":"Green." This acts like an OR
operation, returning data from all records where the value in the
key column matches "Red" OR "Blue" OR "Green." To perform an AND operation
or to test for inequality, use @DbCommand to pass the appropriate
command string to the DBMS. Also use @DbCommand to pass the appropriate
command string if the key is a time-date value, because @DbLookup
does not always convert the time-date value to the correct format
for time-dates in the DBMS command language.

If you cannot
get @DbLookup to return the correct values due to typing or other
problems, try using a SELECT statement in @DbCommand.

## Specifying Distinct

The Distinct string argument is similar to @Unique
in DominoÂ®, except that Distinct
ensures that duplicate values are removed *before* the data is
returned to DominoÂ®. Using
Distinct instead of @Unique has two advantages:

* The formula operates more quickly because the additional work
  is performed outside of DominoÂ®.
* You can potentially retrieve a larger amount of useful data into DominoÂ® -- since the duplicate
  values are removed at the back-end, more unique values can be returned
  to DominoÂ®.

Note: Distinct is not supported by all ODBC drivers.
If there are null values in the data and you specify Distinct, one
null is usually returned.

## Specifying sort

If you use the Distinct string argument, you can append
the *sort* parameter to it with a colon. Use one these keywords
for the *sort* parameter to specify sorting of the return values:

* "Ascending" sorts the list in ascending order.
* "Descending" sorts the list in descending order.

If no sort string argument is specified, values are returned
in arbitrary order.

Note: The *sort* keywords
are not supported by all ODBC drivers. If you attempt to use both
Ascending and Descending in your formula, DominoÂ® displays an "Invalid argument" message.

## Accessing the values found

If multiple values are returned, they
are formatted as a list and are separated with the multi-value separator
designated for the current field.

@DbLookup can return no more
than 64KB of data. Use the following equations to determine how much
of your data can be returned with @DbLookup.

* For lookups that return text:

  2 + (2 \* number of entries returned)
  + total text size of all entries

  Each text string is limited
  to 511 bytes; if only one text string is returned, it is limited to
  64KB.
* For lookups that return numbers or dates:

  (10 \* number of entries
  returned) + 6

If the user's NOTES.INI file includes the statement:

```
NoExternalApps=1
```

the
@DbLookup formula is disabled. The user will not see an error message;
the formula fails to execute. This applies to @DbLookup only when
you use it with ODBC.

## Usage

@DbLookup
is intended mainly for keyword formulas. Instead of hard-coding a
list of keywords and then periodically updating that list, @DbLookup
lets you dynamically retrieve a list of values from an external database
table.

@DbLookup can't be used in mail agents, although it
does work in paste agents. This function only works in Web applications
if the remote server hosting the table from which data is being retrieved
exists on the same machine as the DominoÂ® server,
which is rarely the case.

## Examples

1. This formula retrieves from the inventory database the complete
   list of colors in which company uniforms are available. The data is
   stored like this:

   | Item | Size | Color |
   | --- | --- | --- |
   | Shirt | Small | Red |
   | Skirt | Small | Green |
   | Sweater | Medium | Red |
   | Trousers | Medium | Yellow |

   To retrieve the entire contents of the Color column (column
   3) for all records where the first sorted column (column 1, Item)
   contains "Shirt" or "Trousers":

   ```
   @DbLookup("ODBC"; "INVENTORY"; ""; ""; "UNIFORMS"; "Color"; "Item"; "Shirt" : "Trousers")
   ```

   Since
   multiple records contain at least one of the keys, the result is a
   list:

   ```
   Red:Yellow
   ```

   Values in the resulting
   list appear just as they were encountered in the database; they are
   not sorted and duplicate values are retained.
2. This example uses the sample "pubs" database that is included
   with Microsoftâ¢ SQL Server.
   The formula uses an ODBC driver to access the data source called PUBLISHERS
   and locate the table called "authors" that is owned by user "dbo."
   In this table, the values in the "state" column are compared with
   the values "CA" and "TN." For every record whose state field contains
   either "CA" or "TN", the values stored in the "au\_lname" field are
   returned. The author names are sorted in ascending order; null values
   are discarded.

   ```
   @DbLookup("ODBC";"PUBLISHERS";"dbo";"vanilla";
   "dbo.authors";"au_lname":"Discard";"state";
   "CA":"TN";"Ascending")
   ```

---

## @DbManager

# @DbManager (Formula Language)

Returns a list of users, groups, and servers who currently
have Manager access to the database. In a window title formula, only
the name of the first manager listed in the ACL is displayed.

@DbManager does not work in selection formulas or column
formulas.

## Syntax

**@DbManager**

## Return value

*managers*

Text or text list. The users,
groups, and servers that have manager access.

## Examples

1. This example returns "Gerald Brown" if Gerald Brown is the only
   user with Manager access to the current database.

   ```
   @DbManager
   ```
2. This example returns "Gerald Brown;Supervisors" if Gerald Brown
   and a group called Supervisors have Manager access to the current
   database.

   ```
   @DbManager
   ```
3. This example returns "GERALD BROWN;LOIS BOYD" if Gerald Brown
   and Lois Boyd are the two users with Manager access to the current
   database.

   ```
   @UpperCase(@DbManager)
   ```

---

## @DbName

# @DbName (Formula Language)

Returns the name of the current DominoÂ® server and database.

## Syntax

**@DbName**

## Return value

*server*  **;**  *path*

Text
list with two elements:

* *server* is the hierarchical name of the server on which
  the current database resides.

  This @function returns an empty string
  ("") if:

  + the database is local
  + the formula is used in a Scheduled agent running on the server
  + the formula is used in a view column

  Use [@Name](H_NAME.html "Allows you to manipulate hierarchical names. You can abbreviate the canonical format of a name, expand an abbreviated name to its canonical format, identify particular components within the name, and reverse the order of the components so that you can categorize a view by hierarchical names.") to extract a part
  of the name; for example, [CN] to extract the common name.
* *path* is the path and file name of the database.

  This
  @function returns:

  + the path relative to the NotesÂ® or DominoÂ® data directory if the
    database is in the data directory
  + the absolute path if the database is outside the data directory

  If the database is accessed through a directory or database
  link, this @function returns the location of the:

  + link if the @function is running locally (even if the database
    is on a server), so that the database appears to be where the link
    is
  + actual database if the @function is running on a server (for example,
    a scheduled agent)

## Usage

Be
careful when using @DbName in a column formula. If you build a view,
then move the database within the file directory, thus changing its
path, you must force a rebuild of the view (Cntl+Shift+F9) for the
function to display the updated database information.

## Examples

1. This example returns ";PERSONAL.NSF" if the current document is
   in the PERSONAL database stored in the data directory of the user's
   own computer.

   ```
   @DbName
   ```
2. This example returns "SALES1;ADMIN\STATUS.NSF" if the current
   document is stored in a DominoÂ® database
   named STATUS.NSF in the ADMIN directory on the SALES1 server. If the
   database is stored at the server's root directory (that is, it is
   not stored in a subdirectory), the result would be "SALES1;STATUS.NSF."
   You can extract just the file name of the list by combining @DbName
   with @Subset, as shown in the example.

   ```
   @DbName
   ```
3. This example returns "STATUS.NSF", the file name, since this is
   the last element in the returned list.

   ```
   @Subset(@DbName;-1)
   ```
4. This example returns the path of the current database, without
   the file name. For example, if the current database is SENSES\SOUNDS\SIGH.NSF,
   this formula returns "SENSES\SOUNDS."

   ```
   @LeftBack(@Subset(@DbName;-1);"\\")
   ```
5. This example displays the server, path, and file name of the current
   database, substituting the common name for the hierarchical name of
   the server.

   ```
   database := @Subset(@DbName; -1);
   server := @Name([CN]; @Subset(@DbName; 1));
   @Prompt([OK]; "Database name"; @Implode(server) + " " + @Implode(database))
   ```

---

## @DbTitle

# @DbTitle (Formula Language)

Returns the title of the current database.

## Syntax

**@DbTitle**

## Return value

*title*

Text.
The title of the current database.

## Examples

This form action formula uses @DbTitle
to let the user create and send an e-mail memo to the author of the
current document. @DbTitle is used in the memo's Subject.

```
return:=@Char(13);
memobody:=@Prompt([OKCANCELEDIT]; "Mail message"; 
  "Enter the contents of your mail message below." + return + "It will be sent to " + From + "."; "" );
@MailSend(From; ""; ""; "Your posting in " + @DbTitle; "";
   memobody:return; [IncludeDoclink])
```

---

## @DDEExecute

# @DDEExecute (Formula Language)

Passes the specified command string to the DDE application,
which is identified by the conversation ID. @DDEExecute is always
used in conjunction with @DDEInitiate and @DDETerminate.

Note: DDEExecute is not supported by UNIXâ¢ or on the Macintosh.

## Syntax

**@DDEExecute(**  *conversationID*  **;**  *command*  **)**

## Parameters

*conversationID*

The *conversationID* is
returned by the @DDEInitiate function, which must precede the use
of @DDEExecute. Use your own variable name; that's how you pass the
conversation ID between DominoÂ® and
the other application. If the conversation ID is invalid, DominoÂ® returns an error. See
@IsError.

*command*

Text. The *command* must
be a text string that adheres to the syntax rules of the receiving
application (see that application's documentation). Enclose the command
in quotation marks so it can be passed intact to the DDE application;
that application will then interpret it as a DDE command.

## Return value

*acknowledgment*

Number.

* Returns @True (1) if the DDE command is successfully executed
* Returns @False(0) if not
* Returns an error if the conversation ID is invalid

## Usage

This
function is intended for use primarily in field formulas, agents,
and toolbar buttons. It does not work in column or selection formulas,
and is not intended for use in window title or form formulas. Since
the Macintosh does not support DDE, these commands will not work on
Macintosh workstations. In addition, the format of the DDE commands
may vary somewhat with each platform or application.

If the
user's NOTES.INI file includes the statement:

```
NoExternalApps=1
```

then
any formula involving @DDE functions is disabled. The user doesn't
see an error message; the formula fails to execute.

You can
have up to 10 DDE conversations running concurrently, although under
normal circumstances you should only have one conversation running
at a time. Be sure to terminate all DDE conversations once they're
completed, or you may run out of sessions and be unable to initiate
more conversations when needed.

You cannot use this function
in Web applications.

## Examples

```
Conv_ID := @DDEInitiate("123W";"Budget95.wk3");
@If (@IsError(Conv_ID); @Do(@Prompt([Ok];"Error"; 
"Unable to initiate conversation");@Return(""));
@Do(@DDEPoke(Conv_ID;"A:B6"@Text(Amount));
@DDEExecute(Conv_ID;"[RUN(\"{Goto}A:B6~\")]");
@DDEExecute(Conv_ID;"[RUN(\"/rfc~~\")]");
@DDEExecute(Conv_ID;"[RUN(\"{Goto}A:B10~{Edit-Copy}\")]");
@DDETerminate(Conv_ID);
@Command([EditNextField]);
@Command([EditPaste])))
```

The line-by-line explanations:

```
Conv_ID := @DDEInitiate("123W";"Budget95.wk3");
```

Initiates
a conversation between DominoÂ® and 1-2-3Â®. This statement specifies
which worksheet to use (BUDGET95.WK3) and stores the conversation
ID in the variable Conv\_ID. Note that the specified file must be open
before the @DDEInitiate is executed.

```
@If (@IsError(Conv_ID); @Do(@Prompt([Ok];"Error";   
"Unable to initiate conversation"); @Return(""));
```

Determines
whether the DDE conversation was successfully initiated. If it was,
the formula continues; if it wasn't, a message appears, and the formula
stops executing.

```
@Do(@DDEPoke(Conv_ID;"A:B6";@Text(Amount));
```

Converts
the contents of the numeric Amount field to text, and then passes
that value to cell A:B6 in the 1-2-3Â® worksheet.
The value must be converted to text because only text can be passed
via DDEPoke.

```
@DDEExecute(Conv_ID;"[RUN(\"{Goto}A:B6~\")]");
```

Makes
cell A:B6 the current location in the worksheet.

```
@DDEExecute(Conv_ID;"[RUN(\"/rfc~~\")]");
```

Passes
the Range, Format, Currency command to 1-2-3Â®;
cell A:B6 is now formatted for currency values.

```
@DDEExecute(Conv_ID;"[RUN(\"{Goto}A:B10~{Edit-Copy}\")]");
```

Passes
the Goto and Edit Copy commands to 1-2-3Â®;
cursor is moved to cell A:B10 within the worksheet and the value stored
in that cell is copied to the Windowsâ¢ Clipboard.

```
@DDETerminate(Conv_ID);
```

Terminates
the DDE conversation.

```
@Command([EditNextField]);
```

Navigates
to the next field within the current DominoÂ® document.

```
@Command([EditPaste])))
```

The
contents of the Clipboard (the value from cell A:B10) are pasted into
that field.

---

## @DDEInitiate

# @DDEInitiate (Formula Language)

Initiates a conversation with a DDE server, and returns
the conversation ID.

Note: DDEInitiate is not supported by UNIXâ¢ or on the Macintosh.

## Syntax

**@DDEInitiate(**  *application*  **;**  *topic*  **)**

## Parameters

*application*

Text.
The name of the application you want to initiate a DDE conversation
with. This application must be launched before you call @DDEInitiate.
The values for *application* and *topic* vary from one application
to another; the appropriate values can usually be found in the index
for the application's documentation, under "DDE."

*topic*

Text.
The data file you want to use. This file must be opened before you
call @DDEInitiate.

## Return value

*conversationID*

This ID identifies
the particular DDE conversation so you can pass commands and data
to it with @DDEExecute and @DDEPoke, and eventually terminate the
conversation with @DDETerminate. Returns an error if the conversation
cannot be initiated. See @IsError.

## Usage

It
is intended for use primarily in field formulas, agents, and toolbar
buttons. Since the Macintosh does not support DDE, these commands
will not work on Macintosh workstations. This function does not work
in column or selection formulas, and is not intended for use in window
title or form formulas.

If the user's notes.ini file includes
the statement

```
NoExternalApps=1
```

then
any formula involving @DDE functions is disabled. The user doesn't
see an error message; the formula fails to execute.

You can
have up to 10 DDE conversations running concurrently, although under
normal circumstances you should only have one conversation running
at a time. Be sure to terminate all DDE conversations once they're
completed, or you may run out of sessions and be unable to initiate
more conversations when needed.

You cannot use this function
in Web applications.

## Initiation failures

If the conversation cannot be initiated, @DDEInitiate
will return an error. See @IsError. Below are some reasons why the
initiation could fail:

* The workstation operating system does not support DDE (Macintosh).
* The DDE application you're trying to set up a conversation with
  is not running. The specified file is not open.
* The DDE application is running, but the specified topic is not
  open or the application does not support the topic specified with
  @DDEInitiate.
* The DDE application is running, but the specified file does not
  open.
* The maximum number of concurrent DDE conversations has been reached
  (currently, the maximum is 10).

## Examples

See [@DDEExecute](H_DDEEXECUTE.html "Passes the specified command string to the DDE application, which is identified by the conversation ID. @DDEExecute is always used in conjunction with @DDEInitiate and @DDETerminate.").

---

## @DDEPoke

# @DDEPoke (Formula Language)

Deposits unsolicited data into the specified location within
the DDE server application. If the data was successfully inserted
into the target location, @DDEPoke returns an ACK (acknowledgement)
with the value @True(1); if the attempt was not successful, the call
returns a NACK (negative acknowledgment) with the value @False(0).
If the conversation ID is invalid, an error is returned (see @IsError).

Note: DDEPoke is not supported by UNIXâ¢ or on the Macintosh.

## Syntax

**@DDEPoke(**  *conversationID*  **;**  *location*  **;**  *data*  **)**

## Parameters

*conversationID*

The *conversationID* is
returned by @DDEInitiate.

*location*

Text. The
name of the location where you want to place the *data.* The
location must be a cell, range, or field name; enclose it in quotation
marks.

*data*

Text. Optional. The data you want
to place at *location.* If you want to pass the contents of a
non-text field, use @Text to convert it to text first. If *data* is
a text list, only the first value in the list gets passed. If you
omit *data*, DominoÂ® passes
the contents of the Windowsâ¢ Clipboard
to the receiving application. If you supply the data as a parameter,
either enclose it in quotation marks or specify a DominoÂ® field name.

## Usage

It
is intended for use primarily in field formulas, agents, and toolbar
buttons. Since the Macintosh does not support DDE, these commands
will not work on Macintosh workstations. This function does not work
in column or selection formulas, and is not intended for use in window
title or form formulas.

If the user's NOTES.INi file includes
the statement

```
NoExternalApps=1
```

then
any formula involving @DDE functions is disabled. The user doesn't
see an error message; the formula fails to execute.

You cannot
use this function in Web applications.

## Examples

See [@DDEExecute](H_DDEEXECUTE.html "Passes the specified command string to the DDE application, which is identified by the conversation ID. @DDEExecute is always used in conjunction with @DDEInitiate and @DDETerminate.").

---

## @DDETerminate

# @DDETerminate (Formula Language)

Terminates the conversation with a DDE application.

Note: DDETerminate is not supported by UNIXâ¢ or on the Macintosh.

## Syntax

**@DDETerminate(**  *conversationID*  **)**

## Parameters

*conversationID*

The *conversationID* is
returned by @DDEInitiate. Use the same *conversationID* you used
with the@DDEInitiate and @DDEExecute commands.

## Return value

*status*

* Returns an error if the *conversationID* is invalid
* Returns nothing if the *conversationID* is valid

See @IsError.

## Usage

It
is intended for use primarily in field formulas, agents, and toolbar
buttons. Since the Macintosh does not support DDE, these commands
will not work on Macintosh workstations.This function does not work
in column or selection formulas, and is not intended for use in window
title or form formulas.

If the user's NOTES.INI file includes
the statement

```
NoExternalApps=1
```

then
any formula involving @DDE functions is disabled. The user doesn't
see an error message, the formula fails to execute. Be sure to terminate
all DDE conversations once they're completed, or you may run out of
sessions and be unable to initiate more conversations when needed.

You
cannot use this function in Web applications.

## Examples

See [@DDEExecute](H_DDEEXECUTE.html "Passes the specified command string to the DDE application, which is identified by the conversation ID. @DDEExecute is always used in conjunction with @DDEInitiate and @DDETerminate.").

---

## DEFAULT

# DEFAULT (Formula Language)

A reserved word that does one of the following:

* Assigns a default value to a field.
* Says that for the duration of the computation of this formula,
  if a document does not have this field, act as though it does with
  this as its value.
* Allows you to assign values that provide dynamic defaults to fields.

## Syntax

**DEFAULT** *variableName* **:=** *value*  **;**

## Usage

This
reserved word works in any formula.

Use DEFAULT once in a formula.
If this keyword occurs multiple times in a formula, the first occurrence
is effective and subsequent occurrences have no effect.

Note: Pre-R6 behavior differs:
if a formula contains multiple occurrences of DEFAULT, the last occurrence
is effective.

## Examples

These two formulas display the value
in the field named KeyThought, if that field exists; otherwise, the
value in the field Topic is displayed. Using DEFAULT lets you write
a simpler formula that is less prone to error, and easier for others
to understand.

```
@If(@IsAvailable(KeyThought);KeyThought;Topic);
```

and

```
DEFAULT KeyThought := Topic;
KeyThought;
```

---

## @DeleteDocument

# @DeleteDocument (Formula Language)

Deletes the current document.

## Syntax

**@DeleteDocument**

## Usage

This
function works only in agents.

To mark a document for deletion
in a view, see [@Command[EditClear]](H_EDITCLEAR.html "Performs the menu command Edit - Delete.").

You
cannot use this function in Web applications.

## Examples

This example deletes those documents
where the Status field equals "Closed."

```
FIELD Status:=@If(Status="Closed";@DeleteDocument;Status)
```

---

## @DeleteField

# @DeleteField (Formula Language)

Deletes the value of an editable field.

## Syntax

**FIELD**  *fieldName*  **:=
@DeleteField**

## Usage

This
function works in agent, view action, and toolbar button formulas.

If
the field has a default value, the default value is reinstated after
this function deletes the current value.

This function is the
same as [@Unavailable](H_UNAVAILABLE.html "Deletes the value of an editable field.").

Use
this function to delete invisible fields from documents, such as fields
created using the [@SetField](H_SETFIELD.html "Assigns a value to a field stored within a document (use @Set for temporary variables). This is similar to using the FIELD keyword, except that @SetField can be used within another @function. If the field does not exist, this command creates it and applies the specified value to it.") function.

## Examples

This formula creates a field named
NewDate and sets it to today's date, then removes the field named
OldDate from the document.

```
FIELD NewDate:=@Today
FIELD OldDate:=@DeleteField;
```

---

## @DialogBox

# @DialogBox (Formula Language)

Brings up a dialog box that displays the current document
(either open or selected in a view). The dialog box shares fields
with the underlying document. The user interacts with the dialog box
as usual, clicking OK or Cancel when finished.

This function can be used with any form, but it's particularly
useful with forms that contain a single table or layout region, because
the user can interact with the table or layout region as if it were
a dialog box.

## Syntax

**@DialogBox(**  *form*  **;
[AUTOHORZFIT] : [AUTOVERTFIT] : [NOCANCEL] : [NONEWFIELDS] : [NOFIELDUPDATE]
: [READONLY] : [SIZETOTABLE] : [NOOKCANCEL] : [OKCANCELATBOTTOM] :
[NONOTE] ;**  *title*  **)**

## Parameters

*form*

Text.
The name of the form.

**[AUTOHORZFIT]**

Keyword.
Optional. Scales the dialog box horizontally to fit the first layout
region or table on the form. Otherwise, the dialog box is not scaled
horizontally.

**[AUTOVERTFIT]**

Keyword. Optional.
Scales the dialog box vertically to fit the first layout region or
table on the form. Otherwise, the dialog box is not scaled vertically.

**[NOCANCEL]**

Keyword.
Optional. Does not display the Cancel button. Otherwise, the dialog
box contains the Cancel button.

**[NONEWFIELDS]**

Keyword.
Optional. Does not add fields to the underlying document that are
in the dialog box form but not the underlying form. Otherwise, all
dialog box fields are passed to the underlying document.

**[NOFIELDUPDATE]**

Keyword.
Optional. Does not pass edits from the fields in the dialog box to
the underlying document (for example, if you're passing the edits
somewhere else in a QueryClose script for the dialog box form). Otherwise,
the edits are passed to the underlying form.

**[READONLY]**

Keyword.
Optional. Prohibits writing to the dialog box (for example, if you
are using the dialog box to display a Help screen). Otherwise, the
dialog box is read-write. Use of this keyword implies [NoCancel].

**[SIZETOTABLE]**

Note: This parameter is new with Release 5.

Keyword.
Optional. Applies **[AUTOHORZFIT]** and **[AUTOVERTFIT]** to
the first table on the form. Otherwise, they are applied to the first
layout region on the form.

**[NOOKCANCEL]**

Note: This parameter is new with Release 5.

Keyword.
Optional. Does not display the OK button. Otherwise, the dialog box
contains the OK button. Using this keyword prevents any changes made
in the dialog box from being reflected in the current document. If
you want the current document to be updated, but don't want an OK
button to display, include a hotspot button that executes the RefreshParentNote
@command.

**[OKCANCELATBOTTOM]**

Note: This
parameter is new with Release 6.

Keyword. Optional. Displays
the OK and/or Cancel buttons side-by-side at the end of the dialog
box. Otherwise, the buttons appear stacked near the beginning.

**[NONOTE]**

Note: This parameter is new with Release 7.

Keyword.
Optional. Allows the @Dialogbox function to be used without requiring
a current document. If a current document exists, it will be ignored.
Using this keyword implies [NOFIELDUPDATE] and [NONEWFIELDS] even
if they are not explicitly specified. Using this keyword means that
values are not passed from an underlying document to the dialog box;
the only values displayed are default formula values. If the [NONOTE]
parameter is not specified, a current document is required.

*title*

Text.
The title of the dialog box. Defaults to "NotesÂ®."

## Return value

*number*

@True (1) if the user clicks
OK, @False (0) otherwise. If the user clicks OK, corresponding fields
are updated (see "Sharing of field values") unless [NOFIELDUPDATE]
or [NONOTE] is in effect.

## Usage

This
function is useful in buttons for actions. It does not work in column
or selection formulas, or in agents that run on a server (mail and
scheduled agents). It is not intended for use in window title or
form formulas. It can be used with any form, but it's particularly
useful with forms that contain a single table or layout region, because
the user can interact with the table or layout region as if it were
a dialog box.

@DialogBox cannot return data from a rich text
field.

You
cannot use this function in Web applications.

If the form contains
actions on the action bar, they are not displayed in the dialog box.

**[AUTOHORZFIT]** and **[AUTOVERTFIT]** allow
you to display an entire layout region (no **[SIZETOTABLE]**) or
table (**[SIZETOTABLE]**) in a dialog box without displaying the
rest of the form. If the form has more than one layout region or table,
the first is used. For best results:

* Use both **[AUTOHORZFIT]** and **[AUTOVERTFIT]**.
* In the Layout properties box, deselect "Show border" and select
  "3D style."

If **[AUTOHORZFIT]** and **[AUTOVERTFIT]** are both
omitted, the entire form is used and no sizing takes place. If the
form contains no layout region (no **[SIZETOTABLE]**) or table
(**[SIZETOTABLE]**), the entire form is used and no sizing takes
place.

## Sharing of field values

@DialogBox displays the current document
using a different form. This means:

* If the *form* has field names in common with the current
  document, the field values of the current document are displayed in
  the dialog box. Rich text fields will not be displayed in the *form,* even
  if field names are the same as in the current document.
* If the user changes the value of any fields in the dialog box
  and selects OK, the changes are reflected in the same fields on the
  current document.
* If the user enters a value for a field in the dialog box, and
  the current document does not contain a field by that name, the value
  is added to the document, even if it is not displayed in the form.
* If you do not want to include OK or Cancel buttons on the dialog
  box, but do want any changes made in it to be reflected in the current
  document, use @Command([RefreshParentNote]). If you add this command
  to a hotspot button on the dialog box form, for example, when a user
  clicks the button the field values in the current document are updated
  to reflect any changes made to fields having the same names in the
  dialog box.

## Examples

1. A form called "Profile" contains a button whose formula is

   ```
   @DialogBox("Profile Options"; [AUTOHORZFIT] : [AUTOVERTFIT] )
   ```

   Both
   Profile and Profile Options have a field named "Comments." When the
   user clicks the button, the document is displayed in a dialog box,
   using the Profile Options form. The dialog box is scaled to fit the
   layout region on Profile Options.

   The user can interact with
   the dialog box, for example, by editing the Comments field.

   The
   user clicks OK. The changes made to the Comments field are reflected
   in the document, if it is in Edit mode.
2. This formula displays a form named "Help screen" for reading only.

   ```
   @DialogBox("Help screen"; [AUTOHORZFIT] : [AUTOVERTFIT] : [NOCANCEL] : [NONEWFIELDS] : [READONLY]; "Help")
   ```
3. This formula sizes the table in the form to the dialog box and
   does not display the OK button.

   ```
   @DialogBox( "Table Test" ; [AUTOHORZFIT] : [AUTOVERTFIT] : [SIZETOTABLE] : [NOOKCANCEL])
   ```
4. This formula displays the "info" form in a dialog box entitled,
   "Provide information," that has no dialog box buttons. The only way
   the user can close this form to return to the current document is
   to click the Close icon on the dialog box.

   ```
   @DialogBox("info";[NOCANCEL]:[NOOKCANCEL];"Provide information")
   ```

---

## @Do

# @Do (Formula Language)

Evaluates expressions from left to right, and returns the
value of the last expression in the list.

## Syntax

**@Do(**  *expressions*  **)**

## Parameters

*expressions*

Any
number of expressions that you want @Do to evaluate. Separate multiple
expressions with semicolons: *expression1*  **;**  *expression2*  **;**  *expression3*

## Return value

*lastExpression*

The
value of the last expression.

## Usage

This
function is useful in agents, hotspot buttons, and toolbar buttons
and when you want to execute multiple expressions from within a single
@function. It does not work in column or selection formulas.

## Examples

This formula displays a dialog box
asking whether the user wants to edit the current document. If the
user selects Yes, another dialog box appears, prompting for the user's
name. If the user now selects Cancel, the formula stops execution;
if the user enters a name and selects OK, the document is opened in
Edit mode.

If the user selects No at the first dialog box, a different
one follows it. This time, a message appears noting that the user
chose not to edit the document, and DominoÂ® navigates
to the next document in the view.

```
@If(@Prompt([YESNO]; "Question";
"Edit this document?");
@If(@Prompt([OKCANCELEDIT]; "Positive Response";
"You have chosen to edit this document. Select OK if the name below is correct.";
@UserName) != "ERR_CANCEL";
@Command([EditDocument]);@Return(""));
@Do(@Prompt([OK]; "Negative Response";
"You have chosen not to edit this document. Select OK to continue to the next document.");
@Command([NavNext])))
```

---

## @DocChildren

# @DocChildren (Formula Language)

In a column or window title formula, returns the number
of child documents or categories belonging to the current document
or category. Only immediate responses count as children. For example,
the responses to a main document are its children, but the responses
to a response document are not.

## Syntax

**@DocChildren
@DocChildren(**  *defaultString*  **) @DocChildren(**  *zero-string*  **;** *defaultString*  **)
@DocChildren(**  *zero-string*  **;** *one-string*  **;** *defaultString*  **)**

## Parameters

*defaultString*

Text.
Optional. The text to return. If a "%" is used in the string, it will
be replaced with the number of children documents or categories. Example:
"% Responses."

*zero-string*

Text. Optional.
The text to return if the document or category has no children, such
as "No Responses."

*one-string*

Text. Optional.
The text to return if the document or category has just one child,
such as "One Response."

## Return value

The return value depends on how you call @DocChildren:

*numChildren*

Special
text. If @DocChildren is called with no parameters, then the number
of child documents belonging to the current document or category is
returned. You cannot convert special text to a number.

*childString*

Special
text. If @DocChildren is called with one or more parameters, it returns
the appropriate string, based on the number of child documents belonging
to the current document or category. You cannot convert special text
to a number.

## Usage

Use
@DocChildren in window title and column formulas, when you want to
indicate how many top-level responses a particular document has, or
how many main documents are within a particular category. This function
does not work in any other formula.

This @function is calculated
when the document is opened. Results are undefined in cases where
the document is not opened, such as printing from a view.

You
cannot use this function in Web applications, except in column formulas.

## Examples

1. This example returns 3.

   ```
    @DocChildren
   ```
2. This example returns 3 Responses. DominoÂ® substitutes
   the appropriate number for %. If the document doesn't have any responses,
   this formula returns 0 Responses.

   ```
   @DocChildren("% Responses")
   ```
3. This example returns 3 Responses. This time, if the document doesn't
   have any responses, the formula returns the message No Responses.

   ```
   @DocChildren("No Responses";"% Responses")
   ```
4. This example returns There are 3 Responses. If the document has
   one response, the message is 1 Response; if the document has no responses,
   the message is No Responses.

   ```
   @DocChildren("No Responses";"1 Response";
   "There are % Responses.")
   ```

---

## @DocDescendants

# @DocDescendants (Formula Language)

In a column or window title formula, returns the number
of descendant documents or subcategories belonging to the current
document or category. Where @DocChildren only counts direct descendants,
@DocDescendants counts all descendants, regardless of level.

## Syntax

**@DocDescendants
@DocDescendants(**  *defaultString*  **) @DocDescendants(**  *zero-string* **;**  *defaultString*  **)
@DocDescendants(**  *zero-string* **;**  *one-string* **;**  *defaultString*  **)**

## Parameters

*defaultString*

Text.
Optional. The text to return. If a "%" is used in the string, it will
be replaced with the number of descendant documents or categories.
Example: "% Responses."

*zero-string*

Text. Optional.
The text to return if the document or category has no descendants,
such as "No Responses."

*one-string*

Text. Optional.
The text to return if the document or category has just one descendant,
such as "One Response."

## Return value

The return value can be either special text or text:

*numChildren*

Special
text. If @DocDescendants is called with no parameters, then the number
of descendant documents belonging to the current document or category
is returned. You cannot convert special text to a number.

*childString*

Special
text. If @DocChildren is called with one or more parameters, it returns
the appropriate string, based on the number of descendant documents
belonging to the current document or category. You cannot convert
special text to a number.

## Usage

Use
@DocDescendants in window title and column formulas, when you want
to indicate the total number of responses (at all levels) to a particular
document, or the total number of documents within a particular category.
This function does not work in any other formula.

This @function
is calculated when the document is opened. Results are undefined in
cases where the document is not opened, such as printing from a view.

You
cannot use this function in Web applications, except in column formulas.

## Examples

1. This example returns 3.

   ```
   @DocDescendants
   ```
2. This example returns 3 Response(s).

   ```
   @DocDescendants("% Response(s)")
   ```
3. This example returns 3 Responses. If there are no responses to
   the document, the formula returns No Responses.

   ```
   @DocDescendants("No Responses";"% Responses")
   ```
4. This example returns There are 3 Responses. If the document has
   one response, the message is 1 Response; if the document has no responses,
   the message is No Responses.

   ```
   @DocDescendants("No Responses";"1 Response";
   "There are % Responses.")
   ```

---

## @DocFields

# @DocFields (Formula Language)

Returns a list of all the fields in a document.

## Syntax

**@DocFields**

## Return value

*fields*

Text list. Each item in the
list is the name of a field on the document.

## Usage

This
function works in any formula that runs in the context of one or more
documents. It does not work in column and view selection formulas.

After
a document is saved, the returned list includes some of the internal NotesÂ® fields, such as the Form
field, that is added by NotesÂ® to
a form when it is saved or the $Links field, that indicates that the
form contains a link to another document or database.

## Examples

1. This example returns Form; result; name; phone if those are the
   names of the fields in a document.

   ```
   @DocFields
   ```
2. This example returns Yes if used in a field on a form that contains
   a rich text field containing a link to a database or document.

   ```
   @If(@Contains(@DocFields; "$Links"); "Yes"; "No")
   ```
3. This example, when used in the postopen event of a form, enables
   the user to choose a field to alter from a list of the fields on the
   form then provide a value to put into the chosen field.

   ```
   Field fieldtochange := @Prompt([OkCancelEditCombo]; "Edit Fields"; "Please select the field you want to edit."; ""; @DocFields);
   @SetField(fieldtochange;(@Prompt([OkCancelEdit]; "New Value"; "Please enter a new value.";"") )
   ```
4. This example, when used in a field on a form, returns the number
   of fields contained by that form when it is saved:

   ```
   @Elements(@DocFields)-2
   ```

   Subtract
   2 from the total number of elements to account for the current field
   and the Form field, which is an internal NotesÂ® field.

---

## @DocLength

# @DocLength (Formula Language)

Returns the approximate size of a document in bytes.

## Syntax

**@DocLength**

## Return value

*length*

Number. The size of the document.

## Usage

This
function works in any formula that runs in the context of one or more
documents.

The number returned is only an approximation. The
actual size of the document may differ for the following reasons:

* The number accounts for user data only; it does not take into
  account per document or per field constants such as static text or
  formulas.
* The database allocates storage in 64-byte increments; a document
  may not use all of the 64 bytes allotted to it.

Documents that are open typically use more storage than documents
that are closed. The value returned for @DocLength may vary depending
on whether it is running in an open document or a closed document;
for example, a document selected at the view level.

## Examples

This example returns 1808 if that
is the approximate number of bytes in the document (one-page document,
no enhanced text).

```
@DocLength
```

---

## @DocLevel

# @DocLevel (Formula Language)

Returns a text string that represents the level of the
document or category.

## Syntax

**@DocLevel**

## Return value

*level*

Special text. The level of
the document or category. You cannot convert special text to a number.

## Usage

Use
@DocLevel in column and window title formulas. If you use it in a
window title or field formula, it will always evaluate to "1" until
the document has been saved and reopened. This function does not work
in any other formula.

This @function is calculated when the
document is opened. Results are undefined in cases where the document
is not opened, such as printing from a view.

You cannot use
this function in Web applications.

## Examples

1. This example of a category returns 1.

   ```
   @DocLevel
   ```
2. This example of a main document in a category returns 2.

   ```
   @DocLevel
   ```
3. This example of a response document in a category returns 3.

   ```
   @DocLevel
   ```
4. This example of a main document that is not in a category returns
   1.

   ```
   @DocLevel
   ```

---

## @DocLock

# @DocLock (Formula Language)

Locks, unlocks, returns the locked status of the current
document, or indicates if a database has document locking enabled.

Note: This @function is new with Release 6.

## Syntax

**@DocLock** **(
[** *options* **] )**

## Parameters

**[**  *options*  **]**

Keyword.
Choose one of the following actions:

**[LOCK]**

Locks
the current document.

**[UNLOCK]**

Unlocks the
current document.

**[STATUS]**

Indicates the locked
status of the current document. Returns null if the document is not
locked or a textlist of the users who have locked the document if
it is locked.

**[LOCKINGENABLED]**

Indicates if
the current database has document locking enabled. Returns 1 (@True)
if locking is enabled and 0 if it is not.

## Usage

The
current document has to have been saved previously for this function
to work properly. The document must be in Read mode when this function
is triggered. Additionally, document locking must be enabled for the
database or you will get the error, "Attempted a lock operation on
a DB that doesn't support locking" when you try to use the [LOCK],
[STATUS], or [UNLOCK] keywords.

To enable document locking:

1. Specify a DominoÂ® server
   as the Administration Server (Master Lock Server) on the Advanced
   panel of the Access Control List dialog box for the database.
2. Select the Allow Document Locking check box on the Database Basics
   tab of the Database Properties box.

You cannot use this function in Web applications.

## Examples

If a user wants to lock a document
opened in edit mode, the following four hotspot buttons entitled LockingEnabled,
Status, Lock, and Unlock will enable her to do so.

First, to determine
if the current document can be locked, the user checks if document
locking is enabled for the database. When the user clicks the LockingEnabled
button, which contains the following code, it returns 1 to indicate
that locking is enabled.

```
@Prompt([OK];"Checking if document locking is enabled";@DocLock([LOCKINGENABLED]))
```

If
locking is enabled, the user next clicks the Status button, which
contains the following code. If it returns an empty message box, the
current document is not locked.

```
@Prompt([OK];"Checking document status";@DocLock([STATUS]))
```

Once
the user clicks the Lock hotspot button which contains the following
code, the administrative server locks the document. If the user clicks
the Status button again, a message box appears that displays the current
user's hierarchical name.

```
@DocLock([LOCK])
```

If
the user then clicks the Unlock hotspot button that contains the following
code, the administrative server unlocks the document. When the user
clicks the Status button again, an empty message box appears.

```
@DocLock([UNLOCK])
```

---

## @DocMark

# @DocMark (Formula Language)

In an agent that runs a formula, indicates whether or not
you want to save the changes to a document.

## Syntax

**@DocMark(
[Update] ) @DocMark( [NoUpdate] )**

## Parameters

**[UPDATE]**

Keyword.
Marks a document so that changes made to it are saved to disk.

**[NOUPDATE]**

Keyword.
Marks a document so that changes made to it will not be saved to disk.

## Usage

Use
@DocMark in any type of agent to indicate if the changes made to a
document by the agent should be saved. This function has no effect
in any other formula.

You cannot use this function in Web applications.

---

## @DocNumber

# @DocNumber (Formula Language)

In a column or window title formula, returns a string representing
the entry number of the current document or category. For example,
2.3 indicates that the document is the third entry following the second
entry.

## Syntax

**@DocNumber
@DocNumber(**  *separator*  **) @DocNumber( "" )**

## Parameters

*separator*

Text.
Optional. Indicates a separator to be used in the document number
instead of "." (period); must be one character.

**""**

Empty
string argument. Optional. Tells the function to return the least
significant item of the document number (in other words, its rightmost
component).

## Return value

*docNum*

Special text. The value that
represents the document number of the document or category in the
view. You cannot convert special text to a number.

## Usage

Use
@DocNumber in column or window title formulas. In window title or
field formulas, it will evaluate to "0" until the document has been
saved and reopened. This function does not work in any other formula.

This
@function is calculated when the document is opened. Results are undefined
in cases where the document is not opened, such as printing from a
view.

You cannot use this function in Web applications, except
in column formulas.

## Examples

1. This example returns 37.1.3 for entry 37.1.3.

   ```
   @DocNumber
   ```
2. This example returns 37-1-3 for entry 37.1.3.

   ```
   @DocNumber("-")
   ```
3. This example returns 3 for entry 37.1.3.

   ```
   @DocNumber("")
   ```

---

## @DocOmmittedLength

# @DocOmittedLength (Formula Language)

Returns the approximate number of bytes a truncated document
lost during replication. The bytes are the total number of bytes per
attachment, OLE object, large rich text field, or non-summary items
that were too large, according to the replication settings for the
database, to be replicated.

Note: This @function is new with Release 6.

## Syntax

**@DocOmittedLength**

## Return value

*length*

Number. The bytes of data
that were not replicated. Returns zero if the document has not been
truncated, was truncated previously, or was truncated by a pre-Release
6 server.

## Usage

This
function works only in databases that are running on and were replicated
by a DominoÂ® 6 or later server.

Documents
can be truncated during database replication to save space. One replication
setting option, for instance, enables you to replicate summary data
and only 40KB of rich text for each document. In the resulting replica,
you can retrieve the rest of a truncated document by choosing Actions
- Retrieve Entire Document from the menu. @DocOmittedLength enables
you to determine how much information (in bytes) was removed from
the document during replication to help you determine if you want
to retrieve it.

This function works in any formula that runs
in the context of one or more documents.

The number returned
is only an approximation. The actual size of the document may differ
for the following reasons:

* The number accounts for user data only; it does not take into
  account per document or per field constants such as static text or
  formulas.
* The database allocates storage in 64-byte increments; a document
  may not use all of the 64 bytes allotted to it.

## Examples

This example, when used as a column
formula, returns the total size of the document:

```
@DocLength + @DocOmittedLength
```

---

## @DocParentNumber

# @DocParentNumber (Formula Language)

In a column or window title formula, returns a string that
represents the entry number of the parent view entry. Both the current
view entry and the parent can be either documents or categories.

## Syntax

**@DocParentNumber
@DocParentNumber(**  *separator*  **) @DocParentNumber( ""
)**

## Parameters

*separator*

Text.
Optional. Indicates a separator to be used in the parent document
number instead of ".".

**""**

Empty string argument.
Optional. Tells the function to return the least significant item
of the parent document number (in other words, its rightmost component).

## Return value

*docNum*

Special text. The value that
represents the document number of the document or category in the
view. You cannot convert special text to a number.

## Usage

Use
@DocParentNumber in column and window title formulas. If you use it
in a field formula or window title formula, no result is displayed
until the document has been saved and reopened. This function does
not work in any other formula.

To determine the number for
the current entry, use @DocNumber instead.

You cannot use this
function in Web applications, except in column formulas.

## Examples

1. This example returns 37.1.3 for the document or category for which
   the parent is entry 37.1.3.

   ```
   @DocParentNumber
   ```
2. This example returns 37-1-3 for the document or category for which
   the parent is entry 37.1.3.

   ```
   @DocParentNumber("-")
   ```
3. This example returns 3 for the document or category for which
   the parent is entry 37.1.3.

   ```
   @DocParentNumber("")
   ```

---

## @DocSiblings

# @DocSiblings (Formula Language)

In a column or window title formula, returns a string that
represents the total number of entries at the same level as a view
entry (document or category). The returned total includes the document
itself. For example, if the document is entry 8.2, and entries 8.1,
8.3, and 8.4 also exist, then there are four document siblings.

## Syntax

**@DocSiblings**

## Return value

*numSiblings*

Special text. The number
of entries at the same level as the document or category. You cannot
convert special text to a number.

## Usage

Use
@DocSiblings in column and window title formulas. If you use it in
a field or window title formula, it evaluates to 0 until the document
has been saved and reopened. This function does not work in any other
formula.

This @function is calculated when the document is
opened. Results are undefined in cases where the document is not opened,
such as printing from a view.

You cannot use this function
in Web applications, except in column formulas.

## Examples

This example returns Response 1 of
4 to CurrentÂ® Vacation Policy
if the document is one of four responses to a document with the string CurrentÂ® Vacation Policy in the
Topic field.

```
@If(@IsNewDoc;"New Document";"Response" + @DocNumber(" ") +
 " of " + @DocSiblings + " to " + Topic)
```

---

## @DocumentUniqueID

# @DocumentUniqueID (Formula Language)

The universal ID, which uniquely identifies a document
across all replicas of a database. In text format, the universal ID
is a 32-character combination of hexadecimal digits (0-9, A-F).

The universal ID is also known as the unique ID or UNID.

## Syntax

**@DocumentUniqueID**

## Usage

If
two documents in replica databases share the same universal ID, the
documents are replicas.

This function works in any formula
that runs in the context of one or more documents.

To display
the UNID, you must convert the result of this function to text, that
is, you must specify @Text(@DocumentUniqueID).

The unique ID
is one part of a document's entire ID number. To see a document ID,
click the Document IDs tab of the document properties box. The UNID
is on the first two lines following OF (first line) and ON (second line)
in two 8-character segments.

Once created, a document's UNID never
changes. If a document is copied and pasted, the pasted document gets
a new UNID.

Every response document has a special field called
$Ref that contains the UNID of the parent document.

In a field
formula, @DocumentUniqueID (not converted to text) is a link to the
document.

## Examples

1. This column formula displays the UNID of each document in the
   view.

   ```
   @Text(@DocumentUniqueID)
   ```
2. This computed field formula creates a doclink to the current document.

   ```
   @DocumentUniqueID
   ```
3. This "Computed when composed" field formula in a "Response" document
   creates a doclink to the parent document. In the properties box for
   the "Response" form, "Formulas inherit values from selected document"
   must be checked.

   ```
   @InheritedDocumentUniqueID
   ```
4. You want the Project field of a new "Response" document to match
   the Project field of the parent "Main Topic" document. In the properties
   box for the "Response" form, check "Formulas inherit values from selected
   document." Make Project on the "Response" form a computed field and
   give it this formula:

   ```
   Project
   ```
5. Field inheritance only happens once when the Response is created.
   However, you want to access the "Main Topic" after the "Response"
   is created. Create an agent that runs on a schedule, selects all documents
   in the database that use the form "Response," and runs the following
   formula:

   ```
   FIELD Project := @GetDocField($Ref; "Project");
   @All
   ```
6. This is a long solution to the problem. Create a hidden
   view called, for example, "By doc ID" with the following selection
   formula:

   ```
   SELECT Form = "Main Topic"
   ```

   The first
   column is sorted and its formula is:

   ```
   @Text(@DocumentUniqueID)
   ```

   Create
   an agent that runs on a schedule, selects all documents in the database
   that use the form "Response," and runs the following formula:

   ```
   FIELD Project := @DbLookup("":""; ""; "By doc ID"; @Text($Ref); "Project");
   @All
   ```

   Each time the agent runs, it performs a lookup
   in the "By doc ID" view to find the "Main Topic" that is the parent
   of the current "Response" (that is, the document with a @DocumentUniqueID
   that matches the current document's $Ref field). It then copies the
   contents of the Project field from the parent to the child.

---

## @Domain

# @Domain (Formula Language)

Returns the name of the current user's DominoÂ® mail domain listed in the current location
document of the Personal Address Book.

## Syntax

**@Domain**

## Return value

*domain*

Text. The current user's domain.

## Usage

This
function works in any formula and is useful in formulas that manipulate
mail addresses. When a formula runs on a server, the server is considered
the current user, so @Domain returns the name of the server's domain.

You
cannot use this function in Web applications.

## Examples

1. This example returns WorkSavers if the current user belongs to
   the WorkSavers domain.

   ```
   @Domain
   ```
2. This formula replaces any occurrences of the user's mail address
   with a null string, thus removing the current user's name from CopyTo.

   ```
   FIELD CopyTo:=@Replace(CopyTo;@UserName+"@"+@Domain;"");
   ```

Note: The preceding example works only with non-hierarchical
names (those IDs certified by a non-hierarchical certifier).

---

## @DoWhile

# @DoWhile (Formula Language)

Executes one or more statements iteratively while a condition
is true. Checks the condition after executing the statements.

Note: This
@function is new with Release 6.

## Syntax

**@DoWhile(**  *statement*  **;**  *...*  **;**  *condition*  **)**

## Parameters

*statement*

A
formula language statement. The maximum number of statements you can
include is 254.

*condition*

Expression that returns
a value of True (1) or False (0).

## Return value

*true*

True (1) unless an error occurs
during execution of the condition. An "unexpected data type" error
occurs if the conditional expression results in a non-numeric value.

## Usage

@DoWhile
executes the statements then evaluates the condition. If the condition
is True (1), @DoWhile executes the statements and evaluates the condition
again. If the condition is False (0), @DoWhile terminates.

Tip: If
you are looping through a field containing a list, be sure the Allow
multiple values check box is selected in the Field Properties box
for the list field.

For other iterative statements, see [@For](H_FOR_FUNCTION.html "Executes one or more statements iteratively while a condition remains true. Executes an initialization statement. Checks the condition before executing the statements and executes an increment statement after executing the statements.") and [@While](H_WHILE_FUNCTION.html "Executes one or more statements iteratively while a condition is true. Checks the condition before executing the statements.").

## Examples

This agent displays the elements of
the Categories field one at a time.

```
@If(@Elements(Categories) = 0; @Return(0); "");
n := 1;
	@DoWhile(
		@Prompt([OK]; "Category " + @Text(n); Categories[n]);
		n := n + 1;
	n <= @Elements(Categories)
)
```

---

## @EditECL

# @EditECL (Formula Language)

Displays the administration "Workstation Security: Execution
Control List" dialog box for a specified address book and name, which
lets you change that administration ECL. Administrators can name Administration
ECLs. The name is not usually a user name, but whatever name the administrator
chooses; for example, Manager, Developer, or LimitedAccess.

## Syntax

**@EditECL(** *server* **:** *database* **;** *name* **)**

## Parameters

*server* **:** *database*

Text
list. The server location and file name of the address book. Omit *server* or
specify it as "" (null) for the local Notes/Domino directory.

*name*

Text.
The name of the ECL. Specify "" (null) for the unnamed ECL.

## Usage

You
cannot use this function in Web applications.

## Examples

This formula edits the administration
ECL named "Developers" in the address book on the server Marketing.

```
@EditECL("Marketing" : "names.nsf"; "Developers")
```

---

## @EditUserECL

# @EditUserECL (Formula Language)

Displays the "Workstation Security: Execution Control List"
dialog box, which allows you to change your personal ECL for the current
workstation.

## Syntax

**@EditUserECL**

## Usage

You
cannot use this function in Web applications.

---

## @Elements

# @Elements (Formula Language)

Calculates the number of text, number, or time-date values
in a list. This function always returns a number to indicate the number
of entries in the list.

## Syntax

**@Elements(**  *list*  **)**

## Parameters

*list*

Text
list, number list, or time-date list.

## Return value

*numElements*

Number. The number of
elements in the list. If the field value is a null string, @Elements(list)
returns the number 0. [@Count](H_COUNT.html "Calculates the number of text, number, or time-date values in a list. This function always returns a number to indicate the number of entries in the list.") returns
1 if the field value is a null string or not a list value.

## Usage

You
can use @Elements in the condition statement of @For functions to
set the loop count equal to the number of elements in the list:

@For(n
:= 1; n <= @Elements(*list*); n := n + 1;*formula*)

## Examples

1. This example returns 4 if the list in the SalesForce field is
   "Rogers":"Binney":"Harris":"Larson."

   ```
   @Elements(SalesForce)
   ```
2. This example returns 2.

   ```
   @Elements("Jones":"Portsmore")
   ```
3. This example returns 5.

   ```
   3 + @Elements("Liston":"Reed")
   ```
4. This example, when added to the concat field, concatenates each
   element in the dogs field, containing, "Poodles":"Huskies":"Corgis"
   with each element in the love field, containing: "I love ":"I love
   ":"I love ":

   ```
   @For(n := 1;n <= @Elements(dogs); n := n+1;
   FIELD concat := @If(n = 1;love[n] + dogs[n];concat : (love[n] + dogs[n])));
   concat
   ```

   The result of this formula is: I love Poodles;I
   love Huskies;I love Corgis.

---

## @EnableAlarms

# @EnableAlarms (Formula Language)

Starts or stops the alarm daemon.

## Syntax

**@EnableAlarms(** *enableAlarms* **)**

*enableAlarms*

Flag.
The text "0" or "1". Specify "0" to disable and "1" to enable.

## Usage

@EnableAlarms
brings up the alarm daemon and sets the $EnableAlarms notes.ini variable.
Once the variable is set, re-entering Notes/Domino brings up the alarm
daemon. The "0" option stops the alarm daemon if it is running.

---

## @Ends

# @Ends (Formula Language)

Determines if a substring is at the end of a string.

## Syntax

**@Ends(**  *string*  **;**  *substring*  **)**

## Parameters

*string*

Text
or text string. The string to search.

*substring*

Text
or text string. The string to search for at the end of *string.*

## Return value

*flag*

Boolean

* 1 (True) indicates that the substring is at the end of *string*
* 0 (False) indicates that the substring is not at the end of *string*

## Usage

This
function is case-sensitive.

If the either parameter is a list,
the function tests each element of the second parameter against each
element of the first parameter and returns 1 if any match occurs.

## Examples

1. This example returns 1.

   ```
   @Ends("Hi There";"re")
   ```
2. This example returns 0.

   ```
   @Ends("Hi There";"The")
   ```
3. This formula checks to see if the end of the Signature field contains
   the strings "Owens" or "Irons" or "Baker." If it does, the string
   Verify Signature is returned; otherwise, the string Don't Verify Signature
   is returned.

   ```
   @If(@Ends(Signature;"Owens":"Irons":"Baker");"Verify signature"; "Don't Verify Signature")
   ```

---

## ENVIRONMENT

# ENVIRONMENT (Formula Language)

A reserved word that sets or gets an environment variable stored in the user's notes.ini
file (Windowsâ¢and UNIXâ¢) or NotesÂ® Preferences file
(Macintosh).

## Syntax

**ENVIRONMENT**  *variable*  **:=**  *textValue*  **;**

## Usage

To
get the value of an environment variable, use @Environment. To set
the value of an environment variable, you can also use @Environment,
or you can use @SetEnvironment.

For Web applications, use predefined
field names to gather information about the Web user's environment
by requesting Common Gateway Interface (CGI) environment variables.

## Examples

1. This example returns 5, if that is the value of the variable $IEVersonMajor
   stored in the current user's notes.ini or NotesÂ® Preferences file.

   ```
   @Environment("IEVersionMajor")
   ```
2. This example places a variable called OrderNumber in the current
   user's notes.ini or NotesÂ® Preferences
   file, and assigns it a value of zero.

   ```
   @Environment("OrderNumber";"0")
   ```
3. To save users time while completing Profile documents, you might
   want to automatically fill in an office location for them. You can
   create an editable text field called OfficeLocation. Its default formula
   is:

   ```
   @Environment("ENVOfficeLocation")
   ```

   Its
   input-translation formula is:

   ```
   @Environment("ENVOfficeLocation"; OfficeLocation);
   OfficeLocation
   ```

   The first time the user creates a Profile
   document, the OfficeLocation field is blank, so the user types in
   the office location. When the document is saved, the contents of the
   OfficeLocation field are saved in the notes.ini or NotesÂ® Preferences file. The next time the user
   creates a Profile document, the office location is retrieved from
   the environment variable ENVOfficeLocation, and the user doesn't have
   to type it in again (unless the office location changes, in which
   case the user edits the field).

   You could also write the input-translation
   formula using either @SetEnvironment or the ENVIRONMENT keyword, both
   of which achieve the same result:

   ```
   @SetEnvironment("ENVOfficeLocation"; OfficeLocation);
   OfficeLocation
   ```

   or

   ```
   ENVIRONMENT ENVOfficeLocation:= OfficeLocation;
   OfficeLocation
   ```
4. In addition to the OfficeLocation, you might want to use an environment
   variable to store a user's birthday. You can create an editable time
   field called Birthday. Its default formula is similar to the one used
   for OfficeLocation:

   ```
   @Environment("ENVBirthday")
   ```

   Its
   input-translation formula uses @Text to convert the time value into
   text:

   ```
   @SetEnvironment("ENVBirthday"; @Text(Birthday));
   Birthday
   ```

   Use @Text to write a similar input-translation
   formula for a number field.
5. You want to generate sequential numbers on a per user basis, and
   you want to store the number in a field called OrderNumber. Define
   the field OrderNumber to be a Text data type; it must be some form
   of computed field. You can then write the following formula for the
   field.

   ```
   Temporary := @Environment("OrderNumber");
   Temporary2 := @If(Temporary="";"0";Temporary);
   CurrentOrderNumber := @TextToNumber(Temporary2);
   NextOrderNumber := CurrentOrderNumber + 1;
   ENVIRONMENT OrderNumber := @Text(NextOrderNumber);
   @Text(CurrentOrderNumber);
   ```
6. This formula tests whether an environment variable called OrderNumber
   has been stored in the user's notes.ini or NotesÂ® Preferences file. If there is no such
   variable stored, @SetEnvironment initializes it to zero. If a value
   has already been stored, @Return returns it and stops the formula
   from executing.

   ```
   @If(@Environment(OrderNumber)=""; @SetEnvironment("OrderNumber";"0"); @Return(@Environment("OrderNumber")))
   ```
7. Two agents are used to look up a list of possible group names
   that users might belong to, prompt the user to select one, and then
   enter that name in the Group field for all selected documents (which,
   in this case, pertain to the current user).

   The **Set Group** agent
   looks up the list of group names stored in column 1 of the Service
   Requests - By Group view, prompts the user to select a group name,
   and then stores the selected name in the TmpName environment variable
   before running the "(Set Group Helper)" agent. The "(Set Group Helper)"
   agent then retrieves the group name from the user's notes.ini or NotesÂ® Preferences file and stores
   it in the Group name field for all selected documents.

   **Set
   Group** agent executes once:

   ```
   GroupList:=@DbColumn("":"NoCache";"";
   "Service Requests\\By Group";1);
   Group:=@Prompt([OKCancelEditCombo];"Choose a group";"Choose 
   	a group";"Marketing";GroupList);
   Tmp1:=@Environment("TmpName";Group);
   @Command([RunAgent];"(Set Group Helper)");
   ```

   **(Set
   Group Helper)** agent runs on each selected document:

   ```
   FIELD Group:=@Environment("TmpName");
   ```

---

## @Environment

# @Environment (Formula Language)

Sets or returns an environment variable stored in a formula.

## Syntax

**@Environment(**  *variable*  **)
@Environment(**  *variable*  **;**  *value*  **)**

## Parameters

*variable*

Text
or text list. The name of the environment variable you want to retrieve.
To retrieve multiple environment variables, use a text list.

*value*

Text.
Optional. The value you want to assign to the environment variable.
Since users have their own notes.ini or NotesÂ® Preferences file, this value can be customized
for each user. Omit this parameter if you just want to retrieve the
value, not set it.

* If *variable* is a text list, every environment variable
  in the list will be assigned *value.*
* If *value* is a text list, only the first value in the list
  is used; the rest are ignored.

## Return value

*environmentVariable*

Text. The value
of the environment variable you specified. To use the return value
in arithmetic operations, use @TextToNumber to convert it to a number.

## Usage

Use
@Environment when you want to set an environment variable within a
formula. If it's to be nested within another @function (such as @If
or @Do), use @SetEnvironment instead.

The ENVIRONMENT keyword
works the same as @Environment.

@Environment cannot be used
in column or selection formulas; it's only intended for use in field
formulas, toolbar buttons, and agents. Some formulas, such as scheduled
agents, are run on the server instead of on the user's workstation.
In this case, the environment variables affected are the *server* environment
variables, not the workstation variables. You can use a computed text
formula to retrieve variables, but not to set variables.

You can also use @Environment to get the value of an environment variable stored the user's
notes.ini file (Windowsâ¢ and UNIXâ¢) or NotesÂ®
Preferences file (Macintosh). You can only set and retrieve the values of variables
that begin with a dollar sign ($) symbol. Do not include the dollar sign in the
variable parameter. For instance, to change the value of the $EnableAlarms INI
variable from 1 to 0, enter:

```
@Environment("EnableAlarms";"0")
```

For
Web applications, use predefined field names to gather information
about the Web user's environment by requesting Common Gateway Interface
(CGI) environment variables.

## Examples

1. This example returns 5, if that is the value of the variable $IEVersonMajor
   stored in the current user's notes.ini or NotesÂ® Preferences file.

   ```
   @Environment("IEVersionMajor")
   ```
2. This example places a variable called OrderNumber in the current
   user's notes.ini or NotesÂ® Preferences
   file, and assigns it a value of zero.

   ```
   @Environment("OrderNumber";"0")
   ```
3. To save users time while completing Profile documents, you might
   want to automatically fill in an office location for them. You can
   create an editable text field called OfficeLocation. Its default formula
   is:

   ```
   @Environment("ENVOfficeLocation")
   ```

   Its
   input-translation formula is:

   ```
   @Environment("ENVOfficeLocation"; OfficeLocation);
   OfficeLocation
   ```

   The first time the user creates a Profile
   document, the OfficeLocation field is blank, so the user types in
   the office location. When the document is saved, the contents of the
   OfficeLocation field are saved in the notes.ini or NotesÂ® Preferences file. The next time the user
   creates a Profile document, the office location is retrieved from
   the environment variable ENVOfficeLocation, and the user doesn't have
   to type it in again (unless the office location changes, in which
   case the user edits the field).

   You could also write the input-translation
   formula using either @SetEnvironment or the ENVIRONMENT keyword, both
   of which achieve the same result:

   ```
   @SetEnvironment("ENVOfficeLocation"; OfficeLocation);
   OfficeLocation
   ```

   or

   ```
   ENVIRONMENT ENVOfficeLocation:= OfficeLocation;
   OfficeLocation
   ```
4. In addition to the OfficeLocation, you might want to use an environment
   variable to store a user's birthday. You can create an editable time
   field called Birthday. Its default formula is similar to the one used
   for OfficeLocation:

   ```
   @Environment("ENVBirthday")
   ```

   Its
   input-translation formula uses @Text to convert the time value into
   text:

   ```
   @SetEnvironment("ENVBirthday"; @Text(Birthday));
   Birthday
   ```

   Use @Text to write a similar input-translation
   formula for a number field.
5. You want to generate sequential numbers on a per user basis, and
   you want to store the number in a field called OrderNumber. Define
   the field OrderNumber to be a Text data type; it must be some form
   of computed field. You can then write the following formula for the
   field.

   ```
   Temporary := @Environment("OrderNumber");
   Temporary2 := @If(Temporary="";"0";Temporary);
   CurrentOrderNumber := @TextToNumber(Temporary2);
   NextOrderNumber := CurrentOrderNumber + 1;
   ENVIRONMENT OrderNumber := @Text(NextOrderNumber);
   @Text(CurrentOrderNumber);
   ```
6. This formula tests whether an environment variable called OrderNumber
   has been stored in the user's notes.ini or NotesÂ® Preferences file. If there is no such
   variable stored, @SetEnvironment initializes it to zero. If a value
   has already been stored, @Return returns it and stops the formula
   from executing.

   ```
   @If(@Environment(OrderNumber)=""; @SetEnvironment("OrderNumber";"0"); @Return(@Environment("OrderNumber")))
   ```
7. Two agents are used to look up a list of possible group names
   that users might belong to, prompt the user to select one, and then
   enter that name in the Group field for all selected documents (which,
   in this case, pertain to the current user).

   The **Set Group** agent
   looks up the list of group names stored in column 1 of the Service
   Requests - By Group view, prompts the user to select a group name,
   and then stores the selected name in the TmpName environment variable
   before running the "(Set Group Helper)" agent. The "(Set Group Helper)"
   agent then retrieves the group name from the user's notes.ini or NotesÂ® Preferences file and stores
   it in the Group name field for all selected documents.

   **Set
   Group** agent executes once:

   ```
   GroupList:=@DbColumn("":"NoCache";"";
   "Service Requests\\By Group";1);
   Group:=@Prompt([OKCancelEditCombo];"Choose a group";"Choose 
   	a group";"Marketing";GroupList);
   Tmp1:=@Environment("TmpName";Group);
   @Command([RunAgent];"(Set Group Helper)");
   ```

   **(Set
   Group Helper)** agent runs on each selected document:

   ```
   FIELD Group:=@Environment("TmpName");
   ```

---

## @Error

# @Error (Formula Language)

Allows you to generate an error condition within an expression.
This is useful if you want to evaluate the current values in several
fields and need to know if an error has occurred in the entry of any
of them.

## Syntax

**@Error**

## Return value

**@Error**

## Usage

Use
@IsError to test for a data entry error.

When an error has
occurred, @Error is returned. The function cannot return any other
value.

@Error always results in an error condition when it
tests a single value. If you use @Error alone as a formula, you will
always generate an error.

You cannot test for an @Error value
with any operator or @function other than @IsError. If you use an
error value as an argument to an operator or @function, the return
is always @Error.

## Examples

Read the following examples closely
to understand the difference between @Error and @IsError.

1. This example returns the value in the Price field if it is greater
   than 100, otherwise it returns @Error.

   ```
   @If(Price>100;Price;@Error)
   ```
2. This example checks to see if there is an @Error in the field
   named Price. If there is an @Error, the string There is an error in
   the price field is returned. If the contents of the field are anything
   other than @Error, Price Field Okay is returned.

   ```
   @If(@IsError(Price);"There is an error in the price field";"Price Field Okay")
   ```

---

## @Eval

# @Eval (Formula Language)

At run-time, compiles and runs each element in a text expression
as a formula. Returns the result of the last formula expression in
the list or an error if an error is generated by any of the formula
expressions in the list.

Note: This @function is new with Release 6.

## Syntax

**@Eval(** *textExpressions* **)**

## Parameters

*textExpressions*

Any
text expressions that you want @Eval to evaluate. Surround the text
expression to be evaluated with braces ({ }) or quotation marks ("
"). If you use quotation marks, escape quotes around individual text
expressions within the formula with back-slashes (\). If you use braces,
escape the right brace. Use the plus sign (+) to concatenate text
expressions.

## Return value

*lastExpression*

The
value of the last expression.

## Usage

This
function is useful in agents, hotspot buttons, and toolbar buttons
and when you want to evaluate multiple text expressions at run-time
from within a single @function.

Use of @Eval in view columns
and selection formulas may produce unexpected results. Because this
function is evaluated at run-time, the view engine is unable to follow
its standard procedure of analyzing the formulas ahead of time to
discover what types of @functions it will encounter and prepare for
them.

## Examples

1. This formula concatenates the value of the temporary variable
   x and the text expression "bar." It returns "rebar."

   ```
   x := "re";
   @Eval({x + "bar"});
   ```
2. The following code, when added to an action button, creates the
   field "comment" and adds the user's input to it on the fly.

   ```
   input := {FIELD comment := @Prompt([OKCANCELEDIT];"Input";"Input a value"; "Default");};
   @Eval(input);
   ```

   To view the content of the comment field,
   use the following code in a hotspot or action button.

   ```
   @Prompt([OK];"Value of comment field";@GetField("comment"))
   ```

---

## @Exp

# @Exp (Formula Language)

Calculates the number e (approximately 2.718282) raised
to the specified power (this value can contain up to 14 decimal places).

## Syntax

**@Exp(**  *power*  **)**

## Parameters

*power*

Number
or number list. The power to which you want to raise *e.* Notes/Domino
can only calculate this function when the number is between -11355.1371
and 11356.5234. Values outside this range will return the value @ERROR.

## Return value

*resultString*

Number
or number list. The number *e* raised to the power of the parameter.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

Natural
logs use the constant *e* as their base. Use @Exp in formulas
requiring exponential functions.

## Examples

1. This example returns 3.49034295746184 (e raised to the power of
   1.25).

   ```
   @Exp(1.25)
   ```
2. This example returns 0.28650479686019 (e raised to the power
   of -1.25).

   ```
   @Exp(-1.25)
   ```
3. This example returns 3.49034295746184 and 0.28650479686019 in
   a list.

   ```
   @Exp(1.25 : (-1.25))
   ```

---

## @Explode

# @Explode (Formula Language)

Returns a text list composed of the elements of a text
string or date range.

* If you specify a text string, elements are defined as sequences
  of characters separated by separator characters and newlines.
* If you specify a time-date range, elements are defined as individual
  days within the range.

## Syntax

**@Explode(** *dateRange*  **)
@Explode(**  *string*  **) @Explode(**  *string*  **;**  *separators*  **)
@Explode(**  *string*  **;**  *separators*  **;**  *includeEmpties*  **)
@Explode(**  *string*  **;**  *separators*  **;**  *includeEmpties*  **;** *newlineAsSeparator* **)**

## Parameters

*dateRange*

Time-date
range or time-date range list. The range of dates that you want to
make into a text list. Specify a valid date-time range, not a string
representation of one. For example, @Explode( "05/01/96 - 05/02/96"
) is invalid because the parameter is a string. Use @Explode( [05/01/96
- 05/02/96] ).

*string*

Text or text list. The
string that you want to make into a text list.

*separators*

Text.
Optional. One or more characters that define the end of an element
in *string*. The default separators are " ,;" (space, comma,
semicolon), which means that DominoÂ® adds
a new element to the text list each time a space, comma, or semicolon
occurs in the original string*.* When you use more than one character
to specify separators, each character defines one separator. For example,
the specification "and" breaks the string at each occurrence of "a,"
"n," and "d"; it does not break the string at each occurrence of the
word "and." The newline is a separator, regardless of the specification
of this parameter, unless *newlineAsSeparator* is specified as
False.

*includeEmpties*

Boolean. Optional. Specify
True (1) to place an empty string ("") in the returned list when a
separatorappears at the beginning or end of the string*,* or
two separators appear consecutively in the string*.* Specify
False (0) to not include empty list elements for leading, trailing,
and consecutive separators. Defaults to False.

*newlineAsSeparator*

Note: This parameter is new with Release 6.

Boolean.
Optional. Specify True (1) to treat the newline as a separator*.* Specify
False (0) to not treat the newline as a separator. Defaults to True.

## Return value

*explodedString*

Text list. A list
containing each element found in *string,* or each date found
in *dateRange.*

## Usage

If
the first parameter is a list, the function concatenates the list
elements treating the list boundaries as separators.

## Examples

1. This example returns a, b, and c in a list.

   ```
   @Explode("a,b,c")
   ```
2. This example returns a, b, c, d, and e in a
   list.

   ```
   @Explode("a,b,c" : "d,e")
   ```
3. This example returns a list containing "Weekly," "Status," and "Report" if the content of the
   Topic field is "Weekly Status Report"; "Weekly,Status,Report"; "Weekly;Status;Report"; or
   "Weekly," "Status," and "Report" separated by
   newlines.

   ```
   @Explode(Topic)
   ```
4. This example returns a list containing "Weekly," "Status," and "Report" if the content of the
   Topic field is "Weekly+Status+Report"; or "Weekly," "Status," and "Report" separated by
   newlines.

   ```
   @Explode(Topic; "+&")
   ```
5. This example specifies the default separators but inserts empty elements for leading, trailing,
   and consecutive separators.

   ```
   @Explode(Topic; " ,;"; @True)
   ```
6. This example specifies the defaults for parameters 2 and 3, but does not treat newlines as
   separators.

   ```
   @Explode(Topic; " ,;"; @False, @False)
   ```
7. This example returns "Please send resume + references" if the content of the entry field is:
   "Please send resume &
   references".

   ```
   @Implode( @Explode( entry; "&" ); "+" )
   ```
8. This example returns "Attendance grows at UCLA; Pomona Colleges; and USC" if the content of the
   Headline field is "Attendance grows at UCLA, Pomona Colleges, and
   USC".

   ```
   @Explode(Headline;",")
   ```
9. This example returns 4 if the content of the Country field is "Mexico, Guatemala, Costa Rica, El
   Salvador".

   ```
   @Elements(@Explode(Country; ","))
   ```
10. This example returns 07/02/96; 07/03/96; 07/04/96;
    07/05/96.

    ```
    @Explode([07/02/96 - 07/05/96])
    ```
11. This example returns 07/01/94; 05/01/94; 10/01/94; 10/02/94; 10/03/94; 04/01/94; 04/02/94;
    04/03/94. Note the order in which the dates are returned: single date-time values are returned
    first, followed by exploded date-time ranges. The return value is a text
    list.

    ```
    @Explode([07/01/94]:[10/01/94 - 10/03/94]:[05/01/94]:[04/01/94 - 04/03/94])
    ```
12. You might want users to be able to enter a range of dates into
    an editable, multi-value, time-date field called Duration and display
    them in a computed, multi-value, text field called Days. Give the
    Duration field the following input-translation formula: @Date(Duration).
    Give the Days field the following formula: @Explode(Duration). Users
    can enter dates into the Duration field in this format: 04/16/71-04/18/71.
13. This example returns date
    values.

    ```
    @Explode(@Eval("[" + @Text(@GetField("DateStart")) + " - " + @Text(@GetField("DateEnd")) + "]")))
    ```

---

## @Failure

# @Failure (Formula Language)

Indicates that input to a field does not meet validation.

## Syntax

**@Failure(**  *string*  **)**

## Parameters

*string*

Text.
The error message you want displayed to the user.

## Return value

*string*

Text. The error message.

## Usage

@Failure
is intended for use only in input validation formulas.

@Failure
does not terminate execution of the formula. Use [@Return](H_RETURN.html "Immediately stops the execution of a formula and returns the specified value. This is useful when you only want the remainder of the formula to be executed only if certain conditions are True.") to force a formula to exit.

A
field passes validation if its input validation formula returns [@Success](H_SUCCESS.html "Returns 1 (True). Use this function with @If in field validation formulas to indicate that the value entered satisfies the validation criteria.") (which is the value 1). Any
other return value means failure, and a text return value is displayed
as an error message. The use of @Failure in an input validation formula
is not required, therefore, but is recommended for clarity.

## Examples

This example show an input validation
formula. It returns the error message "Area codes have only 3 digits"
if the user enters a number greater than 999 in the field named AreaCode.

```
@If(AreaCode<1000;@Success;@Failure("Area codes have only 3 digits"))
```

---

## @False

# @False (Formula Language)

Returns the number 0.

## Syntax

**@False**

## Return value

Returns
the number 0.

## Usage

This
function is equivalent to @No.

## Examples

1. This example returns 0.

   ```
   @False
   ```
2. This example returns 0 if the value in the field named Cost is
   100 or less.

   ```
   @If(Amount < 1000; @False; !(@UserRoles = "[Manager]")
   ```

---

## FIELD

# FIELD (Formula Language)

A reserved word that is necessary when you are assigning
values to fields that are stored in a document (as opposed to temporary
fields). You can use FIELD to change the contents of an existing field
or to create new fields.

You cannot use the FIELD reserved word within an @function.
Use [@SetField](H_SETFIELD.html "Assigns a value to a field stored within a document (use @Set for temporary variables). This is similar to using the FIELD keyword, except that @SetField can be used within another @function. If the field does not exist, this command creates it and applies the specified value to it.") instead.

## Syntax

**FIELD**  *fieldName*  **:=** *value* **;**

CAUTION: When you use FIELD to create a new field in existing
documents, make sure that you do not duplicate the name of a field
that already exists.

In some cases, action formulas that
don't evaluate to a result (for example, a button formula) return
a "No Main or Selection expression in formula" error message. You
can supply a value such as an empty string (""), or you could provide
an expression at the end of the formula, as shown:

SELECT
@All

## Usage

This
reserved word is most useful in agent, button, hotspot, and action
formulas. It does not work in column, selection, hide-when, window
title, or form formulas.

## Examples

1. There is a field named Company on a form. When users compose documents
   with this form, they enter the name of the company in this field.
   You can write the following filter, which adds "Inc." to the contents
   of the Company field:

   ```
   FIELD Company := Company + ", Inc.";
   ```
2. Alternatively, you can create a new field called CompanyName in
   the form to hold the name of the company plus "Inc.", by assigning
   it the following formula:

   ```
   FIELD CompanyName := Company + ", Inc.";
   ```
3. To delete the field CompanyName from an existing set of documents,
   you can use the following formula:

   ```
   FIELD CompanyName := @DeleteField;
   ```
4. To assign a value to a field and use it in an @function:

   ```
   FIELD fullname := @If(fullname = "";firstname + " " + lastname;fullname)
   ```

---

## @FileDir

# @FileDir (Formula Language)

Returns the directory portion of a path name, that is,
the path name minus the file name.

Note: This @function is new with Release 6.

## Syntax

**@FileDir(**  *pathname* **)**

## Parameters

*pathname*

Text
or text list. Path name of a file.

## Return value

*directory*

Text or text list. The
directory part of the path name.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

The
directory part of a file name is everything to the left of the file
name as demonstrated in the following:

| Path name | Directory portion |
| --- | --- |
| europe.dat | Null |
| c:\\europe.dat | c:\ |
| c:\\market\\data\\europe.dat | c:\market\data\ |
| \\europe.dat | \ |

Use [@Right](H_RIGHT.html "Returns the rightmost characters in the string. You can specify the number of rightmost characters you want returned, or you can indicate that you want all the characters following a specific substring.") with the path
name and @FileDir to extract the file name.

## Examples

1. This computed field formula returns the directory part of the
   file named by Pathname. If Pathname is list, this formula returns
   a list of the directory parts of the named files.

   ```
   @FileDir(Pathname)
   ```
2. This computed field formula returns the file name part of the
   file named by Pathname.

   ```
   @Right(Pathname; @FileDir(Pathname))
   ```
3. This agent formula displays the directory part of the current
   database name.

   ```
   @Prompt([OK];
   "File directory";
   @FileDir(@Subset(@DbName; -1)))
   ```
4. This agent formula displays the file name part of the current
   database name.

   ```
   pathname := @Subset(@DbName; -1);
   @Prompt([OK];
   "File name";
   @Right(pathname; @FileDir(pathname)))
   ```

---

## @FloatEq

# @FloatEq (Formula Language)

Compares two numbers for equality within a confidence range.

Note: This @function is new with Release 6.

## Syntax

**@FloatEq(**  *number* **;** *number* **;** *confidenceRange*  **)**

## Parameters

*number*

Number
or number list. Any numeric value.

*confidenceRange*

Number.
Optional. The amount within which the numbers must be equal. Defaults
to 0.0001.

## Return value

*flag*

Boolean.

* Returns 1 (True) if the difference of the numbers is less than
  the confidence range.
* Returns 0 (False) if the difference of the numbers exceeds or
  is equal to the confidence range.

## Examples

This action formula compares the fields
SpecifiedLength and MeasuredLength, and displays a message if the
fields are not within 0.01. If MeasuredLength is a list, the message
appears only if every element is not within 0.01.

```
@If(@FloatEq(SpecifiedLength; MeasuredLength; 0.01); "";
@Prompt([OK]; "Length is out of spec";
@Text(MeasuredLength)))
```

## Usage

If
either parameter is a list, the function compares the first two parameters
pair-wise and returns 1 if any comparison is within range.

@FloatEq
is helpful in dealing with the inexactness of floating point operations.

---

## @FontList

# @FontList (Formula Language)

Provides a list of available fonts on the NotesÂ® client where this @function is executed.

Note: This @function is new with Release 5.

## Syntax

**@FontList**

## Return value

*availablefont*

Text list. All the
available font names. For "Default Serif", "Default Sans Serif", and
"Default Monospace" fonts, @FontList returns alias values as follows:

* Default Serif, 0
* Default Sans Serif, 1
* Default Monospace, 4

## Examples

1. The following formula returns a list of font names such as "Arial"
   : "Courier" : "Default Sans Serif|1" : "Default Serif|0" : "Default
   Monospace|4" : "Times New Roman........."

   ```
   @FontList
   ```
2. The following code, when added to the "Change font" hotspot button
   on a form enables the user to apply the font they select from the
   "fontList" listbox field to the text in the "Body" rich text field.

   ```
   @Command([EditGoToField];"Body");
   @Command([EditSelectAll]);
   @Command([TextSetFontFace];fontList)
   ```

   To display the
   fonts available to the user in the fontList field, set the field Type
   to a list field, by choosing Listbox or Dialog list, for example.
   On the control tab of the Field Properties box, select the Use formula
   for choices option and enter the following formula:

   ```
   @FontList
   ```

## Usage

Use
@FontList as the keyword formula for a list field to display a list
of fonts that are available on to the users.

This function
does not work in Web applications.

## Examples

1. The following formula returns a list of font names such as "Arial"
   : "Courier" : "Default Sans Serif|1" : "Default Serif|0" : "Default
   Monospace|4" : "Times New Roman........."

   ```
   @FontList
   ```
2. The following code, when added to the "Change font" hotspot button
   on a form enables the user to apply the font they select from the
   "fontList" listbox field to the text in the "Body" rich text field.

   ```
   @Command([EditGoToField];"Body");
   @Command([EditSelectAll]);
   @Command([TextSetFontFace];fontList)
   ```

   To display the
   fonts available to the user in the fontList field, set the field Type
   to a list field, by choosing Listbox or Dialog list, for example.
   On the control tab of the Field Properties box, select the Use formula
   for choices option and enter the following formula:

   ```
   @FontList
   ```

---

## @For

# @For (Formula Language)

Executes one or more statements iteratively while a condition
remains true. Executes an initialization statement. Checks the condition
before executing the statements and executes an increment statement
after executing the statements.

Note: This @function is new with Release 6.

## Syntax

**@For(**  *initialize* **;** *condition* **;** *increment* ; *statement*  **;**  *...*  **)**

## Parameters

*initialize*

A
statement that assigns an initial value to a variable in *condition*.

*condition*

Expression
that returns a value of True (1) or False (0).

*increment*

A
statement that changes the initialized variable, typically incrementing
it.

*statement*

A formula language statement.
The maximum number of statements you can include is 252.

## Return value

*true*

True (1) unless an error occurs
during execution of the condition. An "unexpected data type" error
occurs if the conditional expression results in a non-numeric value.

## Usage

@For
executes the initialize statement once. Next @For evaluates the condition.
If the condition is True (1), @For executes the statements, executes
the increment statement, and evaluates the condition again. If the
condition is False (0), @For terminates.

Tip: If you
are looping through a field containing a list, be sure the "Allow
multiple values" check box is selected in the Field Properties box
for the list field.

The formula engine exits a formula or
breaks an infinite loop if the time spent performing the iterations
exceeds the standard timeout value allowed for an operation.

For
other iterative statements, see [@DoWhile](H_DOWHILE_FUNCTION.html "Executes one or more statements iteratively while a condition is true. Checks the condition after executing the statements.") and [@While](H_WHILE_FUNCTION.html "Executes one or more statements iteratively while a condition is true. Checks the condition before executing the statements.").

## Examples

1. This agent displays the elements of the Categories field one at a time.

   ```
   @For(n := 1;
   n <= @Elements(Categories);
   n := n + 1;
   @Prompt([OK]; "Category " + @Text(n); Categories[n]))
   ```
2. This computed field formula concatenates the list elements in
   the fname and lname fields:

   ```
   @For(n :=1; n<=@Elements(fname); n:= n + 1;
   full := @If(n=1;fname[n] + " " + lname[n];full : (fname[n] + " " + lname[n])));
   full
   ```

   If
   fname contains: "Catherine":"Patricia":"Maureen" and lname contains:
   "Rolling":"Kearns":"Legacy", the result is: "Catherine Rolling;Patricia
   Kearns;Maureen Legacy." If fname and lname each contain a different
   number of elements, be sure to include the field that has fewer elements
   in the @Elements function or an "Array index out of bounds" error
   results.
3. This computed field formula displays the longest name in a text
   list of poets names stored in the poets field. If the poets field
   contains "T.S. Eliot":"Dorothy Parker":"Edna St. Vincent Millay":"e.e.
   cummings": this field displays Edna St. Vincent Millay.

   ```
   temp := "";
   @For(n := 1; n <= @Elements(poets); n := n + 1;
   @If(@Length(poets[n])>@Length(temp);
   temp := poets[n];temp));
   temp
   ```

---

## @FormLanguage

# @FormLanguage (Formula Language)

Returns the language of the current form.

Note: This @function is new with Release 5.

## Syntax

**@FormLanguage**

## Return value

*language*

Text. The language specified
for the current form. Format of this information is based on RFC1766.

## Usage

If
the database contains multilingual forms, you can specify the language
for each form.

## Examples

The following formula returns "en-US'
when used on a form designed in English(United States).

```
@FormLanguage
```

---

## @GetAddressBooks

# @GetAddressBooks (Formula Language)

Returns a list of the address books associated with a client
(if the current database is local) or server.

Note: This @function is new with Release 6.

## Syntax

**@GetAddressBooks(
[** *options*  **] )**

## Parameters

**[**  *options*  **]**

Keyword.
You must select one of the following keywords as an argument for this
@function:

**[TITLES]**

Returns the file names
of the address books associated with the current database.

**[FIRSTONLY]**

Displays
only the first database in the returned text list of address book
names.

## Return value

*address books*

Text or text list.
When the current database is hosted by a server, returns the address
books that exist on that server. When the current database is hosted
locally, returns the address books listed in the NAMES= line of the
notes.ini file for that client.

## Examples

1. This code populates the chooseAddress listbox field options with
   "names.nsf" and "AcmeNorthServer!!names.nsf" when added to the Use
   formula for choices textbox on the Control tab of the field properties
   box if the database containing the chooseAddress field is running
   on the Acme server:

   ```
   @GetAddressBooks([TITLES])
   ```
2. This code populates the chooseAddress listbox field options with
   "names.nsf" when added to the Use formula for choices textbox on the
   Control tab of the field properties box if the database containing
   the chooseAddress field is running on the Acme server:

   ```
   @GetAddressBooks([FIRSTONLY])
   ```

---

## @GetCurrentTimeZone

# @GetCurrentTimeZone (Formula Language)

Returns the current operating system's time zone settings
in canonical time zone format.

Note: This function is new with Release 6.

## Syntax

**@GetCurrentTimeZone**

## Return value

*fieldValue*

Canonical time zone representing
the time zone settings of the operating system.

## Usage

Use
with the [@TimeZoneToText](H_TIMEZONETOTEXT.html "Converts a canonical time zone value to a human-readable text string.") function
to translate the time zone value returned into a readable time zone
value.

## Examples

1. This code, when added as the default value for a field, returns
   Z=5$DO=1$DL=4 1 1 10 - 1$ZX=10$ZN=Eastern if the current operating
   system's time zone setting is GMT-05:00 Eastern Time.

   ```
   @GetCurrentTimeZone
   ```
2. This code, when added as the default value for a field, returns
   (GMT-5:00) Eastern Time (US & Canada).

   ```
   @TimeZoneToText(@GetCurrentTimeZone)
   ```

---

## @GetDocField

# @GetDocField (Formula Language)

Given the unique ID of a document, returns the contents
of a specific field on that document. The document must reside in
the current database.

## Syntax

**@GetDocField(**  *documentUNID*  **;**  *fieldName*  **)**

## Parameters

*documentUNID*

Text.
The unique ID of a document. [@DocumentUniqueID](H_DOCUMENTUNIQUEID.html "The universal ID, which uniquely identifies a document across all replicas of a database. In text format, the universal ID is a 32-character combination of hexadecimal digits (0-9, A-F).") specifies
the unique id of the current document. To specify the unique id of
the parent document, you can use $Ref as the parameter. $Ref is the
name of the special field on a response document that stores the unique
id of its parent.

*fieldName*

Text. The name
of a field on the document, enclosed in quotation marks. If you store
the field name in a variable, omit the quotation marks here.

## Return value

*fieldValue*

Text or text list; number
or number list; time-date or time-date range. The contents of the
field on the specified document. Returns null if the UNID or field
name is invalid.

## Usage

This
function does not work in column or selection formulas.

## Examples

1. You have a discussion database with main topics and responses.
   In each response, you want to store the subject of the parent document
   in a field called OriginalSubject. You want OriginalSubject to change
   if the subject of the main topic changes, so you write this formula
   for it. $Ref is a special field on a response document that contains
   the unique ID of the parent document.

   ```
   @If(@IsNewDoc; Subject; @GetDocField($Ref; "Subject"))
   ```
2. The following formula can run a scheduled agent to update the
   contents of a child document, based on the parent.

   ```
   FIELD Project:=@GetDocField($Ref; "Project");
   @All
   ```
3. The following formula runs a scheduled agent to update the contents
   of one document based on the content of another. The documents don't
   need to be parent and child. For example, these could be two parent
   documents or two child documents.

   ```
   FIELD Body:=@GetDocField("BB791838F30B20ED852567BA0064DDAF"; "Body");
   @All
   ```

---

## @GetField

# @GetField (Formula Language)

Returns the value of a specified field.

Note: This @function is new with Release 6.

## Syntax

**@GetField** **(**  *fieldName*  **)**

## Parameters

*fieldName*

Text.
The name of a field in the current document.

## Return value

*value*

The value of the specified
field.

## Usage

This
@function returns null if the field does not exist.

This @function
is useful in writing portable code and in other instances where you
want to vary the name of the field.

This function returns the
complete field value, including all values if the field is multivalued,
or the rich text value if it is a rich text field.

## Examples

1. This code, when added to a computed field on a form and accessed
   on the Web or in NotesÂ®, displays
   Hello if "Hello" is the default value of the greeting field.

   ```
   @GetField("greeting")
   ```
2. This computed field formula multiplies values from two fields.
   The fields are named by adding suffixes to the name of the current
   field.

   ```
   @GetField(@ThisName + "_Quantity") * @GetField(@ThisName + "_Cost")
   ```

## Language cross-reference

[FieldGetText
method](H_FIELDGETTEXT_METHOD.html "In a document in read or Edit mode, returns the contents of a field you specify, as a string. If the field is of type numbers or date-time, its contents are converted to a string.") of LotusScriptÂ® NotesUIDocument
class

[GetItemValue
method](H_GETITEMVALUE_METHOD.html "Given the name of an item, returns the value of that item in a document.") of LotusScriptÂ® NotesDocument
class

[getItemValue
method](H_GETITEMVALUE_METHOD_JAVA.html "Returns the value of an item.") of Javaâ¢ Document
class

---

## @GetFocusTable

# @GetFocusTable (Formula Language)

Returns the name, current row, or current column of the
table that is in focus.

Note: This @function is new with Release 6.

## Syntax

**@GetFocusTable** **(
[**  *tableInfoRequest*  **] )**

## Parameters

**[**  *tableInfoRequest*  **]**

Keyword.
The table information to be returned. One of the following:

**[CELLROW]**

Returns
the current row number starting at "1"; returns "0" if a table is
not in focus.

**[CELLCOLUMN]**

Returns the current
column number starting at "1"; returns "0" if a table is not in focus.

**[TABLENAME]**

Returns
the table name (Name/Id under the Table Programming tab in Table Properties);
returns a null string if a table is not in focus or the table has
no name.

## Return value

*tableInfo*

Text. The requested information.

## Usage

This
@function works in the OnHelp event of a form. It is triggered by
selecting Help - Context Help from the menu bar or pressing F1 when:

* The cursor is in a field in a table cell when the document is
  in edit mode
* Text or an object is selected in a table cell and the document
  is in read mode

When focus is in the tab of a tabbed table, [CELLCOLUMN]
always returns zero.

You cannot use this @function in Web applications.

## Examples

This onHelp event returns the name,
row, and column of a table that is currently in focus.

```
row := @GetFocusTable([CELLROW]);
@If(row = "0"; @Prompt([OK]; "*No table*"; "Not in a table");
@Do(
column := @GetFocusTable([CELLCOLUMN]);
name0 := @GetFocusTable([TABLENAME]);
name := @If(name0 = ""; "No name"; name0);
@Prompt([OK]; "*" + name + "*";
"Row " + row + ", column " + column)))
```

---

## @GetHTTPHeader

# @GetHTTPHeader (Formula Language)

In a Web application, returns the value of an HTTP header
from the browser client request being processed by the server.

Note: This @function is new with Release 6.

## Syntax

**@GetHTTPHeader(**  *requestHeader*  **)**

## Parameters

*requestHeaderField*

Text.
The name of a request-header field, for example, "From," "Host," or
"User-Agent."

## Return value

*requestHeaderValue*

Text. The value
of the request-header field, or null if the field does not exist.

## Usage

@GetHTTPHeader
is useful in formulas that run in the context of a browser.

The NotesÂ® client always returns null
for this formula.

See http:/www.w3.org/Protocols for the specification
of a request header.

See [@SetHTTPHeader](H_SETHTTPHEADER.html "In a Web application, sets the value of HTTP headers in the response being generated by the server for the browser client.") for
setting a response header value.

## Examples

These examples return header field
content based on this standard HTTP request:

```
GET /yourdb.nsf/All?OpenView HTTP/1.0
User-Agent: Mozilla 4.0 (X; I; Linux-2.0.35i586)
Host: mylinuxbox.ibm.com
Accept: image/gif, image/jpeg, */*
```

1. This computed field formula returns "Mozilla 4.0 (X; I; Linux-2.0.35i586."

   ```
   @GetHTTPHeader("User-Agent")
   ```
2. This computed field formula returns "mylinuxbox.ibm.com."

   ```
   @GetHTTPHeader("Host")
   ```

---

## @GetIMContactListGroupNames

# @GetIMContactListGroupNames (Formula Language)

Returns the group names in the Instant Messaging Contact List.

Note: This function is new with Release 6.5.

## Syntax

**@GetIMContactListGroupNames**

## Return value

*nameList*

Text list. All group names in the Instant Messaging Contact List.

## Usage

You cannot use this function in Web applications.

---

## @GetPortsList

# @GetPortsList (Formula Language)

Returns a list of enabled or disabled ports.

## Syntax

**@GetPortsList(
[**  *portType*  **] )**

## Parameters

**[**  *portType*  **]**

Keyword.
Must be enclosed in brackets. Use one of the following keywords:

**[ENABLED]**

Returns
a list of currently enabled ports.

**[DISABLED]**

Returns
a list of currently disabled ports.

## Return value

*portsList*

Text list. Each port name
is one element of the list.

## Usage

@GetPortsList
is used by the Public and Personal Address books to determine the
list of available ports for each Location record. You can then select
a port from that list.

This function does not work in column
formulas, selection formulas, or selective replication formulas.

You
cannot use this @function in Web applications.

## Examples

1. This example returns Lan0;TCP;AppleTalk if those are the currently
   enabled ports.

   ```
   @GetPortsList([Enabled])
   ```
2. This example returns COM1;COM2 if those are the currently disabled
   ports.

   ```
   @GetPortsList([Disabled])
   ```

Note: The text list uses the multi-value separator
specified for the current field, or the list separator specified for
the current column in a view.

---

## @GetProfileField

# @GetProfileField (Formula Language)

Retrieves a field from a profile document, and caches the
field value for the remainder of the session.

## Syntax

**@GetProfileField(**  *profilename*  **;**  *fieldname* **;**  *uniqueKey*  **)**

## Parameters

*profilename*

Text.
The name of the profile document that contains the field you want
to access.

*fieldname*

Text. The name of the
field you want to access.

*uniqueKey*

Text. Optional.
The unique key that identifies a profile document.

## Return value

*fieldvalue*

The value of the field.

## Usage

This
function does not work in column, hide-when, section editor, or view
selection formulas. You can use it in toolbar buttons or agents.

You
can use this function on the Web. Use [@SetProfileField](H_SETPROFILEFIELD.html "Sets the value of a field in a profile document or creates a profile document.") to
create a profile document in a Web application. If no profile document
by the name specified as the first parameter to @SetProfileField exists, NotesÂ® creates one. This function
enables you to access the fields in that profile document.

Use
profile documents for values that change infrequently. The profile
is cached in local memory for performance, so if one user changes
the value in the profile, other users will not see the change immediately.
Do not use profile documents to sequentially number documents. Users
should be able to see their own changes immediately in NotesÂ® client applications, since they are updating
their own cache as well as the stored profile document. However, in DominoÂ® web applications, multiple
server processes have their own profile cache, so a change in a profile
document might not immediately apply in all parts of the web application.

## Examples

1. This example gets the contents of the "ProfileCategories" field
   of the "Interest Profile" document.

   ```
   @GetProfileField("Interest Profile"; 
   "ProfileCategories")
   ```
2. This example gets the contents of the "ProfileCategories" field
   of the "Interest Profile" document for the profile document for Monday
   if weekday has "Monday" as its default value.

   ```
   @GetProfileField("Interest Profile"; 
   "ProfileCategories"; "weekday")
   ```
3. This example gets two field values from the age and job fields
   of the "userprofile" profile document and displays them vertically
   in a view column. The following code is in the "profile" field of
   a user-accessible form:

   ```
   @Explode(@GetProfileField("userprofile"; "age"; @UserName):@GetProfileField("userprofile"; "job"; @UserName); ":")
   ```

   The
   column formula of the view that displays these two values has its
   view properties set to Lines per row = 2 and Shrink rows to content
   and column properties set to Multi-value separator = New Line. The
   column value formula is the following:

   ```
   @Trim(profile)
   ```

   The
   @Explode function replaces the semicolon (;) that returns to the profile
   field with the colon (:) which indicates to the @Trim function in
   the column formula that the two values are a text list.
4. The following code, when added to the Update Info action button
   in a Web form, retrieves the user name and address information from
   the user's profile document ("Profile") and fills the name and address
   fields on the Web form with that information:

   ```
   tempName := @GetProfileField("Profile";"userName";@UserName);
   tempAddress := @GetProfileField("Profile";"userAddress";@UserName);
   @SetDocField(@DocumentUniqueID;"name";tempName);
   @SetDocField(@DocumentUniqueID;"address";tempAddress);
   ```

---

## @GetViewInfo

# @GetViewInfo (Formula Language)

Returns a view attribute.

Note: This @function is new with Release 6.

## Syntax

**@GetViewInfo** **(
[**  *attribute*  **] ;** *column*  **)**

## Parameters

**[**  *attribute* **]**

Keyword.
Must be enclosed in brackets. Use one of the following keywords:

**[CalendarViewFormat]**

Returns
as a numeric value the number of days displayed: 1, 2, 5, 7, and so
on. Applies to calendar views only.

**[ColumnValue]**

Returns
as a text value the value of a column for the current row. Requires
the second parameter.

**[IsCalViewTimeSlotOn]**

Returns
@True if time slots are displayed on the lefthand side, @False otherwise.
Applies to 1-day and 2-day calendar views only.

**[IsViewFiltered]**

Returns
True (1) if the @SetViewInfo command has been used to limit which
documents are displayed in the view, False (0) otherwise. This is
useful in hide formulas for view actions.

*column*

Number.
Required for [ColumnValue]; otherwise does not apply. The column number
starting with 0 for the first column and counting hidden columns.

## Return value

*value*

The value of the attribute
as described previously.

## Usage

This
function is not available in selection or column formulas and will
return null if used there.

Look
at the view design or design synopsis to determine column numbers.
You cannot count columns by looking at a view in the NotesÂ® client or a browser.

* If you look at the view in DominoÂ® Designer,
  the first column is 0, the second column is 1, and so on.
* If you look at a view synopsis (Database - Design Synopsis), subtract
  1 from the column number listed there.

## Examples

1. This hide-column formula hides the "End date" column in a calendar
   view if time slots are enabled or the format is for 30 days.

   ```
   @GetViewInfo([IsCalViewTimeSlotOn]) |
   @GetViewInfo([CalendarViewFormat]) = 30
   ```
2. This hide-action formula hides an action if column 4 (a hidden
   column) has the programmatically assigned value "Task."

   ```
   @GetViewInfo([ColumnValue]; 4) = "Task"
   ```

   Note: The fifth column is the hidden column if the column numbers
   start with one. However, the parameter is specified to start with
   zero.
3. This default field value on a form displays the value of the third
   column in the view used with that form.

   ```
   @GetViewInfo([ColumnValue];2)
   ```
4. This hide-action formula displays an action only when the view
   is being filtered.

   ```
   !@GetViewInfo([IsViewFiltered])
   ```

---

## @HardDeleteDocument

# @HardDeleteDocument (Formula Language)

In an agent that runs a formula, @HardDeleteDocument permanently
removes the document currently being processed from the database if
the database has soft deletions enabled. If the database does not
have soft deletions enabled, @HardDeleteDocument performs the same
action as @DeleteDocument.

Note: This function is new with Release 5.0.1.

## Syntax

**@HardDeleteDocument**

## Usage

This
function is intended only for use in agents that run formulas; it
has no effect when run elsewhere.

To mark a document for deletion
from an icon, view, or form action, use @Command[EditClear].

To
soft delete a document, use @DeleteDocument.

To create an agent
that deletes documents from a database without using a formula, use
the Simple action "Delete from Database."

You cannot use this
@function in Web applications.

---

## @HashPassword

# @HashPassword (Formula Language)

Encodes a string.

Note: This @function is new with Release 6.

## Syntax

**@HashPassword(**
*string*
**)**

## Parameters

*string*

Text. The string that you want to encode.

## Return value

*encodedString*

Text. The passed-in string, double digest encoded for maximum security.

## Usage

Some person records contain a $SecurePassword hidden field, which is double digest encoded in the @HashPassword format. If this field is not present in the record, the digest is encoded in the @Password format. @HashPassword creates a more secure password than the @Password function does.

---

## @Hour

# @Hour (Formula Language)

Returns the number of the hour in the specified time-date.

## Syntax

**@Hour(** *timeDateValue* **)**

## Parameters

*timeDateValue*

Time-date
or time-date list. The value with the hour that you want to extract.

## Return value

*hour*

Number
or number list. A number representing the hour contained in *timeDateValue.* Hours
are represented as 0 through 23 for 12 AM through 11 PM. Returns -1
if the time-date provided contains only a date and not a time value.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 9.

   ```
   @Hour([9:30])
   ```
2. This example returns 9 and 10 in a list.

   ```
   @Hour([9:30] : [10:30])
   ```
3. This example returns 8 if the time in the Date field is 8:56:34
   AM.

   ```
   @Hour(Date)
   ```
4. This example returns 20 if a Date field is made up of the date
   and time: 7/30/90 8:56:34 PM.

   ```
   @Hour(Date)
   ```
5. This example returns 3 if the current document was created on
   2/15/92 at 3:00:12 A.M.

   ```
   @Hour(@Created)
   ```

---

## @If

# @If (Formula Language)

Evaluates a condition; if the condition is True, Notes/Domino
performs the action appearing immediately after that condition, and
stops. If the condition is False, Notes/Domino skips to the next condition
and tests it, and so on. If none of the conditions is True, Notes/Domino
performs the else\_action.

## Syntax

**@If(**  *condition1*  **;**  *action1*  **;**  *condition2*  **;**  *action2*  **;**  *...*  **;**  *condition99*  **;**  *action99*  **;**  *else\_action*  **)**

## Parameters

*condition*

Expression
that returns a Boolean. If this expression returns True, *action* is
performed. If it's False, Notes/Domino skips to the next *condition*,
if there is one. Otherwise, Notes/Domino performs *else\_action.*

*action*

An
action to be performed or a value to be returned if the governing
condition returns True.

*else\_action*

An action
to be performed or a value to be returned if none of the conditions
returns True.

## Usage

In
its simplest form, the If statement looks like this: **@If(**  *condition*  **;**  *action*  **;**  *else\_action*  **)**.

You
can list up to 99 conditions and corresponding actions, followed by
just one action to be performed when all the conditions are False.
As soon as a condition evaluates to True, Notes/Domino performs the
associated action and ignores the remainder of the @If statement.

Notes/Domino
accepts the form **@If(**  *condition*  **)**, with only
one condition and no action, but does not perform any action based
on the condition.

If you compare a field to a value (for example,
Year > 1995) and the field is unavailable, the comparison is False.
However, you should check for fields that may not be present with
@IsUnavailable.

## Examples

1. This formula tests the single value in the CostOfGoods field.
   If the value is greater than or equal to 12.45, the condition is True,
   and the string "Over Budget" is returned. If the value is less than
   12.45, the condition is False and the string "Bill of Materials OK"
   is returned.

   ```
   @If(CostOfGoods>=12.45;"Over Budget";"Bill of Materials OK")
   ```
2. In this example, if CostOfGoods is less than 12.45, the null string
   is returned.

   ```
   @If(CostOfGoods>=12.45;"Over Budget";"")
   ```
3. In this example, @If looks at the value in the CostOfGoods field;
   if the value is greater than 12.45, then the string "Over Budget"
   is returned; if not, NotesÂ® skips
   to the next condition. The second condition also evaluates the CostOfGoods
   field and if the value is less than 12.45, then the condition is True
   and NotesÂ® returns the string
   "Bill of Materials OK." If the value is neither greater than nor less
   than 12.45, NotesÂ® moves on
   to the "else" action specified, and the string "Estimate Right on
   Target" is returned.

   ```
   @If(CostOfGoods>12.45;"Over Budget";CostOfGoods<12.45;
   "Bill of Materials OK";"Estimate Right on Target")
   ```
4. NotesÂ® first checks that
   the document has never been saved; if the condition is True, the value
   in the field NewNoteTitle is returned. If the first condition is False, NotesÂ® then checks whether the
   view is the Author View; if this is True, the value in the field ByAuthorTitle
   is returned. If both conditions are False, the value in the field
   StandardTitle is returned.

   ```
   @If(@IsNewDoc; NewNoteTitle; @ViewTitle = 
   "Author View"; ByAuthorTitle; StandardTitle)
   ```
5. This code, when used as the Input Validation for the phoneNumber
   field prohibits a form from being saved until the user enters a value
   in the phoneNumber field. This formula demonstrates how to test more
   than one statement, since a phone number is only required if the contactMe
   field is set to Yes, indicating that the user wants to be contacted.

   ```
   @If((contactMe="Yes") & (@ThisValue = "");@Failure("You must enter a value in " + @ThisName);@Success)
   ```

   Using
   @ThisValue and @ThisName instead of hard-coding in field names enables
   you to copy and paste this code into all the other fields you want
   to require input for, the firstName and lastName fields, for example.

---

## @IfError

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @Implode

# @Implode (Formula Language)

Concatenates all members of a text list and returns a text
string.

## Syntax

**@Implode(**  *textlistValue*  **)** or **@Implode(**  *textlistValue*  **;**  *separator*  **)**

## Parameters

*textlistValue*

Text
or text list. List containing the items you want to concatenate into
a single string. If you send a single piece of text instead of a list,
@Implode returns the text unaltered.

*separator*

Text.
Used to separate the values in the concatenated string. If you don't
specify a *separator,* a space is used.

## Return value

*implodedString*

Text. String containing
each member of *textListValue,* separated by *separator.*

## Examples

1. This example returns Minneapolis Detroit Chicago if the contents
   of the City field are "Minneapolis":"Detroit":"Chicago."

   ```
   @Implode(City)
   ```
2. This example returns Minneapolis,Detroit,Chicago if the contents
   of the City field are "Minneapolis":"Detroit":"Chicago."

   ```
   @Implode(City;",")
   ```
3. This example returns European Capitals/Berlin : European Capitals/Lisbon
   : European Capitals/Madrid if the contents of the Categories field
   are European Capitals, and the content of the Cities field is a list
   consisting of Berlin, Lisbon, and Madrid.

   ```
   @Implode(Categories + "/" + City ; " : ")
   ```

---

## @InheritedDocumentUniqueID

# @InheritedDocumentUniqueID (Formula Language)

The unique ID of the current document's inheritance parent.
See [@DocumentUniqueID](H_DOCUMENTUNIQUEID.html "The universal ID, which uniquely identifies a document across all replicas of a database. In text format, the universal ID is a 32-character combination of hexadecimal digits (0-9, A-F).") for
a description of unique IDs.

## Syntax

**@InheritedDocumentUniqueID**

## Usage

This
function works in a document being created with a form with field
values inherited from the selected document. This function only works
in the NotesÂ® client.

In
documents that do not inherit, @InheritedDocumentUniqueID returns
the same value as @DocumentUniqueID.

## Examples

1. In a response document, this field formula creates a doclink to
   the selected main topic document. The response document must be created
   with a form that inherits values from the selected main topic document.

   ```
   @InheritedDocumentUniqueID
   ```

   The
   next time you access this response document, the field still contains
   a doclink to the parent document.
2. This example, when used as the default value for a Computed When
   Composed field on a response form, displays the contents of the "userName"
   field from the parent document. The form must have the "Formulas inherit
   values from selected document" property selected on the Default tab
   of the Form Properties box.

   ```
   @GetDocField(@InheritedDocumentUniqueID;"userName")
   ```
3. This example, when added to a hotspot button in a response form,
   displays the contents of the "userName" field in the parent document
   in a message box. This button only works if the response document
   has already been saved. On the Paragraph Hide When tab of the Button
   Properties box, select the "Hide paragraph if formula is true" property.
   Add @IsNewDoc as the formula.

   ```
   id := @Text($REF);
   @Prompt([OK];"Parent field value";@GetDocField(id;"userName"))
   ```

---

## @Integer

# @Integer (Formula Language)

Truncates the values in a number or number list at the
whole number, leaving off any decimals. The values in the resulting
list are separated using the multi-value separator that is selected
for display in the field containing the formula.

## Syntax

**@Integer(**  *numberValue*  **)**

## Parameters

*numberValue*

Number
or number list. The value(s) you want to truncate.

## Return value

*truncatedValue*

Number or number list.
The truncated value(s).

## Usage

When
using this function with a number list, the list concatenation operator
takes precedence over any other operators. Negative numbers must be
enclosed in parentheses.

## Examples

1. This example returns 123;789.

   ```
   @Integer(123.001 : 789.999)
   ```
2. This example returns 127580;5;7341 if the numbers in the Sales,
   CommissionRate, and Commission fields are 127580.35, 5.75, and 7341.62015,
   respectively.

   ```
   @Integer(Sales:CommissionRate:Commission)
   ```
3. This example returns 3.

   ```
   @Integer(3.12)
   ```
4. This example returns 6.

   ```
   @Integer(6.735)
   ```

---

## @IsAgentEnabled

# @IsAgentEnabled (Formula Language)

Indicates whether or not a scheduled agent is enabled.

## Syntax

**@IsAgentEnabled(**  *agent* **)**

## Parameters

*agent*

Text.
The name of the agent. Not case-sensitive.

## Return value

*flag*

Number

* 1 (True) indicates that the agent is enabled
* 0 (False) indicates that the agent is disabled, or that an agent
  by that name does not exist

## Usage

A
database must be open. If a database is not open, returns 0.

@IsAgentEnabled
returns 1 for macros created in NotesÂ® Release
3, and for any agents that are not scheduled.

@IsAgentEnabled
does not work in column or selection formulas and is not intended
for use in window title or form formulas.

You cannot use this
function in Web applications.

## Examples

This example returns 1 if the UnderCover
agent is enabled; otherwise, it returns 0.

```
@IsAgentEnabled( "UnderCover" )
```

---

## @IsAppInstalled

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @IsAvailable

# @IsAvailable (Formula Language)

Checks a document for the existence of a field.

## Syntax

**@IsAvailable(**  *fieldName*  **)**

## Parameters

*fieldName*

Field.
The name of a field. Do not treat the name as text. Enter the exact
name of the field and do not enclose it in quotes.

## Return value

*flag*

Boolean

* 1 (True) indicates that the field is contained in the document
* 0 (False) indicates that the field is not contained in the document

## Usage

Use
@IsAvailable to provide a default value for documents created with
forms that do not include a field name.

This function can be
used with select and column formulas using Summary fields only. Non-Summary
fields are not available.

For information on creating a field
in an existing document if it does not exist, see the [FIELD](H_FIELD_KEYWORD.html "A reserved word that is necessary when you are assigning values to fields that are stored in a document (as opposed to temporary fields). You can use FIELD to change the contents of an existing field or to create new fields.") keyword.

## Examples

1. This formula returns the value of the Dept field if it exists
   in the document, otherwise it returns Consultant.

   ```
   @If(@IsAvailable(Dept);Dept;"Consultant")
   ```
2. This formula returns the value of the field named Topic if it
   exists in the document, otherwise it returns the value contained in
   the field named Subject.

   ```
   @If(@IsAvailable(Topic);Topic;Subject)
   ```
3. This formula, when added to a hotspot button, checks for the existence
   of the Priority field in a form, then sets its value if found or creates
   a new Priority field and sets its value, if not found.

   ```
   @If(@IsAvailable(Priority);@SetField("Priority";"High");FIELD Priority := "High")
   ```

   Note: If you create the field using this formula, it is not
   visible on the form, but you can get its value using the @GetField
   function. Be sure you use the correct spelling and capitalization
   when checking for the field in the document.

---

## @IsCategory

# @IsCategory (Formula Language)

In a column formula, returns a specified string if any
item in the row of a view is defined as a category.

## Syntax

**@IsCategory
@IsCategory(**  *trueString*  **) @IsCategory(**  *trueString* **;**  *falseString*  **)**

## Parameters

*trueString*

Text.
A string to return if an item in the view row is a category.

*falseString*

Text.
A string to return if no item in the row is a category.

## Return value

*specifiedString*

Text.

No
parameters:

* \* (asterisk) indicates that the entry is a category
* If the entry is a document, returns nothing

Single *trueString* parameter:

* Returns *trueString* instead of \*

Both *trueString* and *falseString* parameters

* Return *trueString* instead of \*
* Return *falseString*  instead of nothing

## Usage

Use
@IsCategory only in column formulas.

This function only looks
at the columns following it, so be sure to place it preceding the
categorized column to which you are referring.

This function
does not work in Web applications running version 4.5.

## Examples

1. This example returns \* if the row is a category, or nothing if
   the row is not a category.

   ```
   @IsCategory
   ```
2. This example returns C if the row is a category, or nothing if
   the row is not a category.

   ```
   @IsCategory("C")
   ```
3. This example returns Y if the row is a category, or N if the row
   is not a category.

   ```
   @IsCategory("Y";"N")
   ```

---

## @IsDB2

# @IsDB2 (Formula Language)

Given a server and filename or server and replica ID, indicates
if the specified database is backed by DB2Â® or
not.

Note: This @function is new with Release 7.

## Syntax

**@IsDB2**(*server
: file*)

**@IsDB2**(*server ; replicaID*)

## Parameters

*server*

Text.
The name of the server. Use an empty string ("") to indicate the local
computer.

*file*

Text. The path and file name
of the database. Specify the database path and file name using the
appropriate format for the operating system.

*replicaID*

Text.
The replica ID of the database.

## Return value

*flag*

Boolean

* 1 (True) indicates that the specified database is backed by DB2Â®
* 0 (False) indicates that the specified database is not backed
  by DB2Â®

This function will return an error via @Error if:

* The server cannot be reached
* The database specified in file or replicaID cannot be found

## Examples

1. This formula returns 0 (False), since the local names.nsf database
   is not in DB2Â®:

   ```
   @IsDB2("":"names.nsf")
   ```
2. These formulas both return DB2Â® information
   about the current database:

   ```
   @IsDB2(@DbName)
   @IsDB2("":"")
   ```
3. This formula returns 1 if FRITES.NSF in the MAIL directory on
   the server Belgium is DB2Â® backed.
   Otherwise it returns 0.

   ```
   @IsDB2( "Belgium" : "mail\\frites.nsf" )
   ```
4. This formula returns DB2Â® information
   about a database using its replica ID instead of its file name:

   ```
   @IsDB2("Cheshire";"852556DO:00576146")
   ```
5. This example of a column formula first uses @IsDB2 to find out
   if the local database referenced in the dbname field of the document
   is a DB2Â® database, so that a
   more meaningful error message may be displayed from @DB2Schema:

   ```
   result1 := @IsDB2("":dbname);
   result2 := @DB2Schema("":dbname)
   @If(@IsError(result1);"Unable to find database or lost server connection";
   result1;@If(@IsError(result2);
   "Unable to find database or lost server connection";result2);
   "Not a DB2 database");
   ```

## Usage

This
function works in all contexts where @function use is supported, including
view selection formulas, column formulas, and from the Web.

If
the database has been replicated to a local replica, and an empty
string is specified for the server parameter, @IsDB2 will produce
an error on the replica. For this reason, it is extremely important
to use @Error processing with @IsDB2 when using relative paths.

---

## @IsDocBeingEdited

# @IsDocBeingEdited (Formula Language)

Checks the current status of the document and returns 1
(True) if the document is being edited; otherwise returns 0 (False).

## Syntax

**@IsDocBeingEdited**

## Return value

*flag*

Boolean

* 1 (True) indicates that the document is being edited
* 0 (False) indicates that the document is not being edited

## Usage

This
function does not work in column, selection, agent, form, or view
action formulas. It's intended for use in button, hide-when, field,
and form action formulas.

## Examples

This code, when added to an action
button, checks whether the current document is in edit mode. If it's
not, it changes the document's mode to edit in order to execute the
@DocLock function, which requires that the current document be in
edit mode. It then locks the current document.

```
@If(@IsDocBeingEdited; @True;@Command( [EditDocument] ; 1 ));
@DocLock([Lock])
```

---

## @IsDocBeingLoaded

# @IsDocBeingLoaded (Formula Language)

Checks the current status of the document and returns 1
(True) if the document is being loaded into memory for display; otherwise
returns 0 (False).

## Syntax

**@IsDocBeingLoaded**

## Return value

*flag*

Boolean

* 1 (True) indicates that the document is actually being loaded
  into memory
* 0 (False) indicates that the document is not being loaded into
  memory

## Usage

Use
function in field and form formulas. It does not work in toolbar
button, selection, column, agent, section editor, hotspot, form action,
or view action formulas.

## Examples

1. This example returns 1 when the document is being loaded into
   memory.

   ```
   @IsDocBeingLoaded
   ```
2. This example returns 0 when the document is saved.

   ```
   @IsDocBeingLoaded
   ```
3. This example, when used in a computed field named "Editors," displays
   the contents of $UpdatedBy when the document is being loaded. When
   the user recalculates the field (by pressing F9), the field displays
   the user's name as the current editor, followed by previous editors'
   names. When the document is saved, the value of "Editors" remains
   unchanged.

   ```
   @If(@IsDocBeingLoaded;$UpdatedBy;
   @IsDocBeingRecalculated;("Current Editor - " + @UserName):$UpdatedBy;Editors)
   ```

---

## @IsDocBeingRecalculated

# @IsDocBeingRecalculated (Formula Language)

Checks the current status of the document and returns 1
(True) if the document is being recalculated; otherwise, returns 0
(False).

## Syntax

**@IsDocBeingRecalculated**

## Return value

*flag*

Boolean

* Returns 1 (True) only when the fields on the document are actually
  being recalculated
* Returns 0 (False) when the fields on the document are not currently
  being recalculated

## Usage

Use
@IsDocBeingRecalculated in field formulas. It has limited usefulness
in toolbar button, hotspot, and form action formulas. This function
does not work in column, selection, agent, window title, form, or
view action formulas.

If you are using this function to increment
a counter, the count increases by one every time the user recalculates
the fields on a form.

## Examples

1. This example returns 1 while the document is being calculated
   or recalculated.

   ```
   @IsDocBeingRecalculated
   ```
2. This example returns 0 before and after the document is calculated
   or recalculated.

   ```
   @IsDocBeingRecalculated
   ```
3. This example can be used in a time-date field to display different
   dates under different circumstances. The formula causes the current
   time-date to be displayed if the document is recalculated during the
   editing process; otherwise, it displays the original creation date
   of the document.

   ```
   @If(@IsDocBeingRecalculated;@Now;@Created)
   ```

---

## @IsDocBeingSaved

# @IsDocBeingSaved (Formula Language)

Checks the current status of the document and returns 1
(True) if the document is being saved; otherwise, returns 0 (False).

## Syntax

**@IsDocBeingSaved**

## Return value

*flag*

Boolean

* Returns 1 (True) only when the fields on the document are actually
  being saved
* Returns 0 (False) when the fields on the document are not currently
  being saved

## Usage

Use
@IsDocBeingSaved in field formulas. It has limited usefulness in toolbar
button, hotspot, and form action formulas. This function does not
work in column, selection, agent, window title, form, or view action
formulas.

If you are using this function to increment a counter,
the count increases by one every time the user saves the form.

## Examples

1. This example returns 1 while the document is being saved.

   ```
   @IsDocBeingSaved
   ```
2. This example returns 0 before or after the document is saved.

   ```
   @IsDocBeingSaved
   ```
3. This formula sets the field named Readers, which is a Reader Names
   field, to Admins when the document is saved. Otherwise, it sets the
   Readers field to the value already in the field. This type of formula
   is useful for changing the Read Access of a document after it has
   been composed and saved.

   ```
   @If(@IsDocBeingSaved;"Admins";Readers)
   ```

---

## @IsDocTruncated

# @IsDocTruncated (Formula Language)

Indicates whether the current document has been truncated.

## Syntax

**@IsDocTruncated**

## Return value

*flag*

Boolean

* Returns 1 (True) if the document is missing some data
* Returns 0 (False) if the entire document is present

## Usage

You
typically use @IsDocTruncated in a column formula to display the truncated
document indicator. You can also use @IsDocTruncated in a variety
of other formulas, including toolbar buttons, hide-when formulas,
section editors, window title formulas, field formulas, form formulas,
column formulas, selection formulas, and agents.

Documents
may be truncated during database replication. Depending upon the type
of truncation, a document can be missing an attached file, an OLE
object, large rich text fields, or non-summary items.

If the
document is truncated, you can obtain the entire document by choosing
Action - Retrieve Entire Document, either in the background or during
the next replication of the database. You cannot edit a truncated
document.

## Examples

This code, when added to a column
formula, displays a negative (-) icon if the document was truncated
in replication. The column must be set to Display values as icons
in the Column Properties box.

```
@If(@IsDocTruncated;97;0)
```

---

## @IsError

# @IsError (Formula Language)

Returns 1 (True) if the value is an @ERROR value, returns
0 (False) if not an error.

## Syntax

**@IsError(**  *value*  **)**

## Parameters

*value*

Number.
Can be a literal value or a field name containing data of type Number.

## Return value

*flag*

Boolean

* Returns 1 (True) if the value is an @ERROR value
* Returns 0 (False) if not an error

## Usage

Since
this function intercepts the error message and replaces it with your
own value, if you do have an error, you may have trouble figuring
out what's causing the error. For debugging purposes, you may want
to temporarily remove the error handling so that you can see the error
message text or, display the text as shown in example 5.

## Examples

1. This example returns 1.

   ```
   @IsError(1/0)
   ```
2. This example returns 0.

   ```
   @IsError(1/2)
   ```
3. This formula checks to see if there is an @ERROR in the Price
   field, and returns "There is an error in the price field" if it encounters
   an error; otherwise it returns 0.

   ```
   @If(@IsError(Price); 
      @Failure("There is an error in the price field"); @Success)
   ```
4. This agent tests the return value of an @DbLookup statement for
   an error. If the @DbLookup statement causes an error, the agent returns
   the text "Not available."

   ```
   FIELD Phone := @DbLookup(""; "Snapper" : "names.nsf"; "People";
   @Right(Name; " ") + " , " + @Left(Name; " "); "OfficePhoneNumber");
   @If(@IsError(Phone);"Not available")
   ```
5. This returns the lookup result if there is one, but if the lookup
   fails, it returns the text of the error message without causing an
   error condition. This may be useful in debugging.

   ```
   -tmp := @DbLookup("":"NoCache"; ""; "ById"; ID; 2); 
   @Text(_tmp); _tmp)
   ```

---

## @IsExpandable

# @IsExpandable (Formula Language)

In column formulas, returns a specified string if a row
in a view can be expanded.

## Syntax

**@IsExpandable
@IsExpandable(**  *trueString*  **) @IsExpandable(**  *trueString* **;**  *falseString*  **)**

## Parameters

*trueString*

Text.
A string to return if the view row is expandable.

*falseString*

Text.
A string to return if the view row is not expandable.

## Return value

*specifiedString*

Text

No parameters:

* Returns + (plus) if the entry is expandable
* Returns **-** (minus) if the entry is not expandable

Single *trueString* argument:

* Returns the *trueString* instead of **+** if the entry
  is expandable
* Returns nothing if it is not expandable

Both *trueString* and *falseString*:

* Return *trueString* instead of **+**
* Return *falseString* instead of **-**

## Usage

This
function is obsolete and is only provided for compatibility with existing
applications. Instead, enable the view column property "Show twistie
when row is expandable."

Use @IsExpandable in column value
formulas to indicate whether the current level of documents can be
expanded. This function does not work in any other formula.

In
the single parameter and two parameter forms, you should limit the
string to a single character, especially if the lines already have
a lot of text in them.

## Examples

1. This example returns + if the document or category is expandable,
   or **-** if it is not expandable.

   ```
   @IsExpandable
   ```
2. This example returns & if the document or category is expandable.

   ```
   @IsExpandable("&")
   ```
3. This example returns Y if the document or category is expandable,
   or N if it is not expandable.

   ```
   @IsExpandable("Y";"N")
   ```

---

## @IsMember

# @IsMember (Formula Language)

Indicates if a piece of text (or a text list) is contained
within another text list. The function is case-sensitive.

## Syntax

**@IsMember(**  *textValue*  **;**  *textListValue*  **)
@IsMember(**  *textListValue1*  **;**  *textListValue2* **)**

## Parameters

*textValue*

Text.

*textListValue*

Text
list.

*textListValue1*

Text list.

*textListValue2*

Text
list.

## Return value

*flag*

Boolean

* Returns 1 (True) if the *textValue* is contained in *textListValue*
* Returns 0 (False) if not
* If both parameters are lists, returns 1 if all elements of *textListValue1* are
  contained in *textListValue2*

## Usage

In
processing lists, @IsMember differs from a simple = test. An = returns
True if the pair-wise comparison of two entities has even one member;
that is, it is not empty.

@IsMember returns True only if the first parameter is
an exact match, or a subset of the second parameter which is a *list*.

## Examples

1. This example returns 1.

   ```
   @IsMember("computer";"printer":"computer":"monitor")
   ```
2. This example returns 0.

   ```
   @IsMember("computer":"Notes";"Notes":"printer":"monitor")
   ```
3. This example returns 1 if R&D is in the list in the Department
   field, returns 0 if R&D is not in the list.

   ```
   @IsMember("R&D";Department)
   ```
4. This example returns 1, since Fred is a subset of a list.

   ```
   @IsMember("Fred"; "Barney":"Wilma":"Fred")
   ```

---

## @IsModalHelp

# @IsModalHelp (Formula Language)

Indicates whether the current document is a modal Help
document.

## Syntax

**@IsModalHelp**

## Return value

*flag*

Boolean

* Returns 1 (True) if the document is a modal Help document
* Returns 0 (False) if the document is not a modal Help document

## Usage

A
modal Help document is a document that displays as a dialog box that
you must dismiss before you can access any other currently open windows.
Use @IsModalHelp to determine modality so you can execute a formula
only when the document is (or isn't) a modal Help document.

You
cannot use this function in Web applications.

---

## @IsNewDoc

# @IsNewDoc (Formula Language)

For a document being edited, indicates if the document
has been saved to disk.

## Syntax

**@IsNewDoc**

## Return value

*flag*

Boolean

* Returns 1 (True) if the document being edited has not yet been
  saved to disk
* Returns 0 (False) if the document has been saved

## Usage

This
function evaluates the current state of the document when it is used
in toolbar button, hide-when, section editor, window title, field,
form, and form action formulas.

This functions returns 0 if
the document has not yet been saved, regardless of how the document
was created. It *always* returns a 0, even if the document has
been saved, when used in column, selection, agent, and view action
formulas.

## Examples

1. When used in a window title formula, this formula returns **New
   Document** while the document is composed the first time. When a
   document is opened after it has been saved, this formula returns the
   value of the Subject field.

   ```
   @If(@IsNewDoc;"New Document";Subject)
   ```
2. If a new document is being created, the string **New General
   Information** appears in the window title. When an existing document
   is opened, the string **General Information for**, then the contents
   of the field EmpName, a slash, and then the contents of the field
   EmpNumber appear in the window title.

   ```
   @If(@IsNewDoc; "New General Information"; "General Information for" + EmpName + "/" + EmpNumber)
   ```

---

## @IsNotMember

# @IsNotMember (Formula Language)

Indicates if a text string (or a text list) is not contained
within another text list. The function is case-sensitive.

## Syntax

**@IsNotMember(** *textValue*  **;** *textListValue*  **)** or **@IsNotMember(** *textListValue1*  **;** *textListValue2* **)**

## Parameters

*textValue*

Text.

*textListValue*

Text
list.

*textListValue1*

Text list.

*textListValue2*

Text
list.

## Return value

*flag*

Boolean

* Returns 1 (True) if the *textValue* is not contained in *textListValue*
* Returns 0 (False) if it is contained
* If both parameters are lists, returns 1 if all elements of *textListValue1* are *not* contained
  in *textListValue2*

## Usage

In
processing lists, @IsNotMember differs from a simple != test. != returns
True if the pair-wise comparison of two entities has no entities in
common and False only if the pair-wise comparison of the two entities
finds all pairs to be equal. @IsNotMember does not perform a pair-wise
comparison, but tests each element in *textListValue1*against
all the elements in the *textListValue2* and returns False if
it is equal to one of them.

@IsNotMember
returns True only if no member of the first argument is contained
in the second argument.

## Examples

1. This example returns 0.

   ```
   @IsNotMember("computer";"printer":"computer":"monitor")
   ```
2. This example returns 1 if R&D is not in the list of values
   in the field name Department; returns 0 if R&D is in the list.

   ```
   @IsNotMember("R&D";Department)
   ```
3. This example returns **Marketing** in the Dept field if the
   current user is not contained in the list in the SalesDepartment field;
   otherwise **Sales** is returned in the Dept field.

   ```
   FIELD Dept:=@If(@IsNotMember(@Username;SalesDepartment); "Marketing"; "Sales");
   ```
4. This example returns 1 if both the [WebTeam] and [ManageFiles]
   roles are assigned to the current user; it returns 0 if only one or
   neither of the roles is assigned to the user.

   ```
   @IsNotMember("[WebTeam]":"[ManageFiles]";@UserRoles)
   ```

---

## @IsNull

# @IsNull (Formula Language)

Tests for a null value. Returns true only if a value is
a single text value that is null, otherwise it returns false. This
function also returns false if the value is an error.

Note: This @function is new with Release 6.

## Syntax

**@IsNull(**  *value*  **)**

## Parameters

*value*

Any
data type. Any value.

## Return value

*flag*

Boolean

* Returns 1 (True) if the *value* is a text value that is null
* Returns 0 (False) if the *value* is not a text value, not
  null, or is an error

## Usage

This
function is useful for checking for empty fields before using them
in other functions in which they might generate errors.

## Examples

This function, when used as a field
formula, finds the square root of each element in the text list in
the OriginalList field. @IsNull is first used to test the OriginalList
field to ensure that it contains a value and prevents the formula
from calculating the square roots if it does not. If OriginalList
contains 4: 25, the result is 2; 5. If OriginalList is a null field,
the result is a null field, not an error.

```
@If(@IsNull(OriginalList); @Nothing;
@Transform(OriginalList; "x";
@If(x >= 0; @Sqrt(x); @Nothing)))
```

---

## @IsNumber

# @IsNumber (Formula Language)

Indicates if a given value is a number (or a number list).

## Syntax

**@IsNumber(**  *value*  **)**

## Parameters

*value*

Any
data type. Any value.

## Return value

*flag*

Boolean

* Returns 1 (True) if the *value* is a number or a number list
* Returns 0 (False) if the *value* is not a number or a number
  list

## Usage

This
is a useful function for checking to see that you have assigned field
data types correctly.

The parameter must be a number, not a
non-numeric value (for example, text) that can be converted to a number.

## Examples

1. This example returns 1.

   ```
   @IsNumber(123)
   ```
2. This example returns 0.

   ```
   @IsNumber("123")
   ```
3. This example returns 0.

   ```
   @IsNumber(@Created)
   ```
4. This example returns 1.

   ```
   @IsNumber(-345:2.78:997:.7)
   ```
5. This example returns 1 if the field named CostCenters contains
   a list of number values; returns 0 if the list contains at least one
   text string.

   ```
   @IsNumber(CostCenters)
   ```

---

## @IsResponseDoc

# @IsResponseDoc (Formula Language)

Indicates whether a document is a response to another document.

**Syntax**

**@IsResponseDoc**

## Return value

*flag*

Boolean

* Returns 1 (True) if the document is a response document
* Returns 0 (False) if the document is not a response document
* Returns 0 for new documents, since @IsResponseDoc doesn't recognize
  a document type until after the document is saved

## Usage

A
response document is one that was composed with a form which has a
type of either Response or Response to Response. The designer uses
the Form InfoBox to specify the type.

## Examples

This example returns Response if the
document is a response; Topic if the document is not a response.

```
@If(@IsResponseDoc;"Response";"Topic")
```

---

## @IsText

# @IsText (Formula Language)

Indicates whether a value is text (or a text list).

## Syntax

**@IsText(**  *value*  **)**

## Parameters

*value*

Any
data type. Any value.

## Return value

*flag*

Boolean

* Returns 1 (True) if the *value* is text or a text list
* Returns 0 (False) if the *value* is not text or a text list

## Examples

1. This example returns 1.

   ```
   @IsText("Blanchard & Daughters")
   ```
2. This example returns 1 if the field named BranchOffices contains
   the text string list "New Orleans":"Houston":"Dallas":"Mobile."

   ```
   @IsText(BranchOffices)
   ```

---

## @IsTime

# @IsTime (Formula Language)

Indicates whether a value is a time-date (or a time-date
list).

## Syntax

**@IsTime(**  *value*  **)**

## Parameters

*value*

Any
data type. Any value.

## Return value

*flag*

Boolean

* Returns 1 (True) if the *value* is a time-date or a time-date
  list
* Returns 0 (False) if the *value* is not a time-date or a
  time-date list

## Examples

1. This example returns 1 if the DueDate field contains a time-date
   value.

   ```
   @IsTime(DueDate)
   ```
2. This example returns 0.

   ```
   @IsTime(123)
   ```

---

## @IsUnavailable

# @IsUnavailable (Formula Language)

Indicates whether a field name exists in a document.

## Syntax

**@IsUnavailable(**  *fieldname*  **)**

## Parameters

*fieldname*

The
name of a field. Do not enclose the name in quotes.

## Return value

*flag*

Boolean

* Returns 1 (True) if the field name is not contained in the document
* Returns 0 (False) if the field name is contained in the document

## Usage

Use
@IsUnavailable to provide default values for fields in documents created
with forms that do not include a particular field name.

CAUTION: Do not confuse @IsUnavailable with @Unavailable. @Unavailable
deletes fields and can cause serious damage if used unintentionally
in place of @IsUnavailable.

## Examples

This example returns Consultant if
the field Dept does not exist; if Dept does exist, the value contained
in Dept is returned.

```
@If(@IsUnavailable(Dept);"Consultant";Dept)
```

---

## @IsValid

# @IsValid (Formula Language)

Executes all validation formulas within the current form.

## Syntax

**@IsValid**

## Return value

*flag*

Boolean

* Returns 1 (True) if all validation formulas resolve to True
* Returns 0 (False) if all validation formulas do not resolve to
  True

## Usage

Use
@IsValid to initiate execution of all of a form's validation formulas,
as if the document were being saved.

If validation formulas
are added to a form after some documents have already been saved,
you can use @IsValid in a macro to determine which of those documents
need corrections.

## Examples

You edit a form after it's been in
use for a while, and insert validation formulas into several fields.
Now you want to test existing documents to be sure they meet the field
validation requirements. You can create an additional field on the
form and use this formula to indicate whether the document needs corrections:

```
@If(@IsValid;"Ok";"Needs corrections")
```

---

## @IsVirtualizedDirectory

# @IsVirtualizedDirectory (Formula Language)

Indicates whether virtualized directories are enabled for
the current server.

Note: This @function is new with Release 6.

## Syntax

**@IsVirtualizedDirectory**

## Return value

*flag*

Boolean

* Returns 1 (True) if virtualized directories are enabled
* Returns 0 (False) if virtualized directories are not enabled

## Examples

This computed field displays the name
of the current user if virtualized directories are enabled and a message
otherwise.

```
@if(@IsVirtualizedDirectory; @UserName;
@Return("Virtualized directories not enabled"))
```

---

## @Keywords

# @Keywords (Formula Language)

Given two text lists, returns only those items from the
second list that are found in the first list.

## Syntax

**@Keywords(**  *textList1*  **;**  *textList2*  **)** or **@Keywords(**  *textList1*  **;**  *textList2* **;**  *separator*  **)**

## Parameters

*textList1*

Text
list. A list of items.

*textList2*

Text list.
A list of items that you want to compare to *textList1*.

*separator*

Text.
One or more characters to be used as delimiters between words. @Keywords
considers each character (not the combination of multiple characters)
to be a delimiter. For example, defining separator as ". ," (period,
space, comma) tells the function to separate the text at each period,
space, and comma into separate words.

When you do not specify
a separator, the following word delimiters are used by default:

?.
,!;:[](){}"<> (question mark, period, space, comma, exclamation
point, semicolon, colon, (brackets, parentheses, braces, quotation
mark, and angle brackets)

A null separator, represented by
an empty string (""), tells the function to use no delimiters.

## Return value

*resultTextList*

Text list. When a
separator is in effect, either by default or specification, @Keywords
parses textList1 into words delimited by the separator and returns
any word that exactly matches a keyword in textList2. When no separator
is in effect (when you specify a null separator), @Keywords returns
any sequence of characters in textList1 that matches a keyword specified
in textList2.

Note: With Release 6, the order of the
words returned in *resultTextList* match the order of *textList1*.
Prior to Release 6, the order of the words returned in *resultTextList* matched
the order of *textList2*. To retain the pre-Release 6 ordering
of the *resutlTextList*, prepend the formula with another @Keywords
function as follows:

```
@Keywords(textList2;@Keywords(textList1;textList2))
```

## Usage

When
a keyword that you specify in textList2 is the very first word in
the string you are searching AND you specify separators, @Keywords
returns null. To prevent this behavior, prepend textList1 with one
of the separators. For example, if you want to find the keyword, Sally,
in a text list that contains employee names and positions, use the
following formula:

```
@Keywords(" " + " ,Mary Halen, Director of Sales":" ,Sally Hall, VP of Marketing": " ,Joe Halzy, Order entry"; "Sally"; " ,")
```

This
formula returns Sally. Note that one of the formula's separators,
the space(" "), is prepended to textList1. This behavior does not
occur if you accept the default separators or specify a null separator.

If
one of the strings in textList2 contains any of the default delimiters,
@Keywords will not return it. To search for Harvard University, for
example, add a null separator to the formula. This tells @Keywords
to search for any sequence of characters. If you do not specify a
separator, you allow the default delimiters to act. @Keywords does
not return Harvard University because when it parses textList1, it
breaks the phrase into two separate words, Harvard and University,
where it finds the space, which is a default delimiter.

When
using the quotation mark separator ("), precede it with a backslash
(\) to indicate that the quotation mark is a text constant.

This
function is case-sensitive; you must standardize the case of textList1
and textList2 if you want case to be ignored (use @LowerCase, @ProperCase
or @UpperCase).

## Examples

1. This formula returns **Harvard;Yale**.

   ```
   @Keywords(@ProperCase("EPA Head speaks at Harvard and yale":"The UCLA Chancellor Retires":"Ohio State wins big game":"Reed and University of Oregon share research facilities");"Harvard":"Brown":"Stanford":"Yale":"Vassar":"UCLA")
   ```
2. This formula returns **""**, a null string.

   ```
   @Keywords("EPA Head speaks at Harvard,Yale":"UCLA Chancellor Retires":"Ohio State wins big game":"Reed and University of Oregon share research facilities";"harvard":"brown":"stanford":"vassar":"ucla")
   ```
3. This formula returns **Harvard;Yale**. It searches textList1
   for the textList2 keywords that follow either a comma or a space.

   ```
   @Keywords("EPA Head speaks at Harvard, Yale hosts her next month":"UCLA Chancellor Retires":"Ohio State wins big game":"Reed and University of Oregon share research facilities";"Harvard":"Brown":"Stanford":"Yale":"UCLA";", ")
   ```
4. This formula returns **Harvard;Yale University;UCLA**.

   ```
   @Keywords("EPA Head speaks at Harvard, Yale University hosts her next month":"UCLA Chancellor Retires":"Ohio State wins big game":"Reed and University of Oregon share research facilities";"Harvard":"Brown":"Stanford":"Yale University":"UCLA"; "")
   ```
5. This formula returns **Mary Jones.** when used in the "Result"
   field on a form that also contains the "Applicants" field, which has
   a default value of: **",Mary Jones.":",John Chen.":",Miguel Sanchez."**.

   ```
   @Keywords(Applicants;"Mary Jones.";",")
   ```
6. This formula returns **Mary Jones.** when used in the "Result"
   field on a form that also contains the "Applicants" field, which has
   a default value of: **",Mary Jones., who works downtown, is being
   interviewed on Friday.":",John Chen.":",Miguel Sanchez."**.

   ```
   @Keywords("," + Applicants;"Mary Jones.";",")
   ```
7. This formula returns **book**.

   ```
   @Keywords("<booklist> XML tag that represents a list of our books.":"<book> XML tag that represents a book.":"<sale> XML tag that represents the sale price of a book.";"book";"<>")
   ```
8. If list1 contains "guava":"eggplant":"date":"cherry":"banana":"apple"
   and list2 contains "apple":"banana":"date," this formula, when triggered
   from a Release 6 client, returns **date, banana, apple**. When
   triggered from a pre-Release 6 client, it returns **apple, banana,
   date**.

   ```
   @Keywords(list1;list2)
   ```
9. If list1 and list2 contain the same text lists as in the previous
   example, this formula returns **apple, banana, date** from all
   versions of NotesÂ®.

   ```
   @Keywords(list2;@Keywords(list1;list2))
   ```

---

## @LanguagePreference

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @LaunchApp

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @Left

# @Left (Formula Language)

Searches a string from left to right and returns the leftmost
characters of the string.

## Syntax

**@Left(**  *stringToSearch*  **;**  *numberOfChars* **)
@Left(**  *stringToSearch* **;**  *subString* **)**

## Parameters

*stringToSearch*

Text
or text list. The string where you want to find the leftmost characters.

*numberOfChars*

Number.
The number of characters to return. If the number is 2, the first
two characters of the string are returned; if the number is 5, the
first five characters are returned, and so on. If the number is negative,
the entire string is returned.

*subString*

Text.
A substring of *stringToSearch.* @Left returns the characters
to the left of *subString.* It finds *subString* by searching *stringToSearch* from
left to right.

## Return value

*resultString*

Text
or text list. The leftmost characters in *stringToSearch*. The
number of characters returned is determined by either *numberOfChars* or *subString*.
@Left returns "" if *subString* is not found in *stringToSearch*.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Examples

1. This example returns Len.

   ```
   @Left("Lennard Wallace";3)
   ```
2. This example returns Lennard Wal if the string in the Contact
   field is Lennard Wallace.

   ```
   @Left(Contact;"la")
   ```
3. This example returns Tim if the string in the Author field is
   Timothy Altman.

   ```
   @Left(Author;3)
   ```
4. This example returns Timothy if the string in the Author field
   is Timothy Altman.

   ```
   @Left(Author;" ")
   ```
5. This example returns L and W in a list.

   ```
   @Left("Lennard" : "Wallace"; 1)
   ```

---

## @LeftBack

# @LeftBack (Formula Language)

Searches a string from right to left and returns a substring.

## Syntax

**@LeftBack(**  *stringToSearch* **;**  *numToSkip*  **)** or **@LeftBack(**  *stringToSearch*  **;**  *startString*  **)**

## Parameters

*stringToSearch*

Text
or text list. The string where you want to find the leftmost characters.

*numToSkip*

Number.
Counting from right to left, the number of characters to skip. All
the characters to the left of that number of characters are returned.
If the number is negative, the entire string is returned.

*startString*

Text.
A substring of *stringToSearch*. All the characters to the left
of *startString* are returned.

## Return value

*resultString*

Text or text list. The
leftmost characters in *stringToSearch*. The number of characters
returned is determined by either *numToSkip* or *startString*.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Examples

1. This example returns Lennard Wall.

   ```
   @LeftBack("Lennard Wallace";3)
   ```
2. This example returns Lennard.

   ```
   @LeftBack("Lennard Wallace"; " ")
   ```
3. This example returns Timothy Alt if the string in the Author field
   is Timothy Altman.

   ```
   @LeftBack(Author;3)
   ```
4. This example returns Lenn and Wall in a list.

   ```
   @LeftBack("Lennard" : "Wallace"; 3)
   ```

---

## @Length

# @Length (Formula Language)

Returns the number of characters in a text string.

## Syntax

**@Length(** *string* **)** or

**@Length(** *stringlist* **)**

## Parameters

*string*

Text.
A single string with the length you want to find.

*stringList*

Text
list. A list of strings.

## Return value

*length*

* If the parameter is a text string, @Length returns the number
  of characters in the specified string, including spaces and punctuation.
* If the argument is a text list, @Length searches the list of strings
  and returns the number of characters in each string as a number list.

## Examples

1. This example returns 45.

   ```
   @Length("The boy crossed the wide, but gentle, stream.")
   ```
2. This example returns the number list 0:5:3, which displays as
   0;5;3 if the multi-value separator for the field is a semicolon.

   ```
   @Length("": "abcde": "xyz" )
   ```
3. This example returns the number list 16:10:22 if the contents
   of the fields From, Topic, and Date are "Stephen Brewster", "News
   Flash", and @Now (where the current date is 04/01/2001 16:45:10 PM),
   respectively. The number list displays as 16,10,22 if the multi-value
   separator for the field is a comma.

   ```
   @Length(From: Topic: @Text(Date))
   ```

---

## @Like

# @Like (Formula Language)

Matches
a string with a pattern. It is case-sensitive and supports the NotesSQL
ODBC driver.

## Syntax

**@Like(**  *string*  **;**  *pattern*  **)
@Like(**  *string*  **;**  *pattern*  **;**  *escape*  **)**

## Parameters

*string*

Text
or text list. The value to be tested to see if it matches *pattern.*

*pattern*

Text
or text list. The sequence of characters to search for within *string.*
May also contain any of the wildcard characters listed as follows.

*escape*

Text.
Optional. A character to use before a wildcard character to indicate
that it should be treated literally.

Wildcard characters and
symbols are:

| C | Where C is any character. Matches any single, non-special character C. |
| --- | --- |
| *\_\_* | (Underscore). Matches any single character. |
| % | Matches any sequence of zero or more characters. |

## Return value

*flag*

Number

* Returns 1 (True) if the *pattern* matches the *string*
* Returns 0 (False) if the *pattern* does not match the *string*

## Usage

If
either parameter is a list, the function returns 1 if any element
of the first parameter is like any element of the second parameter.

## Examples

1. This example returns 0. The underscore matches only a single character.

   ```
   @Like( "A big test" ; "A_test" )
   ```
2. This example returns 1. The five underscores match "<space>big<space>."

   ```
    @Like( "A big test" ; "A_____test" )
   ```
3. This example returns 1. The % matches "A big ."

   ```
   @Like( "A big test" ; "%test" )
   ```
4. This example returns 1 because the match is true for one element
   of the first parameter.

   ```
   @Like( "A big test" : "A big exam" ; "%test" )
   ```
5. This example returns 0. @Like is case-sensitive.

   ```
   @Like( "A big test" ; "A BIG test" )
   ```
6. This example returns 1. The first percent matches "100." The
   "/%" matches the percent sign because "/" is specified as the escape
   character. The last percent matches "ement."

   ```
   @Like( "A 100% improvement" ; "A %/% improv%" ; "/" )
   ```

---

## @Ln

# @Ln (Formula Language)

Returns the natural log of a number. Natural logs use e
(approximately 2.718282) as their base.

## Syntax

**@Ln(**  *number*  **)**

## Parameters

*number*

Number
or number list. May be any value greater than 0, and can contain up
to 15 decimal places.

## Return value

*naturalLog*

Number
or number list. The natural log of *number*.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

Use
@Ln in formulas requiring natural logs, such as compound growth or
loss.

@Ln is the inverse of @Exp.

## Examples

1. This example returns 0.693147180559945.

   ```
   @Ln(2)
   ```
2. This example returns 0.693147180559945 and 1.38629436111989 in
   a list.

   ```
   @Ln(2 : 4)
   ```

---

## @Locale

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @Log

# @Log (Formula Language)

Returns the common logarithm (base 10) of any number greater
than zero.

## Syntax

**@Log(**  *number*  **)**

## Parameters

*number*

Number
or number list. Must be greater than zero.

## Return value

*commonLog*

Number
or number list. The log of *number*.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

Use
@Log in any formula requiring a common log, such as the formula to
calculate the root of a number. @Log is the reciprocal of scientific
notation.

## Examples

1. This example returns 0.602059991327962.

   ```
   @Log(4)
   ```
2. This example returns 14.

   ```
   @Log(1.0E+14)
   ```
3. This example returns 0.602059991327962 and 14 in a list.

   ```
   @Log(4 : 1.0E+14)
   ```

---

## @LowerCase

# @LowerCase (Formula Language)

Converts the uppercase letters in the specified string
to lowercase.

## Syntax

**@LowerCase(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string you want to convert to lowercase.

## Return value

*lowerCaseString*

Text or text list.
The *string,* converted to lowercase letters.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

This
function is useful when you want to search for a particular value
and cannot predict whether it appears in lowercase or uppercase letters,
or a combination of the two. You can also use it as an input translation
formula to convert the contents of a field to lowercase.

## Examples

1. This example returns juan mendoza.

   ```
   @LowerCase("Juan Mendoza")
   ```
2. This example returns arm chair if the Furniture field contains
   "Arm Chair," "Arm chair," "arm chair," or "ARM CHAIR," or any other
   variation.

   ```
   @LowerCase(Furniture)
   ```
3. This example returns fletcher if William Fletcher is the name
   associated with the current hierarchical User ID.

   ```
   @LowerCase(@Right(@Name([CN];@UserName); " "))
   ```
4. This example returns juan and mendoza in a list.

   ```
   @LowerCase("Juan" : "Mendoza")
   ```

---

## @MailDbName

# @MailDbName (Formula Language)

Returns the name of the DominoÂ® server
and the name of the current user's mail database.

## Syntax

**@MailDbName**

## Return value

*Server*  **:**  *path*

Text
list with two elements:

* *server* is the canonical hierarchical name of the server
  on which the user's mail database resides.

  This element is an empty
  string ("") if:

  + The database is local
  + The formula is used in a Scheduled agent running on the server

  Use [@Name](H_NAME.html "Allows you to manipulate hierarchical names. You can abbreviate the canonical format of a name, expand an abbreviated name to its canonical format, identify particular components within the name, and reverse the order of the components so that you can categorize a view by hierarchical names.") to extract a part
  of the name; for example, [CN] to extract the common name.
* *path* is the path and file name of the database. The path
  is relative to the NotesÂ®
  or DominoÂ® data directory.

## Usage

This
function works in any formula except column formulas. When a formula
runs on a server, the server is considered the current user, so @MailDbName
returns the server's mailbox.

The returned value is formatted
as a two-item text list specifying Server : Directory\Database.NSF,
as in:

```
CN=acmemail/O=Acme : mail\dlee.nsf
```

If
the database is stored on the user's own computer, Notes/Domino returns
the empty string for the server name. For example, user Debbie Lee
may keep a local replica of her Mail database on her workstation;
when she is set up for workstation-based mail, @MailDbName returns:

```
"" : mail\dlee.nsf
```

This
is useful in applications that send mail; for example, you can use
it to determine whether the current user is set up for server-based
mail, and determine the appropriate course of action based on the
result.

You cannot use this function in Web applications.

## Examples

1. This example returns the server and path of the user's mail file.

   ```
   @MailDbName
   ```
2. This example returns the path of the user's mail file.

   ```
   @Subset(@MailDbName;-1)
   ```
3. This example returns the canonical name of the server containing
   the user's mail file.

   ```
   @Subset(@MailDbName;1)
   ```
4. This example returns the common name of the server containing
   the user's mail file.

   ```
   @Name([CN]; @Subset(@MailDbName; 1))
   ```

---

## @MailEncryptSavedPreference

# @MailEncryptSavedPreference (Formula Language)

Indicates whether the user has selected "Encrypt saved
mail" in the User Preferences dialog box.

## Syntax

**@MailEncryptSavedPreference**

## Return value

*flag*

Boolean

* Returns 1 (True) if "Encrypt saved mail" is selected
* Returns 0 (False) if "Encrypt saved mail" is not selected

## Usage

@MailEncryptSavedPreference
is used in the Mail template to determine whether to encrypt saved
memos. This function is not available in column formulas, selection
formulas, or selective replication formulas.

You cannot use
this function in Web applications.

## Examples

You design your own Mail form. To
determine whether memos created with your form and then saved should
be encrypted, use @MailEncryptSavedPreference to determine the current
user's preference. This returns 1 if the "Encrypt saved mail" check
box is selected in the User Preferences dialog box, and 0 if the Encrypt
saved mail check box is not selected.

```
@MailEncryptSavedPreference
```

---

## @MailEncryptSentPreference

# @MailEncryptSentPreference (Formula Language)

Indicates whether the user has selected "Encrypt sent mail"
in the User Preferences dialog box.

## Syntax

**@MailEncryptSentPreference**

## Return value

*flag*

Boolean

* Returns 1 (True) if "Encrypt sent mail" is selected
* Returns 0 (False) if "Encrypt sent mail" is not selected

## Usage

@MailEncryptSentPreference
is used in the Mail template to determine whether to encrypt sent
memos. This function is not available in column formulas, selection
formulas, or selective replication formulas.

You cannot use
this function in Web applications.

## Examples

You can design your own Mail form.
To determine whether outgoing memos should be encrypted automatically,
use @MailEncryptSentPreference to determine the user's preference.
This returns 1 if the "Encrypt sent mail" check box is selected in
the User Preferences dialog box, and 0 if the Encrypt sent mail check
box is not selected.

```
@MailEncryptSentPreference
```

---

## @MailSavePreference

# @MailSavePreference (Formula Language)

Indicates which option the user has selected for the "Save
sent mail" setting in the User Preferences dialog box.

## Syntax

**@MailSavePreference**

## Return value

*flag*

Integer

* Returns 0 if "Don't keep a copy" is selected
* Returns 1 if "Always keep a copy" is selected
* Returns 2 if "Always prompt" is selected

## Usage

@MailSavePreference
is used in the Mail template to determine whether to save copies of
outgoing memos. This function is not available in column formulas,
selection formulas, or selective replication formulas.

You
cannot use this function in Web applications.

## Examples

You design your own Mail form. To
determine whether outgoing memos should be automatically saved, use
@MailSavePreference to determine the user's preference. This returns
2 if the "Save sent mail" list has "Always prompt" selected, 1 if
the "Save sent mail" list has "Always keep a copy" selected, and 0
if the "Save sent mail" list has "Don't keep a copy" selected.

```
@MailSavePreference
```

---

## @MailSend

# @MailSend (Formula Language)

There are two ways to use @MailSend:

* When used with no parameters, @MailSend mails the current document
  (the one being processed when the @function is evaluated) to the recipient
  designated in the document's SendTo field. The document must have
  a SendTo field.
* When used with one or more parameters, @MailSend composes a new
  mail memo based on the information you supply in the arguments list,
  and sends it to the recipients listed in the sendTo, copyTo, and blindcopyTo
  arguments.

## Syntax

**@MailSend(**
 *sendTo* 
**;**
 *copyTo*
 **;** 
*blindCopyTo*
 **;** 
*subject*
 **;** 
*remark*
 **;** 
*bodyFields*
 **; [**
 *flags* 
**] )**

## Parameters

*sendTo*

Text
or text list. The primary recipient(s) of the mail memo.

*copyTo*

Text
or text list. Optional. The copy recipient(s) of the mail memo.

*blindCopyTo*

Text
or text list. Optional. The blind copy recipient(s) of the mail memo.

*subject*

Text.
Optional. The text you want displayed in the Subject field. This is
equivalent to the Subject field on a mail memo; the message is displayed
in the Subject column in the views in the recipients' mail databases.
If this parameter is a list, only the first element is used; use [@Implode](H_IMPLODE.html "Concatenates all members of a text list and returns a text string.") as necessary to reduce a list
to text.

*remark*

Text.
Optional. Any text you want at the beginning of the body field of
the memo. If this parameter is a list, only the first element is used;
use [@Implode](H_IMPLODE.html "Concatenates all members of a text list and returns a text string.") as necessary to reduce
a list to text.

*bodyFields*

Text list. The names
of one or more fields from the current document that you want included
in the mail memo. The fields must be of type text, text list, or rich
text, and are appended to the memo in the order in which you list
them. (You can store @Text of a numeric field in a variable and use
the variable name as a field name.) Enclose each field name in quotation
marks. If you want to list multiple fields, use the list format: "description":"issues":"resolution."
If you store the name of the field in a variable, omit the quotation
marks here.

When you use the **[IncludeDocLink]** flag
(described in the following) to include a link to the current document,
you should set the *bodyFields* parameter to null (""). If Notes/Domino
cannot locate a field by name, it uses the string literal instead.

**[**  *flags*  **]**

Keyword.
One or more flags indicating the priority and security of the memo.
If you specify multiple flags, format them as a list, as in **[SIGN]:[PRIORITYHIGH]:[RETURNRECEIPT]**.
Enclose each flag in brackets, as shown.

The available flags
are:

**[SIGN]**

Electronically sign the memo when
mailing it, using information from the user's ID. Signing does not
occur unless you include this flag. This flag cannot be used in Web
applications.

**[ENCRYPT]**

Encrypt the document
using the recipient's public key, so that only the recipient whose
private key matches can read the document. Encryption does not occur
unless you include this flag. This flag cannot be used in Web applications.

**[PRIORITYHIGH]**

Immediately
routes the message to the next-hop server, as defined by the combination
of Mail Connection records and server records. If a phone call has
to be made in order to route the message, then the call is placed
immediately, regardless of the schedule set in the Remote Connection
record. If you omit this flag, the priority defaults to Normal.

**[PRIORITYNORMAL]**

Routes
the message to the next-hop server based on the schedule defined in
the Mail Connect records. If the recipient's mail file resides on
a server on the same DominoÂ® network,
then delivery occurs immediately. If you omit this flag, the priority
defaults to Normal.

**[PRIORITYLOW]**

Routes the
message overnight if the recipient's mail file does not reside on
a server on the same Notes/Domino network. If the recipient's mail
file does reside on a server on the same Notes/Domino network, then
delivery occurs immediately. Low Priority mail can also be controlled
by a Notes/Domino environment variable called MailLowPriorityTime=x,
where x is equal to a number from 0 to 23. When placed in the server
notes.ini file, this variable tells the server when to route Low Priority
mail. If you omit this flag, the priority defaults to Normal.

**[RETURNRECEIPT]**

Notify
the sender when each recipient reads the message. No receipt is returned
unless you include this flag.

**[DELIVERYREPORTCONFIRMED]**

Notify
the sender whether delivery of the memo was successful or not. By
default, the Basic delivery report is used, which notifies the sender
only when a delivery failure occurs.

**[INCLUDEDOCLINK]**

Include
a link pointing to the document that was open or selected when @MailSend
was used. You must include this flag if you want that document linked
to the mail memo. A new document must be saved.

Note: This
option will only work if the database contains a default view.

## Usage

Use
@MailSend in agents, form actions, form events, view actions, view
events, and toolbar buttons. @MailSend is especially useful with scheduled
agents as a means of sending mail at a predetermined interval; for
example, to send reminders about a departmental meeting. One view
from the database must be selected as the Default when database is
first opened for the scheduled agent to work correctly. This function
does not work in column, selection, hide-when, or window title formulas.

If
the MailOptions field on the form is set to 0, @MailSend is disabled,
and the formula fails to execute.

If the user's
notes.ini file includes the statement

NoExternalApps=1

then
any formula involving @MailSend is disabled. The user doesn't see
an error message; the formula fails to execute.

When the [IncludeDocLink]
option is used, the database must have a default view defined, and
the linked document must be in it. Otherwise, @MailSend will fail.

When
sending to multiple recipients within the SendTo, CopyTo, or BlindCopyTo
arguments, the names must be supplied as a list with a separate list
value for each recipient. For example, to send mail to both Sam and
Martha at Big Company, the SendTo parameter would be "sam@bigcompany.com"
: "martha@bigcompany.com", not "sam@bigcompany.com, martha@bigcompany.com".

## Sending rich text fields

You can specify a rich text field as one
of the *bodyfields* in an agent formula only.

## Mail-related fields in a document

When you use @MailSend with no parameters,
the current document may contain one or more mail-related fields;
if it does, those fields are used when routing the document.

* If the document contains the CopyTo or BlindCopyTo fields, it
  is routed to those recipients at the same time.
* If the document contains the DeliveryPriority, DeliveryReport,
  or ReturnReceipt fields, they are used to control the delivery priority,
  generation of a delivery report, and generation of a return receipt,
  just as they are used in the Actions - Send Document command. If the
  document doesn't contain these fields, they default to normal priority,
  no delivery report, and no return receipt, respectively.

## Examples

1. This formula sends a memo to David Lee with a blind copy to Joseph
   Smith in Support. The memo is titled "Status Report," and its body
   contains the message "Sorry it's late!" plus the contents of the STATUS
   and PLANS fields from the current document. The document is mailed
   with the following options: it is signed, delivery confirmation is
   requested, and a return receipt will be sent when each recipient reads
   the memo. The recipients are listed using distinguished naming syntax
   (available to Release 3 users only). The copyTo information was omitted,
   and was replaced with the null string because additional arguments
   follow.

   ```
   @MailSend("David Lee/";"";"Joseph Smith/Support";"Status Report"; "Sorry it's late!"; "STATUS":"PLANS"; [SIGN] : [DELIVERYREPORTCONFIRMED] : [RETURNRECEIPT])
   ```
2. This formula sends a memo to Mary Tsen and to Joseph Smith in
   Support. The subject uses the text stored in the current document's
   TOPIC field, and the body of the memo draws from the COMMENTS field.
   The copyTo, blindCopyTo, and remark arguments were omitted, and were
   replaced with null strings because additional arguments still followed.
   The flags were omitted, but because no arguments followed their position,
   the null string was not needed.

   ```
   @MailSend("Mary Tsen/":"Joseph Smith/Support";"";"";TOPIC;""; "COMMENTS")
   ```
3. This formula sends a memo to Mary Tsen with the message "Follow
   this link" in the Subject field, and a link to the original document
   in the Body field.

   ```
   @MailSend("Mary Tsen/";"";"";"Follow this link";"";"";[IncludeDocLink])
   ```
4. This agent formula sends Martha O'Connell the contents of the
   Comments rich text field in a memo with the subject Feedback. The
   agent is triggered on an Action menu selection event and its target
   is the selected documents. The formula implodes the multi-value Items
   field to include it as the remarks parameter.

   ```
   @MailSend("Martha O'Connell/MA/Acme"; ""; ""; "Feedback"; @Implode(Items; ", "); "Comments")
   ```

---

## @MailSignPreference

# @MailSignPreference (Formula Language)

Indicates whether the user has selected "Sign sent mail"
in the User Preferences dialog box.

## Syntax

**@MailSignPreference**

## Return value

*flag*

Boolean

* Returns 1 (True) if "Sign sent mail" is selected
* Returns 0 (False) if "Sign sent mail" is not selected

## Usage

@MailSignPreference
is used in the Mail template to determine whether to attach an electronic
signature to outgoing memos. This function is not available in column
formulas, selection formulas, or selective replication formulas.

You
cannot use this function in Web applications.

## Examples

You design your own Mail form. To
determine whether outgoing memos should be electronically signed,
use @MailSignPreference to determine the user's preferences. This
returns 1 if the "Sign sent mail" check box is selected in the User
Preferences dialog box, and 0 if the "Sign sent mail" check box is
not selected.

```
@MailSignPreference
```

---

## @Matches

# @Matches (Formula Language)

Tests a string for a pattern string. Because the pattern
string can contain a number of "wildcard" characters and logical symbols,
you can test for complex character patterns.

## Syntax

**@Matches(**  *string*  **;**  *pattern*  **)**

## Parameters

*string*

Text
or text list. The string you want to scan in quotes. You can also
enter the field name of a field that contains the string you want
to scan; do not surround the field name in quotes.

*pattern*

Text
or text list. The pattern you want to scan for in *string* surrounded
by quotation marks*.* May contain wildcard characters and symbols
(see the following table). The following symbols require a preceding
backslash unless the pattern is enclosed in braces { } as a set: ?,
\*, &, !, |, \, +. The symbols require two preceding backslashes
instead of one if the pattern is specified as a literal. This is because
the backslash is an escape character in string literals, so "\?" passes
"?" to the matching engine, where it is treated as a wildcard, while
"\\?" passes "\?" to the matching engine, where it is treated as a
question mark character.

Note: Simple
characters in the pattern are not case-sensitive. Characters enclosed
in braces must be matched exactly, and are case-sensitive. The character
set {A-z} includes not just upper and lower case alphabet characters
but also the backslash, underscore, and brackets characters.

## Return value

*flag*

Boolean

* Returns 1 (True) if the string contains the pattern
* Returns 0 (False) if the string does not contain the pattern

The wildcard characters and symbols are as follows:

| Symbol | Use |
| --- | --- |
| C | Where C is any character. Matches any single, non-special character C (or c) |
| ? | Matches any single character |
| \* | Matches any string (any number of characters) |
| {ABC} | Matches any character in set ABC |
| {A-FL-R} | Matches any character in the sets A...F and L...R |
| +C | Matches any number of occurrences of C (or c) |
| ! | Complements logical meaning of the pattern (logical NOT) |
| | | Performs logical OR of two patterns |
| & | Performs logical AND of two patterns |

Note: When specifying sets, be sure to enclose
them in { } (curly braces). For example, the set A...F is represented
as {A-F}.

Examples of pattern matching:

| Pattern | Matches |
| --- | --- |
| ABC | The three-character string [a|A][b|B][c|C] |
| {ABC}{ABC} | Any two-character string composed of capital letters A, B, or C |
| A?C | Any three-character string that starts with a|A and ends with c|C |
| ??? | Any three-character string |
| +? | Any string, including the null string |
| +?{A-Z} | Any string that ends in a capital letter |
| +{!A-Z} | Any string that does not contain a capital letter |

## Usage

If
the first or second parameter is a list, the function returns true
if any element in the second parameter matches any element in the
first parameter.

## Examples

1. This example returns 0.

   ```
   @Matches("A big test";"a?test")
   ```
2. This example returns 1.

   ```
   @Matches("A big test";"a?????test")
   ```
3. This example converts the contents of the State field to lowercase,
   and returns 1 for any value in the field that contains "mont," for
   example Vermont or Montana.

   ```
   @Matches(@Lowercase(State);"*mont*")
   ```
4. This example is the default value formula for a field named SalesNumber.
   The formula returns the number 224 if the content of the Division
   field is either Central or Midwest. If the content of Division is
   anything else, the formula returns the number 124.

   ```
   @If(@Matches(Division;"Central | Midwest");224;124)
   ```
5. This code, when added as the validation formula for a number field
   called input, displays the error message, "Value cannot be a letter"
   if the user enters any lowercase or uppercase letter between A and
   Z.

   ```
   @If(@Matches(@Text(input);"+{!A-z}");@Success;@Failure("Value cannot be a letter"))
   ```

   Note: The validation error message is also triggered if the
   user enters a backslash, underscore, or brackets because specifying
   A-z, specifies all ASCII characters between the uppercase A and lowercase
   z. The backslash, underscore, and brackets are included in this set
   of characters.
6. This code, when added as the validation formula for the US\_State
   editable text field, displays the error message, "Entry must be a
   valid two-letter state abbreviation" if the user enters anything besides
   two upper-case letters.

   ```
   @If(@Matches(US_State;"{A-Z}{A-Z}");@Success;@Failure("Entry must be a valid two-letter state abbreviation"))
   ```
7. This example returns 0 because no item in the second list matches
   an item in the first list.

   ```
   @Text(@Matches("one" : "two" : "three"; "four" : "five" : "six"))
   ```
8. This example returns 1 because one item in the second list matches
   an item in the first list.

   ```
   @Text(@Matches("one" : "two" : "three"; "three" : "four" : "five" : "six"))
   ```

---

## @Max

# @Max (Formula Language)

Returns the largest number in a single list, or the larger
of two numbers or number lists.

Note: The single-parameter form of this @function
is new with Release 6.

## Syntax

**@Max(**  *number1*  **)**

**@Max(**  *number1*  **;**  *number2*  **)**

## Parameters

*number1*

Number
or number list.

*number2*

Number or number list.

## Return value

*maxNumber*

(Single
parameter) Number. The largest number in *number1*.

(Two
parameters) Number or number list. Either *number1* or *number2,* whichever
is larger. If the parameters are number lists, @Max returns a list
that is the result of pair-wise computation on the list values.

## Usage

When
using this function with a number list constant, remember that the
list concatenation operator takes precedence over other operators.
Enclose negative numbers in parentheses.

## Examples

1. This example returns 3.

   ```
   @Max(1;3)
   ```
2. This example returns 99;6;7;8.

   ```
   @Max(99:2:3;5:6:7:8)
   ```
3. This example returns -2; 45; 54.

   ```
   @Max((-2.6):45:(-25);(-2):(-50):54)
   ```
4. This formula finds the larger of the values in the fields named
   Commission and Salary, and compares the value to 50,000; if it is
   larger than 50,000, the Bonus field is changed to 0; if it is smaller
   than 50,000, Bonus becomes 10% of the value in the Salary field.

   ```
   FIELD Bonus:=@If(@Max(Commission;Salary)>50000; 0; (0.10 * Salary));
   ```
5. This example returns 99.

   ```
   @Max(99 : 2 : 3)
   ```
6. This formula compares the corresponding sales figures in the jian\_yrtotal
   and julie\_yrtotal fields (which contain number lists) and returns
   a number list containing the larger of the two figures per element.
   If there are seven elements in the jian\_yrtotal field and five in
   the julie\_yrtotal field, this formula only returns the five largest
   numbers. Unlike the default behavior, which is to first repeat the
   fifth element until the list lengths are equal and then compare all
   seven corresponding elements in the lists, it does not repeat the
   fifth number in the julie\_yrtotal field twice before performing the
   comparison.

   ```
   tjian := @Count(jian_yrtotal);
   tjulie := @Count(julie_yrtotal);
   dif := (tjian - tjulie);
   result := @If(@Sign(dif) = -1;@Subset(julie_yrtotal;(tjulie - @Abs(dif)));
   @Subset(jian_yrtotal;(tjian - dif)));
   result2 := (@If(@Sign(dif) = -1;
   @Max(jian_yrtotal;result);@Max(result;julie_yrtotal));"
   @If(@IsError(result2);"One of your list fields is empty";result2)
   ```

---

## @Member

# @Member (Formula Language)

Given a value, finds its position in a text list.

## Syntax

**@Member(**  *value*  **;**  *stringlist*  **)**

## Parameters

*value*

Text.
The value you want to find in *stringlist*.

*stringlist*

Text
list.

## Return value

*position*

Number

* Returns 0 if the *value* is not contained in *stringlist*
* Returns 1 to n if the *value* is contained in the *stringlist*,
  where 1 to n is the position of the *value* in the *stringlist*

## Examples

1. This example returns 0.

   ```
   @Member("Sales";"Finance":"Service":"Legal")
   ```
2. This example returns 12 if the value in the ReportName field is
   the 12th value in a list contained in the RequiredReading field; otherwise
   it returns 0.

   ```
   @Member(ReportName;RequiredReading)
   ```

---

## @MiddleBack

# @MiddleBack (Formula Language)

Returns
any substring from the middle of a string. The middle is found by
scanning the string from right to left, and parameters determine where
the middle begins and ends.

## Syntax

**@MiddleBack(**  *string*  **;**  *offset*  **;**  *numberchars*  **)
@MiddleBack(**  *string*  **;**  *offset*  **;**  *endstring*  **)
@MiddleBack(**  *string*  **;**  *startString*  **;**  *endstring*  **)
@MiddleBack(**  *string*  **;**  *startString*  **;**  *numberchars*  **)**

## Parameters

*string*

Text
or text list. Any string.

*offset*

Number. A
character position in *string* that indicates where you want
the middle to begin, always counting from right to left. The middle
begins one character after the *offset*. Always add 1 to this
number; the end of the string is marked by one non-visible character
and must be counted in the offset.

*startString*

Text.
A substring of *string* that indicates where you want the middle
to begin, always counting from right to left. The middle begins one
character after the end of *startString.*

*numberchars*

Number.
The number of characters that you want in the middle. If *numberchars* is
negative, the middle starts at *offset* or *startString* and
continues from right to left. If *numberchars* is positive, the
middle starts one character past the *offset* or *startString* and
continues from left to right.

*endstring*

Text.
A substring of *string* that indicates the end of the middle.
@MiddleBack returns all the characters between *offset* and *endstring*,
or between *startString* and *endstring.*

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Return value

*middle*

Text or text list. The substring
from the middle of *string,* which begins at the *offset* or *startString* you
specify and ends at the *endstring* you specify, or after the *numberchars* have
been reached.

If *endstring* is not found in the string, it returns the right string from
*startString*  or offset.

## Examples

1. This example returns Alt if the content of the Author field is
   Timothy Altman.

   ```
   @MiddleBack(Author;" ";3)
   ```
2. This example returns an empty string if the content of the Author
   field is any string with no spaces, for example "Smith."

   ```
   @MiddleBack(Author;" ";3)
   ```
3. This example returns" from right to left" with a space before
   "from."

   ```
   @MiddleBack("Middleback searches the string from right to left"; "ing";25)
   ```
4. This example returns "searches the string ."

   ```
   @MiddleBack("@MiddleBack searches the string from right to left"; "from"; -20)
   ```
5. This example returns " is the " with spaces before and after "is
   the." The return string is everything from the fifth to the last character
   through the character after "This."

   ```
   @MiddleBack("This is the text"; 5; "This")
   ```
6. This example returns " the " with spaces before and after "the."
   The return string is everything before "text" and after "is."

   ```
   @MiddleBack("This is the text"; "text"; "is")
   ```
7. This example returns "world" and "time" in a list. The offset
   is the end of each text element and the end text is the last space.

   ```
   @MiddleBack("Hello world" : "This is the time"; 0; " ")
   ```

---

## @Middle

# @Middle (Formula Language)

Returns
any substring from the middle of a string. The middle is found by
scanning the string from left to right, and parameters determine where
the middle begins and ends.

## Syntax

**@Middle(**  *string*  **;**  *offset*  **;**  *numberchars*  **)
@Middle(**  *string*  **;**  *offset*  **;**  *endstring*  **)
@Middle(**  *string*  **;**  *startString*  **;**  *endstring*  **)
@Middle(**  *string*  **;**  *startString*  **;**  *numberchars*  **)**

## Parameters

*string*

Text
or text list. Any string.

*offset*

Number. A
character position in *string* that indicates where you want
the middle to begin, always counting from left to right. The middle
begins one character after the *offset*.

*startString*

Text.
A substring of *string* that indicates where you want the middle
to begin, always counting from left to right. The middle begins one
character after the end of *startString.*

*numberchars*

Number.
The number of characters that you want in the middle. If *numberchars* is
negative, the middle starts at *offset* or *startString* and
continues from right to left. If *numberchars* is positive, the
middle starts one character past the *offset* or *startString* and
continues from left to right.

*endstring*

Text.
A substring of *string* that indicates the end of the middle.
@Middle returns all the characters between *offset* and *endstring*,
or between *startString* and *endstring.*

## Return value

*middle*

Text or text list. The substring
from the middle of *string,* which begins at the *offset* or *startString* you
specify and ends at the *endstring* you specify, or after the *numberchars* have
been reached.

If *endstring* is not found in the string, it returns the right string from
*startString*  or offset.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Examples

1. This example returns h C. The offset is positioned at the "t"
   (the fourth character from the left), and the count starts with the
   first character *after* the offset, moving from left to right.

   ```
   @Middle("North Carolina";4;3)
   ```
2. This example returns ort. The offset is positioned at the "t"
   (the fourth character from the left), and the count begins *at* the
   offset, moving from right to left.

   ```
   @Middle("North Carolina";4;-3)
   ```
3. This example returns Car. The offset is positioned at the first
   space in the string "North Carolina" and the count starts with the
   first character after the offset.

   ```
   @Middle("North Carolina";" ";3)
   ```
4. This example returns or. The offset is positioned at the substring
   "th" and the count starts with the first character after the entire
   offset, moving from right to left.

   ```
   @Middle("North Carolina";"th";-2)
   ```
5. This example returns " is the " with spaces before and after "is
   the." The return string is everything from the fifth character through
   the character before "text."

   ```
   @Middle("This is the text"; 4; "text")
   ```
6. This example returns " the " with a space before and after "the."
   The return string is everything after " is" and before "text." The
   startString " is" begins with a space; this prevents @Middle from
   returning a string that starts at the "is" in the word "This."

   ```
   @Middle("This is the text"; " is"; "text")
   ```
7. This example returns "Hello" and "This" in a list. The offset
   is the beginning of each text element and the end text is the first
   space.

   ```
   @Middle("Hello world" : "This is the time"; 0; " ")
   ```

---

## @Min

# @Min (Formula Language)

Returns the smallest number in a single list, or the smaller
of two numbers or number lists.

Note: The single-parameter form of this @function
is new with Release 6.

## Syntax

**@Min(**  *number1*  **)**

**@Min(**  *number1*  **;**  *number2*  **)**

## Parameters

*number1*

Number
or number list.

*number2*

Number or number list.

## Return value

*minNumber*

(Single
parameter) Number. The smallest number in *number1*.

(Two parameters) Number or number list. Either *number1* or *number2,* whichever is
smaller. If the parameters are number lists, @Min returns a list that is the result of pair-wise
computation on the list values.

## Usage

When
using this function with a number list constant, remember that the
list concatenation operator takes precedence over other operators.
Enclose negative numbers in parentheses.

## Examples

1. This example returns 35.

   ```
   @Min(35;100)
   ```
2. This example returns 5;2;3;3.

   ```
   @Min(99:2:3;5:6:7:8)
   ```
3. This example returns the contents of the field containing the
   smallest value. If Precinct1 contains 150,000 and Precinct2 contains
   100,000, then this formula returns 100,000.

   ```
   @Min(Precinct1;Precinct2)
   ```
4. This example returns 85,000 if 100,000 is the smallest number
   contained in either of the fields AreaAPopulation or AreaBPopulation,
   and the field DistrictPopulation contains the value 15,000.

   ```
   @Min(AreaAPopulation;AreaBPopulation) - DistrictPopulation
   ```
5. This example returns -3.5;-35;54.

   ```
   @Min((-3.5):(-35):100;(-2):45:54)
   ```
6. This example returns 2.

   ```
   @Min(99 : 2 : 3)
   ```

---

## @Minute

# @Minute (Formula Language)

Extracts the number of minutes from the specified time-date.

## Syntax

**@Minute(**  *time-date*  **)**

## Parameters

*time-date*

Time-date
or time-date list. The value with the minute that you want to extract.

## Return value

*minutes*

Number or number list. The
number of minutes in the minute part of the time. Returns -1 if the
time-date provided contains only a date and not a time value.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 30.

   ```
   @Minute([9:30])
   ```
2. This example returns 15 and 30 in a list.

   ```
   @Minute([9:15] : [9:30])
   ```
3. This example returns 56 if the Time field contains 8:56:34 P.M.

   ```
   @Minute(Time)
   ```
4. This example returns 59 if the Date field contains: 7/30/95 9:59:59.

   ```
   @Minute(Date)
   ```
5. This example returns 00 if the current document's created date
   was 9/29/95 3:00:12 A.M.

   ```
   @Minute(@Created)
   ```

---

## @Modified

# @Modified (Formula Language)

Returns a time-date value indicating when the document
was modified initially.

## Syntax

**@Modified**

## Return value

*lastModified*

Time-date. The date
when the current document was last modified.

## Usage

@Modified
works correctly in column formulas and computed-for-display formulas.

When used in computed fields, @Modified returns a value representing
the next-to-last time the document was saved. For example, if you
modified and saved a document on the mornings of May 5th and 6th,
then accessed the document in the afternoon on May 6th, the @Modified
computed field would return the May 5th modification date, since the
5th was the next-to-last time the document was saved.

This
function does not work in navigators, mail agent, paste agent, hide-when,
section editor, or form formulas.

@Modified
and LastModified are not equivalent.

* @Modified reflects the "Modified (Initially)" document property
  and is equivalent to the Last Modified simple function.
* LastModified reflects the "Modified (In this file)" document property
  and is roughly equivalent to @Accessed and the Last Read or Edited
  simple function.

## Examples

1. This example returns 9/30/95 11:00:00 AM if the document was last
   saved on September 30, 1995 at 11:00 A.M.

   ```
   @Modified
   ```
2. This example returns a string made up of the contents of the Topic
   field, then a space, then the string Last Edited: and then the time-date
   value of the last time the document was saved, converted to text.

   ```
   Topic + " " + "Last Edited: " + @Text(@Modified)
   ```

---

## @Modulo

# @Modulo (Formula Language)

Returns the remainder of a division operation.

## Syntax

**@Modulo(**  *number1*  **;**  *number2*  **)**

## Parameters

*number1*

Number
or number list.

*number2*

Number or number list.
If this is equal to 0, @Modulo returns @ERROR.

## Return value

*remainder*

Number or number list.
The remainder of *number1* divided by *number2.* If the
parameters are number lists, @Modulo returns a list that is the result
of pair-wise computation on the list values. The sign of the result
is always the same as the sign of the *number1*.

## Usage

A
common use of @Modulo is to determine whether a number is odd or even;
if the result of @Modulo(number;2) is 1, the number is odd; if the
result is 0, the number is even.

When using this function
with a number list, the list concatenation operator takes precedence
over any other operators; negative numbers must be enclosed in parentheses.

## Examples

1. This example returns 1.

   ```
   @Modulo(4;3)
   ```
2. This example returns 0.

   ```
   @Modulo(4;2)
   ```
3. This example returns -2.

   ```
   @Modulo((-14);3)
   ```
4. This example returns -1;2;3;-3.

   ```
   @Modulo((-4):6:8:(-9);3:4:5:6)
   ```
5. This example returns 1 and 3 in a list.

   ```
   @Modulo(5 : 7; 4)
   ```

---

## @Month

# @Month (Formula Language)

Extracts the number of the month from the specified time-date.

## Syntax

**@Month(**  *time-date*  **)**

## Parameters

*time-date*

Time-date
or time-date list. The value with the month that you want to extract.

## Return value

*month*

Number or number list. The
number of the month. Returns -1 if the time-date provided contains
only a time value and not a date.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 1.

   ```
   @Month([1/15/88])
   ```
2. This example returns 1 and 2 in a list.

   ```
   @Month([1/15/88] : [2/15/88])
   ```
3. This example returns 12 if it is December.

   ```
   @Month(@Now)
   ```
4. This example returns 2 if it is any date in December other than
   the 30th or the 31st. If it is December 30th or 31st, returns 3.

   ```
   @Month(@Adjust(@Now;2;2;2;2;2;2))
   ```
5. This formula returns a formatted date string based on the contents
   of the dueDate field. For example, if dueDate contains "06/26/95"
   the formula returns June 26, 1995. If dueDate contains "01/24/96 3:40:43
   P.M.," the formula returns January 24, 1996.

   ```
   space:= " ";
   comma:=",";
   month:=@Select(@Month(dueDate);"January";"February";"March";
   "April";"May";"June";"July";"August";"September";"October";
   "November";"December");
   day:=@Text(@Day(dueDate));
   year:=@Text(@Year(dueDate));
   month + space + day + comma + space + year
   ```

---

## @Name

# @Name (Formula Language)

Allows you to manipulate hierarchical names. You can abbreviate
the canonical format of a name, expand an abbreviated name to its
canonical format, identify particular components within the name,
and reverse the order of the components so that you can categorize
a view by hierarchical names.

Enables you to convert a name between the DominoÂ® and LDAP formats.

Note: LDAP
conversion is new with Release 6.

## Syntax

**@Name(
[**  *action*  **] ;**  *name*  **)**

## Parameters

**[**  *action*  **]**

Keyword.
Indicates what you want done to the name -- whether you want to expand
it, abbreviate it, convert it, and so on (see list of possible actions).

With
@Name, you can perform the following actions:

**[A]**

Returns
the ADMD component (administration management domain name) of a hierarchical
name.

**[ABBREVIATE]**

Abbreviates a hierarchical
name, removing the component labels. This saves space in the display,
and looks friendlier.

**[ADDRESS821]**

Note: This
keyword is new with R5.

Returns an Internet address in the
format based on RFC 821 Address Format Syntax regardless of whether
the original address was in RFC 821 or RFC 822 form. Case must be
exact.

**[C]**

Returns the country/region component
of a hierarchical name.

**[CANONICALIZE]**

Expands
an abbreviated name, adding in whatever components are missing, as
well as their labels. Missing components are taken from the current
user ID, not from the DominoÂ® Directory.

**[CN]**

* Returns the common name component of a DominoÂ® name.
* Returns the local part of an Internet address in the format based
  on RFC 821 Address Format Syntax.
* Returns the phrase part of an Internet address in the format based
  on RFC 822 Address Format Syntax.

**[G]**

Returns the given name component (the
first name) of a hierarchical name.

**[HIERARCHYONLY]**

Note: This keyword is new with R5.

Strips the CN component
of a hierarchical name and returns the remaining components.

**[I]**

Returns
the initials component of a hierarchical name.

**[LP]**

Note: This keyword is new with R5.

Returns the LocalPart
of a standard Internet address based on RFC 822 Address Format Syntax.

**[O]**

Returns
the organization component of the hierarchical name.

**[OU**  ***n***  **]**

Returns
the specified organizational unit component of a hierarchical name;  ***n***  can
be from 1 to 4, as in OU1. In the canonical form of the name, the
OU components are not numbered; however, they are counted from first
to last so that the first occurrence of the OU label is treated as
OU1, the second occurrence is treated as OU2, and so on. Notes/Domino
does not accept **[OU]** as a keyword.

**[P]**

Returns
the PRMD component (private management domain name) of a hierarchical
name.

**[PHRASE]**

Note: This keyword
is new with R5.

Returns the Phrase part of a standard Internet
address based on RFC 822 Address Format Syntax.

**[Q]**

Returns
the generation component (such as "Jr") of a hierarchical name.

**[S]**

Returns
the surname component (the last name) of a hierarchical name.

**[TOAT]**

Note: This keyword is new with Release 6.

Returns
the LDAP AttributeType name when a DominoÂ® field
name is provided.

**[TODATATYPE]**

Note: This
keyword is new with Release 6.

Returns the DominoÂ® data type name when an LDAP Syntax
name is provided.

**[TOFIELD]**

Note: This
keyword is new with Release 6.

Returns the DominoÂ® field name when an LDAP AttributeType
name is provided.

**[TOFORM]**

Note: This
keyword is new with Release 6.

Returns the DominoÂ® form name when an LDAP ObjectClass
name is provided.

**[TOKEYWORD]**

Reverses the
order in which the naming components are displayed, and replaces slashes
with backslashes: Country\Organization\Organization Unit... This is
useful when you want to categorize a view by the components of a user's
hierarchical name (backslashes represent subcategories in views).
The **[TOKEYWORD]** option does *not* return the Common Name
portion of the user name.

**[TOOC]**

Note: This
keyword is new with Release 6.

Returns the LDAP ObjectClass
name when a DominoÂ® form or
subform name is provided.

**[TOSYNTAX]**

Note: This keyword is new with Release 6.

Returns
the LDAP Syntax name when a DominoÂ® data
type name is provided.

*name*

Text or names,
or text or names list. A user or server name, entered in any form
(Notes/Domino determines the full hierarchical name and then returns
the requested components) or an LDAP AttributeType, ObjectClass, or
Syntax name or a DominoÂ® form,
subform, field, or data type name to be converted from LDAP to DominoÂ® format or vice-versa.

## Return value

*formattedname*

Text
or names, or text or names list. The second parameter formatted according
to the first parameter.

## Usage

@Name
is particularly useful for abbreviating hierarchical names in a view.

If
the second parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

A hierarchical name is qualified with a series of
components identifying the full name, organizational unit, organization,
and country or region. Using hierarchical names guarantees that each
user and server has a unique name.

As the database designer,
you are responsible for controlling how user names are entered and
displayed within NotesÂ® applications.
For simplicity, you should allow users to enter names in abbreviated
form; then you can use @Name to expand the name to its canonical format.
You should also display names in abbreviated form, using @Name to
convert the stored canonical format of the name to its abbreviated
form.

When you use a Names, Readers, or Authors field, Notes/Domino
automatically converts hierarchical names to an appropriate format
for display and storage. If the user enters an abbreviated name, Notes/Domino
expands it to canonical format when storing it; the name is always
displayed on a form in abbreviated format.

When you display
the contents of a hierarchical name field in a view there is no automatic
conversion; the entire canonical format of the name is displayed.
You may want to convert the name to its abbreviated form with @Name.

If
you are using @Name to parse an Internet address, the address must
conform to the format based on the standard RFC 821 or RFC 822 Address
Format Syntax.

Note: If you attempt to use the parameters
A, G, I, P, Q, or S in Notes/Domino with existing user IDs, it may
appear as though the parameters do not work. These parameters were
added to take advantage of the addressing used for external mail and
gateway products. When a mail message is received within Notes/Domino
from an external mail source, the naming convention can include additional
components. The @Name function can be used to manipulate the hierarchical
name, including these additional components. DominoÂ® IDs and names do not use these additional
components, therefore, it is not possible to use these six parameters
with a standard DominoÂ® ID
and name.

The following is an example of a full hierarchical
name that takes advantage of every parameter.

G=Joe/I=JS/S=Smith/Q=Jr/CN=Joseph
Smith/OU=Assembly/OU=Engineering/O=Acme/P=PrivAdmin/ A=PubAdmin/C=US

## Examples

1. This example returns Mary Tsen/Illustration/ Documentation/Development/R&D/WorkSavers/US
   if a user is looking at a document where the AUTHOR field contains
   the hierarchical form of Mary Tsen's name. .

   ```
   				@Name([ABBREVIATE];AUTHOR)
   ```
2. This example returns Mary Tsen.

   ```
   @Name([CANONICALIZE];"Mary Tsen")
   ```

   Since
   there is no slash following the name, it is a nonhierarchical name
   and has no additional components.
3. This example returns CN=MaryTsen/ OU=Illustration/OU=Documentation/OU=Development/OU=R&D/O=Acme/C=US
   if that is the current user ID. The hierarchy of the current user
   ID is appended to the name; no lookup occurs in the DominoÂ® Directory.

   ```
   @Name([CANONICALIZE];"Mary Tsen/")
   ```
4. This example returns Mary Tsen in an informational dialog box
   format, if the AUTHOR field in the document contains: CN=Mary Tsen/OU=Illustration/O=Acme.

   ```
   @Prompt([Ok]; "Common Name"; @Name([CN]; AUTHOR))
   ```
5. This example returns Development.

   ```
   @Name([OU2];AUTHOR)
   ```
6. This example returns US\Acme\R&D\Development\Documentation\Illustration.
   The slashes are now backslashes, which allow the naming components
   to be used as subcategories in a view. The common name component is
   not returned.

   ```
   @Name([TOKEYWORD];AUTHOR)
   ```
7. This example returns SStreitfeld if the User\_Name field contains
   this Internet address in RFC 822 format "Streitfeld, Sara (Miami)" <SStreitfeld@gazette.com>
   .

   ```
   @Name([LP];User_Name)
   ```
8. This example returns "Streitfeld, Sara (Miami)" if the User\_Name
   field contains this Internet address in RFC 822 format "Streitfeld,
   Sara (Miami)" <SStreitfeld@gazette.com> .

   ```
   @Name([Phrase];User_Name)
   ```
9. This example returns SStreitfeld@gazette.com if the User\_Name
   field contains this Internet address in RFC 822 format "Streitfeld,
   Sara (Miami)" <SStreitfeld@gazette.com> .

   ```
   @Name([ADDRESS821];User_Name)
   ```
10. This example returns Cam/IBM If the User\_Name field contains John
    Doe/Cam/IBM.

    ```
    @Name([HIERARCHYONLY];User_Name)
    ```
11. This example returns "secretary," the LDAP AttributeType name
    for the DominoÂ® term, "assistant."

    ```
    @Name([TOAT];"assistant")
    ```
12. This example returns "Internet Address," the DominoÂ® term equivalent to the LDAP AttributeType
    name "mail."

    ```
    @Name([TOFIELD];"mail")
    ```
13. This example returns "Number," the DominoÂ® term equivalent to the LDAP data type,
    "Integer."

    ```
    @Name([TODATATYPE];"Integer")
    ```
14. This example returns "Directory String," the syntax used in the
    LDAP directory for the DominoÂ® data
    type "Text."

    ```
    @Name([TOSYNTAX];"Text")
    ```
15. This example returns "Mary Tsen" and "Jacques Blanc" in a list.

    ```
    @Name([CN]; "Mary Tsen/Acme/US" : "Jacques Blanc/Acme/FR")
    ```

---

## @NameLookup

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @Narrow

*Documentation page not available (404 - Page Not Found on HCL documentation site).*

---

## @NewLine

# @NewLine (Formula Language)

Inserts a new line (carriage return) into a text string.

## Syntax

**@NewLine**

## Return value

*carriageReturn*

Text. A carriage return.

## Usage

On
the Web, this function does not work in selection, form, or window
title formulas.

In NotesÂ®,
this function does not work in selection, hide-when, column, window
title, form formulas, or inside of @Prompt.

If you need to
insert a carriage return inside an @Prompt formula, see [@Char](H_CHAR.html "Converts an HCL Code Page 850 code number into the corresponding single character string.").

Tip: To add multiple
lines to a single column row:

1. In the View Properties box:
   * Change the Lines per row to the number of carriage returns you
     want to include in the row.
   * Select Shrink rows to content.
2. In the Column Properties box:
   * Choose New Line as the Multi-value separator.
   * Deselect the Show multiple values as separate entries check box.
3. In the code for the column formula, specify each string or number
   that you want to display on a new line as a separate value. Since
   you set the Multi-value separator to New Line, this inserts a carriage
   return between each value. For example, the following column formula
   vertically lists the content of the FirstName field preceding the
   content of the LastName field in the column row:

   ```
   first:= FirstName;
   last := LastName;
   @Trim(first : last)
   ```

## Examples

1. This returns

   Hi There

   ```
   "Hi"+@NewLine+"There"
   ```
2. This returns

   Foster, Steven

   in the EmpName field if
   the string in the LastName field is Foster, and the string in the
   field named FirstName is Steven.

   ```
   FIELD EmpName:= LastName + "," + @NewLine + FirstName;
   ```
3. This input translation formula uses @Newline to replace all occurrences
   of "%" with a carriage return. If the description field contains "Here
   we are now%Entertain us," the formula translates it to:

   Here we
   are now Entertain us

   ```
   @Implode(@Explode(description; "%"); @NewLine)
   ```

---

## @No

# @No (Formula Language)

Returns the number 0.

## Syntax

**@No**

## Return value

*no*

Number.
Zero (0).

## Usage

This
function is equivalent to @False.

## Examples

1. This example returns 0.

   ```
   @No
   ```
2. This example returns 1 if the value in the Cost field is greater
   than 100; otherwise returns 0.

   ```
   @If(Amount < 1000; @No; !(@UserRoles = "[Manager]")
   ```

---

## @NoteID

# @NoteID (Formula Language)

The ID number of the current document.

## Syntax

**@NoteID**

## Return value

**NT** *idnumber*

String. The prefix **NT** followed
by the note ID.

## Usage

This
function does not work in forms or navigators.

---

## @Nothing

# @Nothing (Formula Language)

Use with an [@Transform](H_TRANSFORM.html "Applies a formula to each element of a list and returns the results in a list.") formula.
Returns no list element (reducing the return list by one element).
Not valid in any other context.

Note: This @function
is new with Release 6.

## Syntax

**@Nothing**

## Return value

*nothingOrNull*

Nothing or null. Returns
nothing in an @Transform formula, or null.

## Usage

See [@Transform](H_TRANSFORM.html "Applies a formula to each element of a list and returns the results in a list.") for additional information
and examples.

## Examples

See [@Transform](H_TRANSFORM.html "Applies a formula to each element of a list and returns the results in a list.").

---

## @Now

# @Now (Formula Language)

Returns the current time-date.

## Syntax

**@Now(** *flags* **;** *serverNames* **)**

Note: The *flags* and *serverNames* parameters are
new with Release 6.

## Parameters

*flags*

Keyword
or keyword list. Optional.

* **[SERVERTIME]** gets the time-date from the server containing
  the database if *serverNames* is not specified or from *serverNames* if*serverNames* is
  specified.
* **[LOCALTIMEONERROR]** gets the time-date from the local computer
  if an error occurs getting it from a specified server.

*serverNames*

Text or text list. Optional.
A server name or a list of server names. This parameter applies when
[SERVERTIME] is specified.

## Return value

*now*

Time-date
or time-date list. The current time-date of the local computer, the
server containing the current database, or one or more specified servers.
See the "Usage" section that follows.

## Usage

@Now
gets the time-date of the local computer in the following cases:

* No parameters are specified.
* [SERVERTIME] is specified, but the database is local and *serverNames* is
  not specified.
* [LOCALTIMEONERROR] is specified, *serverNames* is specified,
  and an error occurs getting the time-date from a server.

@Now gets the time-date of the server containing the current
database if [SERVERTIME] is specified and *serverNames* is not
specified.

@Now gets the time-date or time-dates of one or
more specified servers if [SERVERTIME] and *serverNames* are
specified.

An error occurs if @Now cannot get the time from
a server specified in *serverNames* and [LOCALTIMEONERROR] is
not specified.

Using @Now in column or selection formulas may
impact the efficiency of your application. It also causes the view
refresh indicator to display constantly.

The @Now function
returns the current time with one hundredths of a second precision.
However, if you use @Now to specify the current time in a computed
field, the hundredths of a second value is always rounded up to the
next second, which can result in the current time being one second
fast. You can avoid this by replacing @Now with the following formula:

```
timenow := @Now;
@Date(@Year(timeNow);@Month(timeNow);@Day(timeNow);@Hour(timeNow);@Minute(timeNow);@Second(timeNow))
```

## Examples

1. This field value formula returns **01/21/96 7:30:45 AM** at
   7:30:45 A.M. on January 21, 1996.

   ```
   @Now
   ```
2. This agent displays the times on the two servers named Snapper
   and Tornado.

   ```
   @Prompt([Ok];
   "Server time";
   @Implode("Snapper" : "Tornado" + " " +
   @Text(@Now([ServerTime] : [LocalTimeOnError];
   "Snapper" : "Tornado")); @Char(13)))
   ```

---

## @OpenInNewWindow

# OpenInNewWindow @Command (Formula Language)

Opens the specified document within a new window.

Note: This @command is new with Release 8.

## Syntax

**@Command( [OpenInNewWindow])**

## Usage

A document must be selected in a view, folder, or calendar.

---

## @OptimizeMailAddress

# @OptimizeMailAddress (Formula Language)

Returns a mail address with all unnecessary domains removed.

## Syntax

**@OptimizeMailAddress(**  *address*  **)**

## Parameters

*address*

Text
or text list. The mail address to optimize.

## Return value

*optimizedAddress*

Text or text list.
The optimized address.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

All
domains between two duplicate domains, including the duplicate domain,
are removed.

## Examples

1. This example returns "username @ firstdomain @ thirdomain."

   ```
   @OptimizeMailAddress ("username @firstdomain @secondomain @firstdomain @thirdomain")
   ```
2. This example returns "username @ firstdomain @ secondomain."

   ```
   @OptimizeMailAddress ("username @firstdomain @firstdomain @secondomain")
   ```
3. This example returns "username @ firstdomain @ thirdomain" and
   "username @ firstdomain @ secondomain" in a list.

   ```
   @OptimizeMailAddress ("username @firstdomain @secondomain @firstdomain @thirdomain" :"username @firstdomain @firstdomain @secondomain")
   ```

---

## @OrgDir

# @OrgDir (Formula Language)

In a Service Provider (xSP) environment, returns the name
of the subdirectory for the company with which the currently authenticated
user is registered. Notes/Domino retrieves this information from the
organization's certifier document.

Note: This function is new with Release 6.

## Syntax

**@OrgDir**

## Return value

*subdirectory name*

String. The name
of the subdirectory containing the data directory for the company
with which the current user is registered.

## Usage

If
the currently authenticated user is not registered in a hosted organization,
is authenticated as an anonymous user, or if the function is invoked
in a non-xSP environment, Notes/Domino returns an empty string ("").

## Examples

If the full path name of the data
directory subdirectory for a hosted organization called Acme is C:\Notes\Data\Acme,
the following code opens a database on the same server that has the
same name as the current database, but that resides in the Acme organization's
subdirectory. @OrgDir returns "Acme" in the following formula.

```
@Command([FileOpenDatabase];@ServerName + ":" + @OrgDir + "\\" + @DbName[2])
```

---

## @Password

# @Password (Formula Language)

Encodes a string.

## Syntax

**@Password(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string that you want encoded.

## Return value

*encodedString*

Text or text list.
The encoded string.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

@Password
is especially useful in an input translation formula to protect a
user's password from being seen by others.

Note: There
is no way to decode the original string once it has been encoded by
@Password.

Note: Strings that begin with an open
parenthesis "(" are not encoded.

## Examples

1. This example returns (0449960361D30391DDA7747D537C32F8).

   ```
   @Password("chocolate")
   ```
2. This example returns (EFFF7C4218F3CBD6D7B509CD2E021DD8).

   ```
   @Password("vanilla")
   ```
3. This example returns (0449960361D30391DDA7747D537C32F8) and(EFFF7C4218F3CBD6D7B509CD2E021DD8)
   in a list.

   ```
   @Password("chocolate":"vanilla")
   ```

---

## @PasswordQuality

# @PasswordQuality (Formula Language)

Evaluates the return value of a Password data type field
with a number.

Note: This function is new with Release 5.0.1.

## Syntax

**@PasswordQuality(**  *field\_name*  **)**

## Parameters

*field\_name*

The
name of a field with a password data type. The password must be unencrypted.

## Return value

*passwordQuality*

Number. A rating
indicating the level of complexity of a password. A high number indicates
a complex password that is difficult to decipher.

## Usage

This
function is supported on the Web.

Note: Using the
@PasswordQuality function against a field which has been encrypted
using the @Password or @Hashpassword functions will yield incorrect
results, as the function will be applied to the encrypted version
of the input.

## Examples

The first two examples illustrate
the unexpected results returned when @PasswordQuality is applied against
an encrypted field. The second two examples illustrate the correct
way to use @PasswordQuality with password encryption, by placing the
password encryption in the document QuerySave event.

1. In this example, a programmer wanted to modify an existing form
   that tested for password quality in the input validation formula,
   so that the password would be encrypted. The programmer added an input
   translation formula using @Password. Now, a blank password, or a simple
   password such as "password" with a password quality of 3, returns
   @Success, which was not the programmer's intent.

   A Password type
   field, PW, is defined as follows. Input Translation formula: @Password(PW)
   Input Validation formula: @If(@PasswordQuality(PW)<6;@Failure("Password
   not complex enough"); @Success)
2. An agent selects all Person documents where @PasswordQuality(HttpPassword)<8.
   No documents are selected, because the Person document HttpPassword
   field is encrypted.
3. In this example, a form with a password field will be saved repeatedly.
   The QuerySave event encrypts an unencrypted password of sufficient
   complexity, but leaves a failed password or already encrypted password
   alone.

   A Password type field, PW, is defined as follows. Input Translation
   formula: None Input Validation formula: @If(@PasswordQuality(PW)>7;@Success;@Failure("Password
   not complex enough")) QuerySave event: FIELD PW:=@If((@PasswordQuality(PW)>7 &
   @PasswordQuality(PW)<30);@Password(PW);PW) If a password of "password"
   is entered in the PW field, the Input Validation fails, returning
   "Password not complex enough". If a password of "a2R5j4K9" is entered
   in the PW field, the document is saved with the encrypted value of
   that password in the PW field.
4. A Password type field, PW, is defined as follows. Input Translation
   formula: None Input Validation formula: @If(@PasswordQuality(PW)<12;@Failure("Password
   not complex enough"); @Success) QuerySave event: Dim doc As NotesDocument
   Set doc=Source.Document res=Evaluate(|@setfield("PW";@Password("PW"))|,doc)
   Call doc.save(True,True) If a password of "password" is entered in
   the PW field, the Input Validation fails, returning "Password not
   complex enough". If a password of "a2R5j4K9" is entered in the PW
   field, the document is saved with the encrypted value of that password
   in the PW field.

---

## @Pi

# @Pi (Formula Language)

Returns the constant value pi, accurate to fifteen decimal
places. The value pi is the ratio of the circumference of a circle
to its diameter.

## Syntax

**@Pi**

## Return value

*pi*

The
number 3.14159265358979.

## Examples

1. This formula returns the circumference of a circle with a radius
   that equals 5.

   ```
   2 * @Pi * 5
   ```
2. This formula converts an angle from degrees to radians. One degree
   equals pi/180 radians. Thus an angle of 360 degrees equals 2pi radians,
   180 degrees equals pi radians, and so on.

   ```
   ( angle * @Pi ) / 180
   ```
3. Given the latitude of a particular location, you can find a location's
   distance from the equator. The numeric field latitude holds the latitude
   in degrees. The numeric field distance computes the distance from
   the equator using this formula.

   First, latitude is converted to
   radians. Next, it's multiplied by 6440, the approximate radius of
   the earth in kilometers. This gives us the length of the arc from
   the equator to the given latitude.

   Notes/Domino treats an empty
   numeric field as a text field, so the formula uses @If to check for
   an empty latitude field.

   ```
   @If( latitude = ""; 0; ( ( latitude * @Pi ) / 180 ) * 6440 )
   ```

---

## @PickList

# @PickList (Formula Language)

Displays a modal window that contains either:

* A view you specify from which the user can select one or more
  documents. @PickList returns a column value from the selected document(s).
* A dialog box, displaying information from all available DominoÂ® Directories. The user
  can select one or more person, group, server, room, or resource names,
  and @PickList returns those names.

## Syntax

**@PickList(** **[CUSTOM]
: [SINGLE] ;**  *server*  **:**  *file*  **;**  *view*  **;**  *title*  **;**  *prompt*  **;**  *column*   ***;***   *categoryname*  **)**

**@PickList(
[NAME] : [SINGLE] [**; *selectedoptions* **])**

**@PickList(
[ROOM] )**

**@PickList( [RESOURCE] )**

**@PickList(
[FOLDERS] : [SINGLE] ;**  *server:database*  **)**

**@PickList(
[FOLDERS] : [SHARED]**  **;**  *server:database*  **)**

**@PickList(
[FOLDERS] : [PRIVATE]**  **;**  *server:database*  **)**

**@PickList(
[FOLDERS] : [NODESKTOP]**  **;**  *server:database*  **)**

## Parameters

**[CUSTOM]**

Keyword.
Indicates that you want to display a view in a dialog box.

**[NAME]**

Keyword.
Opens dialog box for selecting one or more names.

**[SINGLE]**

Keyword.
Optional. Limits the selection to a single document.

**[ROOM]**

Keyword.
Opens dialog box for selecting room.

**[RESOURCE]**

Keyword.
Opens dialog box for selecting resources.

**[FOLDERS]**

Keyword.
Returns a multi-select, text list of all folder names both in the
database and from the desktop. The following keywords can be combined
with [Folders]:

**[SINGLE]**

Keyword. Optional.
Limits selection to a single folder.

**[SHARED]**

Keyword.
Optional. Limits selection to only shared folders.

**[PRIVATE**]

Keyword.
Optional. Limits selection to only private folders (both in the database
and on the desktop).

**[SHARED]:[PRIVATE]**

Keyword.
Optional. Includes in selection all shared and private folders.

**[NODESKTOP]**

Keyword.
Optional. Excludes folders in the desktop from selection.

*selectedoptions*

Text
list. Optional. Pre-selects options.

*server*  ***:***  *file*

Text
list. The *server* is the name of the server where the database
is. The *file* is the path and file name of the database you
want to open. Specify the name and location of the database using
the appropriate format for the operating system.

You can use
a replica ID in place of a server and file name as the parameter following
the [custom] keyword only. The replica ID must be text and must include
the colon between the two sets of eight hex digits. For example:

```
@PickList([CUSTOM]; "852564A0:006B7872"; "By Category"; "Testing replica ID"; "Test prompt"; 3)
```

Use
"" to specify the currently open database.

*view*

Text.
The name of the view that you want to open in the database.

*title*

Text.
The window title for the dialog box.

*prompt*

Text.
The prompt that you want to appear inside the dialog box. Only one
line of text is displayed. Longer lines are truncated.

*column*

Number.
A number indicating which column value you want @PickList to return.
Use 1 to indicate the first column, 2 to indicate the second column,
and so on. Unlike @DbColumn and @DbLookup, @PickList counts all columns,
regardless of the types of formula they contain.

*categoryname*

Note: This parameter is new with Release 5.

Text.
Optional. Displays the specified category in the view. The view should
be categorized in order to use this parameter.

## Return value

*columnValue*

Text list. The value(s)
in the specified *column* for the document(s) that the user selected.

## Usage

This
function is useful in button, manual agent, paste agent, form action,
and view action formulas. It does not work in column, selection, mail
agent, scheduled agent, hide-when, window title, or form formulas.

Although
@PickList([CUSTOM]) operates similarly to @DbColumn and @DbLookup,
@PickList is preferable because it:

* Stores more data
* Performs the lookup faster
* Allows you to quickly locate the desired document by typing the
  first few characters

@PickList doesn't offer a NoCache option like @DbColumn and
@DbLookup because lookup results are never stored. Each time @PickList
is executed, a new lookup is performed.

For a calendar view,
@PickList displays two days starting with today, without time slots.
The user can click on the date picker button to navigate to other
days.

You cannot use this function in Web applications.

@PickList
can return no more than 64K bytes of data. Use the following equations
to determine how much of your data can be returned using @PickList.

For
lookups that return text:

2 + (2 \* number of entries returned)
+ total text size of all entries

For lookups that return numbers
or dates:

(10 \* number of entries returned) + 6

However,
@PickList can access a view of any size, so there are no limits to
the number of choices it can present. Only the return value is limited
in size.

## Examples

1. This formula displays the Products view of PROD.NSF in a dialog
   box. If the user selects a Staple remover and Stapler from the products
   view, the temporary variable choice gets assigned the following text
   list: **Staple remover; Stapler**

   ```
   choice:=@PickList( [CUSTOM] ; "" ; "Products" ; "Select a product" ; "Please select the products you want to order" ; 1 );
   ```
2. This formula achieves the same result as the preceding one, but uses
   @DbName to display the Products view of the current database.

   ```
   choice:=@PickList( [CUSTOM] ; @DbName ; "Products" ; "Select a product" ; "Please select the products you want to order" ; 1 );
   ```
3. This formula also displays the Products view of the current database,
   but returns the contents of the second column in the view.

   ```
   choice:=@PickList( [CUSTOM] ; @DbName ; "Products" ; "Select a product" ; "Please select the products you want to order" ; 2 );
   ```
4. This formula is the same as the preceding one but limits the selection to
   a single document.

   ```
   choice:=@PickList( [CUSTOM] : [SINGLE] ; @DbName ; "Products" ; "Select a product" ; "Please select the products you want to order" ; 2 );
   ```
5. This formula opens the By Category view of the current database
   and displays only the items in the Leather category.

   ```
   choice:=@PickList( [CUSTOM] ; "" ; "By Category" ; "Select a product" ; "Please select the products you want to order" ; 5;  "Leather");
   ```
6. This formula displays the Names dialog box. The names of the people,
   groups, or servers that the user selects are placed in the person
   field on the current document.

   ```
   FIELD person:=person;
   @SetField( "person"; @PickList( [NAME] ) )
   ```
7. This formula displays the Names dialog box, with the current value
   of myNames preselected. The names of the people the user selects are
   placed in the myNames field on the current document.

   ```
   FIELD myNames := @PickList([NAME]; myNames)
   ```

---

## @Platform

# @Platform (Formula Language)

Returns the name of the currently running platform version of Notes, Domino, or Nomad.

## Syntax

**@Platform( [S****PECIFIC****] )**

## Parameters

**[S****PECIFIC****]**

Keyword.
Optional. Returns more detailed information; for example, the version
number in addition to the name of the platform.

## Return value

*platform*

Text or text list. Without the **[S****PECIFIC****]** keyword, returns the name of the
platform.

* AIX/64
* Android
* iOS
* Linux/64
* Macintosh
* OS/400Â®
* UNIXâ¢
* Windows/32
* Windows/64
* WebBrowser

When you use the [Specific] keyword, @Platform returns a text list. For platforms other than
Nomad, the text list contains the following items:

* *PrimaryOSName*

  The detailed platform name.
* *PrimaryOSVersionNumber*

  The current version number of
  the primary operating system. The number is specific; for example,
  3.11. For the UNIXâ¢ platform,
  @Platform([SPECIFIC]) returns only the specific platform name, not
  the version number.
* *SecondaryOSName*

  The name of the secondary operating
  system. The values are the same as those for the primary operating
  system. Most platforms don't have a secondary operating system.
* *SecondaryOSVersionNum*

  The current version number of
  the secondary operating system.

**For Nomad on iOS, the text list contains the following items:**

* *PrimaryOSName*

  The detailed platform name. This will always
  return iOS.
* *PrimaryOSVersionNumber*

  The current iOS version number
* *Device Type* (V1.0.9+)

  Either Tablet or
  Phone
* *The resolution, in pixels, that Nomad is rendering
  at* (V1.0.9+)

  The smaller value comes first
  and this value does not change with orientation
  changes. For example: "567 x 1245".

**For Nomad on Android, the text list contains the following items:** 

* *PrimaryOSName*

  This will always return "Android".
* *PrimaryOSVersionNumber*

  The current Android OS version number, for example:
  "8.0.0".
* *DeviceType*

  Either "phone" or "tablet".
* *ScreenResolution*

  The resolution, in pixels, that Nomad is rendering at. The
  smaller value comes first and this value does not change with
  orientation changes. For example: "567 x 1245".

**For Nomad for web browsers, the text list contains the following items:** 

* *PrimaryOSName*

  This will always return "WebBrowser".
* *HostOSName*

  The name of the operating system that the browser is running on.
  This will always be "Macintosh", "Windows" or "Linux".
* *UserAgent*

  The full user agent from the browser.
* *ScreenResolution*

  The resolution, in pixels, that Nomad is rendering at. The
  first value is the width and the second value is the height. For
  example: "1397 x 775".

## Usage

When it is used in column, selection, or scheduled agent formulas, @Platform returns the current
platform where the database resides. If the database resides on a server, @Platform returns the
server platform; if the database resides locally, @Platform returns the workstation or mobile
platform.

Your application may perform certain operations that are not available in all platform versions
(such as the DDE-related functions). Rather than receive an error, you could use @Platform to
determine whether or not to perform the operation.

You can use @Platform([Specific]) to distinguish between Windows, Unix, and mobile device
platforms.

This
function returns the server platform only. Use @ClientType to distinguish
between Web and Notes/Domino users.

In Web applications, @Platform
returns the platform only.

---

## @PolicyIsFieldLocked

# @PolicyIsFieldLocked (Formula Language)

Indicates
whether a field is locked by an administration policy and cannot be
modified.

Note: This @function is new with Release
7.

## Syntax

**@PolicyIsFieldLocked(**  *fieldName*  **)**

## Parameters

*fieldName*

Text
or text list. The name(s) of the field(s) being queried.

## Return value

*flag*

Number.

* Returns 1 (True) if the field is locked. For a list, all the fields
  must be locked.
* Returns 0 (False) if the field, or any field in a list, is not
  locked.

## Usage

This
function is intended for use in hide-when and Input Enabled formulas.

This
function does not work in view column, view selection, or view action
formulas.

A policy lock is indicated by the presence in the
document of a computed field whose name is or begins with "$DPLocked"
and whose value is the name of the locked field.

A document
may contain any number of locked fields.

## Examples

This field hide-when formula hides
the field if it is locked by an administration policy.

```
@PolicyIsFieldLocked(@ThisName)
```

---

## @PostedCommand

# @PostedCommand (Formula Language)

Executes a Notes/Domino command. Most of the standard menu
commands can be executed using @PostedCommand. In addition, a number
of specialized commands are available. In a formula, any command invoked
using @PostedCommand executes after the rest of the formula has been
evaluated.

## Syntax

**@PostedCommand(
[**  *command*  **] ;**  *parameters*  **)**

## Paramters

**[**  *command*  **]**

See [@Commands](H_COMMANDS_LISTED.html) for a list of available
commands.

*parameters*

See the specific @Command
topic for details on parameters available for that command.

## Usage

This
function does not work in column, selection, hide-when, section editor,
window title, field, or form formulas, or in agents that run on a
server. It's intended for use in toolbar button, hotspot, and action
formulas.

---

## @Power

# @Power (Formula Language)

Raises a number to the power of an exponent.

## Syntax

**@Power(**  *base*  **;**  *exponent*  **)**

## Parameters

*base*

Number
or number list. The value that you want raised to *exponent.* May
be positive or negative.

*exponent*

Number or
number list. The power.

## Return value

*result*

Number or number list. The
value of *base* raised to the power of *exponent*.

## Usage

If
either parameter is a list, the function operates pair-wise on each
element of the list, and the return value is a list with the number
of elements in the larger list.

## Examples

1. This example returns 8 (2 raised to the power of 3, or 23).

   ```
   @Power(2;3)
   ```
2. This example returns -8 (-2 raised to the power of 3, or -23).

   ```
   @Power(-2;3)
   ```
3. This example returns 0.125 (2 raised to the power of -3, or 2-3).

   ```
   @Power(2;-3)
   ```
4. This example returns 8 and -8 in a list.

   ```
   @Power(2 : -2; 3)
   ```

---

## @Prompt

# @Prompt (Formula Language)

Displays a dialog box to the user and returns a text value
based on the user's actions in the dialog box. @Prompt is useful for
prompting a user for information and determining a course of action
based on the user's input.

## Summary of Dialog Box Styles

This table shows the different styles
of dialog boxes you can display. @Prompt accepts parameters and returns
a value based on the style you indicate.

| Style | Purpose | Contains | Return value |
| --- | --- | --- | --- |
| ChooseDatabase | Allows user to select a database | Controls and displays for browsing databases; Open, Select, Cancel, Browse, Help, and About buttons | Text list of 3 values. Server name, file name, and title of database. Returns null for server name if the database is local. |
| LocalBrowse | Allows user to select a file name from the local file system | Controls and displays for browsing local file system; Select, Cancel, and Network or Help buttons | Text. File name that user selected or entered. |
| Ok | Displays an informational message | Title and prompt; OK button | 1 (True). |
| OkCancelCombo | Allows user to select one value from a drop-down list of choices | Title and prompt; List of choices; OK and Cancel buttons | Text. Value that user selected. |
| OkCancelEdit | Allows user to type in text input | Title and prompt; Text box for input; OK and Cancel buttons | Text. Value that user entered. |
| OkCancelEditCombo | Allows user to select one value from a list of choices, or type in a different value | Title and prompt; List of choices with text box; OK and Cancel buttons | Text. Value that user selected or entered. |
| OkCancelList | Allows user to select one value from a list of choices | Title and prompt; List of choices; OK and Cancel buttons | Text. Value that user selected. |
| OkCancelListMult | Allows user to select multiple values from a list of choices | Title and prompt; List of choices; OK and Cancel buttons | Text list. All values that user selected. |
| Password | Allows user to enter password without displaying it on the screen | Title and prompt; Text box that accepts and hides user input; OK and Cancel buttons | Text. Password that user entered. |
| YesNo | Allows user to make a Yes/No decision | Title and prompt; Yes and No buttons | 1 (True, Yes) or 0 (False, No). |
| YesNoCancel | Allows user to make a Yes/No decision, or Cancel | Title and prompt; Yes, No, and Cancel buttons | 1 (True, Yes), 0 (False, No), or -1 (Cancel). |

## Syntax

**@Prompt(
[** *style* **] : [NoSort]** **;**  *title*  **;**  *prompt*  **;**  *defaultChoice*  **;**  *choiceList*  **;**  *filetype*  **)**

## Parameters

**[** *style* **]**

Keyword.
Required. Indicates the type of dialog box to display. May be any
of the following:

**[ChooseDatabase]**

**[LocalBrowse]**

**[Ok]**

**[OkCancelCombo]**

**[OkCancelEdit]**

**[OkCancelEditCombo]**

**[OkCancelList]**

**[OkCancelListMult]**

**[Password]**

**[YesNo]**

**[YesNoCancel]**

Include
the brackets ([ ]); these identify the style parameters as keywords.
If no [NoSort] keyword is provided, follow the style parameter with
a semicolon (**;**).

**[NoSort]**

Keyword.
Optional. Include this keyword if you want the members of *choiceList* to
appear in the exact order in which you enter them. If you omit this
keyword, the members of *choiceList* are sorted alphabetically.

*title*

Text.
The text you want displayed in the dialog box's title bar. Required
for all *style*s, although you can specify a null string with
"". The maximum number of characters you can include in a title is
65. Provide a null string with the [ChooseDatabase] keyword; you cannot
replace the default "Choose Database" title.

*prompt*

Text.
The text you want displayed within the dialog box. Required for all *style*s,
except LocalBrowse. If you use a formula for *prompt* and that
formula returns a list, only the first item in the list is displayed
as the *prompt*. To display the entire list, use @Implode. You
can specify a field name to display the contents of the field as the *prompt*,
but the field must be a text field. If it is a number or datetime
field, precede it with @Text. @NewLine cannot be used in *prompt*.
Use [@Char(13)](H_CHAR.html "Converts an HCL Code Page 850 code number into the corresponding single character string.") to insert a carriage
return. The maximum number of characters you can include in the text
that displays is 255. Provide a null string for the [ChooseDatabase]
keyword; you cannot display custom text in the Choose Database dialog
box.

*defaultChoice*

Text. The value that will
be used as the default value for the user's input. The input section
of the dialog box is primed with the value; the user can either accept
it by clicking OK or replace it with another value. Not applicable
to dialog boxes of *style* [Ok], [YesNo], [YesNoCancel], [LocalBrowse],
or [Password]. Required for all other *style*s. For [OkCancelListMult],
you can specify multiple default values as a text list "item1":"item2."

*choiceList*

Text
list. The values that you want displayed in the dialog box's list
box. The user can select one of these values as the input. Separate
the values with colons, as in: "PHONE.NSF":@MailDbName. Each value
in your list can be a text string, or an @function that returns a
text string. Required only with *style*s [OkCancelList], [OkCancelCombo],
[OkCancelEditCombo], and [OkCancelListMult].

*filetype*

Text.
A value that specifies the types of files to display initially: "1"
for NSF files only; "2" for NTF files only; "3" for files of all type.
Required only with style [LocalBrowse].

## Return value

*choice*

* If the user enters a value, returns the value as text or a text
  list.
* If the user selects Yes, returns 1 (True).
* If the user selects No, returns 0 (False).
* If the user selects Cancel, formula evaluation stops. The exception
  is [YesNoCancel], which returns -1 if the user selects Cancel.
* @Prompt([OkCancelEdit]) returns only the first 254 characters
  of the text entered.

## Usage

Use
@Prompt in field formula, toolbar button, manual agent, form action,
and view action formulas. This function does not work in column, selection,
mail agent, or scheduled agent formulas, and has limited usefulness
in window title and form formulas.

The title and prompt parameters
are scalar. If you enter a list, only the first element displays.
Use [@Implode](H_IMPLODE.html "Concatenates all members of a text list and returns a text string.") to convert the list
to a string.

You cannot use this function
in Web applications.

## Examples

1. [Ok] displays an informational message; the user clicks OK to
   close the dialog box. Use this style when you want to inform the user
   about something, without receiving anything back except an acknowledgement.

   ```
   @Prompt([Ok];"Reminder";"Don't forget to run backup tonight.")
   ```
2. This variation displays the contents of a multi-value item by
   imploding it.

   ```
   @Prompt([Ok];"Value of mylist"; @Implode(mylist))
   ```
3. [YesNo] displays a warning, and gives the user a chance to proceed
   or cancel the operation. If the user selects Yes the numeric value
   1 is returned. If the user selects No the numeric value 0 is returned.

   ```
   @Prompt([YesNo]; "Send memo?"; "This memo will be sent to everyone listed in the To, CC, and BCC fields.")
   ```
4. [YesNoCancel] also displays a warning, and gives the user a chance
   to select Yes, No, or Cancel. If the user selects Cancel, the value
   -1 is returned.

   ```
   result=@Prompt([YesNoCancel]; "Send memo?"; "This memo will be sent to everyone listed in the To, CC, and BCC fields" )
   ```
5. [OkCancelEdit] prompts the user to enter his or her name, which
   is returned as a text string. The name defaults to the current user's
   Notes/Domino user name, which is calculated using @UserName. If the
   user selects Cancel, Notes/Domino cancels the formula evaluation.
   Note that @Prompt([OkCancelEdit]) returns only the first 254 characters
   of the text entered.

   ```
   @Prompt([OkCancelEdit]; "Enter Your Name"; "Type your name in the box below."; @UserName)
   ```
6. [OkCancelList] displays a list box with database names (sorted
   alphabetically), prompts the user to select a database, and returns
   that database's name as a text string for use in a subsequent operation.
   If the user selects Cancel, Notes/Domino cancels the formula evaluation.

   The
   third option in the list is the current user's own mail database,
   the name of which is calculated with @MailDbName. The user must select
   one of the listed options; by default, Schedule is highlighted (the
   value listed as the default must also be included in the display list).

   ```
   @Prompt([OkCancelList]; "Select a Database"; "Select a database to open."; "Schedule"; "Schedule":"Phone Book":@Subset(@MailDbName;-1))
   ```
7. [OkCancelCombo] displays a dialog box similar to example 5, except
   that a drop-down list is used, so that initially only the default
   value is displayed. The user clicks the down arrow on the box to display
   the rest of the list. As in example 5, the user must select one of
   the listed values; by default, Schedule is selected. This function
   returns the user's selection. If the user selects Cancel, Notes/Domino
   cancels the formula evaluation.

   ```
   @Prompt([OkCancelCombo]; "Select a Database"; "Select a database to open."; "Schedule"; "Schedule":"Phone Book":@Subset(@MailDbName;-1))
   ```
8. [OkCancelEditCombo] is similar to example 6, except here the user
   can edit the text box and type in *any* database name; this way,
   the user is not limited to the selections in the list. This function
   returns the user's selection or entry. If the user selects Cancel,
   Notes/Domino cancels the formula evaluation.

   The default value must
   be included in the list, or the text box that displays initially will
   be blank.

   ```
   @Prompt([OkCancelEditCombo]; "Select a Database"; "Select a database to open, or type a database specification."; "Schedule"; "Schedule":"Phone Book": @Subset(@MailDbName;-1))
   ```
9. [OkCancelListMult] displays a list of names, from which the user
   can select one or more (Mary Tsen appears as the default selection).
   This function returns the user's selection(s). If the user selects
   Cancel, Notes/Domino cancels the formula evaluation.

   The default
   value must be included in the list.

   ```
   @Prompt([OkCancelListMult]; "Select a Name"; "Select one or more names as recipients for this request."; "Mary Tsen"; "Mary Tsen":"Bill Chu": "Michael Bowling":"Marian Woodward")
   ```
10. [Password] displays a dialog box where the user can enter a password.
    Notes/Domino does not display the password on the screen. This function
    returns the password.

    ```
    @Prompt([Password]; "Password"; "Enter the password for Approach database.")
    ```
11. [LocalBrowse] provides controls and displays that allow you to
    browse and select a name from the local file system. This example
    opens the Notes/Domino database file the user selects from the local
    browser. The "1" restricts the initial display to .nsf files.

    ```
    file := @Prompt([LocalBrowse]; "Select a database to open"; "1");
    @If(file = ""; @Return(1); "");
    @Command([FileOpenDatabase]; "" :@Left(file; " "))
    ```
12. This code, in a form hotspot button, displays the Choose Database
    dialog box then sets field values on the document (which must be in
    edit mode) based on the result.

    ```
    result := @Prompt([ChooseDatabase];"";"");
    FIELD Server := result[1];
    FIELD Filename := result[2];
    FIELD Title := result[3]
    ```

---

## @ProperCase

# @ProperCase (Formula Language)

Converts the words in a string to proper-name capitalization:
the first letter of each word becomes uppercase, all others become
lowercase.

## Syntax

**@ProperCase(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string you want to convert.

## Return value

*properString*

Text or text list. The *string,* converted
to proper-name capitalization.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

A
"word" is a consecutive set of characters with no spaces. Hyphenated
words are considered two words, as are words separated by any other
punctuation except an apostrophe.

## Examples

1. This example returns Every Child Loves Toys.

   ```
   @ProperCase("every CHILD LOves toys")
   ```
2. This example returns 3-Digit Code.

   ```
   @ProperCase("3-digit code")
   ```
3. This example returns Los Angeles if the string in the field named
   City contains the string los angeles, Los Angeles, LOS ANGELES, los
   Angeles, or any other variation.

   ```
   @ProperCase(City)
   ```
4. This example returns Robert and Smith in a list.

   ```
   @ProperCase("ROBERT" : "SMITH")
   ```

---

## @Random

# @Random (Formula Language)

Generates a random number between 0 and 1, inclusive.

## Syntax

**@Random**

## Usage

To
generate a random number between any two numbers x and y, use the
formula

```
(
 y 
- 
x 
)*@Random + 
x
```

## Examples

This formula generates a random number
between 7 and 22, inclusive. For example, it might return 13.

```
15 * @Random + 7
```

---

## @RefreshECL

# @RefreshECL (Formula Language)

Copies the administration execution control list from a
specified Address Book and name to your personal workstation ECL.

## Syntax

**@RefreshECL(** *server* **:** *database* **;** *name* **)**

## Parameters

*server* **:** *database*

Text
list. The server location and file name of the Address Book. Omit *server* or
specify it as "" (null) for the local Notes/Domino directory.

*name*

Text.
The name of the ECL. Specify "" (null) for the unnamed ECL.

## Usage

You
cannot use this function in Web applications.

## Examples

This formula refreshes your personal
workstation ECL from the administration ECL named "Developers" in
the Address Book on the server Marketing.

```
@RefreshECL("Marketing" : "names.nsf"; "Developers")
```

---

## @RegQueryValue

# @RegQueryValue (Formula Language)

Queries the Windowsâ¢ registry
for a specified value.

Note: This function is new in Release 5.0.2.

## Syntax

**@RegQueryValue(**  *keyName*   ***;***   *subKeyName*   ***;***   *valueName*  **)**

## Parameters

*keyName*

String.
HKEY\_CURRENT\_USER or HKEY\_LOCAL\_MACHINE. The registry key you want
to query.

*subKeyName*

String. The name of the
subkey under *keyName* that you want to query.

*valueName*

String.
The name of the registry value you want to find.

## Return value

*string*

The value associated with
the value name specified in the *valueName* parameter.

## Usage

@RegQueryValue
is intended for use on the Windowsâ¢ platform.
It returns an empty string on non-Windows platforms.

## Examples

This example obtains the current registered NotesÂ® executable directory in Windowsâ¢.

```
@RegQueryValue("HKEY_LOCAL_MACHINE"; "Software\\Lotus\\Notes\\5.0"; "Path")
```

---

## REM

# REM (Formula Language)

The REM reserved word allows you to add explanatory remarks
(comments) to a formula. Quotation marks or braces delimit the text
of the remark.

Note: Using braces to delimit a remark is new
with Release 6.

## Syntax

**REM
"** *comments* **" ;**

**REM {** *comments* **}
;**

## Usage

The
backslash ( \ ) serves as an escape character in a remark. To embed
quotation marks in a remark delimited by quotation marks, precede
each embedded quotation mark with a backslash. To embed a right brace
in a remark delimited by braces, precede each embedded right brace
with a backslash. To embed a backslash in a remark, type two backslashes.

A
compiled formula does not distinguish between quotation marks and
braces. When you open a design element containing formulas, braces
delimit all constants including those previously specified with quotation
marks. A backward slash prefixes a right brace previously specified
in a remark delimited by quotation marks.

If a comment doesn't
fit on one line, add additional REM statements to complete the comment.

## Examples

1. This formula contains five lines of comments before the code.

   ```
   REM "6/15/95";
   REM "The following formula calculates the date";
   REM "for the DueDate field";
   REM "DueDate is the Date field + thirty days";
   REM;
   @Adjust(Date; 0;0;30;0;0;0)
   ```
2. This formula contains five lines of comments before the code.

   ```
   REM {1/15/01};
   REM {The following formula calculates the date};
   REM {for the "DueDate" field};
   REM {"DueDate" is the Date field + thirty days};
   REM;
   @Adjust(Date; 0;0;30;0;0;0)
   ```

---

## @Repeat

# @Repeat (Formula Language)

Repeats a string a specified number of times.

**Syntax
@Repeat(**  *string*  **;**  *number*  **;**  *numberchars*  **)**

## Parameters

*string*

Text
or text list. The string you want to repeat.

*number*

Number.
The number of times you want to repeat *string.*

*numberchars*

Number.
Optional. The maximum number of characters you want returned. @Repeat
truncates the result to this number.

## Return value

*repeatedString*

Text or text list.
The *string,* repeated *number* times until *numberchars* (if
specified) is reached.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

The resultant string cannot be larger than 1,024
characters.

## Examples

1. This example returns HelloHelloHello.

   ```
   @Repeat("Hello";3)
   ```
2. This example returns ByeBy.

   ```
   @Repeat("Bye";2;5)
   ```
3. This example returns Great Month! Great Month! Great Month! in
   the Comments field if the amount in the field named Sales is greater
   than or equal to 100,000; otherwise it returns the string Good Month.

   ```
   FIELD Comments:=@If(Sales>=100000;@Repeat("Great Month!";3);"Good Month");
   ```
4. This example returns HelloHelloHello and ByeByeBye in a list.

   ```
   @Repeat("Hello" : "Bye"; 3)
   ```

---

## @Replace

# @Replace (Formula Language)

Performs a find-and-replace operation on a text list.

## Syntax

**@Replace(**  *sourcelist*  **;**  *fromlist*  **;**  *tolist*  **)**

## Parameters

*sourcelist*

Text
list. The list whose values you want to scan.

*fromlist*

Text
list. A list containing the values that you want to replace.

*tolist*

Text
list. A list containing the replacement values.

## Return value

*replacedList*

Text list. The *sourcelist,* with
any values from *fromlist* replaced by the corresponding value
in *tolist.* If none of the values in *fromlist* matched
the values in *sourcelist*, then *sourcelist* is returned
unaltered.

## Examples

With this example, both sourcelist
and fromlist contain "Orange", which is the first value in fromlist.
The first value in tolist replaces "Orange" in sourcelist. No other
matches were found, so the remainder of sourcelist is left intact;
the result is shown as follows:

```
@Replace("Red":"Orange":"Yellow":"Green";"Orange":"Blue";"Black":"Brown")
```

| sourcelist | fromlist | tolist | result |
| --- | --- | --- | --- |
| Red | Orange | Black | Red |
| Orange | Blue | Brown | Black (replaces "Orange") |
| Yellow |  |  | Yellow |
| Green |  |  | Green |

In this example, the formula looks at the Categories field
in each document that it runs against. If one of the keywords in a
document's Categories field is "To be assigned" then that keyword
is replaced with the name stored in that document's AssignedTo field.

```
FIELD Categories:= @Trim(@Replace(Categories;
"To be assigned"; AssignedTo));
```

You have a database
where you log service requests. Incoming requests are automatically
categorized as "To be assigned" by a mail/paste filter. Each day,
you review the new (unassigned) service requests, and assign them
to technicians by entering the appropriate name in the AssignedTo
field. Once a request has been assigned, you want it to appear under
that technician's name in the view, instead of under "To be assigned."

Rather
than manually categorizing each document a second time, you can write
a filter macro, like the preceding one, to delete the documents from
the "To be assigned" category and add them to the appropriate technician
categories.

---

## @ReplaceSubstring

# @ReplaceSubstring (Formula Language)

Replaces specific words or phrases in a string with new
words or phrases that you specify. Case sensitive.

## Syntax

**@ReplaceSubstring(**  *sourceList*  **;**  *fromList*  **;**  *toList*  **)**

## Parameters

*sourceList*

Text
or text list. The string whose contents you want to modify.

*fromList*

Text
or text list. A list containing the words or phrases that you want
to replace.

*toList*

Text or text list. A list
containing the replacement words or phrases.

## Return value

*newSourceList*

Text or text list.
The *sourceList*, with any values from *fromList* replaced
by the corresponding value in *toList.* If none of the values
in *fromList* matched the values in *sourceList*, then *sourceList* is
returned unaltered.

## Usage

If
more strings are specified in the *fromList* than the *toList*,
the extra strings in *fromList* are replaced with the last string
in *toList*. Extra strings in *toList* are ignored. If no
matches are found, @ReplaceSubstring returns the unmodified *sourceList*.

If
a list is specified for *fromList*, each subsequent list item
is scanned against the resulting *sourceList,* with prior list
item substitutions performed.

For example:

```
@ReplaceSubstring("first";"first":"second";"second":"third")
```

returns **third**.

First,
@ReplaceSubstring substitutes "second" for "first" from the first
list item in *fromList*. The resulting *sourceList* is now
"second." The function substitutes "third" for "second" from the second
list item in *fromList*.

Tip: Use @ReplaceSubString
to remove carriage returns from text by replacing them with " " or
"."

## Examples

1. This example returns "I hate apples".

   ```
   @ReplaceSubstring( "I like apples" ; "like" ; "hate" )
   ```
2. This example returns "I hate peaches".

   ```
   @ReplaceSubstring( "I like apples" ; "like" : "apples" ; "hate" : "peaches")
   ```
3. This example replaces all carriage returns in the Description
   field's text with blank spaces.

   ```
   @ReplaceSubString(Description;@Newline;" ")
   ```

---

## @ReplicaID

# @ReplicaID (Formula Language)

Returns the replica ID of the current database.

Note: This @function is new with Release 6.

## Syntax

**@ReplicaID**

## Return value

*title*

Text. The replica ID of the
current database.

## Usage

The
replica ID is a 16-character combination of letters and numbers that
identifies a NotesÂ® database.
Any databases with the same replica ID are replicas of one another.

## Examples

This agent mails the replica ID of
the current database to the current user.

```
@MailSend(@UserName; ""; ""; "Replica ID"; @ReplicaID)
```

---

## @Responses

# @Responses (Formula Language)

Returns the number of responses (in the current view) to
the document.

## Syntax

**@Responses**

## Return value

*numResponses*

Special text. The number
of responses to the document. Special text cannot be converted to
a number.

## Usage

Use
@Responses in window title formulas. This function does not work in
any other formula.

You cannot use this function in Web applications.

## Examples

1. This example returns **5** if there are five responses to the
   document.

   ```
   @Responses
   ```
2. This formula returns the string **No one has responded to this
   document** if there are no responses to the current document; otherwise
   a blank is returned.

   ```
   @If(@Responses=0; "No one has responded to this document"; " ")
   ```

---

## @Return

# @Return (Formula Language)

Immediately stops the execution of a formula and returns
the specified value. This is useful when you only want the remainder
of the formula to be executed only if certain conditions are True.

## Syntax

**@Return(**  *value*  **)**

## Parameters

*value*

The
value you want returned. You can specify another @function such as
@Error, or a text string such as "Formula stopped," or a Boolean value
(True or False). If you don't want anything returned, use the null
string ("").

## Return value

*result*

Returns *value.*

## Usage

@Return
is most useful in field formulas, agents that run formulas, and toolbar
buttons. Generally, you use it with @If to determine whether to perform
@Return or to perform one or more other statements.

@Return
should not be used in column formulas.

## Examples

1. This formula displays a dialog box offering the user a Yes/No
   choice. If the user selects Yes, the next document in the view is
   opened; if the user selects No, the formula stops and nothing more
   happens.

   ```
   @If(@Prompt([YesNo];"Continue?";"Do you want to continue reading your mail?");@Command([NavNext]);@Return(""))
   ```
2. This formula tests whether an environment variable called OrderNumber
   has been stored in the user's NOTES.INI or NotesÂ® Preferences file. If there is no such
   variable stored, @SetEnvironment is used to initialize it to zero.
   If a value has already been stored, @Return is used to return it and
   stop the formula from executing.

   ```
   @If(@Environment(OrderNumber)="";@SetEnvironment("OrderNumber";
   "0");@Return(@Environment("OrderNumber")))
   ```
3. The following code, when added to a field that displays the result
   of a database lookup, returns a customized error message if an error
   is encountered during that lookup. The temporary variable, "lookup,"
   retrieves the job title (located in column 3 of the "People" view)
   of the person listed in the first sorted column of the "People" view.
   If an error is encountered during the lookup, the field displays the
   specified error message in a dialog box and "1" displays in the field,
   indicating that there was an error encountered.

   ```
   lookup := @DbLookup("" : "" ; "serverName" : "fileDirectory\\databaseName.nsf"; "People" ; "Jackie Brown"; 3);
   @If(@IsError(lookup); @Return(@Prompt([OK];"Error";"Error locating the requested job title. Aborting lookup")); lookup)
   ```

---

## @Right

# @Right (Formula Language)

Returns the rightmost characters in the string. You can
specify the number of rightmost characters you want returned, or you
can indicate that you want all the characters following a specific
substring.

## Syntax

**@Right(**  *stringToSearch*  **;**  *numberOfChars*  **)** or **@Right(**  *stringToSearch*  **;** *subString*  **)**

## Parameters

*stringToSearch*

Text
or text list. The string whose rightmost characters you want to find.

*numberOfChars*

Number.
The number of characters to return. If the number is 2, the last two
characters of *stringToSearch* are returned; if the number is
5, the last five characters are returned, and so on.

*subString*

Text.
A substring of *stringToSearch.* @Right returns all of the characters
to the right of *subString.* It finds *subString* by searching *stringToSearch* from
left to right.

## Return value

*resultString*

Text or text list. The
rightmost characters in *stringToSearch*. The number of characters
returned is determined by either *numberOfChars* or *subString*.
@Right returns "" if *subString* is not found in *stringToSearch*.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Examples

1. This example returns "ace," the rightmost 3 characters in the
   string.

   ```
   @Right("Lennard Wallace";3)
   ```
2. This example returns "Wallace," which represents everything to
   the right of the first occurrence of the blank space.

   ```
   @Right("Lennard Wallace";" ")
   ```
3. This example returns "man" if the Author field contains "Timothy
   Altman."

   ```
   @Right(Author;3)
   ```
4. This example returns "Altman" if the Author field contains "Timothy
   Altman."

   ```
   @Right(Author;" ")
   ```
5. This example returns "ard" and "ace" in a list.

   ```
   @Right("Lennard" : "Wallace";3)
   ```

---

## @RightBack

# @RightBack (Formula Language)

Returns the rightmost characters in a string.

## Syntax

**@RightBack(**  *stringToSearch*  **;**  *numberOfChars*  **)**

**@RightBack(**  *stringToSearch*  **;**  *subString*  **)**

## Parameters

*stringToSearch*

Text
or text list. The string whose rightmost characters you want to find.

*numberOfChars*

Number.
Counting from left to right, the number of characters to skip. All
the characters following that number are returned.

*subString*

Text.
A substring of *stringToSearch.* @RightBack returns all the characters
following *subString*. It finds *subString* by
searching *stringToSearch* from right to left.

## Return value

*resultString*

Text or text list. The
rightmost characters in *stringToSearch*. The number of characters
returned is determined by either *numberOfChars* or *subString*.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Examples

1. This example returns "nard Wallace."

   ```
   @RightBack("Lennard Wallace";3)
   ```
2. This example returns a blank.

   ```
   @RightBack("Lennard Wallace";"")
   ```
3. This example returns "Wallace."

   ```
   @RightBack("Lennard Wallace";" ")
   ```
4. This example returns "othy Altman" if the name in the field named
   Author is Timothy Altman.

   ```
   @RightBack(Author;3)
   ```
5. This example returns lapalooza if the word in the show field is
   Lalapalooza.

   ```
   @RightBack(show;"La")
   ```
6. This example returns palooza if the word in the show field is
   lalapalooza.

   ```
   @RightBack(show;"la")
   ```

   Note: @RightBack returns the string to the right of the *last
   occurrence* of the substring you are searching for.
7. This example returns "nard" and "lace" in a list.

   ```
   @RightBack("Lennard" : "Wallace"; 3)
   ```

---

## @Round

# @Round (Formula Language)

Rounds the designated number to the nearest whole number;
if an additional number is specified, it is used as the rounding factor.

## Syntax

**@Round(**  *number*  **)
@Round(** *number*  **;** *factor* **)**

## Parameters

*number*

Number
or number list. Numbers to be rounded.

*factor*

Number.
Optional. The rounding factor to use. For example, if *factor* is
10, @Round rounds to the nearest number that is a factor of 10. If
you don't specify a *factor,* the *number* is rounded to
the nearest whole number.

## Return value

*roundedNumber*

Number. The value of *number,* rounded
to the specified *factor* or to the nearest whole number. If *number* is
a list, each number in the list is rounded to the specified *factor* or
to the nearest whole number.

## Usage

When
using this function with a number list, the list concatenation operator
takes precedence over any other operators.

For more information,
see "List concatenation operator."

## Examples

1. This example returns 2.

   ```
   @Round(2.499)
   ```
2. This example returns 3.

   ```
   @Round(2.5)
   ```
3. This example returns 2.

   ```
   @Round(1.5)
   ```
4. This example returns 12340 if the number in the field named NumberOfEmployees
   is 12338.

   ```
   @Round(NumberOfEmployees;10)
   ```
5. This example returns 1:3:3:4.

   ```
   @Round(1.333:2.897654:3.1:4)
   ```
6. This example returns 4510:45010:450010.

   ```
   @Round(4505:45005:450005;10)
   ```
7. This example returns 3.1430E+00 in a number field that has scientific
   formatting and is set to display four decimal places.

   ```
   @Round(3.142857; 0.001)
   ```

---

## @ScanBarcode

# @ScanBarcode (Formula Language)

If using HCL Nomad for iOS or Android, then @ScanBarcode will invoke the deviceâs
camera and allow the user to use their mobile device to scan a barcode.

Returns a scanned barcode.

Note: If used on platforms other than
Nomad Mobile, @ScanBarcode will return an empty text string.

## Syntax

*@ScanBarcode*

## Return value

*barcode* Text

The barcode scanned by the user using the device camera. This value is empty if the
user denies Nomad permission to use the device camera. This value is also empty if
the user cancels the barcode scanning process before a barcode is scanned.

---

## @Second

# @Second (Formula Language)

Extracts and returns the seconds value from the specified
time-date.

## Syntax

**@Second(**  *time-date*  **)**

## Parameters

*time-date*

Time-date
or time-date list. The value with the second that you want to extract.

## Return value

*seconds*

Number or number list. The
number of seconds in the second part of the time. Returns -1 if the
time-date provided contains only a date and not a time value.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 45.

   ```
   @Second([9:30:45])
   ```
2. This example returns 45 and 46.

   ```
   @Second([9:30:45] : [9:30:46])
   ```
3. This example returns 45 if the current time is 12:30:45 P.M.

   ```
   @Second(@Now)
   ```
4. This example returns 45 as a text string if the contents of the
   field named Date is any time-date value in which the number of seconds
   is 45.

   ```
   @Text(@Second(Date))
   ```

---

## SELECT

# SELECT (Formula Language)

The SELECT reserved word defines criteria for the selection
of documents in an agent that runs a formula, in a view, or during
replication. You use a SELECT statement before an expression to define
the set of documents that you want to change, see in a view, or replicate.

## Syntax

**SELECT**  *formula*   ***;***

## Usage

* In an agent, you can use the Agent Properties box to select the
  documents you want to act upon.
* In an agent that runs a formula, you can include a SELECT statement
  in the formula. The agent acts upon the documents selected with the
  Agent Properties box *and* the documents selected by the SELECT
  statement.
* In a view, you can use the Search Builder to select the documents
  you want to see in the view. You can use SELECT to select documents
  and provide more complicated conditions for replication.
* For selective replication, you can use the Search Builder to select
  the documents you want to replicate. You can use SELECT to select
  documents and provide more complicated conditions for replication.

Using SELECT in the formula eliminates the need to go through
the database to select the documents. You can run the filter macro
on all the documents in the database, and the SELECT statement performs
the selection process.

The word SELECT is automatically prepended
to the view selection formula when the formula is saved.

Use
SELECT @All to select all documents for an operation (for example,
use it in the selection formula for a view that displays all of the
database's documents). @All should never be used without the SELECT
reserved word. If your formula contains @All by itself, Notes/Domino
appends the SELECT @All statement to your formula:

```
@All;
SELECT @All;
```

If you compare a field to a value (for
example, Year > 1995) and the field is unavailable, the comparison
is false. However, you should check for fields that may not be present
with @IsUnavailable.

This reserved word does not work in column,
hide-when, section editor, window title, hotspot, field, form, or
form action formulas.

SELECT is not intended for use in toolbar
buttons.

## Examples

1. You want to change the contents of the Status field in several
   documents to Closed. However, you do not want to change the Status
   field of any document that contains the value Unsigned Contracts in
   the Categories field.

   To make the desired change, you write and
   run an agent that runs a formula. When you write the formula, you
   specify the documents that you want Notes/Domino to scan to make the
   change. By adding a SELECT statement to the formula, you can further
   limit the documents that Notes/Domino looks at when you run the agent.

   ```
   SELECT Categories != "Unsigned Contracts";
   FIELD Status := "Closed";
   ```
2. This replication formula limits replication to documents that
   contain a Year field whose value is greater than 1995.

   ```
   SELECT @IsAvailable(Year) & Year > 1995
   ```
3. This replication formula limits replication to documents that
   do not contain a Year field or whose Year field is greater than 1995.

   ```
   SELECT !@IsAvailable(Year) | Year > 1995
   ```

---

## @Select

# @Select (Formula Language)

Returns the value that appears in the number position.
If the number is greater than the number of values, @Select returns
the last value in the list. If the value in the number position is
a list, returns the entire list contained within the value.

## Syntax

**@Select(**  *number*  **;**  *values* **)**

## Parameters

*number*

Number.
The position of the value you want to retrieve.

*values*

Any
number of values, separated by semicolons. A value may be a number,
text, time-date, or a number list, text list, or time-date list.

## Examples

1. This example returns 3.

   ```
   @Select(3;1;2;3)
   ```
2. This example returns 3.

   ```
   @Select(5;1;2;3)
   ```
3. This example returns Apr;May;Jun.

   ```
   @Select(2;"Jan":"Feb":"Mar";"Apr":"May":"Jun";
   "Jul":"August":"Sep";"Oct":"Nov":"Dec")
   ```
4. This example returns San Diego;Sydney;New York;Amsterdam if the
   field named TrainingCenters contains these city names.

   ```
   @Select(3;SalesOffices;ServiceOffices;TrainingCenters)
   ```

---

## @ServerAccess

# @ServerAccess (Formula Language)

Checks if a specified user has a specified administrative
access level to a server.

Note: This @function is new with Release 6.

## Syntax

**@ServerAccess(
[**  *access*  **] ;**  *userName*  **;** *serverName* )

## Parameters

**[**  *access*  **]**

Keyword.
Supply one of the following keywords to represent the access level
you want to check for:

**[ACCESS]**

User has administrative
access to the server.

**[CREATEDATABASE]**

User
can create a database on the server.

**[CREATEREPLICA]**

User
can create a replica of a database on the server.

**[CREATETEMPLATE]**

User
can create a master template on the server.

**[DATABASEACCESS]**

User
has administrative access to the server, which enables him or her
to perform all the tasks that adminstrators with Access level access
can perform, except users with DatabaseAccess cannot issue remote
console commands.

**[FULLACCESS]**

User has full
administrative access to the server and is given manager access to
all databases hosted by the server, regardless of the database's ACL
settings.

**[REMOTEACCESS]**

User can issue remote
console commands to the server.

**[RESTRICTEDSYSTEMACCESS]**

User
can issue only those operating system commands that are listed as
Restricted System commands.

**[SYSTEMACCESS]**

User
can issue operating system commands to the server.

**[TRACKMESSAGE]**

User
can track email messages, but cannot view the contents of the Subject
field of mail memos.

**[TRACKMESSAGESUBJECT]**

User
can track email messages and can view the contents of the Subject
field of mail memos.

**[VIEWONLYACCESS]**

User
can issue a subset of remote console commands that supply information
about the server; they cannot execute remote commands that affect
the server's operation.

These access levels are set by the
server administrator on the Security tab of the CurrentÂ® Server Document in the Server settings
found on the Configuration tab of the DominoÂ® Administrator
client.

*userName*

Text; not case-sensitive.
Hierarchical name of the user whose access you want to check, enclosed
in quotation marks. If you supply a short name, this function returns
zero. You can use [@UserName](H_USERNAME.html "Returns the current user name.") to
supply the name of the current user to @ServerName.

*serverName*

Optional.
Text; not case-sensitive. Name of the server you want to test the
user's access level to, enclosed in quotation marks. If not provided,
tests the user's access to the server hosting the current database.
If the current database is Local, tests the user's access to the server
that is listed as the Administrative server in the database ACL for
the current database. If no Administrative server is set, returns
zero.

Note: This parameter is required when using
@ServerAccess in a toolbar button.

## Return value

*flag*

Boolean.

* 1 (True) indicates that the specified user has the specified access
* 0 (False) indicates that the specified user does not have the
  specified access

## Examples

1. This code, when added as the default value of a field in a database
   on the ocean/bay server, returns 1 if Luisa Albright is listed as
   having standard administrative access to the ocean/bay server in the
   server document for ocean/bay.

   ```
   @ServerAccess([ACCESS];"Luisa Albright/bay";"ocean/bay")
   ```
2. This code, when added as the default value of a field in a Local
   database that has ocean/bay selected as its Administrative server
   on the Advanced tab of the database's ACL dialog box, returns 1 if
   Luisa Albright has standard administrative access to the ocean/bay
   server.

   ```
   @ServerAccess([ACCESS];"Luisa Albright/bay")
   ```
3. This code, when added as the default value of a field in a database
   running on the ocean/bay server, returns 1 if the current user has
   full access to the ocean/bay server and all of its databases.

   ```
   @ServerAccess([FULLACCESS];@UserName)
   ```
4. This code, when added as the default value of a field, returns
   0 because it does not recognize the short user name.

   ```
   @ServerAccess([ACCESS];"Luisa Albright";"ocean/bay")
   ```
5. This code, when added as the default value of a field, returns
   0 if Luisa Albright does not have full access to the ocean/bay server
   and all of its databases.

   ```
   @ServerAccess([FULLACCESS];"Luisa Albright/bay";"ocean/bay")
   ```

---

## @ServerName

# @ServerName (Formula Language)

Returns the name of the server containing the current database.
When the database is local, returns the user name.

Note: This @function is new with Release 6.

## Syntax

**@ServerName**

## Return value

*serverName*

Text. The name of the
server containing the current database or the user name if triggered
from a local database.

## Examples

1. This formula, when added to a hotspot button on a form running
   on the acme/central server, displays a Server name message box that
   reads "CN=acme/O=central. "

   ```
   @Prompt([OK]; "Server name"; @ServerName)
   ```
2. This formula, when added to an action button on a form running
   on the acme/central server, displays a Server name message box that
   reads "acme."

   ```
   @Prompt([OK]; "Server name"; @Name([CN]; @ServerName))
   ```
3. When this code is added to a client toolbar button it displays
   "CN=Mary Anne Admin/O=central" if the button is triggered by Mary
   Anne while she is working with a form from a local database.

   ```
   @Prompt([OK]; "Server name"; @ServerName)
   ```

---

## @Set

# @Set (Formula Language)

Assigns a value to a temporary variable for use within
a formula.

## Syntax

**@Set(**  *variableName*  **;**  *value*  **)**

## Parameters

*variableName*

Text.
The name of a temporary variable.

*value*

Text,
number, time-date, time-date range, or list thereof. The value you
want to give to *variableName.*

## Usage

With
Release 6, you no longer need to declare the variable receiving the
assignment prior to setting its value with @Set. For R5 and earlier
clients, declare the variable by assigning it a null value at the
beginning of the formula:

```
TemporaryVariable:=""
```

## Examples

1. This formula determines whether the FirstName field is blank.
   If so, it sets the variable FullName to the concatenation of the Title
   field with the LastName field, as in "Ms. Tsen." If the FirstName
   field contains a value, the variable FullName is instead set to the
   concatenation of the FirstName with the LastName, as in "Mary Tsen."

   ```
   Full Name:="";
   @If(FirstName=""; @Set("FullName"; Title + " " + LastName); @Set("FullName"; FirstName + " " + LastName))
   ```
2. This example assigns FirstName and LastName to the first two elements
   of a list named FullName.

   ```
   FirstName := "Foo";
   LastName := "Bar";
   @Set("FullName"; FirstName : LastName)
   ```

---

## @SetDocField

# @SetDocField (Formula Language)

Given the unique ID of a document, sets the value of a
specific field on that document. The document must reside in the current
database.

## Syntax

**@SetDocField(**  *documentUNID*  **;**  *fieldName*  **;**  *newValue*  **)**

## Parameters

*documentUNID*

Text.
The unique ID of a document. [@DocumentUniqueID](H_DOCUMENTUNIQUEID.html "The universal ID, which uniquely identifies a document across all replicas of a database. In text format, the universal ID is a 32-character combination of hexadecimal digits (0-9, A-F).") specifies
the unique ID of the current document. To specify the unique ID of
the parent document, use $Ref as the first parameter. $Ref is a special
field on a response document that contains the unique ID of the parent
document.

*fieldName*

Text. The name of a field
on the document, enclosed in quotation marks. If you store the field
name in a variable, omit the quotation marks here.

*newValue*

Text,
number, time-date, time-date range, or list thereof. The value you
want to give to the field.

## Usage

This
function does not work in column or selection formulas. @SetDocField
is particularly useful in field, button, and agent formulas.

Note: Starting with Release 6, you can use @SetDocField to set
the value of a field in the current document, not just in other documents
in the same database.

## Examples

1. This formula, if placed on a button in a response form, changes
   the Subject of the parent document to "More people are commuting by
   bicycle." $Ref is a special field on a response document that contains
   the unique ID of the parent document.

   ```
   @SetDocField($Ref; "Subject"; "More people are commuting by bicycle")
   ```
2. This button formula changes the value of the name field in the
   current document to Joseph Riley:

   ```
   @SetDocField(@DocumentUniqueID; "name"; "Joseph Riley")
   ```
3. In a database, you want to update the parent Project document
   whenever its child Status document changes. Each Project document
   has one Status document. Specifically, you want to update the latestStatus
   field on the Project document so that it reflects the contents of
   the lastAction field on the child Status document.

   You write this
   input translation formula for the lastAction field on the Status form:

   ```
   @SetDocField($Ref; "latestStatus"; lastAction );
   lastAction
   ```
4. This button formula uses @DbLookup to retrieve the unique ID of
   a particular document. It then changes the value of the "employee
   title" field in that document to "sales associate."

   ```
   @SetDocField(@DbLookup(""; "Magnet":"Personnel.nsf"; "Staff"; "Joe Smith";
   "uniqueid"); "Employee Title"; "Sales Associate")
   ```
5. This button formula changes the value of the name field in the
   current document to a list containing Joseph and Riley:

   ```
   @SetDocField(@DocumentUniqueID; "name"; "Joseph" : "Riley")
   ```

---

## @SetEnvironment

# @SetEnvironment (Formula Language)

Sets an environment variable stored in the user's notes.ini file (Windowsâ¢ and UNIXâ¢) or NotesÂ® Preferences file (Macintosh).

## Syntax

**@SetEnvironment(**  *variableName* **;**  *value*  **)**

## Parameters

*variableName*

Text.
The name of the environment variable, enclosed in quotation marks.
If you enter a text list for the *variableName*, then every variable
named in that list receives the specified *value*. If you store
the field name in a variable, omit the quotation marks here.

*value*

Text.
The value you want to give to *variableName.* If you use a text
list for *value*, only the first value in the list is used; the
rest are ignored.

## Usage

Use
@SetEnvironment when you want to set an environment variable from
within another @function (such as @If or @Do). To set the environment
variable outside of an @function, use @Environment or the ENVIRONMENT
keyword.

@SetEnvironment cannot be used in column or selection
formulas. Some formulas, such as scheduled agents, are run on the
server instead of the user's workstation. In this case, the environment
variables affected are the *server's* environment variables,
not the workstation's.

To get the value of an environment variable,
use @Environment.

You cannot use this function in Web applications.
However, in Web applications, you can use predefined field names to
gather information about the Web user's environment by requesting
Common Gateway Interface (CGI) environment variables.

This
function prepends a dollar sign ($) to the variable name when it stores
the variable in the notes.ini (or NotesÂ® Preference)
file. Use the SetEnvironmentVar method of the LotusScriptÂ® NotesSession class or the
setEnvironmentVar method of the Javaâ¢ Session
class if you want to create a variable without the prepended dollar
sign.

## Examples

1. This example returns 5, if that is the value of the variable $IEVersonMajor
   stored in the current user's notes.ini or NotesÂ® Preferences file.

   ```
   @Environment("IEVersionMajor")
   ```
2. This example places a variable called OrderNumber in the current
   user's notes.ini or NotesÂ® Preferences
   file, and assigns it a value of zero.

   ```
   @Environment("OrderNumber";"0")
   ```
3. To save users time while completing Profile documents, you might
   want to automatically fill in an office location for them. You can
   create an editable text field called OfficeLocation. Its default formula
   is:

   ```
   @Environment("ENVOfficeLocation")
   ```

   Its
   input-translation formula is:

   ```
   @Environment("ENVOfficeLocation"; OfficeLocation);
   OfficeLocation
   ```

   The first time the user creates a Profile
   document, the OfficeLocation field is blank, so the user types in
   the office location. When the document is saved, the contents of the
   OfficeLocation field are saved in the notes.ini or NotesÂ® Preferences file. The next time the user
   creates a Profile document, the office location is retrieved from
   the environment variable ENVOfficeLocation, and the user doesn't have
   to type it in again (unless the office location changes, in which
   case the user edits the field).

   You could also write the input-translation
   formula using either @SetEnvironment or the ENVIRONMENT keyword, both
   of which achieve the same result:

   ```
   @SetEnvironment("ENVOfficeLocation"; OfficeLocation);
   OfficeLocation
   ```

   or

   ```
   ENVIRONMENT ENVOfficeLocation:= OfficeLocation;
   OfficeLocation
   ```
4. In addition to the OfficeLocation, you might want to use an environment
   variable to store a user's birthday. You can create an editable time
   field called Birthday. Its default formula is similar to the one used
   for OfficeLocation:

   ```
   @Environment("ENVBirthday")
   ```

   Its
   input-translation formula uses @Text to convert the time value into
   text:

   ```
   @SetEnvironment("ENVBirthday"; @Text(Birthday));
   Birthday
   ```

   Use @Text to write a similar input-translation
   formula for a number field.
5. You want to generate sequential numbers on a per user basis, and
   you want to store the number in a field called OrderNumber. Define
   the field OrderNumber to be a Text data type; it must be some form
   of computed field. You can then write the following formula for the
   field.

   ```
   Temporary := @Environment("OrderNumber");
   Temporary2 := @If(Temporary="";"0";Temporary);
   CurrentOrderNumber := @TextToNumber(Temporary2);
   NextOrderNumber := CurrentOrderNumber + 1;
   ENVIRONMENT OrderNumber := @Text(NextOrderNumber);
   @Text(CurrentOrderNumber);
   ```
6. This formula tests whether an environment variable called OrderNumber
   has been stored in the user's notes.ini or NotesÂ® Preferences file. If there is no such
   variable stored, @SetEnvironment initializes it to zero. If a value
   has already been stored, @Return returns it and stops the formula
   from executing.

   ```
   @If(@Environment(OrderNumber)=""; @SetEnvironment("OrderNumber";"0"); @Return(@Environment("OrderNumber")))
   ```
7. Two agents are used to look up a list of possible group names
   that users might belong to, prompt the user to select one, and then
   enter that name in the Group field for all selected documents (which,
   in this case, pertain to the current user).

   The **Set Group** agent
   looks up the list of group names stored in column 1 of the Service
   Requests - By Group view, prompts the user to select a group name,
   and then stores the selected name in the TmpName environment variable
   before running the "(Set Group Helper)" agent. The "(Set Group Helper)"
   agent then retrieves the group name from the user's notes.ini or NotesÂ® Preferences file and stores
   it in the Group name field for all selected documents.

   **Set
   Group** agent executes once:

   ```
   GroupList:=@DbColumn("":"NoCache";"";
   "Service Requests\\By Group";1);
   Group:=@Prompt([OKCancelEditCombo];"Choose a group";"Choose 
   	a group";"Marketing";GroupList);
   Tmp1:=@Environment("TmpName";Group);
   @Command([RunAgent];"(Set Group Helper)");
   ```

   **(Set
   Group Helper)** agent runs on each selected document:

   ```
   FIELD Group:=@Environment("TmpName");
   ```

---

## @SetField

# @SetField (Formula Language)

Assigns a value to a field stored within a document (use
@Set for temporary variables). This is similar to using the FIELD
keyword, except that @SetField can be used within another @function.
If the field does not exist, this command creates it and applies the
specified value to it.

## Syntax

**@SetField(**  *fieldName*  **;**  *value*  **)**

## Parameters

*fieldName*

The
name of the field whose value you want to set, enclosed in quotation
marks.

*value*

Text, number, time-date, time-date
range, or list thereof. The value you want to give to *fieldName.*

## Usage

This
function is most useful in agents, hotspot buttons, actions, and toolbar
buttons. It does not work in column, selection, hide-when, window
title, or form formulas.

With Release 6, you no longer need
to declare the field receiving the assignment prior to setting its
value with @SetField. For R5 or earlier clients, declare the field
at the beginning of the formula, as follows:

```
FIELD Fieldname:=Fieldname;
```

The
field that @SetField creates and assigns the specified value to if
the specified field does not exist in the document is not visible
to the user. You can remove a field added to a form this way using
the [@DeleteField](H_DELETEFIELD.html "Deletes the value of an editable field.") function.

The value can be anything and does not have
to match the type of the field as defined on a form. This function
does not reset flags on an existing field and does not set flags for
a newly stored field. For example, a Readers field does not become
plain text by assigning a text value; and a newly stored field cannot
be made a Readers field. The LotusScriptÂ® and Javaâ¢ classes allow this manipulation
through NotesItem and Item.

## Examples

1. This formula checks the value of the Priority field; if the Priority
   is Low or Medium, the Status field is set to Closed; otherwise, the
   Status is set to Open. Before @SetField is encountered in the formula,
   the Status field is declared using the FIELD keyword.

   ```
   FIELD Status:=Status;
   @If(Priority="Low"|Priority="Medium";@SetField("Status";"Closed");
   @SetField("Status";"Open"))
   ```
2. This code, when used in a view action button, deletes fields x\_1
   through x\_20 in the selected document.

   ```
   @For(i := 1; i <= 20; i := i + 1;
   @SetField("x_" + @Text(i);@DeleteField));
   ```
3. This button formula sets the value of the name field to a list
   containing Joseph and Riley:

   ```
   @SetField("name"; "Joseph" : "Riley")
   ```

---

## @SetHTTPHeader

# @SetHTTPHeader (Formula Language)

In a Web application, sets the value of HTTP headers in
the response being generated by the server for the browser client.

Note: This function is new with Release 6.

## Syntax

**@SetHTTPHeader(**  *responseHeader*  **;**  *value*  **)**

## Parameters

*responseHeader*

String.
The name of a response-header field, for example, "Content-Encoding,"
"Content-Length," or "Set-Cookie." See http:/www.w3.org/Protocols
for specifications of response headers. The following response headers
are read-only and cannot be set or overwritten using this function:

* Connection
* Content-Type
* Date
* Server

*value*

Text, number, or date. A value for
the field. Dates are converted to RFC 1123 format. An empty string
("") removes the header and its value from the HTTP response.

## Return value

*successOrFailure*

Number. @True, or
one (1), if the HTTP response header was successfully updated; @False,
or zero (0), otherwise.

## Usage

@SetHTTPHeader
is useful in formulas that run in the context of a browser; the NotesÂ® client always returns @False,
or zero (0), for this formula.

See [@GetHTTPHeader](H_GETHTTPHEADER.html "In a Web application, returns the value of an HTTP header from the browser client request being processed by the server.") for information
on getting a request header value.

## Examples

This form action sets the value of
the response-header field named "Set-Cookie" to "SHOP\_CART\_ID=4646."
As a result, the browser client registers a cookie for the server
using this name and value.

```
@SetHTTPHeader("Set-Cookie"; "SHOP_CART_ID=4646")
```

This
function appends the Set-Cookie response header to the end of the
following standard HTTP response:

```
HTTP/1.0 200 OK
Date: Thurs, 30 Aug 2001 16:17:52 GMT
Server: Domino/6.0
Content-type: text/html
Content-length: 1538
Last-modified: Mon, 27 Aug 2001 01:23:50 GMT
Set-Cookie: SHOP_CART_ID=4646
```

---

## @SetProfileField

# @SetProfileField (Formula Language)

Sets the value of a field in a profile document or creates
a profile document.

## Syntax

**@SetProfileField(**  *profilename*  **;**  *fieldname*  **;**  *value* **;**  *uniqueKey*  **)**

## Parameters

*profilename*

Text.
The name of the profile document that contains the field you want
to access. If no profile document exists by this name, Notes/Domino
creates one.

*fieldname*

Text. The name of the
field you want to access.

*value*

Text, number,
time-date, time-date range, or list thereof. The value to which you
want to set the field.

*uniqueKey*

Text. Optional.
A unique key that identifies the profile document.

## Return value

*value*

The value to which you set
the field.

## Usage

Use this function
to create a profile document in a Web application. The EditProfile
@command does not work on the Web. If no document exists with the
name specified as the first parameter of this function, Notes/Domino
creates a profile document with that name. Use @GetProfileField to
access data from the profile document.

The value can be anything and does not have
to match the type of the field as defined on a form. This function
does not reset flags on an existing field and does not set flags for
a newly stored field. For example, a Readers field does not become
plain text by assigning a text value; and a newly stored field cannot
be made a Readers field. The LotusScriptÂ® and Javaâ¢ classes allow this manipulation
through NotesItem and Item.

## Examples

1. This example sets the contents of the "Profile Categories" field
   of the "Interest Profile" document to the name of the current platform.

   ```
   @SetProfileField("Interest Profile";
   "ProfileCategories"; @Platform)
   ```
2. This example sets the contents of the "Profile Categories" field
   of the "Interest Profile" document for the current user to the name
   of the current platform.

   ```
   @SetProfileField("Interest Profile"; 
   "ProfileCategories"; @Platform; @UserName)
   ```
3. This code, when added to the Set Profile Field action button in
   a Web application, creates a profile document called webProfile, creates
   a fname field and sets the value of the fname field on the profile
   document equal to the value of the fname field on the current document.

   ```
   @SetProfileField("webProfile";"fname";fname)
   ```

---

## @SetTargetFrame

# @SetTargetFrame (Formula Language)

Allows you to specify a target frame when opening a view,
page, or frameset, or when composing or editing a document.

Note: This @function is new with Release 5.

## Syntax

**@SetTargetFrame(**  *targetframe*  **)**

## Parameters

*targetframe*

Text.
The name of the frame that a view, page, frameset, or document should
open into.

## Usage

Use
@SetTargetFrame before opening or refreshing the view, page, or frameset,
or before composing or editing a document. The following @commands
use the frame specified in the @SetTargetFrame:

* [@Command([Compose])](H_COMPOSE.html "Creates a new, blank document.")
* [@Command([EditDocument])](H_EDITDOCUMENT.html "Places the current document into the mode you specify. If you don't specify a mode, toggles between Read and Edit mode.")
* [@Command([OpenFrameset])](H_OPENFRAMESET_8100.html "Opens a frameset defined for the current database. Framesets provide a way for designers to display several pages at the same time. A frame is actually one page; a frameset is a collection of pages. Page designers can create links between frames. A major advantage of framesets is the ability to leave one page constant as users scroll or link to other pages.")
* [@Command([OpenPage])](H_OPENPAGE_1766.html "Opens a page defined for the current database. A page is a design element that structures and displays information, including text, graphics, applets, and links. Unlike a form, a page cannot contain fields, subforms, layout regions, and some embedded controls.")
* [@Command([OpenView])](H_OPENVIEW.html "Opens the specified view in the current database.")
* [@Command([RefreshFrame])](H_REFRESHFRAME_COMMAND.html "Refreshes the specified frame in a frameset.")

If you specify the *newinstance* parameter for @Command([OpenView]),
the @SetTargetFrame function is ignored.

If you do not specify
a *viewName* for@Command([OpenView]), then the last view
is the one that opens in the specified *targetframe* of @SetTargetFrame.

If
you specify a *targetFrame* parameter for @Command([RefreshFrame]),
the @SetTargetFrame function is ignored.

@SetTargetFrame can
be used in action and hotspot formulas.

## Examples

Consider 2 framesets -- one that contains
"Frame A" and "Frame B" and another frameset nested within "Frame
B" that contains "Frame C" and "Frame D."

This example opens the
view "My View" in "Frame A" of the first frameset.

```
@SetTargetFrame("Frame A");
@Command([OpenView]; "My View");
```

This
example is code in a button on "Frame C" of the nested frameset. It
opens the form "My form" in "Frame D" of the same frameset:

```
@SetTargetFrame("Frame D");
@Command([Compose]; "My form");
```

---

## @SetViewInfo

# @SetViewInfo (Formula Language)

In Standard Outline views, filters a view to display only
documents from a specified category. In Calendar views, filters a
view to display only documents that contain a specified string in
a specified column.

Note: This @function is
new with Release 6.

## Syntax

**@SetViewInfo(
[SETVIEWFILTER] ;**  *filterString*  **;**  *columnName*  **;**  *isCategory*  **[;**  *exactMatch* **]
)**

## Parameters

**[SETVIEWFILTER]**

Keyword.
Required. Indicates you want to qualify the documents that display
in a view.

*filterString*

Text. Serves as the
key to determine which documents display in a view. If this string
is present in the column specified in *columnName*, includes
the document in the view.

*columnName*

Text.
The programmatic name of a column. The column specified here must
contain the *filterString* for the document to display in the
view.

Note: To clear the view filter, specify ""
for *filterString*.

*isCategory*

Number.
Boolean value. Required in a Standard Outline view; not for use in
Calendar views. 1 indicates that the column in the *columnName* value
is a category. 0 indicates that it is not. Set to 0 if using the *exactMatch* parameter
for a Calendar view.

*exactMatch*

Number.
Boolean value. Optional in a Calendar view; not for use in Standard
Outline views. 1 indicates that the string in the *columnName* column
must exactly match the string specified in *filterString*. 0
indicates that the *filterString* does not have to match exactly.
For instance, if the *filterString* is "A," and *exactMatch* is
set to 0, documents with "A" and "A plus" in the column specified
in *columnName* will both be included in the view.

## Usage

This
@function is useful if you want to filter the documents in a view
to display only a subgroup that contains specific data.

## Examples

1. This formula, when added to a hotspot button in a form, opens
   the Customers Standard Outline view, which is categorized by companyName.
   The view contains documents for people from several companies, but
   filters the view to display only those documents for individuals who
   work at the Acme Corp.

   ```
   @Command([OpenView];"Customers");
   @SetViewInfo([SETVIEWFILTER];"Acme Corp.";"companyName";1)
   ```
2. This code, when added to a Sort action button in a Standard Outline
   view, filters the contents of the current view to display only those
   documents that have empolyeeName fields that contain the current user's
   name. The view is categorized by employeeName.

   ```
   @SetViewInfo([SETVIEWFILTER];@Name([CN];@UserName);"employeeName";1)
   ```
3. This code, used in the View by Room action button in the Reservations
   template (resrc60.ntf), updates the Calendar view to display only
   calendar entries that specify as their resourceName value the resource
   chosen by the user from a pick list. $20 is the programmatic name
   of the Resource column, whose value is determined by the resourceName
   field.

   ```
   choice:=@PickList([CUSTOM] : [SINGLE]; @DbName; "Resources";"View by Room or Resource";"Select the room or resource whose calendar you want to see:"; 1);
   @SetViewInfo([SETVIEWFILTER];choice;"$20";0)
   ```
4. This code, in an agent run from your Calendar view, will show
   only your Meetings.

   ```
   @SetViewInfo([SETVIEWFILTER];"3";"$152";0;1)
   ```

---

## @ShowParentPreview

# @ShowParentPreview (Formula Language)

Displays the parent document preview pane.

## Syntax

**@ShowParentPreview**

## Usage

A
document must be open in Read or Edit mode.

This command does
not work on the Web.

## Examples

1. This formula, placed in a button on a document, displays the parent
   preview pane when the button is clicked. If the current document is
   not a response, the preview pane will display the text, "Document
   is not a response."

   ```
   @ShowParentPreview
   ```
2. When a button on a document containing this formula is clicked,
   it displays the parent of the current document in the preview pane
   if the current document is a response document. If the current document
   is not a response document, the preview pane will not display, and
   an informational box pops up informing the user that parent preview
   only works for response documents.

   ```
   @If(@IsResponseDoc;@ShowParentPreview;
   @Prompt([Ok];"Not a response document";
   "Parent preview only works for response documents."))
   ```

---

## @Sign

# @Sign (Formula Language)

Indicates whether a number is positive, negative, or zero.

## Syntax

**@Sign(**  *signedNumber*  **)**

## Parameters

*signedNumber*

Number
or number list. The number whose sign you want to determine.

## Return value

*sign*

Number or number list. May be
any of the following values:

* The signed numberis negative, - 1
* The signed number is zero, 0
* The signed numberis positive, 1

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This formula sets the result field to "Profit!" if the earnings
   field is greater than the expenses field, "Loss!" if expenses are
   greater than earnings, and "Break even" if they are equal.

   ```
   field result:=result;
   difference:=earnings - expenses;
   r:=@If( ( @Sign( difference ) = 1); "Profit!"; ( @Sign( difference ) = -1 ); "Loss!"; "Break even" ); @SetField( "result"; r )
   ```
2. This formula allows division by a positive number but returns
   -1 if the divisor is negative and 0 if the divisor is 0. If number1
   and number2 are lists, the result is a list.

   ```
   @If(@Sign(number2) = 1; number1 / number2; @Sign(number2) = -1; -1; 0)
   ```

---

## @Sin

# @Sin (Formula Language)

Given
an angle in radians, returns the sine of the angle. In a right triangle,
the sine of an acute angle is the ratio of the length of its opposite
side to the length of the hypotenuse.

## Syntax

**@Sin(**  *angle*  **)**

## Parameters

*angle*

Number
or number list. An angle expressed in radians.

## Return value

*sine*

Number
or number list. The sine of *angle*, to 15 decimal places.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This formula returns 1, the sine of the angle Pi/2 (90 degrees).

   ```
   @Sin( @Pi/2 )
   ```
2. You have a triangle ABC. You know the value of angle A in radians,
   and the lengths of sides a and b. This formula finds angle B, in radians.
   This formula is a version of the law of sines, which states that for
   any triangle ABC, (sin A / a) = (sin B / b) = (sin C / c).

   ```
   @ASin( ( sideB *( @Sin( angleA ) ) ) / sideA )
   ```
3. This formula returns 1 and -1 in a list.

   ```
   @Sin((@Pi / 2) : (@Pi / -2))
   ```

---

## @Sort

# @Sort (Formula Language)

Sorts a list.

Note: This @function is new with Release 6.

## Syntax

**@Sort(**  *list* **;** **[**  *order*  **];**  *customSortExpression*  **)**

## Parameters

*list*

Text,
number, or time-date list. The values to be sorted. Any alternate
data types are returned unchanged.

**[**  *order*  **]**

Keyword.
Optional. You can use the following keywords to specify the order
of the sort:

**[ACCENTSENSITIVE]**

**[ACCENTINSENSITIVE]**

**[ASCENDING]**

**[CASESENSITIVE]**

**[CASEINSENSITIVE]**

**[CUSTOMSORT]**

**[DESCENDING]**

**[PITCHSENSITIVE]**

**[PITCHINSENSITIVE]**

Separate
multiple order keywords with a colon(:). By default, the following
keywords automatically format the sort order:

```
[ASCENDING]:[CASESENSITIVE]:[ACCENTSENSITIVE]:[PITCHSENSITIVE].
```

You
can override a default sort order keyword by specifying its opposite
keyword. For example, to override [ASCENDING], specify [DESCENDING]
in the @Sort function. If conflicting keywords are passed, the last
one in the list affects the sort order.

*customSortExpression*

Formula.
Required when the [CUSTOMSORT] keyword is specified. A formula that
uses the temporary variables $A and $B to compare the values of elements
in the list two at a time. Return @True or a number greater than 0
to specify that $A is greater than $B. Return @False or a number less
than or equal to 0 to specify that $B is greater than $A.

An
error is produced if the return value is a data type other than a
number.

## Return value

*list*

Text,
number, or time-date list. The sorted values.

## Usage

The
ascending, case-, and accent-sensitive sort sequence for the English
character set is as follows: the numbers 0-9, the alphabetic characters
aA-zZ, the apostrophe, the dash, and the remaining special characters.
Pitch-sensitivity affects double-byte languages.

Note: The
case sensitive sort only matters when terms are identical except for
case. In that instance, the lower case is sorted first. For example,
cat, Cat, CAT. If terms are not identical except for case, they are
sorted without regard to case.

If you set Unicode standard sorting as
the sorting option, you cannot select the following keywords or combinations:

* **[PITCHINSENSITIVE]**
* **[CASESENSITIVE]:[ACCENTINSENSITIVE]**

You specify Unicode standard sorting by setting the notes.ini
variable $CollationType to @UCA, or by selecting the "Unicode standard
sorting" checkbox that displays in the following dialog boxes:

* Sorting dialog box that displays when you choose File - Preferences
  - User Preferences - International - Sorting from the main menu
* Database Properties box\*
* Design Document Properties box\*

\*The Unicode option is disabled in the Database and Design
Document Properties boxes until you select a default sort order.

For
more information on Unicode sorting, see http://oss.software.ibm.com/icu/

A
date-time value with a wildcard time (no time specified) equals all
date-time values for the same date. For example, the following dates
are considered equal:

[12/12/2000] : [12/12/2000 1:00 PM] :
[12/11/2000 - 12/13/2000]

These values are sorted in random
order and may be ordered differently with each sort if multiple sorts
are performed on them.

## Examples

1. This formula returns: Albany, New Boston, new york, San Francisco.

   ```
   @Sort(@ThisValue)
   ```
2. Same as preceding.

   ```
   @Sort(@ThisValue; [ASCENDING])
   ```
3. This formula returns: San Francisco, New Boston, new york, and
   Albany.

   ```
   @Sort(@ThisValue; [DESCENDING])
   ```
4. This formula returns: Albany, New Boston, New York, San Francisco.

   ```
   @Sort(@ProperCase(@ThisValue); [ASCENDING])
   ```

These examples are used as the default values for form fields.

1. This formula returns 1009;85;79 if the Price column (the 5th column
   in the Gear view) contains the prices 79, 85, and 1009 for three entries
   in the Ski Pants category:

   ```
   @Sort(@DbLookup("";"Server/Name/Notes":"Ski\\Clothing.nsf";"Gear";"Ski Pants";5);[DESCENDING])
   ```
2. This formula returns the contents of the movies field in order
   from the shortest title to the longest; it returns ET;casablanca;The
   Great Escape when the movies field contains "casablanca":"The Great
   Escape":"ET":

   ```
   @Sort(movies;[CASESENSITIVE]:[CUSTOMSORT];@If(@Length($A) < @Length($B);@False;@Length($A) > @Length($B);@True;@False))
   ```

   Note
   that the custom sort keyword overrides the case-sensitivity keyword.
3. This formula returns the following passwords in order from the
   strongest to the weakest: HE5ll+o;Hel$lo;hello, when the pswd1 field
   contains "Hello", pswd2 field contains "HE5ll+o", and the pswd3 field
   contains "Hel$lo."

   ```
   @Sort(pswd1:pswd2:pswd3;[CUSTOMSORT];@If(@PasswordQuality($A) < @PasswordQuality($B);@True;@PasswordQuality($A) > @PasswordQuality($B);@False;@False))
   ```
4. This formula returns: cat, Cat, CAT, dog when the animals field
   contains "CAT, Cat, dog, cat".

   ```
   @Sort(animals;[CASESENSITIVE])
   ```

---

## @Soundex

# @Soundex (Formula Language)

Returns the Soundex (the NotesÂ® phonetic
speller) code for the specified string.

## Syntax

**@Soundex(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string whose Soundex code you want.

## Return value

*code*

Text or text list. The Soundex
code. You cannot convert it to any other data type.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

This
function is used almost exclusively by the DominoÂ® Directory. You will rarely use this
function.

## Examples

1. This example returns F430.

   ```
   @Soundex("field")
   ```
2. This example returns P430.

   ```
   @Soundex("phield")
   ```
3. This example returns F430 and P430 in a list.

   ```
   @Soundex("field" : "phield")
   ```

---

## @Sqrt

# @Sqrt (Formula Language)

Given a number, returns its positive square root.

## Syntax

**@Sqrt(**  *number*  **)**

## Parameters

*number*

Number
or number list. The number whose square root you want to find. The *number* must
be positive, otherwise @Sqrt returns an error.

## Return value

*Nu
number*

Number or number list. The square root of the number.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This formula returns 4.

   ```
   @Sqrt( 16 )
   ```
2. This formula returns 1, 2, 3, 4, and 5 in a list.

   ```
   @Sqrt(1 : 4 : 9 : 16 : 25)
   ```

---

## @StatusBar

# @StatusBar (Formula Language)

Writes a message or messages to the status bar.

Note: This @function is new with Release 6.

## Syntax

**@StatusBar**( *statusBarText* )

## Return value

*statusBarText*

Text or text list.
The text of the status bar message. A list produces one message per
element.

## Usage

This
@function works only in the NotesÂ® client.

## Examples

1. This onLoad/Postopen event writes a message to the status bar.

   ```
   @StatusBar("Loaded \"Form A\" in \"" + @Subset(@DbName; -1) + "\"")
   ```
2. This onLoad/Postopen event writes two messages to the status bar.

   ```
   @StatusBar("Loaded \"Form A\"" :
   ("Database is \"" + @Subset(@DbName; -1) + "\""))
   ```

---

## @Subset

# @Subset (Formula Language)

Searches a list from beginning to end and returns the number
values you specify. If you specify a negative number, @Subset searches
the list from end to beginning, but the result is ordered as from
the beginning of the list.

## Syntax

**@Subset(**  *list*  **;**  *number*  **)**

## Parameters

*list*

Text
list, number list, time-date list, or time-date range list. The list
whose subset you want.

*number*

Number. The
number of values from *list* that you want. Specifying zero (0)
returns the error, "The second argument to @Subset must not be zero."

## Return value

*subsetList*

Text list, number list,
or time-date list. The *list,* containing the *number* of
values you specified.

## Examples

1. This example returns New Orleans;London.

   ```
   @Subset("New Orleans":"London":"Frankfurt":"Tokyo";2)
   ```
2. This example returns London;Frankfurt;Tokyo.

   ```
   @Subset("New Orleans":"London":"Frankfurt":"Tokyo";-3)
   ```
3. This example returns New Orleans;London;Frankfurt if the field
   named BranchOffices is made up of the list "New Orleans" : "London"
   : "Frankfurt" : "Tokyo" : "Singapore" : "Sydney."

   ```
   @Subset(BranchOffices;3)
   ```

---

## @Success

# @Success (Formula Language)

Returns 1 (True). Use this function with @If in field validation
formulas to indicate that the value entered satisfies the validation
criteria.

## Syntax

**@Success**

## Return value

*true*

Number. The number 1, meaning
True.

## Usage

Use
@Success in input validation formulas for editable fields.

## Examples

This example returns 1 and allows
the document to be saved when the value in the field Price is less
than 100. This indicates that acceptable data was entered when used
in an input validation formula.

```
@If(Price<100;@Success;@Failure("Price too large"))
```

---

## @Sum

# @Sum (Formula Language)

Adds a set of numbers or number lists.

## Syntax

**@Sum(**  *numbers*  **)**

## Parameters

*numbers*

Numbers
or number lists. As many numbers or number lists as you want to sum.

## Return value

*result*

Number.
The sum of all the *numbers*, including members of number lists.

## Usage

Make
sure the fields you send as parameters contain a number value -- Notes/Domino
interprets empty number fields as the null string.

Since list
concatenation has the highest precedence, list elements that are expressions
must be in parentheses if the expression applies only to that element.
For example, write @Sum(1:2:(-3):4), not @Sum(1:2:-3:4), if 3 is negative
and 4 is not.

## Examples

1. This example returns 3.

   ```
   @Sum( 1 : 2 )
   ```
2. This example returns 11.

   ```
   @Sum( (-1) : 2 ; (-10) : 20 )
   ```
3. This example sets the Total field to 50 if numPersons is a number
   field containing 5; 10; 15; 20.

   ```
   @SetField("Total";@Sum(numPersons))
   ```
4. This example looks at the Transactions view in the current database,
   whose first column contains number values indicating the amount of
   a transaction. The formula sums the transactions and places the total
   in the result field on the current document.

   ```
   FIELD result:=result;
   r:=@DbColumn("":""; ""; "Transactions"; 1 );
   @SetField( "result"; @Sum( r ) )
   ```
5. This example displays a view in a dialog box. The first column
   in the view contains a product name, the second contains its price.
   After the user selects one or more products in the dialog box, the
   formula displays the total cost of the selected items.

   ```
   amounts:=@PickList( [Custom]; @DbName ; "Products"; "Choose products"; "Please select the products you want to order"; 2 );
   total:=@Sum( @TextToNumber( amounts ) );
   @Prompt([Ok]; "Total"; "The total cost of these products is " + @Text(total))
   ```

---

## @Tan

# @Tan (Formula Language)

Given an angle in radians, returns the tangent of the angle.
In a right triangle the tangent of an acute angle is the ratio of
the length of the opposite side to the length of the adjacent side.

## Syntax

**@Tan(**  *angle*  **)**

## Parameters

*angle*

Number
or number list. Any angle, expressed in radians.

## Return value

*tangent*

Number
or number list. The tangent of *angle.*

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 1 (approximately).

   ```
   @Tan( @Pi/4 )
   ```
2. This example returns 1 and 1.7 (approximately) in a list.

   ```
   @Tan( (@Pi/4) : (@Pi/3) )
   ```

---

## @TemplateVersion

# @TemplateVersion (Formula Language)

Returns the version number of the master template upon which the database design is based.

Note: This @function is new with Release 6.5.

## Syntax

**@TemplateVersion**

## Return value

*versionNumber*

Text. The version number, "6.0.3" as an example.

## Usage

This @function applies when "Inherit design from master template" is checked on the Design tab of the Database Properties. The template version number can be seen there.

You cannot use this function in Web applications.

---

## @Text

# @Text (Formula Language)

Converts any value to a text string.

## Syntax

**@Text(**  *value*  **;**  *format-string*  **)**

## Parameters

*value*

Number,
time-date, text, list thereof, or rich text. The value you want to
convert to text.

Note: Conversion of rich text is
new with Release 6.

*format-string*

Text or
text list. Optional. Up to four format-strings (see table that follows).
These determine how the text is returned. If the *value* is already
a text data type, the *format-string* is ignored.

## Return value

*textValue*

Text
or text list. The *value* you specified, converted to text. If
you used any *format-strings,* they are applied.

## @Text with time-date components

There are four separate categories
of time-date, format-string components. You can include up to four
components, but only one from each category.

| Symbol | Meaning |
| --- | --- |
| D0 | Month, day and year |
| D1 | Month and day, year if it is not the current year |
| D2 | Month and day |
| D3 | Month and year |
| T0 | Hour, minute, and second |
| T1 | Hour and minute |
| Z0 | Always convert time to this zone |
| Z1 | Display zone only when it is not this zone |
| Z2 | Display zone always |
| S0 | Date only |
| S1 | Time only |
| S2 | Date and time |
| S3 | Date, time, Today, or Yesterday |
| Sx | Use when you cannot predict the exact format of the value being passed, but you know that it is either a time, a date, or both. |

## @Text with number values

For number values, compose a format-string
by combining any of the following components into a string.

| Symbol | Meaning |
| --- | --- |
| G | General format (significant digits only) |
| F | Fixed format (set number of decimal places) |
| S | Scientific format (E notation) |
| C | Currency format (two decimal places) |
| , | Punctuated at thousands (using U.S. format) |
| % | Percentage format |
| () | Parentheses around negative numbers |
| *number* | Number of digits of precision |

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

Once
a number value is converted to text, you will not be able to use the
number for arithmetic calculations.

Rich text conversion loses
attachments and all formatting except tabs and spaces. When rich text
is converted in a document, the document must be saved before the
conversion becomes visible.

Rich
text conversion does not work in column formulas. Use [@Abstract](H_ABSTRACT.html "Abbreviates the contents of one or more fields by:") to convert the contents of
a rich text field to plain text. Then reference the plain text field
in the view. For example, if you add the following code to a hidden
computed field called plainText, you can then set the default value
of the view column to "plainText" to display the contents of the RTField:

```
@Abstract([TextOnly];15360;"";"RTField")
```

Use caution if using
@Text to convert numbers or dates in a column. In databases hosted
by a server, the numbers and dates always display using the format
settings of the hosting server's operating system. Also, if the date
or number format settings of either the client accessing the database
or server hosting the database change, you may need to entirely rebuild
the view.

## Examples

1. This example returns 123.45.

   ```
   @Text(123.45)
   ```
2. This example returns $800.00 if the value in the Sales field is
   800.

   ```
   @Text(Sales;"C,2")
   ```
3. This example returns 8.00E+02.

   ```
   @Text(800;"S")
   ```
4. This example returns 8.00E+02 and -6.00E+02 in a list.

   ```
   @Text(800 : (-600);"S")
   ```
5. This example returns 04/11/93 10:43 AM.

   ```
   @Text(@Now)
   ```
6. This example returns 04/11.

   ```
   @Text(@Now;"D1S0")
   ```
7. This example returns 10:43:30 AM.

   ```
   @Text(@Now;"D1S1")
   ```
8. This example returns 04/93 10:43 AM.

   ```
   @Text(@Now;"D3T1")
   ```
9. This example returns the rich-text Body field stripped of attachments
   and formatting.

   ```
   @Text(Body)
   ```
10. To convert a number date (in the ShipDate field) into a written
    date, you can use the following code. If ShipDate contains [08/31/2002],
    the result is "August 31, 2002."

    ```
    @If( @IsTime(ShipDate); 
    @Text(@Select(@Month(ShipDate); "January"; "February"; "March"; "April"; "May"; "June"; "July"; "August"; "September"; "October"; "November";  "December"))  + " " +
    @Text(@Day(ShipDate)) + ", " + @Text(@Year(ShipDate));
     "No date given")
    ```

---

## @TextToNumber

# @TextToNumber (Formula Language)

Converts a text string to a number, where possible.

## Syntax

**@TextToNumber(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string you want to convert to a number. If the *string* contains
both numbers and letters, it must begin with a number to be converted
properly. For example, the string "12ABC" converts to 12, but "ABC12"
produces an error.

## Return value

*number*

Number or number list. The *string,* converted
to a number.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

This
function is useful for converting a number in a text field to a number
that can be used for computation in a number field.

You can't
use @TextToNumber to convert special text (such as that returned by
@DocChildren or @DocDescendants) to a number.

@TextToNumber
returns an error If you try to pass anything besides a string into
it.

## Examples

1. This example returns 123 as a number.

   ```
   @TextToNumber("123")
   ```
2. This example returns 123 and 456 as a number list.

   ```
   @TextToNumber("123" : "456")
   ```
3. This example returns @ERROR if the contents of the field named
   Cost cannot be converted to a number.

   ```
   @TextToNumber(Cost)
   ```

---

## @TextToTime

# @TextToTime (Formula Language)

Converts a text string to a time-date value, where possible.

## Syntax

**@TextToTime(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string you want to convert to a time-date.

## Return value

*time-date*

Time-date, time-date range,
or list thereof. The *string,* converted to a time-date.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

This
function is useful for converting a date within a text field to a
value that can be used for computation in a time-date field.

"Today",
"Tomorrow", and "Yesterday" are the only legal strings to use to represent
relative dates. The formula @TextToTime("Next week") returns a blank
because the text string "Next week" cannot be converted to a time-date
value.

@TextToTime returns an error If you try to pass anything
besides a string into it, including a time-date value.

## Examples

1. This example returns 12-31-2025.

   ```
   @TextToTime("12-31-2025")
   ```
2. This example returns 12-31-2025, if today is December 31,
   2025.

   ```
   @TextToTime("Today")
   ```
3. This example returns 01-01-2026, if tomorrow is January 1,
   2026.

   ```
   @TextToTime("Tomorrow")
   ```
4. This example returns 12-30-2025, if yesterday was December 30,
   2025.

   ```
   @TextToTime("Yesterday")
   ```

---

## @ThisName

# @ThisName (Formula Language)

Returns the name of the current field.

Note: This @function is new with Release 6.

## Syntax

**@ThisName**

## Return value

*name*

Text. The name of the current
field.

## Usage

This
@function returns null outside a field formula.

Note: A
hide formula is not a field formula, even though it can be set from
the field properties dialog. The hide formula applies to the paragraph
containing the field. Since a paragraph can contain several fields,
there is no "current field" in this context.

This @function is useful
in writing portable code. Use @ThisName to construct references to
other fields (for example, in [@GetField](H_GETFIELD.html "Returns the value of a specified field."))
that have similar names.

## Examples

1. Assume a form has fields named Total\_1, Quantity\_1, Cost\_1, Total\_2,
   Quantity\_2, Cost\_2, and so on. The Total fields are computed using
   the following formula for the value. The same formula can be used
   in every Total field.

   ```
   Suffix := @Right(@ThisName; "_");
   QuantityFld := "Quantity_" + Suffix;
   CostFld := "Cost_" + Suffix;
   @GetField(QuantityFld) * @GetField(CostFld)
   ```
2. This formula makes it easier for a designer to check code on a
   form that may have several debugging @Prompt functions in several
   different fields because it identifies which field value is being
   displayed:

   ```
   result := @Round(2.66);
   @Prompt([Ok];@ThisName;@Text(result));
   result + 2
   ```

   This example returns 3 in the "roundNumber"
   dialog box and 5 in the roundNumber field when the form displays.

---

## @ThisValue

# @ThisValue (Formula Language)

Returns the value of the current field.

Note: This @function is new with Release 6.

## Syntax

**@ThisValue**

## Return value

*value*

The value of the current field.

## Usage

This
@function returns null outside a field formula.

Note: A
hide formula is not a field formula, even though it can be set from
the field properties dialog. The hide formula applies to the paragraph
containing the field. Since a paragraph can contain several fields,
there is no "current field" in this context.

This @function is useful
in writing portable code. Use @ThisValue instead of the name of the
current field.

## Examples

1. This translation formula replaces all spaces with underscores
   in the current field.

   ```
   @ReplaceSubstring(@ThisValue; " "; "_")
   ```
2. This input validation formula for a listbox field checks whether
   the user selected more than one list option and asks them to if they
   have not:

   ```
   @If((@ThisValue != "") & (@Elements(@ThisValue) = 1);@Failure("You must select more than one choice");@Success)
   ```

---

## @Time

# @Time (Formula Language)

Translates numbers for the various components of time and
date; then returns the time-date value.

## Syntax

**@Time(**  *hour*  **;**  *minute*  **;**  *second*  **)
@Time(**  *year*  **;**  *month*  **;**  *day*  **;**  *hour*  **;**  *minute*  **;**  *second*  **)
@Time(**  *time-date*  **)**

## Parameters

*year*

Number.
The year.

*month*

Number. The month.

*day*

Number.
The day.

*hour*

Number. The number of hours you
want to appear in the resulting time.

*minute*

Number.
The number of minutes you want to appear in the resulting time.

*second*

Number.
The number of seconds you want to appear in the resulting time.

*time-date*

Time-date
or time-date list. For a time-date value such as @Now or [10/31/93
12:00:00], @Time removes the date portion of the value, leaving only
the time.

## Return value

*truncatedTimeDate*

Time-date.
The time corresponding to the parameters you sent to @Time, minus
any date components if the parameter is *time-date*.

## Usage

If
the first parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

## Examples

1. This example returns 4/11/51 11:50:30 PM.

   ```
   @Time(1951;04;11;23;50;30)
   ```
2. This example returns 12:00 PM and 01:00 PM in a list.

   ```
   @Time([10/31/93 12:00:00] : [10/31/93 13:00:00])
   ```
3. This example returns 09:19:24 AM at 9:19:24 A.M on any day.

   ```
   @Time(@Now)
   ```
4. This example returns 09:19:24 AM if 9:19:24 A.M is the time the
   document was created.

   ```
   @Time(@Created)
   ```

---

## @TimeMerge

# @TimeMerge (Formula Language)

Builds
a time-date value from separate date, time, and time zone values.

Note: This @function is new with Release 6.

## Syntax

**@TimeMerge(**  *date* **;**  *time*  **;**  *timeZone*  **)**

## Parameters

*date*

Time-date
value or time-date list. The date you want to include in the new date-time
value.

*time*

Time-date value or time-date list.
The time you want to include in the new date-time value.

*timeZone*

String.
Optional. The canonical time zone value you want to apply to the new
date-time value. You can use a Time zone field to create this value.

## Return value

*Time-date*

A new time-date value or
time-date list made up of the date, time, and zone supplied as function
parameters.

## Usage

If
the first or second parameter is a list, the function operates pair-wise
on the list elements, and the return value is a list with the same
number of elements as the larger list.

## Examples

1. This code, when added to a hostpot button, displays 02/23/2002
   05:45:00 AM in the Merged date dialog box if the field date contains
   02/23/02 and the field time contains 17:45:00.

   ```
   @Prompt([OK];"Merged date";@Text(@TimeMerge(date;time)))
   ```
2. This code, when added to a form action, displays 02/23/2002 05:45:00
   AM in the Merged date dialog box if the field date contains 02/23/02
   02:30:00 and the field time contains 03/23/03 05:45:00.

   ```
   @Prompt([OK];"Merged date";@Text(@TimeMerge(date;time)))
   ```
3. This code, when added to a hotspot button, displays 07/04/2002
   08:30:00 PM in the Merged date dialog box if the field date contains
   07/04/02, the field time contains 13:30:00, and the field zone contains
   Z=11$DO=0$ZX=1$ZN=Samoa (which displays as GMT-11:00). The hour is
   adjusted to reflect the specified time zone.

   ```
   @Prompt([OK];"Merged date";@Text(@TimeMerge(date;time;zone)))
   ```
4. This code displays 01/01/2008 05:14 AM and 02/14/2008 07:45 PM
   in a list.

   ```
   @TimeMerge([1:1:2008] : [2/14/2008]; [5:14 AM] : [7:45 PM])
   ```

---

## @TimeToTextInZone

# @TimeToTextInZone (Formula Language)

Converts a time-date value to a text string, incorporating
time zone information.

## Syntax

**@TimeToTextInZone(**  *timeDate*  **;**  *timeZone*  **;**  *formatString*  **)**

## Parameters

*timeDate*

Time-date
value or time-date list. The time-date value or values to be converted.

*timeZone*

Canonical
time zone value. You can derive a time zone value using a NotesÂ® Time zone field.

*formatString*

Optional.
String consisting of one or more of the following format specifiers:

| Format specifier | Definition |
| --- | --- |
| D0 | Year, month, and day |
| D1 | Month and day, year if it is not the current year |
| D2 | Month and day |
| D3 | Month and year |
| T0 | Hour, minute, and second |
| T1 | Hour and minute |
| S0 | Date only |
| S1 | Time only |
| S2 | Date and time |
| S3 | Date, time, Today, or Yesterday |
| Sx | Use when you cannot predict the exact format of the value being passed, but you know that it is either a time, a date, or both. |

You can include up to three specifiers, but only one that
begins with D, one that begins with T, and one that begins with S.

## Return value

*string*

The time-date value converted
to a string.

## Usage

If
the first parameter is a list, the function operates on each list
element, and the return value is a list with the same number of elements.

## Examples

1. This code, when used in an action button on a form, applies the
   zone information of GMT-00:00 that a user selects from the list in
   the "There" Time zone field to the time-date of 02/26/2002 03:19 PM
   EST that results from an @Now formula in the "Here" Date/Time field.
   The "Time there" message box that appears displays "02/26/2002 08:19:00
   PM."

   ```
   @Prompt([OK];"Time there";@TimeToTextInZone(Here;There))
   ```
2. This code, when added as a Column Value formula, displays "11:06
   AM Today" in the view column if the Here field contains "02/26/2002
   03:06 PM EST," the There field contains Z=9$DO=1$DL=4 1 1 10-1 1$ZX=3$ZN=Alaskan
   (which displays as GMT -09:00 Alaska), and the current date is 02/26/2002.

   ```
   @TimeToTextInZone(Here;There;"D2T1S3")
   ```

---

## @TimeZoneToText

# @TimeZoneToText (Formula Language)

Converts a canonical time zone value to a human-readable
text string.

## Syntax

**@TimeZoneToText(**  *timeZone*  **;**  *formatString*  **)**

## Parameters

*timeZone*

Canonical
time zone value or list thereof. Use a NotesÂ® Time
zone field to create a time zone value.

*formatString*

Optional.
String consisting of one or more of the following format specifiers:

| Format specifier | Returns |
| --- | --- |
| S | Short time zone string, for example:  "GMT-08:00" |
| A | Alias for local time zone. For example, if the zone is the same as the zone in which the system is running, returns:  "Local time" |

## Return value

*string*

The time-date value converted
to a string. If you do not include a format specifier, a long time
zone label is returned. For example:

"(GMT-08:00) Pacific Time
(US & Canada);Tijuana"

## Usage

If
the first parameter is a list, the function operates on each list
element, and the return value is a list with the same number of elements.

This
function is useful for displaying the contents of a Time zone field
in a view. If you do not use this function, a Time zone field value
displays in the view with a format similar to the following:

Z=9$DO=1$DL=4
1 1 10-1 1$ZX=1$ZN=Alaskan

Also use this function with the [@GetCurrentTimeZone](H_GETCURRENTTIMEZONE.html "Returns the current operating system's time zone settings in canonical time zone format.") function
to translate the time zone value it returns into a readable string.

## Examples

1. This code, when added as the Column Value formula for a view,
   displays the contents of the Time zone field named Zone as "GMT-07:00"
   if the Zone field has the value Z=7$DO=0$ZX=6$ZN=US Mountain (which
   is selected in the Time zone field as GMT-07:00 Arizona).

   ```
   @TimeZoneToText(Zone;"S")
   ```
2. This code, when added as the Column Value formula for a view and
   accessed from a system running in the EST time zone, displays a document
   that has (GMT 00:00) Greenwich Mean Time: Dublin, Edinburgh, Lisbon,
   London selected in its "Zone" Time zone field as "" and a document
   that has (GMT-05:00) Eastern Time (US & Canada) selected in its
   Zone field as "Local time."

   ```
   @TimeZoneToText(Zone;"SA")
   ```
3. This code, when added as the Column Value formula for a view,
   displays the contents of the Time zone fields named Zone1 and Zone2.

   ```
   @TimeZoneToText(Zone1 : Zone2; "S")
   ```

---

## @Today

# @Today (Formula Language)

Returns today's date.

## Syntax

**@Today**

## Return value

*today*

Time-date.
Today's date.

## Usage

This
function is identical to the formula @Date(@Now). It is usually used
in default value formulas to automatically enter the current date.

Using
@Today in column or selection formulas may impact the efficiency of
your application. It also causes the view refresh indicator to display
constantly.

In a field formula, Notes/Domino takes the value
for @Today from the client computer's clock.

## Examples

1. This example returns 02/19/93 if today is February 19, 1993.

   ```
   @Today
   ```
2. This example sets the field named ReceivedDate to today's date.

   ```
   FIELD ReceivedDate:=@Today
   ```

---

## @Tomorrow

# @Tomorrow (Formula Language)

Returns the time-date value that corresponds to tomorrow's
date.

## Syntax

**@Tomorrow**

## Return value

*tomorrow*

Time-date. Tomorrow's date.

## Usage

Using
@Tomorrow in column or selection formulas may impact the efficiency
of your application. It also causes the view refresh indicator to
display constantly.

In a field formula, Notes/Domino takes
the value for @Tomorrow from the clock in the client computer.

## Examples

1. This example returns 4/26/93 if today is April 25, 1993.

   ```
   @Tomorrow
   ```
2. This example sets the field named AnswerBack to tomorrow's date.

   ```
   FIELD AnswerBack:=@Tomorrow
   ```

---

## @ToNumber

# @ToNumber (Formula Language)

Converts a value to a number.

Note: This @function is new with Release 6.

## Syntax

**@ToNumber(** *value* **)**

## Parameters

*value*

Text,
number, or list thereof. A value that cannot be converted returns
the error, "The value cannot be converted to a Number."

## Return value

*number*

Number or number list. The
value converted to a number.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.
A failure occurs if any element cannot be converted.

This function
is useful for ensuring that a value has a number data type before
using it in functions that require numbers as parameters.

## Examples

1. This example returns 20 and 40 in a list.

   ```
   @ToNumber("20" : "40")
   ```
2. This example results in the error, "The value cannot be converted
   to a Number."

   ```
   @ToNumber("20" : "r")
   ```
3. This example converts the values in a text field, containing "20,"
   and a number field containing 10, into numbers so that they can be
   added using the @Sum function, which requires two numbers. The formula
   returns 30.

   ```
   @Sum(@ToNumber(numberField);@ToNumber(textField))
   ```

---

## @ToTime

# @ToTime (Formula Language)

Converts a value with a data type of text or time to a
date-time value.

Note: This @function is new with Release 6.

## Syntax

**@ToTime(** *value* **)**

## Parameters

*value*

Text,
time, or list thereof. A value that cannot be converted returns the
error, "The value cannot be converted to a Number."

## Return value

*time*

The value converted to a time
value.

## Usage

This
function is useful for ensuring that a value has a time data type
before using it in functions that require time values as parameters.

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.
A failure occurs if any element cannot be converted.

## Examples

1. This example returns the time-date values 02/29/2008 and 03/01/2008
   in a list.

   ```
   @ToTime("2/29/08" : "3/1/08")
   ```
2. This code, when added to a field, converts the text value in the
   "date" field containing "08/29/01" into a time value and adds two
   days to the date. This function returns 08/31/01.

   ```
   @Adjust(@ToTime(date);0;0;2;0;0;0)
   ```
3. This example, when added to an action button, displays the date
   two days after the date selected by a user in the "request" date-time
   field.

   ```
   @Prompt([Ok];"Delivery";@Text(@Adjust(@ToTime(holiday);0;0;2;0;0;0)))
   ```

---

## @Transform

# @Transform (Formula Language)

Applies a formula to each element of a list and returns
the results in a list.

Note: This @function is new with Release 6.

## Syntax

**@Transform(**  *list*  **;**  *variableName*  **;**  *formula*  **)**

## Parameters

*list*

Text,
number, or time-date list. The list to be acted upon.

*variableName*

Text.
The name of a variable. Use this variable in the formula to refer
to the list element being acted upon.

*formula*

Valid
formula that evaluates to a result. The remainder of @Transform after
the second parameter is the formula that is applied to each element
of the input list. The formula must return a value.

## Return value

*list*

Text, number, or time-date.
The result of the transformation on the input list. The first value
returned by the formula determines the data type of the list. Subsequent
return values must be of the same type.

## Usage

An
iteration of the formula can return a list, which adds multiple values
to the return list.

@Transform returns an error if any iteration
of the formula returns an error.

If an iteration of the formula
returns [@Nothing](H_NOTHING.html), no element is
added to the return list.

## Examples

1. This formula returns a 3-element list whose values are 2, -2,
   and 4.

   ```
   @Transform(OriginalList; "x";
   @If(x >= 0; @Sqrt(x); -@Sqrt(@Abs(x))))
   ```
2. This formula returns the same as the previous one. However, if OriginalList
   is null, this formula returns null rather than an error.

   ```
   @If(OriginalList = @Nothing; @Nothing;
   @Transform(OriginalList; "x";
   @If(x >= 0; @Sqrt(x); -@Sqrt(@Abs(x)))))
   ```
3. This formula returns a 2-element list whose values are 2 and 4.

   ```
   @If(OriginalList = @Nothing; @Nothing;
   @Transform(OriginalList; "x";
   @If(x >= 0; @Sqrt(x); @Nothing)))
   ```
4. This formula, when used in a hotspot button creates a field called
   originalCorrected that adds an asterisk to the beginning of each element
   in the "original" text list if it does not already have one.

   ```
   FIELD originalCorrected := @Transform(original;"var";
   @If(@Begins(var;"*");
   var;
   "*" + var))
   ```

---

## @Trim

# @Trim (Formula Language)

Removes leading, trailing, and redundant spaces from a
text string, or from each element of a text list.

## Syntax

**@Trim(**  *string*  **)**

## Parameters

*string*

Text
or text list.

## Return value

*trimmedString*

Text
or text list. The *string,* with extra spaces removed.

## Usage

If
a text string is all spaces, @Trim returns an empty string (length
of 0). If an element of a text list is all spaces, @Trim removes the
element. If all elements of a text list are all spaces, @Trim returns
an empty string.

This function removes only spaces, not other
whitespace characters such as tabs and newlines. To remove whitespace
characters, you might first convert them to spaces with [@ReplaceSubstring](H_REPLACESUBSTRING.html "Replaces specific words or phrases in a string with new words or phrases that you specify. Case sensitive.").

## Examples

1. This example returns **ROBERT SMITH**.

   ```
   @Trim(@UpperCase("Robert Smith    "))
   ```
2. This example returns **ROBERT SMITH**.

   ```
   @UpperCase(@Trim("        Robert       Smith"))
   ```
3. This example returns **Just a quick reminder**, if the original
   Topic field is "Just a quick reminder."

   ```
   @Trim(Topic)
   ```
4. This example returns **Seattle;Toronto;Santiago;USA;Canada;Chile** if
   the list of values contained in the City field consists of Seattle,
   Toronto, Santiago; the StateOrProvince field contains no values; and
   the Country field contains the list of values USA, Canada, Chile.

   ```
   @Trim(City:StateOrProvince:Country)
   ```
5. This example returns **45** if the content of the field Date
   is 8/29/89 16:30:45.

   ```
   @Trim(@Text(@Second(Date)))
   ```
6. This input translation formula replaces all tabs and newlines
   with spaces then trims the field.

   ```
   @Trim(@ReplaceSubstring(@ThisValue; @Char(9) : @NewLine; " "))
   ```

---

## @True

# @True (Formula Language)

Returns the number 1. This function is equivalent to @Yes.

## Syntax

**@True**

## Return value

*true*

Number.
The number 1.

## Examples

1. This example returns 1.

   ```
   @True
   ```
2. This example returns 1 if the value in the Dept field is greater
   than 100.

   ```
   @If(Amount > 1000; @True; !(@UserRoles = "[Manager]")
   ```

---

## @Unavailable

# @Unavailable (Formula Language)

Deletes the value of an editable field.

## Syntax

**FIELD**  *fieldName*  **:=
@Unavailable**

## Usage

This
function works in agent, view action, and toolbar button formulas.

If
the field has a default value, the default value is reinstated after
this function deletes the current value.

This function is the
same as [@DeleteField](H_DELETEFIELD.html "Deletes the value of an editable field.").

Do
not use this function to test to see if a field is unavailable. Use [@IsUnavailable](H_ISUNAVAILABLE.html "Indicates whether a field name exists in a document.") instead.

## Examples

This formula creates a field named
NewDate and sets it to today's date, then removes the field named
OldDate from the document.

```
FIELD NewDate:=@Today
FIELD OldDate:=@Unavailable;
```

---

## @UndeleteDocument

# @UndeleteDocument (Formula Language)

In a database with "Allow soft deletions" selected, this command
restores a deleted document.

Note: This @function is new with Release 5.

## Syntax

**@UndeleteDocument**

## Usage

This
@function can be used in toolbar button, hotspot, action, and agent
formulas.

To allow soft -- that is, delayed -- deletions, go
to the Advanced tab of database properties, check "Allow soft deletions,"
and specify an integer value for "Soft delete expire time in hours."
Soft-deleted documents appear to be deleted but are held in the database
for the specified number of hours before actual deletion.

To
see the soft-deleted documents, create a view of type "Shared, contains
deleted documents." To restore a soft-deleted document, run @UndeleteDocument
on it before the "Soft delete expire time in hours" expires.

## Examples

This is the formula for an action
in a view of type "Shared, contains deleted documents." The user can
go to this view, see the documents that are soft-deleted, and run
this action on selected documents to restore them. The database must
"Allow soft deletions" and specify "Soft delete expire time in hours."

```
@UndeleteDocument
```

---

## @Unique

# @Unique (Formula Language)

Without a parameter, returns a random, unique text value.
With a parameter, removes duplicate values from a text list by returning
only the first occurrence of each member of the list.

## Syntax

**@Unique
@Unique(**  *textlist*  **)**

## Parameters

*textlist*

Text
list. Any text list.

## Return value

Without a parameter:

*uniqueValue*

Text.
A random, unique text value.

With a parameter:

*uniqueList*

Text
list. The text list, with duplicate values removed.

## Usage

@Unique
is case-sensitive.

## Examples

1. This example returns red; green; blue.

   ```
   @Unique("red":"green":"blue":"green":"red")
   ```
2. This example returns red; green; blue; Green.

   ```
   @Unique("red":"green":"blue":"Green":"red")
   ```

---

## @UpdateFormulaContext

# @UpdateFormulaContext (Formula Language)

Updates the context of a formula to the NotesÂ® client window currently being accessed
by the code. For example, if the code accesses a new form called "Response"
by using @Command([Compose]:"Response", @UpdateFormulaContext switches
the context of the formula to this new form. Any subsequent functions
in the code execute in the context of the Response document, not the
current document.

Note: This function in new with Release 6.

## Syntax

**@UpdateFormulaContext**

## Usage

You
can use @UpdateFormulaContext to extract values from or set values
in external documents. You can even access document- and database-specific
information using functions such as @DbName, @DbTitle, @Created, @DocumentUniqueID,
@GetDocField, @GetField, @GetProfileDocument.

This function is only valid in the NotesÂ® client; it is not supported in Web applications.
@UpdateFormulaContext is only valid in formulas that interact with
the user, such as in agents that have no target documents, events,
toolbar buttons, hotspot buttons, and actions. It does not work in
formulas in which @commands cannot be used.

## Examples

1. The following code, when used in a view action, creates a response
   document to the currently selected document then populates its fname
   and lname fields with the values of the fname and lname fields in
   the current document:

   ```
   tempfname := fname;
   templname := lname;
   @Command([Compose];"Response");
   @UpdateFormulaContext;
   FIELD fname := tempfname;
   FIELD lname := templname
   ```
2. The following code, when used in a view action that contains documents
   that have the fields "CreatedDate," which displays the document's
   creation date and "nextCreated," an editable text field, opens the
   previous document in the view and adds the creation date of the current
   document into its "nextCreated" field:

   ```
   tempDate := @GetDocField(@DocumentUniqueID;"CreatedDate");
   @Command([NavPrev]);
   @Command([EditDocument]);
   @UpdateFormulaContext;
   @SetDocField(@DocumentUniqueID;"nextCreated";tempDate)
   ```

---

## @UpperCase

# @UpperCase (Formula Language)

Converts the lowercase letters in the specified string
to uppercase.

## Syntax

**@UpperCase(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string you want to convert to uppercase.

## Return value

*uppercaseString*

Text. The *string,* converted
to uppercase letters.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

This
function is useful when you want to search for a particular value
and cannot predict whether it will appear in lowercase, uppercase,
or a combination of the two. You can also use it as an input translation
formula to convert a field's contents to uppercase.

## Examples

1. This example returns ROBERT T. SMITH.

   ```
   @UpperCase("Robert T. Smith")
   ```
2. This example returns MA if the State field contains "ma," "Ma,"
   or "MA."

   ```
   @UpperCase(State)
   ```
3. This example returns FLETCHER if "William Fletcher" is the name
   associated with the current User ID. @UpperCase is used in conjunction
   with @Right to find and convert only the user's last name.

   ```
   @UpperCase(@Right(@UserName;" "))
   ```

   If
   the user id is a hierarchical id, the following code returns FLETCHER:

   ```
   @UpperCase(@Right(@Name([CN]; @UserName); " "))
   ```
4. This example returns ROBERT and SMITH in a list.

   ```
   @UpperCase("Robert" : "Smith")
   ```

---

## @URLDecode

# @URLDecode (Formula Language)

Decodes a URL string into regular text.

Note: This function is new with Release 6.

## Syntax

**@URLDecode(**  *decodeType
; token*  **)**

## Parameters

*decodeType*

Text.
The type of encoding you want to use to translate the token. You can
specify either a string argument or a MIME character set.

String
arguments:

* **"DominoÂ®"** -- Decodes
  the token using the standard character set used by the DominoÂ® Web server. This keyword is equivalent
  to the "UTF-8" MIME character set.
* **"Platform"** -- Decodes the token using the current system's
  native character set.

MIME character set:

Decodes the hexadecimal digits
that represent the code value into octets, then converts the specified
character sets into LBMCS. The supported MIME character sets are:

* "UTF-8" -- UCS(Universal Character Set) Transformation Format
  8. An ASCII-compatible multi-byte Unicode and UCS encoding.
* "ISO-8859-1" -- The ISO's (International Standards Organization)
  8-bit, single-byte-coded graphic character set for European languages.
* "Shift\_JIS" -- The character set for the Japanese language.

*token*

Text or text list. URL string(s) to
be decoded.

## Return value

*String*

Text or text list. Returns
a decoded version of a URL string.

## Examples

This code, when used as the default
value for a field, decodes the URL-formatted string in the encode
field. It returns "Employee/My Database" if the encode field contains
"Employee%2FMy%20Database.nsf."

```
@URLDecode("Domino";encode)
```

---

## @URLEncode

# @URLEncode (Formula Language)

Encodes a string into a URL-safe format.

## Syntax

**@URLEncode(**  *encodingFormat*  **;**  *token*  **)**

## Parameters

*encodingFormat*

Text.
The type of encoding you want to use to translate the token. You can
specify either a string argument or a MIME character set.

String
arguments:

* **"DominoÂ®"** -- Encodes
  the token in the standard character set used by the DominoÂ® Web server. This keyword is equivalent
  to the "UTF-8" MIME character set.
* **"Platform"** -- Encodes the token using the current system's
  native character set.

MIME character set:

Converts non-ASCII characters
into the specified character set and encodes the characters into %XX
format, where XX is a hexadecimal digit representing the encoded value.
Some examples include:

* "UTF-8" -- UCS(Universal Character Set) Transformation Format
  8. An ASCII-compatible multi-byte Unicode and UCS encoding.
* "ISO-8859-1" -- The ISO's (International Standards Organization)
  8-bit, single-byte-coded graphic character set for European languages.
* "Shift\_JIS" -- The character set for the Japanese language.

*token*

Text or text list. URL string(s) to
be encoded.

## Return value

*encodedURLString*

Text or text list.
Returns the URL string(s) encoded in the specified encoding format.

## Usage

Do
not use @URLEncode to encode an entire URL string. For example, @URLEncode("DominoÂ®";"http://www.ibm.com/")
returns "http%3A%2Fwww.ibm.com%2F," which would not link successfully
to the desired website.

## Examples

1. This formula returns "By%20Date" as the encoded URL.

   ```
   @URLEncode("Domino";"By Date")
   ```
2. This formula returns "Support%20%E0%20la%20client%E8le" as the
   encoded URL.

   ```
   @URLEncode("ISO-8859-1";"Support Ã  la clientÃ¨le")
   ```
3. This formula returns "Support%20%C3A0%20la%20client%C3%A8le" as
   the encoded URL.

   ```
   @URLEncode("UTF-8";"Support Ã  la clientÃ¨le")
   ```

---

## @URLGetHeader

# @URLGetHeader (Formula Language)

Returns specific Hypertext Transfer Protocol (HTTP) header
information from the Uniform Resource Locator (URL). A URL is a text
string used for identifying and addressing a Web page.

## Syntax

**@URLGetHeader(**  *urlstring;
headerstring; webusername; webpassword; proxywebusername; proxywebpassword*  **)**

## Parameters

*urlstring*

Text.
The URL for the Web page you want to open, for example, http://www.acme.com/.

*headerstring*

Enter
a header string to return the desired URL header value. The acceptable
header strings are documented in the HTTP specification (available
at locations on the Internet, such as http://www.w3.org/) and are
subject to change based on updated versions of the specification.

*webusername$*

Text.
Optional. Some Internet servers require you to obtain a user name
and password before you can access their pages. This parameter allows
you to enter the user name that you previously obtained from the authenticated
Internet server.

*webpassword$*

Text. Optional.
Some Internet servers require you to obtain a user name and password
before you can access their pages. This parameter allows you to enter
the password that you previously obtained from the authenticated Internet
server.

*proxywebusername$*

Text. Optional. Some
proxy servers require that you specify a user name in order to connect
through them. This parameter allows you to enter the user name for
the proxy server. See your administrator for the username required
by the proxy.

*proxywebpassword$*

Text. Optional.
Some proxy servers require that you specify a password in order to
connect through them. This parameter allows you to enter the user
name for the proxy server. See your administrator for the password
required by the proxy.

## Return value

*headervaluestring*

Text. Returns the
header value that you requested. If a null value is returned, the
header value that you requested was not found in the header of the
Web page.

## Usage

The
@URLGetHeader function should only be used in the context of either
the Server Web Navigator or Personal Web Navigator database.

## Examples

1. This example returns the last date that the www.acme.com Web page
   was modified.

   ```
   @URLGetHeader ("http://www.acme.com/"; "Last-modified")
   ```
2. This example returns the name of the Web server software where
   the www.acme.com Web page resides.

   ```
   @URLGetHeader ("http://www.acme.com/"; "Server")
   ```

---

## @URLHistory

# @URLHistory (Formula Language)

Used for navigating, saving, and reloading a Uniform Resource
Locator (URL) history list. The URL history list keeps track of all
the Web pages you have visited. The history list is used for the Next
and Previous buttons and for the Web Tours.

## Syntax

**@URLHistory(
[**  *command*  **] )**

## Parameters

**[**  *command*  **]**

Keyword.
The name of the @URLHistory command you want to use:

* **[NEXT]**

  Moves to the next URL in the history list.
* **[PREV]**

  Moves to the previous URL in the history list.
* **[RELOAD]**

  Reloads the current history list from the
  Web Tour document.
* **[SHOW]**

  Displays the History dialog box.
* **[SAVE]**

  Saves the history list into a new Web Tour document,
  which a user can reload later to follow that history.

## Usage

The
@URLHistory function works from the Notes/Domino workstation only
and should only be used with either the Server Web Navigator or Personal
Web Navigator database.

## Examples

This example moves to the next URL
in the history list.

```
@URLHistory([NEXT])
```

This
example moves to the previous URL in the history list.

```
@URLHistory([PREV])
```

This
example displays the History dialog box.

```
@URLHistory([SHOW])
```

This
example saves the history list into a new Web Tour document that a
user can reload later to follow that history.

```
@URLHistory([SAVE])
```

This
example reloads the history list from the Web Tour document.

```
@URLHistory([RELOAD])
```

---

## @URLOpen

# @URLOpen (Formula Language)

Retrieves a World Wide Web page specified by its URL.

## Syntax

**@URLOpen**

**@URLOpen(**  *urlstring*  **)**

**@URLOpen(**  *urlstring*  **;** **[**  *reloadflag*  **]
)**

**@URLOpen(**  *urlstring*  **; [URLLIST]
)**

**@URLOpen(**  *urlstring*  **; [**  *reloadflag*  **]:[URLLIST]
)**

**@URLOpen(**  *urlstring*  **; [**  *reloadflag*  **]:[URLLIST]
;**  *charset*  **)**

**@URLOpen(**  *urlstring*  **;
[**  *reloadflag*  **]:[URLLIST] ;** *charset* **;** *webusername* **)**

**@URLOpen(**  *urlstring*  **;
[**  *reloadflag*  **]:[URLLIST] ;** *charset*  **;** *webusername* **;** *webpassword*  **)**

**@URLOpen(**  *urlstring*  **;
[**  *reloadflag*  **]:[URLLIST] ;**  *charset*  **;**  *webusername*  **;**  *webpassword*  **;**  *proxywebusername*  **)**

**@URLOpen(**  *urlstring*  **;
[**  *reloadflag*  **]:[URLLIST] ;**  *charset*  **;**  *webusername*  **;**  *webpassword*  **;**  *proxywebusername*  **;**  *proxywebpassword*  **)**

## Parameters

*urlstring*

Text.
Optional. The URL for the Web page you want to open, for example,
http://www.acme.com/. This parameter may also include comma-separated
arguments to be passed by DominoÂ® to
the javascript window.open command.

**[**  *reloadflag*  **]**

Keyword.
Optional.

**RELOAD**. Reloads the page from its Internet
server.

**RELOADIFMODIFIED**. Reloads the page **only** if
it has been modified on its Internet server.

**[URLLIST]**

Keyword.
Optional. Web pages can contain URL links to other Web pages. This
keyword specifies that the Web Navigator should save the URLs in a
field called URLLinks*n* in the Notes/Domino document. (The Web
Navigator creates a new URLLinks*n* field each time the field
size reaches 64K. For example, the first URLLinks field would be
URLLinks1, the second would be URLLinks2, and so on.)

If you
save the URLs, you can use them in agents; for example, you could
create an agent that opens Web pages in the Web Navigator database
and then loads all the Web pages saved in each of the URLLinks*n* field(s).

CAUTION: Saving URLs in the URLLinks*n* field(s) may affect
performance.

**[RELOAD] : [URLLIST]**

Keywords.
Optional. Specify both keywords to force a reload of the Web page
and save the URLs in the URLLinks*n* field in the Notes/Domino
document.

*charset*

Text. Optional. Enter the
MIME character set (for example, ISO-2022-JP for Japanese or ISO-8859-1
for United States) that you want the Web Navigator to use when processing
the Web page. Only use this parameter when the Web Navigator detects
the MIME character set of the URL contents incorrectly.

*webusername*

Text.
Optional. Some Internet servers require you to obtain a user name
before you can access their pages. This parameter allows you to enter
the user name that you previously obtained from the Internet server.

*webpassword*

Text.
Optional. Some Internet servers require you to obtain a password before
you can access their pages. This parameter allows you to enter the
password that you previously obtained from the Internet server.

*proxywebusername*

Text.
Optional. Some proxy servers require that you specify a user name
in order to connect through them. This parameter allows you to enter
the user name for the proxy server. See your administrator for the
user name required by the proxy.

*proxywebpassword*

Text.
Optional. Some proxy servers require that you specify a password in
order to connect through them. This parameter allows you to enter
the password for the proxy server. See your administrator for the
password required by the proxy.

## Usage

The
@URLOpen function works from both the Notes/Domino workstation and
server.

The user name and password parameters work only with
the NotesÂ® Web Navigator. Other
browsers always prompt for authentication.

For use on the server,
you need to specify at least one parameter with the function; using
the function without any parameters will attempt to display the URL
Open dialog box which cannot be done from the server. If you want
to use any of the parameters that follow the Reload and URLList keywords
without specifying values for either of the keywords, enter a zero
(0) in place of the keyword value(s). For example, @URLOpen("http://www.ibm.com";0;"myusername";"mypassword").

When
a NotesÂ® browser triggers the
@URLOpen function, it displays the retrieved Web page in a new window.
When the @URLOpen function is used on a form or page that is accessed
by a non-Notes browser, DominoÂ® generates
a javascript window.open command with the following syntax:

```
window.open( [sURL] [, sName] [, sFeatures] [, bReplace])
```

To
display the retrieved Web page in a new window, pass the values for
sName and sFeatures (if desired) as comma-separated arguments within
the *urlstring*. For example, @URLOpen("http://www.ibm.com','NEW").
Be sure to use double quotes at the beginning and end of the urlstring
parameter, and single quotes before and after each comma separating
the arguments to be passed to window.open. Do not include any spaces.

To open another design element from
the current NotesÂ® database
in a Web application, use the [@WebDbName](H_WEBDBNAME.html "Returns the name of the current database encoded for URL inclusion.") function
to properly encode the database name.

## Examples

1. This example displays the URL Open dialog box that allows a user
   to enter the URL.

   ```
   @URLOpen
   ```
2. This example opens the www.acme.com Web page from the database
   if it is found there. If the page is not found in the database, it
   is retrieved from the Web, loaded into the database, and then opened.

   ```
   @URLOpen("http://www.acme.com/")
   ```
3. This example retrieves the www.acme.com Web page from the Web,
   loads it into the database, and then opens it.

   ```
   @URLOpen("http://www.acme.com/"; 1)
   ```
4. The following code, when added to an action on the "Purchasing"
   Web application form, opens the "CustomerInfo" NotesÂ® form, which resides in the same database:

   ```
   @URLOpen(@WebDbName + "/CustomerInfo?OpenForm")
   ```
5. The following code, in a document viewed from the Web, will open
   the www.acme.com Web page in the same window (\_self) as that document.

   ```
   @URLOpen("http://www.acme.com")
   ```
6. The following code, in a document viewed from the Web, will open
   the www.acme.com Web page in a new window (\_blank). Note that the
   window will not have any sFeatures assigned by previous javascript
   commands.

   ```
   @URLOpen("http://www.acme.com','_blank")
   ```
7. The following code, in a document viewed from the Web, will open
   the www.acme.com Web page in a new window (\_blank). All sFeatures
   will be inherited from the calling window.

   ```
   @URLOpen("http://www.acme.com','_blank','")
   ```
8. The following code, in a document viewed from the Web, will open
   the www.acme.com Web page in a new window (NEW). The window will
   not inherit any sFeatures.

   ```
   @URLOpen("http://www.acme.com','NEW")
   ```
9. In a document viewed from the Web, clicking on the following hyperlink
   will open a new window (mywindow) displaying the www.yahoo.com Web
   page.

   ```
   <a href="javascript: mywin = window.open('http://www.yahoo.com','mywindow');mywin.focus()">yahoo</a>
   ```

   The
   following code will open the www.acme.com Web page in the mywindow
   window. All sFeatures will be inherited from the mywindow calling
   window.

   ```
   @URLOpen("http://www.acme.com','mywindow','")
   ```

---

## @UrlQueryString

# @UrlQueryString (Formula Language)

In a Web application, returns the current URL command and
parameters, or the value of one of the parameters.

Note: This function is new with Release 6.

## Syntax

**@UrlQueryString(** *parameterName* **)**

## Parameters

*parameterName*

Text.
Optional. The name of a parameter in the URL command.

## Return value

*query*

Text or text list.

* If the parameter is not specified, the return value is the URL
  command name (first list element) followed by the parameters (name,
  equal sign, value).
* If the parameter is specified, the return value is the value of
  the parameter or null if the parameter does not exist.

## Usage

@UrlQueryString
is useful in formulas that run in the context of a browser.

This
function can be used to compose dynamic DB2Â® query
views in Web applications.

The NotesÂ® client always returns null for this formula.

## Examples

For the first two examples, the URL
command is:

```
http://www.acme.com/marketing.nsf?OpenForm&ID=986574&Category=Golf
```

1. This example:

   ```
   @UrlQueryString
   ```

   returns
   the list:

   * OpenForm
   * ID=986574
   * Category=Golf
2. This example:

   ```
   @UrlQueryString("Category")
   ```

   returns
   the text:

   * Golf
3. This code allows a DB2Â® Query
   View to be composed dynamically, by passing the Department information
   as part of the URL. To prevent SQL injection from the URL parameter,
   truncate at the first apostrophe, or if the data may contain apostrophes,
   escape your quotes using @ReplaceSubstring.

   ```
      URLParam := @UrlQueryString("Dept");
   Clause := @If( URLParam ="" ; "" ; " AND DeptName='"+ @Word(URLParam; "'"; 1) +"'");
      
      "SELECT D.DeptID , D.DeptName As DeptName , E.DeptID , E.Lastname AS Lastname FROM " + T1 +"  AS D, " + T2 + " AS E WHERE D.DeptID=E.DeptID" + Clause
   ```

---

## @UserAccess

# @UserAccess (Formula Language)

Given a server and file name, indicates the current user's
level of access to the database.

Note: If you used @UserAccess in Release 4,
it is automatically converted to [@V4UserAccess](H_V4USERACCESS_FUNCTION.html "Given a server and file name, indicates the current user's level of access to the database.") in Release
5 or later to preserve the functionality of your formulas. If you
change those formulas to use @UserAccess, be sure to recompile them
under Release 5. If you use @UserAccess in Release 5, a database created
in Release 4 will not recognize the formula until you upgrade that
database to Release 5. If the formula will be evaluated in Release
4, use @V4UserAccess.

Note: The AccessPrivilege
keyword option is new with Release 6.

## Syntax

**@UserAccess(**  *server*  **:**  *file
;*  **[** *accessPrivilege* **] )**

## Parameters

*server*

Text.
The name of the server. Use an empty string ("") to indicate the local
computer.

*file*

Text. The path and file name
of the database. Specify the database's path and file name using the
appropriate format for the operating system.

**[**  *accessPrivilege* **]**

Keyword.
Optional. Specify one of the following keywords to return a user's
access level or test for a specific database privilege, instead of
returning a list containing all of the user's access information:

* **[ACCESSLEVEL]** returns a number from 1 to 6 that indicates
  the user's access level to the database.

  | Level | User's access level |
  | --- | --- |
  | 1 | Depositor |
  | 2 | Reader |
  | 3 | Author |
  | 4 | Editor |
  | 5 | Designer |
  | 6 | Manager |

The following return 1 (True) if the user has the specified
privilege and 0 (False) if the user does not. These privileges are
assigned in the Access Control List for the database.

* **[CREATEDOCUMENTS]**
* **[DELETEDOCUMENTS]**
* **[CREATEPERSONALAGENTS]**
* **[CREATEPERSONALFOLDERSANDVIEWS]**
* **[CREATELOTUSSCRIPTJAVAAGENTS]**
* **[CREATESHAREDFOLDERSANDVIEWS]**
* **[READPUBLICDOCUMENTS]**
* **[WRITEPUBLICDOCUMENTS]**
* **[REPLICATEORCOPYDOCUMENTS]**

## Return value

If you specify one or more keywords, returns a text
value or a text list containing the following values:

* The [AccessLevel] keyword returns a value of 1 through 6.
* The other keywords return a value of 1 or 0.

If you specify no keywords, returns a text list of values
for the following keywords:

* [AccessLevel] : [CreateDocuments] : [DeleteDocuments] : [CreatePersonalAgents]
  : [CreatePersonalFoldersAndViews] : [CreateSharedFoldersAndViews]
  : [CreateLotusScriptJavaAgents] : [ReadPublicDocuments] : [WritePublicDocuments]

  @UserAccess does not test for
  access to the *ReplicateOrCopyDocuments* privilege by default.

  Tip: If the multi-value separator for the field containing the
  formula is a semicolon, the values in the returned text list are separated
  by semicolons instead of colons.

## Usage

On
a local database without "Enforce a consistent Access Control List,"
@UserAccess without the second parameter always returns 6; 1; 1; 1;
1; 1; 1; 1; 1. If the current user has No Access to the database,
Notes/Domino displays a message: "You are not authorized to perform
that operation."

This
function does not work in column or selection formulas, or in agents
that run on a server (mail and scheduled agents). Hence it does not
work with the Evaluate statement.

## Examples

1. This formula returns the text list 3: 1: 1: 1: 1: 0 if the user
   has Author access, permission to create documents, delete documents,
   create private agents, create personal views and folders, but does
   not have permission to create shared views and folders in the NUN.NSF
   database in the DISCUSS directory on server Gaborone.

   ```
   @UserAccess( "Gaborone" : "discuss\\nun.nsf" )
   ```
2. This formula, when added to a form action button, creates a new
   document using the MyOpinion form if the current user has the privilege
   to create documents in the current (nun.nsf) database.

   ```
   @If(@UserAccess( "" : "discuss\\nun.nsf" ; [CREATEDOCUMENTS]) = "1";@Command([Compose];"MyOpinion");@Prompt([OK];"Access denied";"Sorry, you do not have permission to create documents in this database."))
   ```
3. This formula returns the text list 6: 1: 1: 1: 1: 1: 1: 1: 1 if
   the user has Manager access and permission to create and delete documents,
   create private agents, create personal and shared views and folders,
   create LotusScriptÂ® and/or Javaâ¢ agents, read and write public
   documents in the current database. The text list displays as 6; 1;
   1; 1; 1; 1; 1; 1; 1 if the multi-value separator for the field containing
   this formula is a semicolon.

   ```
   @UserAccess( @DbName )
   ```

---

## @UserName

# @UserName (Formula Language)

Returns the current user name.

If
the user name is hierarchical, @UserName returns it in canonical format
(including the CN, OU, O, and C identifiers). To return the name in
abbreviated format (omitting those identifiers), use @V3UserName.

## NotesÂ®

* If you used @UserName in Release 3, it is automatically converted
  to @V3UserName in Release 4 or later to preserve the functionality
  of your formulas. If you change those formulas to use @UserName, be
  sure to recompile them under Release 4 or later. If you use @UserName
  in Release 4 or later, a database created in Release 3 will not recognize
  the formula until you upgrade that database. If the formula will be
  evaluated in Release 3, use @V3UserName.
* With Release 5, @UserName returns the alternate name as well as
  the primary name which is associated with the ID.

## Syntax

**@UserName
(**  *index*  **)**

## Parameters

*index*

Note: This parameter is new with Release 5.

Number.
Optional. Indicating the index of user names. 0 is for primary name
and 1 is for the alternate name. If this parameter is omitted, @UserName
returns the primary name.

## Return value

*name*

Text. The primary or alternate
user name.

## Usage

When
a formula runs on a server, the agent signer is considered the current
user. Therefore, @UserName should only be used on local databases,
where it will return the user's name. Using @UserName in server-based
private views also returns the user's name, but produces unpredictable
results if the views on the server are rebuilt using Updall. You should
not use @UserName in a public view, doing so produces unpredictable
results. Also, if the field that you are referencing changes, you
will get unpredictable results because the index has to be rebuilt
to accommodate the new information.

One
use for @UserName is to display only those documents relevant to the
current user. For example, your Service Request database could use
@UserName in the private view named Assignments to display each technician's
assignments, weeding out everyone else's:

```
SELECT @UserName=AssignedTo
```

However,
the user can still design a different private view that retrieves
all documents, so don't depend on @UserName as a security mechanism.

For
an alternative way to display only documents relevant to the current
user, see "To show a single category in an embedded view."

If you are using Release 5 and have
an alternate name as well as a primary name, it is best to store the
alternate name in the document as author information when using the
extended feature of @UserName.

## Examples

1. This example returns CN=Robert T. Katsushima/OU=JPN/O=Acme if
   this is the name associated with the current user ID.

   ```
   @UserName(0)
   ```
2. This example returns Robert T. Katsushima.

   ```
   @Name([CN];@UserName)
   ```
3. This example returns CN=Rob Katsushima/OU=JPN/P=Acme if this is
   the first alternate name associated with the current user ID.

   ```
   @UserName(1)
   ```
4. This example returns Fletcher if William Fletcher is the name
   associated with the current User ID.

   ```
   @Right(@UserName;" ")
   ```
5. This example returns FLETCHER if William Fletcher is the name
   associated with the current User ID.

   ```
   @UpperCase(@Right(@UserName;" "))
   ```

   If
   the user id is a hierarchical id, the following code returns FLETCHER:

   ```
   @UpperCase(@Right(@Name([CN]; @UserName); " "))
   ```
6. This example returns the name in canonical format as shown. Given
   this hierarchical user ID: CN=Mary Tsen/OU=Illustration/OU=Documentation/OU=Development/OU=R&D/O=WorkSavers/C=US.
   To return the name in abbreviated format (omitting the CN, OU, O,
   and C identifiers), use [@V3UserName](H_V3USERNAME.html "Returns the current user name or server name. Using @V3UserName on a local database or in a private view in a server-based database returns the user's name.").

   ```
   @UserName
   ```

---

## @UserNameLanguage

# @UserNameLanguage (Formula Language)

Returns language tags associated with the user ID.

Note: This @function is new with Release 5.

## Syntax

**@UserNameLanguage(**  *index*  **)**

## Parameters

*index*

Number.
Indicates the index of user names. 0 is for primary name and 1 is
for alternate name. Numbers greater than 1 are not used but reserved
for future use.

## Return value

*namelanguage*

Text. Language tag for
the alternate user name. If the user does not have the alternate name,
@UserNameLanguage returns an empty string (""). Also, this function
returns an empty string for the primary name.

## Usage

The
alternate name is expected to be used for a user's native language
name.

Generally the native language name contains non-ASCII
characters and cannot be displayed correctly without some proper fonts.
The return value from @UsernameLanguage is used as reference of the
native language.

@UserNameLanguage can be used as a default
value formula to store the author's alternate language tag in their
document as well as their primary name and alternate name. While referring
to the language tag, the DominoÂ® application
can switch the display name on the document between the primary name
and the alternate name.

See [@Locale](H_LOCALE_5907.html "Returns information about language codes.") for
a list of language codes.

## Examples

1. The following example returns "ja" if you have a Japanese name
   for your alternate name.

   ```
   @UserNameLanguage(1)
   ```
2. The following example returns an empty string ("") because the
   primary name has no language tag associated.

   ```
   @UserNameLanguage(0)
   ```

---

## @UserNamesList

# @UserNamesList (Formula Language)

For a database on a server or a local database with "Enforce
a consistent Access Control List across all replicas" in effect, @UserNamesList
returns a text list containing the following information for the current
user:

* Common name
* All hierarchical names (fully distinguished) that include the
  user name; for example, CN=My Name/OU=My Org Unit/O=My Org, plus \*/OU=My
  Org Unit/O=My Org, \*/O=My Org, and \*
* Any roles associated with the user in the ACL
* All groups (excluding âMail onlyâ) to which the user belongs (only if the database is on a
  server)

Note: This @Function is new with Release 5.

## Syntax

**@UserNamesList**

## Return value

*names*

Text list. Each list item is
a name or role as specified previously. Returns an empty string ("") if
the current database is local and "Enforce a consistent Access Control
List across all replicas" is not in effect, and the database is not
replicated with the server database at least once.

## Usage

This
function does not work in column, selection, mail agent, or scheduled
agent formulas.

Choose File - Database - Access Control, Advanced
to set "Enforce a consistent Access Control List across all replicas."

[@UserRoles](H_USERROLES.html "For a database on a server or a local replicated database, returns a list of roles that the current user has. Roles are defined in a database's access control list.") returns a subset of the
information returned by @UserNamesList.

## Examples

This subform formula selects a different
subform depending on whether the user is a member of the Marketing
team or not. This formula works if the database containing it is on
a server.

```
@If(@IsMember("Marketing Team"; @UserNamesList); 
    "Marketing Head"; "Generic Head")
```

---

## @UserPrivileges

# @UserPrivileges (Formula Language)

Returns a text list of the current user's privileges. This
function returns only the position of the privilege in the privilege
list, not the name of the privilege.

Note: This @function is obsolete in Release 3. Use [@UserRoles](H_USERROLES.html "For a database on a server or a local replicated database, returns a list of roles that the current user has. Roles are defined in a database's access control list.") instead.

## Syntax

**@UserPrivileges**

## Return value

*privileges*

Text or text list.

## Usage

This
function does not work in scheduled agent formulas.

You cannot use this function in
Web applications.

## Examples

1. A database has five privileges. User Mary Tsen has been assigned
   Privileges 2 and 3. This example returns the text list **2:3** (which
   displays as 2;3 if the multi-value separator for the field containing
   the formula is semicolon).

   ```
   @UserPrivileges
   ```
2. This form formula causes the Marketing Report form to be used
   if the current user has been assigned the first privilege in the list
   (regardless of what it is called); otherwise, the Main Topic form
   is used.

   ```
   @If(@UserPrivileges = "1"; "Marketing Report"; "Main Topic")
   ```

---

## @UserRoles

# @UserRoles (Formula Language)

For a database on a server or a local replicated database,
returns a list of roles that the current user has. Roles are defined
in a database's access control list.

## Syntax

**@UserRoles**

## Return value

*roles*

Text list. Each item in the
list is the name of a role that the current user has in the current
database. The role names are enclosed in brackets. Returns an empty
string ("") if the current database is local and not a replication.

## Usage

This
function does not work in column, selection, mail agent, or scheduled
agent formulas.

Only roles explicitly assigned to the current
user are returned. Roles assigned to a group which includes the current
user are not returned.

@UserRoles
appends $$WebClient to the list of roles when a Web user opens a database.

@UserRoles
returns a subset of the information returned by [@UserNamesList](H_USERNAMESLIST_3223_ABOUT.html "For a database on a server or a local database with \"Enforce a consistent Access Control List across all replicas\" in effect, @UserNamesList returns a text list containing the following information for the current user:").

## Examples

1. This example displays the roles assigned to the current user.
   The roles are displayed in brackets.

   ```
   @UserRoles
   ```
2. This code, if added to the New Document action button of a database
   that has the Enforce a consistent ACL across all replicas checkbox
   selected on the Advanced tab of the ACL Properties box, opens the
   Manager form if the [Manager] role is assigned to the current user;
   otherwise it open the Employee form in a NotesÂ® application.

   ```
   @Command([Compose];"";@If(@IsMember("[Manager]";@UserRoles);"Manager";
   "Employee"))
   ```
3. This subform formula selects a different subform depending on
   whether the user is a Web client or not. The WebClient role is a role
   that is automatically created by Notes/Domino; it does not require
   the surrounding brackets, but does require the leading double dollar
   signs.

   ```
   @If(@IsMember("$$WebClient"; @UserRoles); "WebSubform"; "NotesSubform")
   ```

---

## @V2If

# @V2If (Formula Language)

This function performs an @If operation; the syntax is
the same as for @If.

## Syntax

**@V2If(**  *condition1*  **;**  *action1*  **;**  *condition2*  **;**  *action2*  **;**  *condition99*  **;**  *action99*  **;**  *else\_action*  **)**

## Usage

Use
@V2If when you expect your application to be used with NotesÂ® Release 2.x. If the application will
only be used with NotesÂ® Release
3 or later, you should use @If. The @If function in Release 3 was
redesigned to work in conjunction with the new @functions first available
in Release 3, such as @Prompt. Due to these changes, releases of NotesÂ® earlier than Release 3 cannot
evaluate @If correctly, and return an error message.

Note: In applications created with NotesÂ® prior to Release 4, the @If function
is automatically renamed to @V2If during the upgrade to Release 4.

---

## @V3UserName

# @V3UserName (Formula Language)

Returns the current user name or server name. Using @V3UserName
on a local database or in a private view in a server-based database
returns the user's name.

If the user name is hierarchical, @V3UserName returns
the name in abbreviated format (omitting the CN, OU, O, and C identifiers).
To return the name in canonical format, use @UserName.

Note: If you used @UserName in Release 3 of NotesÂ®, it is automatically converted to @V3UserName
in Release 4 and later to preserve the functionality of your formulas.
If you change those formulas to use @UserName, be sure to recompile
them. If you use @UserName in Release 4 or later, a database created
in Release 3 does not recognize the formula until you upgrade that
database. If the formula will be evaluated in Release 3, use @V3UserName.

## Syntax

**@V3UserName**

## Return value

*name*

Text. The current user name
or server name.

## Usage

When
a formula runs on a server, the server is considered the current user,
so @V3UserName returns the name of the server. We do not recommend
using @V3UserName in a public view. Doing so produces unpredictable
results.

One use for @V3UserName is to display only those documents
relevant to the current user. For example, your Service Request database
could use @V3UserName in the private view named Assignments to display
each technician's assignments, weeding out everyone else's:

SELECT
@V3UserName=AssignedTo

However, the user can still design a
different private view that retrieves all documents, so don't depend
on @V3UserName as a security mechanism.

## Examples

1. @V3UserName returns Robert T. Smith if this is the name associated
   with the current user ID and returns Robert T. Smith/LA/Deli if this
   is the hierarchical name associated with the user ID.
2. @Right(@V3UserName;" ") returns Fletcher if William Fletcher is
   the name associated with the current user ID.

   If the user ID is
   hierarchical, the following code returns Fletcher:

   ```
   @Right(@Name([CN]; @V3UserName); " ")
   ```
3. @UpperCase(@Right(@V3UserName;" ")) returns FLETCHER if William
   Fletcher is the name associated with the current user ID.

   If the
   user ID is hierarchical, the following code returns FLETCHER:

   ```
   @UpperCase(@Right(@Name([CN]; @V3UserName); " "))
   ```
4. Given this hierarchical user ID:

   ```
   CN=Mary Tsen/OU=Illustration/OU=Documentation/OU=Development/
   OU=R&D/O=WorkSavers/C=US
   ```

   @V3UserName returns the
   name in abbreviated format:

   ```
   Mary Tsen/Illustration/Documentation/Development/R&D/WorkSavers/US
   ```

   To
   return the name in canonical format (using the CN, OU, O, and C identifiers),
   use @UserName.

---

## @V4UserAccess

# @V4UserAccess (Formula Language)

Given a server and file name, indicates the current user's
level of access to the database.

Note: This @function is new with Release 5.
If you used [@UserAccess](H_WHATISUSERACCESS.html "Given a server and file name, indicates the current user's level of access to the database.") in
Release 4, it is automatically converted to @V4UserAccess in Release
5 or later to preserve the functionality of your formulas. With Release
5 and later, more user access information is returned by @UserAccess.
If you change those formulas to use @UserAccess, be sure to recompile
them under the later release. If you use @UserAccess in Release 5
or later, a database created in Release 4 does not recognize the formula
until you upgrade it. If the formula will be evaluated in Release
4, use @V4UserAccess.

## Syntax

**@V4UserAccess(**  *server*  **:**  *file*  **)**

## Parameters

*server*

Text.
The name of the server. Use an empty string ("") to indicate the local
computer.

*file*

Text. The path and file name
of the database. Specify the database's path and file name using the
appropriate format for the operating system.

## Return value

*level*  **;**  *create*  **;**  *delete*

Text
list.

* *level* is a number from 1 to 6 that indicates the user's
  access level to the database.

  | Level | User's access level |
  | --- | --- |
  | 1 | Depositor |
  | 2 | Reader |
  | 3 | Author |
  | 4 | Editor |
  | 5 | Designer |
  | 6 | Manager |
* *create* is a number that returns 1 (True) if the user can
  create documents in the database, and 0 (False) if not.
* *delete* is a number that returns 1 (True) if the user can
  delete documents from the database, and 0 (False) if not.

On a local database without "Enforce a consistent Access
Control List," @V4UserAccess always returns 6; 1; 1. If the current
user has No Access to the database, Notes/Domino displays a message:
"You are not authorized to perform that operation."

## Usage

This
function does not work in column or selection formulas, or in agents
that run on a server (mail and scheduled agents).

## Examples

1. This formula returns 4; 1; 1 if the user has Editor access, permission
   to create documents, and permission to delete documents, in a database
   with the path of DSource\lookup.nsf on server Galactica/Space/Federation.

   ```
   @V4UserAccess( "Galactica//Space//Federation" : "dsource\\lookup.nsf" )
   ```
2. This formula returns 6;1;1, despite the user's access level and
   permissions, since the customer.nsf database is running on the local
   server. Or if the user has No Access to the database, "You are not
   authorized to perform that operation" displays instead.

   ```
   @V4UserAccess("":"\\Notes\\Data\\customer.nsf")
   ```
3. This formula returns 6; 1; 0 if the user has Manager access, permission
   to create documents, and no permission to delete documents in the
   current database if the database is running on a server other than
   the local server.

   ```
   @V4UserAccess( @DbName )
   ```

---

## @ValidateInternetAddress

# @ValidateInternetAddress (Formula Language)

Validates an Internet address based on the RFC 822 or RFC 821 Address
Format Syntax.

Note: This @function is new with Release 5.

## Syntax

**@ValidateInternetAddress(
[**  *addressFormat*  **] ;** *address* **)**

## Parameters

**[**  *addressFormat*  **]**

Keyword.
Specifies the formatting with which to validate an Internet address.
Can be one of the following keywords:

**[ADDRESS821]**

Requests
input address be validated based on RFC821 Address Format Syntax.

SStreitfeld@gazette.com

**[ADDRESS822]**

Requests
input address be validated based on RFC822 Address Format Syntax.

"Streitfeld,
Sara (Miami)" <SStreitfeld@gazette.com>

*address*

Text
or text list. Input address string

## Return value

Text or text list.

* If validation is successful, an empty string is returned.
* If validation fails, an error message string is returned to the
  user specific to the failure. More error message strings will be added
  in the future as necessary.

## Possible error messages

```
Invalid Input Parameter
```

Invalid
parameters to @function - @ValidateInternetAddress.

```
Invalid RFC821 syntax, no Phrase required.
```

When
a phrase is present in an address requiring an RFC821 syntax.

```
Invalid Phrase or character found.
```

Phrase
part of 822 address invalid.

```
Invalid Quoted String or mismatched quotes found.
```

Quoted
string is invalid within the address.

```
Invalid comment or mismatched parenthesis found.
```

Embedded
(comment(s)) within address is invalid.

```
Invalid or missing Domain.
```

Invalid
or missing Domain part of Address.

```
Invalid LocalPart or character found.
```

Invalid
LocalPart specified.

## Usage

@ValidateInternetAddress
is currently used in location records to validate Internet address
fields as well as in mail forms. This function is most useful in field
validation formulas where users are asked to input their Internet
address or in computed fields where Internet addresses are inherited.

If
the second parameter is a list, the function operates on each element
of the list, and the return value is a list with the same number of
elements.

Note: Multi-byte, or 8-bit characters, are
allowed in the Phrase part of an RFC 822 format Internet address.
They are not allowed anywhere else. Also, the Group syntax (that is,
several Internet addresses combined into one group name, such as "Customers")
is not supported in the validator.

## Examples

1. You have designed a form asking the user to input an Internet
   address. The user enters a standard RFC 821 format Internet address
   SStreitfeld@gazette.com in the editable field User\_Address.

   If you
   enter the field validation formula

   ```
   validateAddress := @ValidateInternetAddress([Address821]; User_Address);
   @If(validateAddress != ""; @Failure(validateAddress); @Success)
   ```

   the validation formula returns
   an empty string indicating a successful validation.

   However
   if you enter

   ```
   "Streitfeld, Sara (Miami)" <SStreitfeld@gazette.com>
   ```

   the
   validation formula returns the following error message:

   ```
   "Invalid RFC821 syntax, no Phrase required."
   ```
2. The following example returns "Invalid RFC821 syntax, no Phrase
   required." and "OK" in a list.

   ```
   User_Address1 := {"Streitfeld, Sara (Miami)" <SStreitfeld@gazette.com>};
   User_Address2 := {SStreitfeld@gazette.com};
   @Replace(@ValidateInternetAddress(
   [Address821]; User_Address1 : User_Address2); ""; "OK")
   ```
3. The following example returns "Not OK" if any of the elements
   in the SendTo field validate to an error message and "OK" if every
   element validates to an empty string.

   ```
   @If(@ValidateInternetAddress([Address821]; SendTo) != ""; "Not OK"; "OK")
   ```

   Note: Testing the validated list for equality to an empty string
   does not work because @True is returned if any element is an empty
   string even if others contain error messages.

---

## @VerifyPassword

# @VerifyPassword (Formula Language)

Compares two passwords.

Note: This @function is new with Release 6.

## Syntax

**@VerifyPassword(**  *password*  **;**  *password*  **)**

## Parameters

*password*

Text.
This can be a text expression or a password field name.

## Return value

*flag*

Boolean.

* Returns 1 (True) if the passwords are equivalent.
* Returns 0 (False) if the passwords are not equivalent.

## Usage

Use
this function to verify which password format, @Password or @HashPassword,
was used to encode a password field.

## Examples

1. This example returns true:

   ```
   @VerifyPassword("tolstoy";@HashPassword("tolstoy"))
   ```
2. This example returns false because the hashed string contains
   an upper-case T:

   ```
   @VerifyPassword("tolstoy";@HashPassword("Tolstoy")
   ```
3. If the access field is a password field containing the string,
   "He++llo", this code returns true:

   ```
   @VerifyPassword(access;@Password(access))
   ```
4. This code returns false because the @HashPassword and @Password
   functions use different formats to encode the contents of the access
   field:

   ```
   @VerifyPassword(@HashPassword(access);@Password(access))
   ```

---

## @Version

# @Version (Formula Language)

Returns the release number of the Notes/Domino software
you're running.

## Syntax

**@Version**

## Return value

*versionNumber*

Text. The release number.

## Usage

In
column, selection, mail agent, and scheduled agent formulas, @Version
returns the release number of the Notes/Domino server or workstation
containing the database. In all other formulas, @Version returns the
release number of the Notes/Domino workstation running the formula.

The
following table maps the numbers returned by @Version to each Notes/Domino
version.

| Number Returned by @Version | Corresponding Notes/Domino version |
| --- | --- |
| 114 | NotesÂ® 3.x |
| 136 | NotesÂ® 4.0, 4.0x |
| 138 | NotesÂ® 4.1, 4.1x |
| 145 | NotesÂ® 4.5, 4.5x |
| 147 | NotesÂ® 4.6 |
| 166 | NotesÂ® 5.0, 5.0x |
| 190 | NotesÂ® 6.0, 6.0.1 |
| 191 | NotesÂ® 6.0.2 |
| 194 | NotesÂ® 6.0.3, 6.5 |
| 198 | NotesÂ® 6.5.5 |
| 199 | NotesÂ® 6.5.6 |
| 256 | NotesÂ® 7.0 |
| 261 | NotesÂ® 7.0.1 |
| 265 | NotesÂ® 7.0.2 |
| 266 | NotesÂ® 7.0.3 |
| 307 | NotesÂ® 8.0 |
| 322 | NotesÂ® 8.0.1 |
| 323 | NotesÂ® 8.0.2 |
| 359 | NotesÂ® 8.5 |
| 368 | NotesÂ® 8.5.1 |
| 400 | Notes 9.0 |
| 405 | Notes 9.0.1 |
| 450 | Notes 10.0, 10.0.1 |
| 451 | Notes 11.0 |
| 452 | Notes 11.0.1 |
| 461 | Notes 12.0 |
| 470 | Notes 12.0.1 |
| 475 | Notes 12.0.2 |
| 485 | Notes 14.0 |
| 495 | Notes 14.5 |

Note the following:

* @Version returns the same number for all releases of NotesÂ® 3.x.
* @Version doesn't distinguish between the maintenance releases
  of NotesÂ® 4.x.

---

## @ViewShowThisUnread

# @ViewShowThisUnread (Formula Language)

Changes a view to show only unread documents, or to show read and unread documents.

Note: This @function is new with Release 6.5.

## Syntax

**@ViewShowThisUnread(** 
*unreadOnly*
 **)**

## Parameters

*unreadOnly*

Text.

* The value "1" shows only unread documents.
* The value "0" (or any value but "1") shows read and unread documents.

## Return value

*flag*

Number. Returns 1 (True).

## Usage

This @function is intended for use in view actions.

This @function does not work on the web.

---

## @ViewTitle

# @ViewTitle (Formula Language)

Returns the current view's name. If there are aliases and
synonyms, they are returned in a text list.

## Syntax

**@ViewTitle**

## Return value

*title*

Text or text list.

## Usage

This
function works in toolbar button, hotspot, or form action formulas,
if the formula opens to a view using an @command such as FileOpenDatabase.
It can be used in hide-when formulas for view action bars, but not
for other hide-when formulas. Returns the name of the view that was
last accessed when used in field, form action, section editor, or
window title formulas or null if no view has been accessed. It does
not work in column, selection, mail agent, paste agent, or scheduled
agent formulas.

## Examples

1. This example returns Main View if that is the title of the current
   view.

   ```
   @ViewTitle
   ```
2. This example returns "Main View":"By Date" if the view name is
   Main View|By Date.

   ```
   @ViewTitle
   ```
3. This example returns MAIN VIEW if the title of the current view
   is "main view" in any combination of uppercase and lowercase letters.

   ```
   @UpperCase(@ViewTitle)
   ```

---

## @WebDbName

# @WebDbName (Formula Language)

Returns the name of the current database encoded for URL
inclusion.

Note: This @function is new with Release 6.

## Syntax

**@WebDbName**

## Return value

*databaseName*

Text. The URL encoded
name of the database.

## Usage

The
return value can be placed as is in a URL command.

URL encoding
changes most special characters to the text %xx where xx is a hexadecimal
number representing the value of the character. In particular, spaces
are changed to %20.

A backslash (\) is changed to a forward
slash (/) rather than encoded. Double backslashes (\\) are removed.
Dashes (-) are passed through as is.

The file extension starting
with the period is not encoded.

This function is most effective
when used in Web applications. When executed from the NotesÂ® client, with @URLOpen, for example, specify
the host name before this function or the URL command will not execute
properly:

```
@URLOpen("//hostname/" + @WebDbName + "/viewname?OpenView")
```

## Examples

In an application accessed from the
Web, this action opens "View A" in the current database. Note that
"View+A" could also be written as "View%20A" in the formula.

```
@URLOpen(@WebDbName + "/View+A?OpenView")
```

---

## @Weekday

# @Weekday (Formula Language)

Computes the day of the week and returns a number that
identifies the day.

## Syntax

**@Weekday(**  *time-date*  **)**

## Parameters

*time-date*

Time-date
or time-date list. The date having the weekday value you want.

## Return value

*weekdayNumber*

Number or number list.
Weekday numbers are 1 through 7, with Sunday = 1, Monday = 2, and
so on.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 5.

   ```
   @Weekday([9/29/88])
   ```
2. This example returns 2 if the date in the response field happens
   to fall on a Monday.

   ```
   @Weekday(ResponseDate)
   ```
3. This example returns the string Working on the Weekend if the
   contents of the field named ResponseDate is 7 (Saturday) or 1 (Sunday);
   otherwise, it returns the date the document was created as a text
   string.

   ```
   @If(@Weekday(ResponseDate) = 7 | @Weekday(ResponseDate) = 1;"Working on the Weekend";@Text(@Created))
   ```
4. This example returns 5, 6, and 7 in a list.

   ```
   @Weekday([9/29/88] : [9/30/88] : [10/1/88])
   ```

---

## @WhichFolders

# @WhichFolders (Formula Language)

Returns the names of the folders containing the current
document.

Note: This @function is new with Release 8.5.1.

## Syntax

**@WhichFolders**

## Return value

*folderNames*

Text or text list. Names
of the folders containing the current document.

## Usage

@WhichFolders
is intended for use only as a column formula in the mail template.

This
function is effective only when the view is open in the UI and the
outline pane on the left is visible.

## Examples

This is a column formula in the ($All)
view of the mail template mail85.ntf. When the user opens the view,
this column displays the names of the folders containing the document
in that row.

```
@WhichFolders
```

---

## @While

# @While (Formula Language)

Executes one or more statements iteratively while a condition
is true. Checks the condition before executing the statements.

Note: This
@function is new with Release 6.

## Syntax

**@While(**  *condition*  **;**  *statement*  **;**  *...*  **)**

## Parameters

*condition*

Expression
that returns a value of True (1) or False (0).

*statement*

A
formula language statement. The maximum number of statements you can
include is 254.

## Return value

*true*

True (1) unless an error occurs
during execution of the condition. An "unexpected data type" error
occurs if the conditional expression results in a non-numeric value.

## Usage

@While
evaluates the condition. If the condition is True (1), @While executes
the statements then evaluates the condition again. If the condition
is False (0), @While terminates. Typically one of the statements should
change a value in the conditional expression so that the loop stops
at some point.

Tip: If you are looping
through a field containing a list, be sure the Allow multiple values
check box is selected in the Field Properties box for the list field.

For
other iterative statements, see [@DoWhile](H_DOWHILE_FUNCTION.html "Executes one or more statements iteratively while a condition is true. Checks the condition after executing the statements.") and [@For](H_FOR_FUNCTION.html "Executes one or more statements iteratively while a condition remains true. Executes an initialization statement. Checks the condition before executing the statements and executes an increment statement after executing the statements.").

## Examples

This agent displays the elements of
the Categories field one at a time.

```
n := 1;
@While(n <= @Elements(Categories);
@Prompt([OK]; "Category " + @Text(n); Categories[n]);
n := n + 1)
```

---

## @Wide

# @Wide (Formula Language)

Converts half-pitch alphanumeric characters (single-byte
characters -- SBCS) in the specified string to full-pitch alphanumeric
characters (double-byte characters -- DBCS). This function works in
Japanese, Korean, Simplified Chinese, and traditional Chinese environments.
In the Japanese environment, this function can convert half-pitch
Katakana as well.

Note: This @function is new with Release 5.

## Syntax

**@Wide(**  *string*  **)**

## Parameters

*string*

Text
or text list. The string you want to convert to double-byte characters.

## Return value

*returnstring*

Text or text list. The
string converted to double-byte characters.

## Usage

This
function can be used in input translation formulas to convert a field's
contents to double-byte characters or in computed field formulas to
save space for displaying a string.

If the parameter is a list,
the function operates on each element of the list, and the return
value is a list with the same number of elements.

## Examples

1. This input translation formula returns "Tokyo" as full-pitch characters,
   if the Location field contains a half-pitch character expression of
   "Tokyo."

   ```
   @Wide(Location)
   ```
2. This computed field formula returns "New York" as full-pitch characters,
   to save space for displaying the string.

   ```
   @Wide("New York")
   ```
3. This computed field formula returns "Tokyo" and "New York" as
   full-pitch characters, to save space for displaying the string.

   ```
   @Wide("Tokyo" : "New York")
   ```

---

## @Word

# @Word (Formula Language)

Returns the specified word from a text string. A "word"
is defined as the part of a string that is delimited by the defined
separator character. For example, if you specify a space (" ") as
the separator, then a word is any series of characters preceded by
and followed by a space (or the beginning or end of the string).

**@Word(**  *string*  **;**  *separator*  **;**  *number*  **)**

## Syntax

*string*

Text
or text list. The string you want to scan.

*separator*

Text.
The character that you want used to delimit a word in the *string.*

*number*

Number.
A position indicating which word you want returned from *string.* A
positive number refers to the position of the word starting from the
beginning where 1 is the first word. A negative number refers to the
position of the word starting from the end where -1 is the last word.

## Return value

*word*

Text
or text list. The word that holds the position specified by the *number* in
the *string*; for example, if *number* is 3, @Word returns
the third word in the *string*. If a text list is used, @Word
returns (in list format) a word from each list that holds the specified
position. Returns an empty string if *number* is out of range,
except that 0 is equivalent to 1.

## Examples

1. This example returns Collins,.

   ```
   @Word("Larson, Collins, and Jensen"; " " ; 2)
   ```
2. This example returns Collins,:Marketing,.

   ```
   @Word("Larson, Collins, and Jensen":"Sales, Marketing, and Administration";" ";2)
   ```
3. This example returns M.; here, the specified separator is the
   comma. The string contains 3 words: Larson, James, and M.

   ```
   @Word("Larson,James,M.";",";3)
   ```
4. This example returns Larson if James Larson is the name associated
   with the current user ID. It returns M. if James M. Larson is the
   name associated with the current user ID.

   ```
   @Word(@Username;" ";2)
   ```
5. This example returns Larson if James Larson is the name associated
   with the current user ID. It also returns Larson if James M. Larson
   is the name associated with the current user ID.

   ```
   @Word(@Username;" ";-1)
   ```

---

## @Year

# @Year (Formula Language)

Extracts and returns the year from the specified time-date
value.

## Syntax

**@Year(**  *time-date*  **)**

## Parameters

*time-date*

Time-date
or time-date list. The time-date of the year you want.

## Return value

*year*

Number
or number list. The year of *time-date.* @Year returns the year
relative to the time zone in which the date was generated. Returns
-1 if the time-date provided contains only a time value and not a
date.

## Usage

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Examples

1. This example returns 1995.

   ```
   @Year([9/29/95])
   ```
2. This example returns 1995 and 2008.

   ```
   @Year([9/29/95] : [9/29/08]
   ```

---

## @Yes

# @Yes (Formula Language)

Returns the value 1.

## Syntax

**@Yes**

## Return value

*yes*

Number.
The number 1.

## Usage

This
function is equivalent to @True.

## Examples

1. This example returns 1.

   ```
   @Yes
   ```
2. This example returns 1 if the value in the Cost field is greater
   than 100.

   ```
   @If(Amount > 1000; @Yes; !(@UserRoles = "[Manager]")
   ```

---

## @Yesterday

# @Yesterday (Formula Language)

Returns the time-date value which corresponds to yesterday's
date.

## Syntax

**@Yesterday**

## Return value

*yesterday*

Time-date. Yesterday's
date.

## Usage

Using
@Yesterday in column or selection formulas may impact the efficiency
of your application. It also causes the view refresh indicator to
display constantly.

In a field formula, Notes/Domino takes
the value for @Yesterday from the clock in the client computer.

## Examples

1. This example returns 12/31/92 if today is January 1, 1993.

   ```
   @Yesterday
   ```
2. This example returns 8/16/93 if today is August 17, 1993.

   ```
   @Yesterday
   ```

---

## @Zone

# @Zone (Formula Language)

Returns the time zone setting of the current computer or
of a time-date value, and indicates if daylight-saving time is observed.

The time zone is represented as the number of hours that
must be added to the time-date to convert it to Greenwich Mean Time.

## Syntax

**@Zone
@Zone(**  *timeDate*  **)**

## Parameters

*timeDate*

Time-date
or time-date list. Optional. The time-date whose zone you want to
know. You must specify both a date and a time; otherwise, @Zone returns
0.

## Return value

*zoneNumber* **.** *dstFlag*

Number
or number list. The time zone, followed by a period, followed by a
flag indicating daylight-saving time.

* For time zones east of GMT, *zoneNumber* is negative.
* For time zones west of GMT, *zoneNumber* is positive.
* When you use @Zone with no parameter, and daylight-saving time
  is being observed on the current computer, *dstFlag* is 1. If
  daylight-saving time is not being observed, only the *zoneNumber* is
  returned.
* When you use @Zone with a parameter, and the specified date falls
  within the daylight-saving time boundary, *dstFlag* is 1. If
  the date does not fall within daylight-saving time, only the *zoneNumber* is
  returned.

## Usage

When
used without a parameter, @Zone returns the zone and daylight-saving
time setting of the current computer.

When used with the parameter *currentTimeDate*,
@Zone returns the zone and daylight-saving time setting of *currentTimeDate*.

If
the parameter is a list, the function operates on each element of
the list, and the return value is a list with the same number of elements.

## Time zones that are not full-hour increments from GMT

For time zones
that are not a full hour increment from GMT, the return value is:

*mmhh* **.** *dstFlag*

*mm* is
the minutes component of the time relative to GMT.

*hh* is
the hours component of the time relative to GMT

*dstFlag* is
.1 if daylight-saving time is being observed. Otherwise, only the *mmhh* is
returned.

For example, on a computer with a time zone setting
eleven and a half hours west of GMT, with daylight-saving time disabled,
@Zone returns: 3011

On a computer with a time zone setting
ten and three-quarter hours west of GMT, with daylight-saving time
enabled, @Zone returns: 4510.1

On a computer with a time zone
setting nine and a half hours east of GMT, with daylight-saving time
enabled, @Zone returns: -3009.1

## Examples

1. This example returns:
   * 5.1 for Eastern Standard Time and daylight-saving time observed.
   * 5 for Eastern Standard Time and daylight-saving time not observed.
   * 6 for Central Standard Time and daylight-saving time not observed.
   * 7.1 for Mountain Standard Time and daylight-saving time observed.
   * 8.1 for Pacific Standard Time and daylight-saving time observed.

     ```
     @Zone
     ```
2. This example returns 5 if in the Eastern Standard time zone.

   ```
   @Zone([1/26/94 11:00 AM])
   ```
3. This example returns 5.1 if in the Eastern Standard time zone
   and daylight-saving time is observed, 5 if daylight-saving time is
   not observed.

   ```
   @Zone([5/28/94 11:00 AM])
   ```
4. This example returns 5 and 5.1 in a list if in the Eastern Standard
   time zone.

   ```
   @Zone([1/26/94 11:00 AM] : [5/28/94 11:00 AM])
   ```

---

## @IfError

# @IfError (Formula Language)

Returns a null string ("") or the value of an alternative statement if a statement returns an error.

Note: This @function is new with Release 6.

Note: This @function is obsolete in Release 7.

## Syntax

**@IfError(** _statement1_ **;** _statement2_ **)**

## Parameters

_statement1_

A formula statement. This statement executes first.

_statement2_

Optional. A formula statement. This statement, if available, executes if the first statement returns an error.

## Return value

_statementReturn_

*   Returns the value of the first statement if it is not an error.
*   Returns the value of the second statement if the value of the first statement is an error and the second statement is supplied.
*   Returns a null string ("") if the value of the first statement is an error and the second statement is omitted.

## Usage

Use $Error in the second statement to get the value of the error.

This command should be replaced with the following series of commands.

```
result := statement1;
@If(@IsError(result);statement2;result)
```

Since this function intercepts the error message and replaces it with your own value, if you do have an error, you may have trouble figuring out what's causing the error. For debugging purposes, you may want to temporarily remove the error handling so that you can see the error message text or, display the text as shown in example 4.

## Examples

1.  This agent tests the return value of an @DbLookup statement for an error. If the @DbLookup statement causes an error, the agent returns the text "Not available."
    
    ```
    FIELD Phone :=
    @IfError(
    @DbLookup(""; "Snapper" : "names.nsf"; "People";
    @Right(Name; " ") + " , " + @Left(Name; " "); "OfficePhoneNumber");
    "Not available")
    ```
    
    This agent does the same thing, using @If instead of @IfError.
    
    ```
    result := @DbLookup("";"Snapper":"names.nsf";"People";
    @Right(Name;" ") + " , " + @Left(Name; " "); "OfficePhoneNumber");
    FIELD Phone := @If(@IsError(result);"Not available";result)
    ```
    
2.  The following code, when added to a Computed for display field, displays the price of the product entered in the "product" field, after a page refresh. Enter the text, "Enter product name here" as the default value for the product field. Once a user enters a product name in the product field and presses F9, the price is extracted from the Goods view, which contains the product name in the first sorted column and its price in the second column. If the product name is not recognized or any other error occurs during the lookup, the message, "Unable to retrieve requested price. Aborting lookup" displays. You could add a Get Price action button that contains the code: @Command(\[ViewRefreshFields\]) to prompt the user to refresh the page.
    
    ```
    @If(product="Enter product name here";0;@IfError(@DbLookup("" : "" ; "product/server" : "filename\\productdatabase.nsf" ; "Goods" ; product ; 2); "Unable to retrieve requested price. Aborting lookup"))
    ```
    
3.  This formula, when added to the "Apply font" hotspot button, applies the font a user selects from the "fonts" Dialog list field to the text the user enters or highlights in the "Body" Rich Text field. The "fonts" field contains an @FontList function in the Use formula for choices box in its Field Properties box, which displays a list of available fonts. If no font was selected from the "fonts" field, an error message displays which instructs the user to select one.
    
    ```
    @Command([EditGoToField]);"Body");
    @Command([EditSelectAll]);
    @IfError(@Command([TextSetFontFace];fonts);@Prompt([Ok];"Error encountered";"You must select a font first"))
    ```
    
4.  This returns the lookup result if there is one, but if the lookup fails, it returns the text of the error message without causing an error condition. This may be useful in debugging.
    
    ```
    @IfError(@DbLookup("":"NoCache"; ""; "ById"; ID; 2); 
    @Text($Error))
    ```

---

## @IsAppInstalled

# @IsAppInstalled (Formula Language)

Indicates whether the specified type of application is installed.

Note: This @function is new with Release 5.

## Syntax

**@IsAppInstalled(** _application_ **)**

## Parameters

_application_

Text. Specify "Designer" to check if the Domino® Designer is installed on the system, or "Admin" to check if the Domino® Administrator is installed.

## Return value

_flag_

Boolean

*   True indicates that the specified application is installed
*   False indicates that the specified application is not installed

## Usage

This @function is generally used in hide-when formulas.

---

## @LanguagePreference

# @LanguagePreference (Formula Language)

Returns user's specified preferred language setting.

Note: This function is new with Release 5.

## Syntax

**@LanguagePreference (** **\[** _key_ **\] )**

## Parameters

**\[** _key_ **\]**

Keyword. Specify a category for which you would like to get the preferred language. The following categories are available:

**\[REGION\]**

Returns preferred language for region.

**\[CONTENT\]**

Returns preferred language for database contents.

**\[ALTERNATENAME\]**

Returns preferred language for alternate name.

## Return value

_preferredlanguage_

Text or Text list. Language and country code for user's preferred setting. \[REGION\] language is set as the default. If @LanguagePreference cannot find the language setting for the specified category, it returns the language for \[REGION\].

## Usage

@LanguagePreference is used to implement mechanisms for handling language-dependent features. A database that is designed to store data in multiple languages can select the language in which the data should be published for each user by using @LanguagePreference\[Content\].

@LanguagePreference supports the Web browser client. When the browser client calls @LanguagePreference, it returns a list of languages specified in the Web browser. This returned list is normalized based on the _key_ parameter of the @function.

## Examples

1.  The following example returns "fr" if your region language setting is French.
    
    ```
    @LanguagePreference([REGION])
    ```
    
2.  The following example returns "en" if you call this function from the Web client and your Web browser's accept language is "English(United States)."
    
    ```
    @LanguagePreference([ALTERNATENAME])
    ```

---

## @LaunchApp

# @LaunchApp (Formula Language)

Launches the requested Domino® application.

Note: The @function is new with Release 5.

## Syntax

**@LaunchApp(** _application_ **)**

## Parameters

_application_

Text. The type of application you want to launch. Specify any one of the following:

| Notes® | This launches the Notes® client. |
| --- | --- |
| Designer | This launches Domino® Designer, if installed. |
| Admin | This launches Domino® Administrator, if installed. |

## Usage

If the requested application is already running, it will be brought to the front and it will have focus.

This @function is generally used in action formulas.

You cannot use this function in Web applications.

---

## @Locale

# @Locale (Formula Language)

Returns information about language codes.

Note: This @function is new with Release 5.

## Syntax

**@Locale( \[** _action_ **\] )**

**@Locale(** **\[** _action_ **\]** _; locale-tag_ **)**

## Parameters

**\[** _action_ **\]**

Keyword. One of the following:

**\[NotesLocale\]** without _locale-tag_ returns a text list containing all the content language codes.

**\[NotesLocale\]** with _locale-tag_ returns a text list or value containing each specified content language code, or a null string if the code is not recognized. A code is recognized if it is exact regardless of case. If the language is recognized but not the country or region, the language code alone is returned.

**\[AltNameLocale\]** without _locale-tag_ returns a text list containing all the alternate name language codes.

**\[AltNameLocale\]** with _locale-tag_ returns a text list or value containing each specified alternate user language code, or a null string if the code is not recognized. A code is recognized if it is exact regardless of case. The country or region is ignored where it is not part of the alternate user language code (most cases).

**\[LanguageName\]** with _locale-tag_ returns a text list or value spelling out the language for each specified language code, or a null string if the code is not recognized.

**\[CountryName\]** with _locale-tag_ returns a text list or value spelling out the country or region for each specified language code, or a null string if the code has no country or region, or it is not recognized.

**\[LocaleName\]** with _locale-tag_ returns a text list or value spelling out the language and country (or region), if applicable, for each specified language code, or a null string if the code is not recognized. The country or region is in parentheses and immediately (no space) follows the language.

**\[LocaleName\] : \[NotesLocale\]** (concatenating these two keywords) returns a text list containing, for each content language code, the language name, the country or region name in parentheses, a vertical bar, and the language code. This list can be used in a keyword field where the locale name is the name and the language code is the alias.

**\[LocaleName\] : \[AltNameLocale\]** (concatenating these two keywords) returns a text list containing, for each alternate name language code, the language name, the country or region name in parentheses, a vertical bar, and the language tag. This list can be used in a keyword field where the locale name is the name and the language code is the alias.

_locale-tag_

Text or text list. A language code or list of language codes.

## Supported language codes

| Tag | Language | Country or region | Locale |
| --- | --- | --- | --- |
| af | Afrikaans |  | Notes® |
| ar | Arabic |  | Notes® & AltName |
| ar-AE | Arabic | United Arab Emirates | Notes® |
| ar-BH | Arabic | Bahrain | Notes® |
| ar-DZ | Arabic | Algeria | Notes® |
| ar-EG | Arabic | Egypt | Notes® |
| ar-JO | Arabic | Jordan | Notes® |
| ar-KW | Arabic | Kuwait | Notes® |
| ar-LB | Arabic | Lebanon | Notes® |
| ar-MA | Arabic | Morocco | Notes® |
| ar-OM | Arabic | Oman | Notes® |
| ar-QA | Arabic | Qatar | Notes® |
| ar-SA | Arabic | Saudi Arabia | Notes® |
| ar-TN | Arabic | Tunisia | Notes® |
| ar-YE | Arabic | Yemen | Notes® |
| be | Byelorussian |  | Notes® & AltName |
| bg | Bulgarian |  | Notes® & AltName |
| ca | Catalan |  | Notes® & AltName |
| cs | Czech |  | Notes® & AltName |
| cy | Welsh |  | Notes® & AltName |
| da | Danish |  | Notes® & AltName |
| de | German |  | Notes® & AltName |
| de-AT | German | Austria | Notes® |
| de-CH | German | Switzerland | Notes® |
| de-DE | German | Germany | Notes® |
| de-LI | German | Liechtenstein | Notes® |
| de - LU | German | Luxembourg | Notes® |
| el | Greek |  | Notes® & AltName |
| en | English |  | Notes® & AltName |
| en-AU | English | Australia | Notes® |
| en-CA | English | Canada | Notes® |
| en-GB | English | United Kingdom | Notes® |
| en-HK | English | Hong Kong | Notes® |
| en-IE | English | Ireland | Notes® |
| en-IN | English | India | Notes® |
| en-JM | English | Jamaica | Notes® |
| en-NZ | English | New Zealand | Notes® |
| en-PH | English | Philippines | Notes® |
| en-SG | English | Singapore | Notes® |
| en-US | English | United States | Notes® |
| en-ZA | English | South Africa | Notes® |
| es | Spanish |  | Notes® & AltName |
| es-AR | Spanish | Argentina | Notes® |
| es-BO | Spanish | Bolivia | Notes® |
| es-CL | Spanish | Chile | Notes® |
| es-CO | Spanish | Colombia | Notes® |
| es-CR | Spanish | Costa Rica | Notes® |
| es-DO | Spanish | Dominican Republic | Notes® |
| es-EC | Spanish | Ecuador | Notes® |
| es-ES | Spanish | Spain | Notes® |
| es-GT | Spanish | Guatemala | Notes® |
| es-HN | Spanish | Honduras | Notes® |
| es-MX | Spanish | Mexico | Notes® |
| es-NI | Spanish | Nicaragua | Notes® |
| es-PA | Spanish | Panama | Notes® |
| es-PE | Spanish | Peru | Notes® |
| es-PR | Spanish | Puerto Rico | Notes® |
| es-PY | Spanish | Paraguay | Notes® |
| es-SV | Spanish | El Salvador | Notes® |
| es-US | Spanish | United States | Notes® |
| es-UY | Spanish | Uruguay | Notes® |
| es-VE | Spanish | Venezuela | Notes® |
| et | Estonian |  | Notes® & AltName |
| fi | Finnish |  | Notes® & AltName |
| fr | French |  | Notes® & AltName |
| fr-BE | French | Belgium | Notes® |
| fr-CA | French | Canada | Notes® |
| fr-CH | French | Switzerland | Notes® |
| fr-FR | French | France | Notes® |
| fr-LU | French | Luxembourg | Notes® |
| gu | Gujarati |  | Notes® & AltName |
| he | Hebrew |  | Notes® & AltName |
| hi | Hindi |  | Notes® & AltName |
| hr | Croatian |  | Notes® & AltName |
| hu | Hungarian |  | Notes® & AltName |
| id | Indonesian |  | Notes® & AltName |
| is | Icelandic |  | Notes® & AltName |
| it | Italian |  | Notes® & AltName |
| it-CH | Italian | Switzerland | Notes® |
| it-IT | Italian | Italy | Notes® |
| ja | Japanese |  | Notes® & AltName |
| kk | Kazakh |  | Notes® & AltName |
| ko | Korean |  | Notes® & AltName |
| lt | Lithuanian |  | Notes® & AltName |
| lv | Latvian |  | Notes® & AltName |
| mk | Macedonian |  | Notes® & AltName |
| mr | Marathi |  | Notes® & AltName |
| ms | Malay |  | AltName |
| ms-MY | Malay | Malaysia | Notes® |
| mt | Maltese |  | Notes® & AltName |
| nl | Dutch |  | Notes® & AltName |
| nl-BE | Dutch | Belgium | Notes® |
| nl-NL | Dutch | Netherlands | Notes® |
| no | Norwegian |  | Notes® & AltName |
| no - NO | Norwegian | Norway | Notes® |
| ny - NO | Nynorsk | Norway | Notes® |
| pl | Polish |  | Notes® & AltName |
| pt | Portuguese |  | Notes® & AltName |
| pt-BR | Portuguese | Brazil | Notes® |
| pt-PT | Portuguese | Portugal | Notes® |
| ro | Romanian |  | Notes® & AltName |
| ro - MD | Romanian | Moldavia | Notes® |
| ro - RO | Romanian | Romania | Notes® |
| ru | Russian |  | Notes® & AltName |
| sk | Slovak |  | Notes® & AltName |
| sl | Slovenian |  | Notes® & AltName |
| sq | Albanian |  | Notes® & AltName |
| sr | Serbian |  | Notes® & AltName |
| sv | Swedish |  | Notes® & AltName |
| ta | Tamil |  | Notes® & AltName |
| te | Telugu |  | Notes® & AltName |
| th | Thai |  | Notes® & AltName |
| tr | Turkish |  | Notes® & AltName |
| uk | Ukrainian |  | Notes® & AltName |
| vi | Vietnamese |  | Notes® & AltName |
| x-KOK | Konkani |  | Notes® & AltName |
| zh-CN | Chinese | China | Notes® & AltName |
| zh-HK | Chinese | Hong Kong | Notes® |
| zh-MO | Chinese | Macau | Notes® |
| zh-SG | Chinese | Singapore | Notes® |
| zh-TW | Chinese | Taiwan | Notes® & AltName |

## Examples

1.  The following formulas return "French."
    
    ```
    @Locale([LanguageName]; "fr")
    @Locale([LanguageName]; "fr-CA")
    ```
    
2.  The following formula returns "Canada."
    
    ```
    @Locale([CountryName]; "fr-CA")
    ```
    
3.  The following formula returns "French(Canada)."
    
    ```
    @Locale([LocaleName]; "fr-CA")
    ```
    
4.  The following formula returns "fr-CA."
    
    ```
    @Locale([NotesLocale]; "FR-CA")
    ```
    
5.  The following formula returns "fr."
    
    ```
    @Locale([AltNameLocale]; "FR-CA")
    ```
    
6.  The following formula returns a list of all the content language codes.
    
    ```
    @Locale([NotesLocale])
    ```
    
7.  The following formula returns a list of all the alternate user name language tags.
    
    ```
    @Locale([AltUserLocale])
    ```
    
8.  The following field keyword formula returns a list of each content language code preceded by its locale name and a vertical bar. This formula allows the user to select from a list of names and stores the corresponding language code (which is an alias to the name).
    
    ```
    @Locale([LocaleName] : [NotesLocale])
    ```
    
    It is equivalent to:
    
    ```
    @Locale([LocaleName]; @Locale([NotesLocale]))
    + "|" + @Locale([NotesLocale])
    ```

---

## @NameLookup

# @NameLookup (Formula Language)

Searches for each specified user name across all Domino® Directories and returns a list of single text values for each specified user name.

Note: This @function is new with Release 5.

## Syntax

**@NameLookup( \[** _lookupType_ **\] ;** _username; itemtoreturn_ **)**

## Parameters

**\[** _lookupType_ **\]**

Keyword. Specifies the type of lookup to perform. Supply one or more of the following keywords, separated from each other with a colon:

**\[NoUpdate\]**

Default. Returns a list of user names. Corresponds to NAME\_LOOKUP\_NOUPDATE flag for Notes® API. You can specify this keyword along with the other keywords excluding **\[ForceUpdate\]**.

**\[ForceUpdate\]**

Forces the name space (view) to be updated. Corresponds to NAME\_LOOKUP\_UPDATE flag for Notes® API. You can specify this keyword along with the other keywords excluding **\[NoUpdate\]**.

The following keywords can be used along with the **\[NoUpdate\]** or **\[ForceUpdate\]**.

**\[NoSearching\]**

Searches only the first Domino® Directory containing the "($Users)" view, which is the local Names.nsf database, and returns a list of single text values for each specified user name. This keyword specifies to not retrieve values from the mail server's directory. An empty string is returned for no match found. Corresponds to NAME\_LOOKUP\_NOSEARCHING flag for Notes® API.

**\[Exhaustive\]**

Searches for each value in the username argument through all directories, even after a matching username has been found. Without this option, the search for each username ends with the first directory that contains a match. However, multiple matches may still be returned from that directory. This keyword returns values from the local Names.nsf database as well as the mail server's directory. If the mail server is unavailable, or the Recipient name type ahead setting in the current location document is Disabled or Local Only, it retrieves values from the current name server. If you are using LDAP, it also retrieves values from the LDAP directory. The user's value is omitted if there is no match found.

Without the \[Exhaustive\] argument, a value in the user's local address book may hide values in the server directories. For instance, suppose usernames = "Smith", and your local address book has a "Mary Smith" in it. Unless you specify \[Exhaustive\], @NameLookup will not notice the entry for "George Smith" in the server address book, and will only return information for Mary.

**\[TrustedOnly\]**

Searches only those Domino® Directories that contain trust information and returns a list of single text values for each specified user name. An empty string is returned for no match found. Corresponds to NAME\_LOOKUP\_TRUSTED\_NAMESPACES flag for Notes® API.

_username_

Text or text list. Specify primary or alternate Notes/Domino user names to retrieve their information from the Domino® Directory.

_itemtoreturn_

Text. Item or field name from the Domino® Directory Contact record that you would like to retrieve information from.

## Return value

_valuelist_

Text list. @NameLookup returns a list of single values for each matching user. If a given _username_ value matches multiple users, one entry will be returned for each matching user. Unless \[Exhaustive\] has been specified, an empty string is returned for usernames for which no match was found.

In no case will more than one value be returned from each matching contact document. If the requested field is multivalued (e.g. the FullName field), only the first value in the field is returned.

## Usage

@NameLookup cannot be used in form selection and view column formulas.

To enable server directory data to be included in this function's return value, the Recipient name type ahead setting in the Mail tab of the current location document must be set to Local Then Server.

All the users from secondary directories, including the LDAP directory, need to be authenticated first, and then authorized to access a Notes/Domino database administered by the Domino® server. The Directory Assistance derived from the Master Domino® Directory uses trusted name rules to authenticate users. Once a user name is authenticated, it is added to the list of trusted names. This user name is then compared to the ACL for authorization.

For more information on searching the LDAP directory, see "Setting up Notes® to search an Internet directory for addresses" in the Notes® Help.

## Examples

You have three Domino® Directories on your local environment, namely, Names\_A.nsf, Names\_B.nsf, and Names\_C.nsf. Each Directory has the following entries:

|  | Names_A.nsf | Names_B.nsf | Names_C.nsf |
| --- | --- | --- | --- |
| View: ($Users) | Does not exist | Exists | Exists |
| User: Katsushi | User: KatsushiItem: Katsushi_A | User: KatsushiItem: Katsushi_B | User: KatsushiItem: Katsushi_C |
| User: Jones | User: JonesItem: Jones_A | User: JonesItem: Jones_B1Item: Jones_B2 | Does not exist |
| User: Smith | User: SmithItem: Smith_A | Does not exist | User: SmithItem: Smith_C |
| User: Yoshito | Does not exist | Does not exist | Does not exist |

1.  The following formulas return "Katsushi\_B" : "Jones\_B1" : "Smith\_C" : ""
    
    ```
    @NameLookup ( [NoUpdate]; "Katsushi":"Jones":"Smith": 
                 "Yoshito"; "Item")
    @NameLookup ( [ForceUpdate]; "Katsushi":"Jones":"Smith": 
               "Yoshito"; "Item")
    ```
    
2.  The following formula returns "Katsushi\_B" : "Jones\_B1" :"" : ""
    
    ```
    @NameLookup ( [NoSearching]; "Katsushi":"Jones":"Smith": 
               "Yoshito"; "Item")
    ```
    
3.  The following formula returns "Katsushi\_B" : "Katsushi\_C" : "Jones\_B1" : "Jones\_B2" : "Smith\_C"
    
    ```
    @NameLookup ( [Exhaustive]; "Katsushi":"Jones":"Smith": 
               "Yoshito"; "Item")
    ```
    
4.  If the current user is a software engineer, the following code, when added as the default value for a field, displays SOFTWARE ENGINEER.
    
    ```
    @NameLookup([Exhaustive];@UserName;"JobTitle")
    ```

---

## @Narrow (Formula Language)

# @Narrow (Formula Language)

Converts full-pitch alphanumeric characters (double byte characters -- DBCS) in the specified string to half-pitch alphanumeric characters (single byte characters -- SBCS). This function works in Japanese, Korean, Simplified Chinese, and traditional Chinese environments. In the Japanese environment, this function can convert full-pitch Katakana as well.

Note: This @function is new with Release 5.

## Syntax

**@Narrow(** _string_ **)**

## Parameters

_string_

Text or text list. The string that you want to convert to single byte characters.

## Return value

_returnstring_

Text or text list. The string converted to single byte characters.

## Usage

If the parameter is a list, the function operates on each element of the list, and the return value is a list with the same number of elements.

This function can be used in input translation formulas to convert the contents of a field to single byte characters or in computed field formulas to save space for displaying the string.

## Examples

1.  This input translation formula returns "Tokyo" as half-pitch characters, if the Location field contains a full-pitch character expression of "Tokyo."
    
    ```
    @Narrow(Location)
    ```
    
2.  This computed field formula returns "New York" as half-pitch characters to save space for displaying the string.
    
    ```
    @Narrow("New York")
    ```
    
3.  This computed field formula returns the list "Tokyo" and "New York" as half-pitch characters to save space for displaying the string.
    
    ```
    @Narrow("Tokyo" : "New York")
    ```

