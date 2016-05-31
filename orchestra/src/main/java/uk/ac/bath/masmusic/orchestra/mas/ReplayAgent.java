package uk.ac.bath.masmusic.orchestra.mas;

import java.util.Random;

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

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/replayAgent.asl";

    /** RNG. */
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
            // playNote(pitch + 7, DEFAULT_VELOCITY, timestamp + 300,
            // DEFAULT_DURATION);
            // playNote(pitch + 12, DEFAULT_VELOCITY, timestamp + 600,
            // DEFAULT_DURATION);
            // playNote(pitch + 7, DEFAULT_VELOCITY, timestamp + 900,
            // DEFAULT_DURATION);
            // playNote(pitch + 12, DEFAULT_VELOCITY, timestamp,
            // DEFAULT_DURATION);
            return true;
        } else if (actionTerm.getFunctor().equalsIgnoreCase("compose")) {
            long tStart = Long.parseLong(actionTerm.getTerm(0).toString());
            long tEnd = Long.parseLong(actionTerm.getTerm(1).toString());
            int beatDuration = Integer.parseInt(actionTerm.getTerm(2).toString());
            int beatPhase = Integer.parseInt(actionTerm.getTerm(3).toString());
            String fundamental = actionTerm.getTerm(4).toString();
            String scaleName = actionTerm.getTerm(5).toString();
            Beat beat = new Beat(beatDuration, beatPhase);
            Scale scale = new Scale(Note.fromString(fundamental), scaleName);
            compose(tStart, tEnd, beat, scale);
            return true;
        } else {
            return false;
        }
    }

    private void compose(long tStart, long tEnd, Beat beat, Scale scale) {
        // Come up with some random melody
        long currentTimestamp = beat.nextBeat(tStart);
        long lastBeat = beat.currentBeat(tEnd);
        // Middle fundamental
        int basePitch = 60 + scale.getFundamental().value();
        int currentDegree = 0;
        // Generate notes
        while (currentTimestamp < lastBeat) {
            int degreeStep = (random.nextBoolean() ? 1 : -1) * random.nextInt(3);
            currentDegree += degreeStep;
            while (currentDegree < 0) {
                currentDegree += scale.size();
                basePitch -= 12;
            }
            while (currentDegree >= scale.size()) {
                currentDegree -= scale.size();
                basePitch += 12;
            }
            int pitch = basePitch + scale.getInterval(currentDegree);
            playNote(pitch, DEFAULT_VELOCITY, currentTimestamp, beat.getDuration());
            currentTimestamp = beat.nextBeat(currentTimestamp);
        }
    }

}
