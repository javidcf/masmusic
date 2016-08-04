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
 * Reader for {@link ChordBigramModel} files.
 *
 * @author Javier Dehesa
 */
public class ChordBigramModelReader implements Closeable {

    /** Source input stream. */
    private final BufferedReader input;

    /** Pattern for table entries. */
    static final private Pattern ENTRY_PATTERN = Pattern
            .compile("^\\s*\\((?<prevChord>[^)]+)\\)\\s*"
                    + "\\:\\s*(?<nextChords>\\([^:]+:\\s*\\d*\\.?\\d+\\s*\\)(?:,\\([^:]*:\\s*\\d*\\.?\\d+\\s*\\))*)\\s*$");

    /** Chord reader. */
    static final private ScaleRelativeChordReader CHORD_READER = new ScaleRelativeChordReader();

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public ChordBigramModelReader(InputStream input) {
        this.input = new BufferedReader(new InputStreamReader(input));
    }

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public ChordBigramModelReader(Reader input) {
        this.input = new BufferedReader(input);
    }

    /**
     * Read a model from the input.
     *
     * @return The read model
     * @throws IOException
     *             If the input does not contain a model or it could not be read
     */
    public ChordBigramModel readModel() throws IOException {
        ChordBigramModel model = new ChordBigramModel();
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
    private void readModelEntry(String line, ChordBigramModel model) throws IOException {
        Matcher matcher = ENTRY_PATTERN.matcher(line);
        boolean matched = matcher.matches();
        if (!matched) {
            throw new IOException("Expected '<prevChord>:(<nextChord>:<weight>),...'");
        }
        ScaleRelativeChord prevChord;
        try {
            prevChord = CHORD_READER.readChord(matcher.group("prevChord"));
        } catch (IllegalArgumentException e) {
            throw new IOException("Expected '<prevChord>:(<nextChord>:<weight>),...'");
        }
        Map<ScaleRelativeChord, Double> nextChords = new HashMap<>();
        for (String str : matcher.group("nextChords").split(",")) {
            try {
                int splitIdx = str.indexOf(':');
                String chordStr = str.substring(str.indexOf('(') + 1, splitIdx);
                String weightStr = str.substring(splitIdx + 1, str.lastIndexOf(')'));
                ScaleRelativeChord nextChord = CHORD_READER.readChord(chordStr);
                double weight = Double.parseDouble(weightStr);
                nextChords.put(nextChord, weight);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new IOException("Expected '<prevChord>:(<nextChord>:<weight>),...'");
            }
            model.setModelEntry(prevChord, nextChords);
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