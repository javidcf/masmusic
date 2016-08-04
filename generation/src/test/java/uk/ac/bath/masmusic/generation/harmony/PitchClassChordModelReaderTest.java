package uk.ac.bath.masmusic.generation.harmony;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class PitchClassChordModelReaderTest {

    private final static String MODEL_DATA = "0:(0;[0;4;7]:1.2),(7;[0;4;7]:0.4)\n"
            + "4:(0;[0;4;7]:0.5),(7;[0;4;7]:0.3),(4;[0;3;7]:1.1)\n"
            + "7:(0;[0;7]:0.1),(2;[0;3;7]:0.3)\n";

    @Test
    public void testReadModel() throws IOException {
        try (PitchClassChordModelReader reader = new PitchClassChordModelReader(new StringReader(MODEL_DATA))) {
            PitchClassChordModel model = reader.readModel();
            // System.out.println(model);
        }
    }

}
