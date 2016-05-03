package uk.ac.bath.masmusic;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jason.JasonException;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.runtime.Settings;
import jm.music.data.Note;

public class ReplayAgent extends AgArch implements Perceiver {

    private static final String ASL_PATH = "asl/replayAgent.asl";

    private final Queue<Literal> hearPercepts;
    private final MusicSensorClient sensorClient;
    private final MusicPlayer sensor;

    public ReplayAgent(String serverAddress, String hearerClientName,
            String hearerTopic, String playerClientName,
            String playerTopic) {
        hearPercepts = new ConcurrentLinkedQueue<Literal>();
        sensorClient = new MusicSensorClient(serverAddress, hearerClientName,
                hearerTopic);
        sensor = new MusicPlayer(serverAddress, playerClientName, playerTopic);
        sensorClient.addPerceiver(this);
        initAgent();
    }

    private void initAgent() {
        URL resource = ReplayAgent.class.getResource(ASL_PATH);
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

    public void run() {
        while (isRunning()) {
            getTS().reasoningCycle();
        }
    }

    @Override
    public List<Literal> perceive() {
        Literal heard = hearPercepts.poll();
        if (heard != null) {
            return Collections.singletonList(heard);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void act(ActionExec action, List<ActionExec> feedback) {
        Structure actionTerm = action.getActionTerm();
        if (actionTerm.getFunctor().equalsIgnoreCase("play")) {
            int pitch = Integer.parseInt(actionTerm.getTerm(0).toString());
            Note note = new Note(pitch, 1);
            play(note);
            // percepts.clear(); // ??
            action.setResult(true);
            feedback.add(action);
        }
    }

    public void addPercept(Literal literal) {
        if (literal.getFunctor().equals(MasMusic.HEAR_EVENT))
            hearPercepts.offer(literal);
    }

    private void play(Note note) {
        long timestamp = System.currentTimeMillis();
        // sensor.publishMusic(note, MasMusic.DEFAULT_VELOCITY, timestamp);
        sensor.publishMusic(new Note(note.getPitch() + 7, 1),
                MasMusic.DEFAULT_VELOCITY, timestamp + 300);
        sensor.publishMusic(new Note(note.getPitch() + 12, 1),
                MasMusic.DEFAULT_VELOCITY, timestamp + 600);
        sensor.publishMusic(new Note(note.getPitch(), 1),
                MasMusic.DEFAULT_VELOCITY, timestamp + 900);
    }

}
