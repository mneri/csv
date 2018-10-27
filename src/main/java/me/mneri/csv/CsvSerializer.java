package me.mneri.csv;

import java.util.List;

/**
 * Serialize objects into a {@link List} of strings.
 *
 * @param <T> the type of the objects.
 * @see CsvDeserializer
 */
public interface CsvSerializer<T> {
    /**
     * Serialize an object into a list of strings. The strings should be added in order to the list passed as parameter.
     *
     * @param object the object to serialize.
     * @param out    the list of strings representing the csv line.
     * @throws Exception if anything goes wrong.
     */
    void serialize(T object, List<String> out) throws Exception;
}
