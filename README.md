# Wave Motorcycle

A fully rideable, physics-based Honda Wave-style underbone motorcycle for
**Paper 26.2** (Java plugins, no client mods). The bike is a single logical
vehicle: one hidden controller entity plus a set of linked display entities
that render a low-poly 3D model delivered through the modern Paper
resource-pack API.

Survival-mode friendly: fuel, damage, persistence, multiplayer-safe updates.

---

## Features

- **One vehicle, one controller** — an invisible, invulnerable armor stand
  carries the rider; every display part is bound to the bike's UUID via the
  persistent data container.
- **Genuinely rideable** — mount, throttle, brake, slow reverse, mouse-steer,
  dismount, wheelies (NORMAL → LIFTING → WHEELIE → LOWERING with a ~28° max
  angle), headlight toggle.
- **3D model from a resource pack** — body, front fork, handlebar, headlight,
  rear light and both wheels are separate, independently animated parts
  (wheel spin, steering, wheelie pitch) using display transformations only —
  entities are never recreated.
- **Custom AABB collision** — no wall/terrain/void clipping; per-material
  traction (grass 0.85, stone 1.0, ice 0.35, soul sand 0.60, …), slabs and
  stairs resolved.
- **Airborne physics** — gravity while in the air, wheels keep spinning,
  landing impact with optional damage and speed reduction.
- **Damage & destruction** — collision, fall, fire and explosion damage;
  smoke tiers as health drops; destroyed bikes drop their key.
- **Optional fuel system** — 100-unit tank, configurable consumption, refuel
  with a fuel item or admin command.
- **Emissive lighting** — headlight off/on model variants plus a subtle
  particle beam; rear light brightens while braking (no fake dynamic lights).
- **Persistence** — bikes survive restarts (JSON at
  `plugins/WaveMotorcycle/data/motorcycles.json`).
- **Multiplayer-safe** — independent state per bike, squared distances,
  central update loop, dormant when far from any player, configurable update
  frequency.

---

## Requirements

- **Paper 26.2** (or a build compatible with the 26.2 API) — Paper only; no
  Fabric/Forge/NeoForge, no mods.
- **Java 25** server runtime.
- Survival or Adventure mode (creative works too; the controls are
  state-based and do not depend on damage or fall).

---

## Installation

1. Build the plugin (or grab a release jar):

   ```bash
   ./gradlew build
   ```

   Outputs:
   - `build/libs/WaveMotorcycle-1.0.1.jar` — the plugin
   - `build/resourcepacks/wave-motorcycle-pack-1.0.1.zip` — the resource pack

2. Drop `WaveMotorcycle-1.0.1.jar` into your server's `plugins/` folder.
3. Start the server once (it creates `plugins/WaveMotorcycle/config.yml` and
   `messages.yml`), stop it, edit the config, start again.
4. Set up the resource pack (next section).
5. Give yourself a key and ride:

   ```
   /wave give
   /wave spawn      # or right-click the air holding the key
   ```

---

## Resource pack setup

The bike's 3D model lives in a **server-sent resource pack** — this is the
only way to show custom geometry without a client mod.

1. **Build the ZIP.** The repository ships a complete generated pack
   (procedurally generated low-poly Wave model + textures):

   ```bash
   ./gradlew buildResourcePack
   ```

   → `build/resourcepacks/wave-motorcycle-pack-1.0.1.zip`
   (`pack_format` 88 = Minecraft 26.2; change `pack_format` in `pack.mcmeta`
   if you target a different version).

2. **Host the ZIP somewhere public** — any HTTPS file host works: a website,
   GitHub release, object storage (S3), or a simple HTTP server on your LAN.
   The server does not download the file; only players do, and only from
   your URL.

3. **Compute the SHA-1** of the exact ZIP file you uploaded:

   ```bash
   sha1sum wave-motorcycle-pack-1.0.1.zip
   # Windows (PowerShell):
   Get-FileHash wave-motorcycle-pack-1.0.1.zip -Algorithm SHA1
   ```

4. **Configure the plugin** in `config.yml`:

   ```yaml
   resource-pack:
     enabled: true
     url: "https://your-host.example/wave-motorcycle-pack-1.0.1.zip"
     sha1: "<40-character hash from step 3>"
     required: false
   ```

   - `url` empty → pack sending stays disabled and the plugin still works
     (bikes render with plain fallback geometry — you see the wheels/handlebar
     outlines but not the full model).
   - `required: true` → players must accept the pack to join.
   - When you replace the ZIP, **update the SHA-1** — mismatched hashes make
     the client reject the pack.

5. **Send it.** The pack is sent automatically when players join while
   riding/near a bike, or on demand:

   ```
   /wave pack            # to yourself
   /wave pack <player>   # to one player
   /wave pack            # (console) to all online players
   /wave reloadpack      # re-read config + re-send to everyone
   ```

   Joining players are asked by the standard Minecraft resource-pack dialog.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/wave give [player]` | Give a motorcycle key item | `wavemotorcycle.give` |
| `/wave spawn` | Spawn a motorcycle in front of you (consumes a key if you hold one) | `wavemotorcycle.spawn` |
| `/wave remove` | Remove the nearest motorcycle (drops its key) | `wavemotorcycle.admin` |
| `/wave removeall` | Remove every motorcycle | `wavemotorcycle.admin` |
| `/wave fuel [player]` | Show fuel of your (or a rider's) bike | `wavemotorcycle.use` |
| `/wave refuel [player]` | Fill the tank | `wavemotorcycle.admin` |
| `/wave pack [player]` | Send the resource pack to self/player/all | `wavemotorcycle.admin` |
| `/wave reloadpack` | Reload config and re-send the pack to all players | `wavemotorcycle.admin` |
| `/wave light` | Toggle your headlight | `wavemotorcycle.use` |
| `/wave debug` | Bike diagnostics (speed, state, model parts) | `wavemotorcycle.admin` |
| `/wave reload` | Reload `config.yml` and `messages.yml` | `wavemotorcycle.reload` |
| `/wave help` | Show the help list | — |

---

## Permissions

| Permission | Grants |
|---|---|
| `wavemotorcycle.use` | Mount bikes, `/wave fuel`, `/wave light` (default: `true`) |
| `wavemotorcycle.give` | `/wave give` |
| `wavemotorcycle.spawn` | `/wave spawn` |
| `wavemotorcycle.reload` | `/wave reload` |
| `wavemotorcycle.admin` | `/wave remove`, `removeall`, `refuel`, `pack`, `reloadpack`, `debug` |

`wavemotorcycle.*` covers everything.

---

## Controls

Keyboard detection without a client mod is limited to what the vanilla client
reports: **sprint state** (holding SPACE), **sneak state** (holding SHIFT) and
**mouse view**. The plugin maps those onto motorcycle controls:

| Action | How |
|---|---|
| **Mount** | Right-click (air) within ~2.6 blocks of a bike |
| **Throttle** | Hold **SPACE** (sprint) — engine must be running and fuel > 0 |
| **Steer** | Turn your **mouse view** — the bike follows your view yaw |
| **Brake / reverse** | Hold **SHIFT** (sneak): brakes first, reverses slowly once stopped |
| **Dismount** | Quick **SHIFT tap** while stopped, or vanilla dismount; above ~0.35 b/t you cannot dismount (message + cancel) |
| **Wheelie** | **Double-tap SPACE** (within ~0.35 s) while moving forward; hold the throttle to keep it up, ease off and the front lowers. Collapses when speed drops. |
| **Headlight** | Right-click the **air** while riding (holding a fuel item refuels instead) — or `/wave light` |
| **Refuel** | Right-click the air holding the fuel item (default: glass bottle) |
| **Spawn with key** | Right-click the air holding a motorcycle key |

Speed-based steering: the effective steering angle shrinks with speed
(±28° at low speed, reduced at top speed) for stability.

---

## Configuration

Everything lives in `plugins/WaveMotorcycle/config.yml` (values below are the
defaults). Run `/wave reload` after editing.

### Physics (blocks/tick, 20 ticks = 1 s)

| Key | Default | Meaning |
|---|---|---|
| `motorcycle.max-speed` | `0.75` | Top speed (15 m/s) |
| `motorcycle.acceleration` | `0.025` | Throttle acceleration |
| `motorcycle.braking` | `0.08` | Brake deceleration |
| `motorcycle.friction` | `0.015` | Rolling friction |
| `motorcycle.reverse-speed` | `0.18` | Maximum reverse speed |
| `motorcycle.turn-rate` | `0.35` | Turn scale (lower = wider turns) |
| `motorcycle.gravity` | `0.08` | Airborne gravity |
| `motorcycle.steering-angle` | `28.0` | Max steering angle at low speed (degrees) |
| `motorcycle.traction` | `1.0` | Global traction multiplier |

### Wheelies

`wheelie.enabled`, `lift-force` (0.10 °/tick), `max-angle` (28 — keep 25–35),
`balance` (wobble amplitude), `duration` (0 = hold as long as throttled),
`min-speed` (0.20 b/t required to pop one).

### Lights, fuel, damage, effects

- `lights.*` — headlight / rear light / brake light toggles, particle beam
  (`beam-particle`, `beam-interval`).
- `fuel.*` — `enabled`, `capacity` (100), `consumption-per-second` (0.12),
  `fuel-item` (`glass_bottle`), `fuel-item-amount` (25 per item).
- `damage.*` — master + collision/fall/fire toggles.
- `effects.*` — exhaust particle and wheel dust.

### Interaction & performance

- `rider.seat-y-compensation` — rider seat height (tune if the rider floats or
  sinks: floating → increase, sinking → decrease).
- `interaction.mount-range` (2.6), `interaction.dismount-max-speed` (0.35).
- `tick.update-interval` — physics every N ticks (1 = smoothest, 2 = cheaper).
- `tick.activation-radius` — unriden bikes freeze beyond this radius (96).
- `spawn.consume-key` — consume a held key on spawn.

### Sounds

All `sounds.*` keys are sound resource locations — vanilla sounds by default;
custom sounds from your resource pack work too (e.g.
`wavemotorcycle:engine_idle` with a matching `sounds.json` in the pack).

### Surface traction

The `surfaces:` section maps material names to traction multipliers (unknown
surfaces use 1.0; slabs use their base material):

```yaml
surfaces:
  grass_block: 0.85
  stone: 1.00
  ice: 0.35
  soul_sand: 0.60
  # ... add or adjust any material
```

---

## Troubleshooting

- **The bike looks like plain boxes / no model.** The player has not accepted
  (or failed to download) the pack. Check that `url` is reachable publicly and
  `sha1` matches the exact ZIP. Use `/wave pack <player>` and watch for the
  pack dialog. Console logs the pack status per player.
- **Players see a "broken pack" dialog / pack rejected.** SHA-1 mismatch or a
  `pack_format` that doesn't match the client version (88 for 26.2).
- **Rider floats above / sinks into the seat.** Adjust
  `rider.seat-y-compensation`.
- **Bike feels floaty/jittery.** Keep `tick.update-interval: 1`; make sure TPS
  is healthy (the physics loop is allocation-light and uses squared
  distances).
- **Cannot dismount at speed.** Intentional — above
  `interaction.dismount-max-speed` dismounts are cancelled (slow down first).
- **Engine won't start / dies while riding.** Fuel exhausted — `/wave fuel`,
  then refuel with the fuel item or `/wave refuel`.
- **Bike vanished after a world reload.** Its state is in
  `plugins/WaveMotorcycle/data/motorcycles.json`; bikes whose world was not
  loaded yet are re-created when the world loads.
- **Sound too quiet/loud.** All sounds are configurable; engine pitch follows
  speed and pulses every `sounds.engine-pulse-interval` ticks.
- **Two bikes at the same spot.** Each spawn creates a new UUID; use
  `/wave remove` / `removeall`. Duplication is prevented — keys bind to a
  single bike and spawning with a bound key re-spawns the *same* bike.

---

## Development

```bash
# Requirements: JDK 25, no other tooling (the Gradle wrapper is included)
git clone <repo-url> && cd vehicle-plugin
./gradlew build             # compiles against Paper 26.2 (paper-api 26.2.build.+)
./gradlew buildResourcePack # re-outputs the resource pack ZIP
```

- **Build:** Gradle (Kotlin DSL), Java toolchain 25,
  `compileOnly("io.papermc.paper:paper-api:26.2.build.+")`.
- **Run a test server:** point Paper 26.2 at the built jar; or use
  [Paper's dev bundle](https://docs.papermc.io/paper/dev/bundles/) with the
  jar dropped in `plugins/`.
- **Regenerate the model assets:**

  ```bash
  python3 tools/generate_model_assets.py
  ```

  Rewrites `src/main/resources/resourcepack/**` (pack) and
  `tools/blockbench_src/**` (editable sources).

### Project layout

```
src/main/java/com/wavemotorcycle/
  WaveMotorcyclePlugin.java        entry point, wiring
  config/ConfigManager.java        config + messages
  motorcycle/
    Motorcycle.java                state record (persisted)
    MotorcycleController.java      per-bike brain: physics tick, rider, fuel, damage, wheelies
    MotorcycleManager.java         central loop, spawn/remove, persistence, world load
    input/MotorcycleInput.java     per-tick rider input snapshot
    physics/
      MotorcyclePhysics.java       velocity/steering/airborne integration
      CollisionResolver.java       custom AABB vs. terrain
      TerrainSurface.java          ground height + per-material traction
    model/
      ModelPart.java               part anchors/steering/spin metadata
      MotorcycleModel.java         display entities + custom-model-data variants
      MotorcycleAnimationManager.java  IDLE/ACCELERATING/BRAKING/TURN/WHEELIE/... blended transforms
    sound/MotorcycleSoundManager.java  throttled, speed-pitched engine + event sounds
    persistence/MotorcyclePersistence.java  atomic JSON save/load
    key/WaveKeyItem.java           key item (PDC-marked NETHER_STAR)
    pack/ResourcePackManager.java  modern Paper pack delivery + status tracking
  commands/WaveCommand.java        /wave ...
  events/                          player / entity / world listeners
  util/
src/main/resources/
  paper-plugin.yml                 plugin metadata
  config.yml  messages.yml
  resourcepack/                    the generated pack (models + textures)
tools/
  generate_model_assets.py         asset generator (stdlib-only Python)
  blockbench_src/                  Blockbench-editable model sources + textures
```

---

## Model replacement guide

The pack uses standard Blockbench/`assets/minecraft` item models with
**custom model data** on a `PAPER` item, so replacing the model is a
vanilla resource-pack workflow:

1. **Where each part is loaded** (`Material.PAPER` + custom model data):

   | Part | Custom model data | Pack file |
   |---|---|---|
   | Body/frame/seat/exhaust | 1 | `assets/minecraft/models/item/paper_1.json` |
   | Front fork + fender | 2 | `assets/minecraft/models/item/paper_2.json` |
   | Handlebar + grips + mirrors | 3 | `assets/minecraft/models/item/paper_3.json` |
   | Headlight (off) | 101 | `paper_101.json` |
   | Headlight (on) | 102 | `paper_102.json` |
   | Rear light (dim) | 201 | `paper_201.json` |
   | Rear light (brake) | 202 | `paper_202.json` |
   | Front wheel | 301 | `paper_301.json` |
   | Rear wheel | 302 | `paper_302.json` |
   | Key item | 1001 (on `nether_star`) | `nether_star_1001.json` |

2. **Authored coordinate system** (important — the animation math depends on
   it): 1 model unit = 1/16 block. Each part model is authored around its own
   **anchor origin**:
   - body — ground at the middle of the wheelbase, bike facing **+Z**, +Y up;
   - front assembly / handlebar / headlight — the **front axle plane**
     (wheel center at y ≈ 5.3 units above ground, z = +10.8 units from the
     body origin);
   - rear light — the **rear axle plane** (z = −10.8);
   - wheels — centered exactly on the **axle**, in the Y–Z plane (X is the
     axle direction). Wheels must be rotationally symmetric about X to spin
     correctly.
   - Overall envelope: length ≈ 2.4 blocks, width 0.6–0.8, height ≈ 1.3.

3. **Edit and rebuild:**
   - Open `tools/blockbench_src/<part>.blockbench.json` in
     [Blockbench](https://www.blockbench.net/) (textures sit under
     `tools/blockbench_src/wavemotorcycle/textures/item/`), or edit the pack
     JSONs directly.
   - Keep UVs inside the referenced texture and keep part origins as above.
   - Copy the final models into
     `src/main/resources/resourcepack/assets/minecraft/models/item/`, rebuild
     the ZIP, re-upload, update the SHA-1, `/wave reloadpack`.
   - If a replacement model faces the wrong way, set
     `model.yaw-offset-degrees` (e.g. `180`) or `model.mirror-x: true` in
     `config.yml` — no code change needed.

4. **Custom sounds:** add `assets/<namespace>/sounds.json` to the pack and
   point the `sounds.*` config keys at them (e.g.
   `wavemotorcycle:engine_idle`).

The shipped model is a deliberately low-poly, clearly-labeled starting point
(silver fairing, black rubber, glowing headlight, red rear light) — swap in
higher-detail Wave models using the same anchors and the plugin's animation
(wheel spin, steering, wheelie, light variants) keeps working unchanged.
