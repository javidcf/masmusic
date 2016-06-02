package uk.ac.bath.masmusic.orchestra.mas;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Scale;

/**
 * An agent that replays the received notes.
 *
 * @author Javier Dehesa
 */
@Component
public class ReplayAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(ReplayAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/replayAgent.asl";

    /** Random number generator. */
    private Random random = new Random();

    public ReplayAgent() {
        initAgent(ASL_PATH);
    }

    @Override
    protected boolean doAction(ActionExec action) {
        Structure actionTerm = action.getActionTerm();
        if (actionTerm.getFunctor().equalsIgnoreCase("play")) {
            int pitch = Integer.parseInt(actionTerm.getTerm(0).toString());
            long timestamp = System.currentTimeMillis();
            playNote(pitch, DEFAULT_VELOCITY, timestamp, DEFAULT_DURATION);
            return true;
        } else if (actionTerm.getFunctor().equalsIgnoreCase("compose")) {
            long start = Long.parseLong(actionTerm.getTerm(0).toString());
            long duration = Long.parseLong(actionTerm.getTerm(1).toString());
            int beatDuration = Integer
                    .parseInt(actionTerm.getTerm(2).toString());
            int beatPhase = Integer.parseInt(actionTerm.getTerm(3).toString());
            String fundamental = actionTerm.getTerm(4).toString();
            String scaleName = actionTerm.getTerm(5).toString();
            Beat beat = new Beat(beatDuration, beatPhase);
            Scale scale = new Scale(Note.fromString(fundamental), scaleName);
            compose(start, duration, beat, scale);
            return true;
        } else {
            return false;
        }
    }

    private void compose(long start, long duration, Beat beat, Scale scale) {
        // Come up with some random melody
        long end = start + duration;
        long currentBeat = beat.nextBeat(start);
        long lastBeat = beat.currentBeat(end);
        long currentTimestamp = currentBeat;
        // Middle fundamental
        int basePitch = 60 + scale.getFundamental().value();
        int currentDegree = 0;
        // Generate notes with random rhythmic patterns
        while (currentBeat < lastBeat) {
            final int DIVISIONS = 1;
            final int SLOTS = 1 << DIVISIONS;
            int filled = 0;
            while (filled < SLOTS) {
                // Choose note duration
                int durationSlotsExp = random
                        .nextInt(log2Int(SLOTS - filled) + 1);
                int durationSlots = 1 << durationSlotsExp;
                int noteDuration = (beat.getDuration() * durationSlots) / SLOTS;
                filled += durationSlots;

                // Choose note pitch
                int degreeStep = (random.nextBoolean() ? 1 : -1)
                        * (random.nextInt(3) + 1);
                currentDegree += degreeStep;
                // Change direction if going too far
                if ((currentDegree < 0 && basePitch <= 36)
                        || (currentDegree >= scale.size() && basePitch >= 72)) {
                    currentDegree -= degreeStep * 2;
                }
                // Adjust current octave
                while (currentDegree < 0) {
                    currentDegree += scale.size();
                    basePitch -= 12;
                }
                while (currentDegree >= scale.size()) {
                    currentDegree -= scale.size();
                    basePitch += 12;
                }
                int pitch = basePitch + scale.getInterval(currentDegree);

                // Play note
                playNote(pitch, DEFAULT_VELOCITY, currentTimestamp,
                        noteDuration);
                currentTimestamp += noteDuration;
            }
            currentBeat = beat.nextBeat(currentBeat);
            currentTimestamp = currentBeat;
        }
    }

    private static int log2Int(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.SIZE - 1 - Integer.numberOfLeadingZeros(value);
    }

}
