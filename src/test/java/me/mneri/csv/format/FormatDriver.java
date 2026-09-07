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

    List<List<String>> parse(String input) throws UnexpectedCharacterException {
        List<List<String>> result = new ArrayList<>();
        List<String> line = null;

        int pos = 0;
        int start = 0;
        int s = format.base();

        while (pos <= input.length()) { // Loop one over to add the EOF character
            int c = pos < input.length() ? input.charAt(pos) : -1;
            s = format.consume(s, c);

            if ((s & SFH) != 0) {
                start = pos;
            }
            if ((s & EFH) != 0) {
                if (line == null) {
                    line = new ArrayList<>();
                }
                line.add(input.substring(start, pos));
            }
            if ((s & EFB) != 0) {
                if (line == null) {
                    line = new ArrayList<>();
                }
                line.add(input.substring(start, pos - 1));
            }
            if ((s & ELH) != 0) {
                result.add(line);
                line = null;
            }
            if ((s & RPL) != 0) {
                pos = pos - 1;
            }
            if ((s & RMB) != 0) {
                input = input.substring(0, pos - 1) + input.substring(pos);
                pos = pos - 1;
            }
            if (((s & ERH) != 0)) {
                throw new UnexpectedCharacterException(result.size());
            }
            if ((s & STP) != 0) {
                break;
            }
            pos = pos + 1;
        }

        if (line != null) {
            result.add(line);
        }

        return result;
    }
}
