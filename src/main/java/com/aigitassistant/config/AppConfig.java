package com.aigitassistant.config;

/**
 * Centralizes all application configuration by loading values
 * from environment variables.
 *
 * <p>Security principle: API keys are NEVER hardcoded. They are read
 * from the environment at runtime, following the 12-Factor App methodology.
 *
 * <p>Provider detection: The config auto-detects which AI provider to use
 * based on which API key environment variable is set:
 * <ul>
 *   <li>{@code ANTHROPIC_API_KEY} → Anthropic Claude</li>
 *   <li>{@code GEMINI_API_KEY} → Google Gemini</li>
 * </ul>
 */
public class AppConfig {

    /**
     * Supported AI providers. Adding a new provider requires:
     * 1. Adding a value here
     * 2. Implementing {@code AiClient} interface
     * 3. Adding detection logic in {@code AppConfig} constructor
     */
    public enum Provider {
        ANTHROPIC, GEMINI
    }

    // --- Environment Variable Names -----------------------------------------
    private static final String ANTHROPIC_KEY_ENV = "ANTHROPIC_API_KEY";
    private static final String GEMINI_KEY_ENV = "GEMINI_API_KEY";
    private static final String MODEL_ENV = "AI_MODEL";

    // --- Default Models -----------------------------------------------------
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514";
    // We use gemini-flash-latest as it reliably maps to the currently active
    // free-tier flash model (e.g., 2.5-flash or 3.5-flash), avoiding 404s on
    // deprecated models and 429s on restricted ones.
    private static final String DEFAULT_GEMINI_MODEL = "gemini-flash-latest";

    private final Provider provider;
    private final String apiKey;
    private final String model;

    /**
     * Loads configuration from environment variables.
     * Auto-detects the AI provider based on which API key is set.
     *
     * @throws IllegalStateException if no API key is configured
     */
    public AppConfig() {
        String anthropicKey = System.getenv(ANTHROPIC_KEY_ENV);
        String geminiKey = System.getenv(GEMINI_KEY_ENV);

        // --- Provider auto-detection ----------------------------------------
        // Priority: Anthropic first (if both are set), then Gemini.
        if (anthropicKey != null && !anthropicKey.isBlank()) {
            this.provider = Provider.ANTHROPIC;
            this.apiKey = anthropicKey.trim();
            this.model = loadOptionalEnv(MODEL_ENV, DEFAULT_ANTHROPIC_MODEL);
        } else if (geminiKey != null && !geminiKey.isBlank()) {
            this.provider = Provider.GEMINI;
            this.apiKey = geminiKey.trim();
            this.model = loadOptionalEnv(MODEL_ENV, DEFAULT_GEMINI_MODEL);
        } else {
            throw new IllegalStateException(
                    "No API key found. Set one of the following environment variables:\n"
                    + "  • export ANTHROPIC_API_KEY=your-key   (for Claude)\n"
                    + "  • export GEMINI_API_KEY=your-key      (for Gemini — free tier available)"
            );
        }
    }

    public Provider getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    /**
     * Returns a human-readable provider name for terminal output.
     */
    public String getProviderDisplayName() {
        return switch (provider) {
            case ANTHROPIC -> "Claude (" + model + ")";
            case GEMINI -> "Gemini (" + model + ")";
        };
    }

    /**
     * Loads an optional environment variable, falling back to a default.
     */
    private String loadOptionalEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
}
