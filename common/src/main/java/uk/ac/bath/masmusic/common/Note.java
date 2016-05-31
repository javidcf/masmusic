package uk.ac.bath.masmusic.common;

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

    /**
     * @param noteStr A string representing a note
     * @return The note represented by the given string
     */
    public static Note fromString(String noteStr) {
        switch (noteStr.trim().toUpperCase()) {
        case "C":
            return C;
        case "C#":
            return C_SHARP;
        case "DB":
            return D_FLAT;
        case "D":
            return D;
        case "D#":
            return D_SHARP;
        case "EB":
            return E_FLAT;
        case "E":
            return E;
        case "F":
            return F;
        case "F#":
            return F_SHARP;
        case "GB":
            return G_FLAT;
        case "G":
            return G;
        case "G#":
            return G_SHARP;
        case "AB":
            return A_FLAT;
        case "A":
            return A;
        case "A#":
            return A_SHARP;
        case "BB":
            return B_FLAT;
        case "B":
            return B;
        default:
            return null;
        }
    }
}
