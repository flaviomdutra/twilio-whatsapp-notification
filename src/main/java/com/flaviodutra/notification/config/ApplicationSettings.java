package com.flaviodutra.notification.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/** Application defaults from YAML, overridden by environment variables. */
public final class ApplicationSettings {
    private final JsonNode properties;
    private final Map<String, String> environment;

    private ApplicationSettings(JsonNode properties, Map<String, String> environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public static ApplicationSettings load() {
        try (InputStream stream = ApplicationSettings.class.getResourceAsStream("/application.yaml")) {
            if (stream == null) throw new IllegalStateException("Missing application.yaml");
            return fromYaml(stream, System.getenv());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load application.yaml", e);
        }
    }

    public static ApplicationSettings fromYaml(InputStream stream, Map<String, String> environment) throws IOException {
        JsonNode root = new ObjectMapper(new YAMLFactory()).readTree(stream);
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("application.yaml must contain a YAML mapping");
        }
        return new ApplicationSettings(root, environment);
    }

    public String get(String key, String environmentName) {
        if (environment.containsKey(environmentName)) return environment.get(environmentName);
        JsonNode value = properties;
        for (String segment : key.split("\\.")) {
            value = value.path(segment);
        }
        if (value.isMissingNode() || value.isNull()) return "";
        if (!value.isTextual()) throw new IllegalArgumentException("Configuration value must be a string: " + key);
        return value.textValue();
    }
}
