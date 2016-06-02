package uk.ac.bath.masmusic.orchestra.mas;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jason.JasonException;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.runtime.Settings;
import uk.ac.bath.masmusic.common.Note;

/**
 * Base class for MasMusic agents.
 *
 * @author Javier Dehesa
 */
public abstract class MasMusicAbstractAgent extends AgArch {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MasMusicAbstractAgent.class);

    /** Default note velocity. */
    public static final int DEFAULT_VELOCITY = 64;

    /** Default note duration. */
    public static final int DEFAULT_DURATION = 500;

    /** Event for heard notes. */
    public static final String HEAR_EVENT = "hear";

    /** Event for performance instructions. */
    public static final String PERFORM_EVENT = "perform";

    /** Event for beat. */
    public static final String BEAT_EVENT = "beat";

    /** Event for scale. */
    public static final String SCALE_EVENT = "scale";

    /** MasMusic multi-agent system. */
    @Autowired
    private MasMusic masMusic;

    /** Agent hearing literals queue. */
    private final Queue<Literal> heard;

    /** Agent instructions literals queue. */
    private final List<Literal> instructions;

    /** Currently perceived beat literal. */
    private Literal currentBeat;

    /** Currently perceived scale literal. */
    private Literal currentScale;

    /** Perceived literals. */
    private final List<Literal> percepts;

    public MasMusicAbstractAgent() {
        heard = new ConcurrentLinkedQueue<Literal>();
        instructions = Collections.synchronizedList(new ArrayList<>());
        currentBeat = null;
        currentScale = null;
        percepts = new ArrayList<>();
    }

    /**
     * Initialize an agent with the given ASL source.
     *
     * @param aslPath
     *            ASL source resource path
     */
    protected void initAgent(String aslPath) {
        URL resource = MasMusicAbstractAgent.class.getResource(aslPath);
        String agentSourcePath = "";
        try {
            agentSourcePath = Paths.get(resource.toURI()).toString();
            Agent ag = new Agent();
            new TransitionSystem(ag, new Circumstance(), new Settings(), this);
            ag.initAg(agentSourcePath);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (JasonException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Literal> perceive() {
        super.perceive();
        percepts.clear();
        if (currentBeat != null) {
            percepts.add(currentBeat);
        }
        if (currentScale != null) {
            percepts.add(currentScale);
        }
        if (!heard.isEmpty()) {
            percepts.add(heard.poll());
        }
        percepts.addAll(instructions);
        return percepts;
    }

    @Override
    public void act(ActionExec action, List<ActionExec> feedback) {
        super.act(action, feedback);
        if (doAction(action)) {
            action.setResult(true);
            feedback.add(action);
        }
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    /**
     * Carry out an action.
     *
     * @param action
     *            The action to perform
     * @return true if the action was carried out, false otherwise
     */
    protected abstract boolean doAction(ActionExec action);

    /**
     * Run a reasoning cycle of the agent.
     */
    public void reason() {
        if (isRunning()) {
            getTS().reasoningCycle();
        }
    }

    /**
     * Inform the agent about a note event.
     *
     * @param pitch
     *            Pitch of the note
     * @param velocity
     *            Velocity of the note
     * @param timestamp
     *            Timestamp at which the note was played
     */
    public void hear(int pitch, int velocity, long timestamp) {
        Literal literal = Literal.parseLiteral(
                String.format("%s(%d, %d)", HEAR_EVENT, pitch, velocity));
        heard.offer(literal);
    }

    /**
     * Instruct the agent to perform in a time span.
     *
     * @param start
     *            Timestamp of the beginning of the performance time span in
     *            milliseconds
     * @param duration
     *            Duration of the performance time span in milliseconds
     */
    public void perform(long start, long duration) {
        Literal literal = Literal.parseLiteral(
                String.format("%s(%d, %d)", PERFORM_EVENT, start,
                        duration));
        instructions.add(literal);
    }

    /**
     * Inform the agent about a new beat event.
     *
     * @param duration
     *            Duration of the new beat in milliseconds
     * @param phase
     *            Phase of the new beat in milliseconds
     */
    public void setBeat(int duration, int phase) {
        currentBeat = Literal
                .parseLiteral(String.format("%s(%d, %d)", BEAT_EVENT, duration, phase));
    }

    /**
     * Inform the agent about a new scale event.
     *
     * @param fundamental
     *            Note number of the fundamental of the scale
     * @param type
     *            Name of the scale type
     */
    public void setScale(int fundamental, String type) {
        String fundamentalStr = Note.fromValue(fundamental).toString();
        currentScale = Literal.parseLiteral(String.format("%s(%s, %s)", SCALE_EVENT,
                fundamentalStr.toLowerCase(), type.toLowerCase()));
    }

    /**
     * @param pitch
     *            Pitch of the played note
     * @param velocity
     *            Velocity of the played note
     * @param timestamp
     *            Start time of the played note
     * @param duration
     *            Duration of the played note
     */
    protected void playNote(int pitch, int velocity, long timestamp,
            int duration) {
        masMusic.play(pitch, velocity, timestamp, duration);
    }

}
