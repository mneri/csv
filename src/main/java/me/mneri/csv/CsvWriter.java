package me.mneri.csv;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvWriter<T> implements Closeable {
    private static final int CLOSED = 1;
    private static final int OPENED = 0;

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

    private void checkFields(int fieldno) throws NotEnoughFieldsException, TooManyFieldsException {
        if (nfields == -1) {
            nfields = fieldno;
        } else {
            if (nfields != fieldno) {
                if (fieldno < nfields)
                    throw new NotEnoughFieldsException(lineno, nfields, fieldno);
                else
                    throw new TooManyFieldsException(lineno, nfields, fieldno);
            }
        }
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
        return open(writer, converter);
    }

    public static <T> CsvWriter<T> open(Writer writer, CsvConverter<T> converter) {
        if (writer == null)
            throw new IllegalArgumentException("Writer cannot be null.");

        if (converter == null)
            throw new IllegalArgumentException("Converter cannot be null.");

        return new CsvWriter<>(writer, converter);
    }

    private void writeField(String string) throws IOException {
        if (string == null)
            return;

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

        line.clear();
        converter.toCsvLine(object, line);

        int fieldno = line.size();

        checkFields(fieldno);

        for (int i = 0; i < fieldno; i++) {
            writeField(line.get(i));

            if (i != fieldno - 1)
                writer.write(",");
        }

        writer.write("\r\n");
        lineno++;
    }

    public void writeLines(List<T> objects) throws CsvException, IOException {
        for (T object : objects)
            writeLine(object);
    }
}
