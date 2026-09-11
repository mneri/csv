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

package me.mneri.csv.deserializer;

import me.mneri.csv.line.RecycledLine;

import java.io.IOException;

/**
 * Given a CSV line, construct a Java object. Clients are normally required to implement this class to map CSV lines to
 * their domain objects.
 * <p>
 * <strong>Example</strong>
 * <pre>{@code
 * public class ContactDeserializer implements Deserializer<Contact> {
 *     @Override
 *     public Contact deserialize(RecycledLine line) throws IOException {
 *         Contact contact = new Contact();
 *         contact.setFirstName(line.getString(0));
 *         contact.setLastName(line.getString(1));
 *         // ...
 *         return contact;
 *     }
 * }}</pre>
 * <p>
 * The {@link RecycledLine} instance passed to the {@link #deserialize(RecycledLine)} method is internally reused,
 * cleared, and repopulated. Implementations of this interface should never store, return or otherwise use the
 * {@code RecycledLine} instance outside the scope of this method. The state of {@code RecycledLine} can (and will)
 * change frequently and without warning. Clients should use {@code RecycleLine} in the scope of the
 * {@link #deserialize(RecycledLine)} method to create domain objects.
 *
 * @param <T> The type of the objects.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 * @see RecycledLine
 */
public interface Deserializer<T> {
    /**
     * Given a {@link RecycledLine}, construct an object. The order of the fields is the same as found in the CSV.
     * <i>
     * Implementations of this interface should never store, return or otherwise use the {@link RecycledLine} instance
     * outside the scope of this method.
     *
     * @param line The CSV line.
     * @return An object.
     * @throws IOException If an I/O error occurs.
     */
    T deserialize(RecycledLine line) throws IOException;
}
