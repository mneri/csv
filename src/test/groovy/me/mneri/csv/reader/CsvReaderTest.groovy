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
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Specification

import java.util.concurrent.ThreadFactory

import static java.nio.charset.StandardCharsets.UTF_8

class CsvReaderTest extends Specification {
    static final FORMAT = Rfc4180FullyRelaxedFormat.provider()
    static final STANDARD = Configuration.standard()
    static final HUGE_PAGES = Configuration.builder().withMaxLineSize(Integer.MAX_VALUE).build()
    static final NO_THREAD = { null } as ThreadFactory

    def "a reader that fails to open closes the stream opened for it: #description"() {
        given: "the stream that open() and parallel() open on a file"
        def stream = Mock(InputStream)

        when:
        CsvReader.newInstance(threads, stream, charset, format, new StringListDeserializer(), config)

        then:
        thrown(RuntimeException)
        1 * stream.close()

        where:
        description                                | threads   | charset | format | config
        "no charset"                               | null      | null    | FORMAT | STANDARD
        "no format"                                | null      | UTF_8   | null   | STANDARD
        "no configuration"                         | null      | UTF_8   | FORMAT | null
        "pages too large to allocate"              | null      | UTF_8   | FORMAT | HUGE_PAGES
        "parallel, a factory that makes no thread" | NO_THREAD | UTF_8   | FORMAT | STANDARD
    }
}
