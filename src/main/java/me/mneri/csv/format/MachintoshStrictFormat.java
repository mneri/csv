package me.mneri.csv.format;

public final class MachintoshStrictFormat implements Format {
    private static final int BFL = 0;  // Before line
    private static final int BFF = 8;  // Before field
    private static final int SQT = 16; // Start quotation
    private static final int SQE = 24; // Escape at start quotation
    private static final int QOT = 32; // Quotation
    private static final int ESC = 40; // Escape
    private static final int FLD = 48; // Field
    private static final int CAR = 56; // Carriage return
    private static final int EOF = 64; // End of file
    private static final int ERR = 72; // Error

    //@formatter:off
    private static final int[] DFA = {
    // *                "                ,                \r               \n               EOF
       FLD|SFH,         SQT,             BFF|SFH|EFH,     CAR|SFH|EFH,     BFL|ELH,         EOF|STP,         0,0, // BFL
       FLD|SFH,         SQT,             BFF|SFH|EFH,     CAR|SFH|EFH,     BFL|SFH|EFH|ELH, EOF|SFH|EFH|ELH, 0,0, // BFF
       QOT|SFH,         SQE,             QOT,             QOT,             QOT,             ERR|ERH,         0,0, // SQT
       ERR|ERH,         QOT|SFH,         BFF|SFH|EFH,     CAR|SFH|EFH,     BFL|SFH|EFH,     EOF|SFH|EFH|STP, 0,0, // SQE
       QOT,             ESC,             QOT,             QOT,             QOT,             ERR|ERH,         0,0, // QOT
       ERR|ERH,         QOT|RCB,         BFF|EFB,         CAR|EFB,         BFL|EFB|ELH,     EOF|EFB|ELH|STP, 0,0, // ESC
       FLD,             FLD,             BFF|EFH,         CAR|EFH,         BFL|EFH|ELH,     EOF|EFH|STP,     0,0, // FLD
       ERR|RLR|ELH,     ERR|RLR,         ERR|RLR,         ERR|RLR,         BFL|ELH,         ERR|ERH,         0,0, // CAR
       ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         0,0, // EOF
       ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         0,0, // ERR
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0};
    //@formatter:on

    /**
     * {@inheritDoc}
     *
     * @return The initial state.
     */
    @Override
    public int base() {
        return BFL;
    }

    private int indexOf(int c) {
        if (c > ',') {
            return 0;
        } else if (c == ',') {
            return 2;
        } else if (c == '\n') {
            return 4;
        } else if (c == '\r') {
            return 3;
        } else if (c == '"') {
            return 1;
        } else if (c > 0) {
            return 0;
        }
        return 5;
    }

    @Override
    public int consume(int s, int c) {
        final int i = (s | indexOf(c)) & 0x7F;
        return i == FLD ? FLD : DFA[i];
    }
}
