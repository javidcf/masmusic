package uk.ac.bath.masmusic.orchestra.mas;

import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;

/**
 * An agent that replays the received notes.
 *
 * @author Javier Dehesa
 */
@Component
public class ReplayAgent extends MasMusicAbstractAgent {

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/replayAgent.asl";

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
            playNote(pitch + 7, DEFAULT_VELOCITY, timestamp + 300,
                    DEFAULT_DURATION);
            playNote(pitch + 12, DEFAULT_VELOCITY, timestamp + 600,
                    DEFAULT_DURATION);
            playNote(pitch + 7, DEFAULT_VELOCITY, timestamp + 900,
                    DEFAULT_DURATION);
            return true;
        } else {
            return false;
        }
    }

}
