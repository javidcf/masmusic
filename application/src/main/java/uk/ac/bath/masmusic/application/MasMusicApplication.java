package uk.ac.bath.masmusic.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import uk.ac.bath.masmusic.mas.MasMusic;

/**
 * MasMusic application entry point.
 *
 * @author Javier Dehesa
 */
@SpringBootApplication
@ComponentScan("uk.ac.bath.masmusic")
public class MasMusicApplication implements CommandLineRunner {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MasMusicApplication.class);

    @Value("${mqtt.username}")
    private String  mqttUsername;
    @Value("${mqtt.password}")
    private String  mqttPassword;
    @Value("${mqtt.url}")
    private String  mqttUrl;
    @Value("${mqtt.qos}")
    private int     mqttQos;
    @Value("${mqtt.retain}")
    private boolean mqttRetain;
    @Value("${mqtt.hear.client.id}")
    private String  mqttHearClientId;
    @Value("${mqtt.hear.topic}")
    private String  mqttHearTopic;
    @Value("${mqtt.play.client.id}")
    private String  mqttPlayClientId;
    @Value("${mqtt.play.topic}")
    private String  mqttPlayTopic;

    @Autowired
    private MasMusic masMusic;

    /**
     * Start the conductor.
     *
     * @param args
     *            Command-line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SpringApplication.run(MasMusicApplication.class, args);
    }

    @Override
    public void run(String... args) {
        LOG.info("MasMusic started, press Ctrl+C to finish...");
    }

}
