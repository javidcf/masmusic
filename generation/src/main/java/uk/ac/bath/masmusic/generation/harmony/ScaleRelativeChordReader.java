package uk.ac.bath.masmusic.generation.harmony;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for {@link ScaleRelativeChord} elements.
 *
 * @author Javier Dehesa
 */
public class ScaleRelativeChordReader {

    /** Pattern for chords. */
    static final Pattern CHORD_PATTERN = Pattern
            .compile("^\\s*(?<root>[-+]?\\d+)\\s*"
                    + ";\\s*\\[\\s*(?<structure>(?:(?:[+-]?\\d+)(?:\\s*;\\s*[+-]?\\d+)*)?)\\s*\\]\\s*$");

    /**
     * Read a chord.
     *
     * @param input
     *            String containing a chord
     * @return The parsed chord
     * @throws IllegalArgumentException
     *             If the given string does not contain a chord
     */
    public ScaleRelativeChord readChord(String input) {
        Matcher matcher = CHORD_PATTERN.matcher(input);
        boolean matches = matcher.matches();
        if (!matches) {
            throw new IllegalArgumentException("The input does not contain a chord");
        }
        int root;
        List<Integer> structure;
        try {
            root = Integer.parseInt(matcher.group("root"));
            structure = Arrays.stream(matcher.group("structure").split(";"))
                    .map(s -> new Integer(s)).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The input does not contain a chord");
        }
        return new ScaleRelativeChord(root, structure);
    }

}
