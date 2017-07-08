package me.mneri.csv;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvWriter<T> implements Closeable {
    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private CsvConverter<T> converter;
    private List<String> line = new ArrayList<>();
    private int lineno = 1;
    private int nfields = -1;
    private int state = OPENED;
    private Writer writer;

    private CsvWriter(Writer writer, CsvConverter<T> converter) {
        this.writer = writer;
        this.converter = converter;
    }

    @Override
    public void close() throws IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The writer has already been closed.");

        state = CLOSED;
        writer.close();
    }

    public static <T> CsvWriter<T> open(File file, CsvConverter<T> converter) throws FileNotFoundException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        return new CsvWriter<>(writer, converter);
    }

    public static <T> CsvWriter<T> open(Writer writer, CsvConverter<T> converter) {
        return new CsvWriter<>(writer, converter);
    }

    private void writeField(String string) throws IOException {
        int length = string.length();
        boolean shouldQuote = false;

        if (string.length() == 0) {
            shouldQuote = true;
        } else {
            for (int i = 0; i < length; i++) {
                int c = string.charAt(i);

                if (c == ',' || c == '"') {
                    shouldQuote = true;
                    break;
                }
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

    public void writeLine(T object) throws CsvException, IOException {
        if (state == CLOSED)
            throw new IllegalStateException("The writer has already been closed.");

        if (object == null)
            throw new NullPointerException("The supplied object was null.");

        line.clear();
        converter.toCsvLine(object, line);

        int size = line.size();

        if (nfields == -1) {
            nfields = size;
        } else {
            if (nfields != size) {
                if (size < nfields)
                    throw new NotEnoughFieldsException(lineno, nfields, size);
                else
                    throw new TooManyFieldsException(lineno, nfields, size);
            }
        }

        for (int i = 0; i < size; i++) {
            String field = line.get(i);

            if (field != null)
                writeField(field);

            if (i != size - 1)
                writer.write(",");
        }

        writer.write("\r\n");
        lineno++;
    }
}
