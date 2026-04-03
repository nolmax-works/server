package com.qtpc.tech.nolmax.server.utils;

import java.util.HashMap;
import java.util.Map;

public class CommandLineParser {

    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (!arg.startsWith("--")) continue;

            String key;
            String value;

            if (arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2);
                key = split[0];
                value = split.length > 1 ? split[1] : "true";
            } else {
                key = arg.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                } else {
                    value = "true";
                }
            }

            result.put(key, value);
        }

        return result;
    }
}