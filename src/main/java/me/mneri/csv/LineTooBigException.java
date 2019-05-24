package me.mneri.csv;

/**
 * This exception is thrown when {@link CsvReader} encounters a line longer than the limit specified in
 * {@link CsvOptions}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class LineTooBigException extends CsvUncheckedException {
    LineTooBigException() {
        super("The line is too big.");
    }
}
