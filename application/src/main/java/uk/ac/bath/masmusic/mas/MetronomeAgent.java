package uk.ac.bath.masmusic.mas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.TimeSignature;

/**
 * An agent that replays the received notes.
 *
 * @author Javier Dehesa
 */
@Component
public class MetronomeAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MetronomeAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/metronomeAgent.asl";

    public MetronomeAgent() {
        initAgent(ASL_PATH);
    }

    @Override
    protected boolean doAction(ActionExec action) {
        Structure actionTerm = action.getActionTerm();
        if (actionTerm.getFunctor().equalsIgnoreCase("metronome")) {
            long start = System.currentTimeMillis();
            int bars = Integer.parseInt(actionTerm.getTerm(0).toString());
            int beatDuration = Integer
                    .parseInt(actionTerm.getTerm(1).toString());
            int beatPhase = Integer.parseInt(actionTerm.getTerm(2).toString());
            int barBeats = Integer.parseInt(actionTerm.getTerm(3).toString());
            int barUnit = Integer.parseInt(actionTerm.getTerm(4).toString());
            int barBeatOffset = Integer
                    .parseInt(actionTerm.getTerm(5).toString());
            Rhythm rhythm = new Rhythm(new Beat(beatDuration, beatPhase),
                    new TimeSignature(barBeats, barUnit), barBeatOffset);
            metronome(start, bars, rhythm);
            return true;
        } else {
            return false;
        }
    }

    private void metronome(long start, int beats, Rhythm rhythm) {
        // Emit notes displaying the bar structure
        final int FIRST_PITCH = 103;
        final int NEXT_PITCH = 96;
        Beat beat = rhythm.getBeat();
        int beatDuration = beat.getDuration();
        long currentBeat = beat.nextBeat(start);
        for (int iBar = 0; iBar < beats; iBar++) {
            int beatPosition = rhythm.beatPosition(currentBeat);
            playNote(beatPosition == 0 ? FIRST_PITCH : NEXT_PITCH, DEFAULT_VELOCITY, currentBeat, beatDuration);
            currentBeat = beat.nextBeat(currentBeat);
        }
    }

}
