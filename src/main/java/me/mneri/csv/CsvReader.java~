package me.mneri.csv;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {
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
    private int fields = -1;
    private int lineno = 1;
    private Reader reader;
    private int state = OPENED;
    private CsvConverter converter;

    private CsvReader(Reader reader, CsvConverter translator) {
        this.reader = reader;
        this.converter = translator;
    }

    public void close() throws IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The reader has already been closed.");

        state = CLOSED;
        reader.close();
    }

    private int indexOf(int c) {
        switch(c) {
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

    public static void main(String... args) {
        CsvReader reader = null;

        try {
            reader = CsvReader.open(new File(args[0]));
            List<Object> line = new ArrayList<>();

            while (reader.readLine(line) != -1) {
                System.out.println(line);
                line.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            try { reader.close(); } catch (Exception ignored) { }
            //@formatter:on
        }
    }

    public static CsvReader open(File file) throws FileNotFoundException {
        return open(file, null);
    }

    public static CsvReader open(File file, CsvConverter converter) throws FileNotFoundException {
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        return open(reader, converter);
    }

    public static CsvReader open(Reader reader) {
        return new CsvReader(reader, null);
    }

    public static CsvReader open(Reader reader, CsvConverter converter) {
        if (reader == null)
            throw new IllegalArgumentException("Reader cannot be null.");

        return new CsvReader(reader, converter);
    }

    public int readLine(List<Object> out) throws IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The reader has already been closed.");

        int action;
        int character;
        int column = 0;
        boolean dirty = false;
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
                    String string = buffer.toString();
                    Object value = converter != null ? converter.fromString(column, string) : string;

                    out.add(value);
                    buffer.setLength(0);
                    dirty = false;
                } else {
                    out.add(null);
                }

                column++;
            }

            if ((action & DIRTY) != 0)
                dirty = true;

            if ((action & NLINE) != 0) {
                lineno++;

                if (fields == -1) {
                    fields = column;
                } else if (fields != column) {
                    if (column < fields)
                        throw new NotEnoughFieldsException(lineno, fields, column);
                    else
                        throw new TooManyFieldsException(lineno, fields, column);
                }

                return column;
            }

            state = TRANSITIONS[state][index];

            if (state == FINSH)
                return -1;
            else if (state == ERROR)
                throw new UnexpectedCharacterException(lineno, character);
        }
    }
}
