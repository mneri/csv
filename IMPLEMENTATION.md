# Transition Tables
The CSV grammar defines a [regular language](https://en.wikipedia.org/wiki/Regular_language). By definition, a regular
language is the set of strings recognised by a
[finite state automaton](https://en.wikipedia.org/wiki/Finite-state_machine) (FSA).

Under the hood, most CSV parsers are finite state automata. For example, when a parser encounters a double quote
character, it could transition to the `INSIDE_QUALIFIED_FIELD` state; a second double quote could make the parser
transition to the `END_QUALIFIED_FIELD` state. These would be two of the finite number of states in the parser.

There are many ways to implement a finite state automaton, one of which is to keep the current state in a variable. In
[`SimpleFlatMapper`](https://github.com/arnaudroger/SimpleFlatMapper), the author `arnaudroger` uses precisely this
technique.

```java
while (currentIndex < bufferSize) {
    final char character = chars[currentIndex];
    if (character == separatorChar) {
        // ...
        currentState = LAST_CHAR_WAS_SEPARATOR | ROW_DATA;
        continue;
    } else if (character == LF) {
        // ...
        currentState = NONE;
        continue;
    } else if (character == CR) {
        // ...
        currentState = LAST_CHAR_WAS_CR;
        continue;
    } // ...
}
```
In the example above[^1], the state of the parser changes by updating `currentState` and the change is driven by the flow of
the code: a cascade of `if-else` statements decides which state is next. This technique is simple and very effective
and, in fact, `SimpleFlatMapper` is one of the fastest Java CSV parsers in circulation.

`skjolberg `takes a slightly different approach. In [`sesseltjonna-csv`](https://github.com/skjolber/sesseltjonna-csv)
the state is kept _implicit_. There is no `currentState` variable; instead, the _execution point_ implicitly defines the
state of the automaton[^2].

```java
if (current[currentOffset] != quoteCharacter) {
    // Implicit state: AT_START_OF_FIELD
    // ...
    if (current[currentOffset] != divider) {
        // Implicit state: IN_UNQUALIFIED_FIELD
        // ...
    } else {
        // Implicit state: IN_EMPTY_FIELD
        // ...
    }
} else {
    // Implicit state: IN_QUALIFIED_FIELD
    // ...
}
```
Performance-wise, the implicit state technique works really well, and to my knowledge `sesseltjonna-csv` is currently
the fastest Java CSV parser. In both `SimpleFlatMapper` and `sesseltjonna-csv`, transitions from one state to the next
are not explicit, and their logic is pushed deep within the code. _Understanding state transitions with these two models
is difficult._

`mneri/csv` takes a different approach: all the different states are explicitly laid out in a transition table. Rows
represent states, columns represent input characters, and the intersection indicates the next state.

```
|                 | [A-Za-z0-9]     | ,               | \r              | \n              | "               | EOF         |
+-----------------+-----------------+-----------------+-----------------+-----------------+-----------------+-------------+
| BEFORE_LINE *   | FIELD           | BEFORE_FIELD    | CAR             | ERROR           | START_QUALIFIED | END_OF_FILE |
| BEFORE_FIELD    | FIELD           | BEFORE_FIELD    | CAR             | ERROR           | START_QUALIFIED | END_OF_FILE |
| FIELD           | FIELD           | BEFORE_FIELD    | CAR             | ERROR           | ERROR           | END_OF_FILE |
| START_QUALIFIED | QUALIFIED_FIELD | QUALIFIED_FIELD | QUALIFIED_FIELD | QUALIFIED_FIELD | ESCAPE          | ERROR       |
| QUALIFIED_FIELD | QUALIFIED_FIELD | QUALIFIED_FIELD | QUALIFIED_FIELD | QUALIFIED_FIELD | ESCAPE          | ERROR       |
| ESCAPE          | ERROR           | BEFORE_FIELD    | CAR             | ERROR           | QUALIFIED       | END_OF_FILE |
| CAR             | ERROR           | ERROR           | ERROR           | BEFORE_LINE     | ERROR           | ERROR       |
| END_OF_FILE     | ERROR           | ERROR           | ERROR           | ERROR           | ERROR           | ERROR       |
| ERROR           | ERROR           | ERROR           | ERROR           | ERROR           | ERROR           | ERROR       |
```
The transition table above can be used to parse a CSV file that is fully compliant with
[RFC 4180](https://datatracker.ietf.org/doc/rfc4180/). The initial state is `BEFORE_LINE` (marked with `*`). Upon
consuming the character `a` (first column), the state transitions to `FIELD`. From the `FIELD` state, a comma sets the
transition to `BEFORE_FIELD` (the "cursor" is positioned before the _next_ field). Transitions continue until either the
state `END_OF_FILE` or `ERROR` are reached. To be fully compliant with RFC 4180, the transition table must reject
malformed inputs. In the example above, `ERROR` is a _sink state_; once entered, it cannot be left because all outgoing
transitions loop back to itself.

_A transition table makes it easier to reason about the parser and document its behaviour._ All the states and
transitions are laid out clearly in a single point.

When transitioning from one state to the next, the parser shall perform some actions. Actions can be encoded in the
transition table along with the state changes. Below is an example for the `BEFORE_LINE` state shown above.

```
|               | [A-Za-z0-9]         | ,                             | \r               | \n                      | EOF                 |
+---------------+---------------------+-------------------------------+------------------+-------------------------+---------------------+
| BEFORE_LINE * | next:   FIELD       | next:   BEFORE_FIELD          | next:   CAR      | next:   ERROR           | next:   END_OF_FILE |
|               | action: START_FIELD | action: START_FIELD,END_FIELD | action: END_LINE | action: THROW_EXCEPTION | action: STOP        |
```
Consuming the character `a` while in state `BEFORE_LINE` makes the parser transition to the state `FIELD` and record the
start of a field (`START_FIELD` action).

# Formats
`mneri/csv` supports many different CSV dialects, each one implemented as a _separate transition table_ and enclosed in
a [`Format`](https://github.com/mneri/csv/tree/master/src/main/java/me/mneri/csv/format) implementation.

Transition tables are implemented using `int[]` and as explained before, states and actions are encoded together: the
low 16 bits of each element encode the target state, while the high 16 bits encode the corresponding actions. Below is
the transition table for `Rfc4180StrictFormat`. While it looks complex, it is actually quite straightforward.

```java
private static final int[] DFA = {
// *            ,            \r           \n           "            EOF                  padding
   FLD,         BFF|EFH,     CAR|EFH,     ERR|ERH,     ERR|ERH,     EOF|EFH|ELH|RPL,     0,0,  // FLD
   QOT,         QOT,         QOT,         QOT,         ESC,         ERR|ERH,             0,0,  // QOT
   FLD|SFH,     BFF|SFH|EFH, CAR|SFH|EFH, ERR|ERH,     SQT,         EOF|SFH|EFH|ELH|RPL, 0,0,  // BFF
   QOT|SFH,     QOT|SFH,     QOT|SFH,     QOT|SFH,     SQE,         ERR|ERH,             0,0,  // SQT
   ERR|ERH,     BFF|EFB,     CAR|EFB,     ERR|ERH,     QOT|RMB,     EOF|EFB|ELH|RPL,     0,0,  // ESC
   ERR|ERH,     BFF|SFH|EFH, CAR|SFH|EFH, ERR|ERH,     QOT|SFH,     EOF|SFH|EFH|RPL,     0,0,  // SQE
   FLD|SFH,     BFF|SFH|EFH, CAR|SFH|EFH, ERR|ERH,     SQT,         EOF|STP,             0,0,  // BFL *
   ERR|ERH,     ERR|ERH,     ERR|ERH,     BFL|ELH,     ERR|ERH,     ERR|ERH,             0,0,  // CAR
   ERR|ERH,     ERR|ERH,     ERR|ERH,     ERR|ERH,     ERR|ERH,     EOF|STP,             0,0,  // EOF
   ERR|ERH,     ERR|ERH,     ERR|ERH,     ERR|ERH,     ERR|ERH,     ERR|ERH,             0,0,  // ERR
// padding      padding      padding      padding      padding      padding              padding
   0,           0,           0,           0,           0,           0,                   0,0,
   0,           0,           0,           0,           0,           0,                   0,0,
   0,           0,           0,           0,           0,           0,                   0,0,
   0,           0,           0,           0,           0,           0,                   0,0,
   0,           0,           0,           0,           0,           0,                   0,0,
   0,           0,           0,           0,           0,           0,                   0,0};
```
Rows represent the current state, and columns represent classes of characters. Each line has a comment on the right
indicating the state, and the starting state is noted with an `*`. A line comment above the table labels the columns.
The first column is labelled with `*`, meaning any character other than those listed in the subsequent columns. Next, in
order, come the columns for `,`, `\r`, `\n`, `"`, and `EOF` (end of file).<br/>
States and actions appear in the code as three-letter mnemonics, shown in alphabetical order in the tables below.

```
| STATE MNEMONIC  | MEANING                                | STATE MNEMONIC  | MEANING                                |
|-----------------|----------------------------------------|-----------------|----------------------------------------|
| BFF             | before field                           | ESC             | escape in qualified field              |
| BFL             | before line                            | FLD             | field                                  |
| CAR             | carriage return                        | QOT             | qualified field                        |
| EOF             | end of file                            | SQE             | escape at the start of qualified field |
| ERR             | error                                  | SQT             | start of qualified field               |


| ACTION MNEMONIC | MEANING                                | ACTION MNEMONIC | MEANING                                |
|-----------------|----------------------------------------|-----------------|----------------------------------------|
| EFB             | end field at the previous position     | RMB             | remove the previous character          |
| EFH             | end field at the current position      | RPL             | replay the last character              |
| ELB             | end line at the previous position      | SFH             | start field at the current position    |
| ELH             | end line at the current position       | STP             | stop processing                        |
| ERH             | report error at this position          |                 |                                        |
```

Cells encode both the next state and the actions. For example, the cell `BFF|EFH` encodes both the _"before field"_
state and the _"end field at the current position"_ action. The first element in each cell is always the state.

The transition table is hidden behind the `Format`'s `consume()` method.
```java
state = format.consume(state, nextChar);
```
Given the current state and the next input character, `consume()` returns the next state and actions, consulting its
private transition table.

# Parsers
In this architecture, parsers are responsible for reading characters from the input stream, feeding them to the format,
and executing any actions dictated by the format.

```java
public void parse() {
    do {
        state = format.consume(state, nextChar());
        if (isStartOfField(state)) {
            // ...
        }
        if (isEndOfField(state)) {
            // ...    
        }
        // ...
    } while // ...
}

private boolean isStartOfField(int state) {
    return (state & SFH) != 0; 
}
```


# Footnotes
[^1]: See `SimpleFlatMapper`'s [ConfigurableCharConsumer.java](https://github.com/arnaudroger/SimpleFlatMapper/blob/0f0977f4c1e03cfeb3c4ca1dd5d4050462b01df8/lightningcsv/src/main/java/org/simpleflatmapper/lightningcsv/parser/ConfigurableCharConsumer.java#L204)
[^2]: See `sesseltjonna-csv`'s [DefaultStringArrayCsvReader.java](https://github.com/skjolber/sesseltjonna-csv/blob/master/parser/src/main/java/com/github/skjolber/stcsv/sa/DefaultStringArrayCsvReader.java#L65)
