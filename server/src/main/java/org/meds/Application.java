package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.map.MapManager;
import org.meds.server.Server;
import org.meds.util.Random;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * A cThe entry point of the application. The class contains <code>main</code> method that loads
 * the application context and does some initialization stuff.
 */
public class Application {

    private static Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(ApplicationConfig.class);

        try {
            Random.initialize();

            // Load map data
            MapManager mapManager = applicationContext.getBean(MapManager.class);
            logger.info("Map is loaded");

            // Load locale
            applicationContext.getBean(Locale.class);

            Server server = applicationContext.getBean(Server.class);
            server.start();
        } catch (Exception ex) {
            logger.fatal("An exception has occurred while starting the Server", ex);
        }
    }
}
