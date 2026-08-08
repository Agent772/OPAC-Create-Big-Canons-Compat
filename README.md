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
block state whenever it blocks a penetration, so the block may flicker for a
moment on impact but stays intact.

### Debug logging

Set `debugLogging = true` in this mod's server config
(`serverconfig/opaccbccompat-server.toml`) to log every OPAC verdict at INFO
level:

```
[OPAC-CBC] block damage ALLOWED at 120, 64, -40 in minecraft:overworld | projectile=createbigcannons:shot_projectile[.../uuid] accessor=minecraft:player[Agent772/uuid] claim=... | accessor has FULL chunk access to this claim (claim owner, admin mode or server claiming mode) - OPAC allows before claim options or exception groups are checked
```

Each line shows the position, the projectile, the resolved accessor (the firing
player), the claim at the position and *why* the action was allowed or blocked.
Disable it again on production servers — a single shot can query many blocks.

## Known limitations

- **Explosion block damage** (HE/impact/mortar shells) uses an anonymous accessor,
  because CBC creates those explosions without a source entity. Explosions are
  therefore protected in any claim whose OPAC options protect blocks, but ally /
  owner exceptions do not apply to explosion *block* destruction.
- **Fluid shell bursts** apply their effect through CBC's fluid-effect registry
  rather than the standard block/entity path and are not currently gated.
- **Drop-mortar rounds** are fired without a rider context and are attributed
  anonymously, so they are blocked inside protected claims regardless of who
  dropped them.
