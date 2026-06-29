package com.aigitassistant.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Parses the raw JSON response from the Anthropic Messages API
 * and extracts the commit message text.
 *
 * <p>Anthropic's response structure (simplified):
 * <pre>{@code
 * {
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "feat(auth): add JWT token validation"
 *     }
 *   ],
 *   "stop_reason": "end_turn"
 * }
 * }</pre>
 *
 * <p>We navigate this JSON to extract the {@code text} field from the
 * first content block.
 */
public class ApiResponseParser {

    /**
     * Parses the Anthropic API JSON response and returns a {@link CommitMessage}.
     *
     * @param jsonResponse the raw JSON string from the API
     * @return a CommitMessage containing the generated text
     * @throws IllegalStateException if the response structure is unexpected
     *                               or contains no text content
     */
    public CommitMessage parse(String jsonResponse) {

        // --- 1. Parse the raw JSON string into a JsonObject -----------------
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

        // --- 2. Check for API-level errors ----------------------------------
        // The Anthropic API returns an "error" object when something goes wrong
        // (e.g., invalid API key, rate limit exceeded).
        if (root.has("error")) {
            JsonObject error = root.getAsJsonObject("error");
            String errorMessage = error.has("message")
                    ? error.get("message").getAsString()
                    : "Unknown API error";
            throw new IllegalStateException("Anthropic API error: " + errorMessage);
        }

        // --- 3. Navigate to the content array -------------------------------
        // The "content" field is an array of content blocks.
        // Claude typically returns a single text block.
        if (!root.has("content")) {
            throw new IllegalStateException(
                    "Unexpected API response: missing 'content' field."
            );
        }

        JsonArray contentArray = root.getAsJsonArray("content");

        if (contentArray.isEmpty()) {
            throw new IllegalStateException(
                    "Unexpected API response: 'content' array is empty."
            );
        }

        // --- 4. Extract the text from the first content block ---------------
        // Validate the content type. Claude can return different block types
        // (e.g., "tool_use" for function calling). We only expect "text".
        JsonObject firstBlock = contentArray.get(0).getAsJsonObject();
        String blockType = firstBlock.has("type")
                ? firstBlock.get("type").getAsString()
                : "unknown";

        if (!"text".equals(blockType)) {
            throw new IllegalStateException(
                    "Unexpected content block type from API: '" + blockType
                    + "'. Expected 'text'."
            );
        }

        String text = firstBlock.get("text").getAsString();

        // --- 5. Wrap in our domain model and return -------------------------
        return new CommitMessage(text.trim());
    }
}
