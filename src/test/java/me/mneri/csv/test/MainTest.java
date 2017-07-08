package me.mneri.csv.test;

import me.mneri.csv.CsvException;
import me.mneri.csv.CsvReader;
import me.mneri.csv.CsvWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class MainTest {
    @Test
    public void main() {
        Person mneri = new Person();
        mneri.setFirstName("Massimo");
        mneri.setLastName("Neri");
        mneri.setNickname("\"mneri\"");
        mneri.setAddress("Gambettola, Italy");
        mneri.setWebsite("http://mneri.me");

        try {
            File dir = new File(System.getProperty("java.io.tmpdir"));
            File file = File.createTempFile("junit_", ".csv", dir);

            try (CsvWriter<Person> writer = CsvWriter.open(file, new PersonConverter())) {
                writer.writeLine(mneri);
            }

            try (CsvReader<Person> reader = CsvReader.open(file, new PersonConverter())) {
                Person person = reader.readLine();
                Assert.assertEquals(mneri, person);
            }
        } catch (CsvException | IOException e) {
            e.printStackTrace();
        }
    }
}
