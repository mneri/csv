# Transition Tables

The CSV grammar defines a [regular language](https://en.wikipedia.org/wiki/Regular_language). By definition, a regular
language is the set of strings recognised by a
[finite state automaton](https://en.wikipedia.org/wiki/Finite-state_machine) (FSA).

Under the hood, most CSV parsers are finite state automata. For example, when a parser encounters a double quote
character, it could transition to the `INSIDE_QUALIFIED_FIELD` state; a second double quote character could make the
parser transition to the `END_QUALIFIED_FIELD` state. These states would be two of the finite number of states in the
parser.

There are many ways to implement a finite state automaton, one of which is to keep a variable with the current state. In
[`SimpleFlatMapper`](https://github.com/arnaudroger/SimpleFlatMapper), author `arnaudroger` uses this technique.

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
    }
}
```
In the code above, the state of the parser changes by updating the `currentState` variable and the change is driven by
the flow of the code: a cascade of `if-else` statements decides what state is next. This technique is simple and very
effective and, in fact, `SimpleFlatMapper` is one of the fastest Java CSV parsers in circulation.

`skjolberg `takes a slightly different approach. In [`sesseltjonna-csv`](https://github.com/skjolber/sesseltjonna-csv)
the state is kept _implicit_. There is no `currentState` variable; instead, the execution point implicitly defines the
state of the automaton.

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

`mneri/csv` takes a different approach: all the states of the parser are explicitly laid out in a transition table. Rows
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
[RFC 4180](https://datatracker.ietf.org/doc/rfc4180/). The initial state (marked with `*`) is `BEFORE_LINE`. Upon
consuming the character `a` (first column), the state transitions to `FIELD`. From the `FIELD` state, a comma sets the
transition to `BEFORE_FIELD`. Transitions continue until either the state `END_OF_FILE` or `ERROR` is reached. `ERROR`
is called a _sink state_; once entered, it cannot be left because all outgoing transitions loop back to itself. _A
transition table makes it easier to reason about the parser._ All the states and transitions are laid out in a single
point.

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

`mneri/csv` supports many different CSV dialects, and for each one there's a transition table. In the code [they're
called formats](https://github.com/mneri/csv/tree/master/src/main/java/me/mneri/csv/format).

```java
private static final int[] DFA = {
// *                    ,                    \r                   \n                   "                    EOF                  padding
   FLD,                 BFF|EFH,             CAR|EFH,             ERR|ERH,             ERR|ERH,             EOF|EFH|ELH|RPL,     0,0,  // FLD
   QOT,                 QOT,                 QOT,                 QOT,                 ESC,                 ERR|ERH,             0,0,  // QOT
   FLD|SFH,             BFF|SFH|EFH,         CAR|SFH|EFH,         ERR|ERH,             SQT,                 EOF|SFH|EFH|ELH|RPL, 0,0,  // BFF
   QOT|SFH,             QOT|SFH,             QOT|SFH,             QOT|SFH,             SQE,                 ERR|ERH,             0,0,  // SQT
   ERR|ERH,             BFF|EFB,             CAR|EFB,             ERR|ERH,             QOT|RMB,             EOF|EFB|ELH|RPL,     0,0,  // ESC
   ERR|ERH,             BFF|SFH|EFH,         CAR|SFH|EFH,         ERR|ERH,             QOT|SFH,             EOF|SFH|EFH|RPL,     0,0,  // SQE
   FLD|SFH,             BFF|SFH|EFH,         CAR|SFH|EFH,         ERR|ERH,             SQT,                 EOF|STP,             0,0,  // BFL *
   ERR|ERH,             ERR|ERH,             ERR|ERH,             BFL|ELH,             ERR|ERH,             ERR|ERH,             0,0,  // CAR
   ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             EOF|STP,             0,0,  // EOF
   ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             0,0,  // ERR
   0,                   0,                   0,                   0,                   0,                   0,                   0,0,
   0,                   0,                   0,                   0,                   0,                   0,                   0,0,
   0,                   0,                   0,                   0,                   0,                   0,                   0,0,
   0,                   0,                   0,                   0,                   0,                   0,                   0,0,
   0,                   0,                   0,                   0,                   0,                   0,                   0,0,
   0,                   0,                   0,                   0,                   0,                   0,                   0,0};
```