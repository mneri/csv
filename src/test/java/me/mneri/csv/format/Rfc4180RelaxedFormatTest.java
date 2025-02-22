package me.mneri.csv.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class Rfc4180RelaxedFormatTest {
    private Rfc4180RelaxedFormat fmt;

    @BeforeEach
    public void beforeEach() {
        fmt = new Rfc4180RelaxedFormat.Provider().provide();
    }

    @Test
    @DisplayName("Call base(), verify it doesn't throw an exception.")
    public void base() {
        int ignored = fmt.base();
    }
}
