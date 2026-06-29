package com.aigitassistant;

import com.aigitassistant.ai.AiClient;
import com.aigitassistant.ai.AnthropicClient;
import com.aigitassistant.ai.GeminiClient;
import com.aigitassistant.ai.PromptBuilder;
import com.aigitassistant.config.AppConfig;
import com.aigitassistant.git.GitDiffProvider;
import com.aigitassistant.model.ApiResponseParser;
import com.aigitassistant.model.CommitMessage;

/**
 * Entry point for the AI Git Assistant CLI tool.
 *
 * <p>This class is the <b>composition root</b> — it wires together all
 * components and orchestrates the pipeline:
 * <ol>
 *   <li>Load configuration (API key from env vars)</li>
 *   <li>Execute {@code git diff --cached} to capture changes</li>
 *   <li>Build the prompt with the diff</li>
 *   <li>Send to the configured AI provider (Claude or Gemini)</li>
 *   <li>Display the commit message</li>
 * </ol>
 *
 * <p>ANSI escape codes are used for colorful terminal output to
 * improve the developer experience.
 */
public class Main {

    // ========================================================================
    // ANSI Escape Codes for Terminal Colors
    // ========================================================================
    // These work on most modern terminals (macOS, Linux, Windows Terminal,
    // Git Bash). They add zero dependencies — just raw escape sequences.
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String CYAN    = "\u001B[36m";
    private static final String RED     = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";

    // DEFENSIVE PROGRAMMING: Large diffs (e.g., staging a binary file or
    // generated code) can cause OutOfMemoryError and waste API tokens.
    // 2000 lines is generous for real code changes while preventing abuse.
    private static final int MAX_DIFF_LINES = 2000;

    public static void main(String[] args) {
        // --- CLI --help flag -------------------------------------------------
        // Even minimal CLI tools should support --help. It shows interviewers
        // you think about user experience, not just functionality.
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printHelp();
            return;
        }

        try {
            printBanner();
            run();
        } catch (IllegalStateException e) {
            // Known, expected errors (no staged changes, missing API key, etc.)
            printError(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // Unexpected errors — show the full exception for debugging
            printError("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The main application pipeline. Each step is clearly separated
     * and delegated to its responsible class.
     */
    private static void run() throws Exception {

        // --- Step 1: Load configuration -------------------------------------
        printStep("Loading configuration...");
        AppConfig config = new AppConfig();
        printSuccess("API key loaded successfully.");

        // --- Step 2: Get the staged diff ------------------------------------
        printStep("Reading staged changes (git diff --cached)...");
        GitDiffProvider gitDiffProvider = new GitDiffProvider();
        String diff = gitDiffProvider.getStagedDiff();
        long lineCount = diff.lines().count();

        // GUARD: Reject oversized diffs before sending to the API.
        // This prevents OutOfMemoryError from massive binary/generated files
        // and avoids wasting Anthropic API tokens on unprocessable input.
        if (lineCount > MAX_DIFF_LINES) {
            throw new IllegalStateException(
                    "Staged diff is too large (" + lineCount + " lines, max " + MAX_DIFF_LINES + "). "
                    + "Please stage fewer files or split your commit."
            );
        }
        printSuccess("Captured " + lineCount + " lines of staged changes.");

        // --- Step 3: Build the prompt ---------------------------------------
        printStep("Constructing prompt for AI...");
        PromptBuilder promptBuilder = new PromptBuilder();
        String systemPrompt = promptBuilder.getSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(diff);
        printSuccess("Prompt ready.");

        // --- Step 4: Send to AI Provider ------------------------------------
        printStep("Sending diff to " + config.getProviderDisplayName() + "...");
        AiClient client = switch (config.getProvider()) {
            case ANTHROPIC -> new AnthropicClient(config.getApiKey(), config.getModel());
            case GEMINI -> new GeminiClient(config.getApiKey(), config.getModel());
        };
        
        // The clients now parse the JSON internally and return the clean text
        String messageText = client.sendMessage(systemPrompt, userPrompt);
        printSuccess("Response received.");

        // --- Step 5: Process the response -----------------------------------
        CommitMessage commitMessage = new CommitMessage(messageText);

        // --- Step 6: Display the result -------------------------------------
        printCommitMessage(commitMessage);
    }

    // ========================================================================
    // Terminal Output Helpers
    // ========================================================================

    private static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD + "  ╔══════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + BOLD + "  ║       🤖 AI Git Assistant            ║" + RESET);
        System.out.println(CYAN + BOLD + "  ║   Smart Commits, Powered by AI       ║" + RESET);
        System.out.println(CYAN + BOLD + "  ╚══════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private static void printStep(String message) {
        System.out.println(DIM + "  ⏳ " + message + RESET);
    }

    private static void printSuccess(String message) {
        System.out.println(GREEN + "  ✔ " + message + RESET);
    }

    private static void printError(String message) {
        System.out.println();
        System.out.println(RED + BOLD + "  ✖ ERROR: " + message + RESET);
        System.out.println();
    }

    private static void printHelp() {
        printBanner();
        System.out.println(BOLD + "  USAGE:" + RESET);
        System.out.println("    java -jar ai-git-assistant.jar [OPTIONS]");
        System.out.println();
        System.out.println(BOLD + "  DESCRIPTION:" + RESET);
        System.out.println("    Analyzes staged Git changes and generates a professional");
        System.out.println("    commit message using Claude or Gemini (Conventional Commits format).");
        System.out.println();
        System.out.println(BOLD + "  OPTIONS:" + RESET);
        System.out.println("    -h, --help    Show this help message and exit");
        System.out.println();
        System.out.println(BOLD + "  ENVIRONMENT VARIABLES (Set ONE of these):" + RESET);
        System.out.println(GREEN + "    ANTHROPIC_API_KEY" + RESET + "   Your Anthropic API key (for Claude)");
        System.out.println(GREEN + "    GEMINI_API_KEY" + RESET + "      Your Google Gemini API key (Free tier)");
        System.out.println(DIM   + "    AI_MODEL" + RESET + "            (optional) Override the default model");
        System.out.println();
        System.out.println(BOLD + "  EXAMPLE:" + RESET);
        System.out.println(CYAN + "    git add ." + RESET);
        System.out.println(CYAN + "    java -jar ai-git-assistant.jar" + RESET);
        System.out.println();
    }

    private static void printCommitMessage(CommitMessage commitMessage) {
        System.out.println();
        System.out.println(YELLOW + "  ─────────────────────────────────────" + RESET);
        System.out.println(MAGENTA + BOLD + "  📝 Suggested Commit Message:" + RESET);
        System.out.println(YELLOW + "  ─────────────────────────────────────" + RESET);
        System.out.println();

        // Indent each line of the commit message for clean formatting
        String[] lines = commitMessage.message().split("\n");
        for (String line : lines) {
            System.out.println(GREEN + BOLD + "    " + line + RESET);
        }

        System.out.println();
        System.out.println(YELLOW + "  ─────────────────────────────────────" + RESET);
        System.out.println(DIM + "  Copy the message above and use it with:" + RESET);
        System.out.println(CYAN + "  git commit -m \"<message>\"" + RESET);
        System.out.println();
    }
}
