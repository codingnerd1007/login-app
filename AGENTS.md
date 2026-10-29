# AGENTS.md

Before making any change, read **[docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md)**. It has the full context:
purpose, architecture, login flow, API, encryption design, keystore, branches, known gaps and working rules.

Key rules (details in §15 of that document):
- This repo recreates an office login module; changes must stay portable to the office code.
- Do not change the request DTOs, payloads or the captcha → credentials → OTP → JWT flow unless the owner asks.
- Branch from `main`; open or merge PRs only when asked. Run `mvn test` (backend) and `npx ng build` (frontend).
- The repo is public: never commit keystores, passwords, real office secrets or personal data.
