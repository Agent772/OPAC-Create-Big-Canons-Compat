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
blockAccessEntityGroups = [ "CBC{createbigcannons:*}" ]
entityAccessEntityGroups = [ "CBC{createbigcannons:*}" ]
```

With the projectile owner attributed, OPAC treats a cannon shot like an action by
its firer, so these groups grant the firer's own claim access while everyone else's
claims stay protected.

These two lists create per-claim options that appear in the claim config UI as:

- **"Blocks (CBC)"** – from `blockAccessEntityGroups`: which players' CBC
  projectiles may break blocks in the claim.
- **"Entities By (CBC)"** – from `entityAccessEntityGroups`: which players' CBC
  projectiles may damage entities in the claim.

The lists take **entity** IDs. Take care not to put the entry into
`blockExceptionGroups` instead — that list takes **block** IDs and creates a
**"Blocks (CBC)"** option, which controls who may break *CBC's own blocks* (cannon
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
claim config and set "Block (CBC)" / "Entity By (CBC)" to the players you want
to allow.

By default the new options are grayed out in the claim config UI, meaning
players cannot change them. To let claim owners toggle them, add the option
IDs to the `playerConfigurablePlayerConfigOptions` list in the same file:

```toml
playerConfigurablePlayerConfigOptions = [
    # ... existing entries ...
    "claims.protection.exceptions.groups.entity.blockAccess.CBC", 
    "claims.protection.exceptions.groups.entity.entityAccess.CBC"
]
```


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
as a **player attack** and follows the same options as a melee hit: the general
**"Allow Entities By Players"** option and **"Attack By (CBC)"** govern it,
**not** "Allow Entities By Explosions". Since these options can only *grant*
access, this has a deliberate and unavoidable consequence: a claim with
**"Allow Entities By Players": Everyone** **will** have its entities killed by
cannon explosions even if **"Allow Entities By Explosions": Nobody** and
**"Attack By (CBC)": Nobody** — the general "Everyone" grants access that the
other two cannot revoke. In other words, you cannot allow player melee on
entities while blocking cannon-explosion damage through these options; both are
the same attack path. To block cannon-explosion entity damage, "Allow Entities
By Players" (and "Attack By (CBC)") must not grant access. If you need cannon
explosions to instead follow the "Allow Entities By Explosions" option, that is
not currently supported — file an issue.

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
