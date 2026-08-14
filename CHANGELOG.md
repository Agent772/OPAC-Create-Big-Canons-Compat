## Version 1.0.0

### Added
- Initial release of the OPAC × Create: Big Cannons compatibility mod. CBC projectile damage — including big-cannon and autocannon block penetration, shrapnel terrain damage, direct entity hits, and burst sub-projectiles — is now routed through OPAC's claim protection API, respecting per-claim options, exception groups, and player attribution (owner, party, and ally exceptions all apply).
- Debug verdict logging available via JVM argument `-Dopaccbccompat.debugLogging=true`. When enabled, every block and entity protection decision is logged at INFO level in `latest.log`, showing the projectile, resolved owner, claim position, and reason for each verdict.

### Fixed
- Fixed ghost holes appearing in protected walls when a cannon shot is blocked server-side — the real block state is resynced to clients so protected walls remain visually intact.
- Fixed cannon explosion entity damage ignoring entity-access exception groups — entities inside claims can now be granted damage permission via exception groups, matching the behaviour of direct projectile hits.
- Fixed block penetration checks using pre-transform coordinates on VS2/Sable sub-levels — claim checks now use the correct world-space position so protection works on moving contraptions.
