# OPAC-Create-Big-Canons-Compat

An [Open Parties and Claims](https://modrinth.com/mod/open-parties-and-claims) (OPAC)
addon for [Create: Big Cannons](https://modrinth.com/mod/create-big-cannons) (CBC).

Create: Big Cannons destroys blocks and damages entities through its own projectile
impact code, which bypasses the normal block-break / explosion events that OPAC
listens to. As a result, plain OPAC never sees cannon fire and cannot protect claims
from it. This mod is a **bridge**: it routes CBC's destruction through OPAC's
protection API so that OPAC's existing per-claim options decide what happens.

## How it works

Two master switches live in this mod's config (`opaccbccompat`):

- **Protect Blocks** – route CBC block destruction through OPAC.
- **Protect Entities** – route CBC entity damage through OPAC.

These are only on/off switches. When a switch is on, **all** of the granular control
lives in OPAC itself — the per-claim block/entity protection toggles, the
wilderness / server-claim / expired-claim settings, and admin-defined exception
groups. When a switch is off, cannons behave as they do in vanilla CBC (destroy
everything).

Fired projectiles are tagged with the player who fired them (CBC leaves them
anonymous), so OPAC's party/ally exceptions and owner redirection apply to cannon
fire the same way they do to any other player action.

CBC's damage *sources* are wrapped the same way. OPAC independently re-checks
every entity hurt on the server (its `LivingIncomingDamageEvent` handler) using
the entities carried by the damage source — and CBC's `CannonDamageSource`
carries none, so that final check used to see every cannon hurt as anonymous and
block it in any protected claim, no matter what the claim's options or the
bridge's own checks said. The bridge wraps CBC's damage sources so they carry
the projectile (and its owner, when attributed), which makes OPAC's final check
consult the same exception groups and owner redirection as the rest of the
bridge. Armor-bypass behaviour of the original CBC source is preserved.

## Recommended OPAC server config

To let cannons fire freely *inside your own claim* while still protecting other
players' claims, add CBC's entities to OPAC's exception groups in
`serverconfig/openpartiesandclaims-server.toml`:

```toml
blockAccessEntityGroups = [ "break$CBC{createbigcannons:*}" ]
entityAccessEntityGroups = [ "break$CBC{createbigcannons:*}" ]
```

With the projectile owner attributed, OPAC treats a cannon shot like an action by
its firer, so these groups grant the firer's own claim access while everyone else's
claims stay protected.

These two lists create per-claim options that appear in the claim config UI as:

- **"Mine (CBC)"** – from `blockAccessEntityGroups`: which players' CBC
  projectiles may break blocks in the claim.
- **"Attack By (CBC)"** – from `entityAccessEntityGroups`: which players' CBC
  projectiles may damage entities in the claim.

The lists take **entity** IDs. Take care not to put the entry into
`blockExceptionGroups` instead — that list takes **block** IDs and creates a
**"Break (CBC)"** option, which controls who may break *CBC's own blocks* (cannon
parts) in the claim and has no effect on what cannon fire can destroy.

These group options are checked *in addition to* OPAC's general exception
options. Because projectiles are redirected to their firing player, the general
**"Allow Blocks By Players"** / **"Allow Entities By Players"** options also
apply to cannon fire: if one of those is set to Everyone, cannon damage is
allowed regardless of the CBC group options. The CBC options only *grant extra
access* (e.g. "Mine (CBC): Everyone" lets anyone's cannons break blocks even
while "Allow Blocks By Players" is Nobody); they cannot revoke access granted by
a general option.

Note that adding a group to the server config only *creates* the per-claim
option — its value still defaults to **Nobody**. After a restart, open the
claim config and set "Mine (CBC)" / "Attack By (CBC)" to the players you want
to allow.

### Explosion entity damage

CBC shells hurt entities in two ways: a direct projectile hit and the shell's
explosion (HE, AP and impact explosions). Direct hits go through OPAC's normal
entity-attack check, which consults the exception groups. Explosions, however,
are filtered by OPAC's *own* explosion handler, which only looks at the
general options — "Allow Entities By Explosions" (off by default) and "Allow
Entities By Players" — and never consults entity-access groups, so "Attack By
(CBC)" alone could not permit explosion kills. The bridge therefore re-checks
every entity that OPAC's explosion filter removed through the attributed
attack path (the same one direct hits use) and restores the ones it allows.
The bridge only ever *restores* entities — anything OPAC's own handler
permits (e.g. "Allow Entities By Explosions": on) stays permitted.

Because it re-checks through the *attack* path, a cannon explosion is treated
as a **player attack**: the general **"Allow Entities By Players"** option
governs it too, exactly as it does for block damage. This has a deliberate
consequence: a claim with **"Allow Entities By Explosions": Nobody** but
**"Allow Entities By Players": Everyone** (and no restricting "Attack By (CBC)")
**will** have its entities killed by cannon explosions, because cannon fire
counts as a player attack rather than a generic explosion. To block explosion
kills while still allowing player melee, set **"Attack By (CBC)": Nobody** and
keep "Allow Entities By Players" from granting access. If you want cannon
explosions to strictly follow the "Allow Entities By Explosions" option instead,
that is not currently supported — file an issue if you need it.

## Sub-level (VS2 / Sable) compatibility

CBC's `canDamageTerrain` hook maps the impact position through
`CBCCompatTransformers.transformBlockPos` before checking damage, which is how CBC
supports Valkyrien Skies 2 / Sable sub-levels (real blocks stored at extreme
coordinates in the same level). The block-penetration mixins apply the same
transform before querying OPAC, so claim checks use world-space coordinates. On a
normal world this transform is an identity no-op.

## Testing and troubleshooting

### Players with full chunk access always get through

OPAC grants some players **full access** to a claim, and for them every action is
allowed *before* the claim's protection options or exception groups are even
consulted — exception groups can only grant access to players who lack it, never
revoke it. A player has full access to a claim when:

- they **own** the claim,
- they are in **admin mode** (`/opm-admin`), or
- the claim is a **server claim** and they are in server claiming mode with the
  server claim permission (typically the operator who created the claim).

Because projectiles are attributed to the player who fired them, the same applies
to cannon fire: **if you can break a block by hand inside the claim, your cannon
can break it too.** A quick equivalence check: try mining the block — if OPAC
doesn't stop your pickaxe, it won't stop your shell either.

**Test protection with a non-privileged account** (or leave admin / server
claiming mode first), otherwise settings like "Break CBC: Nobody" will appear
not to work.

### Client-side prediction (ghost holes)

CBC destroys penetrated blocks on **both** the server and the client — the client
predicts the destruction locally to hide latency. When OPAC blocks a penetration
on the server, the client has already removed the block, which used to leave a
client-only "ghost hole" in the wall (walk up to it, relog, or F3+A and the block
reappears — it was never gone on the server). The bridge now resends the real
block state whenever it blocks a hit, so the block may flicker for a moment on
impact but stays intact. This resync covers **all** client-predicted block paths:
big-cannon and autocannon penetration, shrapnel terrain hits, and the cosmetic
cracking/denting CBC explosions apply along their blast.

### Debug logging

Set `debugLogging = true` in this mod's server config
(`serverconfig/opaccbccompat-server.toml`) to log every OPAC verdict at INFO
level:

```
[OPAC-CBC] block damage BLOCKED at 120, 64, -40 in minecraft:overworld | projectile=createbigcannons:shot_projectile[.../uuid] accessor=minecraft:player[Agent772/uuid] claim=... | blocked by the claim's protection options / exception groups | options: blocksRedirect=true allowBlocksByPlayers=N(nobody) "Mine (CBC)"=N(nobody)
```

Each line shows the position, the projectile, the resolved accessor (the firing
player), the claim at the position, *why* the action was allowed or blocked, and
the claim config values that decide the verdict: the general "Allow Blocks/
Entities By Players" option and every entity-access exception group option
(listed by its UI label, e.g. `"Mine (CBC)"`). If the log says no entity-access
exception groups are defined, the OPAC server config is missing the
`blockAccessEntityGroups` / `entityAccessEntityGroups` entries above.

Entity verdicts are labelled with the damage path — `entity damage via direct
hit`, `via sub-projectile hit` or `via explosion` — so you can tell which code
path produced them. When OPAC's own explosion filter removes entities from a
CBC blast, an extra summary line reports how many the attributed re-check
restored.

Disable it again on production servers — a single shot can query many blocks.
Debug lines also write **player names and UUIDs** to the server log; this is the
same personal data most server logs already contain, but keep it in mind for
GDPR-conscious deployments.

## Known limitations

- **Flak, fluid, mortar-stone, shrapnel and smoke explosions** are created by CBC
  without a source entity, so their block/entity damage uses an anonymous
  accessor: they are blocked inside protected claims regardless of who fired
  them, and ally / owner / group exceptions do not apply. HE, AP and impact
  explosions *do* carry the projectile as their source and are fully attributed
  to the firing player.
- **Fluid shell bursts** apply their effect through CBC's fluid-effect registry
  rather than the standard block/entity path and are not currently gated.
- **Drop-mortar rounds** are fired without a rider context and are attributed
  anonymously, so they are blocked inside protected claims regardless of who
  dropped them.
