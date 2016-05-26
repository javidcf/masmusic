package uk.ac.bath.masmusic.key;

public enum Note {

    //@formatter:off
    C(0),
    C_SHARP(1),
    D(2),
    D_SHARP(3),
    E(4),
    F(5),
    F_SHARP(6),
    G(7),
    G_SHARP(8),
    A(9),
    A_SHARP(10),
    B(11);
    //@formatter:on

    public static final Note D_FLAT = C_SHARP;
    public static final Note E_FLAT = D_SHARP;
    public static final Note G_FLAT = F_SHARP;
    public static final Note A_FLAT = G_SHARP;
    public static final Note B_FLAT = A_SHARP;

    private static final Note[] VALUES = {
            C,
            C_SHARP,
            D,
            D_SHARP,
            E,
            F,
            F_SHARP,
            G,
            G_SHARP,
            A,
            A_SHARP,
            B
    };

    private final int value;

    Note(int value) {
        this.value = value;
    }

    /**
     * @return The note value, as the interval from C in half steps
     */
    public int value() {
        return value;
    }

    /**
     * @param value
     *            A note value, as the interval from C in half steps
     * @return The note corresponding to the given value
     */
    public static Note fromValue(int value) {
        return VALUES[Math.floorMod(value, VALUES.length)];
    }

    @Override
    public String toString() {
        switch (this) {
        case C:
            return "C";
        case C_SHARP:
            return "C#";
        case D:
            return "D";
        case D_SHARP:
            return "D#";
        case E:
            return "E";
        case F:
            return "F";
        case F_SHARP:
            return "F#";
        case G:
            return "G";
        case G_SHARP:
            return "G#";
        case A:
            return "A";
        case A_SHARP:
            return "A#";
        case B:
            return "B";
        default:
            return "";
        }
    }
}
