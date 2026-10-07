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

import me.mneri.csv.deserializer.StringListDeserializer
import me.mneri.csv.format.Format
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Requires
import spock.lang.Specification

import java.nio.charset.Charset
import java.util.concurrent.ThreadFactory

import static java.nio.charset.StandardCharsets.UTF_8

class CsvReaderTest extends Specification {
    static final FORMAT = Rfc4180FullyRelaxedFormat.provider()
    static final DESERIALIZER = new StringListDeserializer()
    static final HUGE_PAGES = Configuration.builder().withMaxLineSize(Integer.MAX_VALUE).build()

    @Requires({ new File("/proc/self/fd").directory }) // Linux lists the files the JVM holds open
    def "a reader that fails to open closes its file: #description"() {
        given:
        def file = File.createTempFile("csv", ".csv")
        file.text = "a,b\n"

        when:
        open(file)

        then:
        thrown(RuntimeException)
        openHandles(file) == 0

        cleanup:
        file.delete()

        where:
        description                                | open
        "no charset"                               | { File f -> CsvReader.open(f, (Charset) null) }
        "no format"                                | { File f -> CsvReader.open(f, UTF_8, (Format.Provider) null, DESERIALIZER) }
        "no configuration"                         | { File f -> CsvReader.open(f, UTF_8, FORMAT, DESERIALIZER, null) }
        "pages too large to allocate"              | { File f -> CsvReader.open(f, UTF_8, FORMAT, DESERIALIZER, HUGE_PAGES) }
        "parallel, no thread factory"              | { File f -> CsvReader.parallel((ThreadFactory) null, f, UTF_8) }
        "parallel, a factory that makes no thread" | { File f -> CsvReader.parallel({ null } as ThreadFactory, f, UTF_8) }
    }

    /**
     * Return how many times the JVM holds the file open.
     */
    static int openHandles(File file) {
        def path = file.canonicalPath
        return new File("/proc/self/fd").listFiles().count { it.canonicalPath == path }
    }
}
