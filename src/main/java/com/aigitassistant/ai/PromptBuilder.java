package com.aigitassistant.ai;

/**
 * Builds the system and user prompts for the Anthropic API.
 *
 * <p>The system prompt is the most critical part of this application.
 * It instructs Claude to act as a commit message expert and strictly
 * follow the Conventional Commits specification.
 *
 * <p>Prompt engineering techniques used:
 * <ul>
 *   <li><b>Role assignment</b> — "You are a senior software engineer"</li>
 *   <li><b>Explicit format specification</b> — exact structure defined</li>
 *   <li><b>Constraint listing</b> — hard rules that prevent bad output</li>
 *   <li><b>Few-shot examples</b> — concrete examples of desired output</li>
 *   <li><b>Negative constraints</b> — "Do NOT" rules to prevent common issues</li>
 * </ul>
 */
public class PromptBuilder {

    /**
     * The system prompt uses a Java text block (triple quotes).
     * Text blocks preserve formatting and avoid messy string concatenation.
     *
     * This prompt is carefully engineered to produce consistent,
     * high-quality Conventional Commit messages.
     */
    private static final String SYSTEM_PROMPT = """
            You are a senior software engineer who is an expert at writing \
            Git commit messages following the Conventional Commits specification \
            (https://www.conventionalcommits.org/).

            Your task: Analyze the provided `git diff --cached` output and generate \
            a single, professional commit message.

            ## FORMAT RULES (strictly follow this structure):

            ```
            <type>(<optional-scope>): <short description>

            <optional body — explain WHAT changed and WHY>
            ```

            ## ALLOWED TYPES:
            - feat:     A new feature
            - fix:      A bug fix
            - docs:     Documentation only changes
            - style:    Formatting, missing semicolons, etc. (no code logic change)
            - refactor: Code change that neither fixes a bug nor adds a feature
            - test:     Adding or correcting tests
            - chore:    Build process, dependencies, or tooling changes
            - perf:     Performance improvement
            - ci:       CI/CD configuration changes
            - build:    Changes affecting the build system or dependencies

            ## CONSTRAINTS:
            1. The subject line MUST be lowercase and no longer than 72 characters.
            2. Do NOT end the subject line with a period.
            3. Use the imperative mood ("add feature" not "added feature").
            4. If the diff touches multiple concerns, pick the MOST significant type.
            5. Include a scope in parentheses when the change is clearly scoped \
               to a module, file, or feature area.
            6. Add a body ONLY when the "what" and "why" are not obvious from the \
               subject line alone.
            7. Output ONLY the commit message — no explanations, no markdown fences, \
               no extra commentary.

            ## EXAMPLES OF GOOD COMMIT MESSAGES:
            - feat(auth): add JWT token validation middleware
            - fix(parser): handle null pointer when input is empty
            - refactor(utils): extract date formatting into shared helper
            - docs(readme): add Docker usage instructions
            - chore(deps): bump gson from 2.10 to 2.13
            """;

    /**
     * Builds the user-facing message that includes the actual diff.
     *
     * @param diff the raw output from {@code git diff --staged}
     * @return the formatted user prompt
     */
    public String buildUserPrompt(String diff) {
        return """
                Analyze the following staged changes and generate a commit message:

                ```diff
                %s
                ```
                """.formatted(diff);
    }

    /**
     * Returns the system prompt that defines Claude's behavior.
     *
     * @return the system prompt string
     */
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
