package com.example.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SecupiTestConfig {
    private static final String RESOURCE_NAME = "/secupi-test.properties";
    private final Properties properties;

    public SecupiTestConfig() {
        this.properties = loadProperties();
    }

    public String jdbcUrl() {
        return required("secupi.jdbc.url");
    }

    public String query() {
        String query = required("secupi.query").trim();
        if (!query.regionMatches(true, 0, "select", 0, "select".length())
                || (query.length() > "select".length()
                && !Character.isWhitespace(query.charAt("select".length())))) {
            throw new IllegalArgumentException("Configuration 'secupi.query' must be a single SELECT statement");
        }
        if (query.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Configuration 'secupi.query' must not contain semicolons");
        }
        return query;
    }

    public String username(String role) {
        return required("secupi.database." + role + ".username");
    }

    public String password(String role) {
        return required("secupi.database." + role + ".password");
    }

    public List<String> clearColumns(String role) {
        return columns("secupi.expected." + role + ".clear.columns");
    }

    public List<String> maskedColumns(String role) {
        return columns("secupi.expected." + role + ".masked.columns");
    }

    public Pattern maskedValuePattern() {
        String expression = required("secupi.masked.value.regex");
        try {
            return Pattern.compile(expression);
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("Invalid regular expression in 'secupi.masked.value.regex'",
                    exception);
        }
    }

    private List<String> columns(String propertyName) {
        String configuredColumns = find(propertyName);
        if (configuredColumns == null) {
            throw new IllegalStateException("Missing required SecuPI test configuration: " + propertyName);
        }
        if (configuredColumns.isBlank()) {
            return List.of();
        }

        String[] columns = configuredColumns.split(",", -1);
        List<String> normalizedColumns = new ArrayList<>();
        Map<String, Boolean> seenColumns = new LinkedHashMap<>();
        for (String column : columns) {
            String normalizedColumn = column.trim().toUpperCase(Locale.ROOT);
            if (normalizedColumn.isEmpty()) {
                throw new IllegalArgumentException("Column names must not be blank in " + propertyName);
            }
            if (seenColumns.put(normalizedColumn, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("Duplicate column " + normalizedColumn
                        + " in " + propertyName);
            }
            normalizedColumns.add(normalizedColumn);
        }
        return Collections.unmodifiableList(normalizedColumns);
    }

    private String required(String name) {
        String value = find(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required SecuPI test configuration: " + name
                    + " (set a -D property, matching environment variable, or config-file value)");
        }
        return value.trim();
    }

    private String find(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv(name.toUpperCase(Locale.ROOT).replace('.', '_'));
        }
        if (value == null) {
            value = properties.getProperty(name);
        }
        return value;
    }

    private static Properties loadProperties() {
        String externalConfig = System.getProperty("secupi.config.file");
        if (externalConfig == null || externalConfig.isBlank()) {
            externalConfig = System.getenv("SECUPI_CONFIG_FILE");
        }

        Properties properties = new Properties();
        if (externalConfig != null && !externalConfig.isBlank()) {
            Path path = Path.of(externalConfig);
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
                return properties;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load SecuPI test configuration file: " + path,
                        exception);
            }
        }

        try (InputStream input = SecupiTestConfig.class.getResourceAsStream(RESOURCE_NAME)) {
            if (input == null) {
                throw new IllegalStateException("Could not find " + RESOURCE_NAME + " on the classpath");
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load " + RESOURCE_NAME, exception);
        }
    }
}
