package com.aigitassistant.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for the Anthropic Messages API.
 *
 * <p>Uses Java's built-in {@link HttpClient} (available since Java 11)
 * instead of third-party libraries like OkHttp or Apache HttpClient.
 * This keeps our dependency footprint minimal.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>The client is constructed with its dependencies (API key, model)
 *       via constructor injection — making it testable.</li>
 *   <li>Request timeout is set to 30 seconds to handle large diffs.</li>
 *   <li>The Gson instance is reused (thread-safe, immutable).</li>
 * </ul>
 */
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Creates a new AnthropicClient.
     *
     * @param apiKey the Anthropic API key (from environment variable)
     * @param model  the model identifier (e.g., "claude-sonnet-4-20250514")
     */
    public AnthropicClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        // LIFECYCLE NOTE: java.net.http.HttpClient manages an internal thread pool
        // and connection pool. In Java 21+ it implements AutoCloseable, but in Java 17
        // it does not. For a single-shot CLI tool, the JVM handles cleanup on exit.
        // In a long-running server application, we would need to manage this explicitly.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.gson = new Gson();
    }

    /**
     * Sends the system and user prompts to Claude and returns the raw
     * JSON response body.
     *
     * @param systemPrompt the system prompt defining Claude's behavior
     * @param userPrompt   the user prompt containing the diff
     * @return the raw JSON response string from the Anthropic API
     * @throws IOException          if the HTTP request fails
     * @throws InterruptedException if the request is interrupted
     * @throws IllegalStateException if the API returns a non-2xx status code
     */
    public String sendMessage(String systemPrompt, String userPrompt)
            throws IOException, InterruptedException {

        // --- 1. Build the JSON request body ---------------------------------
        String requestBody = buildRequestBody(systemPrompt, userPrompt);

        // --- 2. Construct the HTTP request ----------------------------------
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)                  // Anthropic auth
                .header("anthropic-version", API_VERSION)      // Required API version header
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // --- 3. Send the request and get the response -----------------------
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // --- 4. Validate the HTTP status code -------------------------------
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            // SECURITY: Truncate the error body to prevent leaking sensitive data.
            // Some API error responses can echo back request headers or partial
            // authentication info — we never want that printed to a terminal or log.
            String errorBody = response.body();
            String safeBody = errorBody.length() > 500
                    ? errorBody.substring(0, 500) + "... [truncated]"
                    : errorBody;
            throw new IllegalStateException(
                    "Anthropic API returned HTTP " + statusCode + ": " + safeBody
            );
        }

        return response.body();
    }

    /**
     * Builds the JSON payload for the Anthropic Messages API.
     *
     * <p>The expected structure:
     * <pre>{@code
     * {
     *   "model": "claude-sonnet-4-20250514",
     *   "max_tokens": 1024,
     *   "system": "You are a senior software engineer...",
     *   "messages": [
     *     { "role": "user", "content": "Analyze the following..." }
     *   ]
     * }
     * }</pre>
     *
     * <p>We build this using Gson's JsonObject API for type safety
     * instead of raw string concatenation (which is error-prone with
     * special characters in diffs).
     */
    private String buildRequestBody(String systemPrompt, String userPrompt) {
        // Build the user message object
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);

        // Build the messages array
        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        // Build the top-level request object
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", MAX_TOKENS);
        requestBody.addProperty("system", systemPrompt);
        requestBody.add("messages", messages);

        return gson.toJson(requestBody);
    }
}
