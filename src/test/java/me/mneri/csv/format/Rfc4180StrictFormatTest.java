package me.mneri.csv.format;

import me.mneri.csv.exception.UnexpectedCharacterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Rfc4180StrictFormatTest {
    private FormatDriver driver;

    @BeforeEach
    public void beforeEach() {
        Format format = Rfc4180StrictFormat.provider().provide();
        driver = new FormatDriver(format);
    }
    @Test
    void parseSimple() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\r\ndate,elderberry,fig\r\n\uFFFF";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"),
                List.of("date", "elderberry", "fig"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseVariableNumberOfFields() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana\r\ndate,elderberry,fig\r\n\uFFFF";
        List<List<String>> expected = List.of(
                List.of("apple", "banana"),
                List.of("date", "elderberry", "fig"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyField() throws UnexpectedCharacterException {
        // Given
//        String input = "apple,,cherry\uFFFF";
//        List<List<String>> expected = List.of(List.of("apple", null, "cherry"));
//
        // When
//        List<List<String>> result = driver.parse(input);
//
        // Then
//        assertEquals(expected, result);
    }
}