package me.mneri.csv.format;

import me.mneri.csv.exception.UnexpectedCharacterException;

import java.util.ArrayList;
import java.util.List;

import static me.mneri.csv.format.Format.*;

class FormatDriver {
    private final Format format;

    FormatDriver(Format format) {
        this.format = format;
    }

    public List<List<String>> parse(String input) throws UnexpectedCharacterException {
        List<List<String>> result = new ArrayList<>();
        List<String> line = null;

        int i = 0;
        int start = 0;
        int s = format.base();

        while (i < input.length() && (s & STP) == 0) {
            char c = input.charAt(i);
            s = format.consume(s, c);

            if ((s & SFH) != 0) {
                start = i;
            }
            if ((s & EFH) != 0) {
                if (line == null) {
                    line = new ArrayList<>();
                }
                line.add(input.substring(start, i));
            }
            if ((s & EFB) != 0) {
                if (line == null) {
                    line = new ArrayList<>();
                }
                line.add(input.substring(start, i - 1));
            }
            if ((s & ELH) != 0) {
                result.add(line);
                line = null;
            }
            if ((s & RPL) != 0) {
                i = i - 1;
            }
            if ((s & RMB) != 0) {
                input = input.substring(0, i) + input.substring(i + 1);
                i = i - 1;
            }
            if (((s & ERH) != 0)) {
                throw new UnexpectedCharacterException(result.size());
            }

            i = i + 1;
        }

        if (line != null) {
            result.add(line);
        }

        return result;
    }
}
