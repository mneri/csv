/*
 * Copyright 2018 Massimo Neri <hello@mneri.me>
 *
 * This file is part of mneri/csv.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.mneri.csv.test;

import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.reader.CsvReader;
import me.mneri.csv.reader.CsvReaderFactory;
import me.mneri.csv.reader.DefaultCsvReaderFactory;
import me.mneri.csv.test.model.CityPop;
import me.mneri.csv.test.model.Person;
import me.mneri.csv.test.serialization.*;
import me.mneri.csv.writer.CsvWriter;
import me.mneri.csv.writer.CsvWriterFactory;
import me.mneri.csv.writer.DefaultCsvWriterFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.zip.ZipInputStream;

public class MainTest {
    private CsvReaderFactory readerFactory;
    private CsvWriterFactory writerFactory;

    @Before
    public void beforeEach() {
        readerFactory = new DefaultCsvReaderFactory();
        writerFactory = new DefaultCsvWriterFactory();
    }

    @Test(expected = CsvConversionException.class)
    public void conversionException1() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<Void> reader = readerFactory.open(file, new ExceptionDeserializer())) {
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
            writer = writerFactory.open(file, new ExceptionSerializer());

            writer.write(null);
        } finally {
            //@formatter:off
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (file != null) {
                try {
                    file.delete();
                } catch (SecurityException ignored) {
                }
            }
            //@formatter:on
        }
    }

    private Person createMneri() {
        Person mneri = new Person();
        mneri.setFirstName("Massimo");
        mneri.setLastName("Neri");
        mneri.setNickname("\"mneri\"");
        mneri.setAddress("Gambettola, Italy");
        mneri.setWebsite("http://mneri.me");
        return mneri;
    }

    private Person createRms() {
        Person rms = new Person();
        rms.setFirstName("Richard");
        rms.setMiddleName("Matthew");
        rms.setLastName("Stallman");
        rms.setNickname("\"rms\"");
        rms.setAddress("Cambridge, Massachusetts");
        rms.setWebsite("http://stallman.org/");
        return rms;
    }

    private File createTempFile() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        return File.createTempFile("junit_", ".csv", dir);
    }

    @Test(expected = NoSuchElementException.class)
    public void empty() throws CsvException, IOException {
        File file = getResourceFile("empty.csv");

        try (CsvReader<Void> reader = readerFactory.open(file, new VoidDeserializer())) {
            reader.next();
        } finally {
            //@formatter:off
            try {
                file.delete();
            } catch (SecurityException ignored) {
            }
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

            writer = writerFactory.open(file, new IntegerListSerializer());
            writer.write(line);
            writer.flush();

            reader = readerFactory.open(file, new IntegerListDeserializer());
            Assert.assertEquals(line, reader.next());
        } finally {
            //@formatter:off
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (file != null) {
                try {
                    file.delete();
                } catch (SecurityException ignored) {
                }
            }
            //@formatter:on
        }
    }

    private File getResourceFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(name)).getFile());
    }

    @Test(expected = IllegalStateException.class)
    public void readAfterClose() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
            reader.close();
            reader.next();
        }
    }

    @Test
    public void skip1() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");
        List<Integer> expected = Arrays.asList(6, 7, 8, 9, 0);

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
            reader.skip(1);
            List<Integer> second = reader.next();
            Assert.assertEquals(second, expected);
        }
    }

    @Test
    public void skip2() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");
        List<Integer> expected = Arrays.asList(6, 7, 8, 9, 0);

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
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

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
            reader.next();
            reader.skip(1);
            Assert.assertFalse(reader.hasNext());
        }
    }

    @Test
    public void skip4() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
            reader.skip(2);
            Assert.assertFalse(reader.hasNext());
        }
    }

    @Test
    public void skip5() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
            reader.skip(999);
            Assert.assertFalse(reader.hasNext());
        }
    }

    @Test
    public void skip6() throws CsvException, IOException {
        File file = getResourceFile("simple.csv");

        try (CsvReader<List<Integer>> reader = readerFactory.open(file, new IntegerListDeserializer())) {
            while (reader.hasNext()) {
                reader.next();
            }

            reader.skip(1);
            Assert.assertFalse(reader.hasNext());
        }
    }

    //@Test
    public void worldcitiespop() throws IOException, CsvException {
        ZipInputStream zip = null;
        CsvReader<CityPop> reader = null;
        CsvWriter<CityPop> writer = null;

        File input = getResourceFile("worldcitiespop.txt.zip");
        File output = createTempFile();

        try {
            zip = new ZipInputStream(new FileInputStream(input));
            zip.getNextEntry();

            reader = readerFactory.open(new BufferedReader(new InputStreamReader(zip)), new CityPopDeserializer());
            writer = writerFactory.open(output, new CityPopSerializer());

            reader.skip(1);

            while (reader.hasNext()) {
                writer.write(reader.next());
            }
        } finally {
            //@formatter:off
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            try {
                output.delete();
            } catch (SecurityException ignored) {
            }
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                }
            }
            //@formatter:on
        }
    }

    @Test(expected = IllegalStateException.class)
    public void writeAfterClose() throws CsvException, IOException {
        File file = null;
        CsvWriter<List<Integer>> writer = null;

        List<Integer> line = Arrays.asList(0, 1, 2, 3);

        try {
            file = createTempFile();
            writer = writerFactory.open(file, new IntegerListSerializer());
            writer.close();
            writer.write(line);
        } finally {
            //@formatter:off
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (file != null) {
                try {
                    file.delete();
                } catch (SecurityException ignored) {
                }
            }
            //@formatter:on
        }
    }

    @Test
    public void writeRead() throws CsvException, IOException {
        File file = null;
        Person mneri = createMneri();

        try {
            file = createTempFile();

            try (CsvWriter<Person> writer = writerFactory.open(file, new PersonSerializer())) {
                writer.write(mneri);
            }

            try (CsvReader<Person> reader = readerFactory.open(file, new PersonDeserializer())) {
                Person person = reader.next();
                Assert.assertEquals(mneri, person);
            }
        } finally {
            //@formatter:off
            if (file != null) {
                try {
                    file.delete();
                } catch (SecurityException ignored) {
                }
            }
            //@formatter:on
        }
    }
}
