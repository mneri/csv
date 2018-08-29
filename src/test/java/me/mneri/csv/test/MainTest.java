package me.mneri.csv.test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.mneri.csv.*;

import org.junit.Assert;
import org.junit.Test;

public class MainTest {
    private File createTempFile() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        return File.createTempFile("junit_", ".csv", dir);
    }

    private File getResourceFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }

    @Test(expected = NotEnoughFieldsException.class)
    public void notEnoughFields() throws CsvException {
        File file = getResourceFile("not-enough-fields.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerConverter())) {
            while (reader.readLine() != null) {
                // Do nothing
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            if (reader != null)
                try { reader.close(); } catch (Exception ignored) { }

            if (tempFile != null)
                try { tempFile.delete(); } catch (Exception ignored) { }
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
            if (writer != null)
                try { writer.close(); } catch (Exception ignored) { }

            if (tempFile != null)
                try { tempFile.delete(); } catch (Exception ignored) { }
            //@formatter:on
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullReader() {
        CsvReader<Void> reader = null;

        try {
            reader = CsvReader.open((Reader) null, new VoidConverter());
        } finally {
            //@formatter:off
            if (reader != null)
                try { reader.close(); } catch (Exception ignored) { }
            //@formatter:on
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriter() {
        CsvWriter<Void> writer = null;

        try {
            writer = CsvWriter.open((Writer) null, new VoidConverter());
        } finally {
            //@formatter:off
            if (writer != null)
                try { writer.close(); } catch (Exception ignored) { }
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

            try (CsvWriter<Person> writer = CsvWriter.open(tempFile, new PersonConverter())) {
                writer.writeLines(persons);
            }

            try (Stream<Person> stream = CsvReader.stream(tempFile, new PersonConverter())) {
                List<Person> collected = stream.collect(Collectors.toList());
                Assert.assertEquals(persons, collected);
            }
        } catch (CsvException | IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (tempFile != null)
                try { tempFile.delete(); } catch (Exception ignored) { }
            //@formatter:on
        }
    }

    @Test(expected = TooManyFieldsException.class)
    public void tooManyFields() throws CsvException {
        File file = getResourceFile("too-many-fields.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerConverter())) {
            while (reader.readLine() != null) {
                // Do nothing
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void writeRead() {
        Person mneri = new Person();
        mneri.setFirstName("Massimo");
        mneri.setLastName("Neri");
        mneri.setNickname("\"mneri\"");
        mneri.setAddress("Gambettola, Italy");
        mneri.setWebsite("http://mneri.me");

        File tempFile = null;

        try {
            tempFile = createTempFile();

            try (CsvWriter<Person> writer = CsvWriter.open(tempFile, new PersonConverter())) {
                writer.writeLine(mneri);
            }

            try (CsvReader<Person> reader = CsvReader.open(tempFile, new PersonConverter())) {
                Person person = reader.readLine();
                Assert.assertEquals(mneri, person);
            }
        } catch (CsvException | IOException e) {
            e.printStackTrace();
        } finally {
            //@formatter:off
            if (tempFile != null)
                try { tempFile.delete(); } catch (Exception ignored) { }
            //@formatter:on
        }
    }
}
