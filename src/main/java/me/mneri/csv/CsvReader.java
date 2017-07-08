package me.mneri.csv;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CsvReader<T> implements Closeable, Iterable<T> {
    // States
    private static final int ERROR = -1;
    private static final int START = 0;
    private static final int QUOTE = 1;
    private static final int ESCAP = 2;
    private static final int STRNG = 3;
    private static final int CARRG = 4;
    private static final int FINSH = 5;

    // Actions
    private static final int NO_OP = 0;
    private static final int ACCUM = 1;
    private static final int FIELD = 2;
    private static final int DIRTY = 4;
    private static final int NLINE = 8;

    //@formatter:off
    private static final int[][] TRANSITIONS = {
    //       *      "      ,      \r     \n     eof
            {STRNG, QUOTE, START, CARRG, FINSH, FINSH},  // START
            {QUOTE, ESCAP, QUOTE, QUOTE, QUOTE, ERROR},  // QUOTE
            {ERROR, QUOTE, START, CARRG, FINSH, FINSH},  // ESCAP
            {STRNG, ERROR, START, CARRG, FINSH, FINSH},  // STRNG
            {ERROR, ERROR, ERROR, ERROR, FINSH, ERROR},  // CARRG
            {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}}; // FINSH
    //@formatter:on

    //@formatter:off
    private static final int[][] ACTIONS = {
    //       *              "              ,              \r             \n             eof
            {ACCUM        , DIRTY        , FIELD        , FIELD        , FIELD | NLINE, NO_OP        },  // START
            {ACCUM        , NO_OP        , ACCUM        , ACCUM        , ACCUM        , NO_OP        },  // QUOTE
            {NO_OP        , ACCUM        , FIELD        , FIELD        , FIELD | NLINE, FIELD | NLINE},  // ESCAP
            {ACCUM        , NO_OP        , FIELD        , FIELD        , FIELD | NLINE, FIELD | NLINE},  // STRNG
            {NO_OP        , NO_OP        , NO_OP        , NO_OP        , NLINE        , NO_OP        },  // CARRG
            {NO_OP        , NO_OP        , NO_OP        , NO_OP        , NO_OP        , NO_OP        }}; // FINSH
    //@formatter:on

    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private StringBuilder buffer = new StringBuilder();
    private List<String> fields = new ArrayList<>();
    private int lineno = 1;
    private int nfields = -1;
    private Reader reader;
    private int state = OPENED;
    private CsvConverter<T> converter;

    private CsvReader(Reader reader, CsvConverter<T> converter) {
        this.reader = reader;
        this.converter = converter;
    }

    @Override
    public void close() throws IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The reader has already been closed.");

        state = CLOSED;
        reader.close();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (T object : this)
            action.accept(object);
    }

    private int indexOf(int c) {
        switch (c) {
            //@formatter:off
            case '"' : return 1;
            case ',' : return 2;
            case '\r': return 3;
            case '\n': return 4;
            case -1  : return 5; // EOF
            default  : return 0; // *
            //@formatter:on
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private T object = null;

            @Override
            public boolean hasNext() {
                if (object != null)
                    return true;

                try {
                    return (object = readLine()) != null;
                } catch (CsvException e) {
                    throw new UncheckedCsvException(e);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                T result = object;
                object = null;

                return result;
            }
        };
    }

    public static <T> CsvReader<T> open(File file, CsvConverter<T> converter) throws FileNotFoundException {
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        return open(reader, converter);
    }

    public static <T> CsvReader<T> open(Reader reader, CsvConverter<T> converter) {
        if (reader == null)
            throw new IllegalArgumentException("Reader cannot be null.");

        return new CsvReader<>(reader, converter);
    }

    public T readLine() throws CsvException, IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The reader has already been closed.");

        int action;
        int character;
        boolean dirty = false;
        int fieldno = 0;
        int index;
        int state = START;

        while (true) {
            character = reader.read();
            index = indexOf(character);
            action = ACTIONS[state][index];

            if ((action & ACCUM) != 0) {
                buffer.append((char) character);
                dirty = true;
            }

            if ((action & FIELD) != 0) {
                if (dirty) {
                    fields.add(buffer.toString());
                    buffer.setLength(0);
                    dirty = false;
                } else {
                    fields.add(null);
                }

                fieldno++;
            }

            if ((action & DIRTY) != 0)
                dirty = true;

            if ((action & NLINE) != 0) {
                lineno++;

                if (nfields == -1) {
                    nfields = fieldno;
                } else if (nfields != fieldno) {
                    if (fieldno < nfields)
                        throw new NotEnoughFieldsException(lineno, nfields, fieldno);
                    else
                        throw new TooManyFieldsException(lineno, nfields, fieldno);
                }

                try {
                    T object = converter.toObject(fields);
                    fields.clear();

                    return object;
                } catch (Exception e) {
                    throw new CsvConversionException(fields, e);
                }
            }

            state = TRANSITIONS[state][index];

            if (state == FINSH)
                return null;
            else if (state == ERROR)
                throw new UnexpectedCharacterException(lineno, character);
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        int characteristics = Spliterator.IMMUTABLE | Spliterator.ORDERED;
        return Spliterators.spliteratorUnknownSize(iterator(), characteristics);
    }

    public static <T> Stream<T> stream(File file, CsvConverter<T> converter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        return stream(reader, converter);
    }

    public static <T> Stream<T> stream(Reader reader, CsvConverter<T> converter) throws IOException {
        CsvReader<T> csvReader = CsvReader.open(reader, converter);
        return StreamSupport.stream(csvReader.spliterator(), false);
    }
}
