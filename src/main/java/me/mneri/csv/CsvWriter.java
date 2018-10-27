package me.mneri.csv;

import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.exception.UncheckedCsvException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class CsvWriter<T> implements Closeable {
    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private final CsvSerializer<T> serializer;
    private final List<String> line = new ArrayList<>();
    private int state = OPENED;
    private final Writer writer;

    private CsvWriter(Writer writer, CsvSerializer<T> serializer) {
        this.writer = writer;
        this.serializer = serializer;
    }

    private void checkClosedState() {
        if (state == CLOSED) {
            throw new IllegalStateException("The writer is closed.");
        }
    }

    @Override
    public void close() throws IOException {
        checkClosedState();
        state = CLOSED;
        writer.close();
    }

    public static <T> CsvWriter<T> open(File file, Charset charset, CsvSerializer<T> serializer) throws IOException {
        return open(Files.newBufferedWriter(file.toPath(), charset), serializer);
    }

    public static <T> CsvWriter<T> open(File file, CsvSerializer<T> serializer) throws IOException {
        return open(file, Charset.defaultCharset(), serializer);
    }

    public static <T> CsvWriter<T> open(Writer writer, CsvSerializer<T> serializer) {
        return new CsvWriter<>(writer, serializer);
    }

    public void put(T object) throws CsvException, IOException {
        checkClosedState();

        try {
            line.clear();
            serializer.serialize(object, line);
        } catch (Exception e) {
            throw new CsvConversionException(line, e);
        }

        int fields = line.size();

        for (int i = 0; i < fields; i++) {
            putField(line.get(i));

            if (i != fields - 1) {
                writer.write(",");
            }
        }

        writer.write("\r\n");
    }

    public void putAll(List<T> objects) throws CsvException, IOException {
        checkClosedState();

        for (T object : objects) {
            put(object);
        }
    }

    public void putAll(Stream<T> stream) {
        checkClosedState();

        stream.forEach(object -> {
            try {
                put(object);
            } catch (CsvException e) {
                throw new UncheckedCsvException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void putField(String string) throws IOException {
        if (string == null) {
            return;
        }

        int length = string.length();
        boolean quote = false;

        if (string.length() == 0) {
            quote = true;
        } else {
            for (int i = 0; i < length; i++) {
                int c = string.charAt(i);

                if (c == ',' || c == '"') {
                    quote = true;
                    break;
                }
            }
        }

        if (quote) {
            writer.write('"');

            for (int i = 0; i < length; i++) {
                int c = string.charAt(i);

                if (c == '"') {
                    writer.write("\"\"");
                } else {
                    writer.write(c);
                }
            }

            writer.write('"');
        } else {
            writer.write(string);
        }
    }
}
