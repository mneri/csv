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

package me.mneri.csv.writer

import java.io.StringWriter
import me.mneri.csv.format.Format
import me.mneri.csv.serializer.Serializer
import spock.lang.Specification
import spock.lang.TempDir

import static java.nio.charset.StandardCharsets.UTF_16

class CsvWriterTest extends Specification {
    def output = Spy(StringWriter)
    def format = Mock(Format)
    def serializer = Mock(Serializer)
    def writer = new CsvWriter(output, () -> format, serializer)

    @TempDir
    File folder

    def "write() writes #value as #expected correctly"() {
        given:
        format.delimiter() >> ','
        format.qualifier() >> '"'
        format.lineSeparator() >> "\r\n"
        serializer.serialize(_, (List<String>) _) >> { o, List<String> list ->
            list.addAll(value)
        }

        when:
        writer.write(new Object())

        then:
        output.toString() == expected

        where:
        value                   | expected
        []                      | "\r\n"
        [null]                  | "\r\n"
        [null, null]            | ",\r\n"
        ["apple", "banana"]     | "apple,banana\r\n"
        ["apple,banana"]        | "\"apple,banana\"\r\n"
        ["apple", "\"banana\""] | "apple,\"\"\"banana\"\"\"\r\n"
        ["apple\nbanana"]       | "\"apple\nbanana\"\r\n"
        ["apple\rbanana"]       | "\"apple\rbanana\"\r\n"
        ["apple\r\nbanana"]     | "\"apple\r\nbanana\"\r\n"
    }

    def "write() ends each line with the format's line separator: #description"() {
        given:
        format.delimiter() >> ','
        format.qualifier() >> '"'
        format.lineSeparator() >> separator
        serializer.serialize(_, (List<String>) _) >> { o, List<String> list ->
            list.addAll(["a", "b"])
        }

        when:
        writer.write(new Object())
        writer.write(new Object())

        then:
        output.toString() == ("a,b" + separator) * 2

        where:
        description | separator
        "CR LF"     | "\r\n"
        "CR"        | "\r"
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

    def "close() closes the output stream even if flushing fails"() {
        given:
        output.flush() >> { throw new IOException() }

        when:
        writer.close()

        then:
        thrown(IOException)
        1 * output.close()
    }

    def "close() twice doesn't throw an exception"() {
        when:
        writer.close()
        writer.close()

        then:
        noExceptionThrown()
    }

    def "open(File, Charset, Provider, Serializer) writes the file in the charset given"() {
        given:
        format.delimiter() >> ','
        format.qualifier() >> '"'
        format.lineSeparator() >> "\r\n"
        serializer.serialize(_, (List<String>) _) >> { o, List<String> list ->
            list.addAll(["é", "中文"])
        }
        def file = new File(folder, "test.csv")

        when:
        def csv = CsvWriter.open(file, UTF_16, () -> format, serializer)
        csv.write(new Object())
        csv.close()

        then: "UTF-16 is no platform's default charset"
        file.bytes == "é,中文\r\n".getBytes(UTF_16)
    }

    def "open(File, Charset, Serializer) writes the file in the charset given"() {
        given:
        serializer.serialize(_, (List<String>) _) >> { o, List<String> list ->
            list.addAll(["é", "中文"])
        }
        def file = new File(folder, "test.csv")

        when: "the format is Rfc4180StrictFormat, built into this overload"
        def csv = CsvWriter.open(file, UTF_16, serializer)
        csv.write(new Object())
        csv.close()

        then: "UTF-16 is no platform's default charset"
        file.bytes == "é,中文\r\n".getBytes(UTF_16)
    }
}
