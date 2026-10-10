package com.example.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {
    private static final Properties PROPERTIES = loadProperties();

    private Config() {
    }

    public static String browser() {
        return get("browser", "BROWSER");
    }

    public static boolean headless() {
        String value = get("headless", "HEADLESS");
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Configuration 'headless' must be true or false");
    }

    public static String baseUrl() {
        return get("base.url", "BASE_URL");
    }

    public static int explicitWaitSeconds() {
        int seconds = Integer.parseInt(get("explicit.wait.seconds", "EXPLICIT_WAIT_SECONDS"));
        if (seconds <= 0) {
            throw new IllegalArgumentException("Configuration 'explicit.wait.seconds' must be greater than zero");
        }
        return seconds;
    }

    private static String get(String propertyName, String environmentName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentName);
        }
        if (value == null || value.isBlank()) {
            value = PROPERTIES.getProperty(propertyName);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration: " + propertyName);
        }
        return value.trim();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = Config.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new IllegalStateException("Could not find /config.properties on the classpath");
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load /config.properties", exception);
        }
    }
}
