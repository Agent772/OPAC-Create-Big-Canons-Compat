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
