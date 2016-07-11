package uk.ac.bath.masmusic.conductor.generate;

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
 * Reader for {@link MarkovPitchGeneratorTable} files.
 *
 * @author Javier Dehesa
 */
public class MarkovPitchGeneratorTableReader implements Closeable {

    /** Source input stream. */
    private final BufferedReader input;

    /** Pattern for table entries. */
    static final Pattern ENTRY_PATTERN = Pattern
            .compile("^\\s*\\(\\s*(?<degree>\\d+)\\s*"
                    + ",\\s*(?<alter>(?:[-#]?\\s?)*)\\s*"
                    + ",\\s*\\[\\s*(?<ngram>(?:(?:[+-]?\\s*\\d+)(?:\\s*,\\s*[+-]?\\s*\\d+)*)?)\\s*\\]\\s*\\)\\s*"
                    + "\\:\\s*(?<trans>\\(\\s*[+-]?\\s*\\d+\\s*\\:\\s*\\+?\\s*\\d+\\s*\\)(?:,\\(\\s*[+-]?\\s*\\d+\\s*\\:\\s*\\+?\\s*\\d+\\s*\\))*)\\s*$");

    /** Pattern for step/count elements */
    static final Pattern STEP_COUNT_PATTERN = Pattern
            .compile("^\\s*\\(\\s*(?<step>[+-]?\\d+)\\s*"
                    + "\\:(?<count>\\+?\\s*\\d+)\\s*\\)\\s*$");

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public MarkovPitchGeneratorTableReader(InputStream input) {
        this.input = new BufferedReader(new InputStreamReader(input));
    }

    /**
     * Constructor.
     *
     * @param input
     *            Source stream
     */
    public MarkovPitchGeneratorTableReader(Reader input) {
        this.input = new BufferedReader(input);
    }

    /**
     * @return The generator table contained in the input
     */
    public MarkovPitchGeneratorTable readTable() throws IOException {
        return readTable(false);
    }

    /**
     * @param ignoreZeroStep
     *            Whether table entries corresponding to a step of zero should
     *            be ignored
     * @return The generator table contained in the input
     */
    public MarkovPitchGeneratorTable readTable(boolean ignoreZeroStep)
            throws IOException {
        int order = readOrder();
        MarkovPitchGeneratorTable table = new MarkovPitchGeneratorTable(order);
        String line = input.readLine();
        while (line != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                readTableEntry(line, table, ignoreZeroStep);
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
     * @param ignoreZeroStep
     *            Whether table entries corresponding to a step of zero should
     *            be ignored
     * @throws IOException
     *             If the next input is not a table entry
     */
    private void readTableEntry(String line, MarkovPitchGeneratorTable table,
            boolean ignoreZeroStep)
            throws IOException {
        Matcher matcher = ENTRY_PATTERN.matcher(line);
        boolean match = matcher.matches();
        if (!match) {
            throw new IOException(
                    "Expected '(<degree>,<alteration>,<ngram>):(<step>,<count>),...'");
        }
        // Read degree
        int degree;
        try {
            degree = Integer.parseUnsignedInt(matcher.group("degree"));
        } catch (NumberFormatException e) {
            throw new IOException(
                    "Expected '(<degree>,<alteration>,<ngram>):(<step>,<count>),...'");
        }
        if (degree < 1) {
            throw new IOException(
                    "Expected '(<degree>,<alteration>,<ngram>):(<step>,<count>),...'");
        }
        // Read alteration
        int alter = 0;
        String alterStr = matcher.group("alter");
        for (int i = 0; i < alterStr.length(); i++) {
            char c = alterStr.charAt(i);
            if (c == '#') {
                alter++;
            } else if (c == '-') {
                alter--;
            }
        }
        // Read ngram
        List<Integer> ngram;
        try {
            ngram = Arrays.stream(matcher.group("ngram").split(","))
                    .map(s -> new Integer(s)).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IOException(
                    "Expected '(<degree>,<alteration>,<ngram>):(<step>,<count>),...'");
        }
        // Read step/count pairs
        Map<Integer, Integer> transitions = new HashMap<>();
        for (String stepCount : matcher.group("trans").split(",")) {
            Matcher stepCountMatcher = STEP_COUNT_PATTERN.matcher(stepCount);
            boolean stepCountMatch = stepCountMatcher.matches();
            if (!stepCountMatch) {
                throw new IOException(
                        "Expected '(<degree>,<alteration>,<ngram>):(<step>,<count>),...'");
            }
            int step;
            int count;
            try {
                step = Integer.parseInt(stepCountMatcher.group("step"));
                count = Integer
                        .parseUnsignedInt(stepCountMatcher.group("count"));
            } catch (NumberFormatException e) {
                throw new IOException(
                        "Expected '(<degree>,<alteration>,<ngram>):(<step>,<count>),...'");
            }
            if (!ignoreZeroStep || step != 0) {
                transitions.put(step, count);
            }
        }
        // Write to table
        if (!transitions.isEmpty()) {
            table.setEntry(degree, alter, ngram, transitions);
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
