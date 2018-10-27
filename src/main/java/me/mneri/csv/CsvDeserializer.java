package me.mneri.csv;

import java.util.List;

/**
 * Deserialize objects into a {@link List} of strings.
 *
 * @param <T> the type of the objects.
 * @see CsvSerializer
 */
public interface CsvDeserializer<T> {
    /**
     * Deserialize an object starting from a list of strings. The order of the strings is the same order as found in the
     * csv.
     *
     * @param line the list of strings.
     * @return An object.
     * @throws Exception if anything goes wrong.
     */
    T deserialize(List<String> line) throws Exception;
}
