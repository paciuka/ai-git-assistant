package com.aigitassistant.model;

/**
 * Immutable value object that holds the generated commit message.
 *
 * <p>Using a Java 17 {@code record} here because:
 * <ul>
 *   <li>Records auto-generate constructor, getters, equals, hashCode, toString.</li>
 *   <li>They are inherently immutable — no setters, fields are final.</li>
 *   <li>Perfect for "data carrier" objects like API responses.</li>
 * </ul>
 *
 * @param message the generated commit message following Conventional Commits
 */
public record CommitMessage(String message) {

    /**
     * Compact constructor — validates that the message is not blank.
     * This runs automatically before the canonical constructor.
     */
    public CommitMessage {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Commit message cannot be null or blank.");
        }
    }
}
