package me.mneri.csv.util;

public class CharUtils {
    //@formatter:off
    private static String[] NON_PRINTABLE =
            { "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL",
              "BS" , "TAB", "LF" , "VT" , "FF" , "CR" , "SO" , "SI" ,
              "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB",
              "CAN", "EM" , "SUB", "ESC", "FS" , "GS" , "RS" , "US"   };
    //@formatter:on

    private CharUtils() {
    }

    public static String toString(int c) {
        //@formatter:off
        if      (c == -1) return "EOF";
        else if (c < 32)  return NON_PRINTABLE[c];
        else              return String.valueOf((char) c);
        //@formatter:on
    }
}
