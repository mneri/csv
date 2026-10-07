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

import me.mneri.csv.concurrent.DefaultThreadFactory
import me.mneri.csv.deserializer.Deserializer
import me.mneri.csv.format.Format
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadFactory

class ParallelCsvReaderTest extends CsvReaderContract {
    // The background threads, in the order the readers made them
    List<Thread> threads = []

    @Override
    <T> CsvReader<T> newReader(Reader stream, Format.Provider<? extends Format> format, Deserializer<T> deserializer,
                               Configuration config) {
        def factory = { Runnable runnable ->
            def thread = new DefaultThreadFactory().newThread(runnable)
            threads << thread
            return thread
        } as ThreadFactory
        return CsvReader.parallel(factory, stream, format, deserializer, config)
    }

    def "reads the stream on a single background thread, made by the thread factory"() {
        given:
        def stream = new RecordingReader((1..1_000).collect { "$it,aaa" }.join("\n"))
        def reader = open(stream, maxLineSize: 16)

        when:
        readAll(reader)

        then:
        threads.size() == 1
        stream.threads == threads as Set
    }

    def "close() ends the background thread, after #linesRead lines (#parser parser)"() {
        given: "a stream long enough to keep the background thread waiting for the client"
        def reader = open((1..1_000).collect { "$it,aaa" }.join("\n"), parser: parser, maxLineSize: 16)
        linesRead.times { reader.next() }

        when:
        reader.close()

        then:
        !threads[0].alive

        where:
        [parser, linesRead] << [PARSERS.keySet(), [0, 1, 500, 1_000]].combinations()
    }

    @Timeout(10) // If the reader hangs, fail instead of freezing the build
    def "a client interrupted while it waits for a page can keep reading"() {
        given: "a stream that stops before its first character, so the client has to wait for the first page"
        def expected = (1..1_000).collect { [it.toString(), "aaa", "bbb"] }
        def stream = new StalledReader(expected*.join(",").join("\n"))
        def reader = open(stream, maxLineSize: 32) // Small pages: about two lines each
        stream.reading.await() // The loader is stuck in read(), outside the reader's locks

        and: "a thread that interrupts the client as soon as it waits"
        def client = Thread.currentThread()
        Thread.start {
            while (client.state != Thread.State.WAITING) {
                Thread.onSpinWait()
            }
            client.interrupt()
        }

        when: "the client asks for the first line"
        reader.hasNext()

        then: "the wait is interrupted, and the interrupt is reported"
        thrown(InterruptedIOException)
        Thread.interrupted() // The flag is set again; this clears it

        when: "the stream resumes, and the client tries again"
        stream.go.countDown()
        def lines = readAll(reader)

        then: "every line is read, once and in order"
        lines == expected

        cleanup:
        Thread.interrupted()
        stream.go.countDown()
    }

    @Timeout(10) // If close() hangs, fail instead of freezing the build
    def "close() doesn't wait forever for a background thread stuck in a read that ignores interrupts"() {
        given: "a stream that keeps the background thread in read(), as a silent socket does"
        def stream = new StuckReader()
        def reader = open(stream)
        stream.reading.await()

        when:
        reader.close()

        then: "close() returns, though the read hasn't"
        noExceptionThrown()

        when: "the read returns"
        stream.go.countDown()
        threads[0].join(5_000)

        then: "the background thread closes the stream, and ends"
        stream.closed
        !threads[0].alive

        cleanup: "release the read anyway, or closing the reader again could hang too"
        stream.go.countDown()
    }

    /**
     * A stream that stops before its first character, until it's told to go.
     */
    static class StalledReader extends Reader {
        final CountDownLatch reading = new CountDownLatch(1) // Counted down when the loader first calls read()
        final CountDownLatch go = new CountDownLatch(1)
        private final Reader input

        StalledReader(String input) {
            this.input = new StringReader(input)
        }

        @Override
        int read(char[] buf, int off, int len) throws IOException {
            reading.countDown()
            try {
                go.await()
            } catch (InterruptedException ignored) {
                throw new InterruptedIOException()
            }
            return input.read(buf, off, len)
        }

        @Override
        void close() throws IOException {
            input.close()
        }
    }

    /**
     * A stream whose first read blocks until it's told to go, and ignores interrupts meanwhile, as sockets and pipes do.
     */
    static class StuckReader extends Reader {
        final CountDownLatch reading = new CountDownLatch(1) // Counted down when the loader first calls read()
        final CountDownLatch go = new CountDownLatch(1)
        volatile boolean closed

        @Override
        int read(char[] buf, int off, int len) throws IOException {
            reading.countDown()
            boolean interrupted = false
            while (go.count > 0) {
                try {
                    go.await()
                } catch (InterruptedException ignored) {
                    interrupted = true // Noted, but the read goes on
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt()
            }
            return -1
        }

        @Override
        void close() throws IOException {
            closed = true
        }
    }
}
