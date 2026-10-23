# SuperTeams

A configurable Paper/Purpur team and guild plugin focused on **independent per-member rights** rather than fixed officer/member roles.

> **Development status:** Work in progress. Do not treat this repository as production-ready until the project builds and its gameplay, persistence, and economy tests have passed.

## Target

- Minecraft Java Edition 1.21.11
- Paper / Purpur
- Maven build
- Java version required by the target server
- YAML persistence by default
- Optional Vault economy and PlaceholderAPI integrations when implemented

## Core design

Team permissions are individual rights. For example, a member can receive `INVITE` and `HOME_USE` without receiving `HOME_SET` or `BANK_WITHDRAW`. Rights are checked on the server for every protected action; GUI visibility is not authorization.

Planned features include team creation, invitations, member management, rights GUIs, team homes, private team chat, friendly fire, a shared Vault bank with recoverable transaction records, YAML/SQLite/MySQL storage, and optional placeholders.

## Build

Install a compatible JDK and Maven, then run:

```sh
mvn clean package
```

The plugin JAR should be produced under `target/` when the build succeeds.

## Installation

1. Build the project with Maven.
2. Copy the resulting SuperTeams JAR into the server's `plugins/` directory.
3. Start the server once to generate configuration files.
4. Stop the server before editing configuration or moving persistent data.
5. Restart and review the server log for configuration or integration errors.

## Safety and compatibility

- No Folia compatibility is claimed unless region-aware scheduling is implemented and tested on Folia.
- Bank transactions must not be blindly retried after ambiguous economy-provider failures.
- Back up team data before changing storage backends or testing migrations.
- Build/test status must reflect actual executed results; generated code alone is not proof of production readiness.

## License

See [LICENSE](LICENSE).
