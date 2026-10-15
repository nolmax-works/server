package com.qtpc.tech.nolmax.server.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsServerSettingsAndAppliesCommandLineOverrides() throws Exception {
        Path configPath = tempDir.resolve("config.yml");
        Files.writeString(configPath, """
                database:
                  address: "db.example.test"
                  port: 5432
                  username: "chat-user"
                  password: "secret"
                  db: "chat"
                server:
                  listen_address: "127.0.0.1"
                  port: 18181
                  debug: false
                """);

        AppConfig config = ConfigLoader.loadConfig(Map.of(
                "config", configPath.toString(),
                "server.listen_address", "0.0.0.0",
                "server.port", "19191",
                "server.debug", "true"
        ));

        assertEquals("0.0.0.0", config.server.listen_address);
        assertEquals(19191, config.server.port);
        assertTrue(config.server.debug);
    }

    @Test
    void createsDefaultConfigAndReturnsNullWhenConfigFileIsMissing() throws Exception {
        Path configPath = tempDir.resolve("new").resolve("config.yml");

        AppConfig config = ConfigLoader.loadConfig(Map.of("config", configPath.toString()));

        assertNull(config);
        assertTrue(Files.exists(configPath));
        String contents = Files.readString(configPath);
        assertTrue(contents.contains("address: \"127.0.0.1\""));
        assertTrue(contents.contains("listen_address: \"0.0.0.0\""));
        assertTrue(contents.contains("port: 18181"));
        assertTrue(contents.contains("debug: false"));
    }
}
