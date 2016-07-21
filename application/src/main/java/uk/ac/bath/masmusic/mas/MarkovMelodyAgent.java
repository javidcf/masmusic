package uk.ac.bath.masmusic.mas;

import java.util.List;

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
import uk.ac.bath.masmusic.common.ScoreElement;
import uk.ac.bath.masmusic.common.TimeSignature;

/**
 * An agent that plays a melody generated through a Markov process.
 *
 * @author Javier Dehesa
 */
@Component
public class MarkovMelodyAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MarkovMelodyAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/markovMelodyAgent.asl";

    @Autowired
    private MasMusic masMusic;

    @Autowired
    private MelodyGenerator melodyGenerator;

    public MarkovMelodyAgent() {
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
            // Read parameters
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

            // Generate melody and play it
            List<ScoreElement> generated = melodyGenerator.generateMelody(scale, barBeats * bars);
            long currentTimestamp = rhythm.nextBar(start);
            for (ScoreElement element : generated) {
                int duration = (int) Math.round(element.getDuration() * rhythm.getBeat().getDuration());
                for (int pitch : element.getPitches()) {
                    masMusic.play(pitch, MasMusic.DEFAULT_VELOCITY, currentTimestamp, duration);
                }
                currentTimestamp += duration;
            }
            return true;
        } else {
            return false;
        }
    }

}
