package me.mneri.csv.test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.mneri.csv.*;

import me.mneri.csv.test.model.Person;
import me.mneri.csv.test.serialization.*;
import org.junit.Assert;
import org.junit.Test;

public class MainTest {
    @Test(expected = CsvConversionException.class)
    public void conversionException1() throws CsvException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<Void> reader = CsvReader.open(file, new ExceptionDeserializer())) {
            while (reader.readLine() != null) {
                // Do nothing
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = CsvConversionException.class)
    public void conversionException2() throws CsvException {
        CsvWriter<Void> writer = null;
        File tempFile = null;

        try {
            tempFile = createTempFile();
            writer = CsvWriter.open(tempFile, new ExceptionSerializer());

            writer.writeLine(null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (writer != null)   { try { writer.close(); }    catch (Exception ignored) { } }
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    private File createTempFile() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        return File.createTempFile("junit_", ".csv", dir);
    }

    private File getResourceFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }

    @Test(expected = NotEnoughFieldsException.class)
    public void notEnoughFields1() throws CsvException {
        File file = getResourceFile("not-enough-fields.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            while (reader.readLine() != null) {
                // Do nothing
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = NotEnoughFieldsException.class)
    public void notEnoughFields2() throws CsvException {
        CsvWriter<List<Integer>> writer = null;
        File tempFile = null;

        List<Integer> first = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> second = Arrays.asList(6, 7, 8, 9);

        try {
            tempFile = createTempFile();
            writer = CsvWriter.open(tempFile, new IntegerListSerializer());

            writer.writeLine(first);
            writer.writeLine(second);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (writer != null)   { try { writer.close(); }    catch (Exception ignored) { } }
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConverter1() {
        CsvReader<Void> reader = null;
        File tempFile = null;

        try {
            tempFile = createTempFile();
            reader = CsvReader.open(tempFile, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (reader != null)   { try { reader.close(); }    catch (Exception ignored) { } }
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConverter2() {
        CsvWriter<Void> writer = null;
        File tempFile = null;

        try {
            tempFile = createTempFile();
            writer = CsvWriter.open(tempFile, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (writer != null)   { try { writer.close(); }    catch (Exception ignored) { } }
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullReader() {
        try (CsvReader reader = CsvReader.open((Reader) null, new VoidDeserializer())) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriter() {
        try (CsvWriter<Void> writer = CsvWriter.open((Writer) null, new VoidSerializer())) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) throws CsvException {
        new MainTest().shouldQuote();
    }

    @Test
    public void shouldQuote() throws CsvException {
        File tempFile = null;

        List<String> strings = Arrays.asList("a", "\"b\"", "c", "d,e");

        try {
            tempFile = createTempFile();

            try (CsvWriter<List<String>> writer = CsvWriter.open(tempFile, new StringListSerializer())) {
                writer.writeLine(strings);
            }

            try (CsvReader<List<String>> reader = CsvReader.open(tempFile, new StringListDeserializer())) {
                Assert.assertEquals(strings, reader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void stream() {
        List<Person> persons = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Person person = new Person();
            person.setFirstName("First name " + i);
            person.setLastName("Last name " + i);
            persons.add(person);
        }

        File tempFile = null;

        try {
            tempFile = createTempFile();

            try (CsvWriter<Person> writer = CsvWriter.open(tempFile, new PersonSerializer())) {
                writer.writeLines(persons);
            }

            try (Stream<Person> stream = CsvReader.stream(tempFile, new PersonDeserializer())) {
                List<Person> collected = stream.collect(Collectors.toList());
                Assert.assertEquals(persons, collected);
            }
        } catch (CsvException | IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test(expected = TooManyFieldsException.class)
    public void tooManyFields1() throws CsvException {
        File file = getResourceFile("too-many-fields.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            while (reader.readLine() != null) {
                // Do nothing
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = TooManyFieldsException.class)
    public void tooManyFields2() throws CsvException {
        CsvWriter<List<Integer>> writer = null;
        File tempFile = null;

        List<Integer> first = Arrays.asList(1, 2, 3, 4);
        List<Integer> second = Arrays.asList(5, 6, 7, 8, 9);

        try {
            tempFile = createTempFile();
            writer = CsvWriter.open(tempFile, new IntegerListSerializer());

            writer.writeLine(first);
            writer.writeLine(second);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (writer != null)   { try { writer.close(); }    catch (Exception ignored) { } }
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void writeRead() throws CsvException {
        Person mneri = new Person();
        mneri.setFirstName("Massimo");
        mneri.setLastName("Neri");
        mneri.setNickname("\"mneri\"");
        mneri.setAddress("Gambettola, Italy");
        mneri.setWebsite("http://mneri.me");

        File tempFile = null;

        try {
            tempFile = createTempFile();

            try (CsvWriter<Person> writer = CsvWriter.open(tempFile, new PersonSerializer())) {
                writer.writeLine(mneri);
            }

            try (CsvReader<Person> reader = CsvReader.open(tempFile, new PersonDeserializer())) {
                Person person = reader.readLine();
                Assert.assertEquals(mneri, person);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (tempFile != null) { try { tempFile.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }
}
