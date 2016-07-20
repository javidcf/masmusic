package uk.ac.bath.masmusic.mas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;

/**
 * An agent that plays variations of the received melody.
 *
 * @author Javier Dehesa
 */
@Component
public class VariationsAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(VariationsAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/variationsAgent.asl";

    @Autowired
    private MelodyGenerator melodyGenerator;

    public VariationsAgent() {
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
            int bars = Integer.parseInt(actionTerm.getTerm(1).toString());
            int beatDuration = Integer.parseInt(actionTerm.getTerm(2).toString());
            int beatPhase = Integer.parseInt(actionTerm.getTerm(3).toString());
            int barBeats = Integer.parseInt(actionTerm.getTerm(4).toString());
            int barUnit = Integer.parseInt(actionTerm.getTerm(5).toString());
            int barBeatOffset = Integer.parseInt(actionTerm.getTerm(6).toString());
            int fundamental = Integer.parseInt(actionTerm.getTerm(7).toString());
            String scaleName = actionTerm.getTerm(8).toString();
            Rhythm rhythm = new Rhythm(new Beat(beatDuration, beatPhase),
                    new TimeSignature(barBeats, barUnit), barBeatOffset);
            Scale scale = new Scale(Note.fromValue(fundamental), scaleName);
            melodyGenerator.generateMelody(start);
            return true;
        } else {
            return false;
        }
    }

}
