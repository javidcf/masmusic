package uk.ac.bath.masmusic.generation;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reader for {@link MarkovDurationGeneratorTable} files.
 *
 * @author Javier Dehesa
 */
public class MarkovDurationGeneratorTableReader implements Closeable {

    /** Source input stream. */
    private final BufferedReader input;

    /** Pattern for table entries. */
    static final Pattern ENTRY_PATTERN = Pattern
            .compile("^\\s*\\(\\s*(?<relPitch>\\d+)\\s*"
                    + ",\\s*\\[\\s*(?<ngram>(?:(?:\\d*\\.?\\d+)(?:\\s*,\\s*\\d*\\.?\\d+)*)?)\\s*\\]\\s*\\)\\s*"
                    + "\\:\\s*(?<trans>\\(\\s*\\d*\\.?\\d+\\s*\\:\\s*\\+?\\s*\\d+\\s*\\)(?:,\\(\\s*\\d*\\.?\\d+\\s*\\:\\s*\\+?\\s*\\d+\\s*\\))*)\\s*$");

    /** Pattern for step/count elements */
    static final Pattern DURATION_COUNT_PATTERN = Pattern
            .compile("^\\s*\\(\\s*(?<duration>\\d*\\.?\\d+)\\s*"
                    + "\\:(?<count>\\+?\\s*\\d+)\\s*\\)\\s*$");

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public MarkovDurationGeneratorTableReader(InputStream input) {
        this.input = new BufferedReader(new InputStreamReader(input));
    }

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public MarkovDurationGeneratorTableReader(Reader input) {
        this.input = new BufferedReader(input);
    }

    /**
     * @return The generator table contained in the input
     */
    public MarkovDurationGeneratorTable readTable() throws IOException {
        int order = readOrder();
        MarkovDurationGeneratorTable table = new MarkovDurationGeneratorTable(order);
        String line = input.readLine();
        while (line != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                readTableEntry(line, table);
            }
            line = input.readLine();
        }
        return table;
    }

    /**
     * @return The order of the table
     * @throws IOException
     *             If the next input is not the order of the table
     */
    private int readOrder() throws IOException {
        String orderLine = input.readLine();
        String[] orderLineSplit = orderLine.split(":");
        if (orderLineSplit.length != 2) {
            throw new IOException("Expected 'order:<int>'");
        }
        if (!orderLineSplit[0].trim().equalsIgnoreCase("order")) {
            throw new IOException("Expected 'order:<int>'");
        }
        int order;
        try {
            order = Integer.parseInt(orderLineSplit[1].trim());
        } catch (NumberFormatException e) {
            throw new IOException("Expected 'order:<int>'");
        }
        return order;
    }

    /**
     * Read the next table entry from the input.
     *
     * @param line
     *            The line containing the entry
     * @param table
     *            The table where the entry is written to
     * @throws IOException
     *             If the next input is not a table entry
     */
    private void readTableEntry(String line, MarkovDurationGeneratorTable table)
            throws IOException {
        Matcher matcher = ENTRY_PATTERN.matcher(line);
        boolean match = matcher.matches();
        if (!match) {
            throw new IOException("Expected '(<relPitch>,<ngram>):(<duration>:<count>),...'");
        }
        // Read reltive pitch
        int relPitch;
        try {
            relPitch = Integer.parseUnsignedInt(matcher.group("relPitch"));
        } catch (NumberFormatException e) {
            throw new IOException("Expected '(<relPitch>,<ngram>):(<duration>:<count>),...'");
        }
        if (relPitch < 0 || relPitch >= 12) {
            throw new IOException("Expected '(<relPitch>,<ngram>):(<duration>:<count>),...'");
        }
        // Read ngram
        List<Double> ngram;
        try {
            ngram = Arrays.stream(matcher.group("ngram").split(","))
                    .map(s -> new Double(s)).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IOException("Expected '(<relPitch>,<ngram>):(<duration>:<count>),...'");
        }
        // Read step/count pairs
        Map<Double, Integer> transitions = new HashMap<>();
        for (String stepCount : matcher.group("trans").split(",")) {
            Matcher stepCountMatcher = DURATION_COUNT_PATTERN.matcher(stepCount);
            boolean stepCountMatch = stepCountMatcher.matches();
            if (!stepCountMatch) {
                throw new IOException("Expected '(<relPitch>,<ngram>):(<duration>:<count>),...'");
            }
            double duration;
            int count;
            try {
                duration = Double.parseDouble(stepCountMatcher.group("duration"));
                count = Integer.parseUnsignedInt(stepCountMatcher.group("count"));
            } catch (NumberFormatException e) {
                throw new IOException("Expected '(<relPitch>,<ngram>):(<duration>:<count>),...'");
            }
            transitions.put(duration, count);
        }
        // Write to table
        if (!transitions.isEmpty()) {
            table.setEntry(relPitch, ngram, transitions);
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
