package uk.ac.bath.masmusic.conductor.generate;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import uk.ac.bath.masmusic.conductor.generation.MarkovPitchGeneratorTable;
import uk.ac.bath.masmusic.conductor.generation.MarkovPitchGeneratorTableReader;

public class MarkovPitchGeneratorTableReaderTest {

    static final String VALID_TABLE = "order:2\n"
            + "(0,[-2,-5]):(0:10),(2:6),(5:4)\n"
            + "(11,[3,3]):(2:5),(-3:3)\n";

    @Test
    public void test() throws IOException {
        try (MarkovPitchGeneratorTableReader reader = new MarkovPitchGeneratorTableReader(
                new StringReader(VALID_TABLE))) {
            MarkovPitchGeneratorTable table = reader.readTable();
            System.out.println(table);
        }
    }

}
