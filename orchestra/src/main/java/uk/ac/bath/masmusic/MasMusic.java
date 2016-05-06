package uk.ac.bath.masmusic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class MasMusic {

    public static final String JSON_TIMESTAMP = "at";
    public static final String JSON_EVENT_DATA = "data";

    public static final String HEAR_EVENT = "hear";

    public static final int DEFAULT_VELOCITY = 64;

    public static void main(String[] args) {
        Properties conf = new Properties();
        try {
            InputStream in = MasMusic.class.getResourceAsStream("conf.properties");
            conf.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String serverAddress = conf.getProperty("server.address");
        String hearerClientName = conf.getProperty("hearer.client.name");
        String hearerTopic = conf.getProperty("hearer.topic");
        String playerClientName = conf.getProperty("player.client.name");
        String playerTopic = conf.getProperty("player.topic");
        ReplayAgent agent = new ReplayAgent(serverAddress, hearerClientName, hearerTopic, playerClientName,
                playerTopic);
        agent.run();
    }
}
