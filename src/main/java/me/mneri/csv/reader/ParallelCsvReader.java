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

package me.mneri.csv.reader;

import me.mneri.csv.deserializer.Deserializer;
import me.mneri.csv.parser.internal.InternalRecycledLine;
import me.mneri.csv.parser.internal.Page;
import me.mneri.csv.parser.internal.PageLoader;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * A {@link CsvReader} that loads pages on a background thread, while the calling thread deserializes lines.
 * <p>
 * Four pages rotate between the two threads: one with the client, one being loaded, and two spares that let the loader
 * run ahead. A page always has a single owner, and changes owner only through a queue: the queues are the only
 * synchronization, and they also make the page's content visible to its new owner.
 * <p>
 * Pages are as large as the longest line accepted.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
final class ParallelCsvReader<T> extends CsvReader<T> {
    private static final int STATE_OPEN = 0;
    private static final int STATE_CLOSED = 1;

    private static final int N_PAGES = 4;
    private static final long CLOSE_TIMEOUT_MILLIS = 1_000L;

    private final BlockingQueue<Page> free = new ArrayBlockingQueue<>(N_PAGES);
    private final BlockingQueue<Page> loaded = new ArrayBlockingQueue<>(N_PAGES);

    private final PageLoader loader;
    private final Deserializer<T> deserializer;
    private final Thread worker;

    private Page page;
    private int cursor;
    private int state = STATE_OPEN;

    ParallelCsvReader(ThreadFactory threads, int pageSize, PageLoader loader, Deserializer<T> deserializer) {
        this.loader = loader;
        this.deserializer = deserializer;

        // Please note, there are N_PAGES in total and the two ArrayBlockingQueues are of size N_PAGES each.
        this.page = new Page(pageSize);
        for (int i = 0; i < N_PAGES - 1; i++) {
            free.add(new Page(pageSize));
        }

        worker = threads.newThread(new Worker());
        if (worker == null) {
            throw new RejectedExecutionException("The thread factory didn't create a thread.");
        }
        worker.start();
    }

    @Override
    public void close() throws IOException {
        state = STATE_CLOSED;
        cursor = Integer.MAX_VALUE;

        worker.interrupt();
        try {
            worker.join(CLOSE_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return cursor < page.lineCount() || hasNext2();
    }

    private boolean hasNext2() throws IOException {
        if (state != STATE_OPEN) {
            throw new IllegalStateException("The reader is closed.");
        }

        do {
            if (page.error() != null) {
                rethrow(page.error());
            }
            if (page.isLast()) {
                return false;
            }
            flip();
        } while (page.lineCount() == 0);

        cursor = 0;
        return true;
    }

    @Override
    public T next() throws IOException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        InternalRecycledLine line = page.line(cursor++);
        return deserializer.deserialize(line);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void flip() throws IOException {
        try {
            Page next = loaded.take(); // Blocking
            free.offer(page); // Non-blocking
            page = next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
    }

    private void rethrow(Throwable t) throws IOException {
        if (t instanceof IOException) {
            throw (IOException) t;
        }
        if (t instanceof InterruptedException) {
            throw new InterruptedIOException();
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new IOException(t);
    }

    private final class Worker implements Runnable {
        @Override
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public void run() {
            Page current = free.remove(); // Non-blocking (at the start a free page is always available).
            try {
                loader.load(current);
                while (!current.isLast()) {
                    Page next = free.take(); // Blocking
                    current.carryover(next);
                    loaded.offer(current); // Non-blocking
                    current = next;
                    loader.load(current);
                }
            } catch (Throwable t) {
                current.fail(t);
            } finally {
                loaded.offer(current); // Non-blocking
                try {
                    loader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
