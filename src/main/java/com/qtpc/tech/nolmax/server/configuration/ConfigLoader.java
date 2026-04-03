package com.qtpc.tech.nolmax.server.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    public static AppConfig loadConfig(Map<String, String> cliArgs) throws Exception {
        Path configPath = cliArgs.containsKey("config") ? Path.of(cliArgs.get("config")) : AppConfig.getJarDirectory().resolve("config.yml");

        if (Files.notExists(configPath)) {
            AppConfig.createDefaultConfig(configPath);
            log.info("Created default config.yml at: {}", configPath.toAbsolutePath());
            log.info("Exiting the app so you can edit the file...");
            return null; // return null to indicate the app should gracefully exit
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        AppConfig config = mapper.readValue(configPath.toFile(), AppConfig.class);
        applyOverrides(config, cliArgs);

        return config;
    }

    private static void applyOverrides(AppConfig config, Map<String, String> cliArgs) {
        if (config == null) return;

        if (cliArgs.containsKey("database.address")) config.database.address = cliArgs.get("database.address");
        if (cliArgs.containsKey("database.port")) config.database.port = Integer.parseInt(cliArgs.get("database.port"));
        if (cliArgs.containsKey("database.username")) config.database.username = cliArgs.get("database.username");
        if (cliArgs.containsKey("database.password")) config.database.password = cliArgs.get("database.password");
        if (cliArgs.containsKey("database.db")) config.database.db = cliArgs.get("database.db");

        if (cliArgs.containsKey("server.listen_address")) config.server.listen_address = cliArgs.get("server.listen_address");
        if (cliArgs.containsKey("server.port")) config.server.port = Integer.parseInt(cliArgs.get("server.port"));
        if (cliArgs.containsKey("server.debug")) config.server.debug = Boolean.parseBoolean(cliArgs.get("server.debug"));
    }
}