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
the fastest Java CSV parser. In both `SimpleFlatMapper` and `sesseltjonna-csv`, _transitions_ from one state to the next
are not explicit, and their logic is pushed deep within the code. _Understanding state transitions with these two models
is difficult._

`mneri/csv` takes a different approach: all the states are explicitly laid out in a transition table. Rows represent
states, columns represent input characters, and the intersection indicates the next state.

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
Rows represent the current state, and columns represent classes of characters. On the right-hand side of each row there
is a comment indicating the state name, and the starting state is noted with an `*`. A line comment above the table
labels the columns. The first column is labelled with `*`, meaning any character other than those listed in the
subsequent columns. Next, in order, come the columns for `,`, `\r`, `\n`, `"`, and `EOF` (end of file). The padding
around rows and columns is a low-level optimization, and is explained later on in the document. Cells encode both the
next state and the actions. For example, the cell `BFF|EFH` encodes both the _"before field"_ state and the _"end field
at the current position"_ action. The first element in each cell is always the state.<br/>
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
| ERH             | report error at the current position   |                 |                                        |
```

The transition table is hidden behind the `Format`'s `consume()` method.
```java
state = format.consume(state, nextChar);
```
Given the current state and the next input character, `consume()` returns the next state and actions, consulting its
private transition table.

As mentioned before, `mneri/csv` implements different CSV formats; most notably:

* `Rfc4180StrictFormat`: strict interpretation of the RFC 4180 specification, throwing an exception if the document is
  not compliant.
* `Rfc4180HalfRelaxedFormat`: a more relaxed interpretation of RFC 4180, allowing for different line termination
  characters, and misplaced double-quotes.
* `Rfc4180FullyRelaxedFormat`: a fully relaxed interpretation of RFC 4180 that is guaranteed to never throw an
  exception, even if the document does not conform to the specification.
* `MsExcelFormat`: a format implementation inspired by Microsoft Excel's behaviour.
* `MacintoshFormat`: an interpretation of RFC 4180 compliant with legacy Macintosh systems where the line separator
  is `\r`.

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
Action flags are checked using a simple bitwise `&` operation (`state & SFH`). If the flag is set, the parser shall take
the corresponding action. _This separation allows different CSV dialects to be plugged seamlessly into high-performance
parsing pipelines without duplicating stream-handling or optimization logic._ The parser components are deliberately
kept stupid (and that's a compliment).

# Vector API
The [Vector API](https://openjdk.org/jeps/508) is an exciting feature of JDK 16 and above that allows engineers to
access CPU vector operations.

Vector operations (also known as SIMD, Single Instruction Multiple Data) can substantially speed up computation. Instead
of processing values one-by-one in a sequential loop, the CPU operates on entire blocks of data in a single clock cycle.
For some time now, the Java C2 JIT compiler can transform tight loops into vector operations, but the result has always
been somewhat unreliable. The Vector API gives engineers explicit control. If the Java runtime supports the Vector API,
`mneri/csv` will leverage vector operations. If not, it will fall back to sequential operations.

Rather than processing every character, `mneri/csv` uses vector operations to calculate a 64-bit mask. The mask
indicates which characters must be processed, and which can be ignored. In the example below, bits are set at key
positions (such as the start of a field, commas, and new-line characters).
```
CSV chunk: a a a a , b b b b , c c c c \r\n
Mask:      1 0 0 0 1 1 0 0 0 1 1 0 0 0 1 1
```
Processing only these characters is sufficient to keep the state machine consistent, while the characters with bits set
to zero can be safely skipped. The mask can occasionally contain false-positives, for example a comma enclosed in a
qualified field (this is unavoidable, but luckily rare). In this case the state machine knows it is inside a qualified
field and correctly ignores the comma.

Below is an abstraction of the vectorised parser's loop[^3].
```java
do {
    while (bitmask == 0L) {
        strideStart = strideEnd;
        strideEnd += STRIDE; // The stride is 64 characters for a 64-bit bitmask
        bitmask = bitmask(s, strideStart);
    }
    shift = Long.numberOfTrailingZeros(bitmask);
    bitmask &= bitmask - 1L; // Kernighan's trick
    pos = strideStart + shift;

    state = format.consume(state, getChar(pos));
    if (isStartOfField(state)) {
        // ...
    }
    if (isEndOfField(state)) {
        // ...
    }
} while // ...
```
The parser maintains a 64-character window (stride). The bitmask tells which characters to process, and which not. Using
Kernighan's trick the bitmask is zeroed one bit at a time. Please, note that `Long.numberOfTrailingZeros()` is a HotSpot
intrinsic and the result is calculated in a couple of CPU cycles. 

Experiments have shown that 50-70% of the characters in popular benchmarks are skipped, leading to a considerable
performance gain. For example, in the classic CSV benchmark `worldcitiespop.txt` from MaxMind, out of the `129,212,350`
total characters, the vectorised parser is able to safely ignore `72.80%` of them (`94,072,029` characters), processing
only the remaining `27.20%` (`35,140,321` characters). Calculating the mask is not free, but the cost is very well
offset by the savings downstream.

# Low-Level Optimisations
Maintaining the state in a local variable or relying on the execution stack to keep an _implicit state_ (like
`SimpleFlatMapper` and `sesseltjonna-csv` do respectively) is generally faster than querying a transition table for
every character in the stream. `mneri/csv` mitigates this architectural penalty with a series of low-level
optimisations.

## Branchless Column Mapping
As explained earlier, the parser decides its next state by querying a transition table. To do the lookup, it first needs
to translate the current character into a column index.

`Rfc4180StrictFormat` performs a mapping similar to the following:
```java
if (c == ',') {
    return 1;
} else if (c == '\r') {
    return 2;
} else if (c == '\n') {
    return 3;
} else if (c == '"') {
    return 4;
} else if (c == -1) {
    return 5;
} else {
    return 0;
}
```
However, branching introduces a major performance bottleneck due to possible misprediction. Modern CPUs rely on branch
predictors to guess execution paths and pre-fill the pipeline. In a CSV stream, this mechanism breaks down. Real-world
data alternates unpredictably between long stretches of ordinary text and sparse structural markers like commas, quotes,
and newlines. Because these tokens appear at irregular intervals depending on the data content, branch predictors cannot
establish a reliable pattern. Every misprediction triggers a costly pipeline flush, stalling the CPU.

# Other Low-Level Optimisations
## Facilitating Method Inlining
Every time a method is invoked, the CPU must incur the cost of setting up a stack frame, jumping to a new memory
address, and returning once finished. For small, frequently executed methods (like getters), the overhead can easily
eclipse the actual execution time. The JIT compiler is capable of inlining short methods (removing the cost of the call
completely), and it is more likely to do so when its bytecode is small.

`mneri/csv` structures hot methods to keep the common case short and inlineable, pushing uncommon code paths and error
handling into a separate, cold method that is only reached when needed. An example of this can be found in `CsvReader`.
Clients are expected to use this class following the idiomatic `hasNext()`-`next()` pattern, as shown below.

```java
try (CsvReader<Contact> reader = CsvReader.open(new File("contacts.csv"), StandardCharsets.UTF_8, new ContactDeserializer())) {
    while (reader.hasNext()) {
        Contact contact = reader.next();
        // ...
    }
}
```
Under this pattern, `hasNext()` _prepares_ the next element returning `true` if present, while `next()` simply returns
the element to the client. If the client follows the pattern, when `hasNext()` is called the state of `CsvReader` is
_always_ `ELEMENT_NOT_PREPARED`; the method then loads a new element and sets the state to `ELEMENT_PREPARED` before the
client calls to `next()`.

| Method      | Common-Case Initial State | Common-Case Final State |
|-------------|---------------------------|-------------------------|
| `hasNext()` | `ELEMENT_NOT_PREPARED`    | `ELEMENT_PREPARED`      |
| `next()`    | `ELEMENT_PREPARED`        | `ELEMENT_NOT_PREPARED`  |

Obviously, the code must be robust enough to handle a client using it _slightly_ wrong, but we can structure it in a way
to have a performance gain if the client does it correctly.

```java
public T next() throws IOException {
    if (state == ELEMENT_PREPARED) {
        state = ELEMENT_NOT_PREPARED;
        return deserializer.deserialize(line);
    }
    return next2(); // Only called if the client doesn't follow the idiomatic pattern hasNext()-next()
}
```
Notice how `next()` performs only a single check (`state == ELEMENT_PREPARED`), skipping other sanity checks like
verifying whether the reader is still open or trying to prepare an element on-the-fly. This minimalism keeps the
bytecode footprint tiny and the method is very likely to be inlined in the caller. All the edge-cases are handled by the
`next2()` method.

```java
  private T next2() throws IOException {
      if (state == READER_CLOSED) {
          readerIsClosedException();
      }
      if (!hasNext()) {
          noSuchElementException();
      }
      state = ELEMENT_NOT_PREPARED;
      return deserializer.deserialize(line);
    }
```
If the client behaves correctly, `next2()` will never be called.

This technique is not limited to the `CsvReader` class, but used throughout the code. Another example can be found in
`RandomAccessStream`.

```java
  public int getChar(long pos) throws IOException {
      if (pos >= first && pos < last) {
          return cb[(int) (pos + offset)];
      }
      return getChar2(pos);
  }
```
`getChar()` only checks if the position is within range and immediately returns; if not, it delegates to `getChar2()`.
The cold method performs further checks and might make calls to reload the buffer (`cb`). `getChar2()` is invoked 
approximately once every `8,000` calls of `getChar()`. We need `getChar()` to be inlined, and to push the JIT compiler
to do so we must keep it as lean as possible. We are happy to pay a full method call for `getChar2()` because it happens
so infrequently.

_It sounds off, but sometimes you can get better performances by adding a method call._

# Performances

[^1]: See `SimpleFlatMapper`'s [ConfigurableCharConsumer.java](https://github.com/arnaudroger/SimpleFlatMapper/blob/0f0977f4c1e03cfeb3c4ca1dd5d4050462b01df8/lightningcsv/src/main/java/org/simpleflatmapper/lightningcsv/parser/ConfigurableCharConsumer.java#L204)
[^2]: See `sesseltjonna-csv`'s [DefaultStringArrayCsvReader.java](https://github.com/skjolber/sesseltjonna-csv/blob/master/parser/src/main/java/com/github/skjolber/stcsv/sa/DefaultStringArrayCsvReader.java#L65)
[^3]: For the full implementation, see [VectorLineParser.java](https://github.com/mneri/csv/blob/master/src/main/java/me/mneri/csv/parser/internal/VectorLineParser.java)
