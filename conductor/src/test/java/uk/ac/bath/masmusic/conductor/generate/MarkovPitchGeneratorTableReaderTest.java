package uk.ac.bath.masmusic.conductor.generate;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class MarkovPitchGeneratorTableReaderTest {

    static final String VALID_TABLE = "order:2\n"
            + "(1,,[-2,-5]):(0:10),(2:6),(5:4)\n"
            + "(7,-,[3,3]):(2:5),(-3:3)\n";

    @Test
    public void test() throws IOException {
        try (MarkovPitchGeneratorTableReader reader = new MarkovPitchGeneratorTableReader(
                new StringReader(VALID_TABLE))) {
            MarkovPitchGeneratorTable table = reader.readTable();
            System.out.println(table);
        }
    }

}
