package org.danielper.swapi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Secrets {
    private static final Logger log = LogManager.getLogger(Secrets.class);

    public static String getOrDefault(final String env, final String def){
        final var envValue = System.getenv().getOrDefault(env, def);

        if(envValue.contains("/")){
            try {
                return Files.readString(Paths.get(envValue));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }

        return envValue;
    }
}
