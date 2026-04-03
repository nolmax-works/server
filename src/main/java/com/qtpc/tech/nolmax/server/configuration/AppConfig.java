package com.qtpc.tech.nolmax.server.configuration;

import com.qtpc.tech.nolmax.server.Main;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppConfig {

    public Database database;
    public Server server;

    public static class Database {
        public String address;
        public int port;
        public String username;
        public String password;
        public String db;
    }

    public static class Server {
        public String listen_address;
        public int port;
        public boolean debug;
    }

    public static void createDefaultConfig(Path configPath) throws IOException {
        String defaultConfig = """
                database:
                  address: "127.0.0.1"
                  port: 5432
                  username: ""
                  password: ""
                  db: ""
                
                server:
                  listen_address: "0.0.0.0"
                  port: 18181
                  debug: false
                """;

        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, defaultConfig, StandardCharsets.UTF_8);
    }

    public static Path getJarDirectory() throws URISyntaxException {
        return Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
    }
}