package me.mneri.csv;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class CsvReader<T> implements Closeable {
    // States
    private static final byte ERROR = -1;
    private static final byte START = 0;
    private static final byte QUOTE = 1;
    private static final byte ESCAP = 2;
    private static final byte STRNG = 3;
    private static final byte CARRG = 4;
    private static final byte FINSH = 5;

    // Actions
    private static final byte NO_OP = 0;
    private static final byte ACCUM = 1;
    private static final byte FIELD = 2;
    private static final byte DIRTY = 4;
    private static final byte NLINE = 8;

    //@formatter:off
    private static final byte[][] TRANSITIONS = {
    //       *      "      ,      \r     \n     eof
            { STRNG, QUOTE, START, CARRG, FINSH, FINSH },  // START
            { QUOTE, ESCAP, QUOTE, QUOTE, QUOTE, ERROR },  // QUOTE
            { ERROR, QUOTE, START, CARRG, FINSH, FINSH },  // ESCAP
            { STRNG, STRNG, START, CARRG, FINSH, FINSH },  // STRNG
            { ERROR, ERROR, ERROR, ERROR, FINSH, ERROR },  // CARRG
            { ERROR, ERROR, ERROR, ERROR, ERROR, ERROR }}; // FINSH
    //@formatter:on

    //@formatter:off
    private static final byte[][] ACTIONS = {
    //       *              "              ,              \r             \n             eof
            { ACCUM        , DIRTY        , FIELD        , FIELD        , FIELD | NLINE, NO_OP         },  // START
            { ACCUM        , NO_OP        , ACCUM        , ACCUM        , ACCUM        , NO_OP         },  // QUOTE
            { NO_OP        , ACCUM        , FIELD        , FIELD        , FIELD | NLINE, FIELD | NLINE },  // ESCAP
            { ACCUM        , ACCUM        , FIELD        , FIELD        , FIELD | NLINE, FIELD | NLINE },  // STRNG
            { NO_OP        , NO_OP        , NO_OP        , NO_OP        , NLINE        , NO_OP         },  // CARRG
            { NO_OP        , NO_OP        , NO_OP        , NO_OP        , NO_OP        , NO_OP         }}; // FINSH
    //@formatter:on

    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private final StringBuilder buffer = new StringBuilder(8192);
    private final List<String> line = new ArrayList<>();
    private int lines = 1;
    private int fields = -1;
    private final Reader reader;
    private int state = OPENED;
    private final CsvDeserializer<T> deserializer;

    private CsvReader(Reader reader, CsvDeserializer<T> deserializer) {
        this.reader = reader;
        this.deserializer = deserializer;
    }

    private void checkFields(int fields) throws IllegalCsvFormatException {
        if (this.fields == -1) {
            this.fields = fields;
        } else if (this.fields != fields) {
            if (fields < this.fields) {
                throw new NotEnoughFieldsException(lines, this.fields, fields);
            } else {
                throw new TooManyFieldsException(lines, this.fields, fields);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (state == CLOSED) {
            throw new IllegalStateException("The reader has already been closed.");
        }

        state = CLOSED;
        reader.close();
    }

    private int indexOf(int charCode) {
        switch (charCode) {
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

    private Iterator<T> iterator() {
        return new Iterator<T>() {
            private T object = null;

            @Override
            public boolean hasNext() {
                if (object != null) {
                    return true;
                }

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
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                T result = object;
                object = null;

                return result;
            }
        };
    }

    public static CsvReader<List<String>> open(File file) throws IOException {
        return open(file, Charset.defaultCharset());
    }

    public static CsvReader<List<String>> open(File file, Charset charset) throws IOException {
        return open(file, charset, new StringListDeserializer());
    }

    public static <T> CsvReader<T> open(File file, CsvDeserializer<T> deserializer) throws IOException {
        if (deserializer == null) {
            throw new IllegalArgumentException("Deserializer cannot be null.");
        }

        return open(Files.newBufferedReader(file.toPath(), Charset.defaultCharset()), deserializer);
    }

    public static <T> CsvReader<T> open(File file, Charset charset, CsvDeserializer<T> deserializer) throws IOException {
        if (deserializer == null) {
            throw new IllegalArgumentException("Deserializer cannot be null.");
        }

        return open(Files.newBufferedReader(file.toPath(), charset), deserializer);
    }

    public static CsvReader<List<String>> open(Reader reader) {
        return open(reader, new StringListDeserializer());
    }

    public static <T> CsvReader<T> open(Reader reader, CsvDeserializer<T> deserializer) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader cannot be null.");
        }

        if (deserializer == null) {
            throw new IllegalArgumentException("Deserializer cannot be null.");
        }

        return new CsvReader<>(reader, deserializer);
    }

    public T readLine() throws CsvException, IOException {
        if (state == CLOSED) {
            throw new IllegalStateException("The reader has already been closed.");
        }

        byte action;
        int charCode;
        boolean dirty = false;
        int fields = 0;
        int index;
        byte state = START;

        while (true) {
            charCode = reader.read();
            index = indexOf(charCode);
            action = ACTIONS[state][index];

            if ((action & ACCUM) != 0) {
                buffer.append((char) charCode);
                dirty = true;
            } else if ((action & FIELD) != 0) {
                if (dirty) {
                    line.add(buffer.toString());
                    buffer.setLength(0);
                    dirty = false;
                } else {
                    line.add(null);
                }

                fields++;
            } else if ((action & DIRTY) != 0) {
                dirty = true;
            }

            if ((action & NLINE) != 0) {
                lines++;
                checkFields(fields);

                try {
                    T object = deserializer.deserialize(line);
                    line.clear();

                    return object;
                } catch (Exception e) {
                    throw new CsvConversionException(line, e);
                }
            }

            state = TRANSITIONS[state][index];

            if (state == FINSH) {
                return null;
            } else if (state == ERROR) {
                throw new UnexpectedCharacterException(lines, charCode);
            }
        }
    }

    private Spliterator<T> spliterator() {
        int characteristics = Spliterator.IMMUTABLE | Spliterator.ORDERED;
        return Spliterators.spliteratorUnknownSize(iterator(), characteristics);
    }

    public static Stream<List<String>> stream(File file) throws IOException {
        return stream(file, new StringListDeserializer());
    }

    public static <T> Stream<T> stream(File file, CsvDeserializer<T> deserializer) throws IOException {
        return stream(Files.newBufferedReader(file.toPath()), deserializer);
    }

    public static <T> Stream<T> stream(Reader reader, CsvDeserializer<T> deserializer) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader cannot be null.");
        }

        //@formatter:off
        CsvReader<T> csvReader = CsvReader.open(reader, deserializer);
        return StreamSupport.stream(csvReader.spliterator(), false)
                            .onClose(() -> { try { csvReader.close(); } catch (Exception ignored) { } });
        //@formatter:on
    }
}
