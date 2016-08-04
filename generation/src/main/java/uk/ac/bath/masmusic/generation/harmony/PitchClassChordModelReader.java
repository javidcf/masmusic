package uk.ac.bath.masmusic.generation.harmony;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader for {@link PitchClassChordModel} files.
 *
 * @author Javier Dehesa
 */
public class PitchClassChordModelReader implements Closeable {

    /** Source input stream. */
    private final BufferedReader input;

    /** Pattern for table entries. */
    static final private Pattern ENTRY_PATTERN = Pattern
            .compile("^\\s*(?<relPitch>[-+]?\\d+)\\s*"
                    + "\\:\\s*(?<chords>\\([^:]+:\\s*\\d*\\.?\\d+\\s*\\)(?:,\\([^:]*:\\s*\\d*\\.?\\d+\\s*\\))*)\\s*$");

    /** Chord reader. */
    static final private ScaleRelativeChordReader CHORD_READER = new ScaleRelativeChordReader();

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public PitchClassChordModelReader(InputStream input) {
        this.input = new BufferedReader(new InputStreamReader(input));
    }

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public PitchClassChordModelReader(Reader input) {
        this.input = new BufferedReader(input);
    }

    /**
     * Read a model from the input.
     *
     * @return The read model
     * @throws IOException
     *             If the input does not contain a model or it could not be read
     */
    public PitchClassChordModel readModel() throws IOException {
        PitchClassChordModel model = new PitchClassChordModel();
        String line = input.readLine();
        while (line != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                readModelEntry(line, model);
            }
            line = input.readLine();
        }
        return model;
    }

    /**
     * Read a model entry.
     *
     * @param line
     *            A line from the input
     * @param model
     *            The model to be filled
     * @throws IOException
     *             If the input does not contain a model or it could not be read
     */
    private void readModelEntry(String line, PitchClassChordModel model) throws IOException {
        Matcher matcher = ENTRY_PATTERN.matcher(line);
        boolean matched = matcher.matches();
        if (!matched) {
            throw new IOException("Expected '<relPitch>:(<chord>:<weight>),...'");
        }
        int relPitchClass;
        try {
            relPitchClass = Math.floorMod(Integer.parseInt(matcher.group("relPitch")), 12);
        } catch (NumberFormatException e) {
            throw new IOException("Expected '<relPitch>:(<chord>:<weight>),...'");
        }
        Map<ScaleRelativeChord, Double> chords = new HashMap<>();
        for (String str : matcher.group("chords").split(",")) {
            try {
                int splitIdx = str.indexOf(':');
                String chordStr = str.substring(str.indexOf('(') + 1, splitIdx);
                String weightStr = str.substring(splitIdx + 1, str.lastIndexOf(')'));
                ScaleRelativeChord chord = CHORD_READER.readChord(chordStr);
                double weight = Double.parseDouble(weightStr);
                chords.put(chord, weight);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new IOException("Expected '<relPitch>:(<chord>:<weight>),...'");
            }
            model.setModelEntry(relPitchClass, chords);
        }
    }

    /**
     * Closes this reader and releases resources associated with it.
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        input.close();
    }

}