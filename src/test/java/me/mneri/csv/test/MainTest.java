package me.mneri.csv.test;

import java.io.File;
import java.io.IOException;

import me.mneri.csv.CsvException;
import me.mneri.csv.CsvReader;
import me.mneri.csv.CsvWriter;

import org.junit.Assert;
import org.junit.Test;

public class MainTest {
    public static void main(String... args) {
        new MainTest().main();
    }

    @Test
    public void main() {
        Person mneri = new Person();
        mneri.setFirstName("Massimo");
        mneri.setLastName("Neri");
        mneri.setNickname("\"mneri\"");
        mneri.setAddress("Gambettola, Italy");
        mneri.setWebsite("http://mneri.me");

        File tempFile = null;

        try {
            File dir = new File(System.getProperty("java.io.tmpdir"));
            tempFile = File.createTempFile("junit_", ".csv", dir);

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
            try { tempFile.delete(); } catch (Exception ignored) { }
            //@formatter:on
        }
    }
}
