package me.mneri.csv.format;

import me.mneri.csv.exception.UnexpectedCharacterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Rfc4180FullyRelaxedFormatTest {
    private FormatDriver driver;

    @BeforeEach
    public void beforeEach() {
        Format format = Rfc4180FullyRelaxedFormat.provider().provide();
        driver = new FormatDriver(format);
    }

    @Test
    void parseEmptyFile() throws UnexpectedCharacterException {
        // Given
        String input = "";
        List<List<String>> expected = List.of();

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseOneLineEndingWithCr() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseOneLineEndingWithLf() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseOneLineEndingWithCrLf() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseOneLineWithoutEndOfLine() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseTwoLinesWithCr() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\rdate,elderberry,fig\r";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"),
                List.of("date", "elderberry", "fig"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseTwoLinesWithLf() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\ndate,elderberry,fig\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"),
                List.of("date", "elderberry", "fig"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseTwoLinesWithCrLf() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,cherry\r\ndate,elderberry,fig\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"),
                List.of("date", "elderberry", "fig"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyLineWithCr() throws UnexpectedCharacterException {
        // Given
        String input = "\r";
        List<List<String>> expected = List.of(
                List.of(""));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyLineWithLf() throws UnexpectedCharacterException {
        // Given
        String input = "\n";
        List<List<String>> expected = List.of(
                List.of(""));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyLineWithCrLf() throws UnexpectedCharacterException {
        // Given
        String input = "\r\n";
        List<List<String>> expected = List.of(
                List.of(""));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseVariableNumberOfFields() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana\r\ndate,elderberry,fig\r\ngrapefruit\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana"),
                List.of("date", "elderberry", "fig"),
                List.of("grapefruit"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyField() throws UnexpectedCharacterException {
        // Given
        String input = "apple,,cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyFieldAtStartOfLine() throws UnexpectedCharacterException {
        // Given
        String input = ",banana,cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyFieldAtEndOfLine() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", ""));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseEmptyFieldAtEndOfFile() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", ""));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseFieldWithDoubleQuotes() throws UnexpectedCharacterException {
        // Given
        String input = "apple,ban\"ana,cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "ban\"ana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedField() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQuotedAtStartOfLine() throws UnexpectedCharacterException {
        // Given
        String input = "\"apple\",banana,cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldAtEndOfLine() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,\"cherry\"\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithComma() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana,cherry\"\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana,cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithQuotes() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ba\"\"na\"\"na\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "ba\"na\"na", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithQuotesAtStart() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"\"\"banana\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "\"banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithQuotesAtEnd() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana\"\"\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana\"", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithTwoConsecutiveQuotes() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ban\"\"\"\"ana\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "ban\"\"ana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithCr() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ban\rana\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "ban\rana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithLn() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ban\nana\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "ban\nana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithCrLn() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ban\r\nana\",cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "ban\r\nana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithTextAfter() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ban\"ana,cherry\r\n";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldMissingEndQuotes() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,\"cherry";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldMissingEndQuotesAndEmpty() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana,\"";
        List<List<String>> expected = List.of(
                List.of("apple", "banana", ""));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }
}