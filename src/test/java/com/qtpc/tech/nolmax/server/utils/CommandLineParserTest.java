package com.qtpc.tech.nolmax.server.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineParserTest {

    @Test
    void parsesOptionsWithEqualsAndPreservesAdditionalEqualsInValue() {
        Map<String, String> parsed = CommandLineParser.parseArgs(new String[]{
                "--config=local=config.yml",
                "--server.port=18181"
        });

        assertEquals(Map.of(
                "config", "local=config.yml",
                "server.port", "18181"
        ), parsed);
    }

    @Test
    void parsesOptionsWithSeparateValuesAndBooleanFlags() {
        Map<String, String> parsed = CommandLineParser.parseArgs(new String[]{
                "--config", "config.yml",
                "--debug"
        });

        assertEquals(Map.of(
                "config", "config.yml",
                "debug", "true"
        ), parsed);
    }

    @Test
    void ignoresPositionalArgumentsAndUsesTheLastValueForRepeatedOptions() {
        Map<String, String> parsed = CommandLineParser.parseArgs(new String[]{
                "server.jar",
                "--server.port", "18181",
                "--server.port", "19191"
        });

        assertEquals(Map.of("server.port", "19191"), parsed);
    }
}
