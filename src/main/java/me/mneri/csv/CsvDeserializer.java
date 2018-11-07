/*
 * This file is part of mneri/csv.
 *
 * mneri/csv is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mneri/csv is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mneri/csv. If not, see <http://www.gnu.org/licenses/>.
 */

package me.mneri.csv;

import java.util.List;

/**
 * Deserialize objects into a {@link List} of strings.
 *
 * @param <T> the type of the objects.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 * @see CsvSerializer
 */
public interface CsvDeserializer<T> {
    /**
     * Deserialize an object starting from csv line. The order of the strings is the same as found in the csv.
     *
     * @param line the csv line.
     * @return An object.
     * @throws Exception if anything goes wrong.
     */
    T deserialize(RecyclableCsvLine line) throws Exception;
}
