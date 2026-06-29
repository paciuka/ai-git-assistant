package com.aigitassistant.config;

/**
 * Centralizes all application configuration by loading values
 * from environment variables.
 *
 * <p>Security principle: API keys are NEVER hardcoded. They are read
 * from the environment at runtime, following the 12-Factor App methodology.
 */
public class AppConfig {

    private static final String API_KEY_ENV = "ANTHROPIC_API_KEY";
    private static final String MODEL_ENV = "ANTHROPIC_MODEL";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";

    private final String apiKey;
    private final String model;

    /**
     * Loads configuration from environment variables.
     *
     * @throws IllegalStateException if the required API key is not set
     */
    public AppConfig() {
        this.apiKey = loadRequiredEnv(API_KEY_ENV);
        this.model = loadOptionalEnv(MODEL_ENV, DEFAULT_MODEL);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    /**
     * Loads a required environment variable or fails fast with a clear message.
     */
    private String loadRequiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + name + "\n"
                    + "Set it with: export " + name + "=your-api-key-here"
            );
        }
        return value.trim();
    }

    /**
     * Loads an optional environment variable, falling back to a default.
     */
    private String loadOptionalEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
}
