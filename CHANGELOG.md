## Version 0.2.0
### Removed
- The mod's own server config (`opaccbccompat-server.toml`) and its registration. The `protectBlocks`, `protectEntities` and `debugLogging` options are gone: all protection control lives in OPAC's own config and per-claim options. Any leftover `serverconfig/opaccbccompat-server.toml` in an existing world is ignored and can be deleted. #5

### Changed
- **BREAKING:** installing the mod now always activates the bridge. Servers that ran with `protectBlocks` or `protectEntities` set to `false` had cannon fire bypass OPAC entirely; those shots now go through the bridge and are subject to claim protection. #5
- Debug verdict logging moved from the removed `debugLogging` toml flag to the JVM argument `-Dopaccbccompat.debugLogging=true`. It is off by default and the `[OPAC-CBC]` lines log at `INFO` in `latest.log` when enabled. #5

## Version 0.1.0
### Added
- Initial project scaffolding: NeoForge 1.21.1 build setup, main mod class, server config (`protectBlocks`, `protectEntities`), mixin config, and mod metadata. #2
