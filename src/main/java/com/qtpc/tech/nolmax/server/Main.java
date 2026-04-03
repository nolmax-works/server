package com.qtpc.tech.nolmax.server;

import com.nolmax.database.config.DatabaseConfig;
import com.qtpc.tech.nolmax.server.configuration.AppConfig;
import com.qtpc.tech.nolmax.server.configuration.ConfigLoader;
import com.qtpc.tech.nolmax.server.utils.CommandLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {
    public static AppConfig config;
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // parse cli arguments
            Map<String, String> cliArgs = CommandLineParser.parseArgs(args);

            // load configuration
            config = ConfigLoader.loadConfig(cliArgs);
            if (config == null) {
                return; // exit if default config was just created
            }

            // init db
            DatabaseConfig.initialize(
                config.database.address,
                config.database.port,
                config.database.db,
                config.database.username,
                config.database.password
            );

            // start netty server
            ChatServer server = new ChatServer(config);
            server.start();

        } catch (Exception e) {
            log.error("Failed to start the application", e);
        } finally {
            // cleanup on exit
            DatabaseConfig.close();
            log.info("Database connection closed.");
        }
    }
}