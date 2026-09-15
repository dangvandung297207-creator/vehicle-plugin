# VanillaVehicles

A complete, dependency-free vehicle framework and 34 ready-to-use vehicles for **Paper 26.2** — working with a **100% vanilla client**. No mods, no resource packs, no ProtocolLib, no ModelEngine. Every vehicle is built at runtime from vanilla display entities, sounds and particles.

## Vehicle roster (34)

**Ground:** Compact Car, Sports Car, Muscle Car, Pickup Truck, Delivery Van, Minivan, SUV, Bus, Taxi, Police Car, Ambulance, Fire Truck, Garbage Truck, Construction Truck, Military Truck, Tank, Golf Cart, ATV, Motorcycle, Scooter
**Utility:** Tractor, Forklift, Excavator, Bulldozer, Racing Kart
**Water:** Speedboat, Fishing Boat, Yacht
**Air:** Helicopter, Small Plane, Fighter Jet
**Rail:** Minecart Racer, Locomotive, Passenger Train

Type ids for commands: `car sports_car muscle_car pickup van minivan suv bus taxi police_car ambulance fire_truck garbage_truck construction_truck military_truck tank golf_cart atv motorcycle scooter tractor forklift excavator bulldozer racing_kart speedboat fishing_boat yacht helicopter plane fighter_jet locomotive passenger_train minecart_racer`

## Installation

1. Build the jar (see below) or download `VanillaVehicles-1.1.0.jar`.
2. Drop it into your server's `plugins/` folder.
3. Start a Paper 26.2 server (Java 25). `config.yml` is generated on first run.
4. Run `/vehicle garage`, click a vehicle, right-click it to board.

## Build instructions

Requirements: JDK 25 + Gradle 9.x.

```bash
gradle build
# jar: build/libs/VanillaVehicles-1.1.0.jar
```

The build only needs `io.papermc.paper:paper-api:26.2.build.124-stable` from `https://repo.papermc.io/repository/maven-public/`. Nothing is shaded — the plugin has zero runtime dependencies.

## Commands (`/vehicle`, alias `/vv`)

| Command | Permission | Description |
|---|---|---|
| `spawn <type>` | `vehicle.spawn` | Spawn a vehicle in front of you |
| `remove` | `vehicle.remove` | Remove the vehicle you ride / own nearby |
| `removeall` | `vehicle.admin` | Remove every vehicle |
| `list [mine\|all]` | `vehicle.use` | List active vehicles |
| `info` | `vehicle.use` | Inspect your / nearest vehicle |
| `give <type> [player]` | `vehicle.give` | Vehicle spawner item (right-click a block) |
| `reload` | `vehicle.reload` | Reload config (applies to newly spawned vehicles) |
| `debug` | `vehicle.admin` | Toggle live debug action-bar readout |
| `help` | `vehicle.use` | Help + type list |
| `garage` | `vehicle.garage` | Click-to-spawn GUI |
| `siren` | `vehicle.use` | Toggle siren (driver, emergency vehicles) |
| `boost` | `vehicle.use` | Trigger boost (driver, supported vehicles) |
| `lights` | `vehicle.use` | Toggle headlights (driver) |
| `horn` | `vehicle.use` | Honk (driver) |
| `exit` | `vehicle.use` | Get out |
| `cargo` | `vehicle.use` | Open vehicle storage |
| `fare [reset]` | `vehicle.use` | Taxi fare meter |
| `tool <ladder\|fork\|dump\|arm\|bucket\|turret>` | `vehicle.use` | Mechanisms (driver) |

Tab completion is provided for everything.

## Permissions

`vehicle.use` (default true), `vehicle.spawn` (true), `vehicle.remove` (true), `vehicle.garage` (true), `vehicle.give` (op), `vehicle.reload` (op), `vehicle.admin` (op, includes the rest). Each vehicle type can additionally require its own permission via `vehicles.<id>.permission` in config.yml.

## Controls

The plugin uses Paper's native `PlayerInputEvent`, so every driver gets true key input:

**Standard mode** (always on): `W/S` throttle/brake/reverse, `A/D` steer, `Space` lift/boost, `Shift` brake/exit, plus mouse steering for boats and aircraft aim.

**Fallback safety net** (kicks in if live input ever goes stale): hotbar slot = cruise gear (slot 1 reverse … slot 9 full speed), mouse = steering (look where you want to go), sneak-hold = brake (dismount is cancelled while moving; press Shift again when slow to exit), `Q` = exit, `F` = headlights, left-click = horn, right-click = vehicle special (siren / boost / horn / tools), `Space` = lift where supported.

Aircraft: `Shift` never exits mid-air (it is descend/airbrake) — press `Q` or use `/vehicle exit`. Helicopters fly toward your crosshair; planes use a persistent throttle (hotbar or `W/S`) and rotate when fast with the nose up.

## Architecture

```
com.example.vanillavehicles
├── VanillaVehicles        # boot, shutdown save, debug registry
├── api/                   # VanillaVehiclesAPI for other plugins
├── vehicle/               # Vehicle, VehicleManager, VehicleType/Stats/Definition/Registry, TrainCarriage, TaxiMeter, VehicleDefinitions (all 34)
├── model/                 # VehicleModel builder, ModelPart, ModelMath, PartKind/PartFlag
├── entity/                # DisplayFactory (spawning + PDC tagging), VehicleTags
├── seat/                  # Seat definitions + SeatInstance (invisible armor stands)
├── input/                 # InputManager, InputState (native PlayerInputEvent WASD)
├── physics/               # PhysicsEngine (CAR/BIKE/HEAVY/BOAT/AIRCRAFT/TRAIN/CONSTRUCTION)
├── collision/             # CollisionHandler (cheap block sampling)
├── animation/             # VehicleAnimator (wheels, tracks, rotors, turret, lamps, channels, particles)
├── sound/                 # SoundManager + SoundProfile (vanilla sounds, speed-pitched)
├── storage/               # VehicleStorage (vehicles.yml + orphan cleanup), GarageManager, CargoHolder
├── config/                # PluginConfig (config.yml overlay)
├── command/               # /vehicle
├── event/                 # Spawn/Enter/Exit/Damage/Destroy/Move events
└── util/                  # Message/Particle/Sound helpers (name-resolved, crash-proof)
```

**One central tick:** `VehicleManager` runs a single 20 Hz task that ticks every active vehicle. Parked vehicles (no driver, no speed, settled mechanisms, siren off) skip physics/transforms entirely. Entity validity is re-checked periodically and missing parts are respawned and re-indexed.

## Physics

Arcade, stable, deterministic at 20 Hz: scalar speed + heading per vehicle, acceleration curves with top-speed falloff, coasting friction + proportional drag, speed-sensitive steering authority (full lock when slow, damped when fast), reverse-inverted steering, gravity with ground snapping, and per-profile behavior:

- **CAR/BIKE/HEAVY/CONSTRUCTION** — road model; bikes lean; tanks/dozers/excavators turn in place; ATV/SUV/motorcycle/military climb 1-block steps; kart drifts; muscle car burnouts.
- **BOAT** — floats on the liquid surface with bob, spray and beaching drag.
- **AIRCRAFT** — helicopter hover with look-vector flight + Space lift; planes with throttle, takeoff speed, climb angle from view pitch, stall drop and landing impact.
- **TRAIN** — persistent target speed, keyboard-only micro steering (“rails feel”), articulated carriages following a recorded path.

Collision samples ~21 blocks in a yaw-aligned box plus ground/water scans — no ray tracing. Impacts bounce, play sounds, emit particles and scale damage by speed and mass (heavy vehicles shrug off crashes, bikes don't). Vehicle-vs-vehicle uses cheap circle separation with mass weighting. Lava burns.

## Display-entity model system

Each vehicle is a `VehicleModel`: `box()` (BlockDisplay, axis-aligned, corner-pivot compensated), `item()`/`itemSized()` (ItemDisplay, center-pivot — used for **everything that rotates**), `text()` (TextDisplay signs), `wheel()` (tire + hub with spin/steer flags), `track()` (scrolling tread segments), `seat()`, `lamp()` (swappable headlight/brake/reverse/emergency materials), `hinge()`/`slider()` (channel-driven mechanisms: ladder, forks, dump bed, excavator boom/stick/bucket, mower).

Local space is `+X = left, +Y = up, +Z = forward`. Part entities are teleported to the vehicle origin each tick with the vehicle yaw/pitch — translations live in entity-local space so the whole model follows for free. Roll (lean/bank) is baked into translations because entities have no roll axis; static parts refresh only while rolling. All rotations compose through dependency-free quaternion math (`ModelMath.eulerToAxisAngle`), and wheel spin derives from travelled distance ÷ wheel radius so it always matches ground speed.

Every entity carries `vehicle_id / vehicle_type / vehicle_part / vehicle_instance` PDC tags; seats are invisible marker armor stands; one `Interaction` hitbox per vehicle handles right-click boarding.

## Seat / input system

Seats are definitions (offset + driver flag); each spawn creates invisible, gravity-free, invulnerable, non-colliding armor stands teleported to the rotated offsets every tick. Entering prefers the free driver seat, then any free seat (including train carriages). Dismounts route through `EntityDismountEvent`: cancelled for braking (fallback) and aircraft descend, otherwise exiting through the cancellable `VehicleExitEvent`. Stale riders (quit/dead) are cleaned every tick; `Q` always exits.

Input merges (a) the native `PlayerInputEvent` listener, (b) look tracking, sneak/gear/click/`Q`/`F`/jump listeners. Sounds and names everywhere resolve by string with safe fallbacks, so config typos degrade gracefully instead of crashing.

## Adding a new vehicle

1. Add an entry to `VehicleType` (id, names, category/tier/physics, tuning, size, description).
2. Add a `buildX()` method in `VehicleDefinitions` using the `VehicleModel` builder and register it in `registerAll()` with a garage icon.
3. Add a `vehicles.<id>:` section to `config.yml` (optional — defaults come from the enum).
4. If it needs unique behavior, extend `Vehicle.special()`/`tool()` and/or `VehicleAnimator` with a new channel.

No other code changes are required — spawning, seats, physics, garage, persistence, tab completion and the API pick it up automatically.

## Garage, cargo, taxi, trains

- **Garage** (`/vehicle garage`): permission-aware GUI, click to spawn; modular for future economy hooks.
- **Cargo**: vans/trucks/ambulances have inventories (`/vehicle cargo`), dropped on destroy, persisted across restarts.
- **Taxi**: distance fare meter with configurable rate, no economy dependency by design.
- **Trains**: configurable carriage counts; carriages follow the leader's path with independent seats; locomotive + express styling.

## Performance notes

- One repeating task total; parked vehicles cost ~nothing; unloaded chunks are skipped.
- Teleports only (no respawns) per tick; transformation updates limited to animated parts (+ roll refresh while leaning).
- No world entity scans except one orphan cleanup at boot and throttled 4 Hz vehicle-vs-vehicle checks.
- Particles/sounds are throttled per vehicle. Designed for dozens of concurrent vehicles; trains are the heaviest (each carriage is a full model).

## Known limitations (vanilla Paper 26.2)

- Requires Paper 26.2+ (uses the stable `PlayerInputEvent` API) — the v1.0.0 release remains available for Paper 1.21.1 servers.
- Riders keep upright posture (armor-stand seats), so tall leans/pitches move the seat position but not the rider's body angle.
- Heads may sit above the roofline on low closed cars (standard for display-entity vehicles); seat offsets are tunable in `VehicleDefinitions`.
- Headlights are bright blocks + night beams, not real dynamic light (impossible vanilla).
- No per-wheel suspension raycasts; step-up climbing and slopes are approximated.
- Display entities render within their view range (~64 blocks); distant vehicles pop in like any entity.
- The tank cannon is visual-only by design (smoke + flash + thump, zero damage).
- Cargo persists, but riders never persist across restarts (everyone dismounts on shutdown).
