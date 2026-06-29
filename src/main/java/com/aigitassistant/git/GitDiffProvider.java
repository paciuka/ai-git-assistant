package com.aigitassistant.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Responsible for executing the {@code git diff --cached} command
 * and returning the raw diff output as a String.
 *
 * <p>Design notes:
 * <ul>
 *   <li>It uses {@code ProcessBuilder} which is preferred over
 *       {@code Runtime.exec()} for better control over I/O.</li>
 *   <li>It handles standard error streams to provide meaningful error
 *       messages if {@code git diff --cached} fails.</li>
 *   <li>It uses try-with-resources to ensure streams are properly closed,
 *       preventing resource leaks.</li>
 *   <li>Throws descriptive, custom-message exceptions rather than
 *       returning null — fail fast, fail loud.</li>
 * </ul>
 */
public class GitDiffProvider {

    /**
     * Executes {@code git diff --cached} in the current working directory
     * and returns the diff output.
     *
     * @return the staged diff as a non-empty String
     * @throws IOException          if the git process cannot be started
     *                              (e.g., git is not installed)
     * @throws InterruptedException if the process is interrupted while waiting
     * @throws IllegalStateException if there are no staged changes or
     *                               the command fails (e.g., not a git repo)
     */
    public String getStagedDiff() throws IOException, InterruptedException {

        // --- 1. Configure the ProcessBuilder --------------------------------
        // "git diff --cached" shows changes added to the index (staged).
        // We use --cached instead of --staged for maximum compatibility 
        // with all Git versions.
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--cached");

        // Merge stderr into stdout so we can read ALL output from one stream.
        // Without this, if git writes to stderr (like "fatal: not a git repository"),
        // we'd need a separate thread to read stderr — adding complexity.
        processBuilder.redirectErrorStream(true);

        // --- 2. Start the process and capture output ------------------------
        // If git is not installed or not on PATH, start() throws IOException.
        // We catch it here and re-throw as IllegalStateException so it reaches
        // Main's "known errors" handler with a clear, actionable message.
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not execute 'git'. Is Git installed and on your PATH?", e
            );
        }

        // Read all output into a single String using try-with-resources.
        // BufferedReader + InputStreamReader converts the byte stream to chars.
        String output;
        try (var reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        // --- 3. Wait for the process to finish ------------------------------
        // waitFor() blocks until git exits and returns its exit code.
        // Exit code 0 = success, anything else = failure.
        int exitCode = process.waitFor();

        // --- 4. Handle failure cases ----------------------------------------
        if (exitCode != 0) {
            // Git returned an error. Common causes:
            //   - "fatal: not a git repository" (exit code 128)
            //   - Invalid git state or corrupted repo
            throw new IllegalStateException(
                    "Git command failed (exit code " + exitCode + "): " + output.trim()
            );
        }

        // --- 5. Handle "no staged changes" ----------------------------------
        // If git diff --staged succeeds but produces empty output, it means
        // the user hasn't staged anything with `git add`.
        if (output.isBlank()) {
            throw new IllegalStateException(
                    "No staged changes found. Use 'git add <file>' to stage changes first."
            );
        }

        return output;
    }
}
