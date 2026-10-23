# SuperTeams

Minecraft team/guild management for Paper and Purpur, with **independent rights per member** rather than fixed officer/member roles.

> **Status: active development, not yet production-certified.** The GitHub Actions Maven build is used as a compile check. A successful compile is not a substitute for in-game integration testing.

## Requirements
- Java 21
- Maven 3.9+
- Paper/Purpur 1.21.11 (API target)
- Vault plus a compatible economy provider for team-bank commands

## Build
```sh
mvn --batch-mode clean verify
```
The Maven Build workflow runs on pushes and pull requests. The plugin jar is published as a workflow artifact when the build succeeds.

## Commands
| Command | Purpose |
|---|---|
| `/team` | Open the team menu |
| `/team create <name>` | Create a team |
| `/team info [name]` | Show team summary |
| `/team invite <player>` | Invite an online player |
| `/team accept`, `/team decline` | Accept or decline a pending invitation |
| `/team kick <member>` | Remove a non-owner member |
| `/team leave` | Leave a team (owners must transfer or disband) |
| `/team transfer <member>` | Transfer ownership |
| `/team disband confirm` | Permanently disband your team |
| `/team rights <member> <RIGHT> <on|off>` | Edit an individual member's rights |
| `/team sethome`, `/team home`, `/team cancel` | Set/use/cancel a team-home teleport |
| `/team bank balance` | View shared bank balance |
| `/team bank deposit <amount>` | Deposit economy funds |
| `/team bank withdraw <amount>` | Withdraw from the shared bank |
| `/team enderchest` | Open persistent shared 27-slot team storage |
| `/team pvp` | Toggle friendly fire |
| `/team chat <message>` | Send team chat |

Available individual rights are defined by `TeamRight`: `INVITE`, `KICK`, `MANAGE_MEMBERS`, `HOME_USE`, `HOME_SET`, `HOME_BYPASS`, `BANK_VIEW`, `BANK_DEPOSIT`, `BANK_WITHDRAW`, `CHAT`, `CHAT_COLOR`, `PVP`, `TEAM_SETTINGS`, and `ENDERCHEST_USE`. The owner always retains full control. Only the owner can delegate `MANAGE_MEMBERS`.

## Configuration and data
- `plugins/SuperTeams/config.yml` controls team name rules, member limits, invitation expiry, home warm-up/cooldown, and chat spy.
- `storage.type` in `config.yml` selects `YAML`, `SQLITE`, or `MYSQL`. SQLite creates the configured local database file; MySQL uses the host, port, database, username, and password you supply. The plugin creates its table automatically.\n- With YAML selected, `plugins/SuperTeams/teams.yml` stores team, member, rights, home, friendly-fire, bank balance, and shared team ender chest contents. Switching an existing YAML installation to a database imports `teams.yml` once if the database has no saved state. Back up your data before changing backends.
- `plugins/SuperTeams/invitations.yml` stores expiring invitations.\n- `plugins/SuperTeams/bank-transactions.yml` keeps a durable transaction audit journal. Incomplete entries are flagged for manual reconciliation at startup.
- Back up the whole plugin data directory before upgrades or migrations.

## Important limitations before production
- The bank uses Vault through an optional runtime bridge and journals each operation. Vault providers do not offer a universal idempotent transaction API, so a crash between an economy-provider action and journal update can require **manual reconciliation**. The journal intentionally does not replay unfinished transactions automatically. Test against your exact economy provider and do not treat this as a guaranteed atomic financial ledger.
- The SQLite/MySQL backends store the serialized team state in a database row. This is a simple single-server persistence backend, not a multi-server concurrent-write or migration framework. Back up data before switching storage types.
- The GUI includes a clickable member list and individual rights editor, plus shortcuts for home, friendly fire, bank balance, and the shared team ender chest. Deposits and withdrawals remain command-based.
- Home warm-up supports movement and damage cancellation, cooldowns, and manual cancellation. A full teleport edge-case test suite and live server testing remain outstanding.
- PlaceholderAPI expansion, complete settings GUI, comprehensive automated tests, and live Paper/Purpur integration tests remain outstanding. Database backends still need live testing against the user-provided MySQL server and target hosting environment.
- Folia compatibility is not claimed.

Please test on a staging server, verify Vault/economy compatibility, and keep backups. Do not treat this build as production-ready until the remaining financial, persistence, and integration tests are completed.
