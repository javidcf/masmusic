package uk.ac.bath.masmusic.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

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
