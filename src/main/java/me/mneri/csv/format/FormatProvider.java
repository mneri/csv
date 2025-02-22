package me.mneri.csv.format;

public interface FormatProvider<T extends Format> {
    /**
     * Return a new {@link Format} instance.
     * <p>
     * Implementors of this interface must guarantee a new instance is returned for each call.
     *
     * @return A fresh format instance.
     */
    T provide();
}
