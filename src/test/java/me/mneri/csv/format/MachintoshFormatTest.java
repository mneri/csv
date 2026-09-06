package me.mneri.csv.format;

import me.mneri.csv.exception.UnexpectedCharacterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MachintoshFormatTest {
    private FormatDriver driver;

    @BeforeEach
    public void beforeEach() {
        Format format = MachintoshFormat.provider().provide();
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
                List.of("apple", "banana", "cherry\n"));

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
                List.of("apple", "banana", "cherry"),
                List.of("\n"));

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
                List.of("apple", "banana", "cherry\ndate", "elderberry", "fig\n"));

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
                List.of("\ndate", "elderberry", "fig"),
                List.of("\n"));

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
                List.of("\n"));

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
                List.of(""),
                List.of("\n"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseVariableNumberOfFields() throws UnexpectedCharacterException {
        // Given
        String input = "apple,banana\rdate,elderberry,fig\rgrapefruit\r";
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
        String input = "apple,,cherry\r";
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
        String input = ",banana,cherry\r";
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
        String input = "apple,banana,\r";
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
    void parseFieldWithDoubleQuotes() {
        // Given
        String input = "apple,ban\"ana,cherry\r";

        // When/Then
        assertThrows(UnexpectedCharacterException.class, () -> driver.parse(input));
    }

    @Test
    void parseQualifiedField() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana\",cherry\r";
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
        String input = "\"apple\",banana,cherry\r";
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
        String input = "apple,banana,\"cherry\"\r";
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
        String input = "apple,\"banana,cherry\"\r";
        List<List<String>> expected = List.of(
                List.of("apple", "banana,cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithCommaAtStart() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\",banana\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", ",banana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithCommaAtEnd() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana,\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "banana,", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithCrAtStart() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"\rbanana\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "\rbanana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithCrAtEnd() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana\r\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "banana\r", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithLfAtStart() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"\nbanana\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "\nbanana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithLfAtEnd() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"banana\n\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "banana\n", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithQuotes() throws UnexpectedCharacterException {
        // Given
        String input = "apple,\"ba\"\"na\"\"na\",cherry\r";
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
        String input = "apple,\"\"\"banana\",cherry\r";
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
        String input = "apple,\"banana\"\"\",cherry\r";
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
        String input = "apple,\"ban\"\"\"\"ana\",cherry\r";
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
        String input = "apple,\"ban\rana\",cherry\r";
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
        String input = "apple,\"ban\nana\",cherry\r";
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
        String input = "apple,\"ban\r\nana\",cherry\r";
        List<List<String>> expected = List.of(
                List.of("apple", "ban\r\nana", "cherry"));

        // When
        List<List<String>> result = driver.parse(input);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void parseQualifiedFieldWithTextAfter() {
        // Given
        String input = "apple,\"ban\"ana,cherry\r";

        // When/Then
        assertThrows(UnexpectedCharacterException.class, () -> driver.parse(input));
    }

    @Test
    void parseQualifiedFieldMissingEndQuotes() {
        // Given
        String input = "apple,banana,\"cherry";

        // When/Then
        assertThrows(UnexpectedCharacterException.class, () -> driver.parse(input));
    }

    @Test
    void parseQualifiedFieldMissingEndQuotesAndEmpty() {
        // Given
        String input = "apple,banana,\"";

        // When/Then
        assertThrows(UnexpectedCharacterException.class, () -> driver.parse(input));
    }
}