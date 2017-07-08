package me.mneri.csv;

import java.io.*;
import java.util.List;

public class CsvWriter {
    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private CsvConverter converter;
    private int fields = -1;
    private int lineno = 1;
    private int state = OPENED;
    private Writer writer;

    private CsvWriter(Writer writer, CsvConverter converter) {
        this.writer = writer;
        this.converter = converter;
    }

    public void close() throws IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The writer has already been closed.");

        state = CLOSED;
        writer.close();
    }

    public static CsvWriter open(File file) throws FileNotFoundException {
        return open(file, null);
    }

    public static CsvWriter open(File file, CsvConverter converter) throws FileNotFoundException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        return new CsvWriter(writer, converter);
    }

    public static CsvWriter open(Writer writer) {
        return open(writer, null);
    }

    public static CsvWriter open(Writer writer, CsvConverter converter) {
        return new CsvWriter(writer, converter);
    }

    private void writeField(String string) throws IOException {
        int length = string.length();
        boolean shouldQuote = false;

        for (int i = 0; i < length; i++) {
            int c = string.charAt(i);

            if (c == ',' || c == '"') {
                shouldQuote = true;
                break;
            }
        }

        if (shouldQuote) {
            writer.write('"');

            for (int i = 0; i < length; i++) {
                int c = string.charAt(i);

                if (c == '"')
                    writer.write("\"\"");
                else
                    writer.write(c);
            }

            writer.write('"');
        } else {
            writer.write(string);
        }
    }

    public void writeLine(List<Object> line) throws IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The writer has already been closed.");

        if (line == null)
            throw new NullPointerException("The supplied line was null.");

        int size = line.size();

        if (fields == -1) {
            fields = size;
        } else {
            if (fields != size) {
                if (size < fields)
                    throw new NotEnoughFieldsException(lineno, fields, size);
                else
                    throw new TooManyFieldsException(lineno, fields, size);
            }
        }

        for (int i = 0; i < size; i++) {
            Object object = line.get(i);

            if (object != null)
                writeField(converter != null ? converter.toString(i, object) : object.toString());

            if (i != size - 1)
                writer.write(",");
        }

        writer.write("\r\n");
        lineno++;
    }
}
