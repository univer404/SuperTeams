# SuperTeams

Team and guild management for Paper/Purpur with independent rights per member.

**Status: early development scaffold; not production-ready.** Some commands and features intentionally report as unavailable rather than pretending to work.

## Requirements
- JDK 21
- Maven
- Paper/Purpur API for 1.21.11

## Build
`mvn clean package`

## Current implementation
- Maven plugin metadata and configurable settings
- UUID-backed team/member model and individual `TeamRight` values
- YAML persistence with temporary-file replacement and basic load repair
- Team create, info, leave, kick, chat, and friendly-fire toggle commands
- Friendly-fire cancellation for direct player attacks and player-shot projectiles
- Basic tab completion

## Not implemented yet
- Persistent, expiring invitation acceptance flow
- Inventory GUIs and rights editor
- Team homes and teleport warm-ups
- Vault shared bank and financial transaction journal/recovery
- SQLite/MySQL backends and schema migrations
- PlaceholderAPI expansion
- Ownership transfer and confirmed disband flow
- Automated test suite and verified build

## Safety notes
Bank operations are not exposed until transaction handling can be implemented safely. Folia compatibility is not claimed. Test on a staging server and back up data before use.
