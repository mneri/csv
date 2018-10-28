/*
 * This file is part of mneri/csv.
 *
 * mneri/csv is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mneri/csv is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mneri/csv. If not, see <http://www.gnu.org/licenses/>.
 */

package me.mneri.csv.test;

import me.mneri.csv.CsvReader;
import me.mneri.csv.CsvWriter;
import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.test.model.Person;
import me.mneri.csv.test.serialization.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainTest {
    @Test(expected = CsvConversionException.class)
    public void conversionException1() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<Void> reader = CsvReader.open(file, new ExceptionDeserializer())) {
            while (reader.hasNext()) {
                reader.next();
            }
        }
    }

    @Test(expected = CsvConversionException.class)
    public void conversionException2() throws CsvException, IOException {
        CsvWriter<Void> writer = null;
        File file = null;

        try {
            file = createTempFile();
            writer = CsvWriter.open(file, new ExceptionSerializer());

            writer.put(null);
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

    @Test(expected = NoSuchElementException.class)
    public void empty() throws CsvException, IOException {
        File file = getResourceFile("empty.csv");

        try (CsvReader<Void> reader = CsvReader.open(file, new VoidDeserializer())) {
            reader.next();
        } finally {
            //@formatter:off
            if (file != null)   { try { file.delete(); }  catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void flush() throws CsvException, IOException {
        File file = null;

        CsvReader<List<Integer>> reader = null;
        CsvWriter<List<Integer>> writer = null;

        try {
            file = createTempFile();
            List<Integer> line = Arrays.asList(0, 1, 2, 3);

            writer = CsvWriter.open(file, new IntegerListSerializer());
            writer.put(line);
            writer.flush();

            reader = CsvReader.open(file, new IntegerListDeserializer());
            Assert.assertEquals(line, reader.next());
        } finally {
            //@formatter:off
            if (writer != null) { try { writer.close(); } catch (Exception ignored) { } }
            if (reader != null) { try { reader.close(); } catch (Exception ignored) { } }
            if (file != null)   { try { file.delete(); }  catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    private File getResourceFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }

    @Test(expected = UnexpectedCharacterException.class)
    public void illegal() throws CsvException, IOException {
        File file = getResourceFile("illegal.csv");

        try (CsvReader<List<String>> reader = CsvReader.open(file, new StringListDeserializer())) {
            while (reader.hasNext()) {
                reader.next();
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void readAfterClose() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");
        CsvReader<List<Integer>> reader = null;

        try {
            reader = CsvReader.open(file, new IntegerListDeserializer());
            reader.close();
            reader.next();
        } finally {
            //@formatter:off
            if (reader != null) { try { reader.close(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void shouldQuote() throws CsvException, IOException {
        File file = null;
        List<String> strings = Arrays.asList("a", "\"b\"", "c", "d,e");

        try {
            file = createTempFile();

            try (CsvWriter<List<String>> writer = CsvWriter.open(file, new StringListSerializer())) {
                writer.put(strings);
            }

            try (CsvReader<List<String>> reader = CsvReader.open(file, new StringListDeserializer())) {
                Assert.assertEquals(strings, reader.next());
            }
        } finally {
            //@formatter:off
            if (file != null) { try { file.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void skip1() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");
        List<Integer> expected = Arrays.asList(6, 7, 8, 9, 0);

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            reader.skip(1);
            List<Integer> second = reader.next();
            Assert.assertEquals(second, expected);
        }
    }

    @Test
    public void skip2() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");
        List<Integer> expected = Arrays.asList(6, 7, 8, 9, 0);

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            if (reader.hasNext()) {
                reader.skip(1);
            }

            List<Integer> second = reader.next();
            Assert.assertEquals(second, expected);
        }
    }

    @Test
    public void skip3() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            reader.next();
            reader.skip(1);
            Assert.assertFalse(reader.hasNext());
        }
    }

    @Test
    public void skip4() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            reader.skip(2);
            Assert.assertFalse(reader.hasNext());
        }
    }

    @Test
    public void skip5() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            reader.skip(999);
            Assert.assertFalse(reader.hasNext());
        }
    }

    @Test
    public void skip6() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = CsvReader.open(file, new IntegerListDeserializer())) {
            while (reader.hasNext()) {
                reader.next();
            }

            reader.skip(1);
        }
    }

    @Test(expected = UnexpectedCharacterException.class)
    public void skip7() throws CsvException, IOException {
        File file = getResourceFile("illegal.csv");

        try (CsvReader<List<String>> reader = CsvReader.open(file, new StringListDeserializer())) {
            reader.skip(1);
        }
    }

    @Test
    public void stream() throws CsvConversionException, IOException {
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
                writer.putAll(persons);
            }

            try (Stream<Person> stream = CsvReader.stream(file, new PersonDeserializer())) {
                List<Person> collected = stream.collect(Collectors.toList());
                Assert.assertEquals(persons, collected);
            }
        } finally {
            //@formatter:off
            if (file != null) { try { file.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }

    @Test
    public void writeRead() throws CsvException, IOException {
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
                writer.put(mneri);
            }

            try (CsvReader<Person> reader = CsvReader.open(file, new PersonDeserializer())) {
                Person person = reader.next();
                Assert.assertEquals(mneri, person);
            }
        } finally {
            //@formatter:off
            if (file != null) { try { file.delete(); } catch (Exception ignored) { } }
            //@formatter:on
        }
    }
}
