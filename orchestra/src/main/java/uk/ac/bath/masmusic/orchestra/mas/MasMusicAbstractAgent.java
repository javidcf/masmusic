package uk.ac.bath.masmusic.orchestra.mas;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;

import jason.JasonException;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.runtime.Settings;

/**
 * Base class for MasMusic agents.
 *
 * @author Javier Dehesa
 */
public abstract class MasMusicAbstractAgent extends AgArch {

    /** Default note velocity. */
    public static final int DEFAULT_VELOCITY = 64;

    /** Default note duration. */
    public static final int DEFAULT_DURATION = 500;

    /** Event for heard notes. */
    public static final String HEAR_EVENT = "hear";

    /** MasMusic multi-agent system. */
    @Autowired
    private MasMusic masMusic;

    /** Agent perceived literals queue. */
    private final Queue<Literal> hearPercepts;

    public MasMusicAbstractAgent() {
        hearPercepts = new ConcurrentLinkedQueue<Literal>();
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
        Literal heard = hearPercepts.poll();
        if (heard != null) {
            return Collections.singletonList(heard);
        } else {
            return Collections.emptyList();
        }
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
        // Create Jason literal
        Literal literal = Literal.parseLiteral(
                String.format("%s(%d, %d)", HEAR_EVENT, pitch, velocity));
        hearPercepts.offer(literal);
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
