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
import me.mneri.csv.parser.internal.Page;
import me.mneri.csv.parser.internal.PageLoader;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * A {@link CsvReader} that loads pages on a background thread, while the calling thread deserializes lines.
 * <p>
 * Three pages rotate between the two threads: one with the client, one being loaded, and a spare that lets the loader
 * run a page ahead. A page always has a single owner, and changes owner only through a queue: the queues are the only
 * synchronization, and they also make the page's content visible to its new owner.
 * <p>
 * Pages are as large as the longest line accepted.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
final class ParallelCsvReader<T> extends CsvReader<T> {
    private static final int N_PAGES = 4; // At least 3

    private final BlockingQueue<Page> free = new ArrayBlockingQueue<>(N_PAGES);
    private final BlockingQueue<Page> loaded = new ArrayBlockingQueue<>(N_PAGES);
    private final PageLoader loader;
    private final Deserializer<T> deserializer;
    private final Thread thread;
    private Page page;
    private int cursor;
    private boolean closed;

    ParallelCsvReader(ThreadFactory threads, int pageSize, PageLoader loader, Deserializer<T> deserializer) {
        this.loader = loader;
        this.deserializer = deserializer;
        this.page = new Page(pageSize); // The client's page: empty, it's handed back at the first read
        for (int i = 1; i < N_PAGES; i++) {
            free.add(new Page(pageSize));
        }
        thread = threads.newThread(new Worker());
        thread.start();
    }

    @Override
    public boolean hasNext() throws IOException {
        // Optimization: the page holds hundreds of lines, so this is true hundreds of times in a row. The rest is in
        // the cold-path method hasNext2(), keeping this method small enough to be inlined by the JIT compiler.
        return cursor < page.lineCount() || hasNext2();
    }

    private boolean hasNext2() throws IOException {
        if (closed) {
            throw new IllegalStateException("The reader is closed.");
        }
        while (cursor == page.lineCount()) {
            if (page.error() != null) {
                throw rethrow(page.error());
            }
            if (page.isLast()) {
                return false;
            }
            flip();
            cursor = 0;
        }
        return true;
    }

    @Override
    public T next() throws IOException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return deserializer.deserialize(page.line(cursor++));
    }

    @Override
    public void close() throws IOException {
        closed = true;
        cursor = Integer.MAX_VALUE; // Sends hasNext() to its slow path, which throws
        thread.interrupt(); // Once the thread is over, this and join() have no effect
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
        loader.close(); // The worker is over: the stream is ours again
    }

    private void flip() throws IOException {
        Page next;
        try {
            next = loaded.take(); // If the wait is interrupted, nothing has changed: hasNext() can simply try again
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
        free.offer(page); // Never fails, and can't be interrupted: the queue has room for all the pages
        page = next;
    }

    private IOException rethrow(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        return (IOException) e;
    }

    /**
     * The body of the background thread, which is the only one to touch the stream: it loads pages from the free queue
     * and publishes them to the loaded queue, until the last page or until the client closes the reader.
     */
    private final class Worker implements Runnable {
        @Override
        public void run() {
            try {
                Page current = free.take();
                loader.load(current);
                while (!current.isLast()) {
                    Page next = free.take();
                    current.carryover(next);
                    loaded.put(current);
                    loader.load(next);
                    current = next;
                }
                loaded.put(current);
            } catch (InterruptedException e) {
                // The client closed the reader
            }
        }
    }
}
