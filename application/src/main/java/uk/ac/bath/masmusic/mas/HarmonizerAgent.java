package uk.ac.bath.masmusic.mas;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;
import uk.ac.bath.masmusic.events.MusicInputBufferUpdatedEvent;

/**
 * An agent that plays a melody generated through a Markov process.
 *
 * @author Javier Dehesa
 */
@Component
public class HarmonizerAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(HarmonizerAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/harmonizerAgent.asl";

    /** Literal indicating that an harmonization is available. */
    private static final Literal HARMONIZATION_AVAILABLE = Literal.parseLiteral("harmonizationAvailable");

    @Autowired
    private MasMusic masMusic;

    @Autowired
    private HarmonyGenerator harmonyGenerator;

    public HarmonizerAgent() {
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
        } else if (actionTerm.getFunctor().equalsIgnoreCase("harmonize")) {
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
            Beat beat = new Beat(beatDuration, beatPhase);
            TimeSignature timeSignature = new TimeSignature(barBeats, barUnit);
            Rhythm rhythm = new Rhythm(beat, timeSignature, barBeatOffset);
            Scale scale = new Scale(Note.fromValue(fundamental), scaleName);

            // Generate harmony and play it
            List<Onset> harmony = harmonyGenerator.getHarmony(scale, rhythm, start, bars);
            for (Onset onset : harmony) {
                masMusic.play(onset.getPitch(), onset.getVelocity(), onset.getTimestamp(), onset.getDuration());
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Literal> perceive() {
        List<Literal> percepts = super.perceive();
        if (harmonyGenerator.hasHarmonization()) {
            percepts.add(HARMONIZATION_AVAILABLE);
        }
        return percepts;
    }

    /**
     * Handle an input update event.
     *
     * @param event
     *            The input update event
     */
    @EventListener
    public void onInputUpdated(MusicInputBufferUpdatedEvent event) {
        Scale scale = masMusic.getScale();
        Rhythm rhythm = masMusic.getRhythm();
        if (scale != null && rhythm != null) {
            harmonyGenerator.harmonize(scale, rhythm, event.getInputBuffer());
        }
    }

}
