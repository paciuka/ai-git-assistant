# 🤖 AI Git Assistant

> A Java CLI tool that uses **Claude AI / Google Gemini** to generate professional, semantic commit messages from your staged Git changes — following the [Conventional Commits](https://www.conventionalcommits.org/) specification.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.9+-blue?style=flat-square&logo=apachemaven)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)
![Claude & Gemini](https://img.shields.io/badge/Powered%20by-Claude%20%7C%20Gemini-blueviolet?style=flat-square)

---

## 📋 What It Does

1. Captures your staged changes via `git diff --cached`
2. Sends the diff to your chosen AI provider (**Claude Sonnet ** or **Google Gemini**)
3. The AI analyzes the code and generates a **Conventional Commit** message
4. The message is displayed in your terminal, ready to copy

```
  ╔══════════════════════════════════════╗
  ║       🤖 AI Git Assistant            ║
  ║   Smart Commits, Powered by AI       ║
  ╚══════════════════════════════════════╝

  ⏳ Loading configuration...
  ✔ API key loaded successfully.
  ⏳ Reading staged changes (git diff --cached)...
  ✔ Captured 42 lines of staged changes.
  ⏳ Sending diff to Claude (claude-sonnet-4-20250514)...
  ✔ Response received from Claude.

  ─────────────────────────────────────
  📝 Suggested Commit Message:
  ─────────────────────────────────────

    feat(auth): add JWT token validation middleware

    Implement token verification for protected API routes using
    the jsonwebtoken library. Tokens are validated against the
    secret stored in environment variables.

  ─────────────────────────────────────
  Copy the message above and use it with:
  git commit -m "<message>"
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Git** — [Download](https://git-scm.com/)
- **API Key** — You need ONE of the following:
  - **Google Gemini** (Free tier) — [Get one here](https://aistudio.google.com/apikey)
  - **Anthropic Claude** — [Get one here](https://console.anthropic.com/)

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/paciuka/ai-git-assistant.git
cd ai-git-assistant

# 2. Build the project (uses Maven Wrapper — no global Maven install needed)
./mvnw clean package      # Linux/macOS
.\mvnw.cmd clean package  # Windows

# 3. Set your API key (Choose One)
# For Gemini (Free):
export GEMINI_API_KEY="your-gemini-key"
# OR for Claude:
export ANTHROPIC_API_KEY="your-anthropic-key"

# 4. Stage some changes and run
git add .
java -jar target/ai-git-assistant-1.0.0.jar

# 5. See all options
java -jar target/ai-git-assistant-1.0.0.jar --help
```

### Optional: Custom Model

```bash
# Override the default model
export AI_MODEL="gemini-1.5-pro"
```

---

## 🐳 Docker Usage

Build and run with Docker — no Java installation required on the host machine.

```bash
# Build the image
docker build -t ai-git-assistant .

# Run from any git repository
# Mount the repo and pass the API key as an env variable
docker run --rm \
  -v "$(pwd):/repo" \
  -e GEMINI_API_KEY=your-api-key-here \
  ai-git-assistant
```

**Windows (PowerShell):**
```powershell
docker run --rm `
  -v "${PWD}:/repo" `
  -e GEMINI_API_KEY="your-api-key-here" `
  ai-git-assistant
```

---

## 🏗️ Architecture

The project follows **SOLID principles** with clear separation of concerns:

```
com.aigitassistant/
├── Main.java                 # Composition root — wires all components
├── config/
│   └── AppConfig.java        # Loads API key and auto-detects AI provider
├── git/
│   └── GitDiffProvider.java  # Executes `git diff --cached` via ProcessBuilder
├── ai/
│   ├── AiClient.java         # Interface for Strategy Pattern (OCP)
│   ├── AnthropicClient.java  # Claude implementation
│   ├── GeminiClient.java     # Google Gemini implementation
│   └── PromptBuilder.java    # Crafts the system/user prompts
└── model/
    ├── CommitMessage.java    # Immutable record (Java 17 feature)
    └── ApiResponseParser.java# JSON parser helper
```

### Data Flow

```
Environment Variables → AppConfig → (Detects Provider: Gemini/Claude)
                                         │
git diff --cached → GitDiffProvider → PromptBuilder → AiClient (Strategy)
                                                          │
                               Terminal ← CommitMessage ← ┘
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **java.net.http.HttpClient** | Built into JDK 11+. Zero external dependencies for HTTP. |
| **Strategy Pattern (AiClient)** | Open/Closed principle. Easily add new AI backends without modifying `Main`. |
| **Gson** (single external dep) | Lightweight, no transitive dependencies. |
| **ProcessBuilder** over Runtime.exec() | Better API, supports stream redirection, avoids deadlocks. |
| **Java record** for CommitMessage | Immutable by design, auto-generates boilerplate. |
| **Multi-stage Docker build** | Separates build (Maven+JDK) from runtime (JRE-only). Final image is ~150MB vs ~400MB. |
| **Environment variables** for config | 12-Factor App methodology. Keys never touch the codebase. |

---

## 🧠 Prompt Engineering

The system prompt in [`PromptBuilder.java`](src/main/java/com/aigitassistant/ai/PromptBuilder.java) uses several prompt engineering techniques:

| Technique | How It's Used |
|---|---|
| **Role Assignment** | "You are a senior software engineer who is an expert at writing Git commit messages" |
| **Format Specification** | Exact `<type>(<scope>): <description>` template |
| **Allowed Values** | Explicit list of valid types (feat, fix, docs, refactor, etc.) |
| **Hard Constraints** | "Subject line MUST be lowercase", "No longer than 72 characters" |
| **Negative Constraints** | "Do NOT end with a period", "No markdown fences" |
| **Imperative Mood Rule** | "Use 'add feature' not 'added feature'" |

---

## 🤝 AI Collaboration Log

> **This section documents how AI was used as a thinking and collaboration tool throughout the development of this project.** This is not just AI-generated code — it's a demonstration of **AI-augmented development**, where the developer drives the architecture and the AI accelerates execution.

### Phase 1: Brainstorming & Architecture

I started by describing my high-level idea — a CLI tool that reads git diffs and generates commit messages — to an AI assistant. Instead of jumping into code, we had an **architectural discussion**:

- **I proposed** the tech stack (Java 17, Maven, no Spring).
- **The AI challenged decisions**: Why not Spring Boot? (Answer: too heavy for a CLI tool — we want a single fat JAR with minimal dependencies.)
- **Together we designed** the package structure following SOLID principles, identifying five distinct responsibilities that became five classes.

**What I learned:** Using AI for architecture forces you to articulate *why* you make decisions, not just *what* to build.

### Phase 2: Iterative Development

We built the project in deliberate steps, not all at once:

1. **`GitDiffProvider`** — I learned about `ProcessBuilder` vs `Runtime.exec()`, why you must read stdout before calling `waitFor()` (to avoid deadlocks), and how `redirectErrorStream(true)` simplifies error handling.

2. **`PromptBuilder`** — The AI and I iterated on the system prompt multiple times. The first version produced verbose, inconsistent messages. We refined it by adding negative constraints ("Do NOT...") and explicit format rules until the output was consistently professional.

3. **`AnthropicClient`** — We discussed why building JSON with Gson's JsonObject API is safer than string concatenation (special characters in diffs would break raw strings).

4. **`Main` class** — I asked the AI how to make CLI output more professional. It suggested ANSI escape codes, which I hadn't worked with before. We designed the status indicators (⏳, ✔, ✖) together.

### Phase 3: Debugging & Problem-Solving

AI was invaluable for debugging:

- **Process deadlock issue**: My initial `GitDiffProvider` called `waitFor()` before reading the output stream. The AI explained that if the output buffer fills up, the subprocess blocks waiting for the buffer to be read, while Java blocks on `waitFor()` — a classic deadlock. We fixed the ordering.
- **JSON escaping bug**: Early tests with diffs containing special characters (quotes, backslashes) broke the JSON payload. The AI suggested using Gson's structured API instead of string templates.

### Phase 4: Dockerization

The AI explained multi-stage Docker builds step by step:
- **Why stage 1 copies `pom.xml` first**: Docker layer caching — if only source code changes, Docker reuses the cached dependency layer.
- **Why stage 2 uses `jre-alpine`**: Minimal image, smaller attack surface.
- **Why we need `apk add git`**: The runtime container needs git to execute `git diff`.

### How AI Changed My Workflow

| Without AI | With AI |
|---|---|
| Google "Java ProcessBuilder tutorial" → skim 5 articles | Describe my use case → get a targeted explanation with caveats |
| Copy-paste from StackOverflow → debug for hours | Understand the pattern first → write it correctly |
| Write code → discover edge cases later | AI surfaces edge cases upfront (deadlocks, encoding, etc.) |
| Read Anthropic API docs end-to-end | Ask "what headers does the Messages API need?" → get the 3 essential ones |

**Key insight:** AI didn't replace my thinking — it amplified it. I still made every architectural decision, but I made them faster and with more context.

### Phase 5: AI-Powered Code Review

Before submitting the project, I used AI to perform a rigorous code review simulating a Senior Tech Lead. This uncovered issues I would have missed:

- **Memory safety (OOM prevention):** The review identified that staging a massive binary file could cause `OutOfMemoryError` and waste API tokens. We added a `MAX_DIFF_LINES` guard (2000 lines) that rejects oversized diffs before they reach the API — a defensive programming pattern that shows cost-awareness with paid APIs.
- **Security hardening:** API error responses can sometimes echo back request headers or authentication info. The review caught that we were dumping the full `response.body()` into exception messages. We now truncate error output to 500 characters to prevent sensitive data leakage in logs.
- **Robustness improvements:** Added content type validation in the response parser (protecting against `NullPointerException` if the API returns unexpected block types), and wrapped the `IOException` from `ProcessBuilder.start()` into a user-friendly error message when Git isn't installed.
- **Prompt engineering fix:** The Javadoc claimed we used "few-shot examples" but the system prompt had none. We added concrete examples, which measurably improved output consistency.

### Phase 6: Architecture Evolution (Open/Closed Principle)

I realized users might not have a paid Anthropic account, so I wanted to add support for **Google Gemini** (which has a free tier). Because we laid a good foundation, this was extremely clean:
- Created an `AiClient` interface (**Strategy Pattern**).
- Made `AnthropicClient` implement it.
- Created `GeminiClient` implementing the same interface.
- Updated `AppConfig` to auto-detect the provider based on which environment variable is set (`GEMINI_API_KEY` vs `ANTHROPIC_API_KEY`).
- `Main.java` didn't need its core pipeline modified at all — it just calls `AiClient.sendMessage()`.

**What I learned:** This perfectly demonstrated the **Open/Closed Principle** (SOLID). The application was open for extension (adding Gemini) but closed for modification (the core pipeline remained identical).

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

<p align="center">
  Built with ☕ Java and 🤖 Claude AI
</p>
