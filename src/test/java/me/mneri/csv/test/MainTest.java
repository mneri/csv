package me.mneri.csv.test;

import me.mneri.csv.CsvReader;
import me.mneri.csv.CsvWriter;
import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.test.model.Person;
import me.mneri.csv.test.serialization.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        File file = null;

        try {
            file = createTempFile();
            writer = CsvWriter.open(file, new ExceptionSerializer());

            writer.writeLine(null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (writer != null) { try { writer.close(); } catch (Exception ignored) { } }
            if (file != null)   { try { file.delete(); }  catch (Exception ignored) { } }
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

    @Test
    public void shouldQuote() throws CsvException {
        File file = null;
        List<String> strings = Arrays.asList("a", "\"b\"", "c", "d,e");

        try {
            file = createTempFile();

            try (CsvWriter<List<String>> writer = CsvWriter.open(file, new StringListSerializer())) {
                writer.writeLine(strings);
            }

            try (CsvReader<List<String>> reader = CsvReader.open(file, new StringListDeserializer())) {
                Assert.assertEquals(strings, reader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (file != null) { try { file.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void skip() throws CsvException {
        File file = getResourceFile("simple.csv");
        List<Integer> expected = Arrays.asList(6, 7, 8, 9, 0);

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            reader.skip(1);
            List<Integer> second = reader.readLine();
            Assert.assertEquals(second, expected);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void stream() {
        File file = null;
        List<Person> persons = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Person person = new Person();
            person.setFirstName("First name " + i);
            person.setLastName("Last name " + i);
            persons.add(person);
        }

        try {
            file = createTempFile();

            try (CsvWriter<Person> writer = CsvWriter.open(file, new PersonSerializer())) {
                writer.writeLines(persons);
            }

            try (Stream<Person> stream = CsvReader.stream(file, new PersonDeserializer())) {
                List<Person> collected = stream.collect(Collectors.toList());
                Assert.assertEquals(persons, collected);
            }
        } catch (CsvException | IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (file != null) { try { file.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void writeRead() throws CsvException {
        File file = null;

        Person mneri = new Person();
        mneri.setFirstName("Massimo");
        mneri.setLastName("Neri");
        mneri.setNickname("\"mneri\"");
        mneri.setAddress("Gambettola, Italy");
        mneri.setWebsite("http://mneri.me");

        try {
            file = createTempFile();

            try (CsvWriter<Person> writer = CsvWriter.open(file, new PersonSerializer())) {
                writer.writeLine(mneri);
            }

            try (CsvReader<Person> reader = CsvReader.open(file, new PersonDeserializer())) {
                Person person = reader.readLine();
                Assert.assertEquals(mneri, person);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (file != null) { try { file.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }
}
