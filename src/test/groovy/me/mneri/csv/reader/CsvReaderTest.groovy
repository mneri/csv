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

package me.mneri.csv.reader

import me.mneri.csv.deserializer.Deserializer
import me.mneri.csv.format.Format
import me.mneri.csv.parser.RecycledLine
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.BufferOverflowException
import java.nio.charset.Charset
import java.util.concurrent.ThreadFactory

import static java.nio.charset.StandardCharsets.UTF_16
import static java.nio.charset.StandardCharsets.UTF_8
import static me.mneri.csv.format.Format.*

/**
 * The factory methods are given test doubles of the format, the deserializer and the thread factory. The methods that
 * don't take one build their own default, so the features that check the defaults read with the real ones.
 */
class CsvReaderTest extends Specification {
    // A format that makes the whole stream a single field, ended by the end of file
    static final SINGLE_FIELD = { ->
        def rules = { int s, int c ->
            boolean inField = (s & 0xFFFF) != 0
            c == -1 ? (inField ? EFH | ELH | STP : STP) : (inField ? 1 : 1 | SFH)
        }
        [base: { 0 }, bitmask: { int s, char[] buf, int offset -> -1L }, consume: rules, consumeSlow: rules] as Format
    } as Format.Provider
    static final FIELDS = { RecycledLine line -> (0..<line.fieldCount).collect { line.getString(it) } } as Deserializer
    static final THREADS = { Runnable runnable -> new Thread(runnable).tap { daemon = true } } as ThreadFactory
    static final STANDARD = Configuration.standard()
    static final LONG_LINES = Configuration.builder().withMaxLineSize(8_192).build()
    static final NO_THREAD = { null } as ThreadFactory

    static final Map<String, Closure<CsvReader>> FACTORIES = [
            "open(Reader, Provider, Deserializer, Configuration)":
                    { a -> CsvReader.open(a.reader, a.format, a.deserializer, a.configuration) },
            "open(Reader, Provider, Deserializer)":
                    { a -> CsvReader.open(a.reader, a.format, a.deserializer) },
            "open(Reader, Provider)":
                    { a -> CsvReader.open(a.reader, a.format) },
            "open(Reader, Deserializer)":
                    { a -> CsvReader.open(a.reader, a.deserializer) },
            "open(Reader)":
                    { a -> CsvReader.open(a.reader) },
            "open(File, Charset, Provider, Deserializer, Configuration)":
                    { a -> CsvReader.open(a.file, a.charset, a.format, a.deserializer, a.configuration) },
            "open(File, Charset, Provider, Deserializer)":
                    { a -> CsvReader.open(a.file, a.charset, a.format, a.deserializer) },
            "open(File, Charset, Provider)":
                    { a -> CsvReader.open(a.file, a.charset, a.format) },
            "open(File, Charset, Deserializer)":
                    { a -> CsvReader.open(a.file, a.charset, a.deserializer) },
            "open(File, Charset)":
                    { a -> CsvReader.open(a.file, a.charset) },
            "parallel(ThreadFactory, Reader, Provider, Deserializer, Configuration)":
                    { a -> CsvReader.parallel(a.threads, a.reader, a.format, a.deserializer, a.configuration) },
            "parallel(ThreadFactory, Reader, Provider, Deserializer)":
                    { a -> CsvReader.parallel(a.threads, a.reader, a.format, a.deserializer) },
            "parallel(ThreadFactory, Reader, Provider)":
                    { a -> CsvReader.parallel(a.threads, a.reader, a.format) },
            "parallel(ThreadFactory, Reader, Deserializer)":
                    { a -> CsvReader.parallel(a.threads, a.reader, a.deserializer) },
            "parallel(ThreadFactory, Reader)":
                    { a -> CsvReader.parallel(a.threads, a.reader) },
            "parallel(ThreadFactory, File, Charset, Provider, Deserializer, Configuration)":
                    { a ->
                        CsvReader.parallel(a.threads, a.file, a.charset, a.format, a.deserializer, a.configuration)
                    },
            "parallel(ThreadFactory, File, Charset, Provider, Deserializer)":
                    { a -> CsvReader.parallel(a.threads, a.file, a.charset, a.format, a.deserializer) },
            "parallel(ThreadFactory, File, Charset, Provider)":
                    { a -> CsvReader.parallel(a.threads, a.file, a.charset, a.format) },
            "parallel(ThreadFactory, File, Charset, Deserializer)":
                    { a -> CsvReader.parallel(a.threads, a.file, a.charset, a.deserializer) },
            "parallel(ThreadFactory, File, Charset)":
                    { a -> CsvReader.parallel(a.threads, a.file, a.charset) },
    ]

    @TempDir
    File folder

    def "#signature returns a #type.simpleName"() {
        when:
        def reader = open(arguments("a,b"))

        then:
        reader.class == type

        cleanup:
        reader?.close()

        where:
        [signature, open] << factories()
        type = signature.startsWith("parallel") ? ParallelCsvReader : SequentialCsvReader
    }

    def "#signature uses the format given"() {
        given:
        def format = Mock(Format.Provider)

        when:
        def reader = open(arguments("a", [format: format]))

        then:
        1 * format.provide() >> SINGLE_FIELD.provide()

        cleanup:
        reader?.close()

        where:
        [signature, open] << factories { it.contains("Provider") }
    }

    def "#signature uses Rfc4180FullyRelaxedFormat"() {
        expect: "text after a closing quote and a lone CR, which Rfc4180StrictFormat rejects"
        read(open, 'a,"b"c\rd') == [["a", "bc"], ["d"]]

        where:
        [signature, open] << factories { !it.contains("Provider") }
    }

    def "#signature maps the lines with the deserializer given"() {
        given:
        def deserializer = Mock(Deserializer)

        when:
        def lines = read(open, "a", [deserializer: deserializer])

        then:
        1 * deserializer.deserialize({ it.getString(0) == "a" }) >> "x"
        lines == ["x"]

        where:
        [signature, open] << factories { it.contains("Deserializer") }
    }

    def "#signature maps the lines to lists of strings"() {
        expect:
        read(open, "a") == [["a"]]

        where:
        [signature, open] << factories { !it.contains("Deserializer") }
    }

    def "#signature reads with #configuration: a line of 5,000 characters"() {
        expect: "the standard configuration takes lines of up to 4,096 characters, the one given up to 8,192"
        read(open, "a" * 5_000, [configuration: LONG_LINES]) == expected

        where:
        [signature, open] << factories()
        configuration = signature.contains("Configuration") ? "the configuration given" : "the standard configuration"
        expected = signature.contains("Configuration") ? [["a" * 5_000]] : BufferOverflowException
    }

    def "#signature reads the file with the charset given"() {
        expect: "UTF-16 is no platform's default charset"
        read(open, "é", [charset: UTF_16]) == [["é"]]

        where:
        [signature, open] << factories { it.contains("File") }
    }

    def "#signature throws FileNotFoundException for a file that doesn't exist"() {
        expect:
        read(open, "a,b", [file: new File(folder, "missing.csv")]) == FileNotFoundException

        where:
        [signature, open] << factories { it.contains("File") }
    }

    def "#signature makes its background thread with the thread factory given"() {
        given:
        def threads = Mock(ThreadFactory)

        when:
        def reader = open(arguments("a", [threads: threads]))

        then:
        1 * threads.newThread(_) >> { Runnable runnable -> THREADS.newThread(runnable) }

        cleanup:
        reader?.close()

        where:
        [signature, open] << factories { it.startsWith("parallel") }
    }

    def "#signature rejects a null thread factory"() {
        expect:
        read(open, "a,b", [threads: null]) == NullPointerException

        where:
        [signature, open] << factories { it.startsWith("parallel") }
    }

    def "a reader that fails to open closes the stream opened for it: #description"() {
        given: "the stream that open() and parallel() open on a file"
        def stream = Mock(InputStream)

        when:
        CsvReader.newInstance(threads, stream, charset, format, FIELDS, config)

        then:
        thrown(RuntimeException)
        1 * stream.close()

        where:
        description                                | threads   | charset | format       | config
        "no charset"                               | null      | null    | SINGLE_FIELD | STANDARD
        "no format"                                | null      | UTF_8   | null         | STANDARD
        "no configuration"                         | null      | UTF_8   | SINGLE_FIELD | null
        "parallel, a factory that makes no thread" | NO_THREAD | UTF_8   | SINGLE_FIELD | STANDARD
    }

    /**
     * Return the factory methods whose signature matches, as pairs of signature and method.
     */
    static List factories(Closure<Boolean> matching = { true }) {
        return FACTORIES.findAll { matching(it.key) }.collect { [it.key, it.value] }
    }

    /**
     * Return the arguments for a factory method: a reader and a file of the text, and test doubles for the others.
     */
    Map arguments(String text, Map overrides = [:]) {
        Charset charset = overrides.charset ?: UTF_8
        def file = new File(folder, "test.csv")
        file.bytes = text.getBytes(charset)
        return [reader: new StringReader(text), file: file, charset: charset, threads: THREADS, format: SINGLE_FIELD,
                deserializer: FIELDS, configuration: STANDARD] + overrides
    }

    /**
     * Read the text with the factory method, and return its lines or the class of the exception that stopped it.
     */
    def read(Closure<CsvReader> open, String text, Map overrides = [:]) {
        try {
            def reader = open(arguments(text, overrides))
            try {
                def lines = []
                while (reader.hasNext()) {
                    lines << reader.next()
                }
                return lines
            } finally {
                reader.close()
            }
        } catch (Exception e) {
            return e.class
        }
    }
}
