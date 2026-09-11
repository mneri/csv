package me.mneri.csv

import java.io.StringWriter
import me.mneri.csv.format.Format
import me.mneri.csv.serializer.Serializer
import spock.lang.Specification

class CsvWriterTest extends Specification {
    def output = Spy(StringWriter)
    def format = Mock(Format)
    def serializer = Mock(Serializer)
    def writer = new CsvWriter(output, () -> format, serializer)

    def "write() writes #value as #expected correctly"() {
        given:
        format.delimiter() >> ','
        format.qualifier() >> '"'
        serializer.serialize(_, (List<String>) _) >> { o, List<String> list ->
            list.addAll(value)
        }

        when:
        writer.write(new Object())

        then:
        output.toString() == expected

        where:
        value                   | expected
        [null]                  | "\r\n"
        [null, null]            | ",\r\n"
        ["apple", "banana"]     | "apple,banana\r\n"
        ["apple,banana"]        | "\"apple,banana\"\r\n"
        ["apple", "\"banana\""] | "apple,\"\"\"banana\"\"\"\r\n"
        ["apple\nbanana"]       | "\"apple\nbanana\"\r\n"
        ["apple\rbanana"]       | "\"apple\rbanana\"\r\n"
        ["apple\r\nbanana"]     | "\"apple\r\nbanana\"\r\n"
    }

    def "#label throws IllegalStateException when the writer is closed"() {
        given:
        writer.close()

        when:
        action(writer)

        then:
        thrown(IllegalStateException)

        where:
        label        | action
        "write()"    | { w -> w.write("hello") }
        "writeAll()" | { w -> w.writeAll(["hello", "world"]) }
    }

    def "flush() flushes the output stream"() {
        when:
        writer.flush()

        then:
        1 * output.flush()
    }

    def "close() flushes and closes the output stream"() {
        when:
        writer.close()

        then:
        1 * output.flush()
        1 * output.close()
    }

    def "close() twice doesn't throw an exception"() {
        when:
        writer.close()
        writer.close()

        then:
        noExceptionThrown()
    }
}
