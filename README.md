# AeroShip Saver

A **content-free** NeoForge 1.21.1 mod that saves and loads **real Create: Aeronautics
ships** (physical structures / sub-levels, with their mounted contraptions) via a command
and a keybind — with **no items, no blocks, and no network packets of its own**.

Because it registers nothing synced, it does **not** break logging in to a multiplayer
server that doesn't have it. Install it in your singleplayer profile, save your ships, and
still join your survival server with the same instance.

- **Minecraft:** 1.21.1 · **Loader:** NeoForge 21.1.230+
- **Requires (already in your modpack):** Create, Create: Aeronautics (`aeronautics_bundled`), Sable
- **Built jar:** `build/libs/aeroshipsaver-1.0.0.jar`
- **License:** CC BY-NC 4.0 · **Credit:** bundles & adapts the save/load engine from the [Create Aeronautics: Toolgun](https://modrinth.com/mod/create-aeronautics-toolgun) by **enxv233** (CC BY-NC 4.0). See [LICENSE](LICENSE).

## How it works

The Create Aeronautics: Toolgun saves real ships, but it adds items, blocks and packets, so
a server without it rejects your login. AeroShip Saver is a **fork-and-strip** of that
toolgun: it bundles the toolgun's actual save/load engine (`SubLevelFileStore` + restore
managers, by enxv233, CC-BY-NC-4.0) but replaces the two classes that register content — the
`@Mod` entry and the networking class — with stubs that register nothing. The runtime
contraption restore manager is re-enabled so mounted contraptions (e.g. swivel bearings)
come back after a load.

## Install

Put `aeroshipsaver-1.0.0.jar` in the `mods` folder of your singleplayer profile (alongside
Create + Aeronautics + Sable). Saved ships are written to `enxv_aeronautics_structures` in
your game directory as `.excraft` files.

## Usage

1. Build and **assemble** your Aeronautics ship into a physical structure.
2. **Look at it** (within ~48 blocks) or stand on it.
3. `/aeroship save <name>` — or press **G** for a menu, type a name, Save.
4. Later: stand where you want it and `/aeroship load <name>` (it spawns at you).

| Command | Does |
|---|---|
| `/aeroship save <name>` | Save the physical ship you're looking at / standing on (with connected sub-levels and mounted contraptions) |
| `/aeroship load <name>` | Spawn a saved ship at your position |
| `/aeroship list` | List saved ships |
| `/aeroship delete <name>` | Delete a saved ship |

## What works

Tested and working: single and multi-sub-level ships (7+ sub-levels), swivel bearings and
other mounted contraptions, wireless redstone links, repeated loads, persistence across game
restarts, and — the whole point — **joining a survival server that doesn't run this mod**.

## Known limitation

After loading, some swivel bearings may revert to **locked** even if you'd set them to
unlocked. This is a timing quirk in the engine's contraption re-assembly (the bearing
initializes locked before the saved state applies). Workaround: re-toggle the affected
bearings to unlocked after loading.

## Build from source

This repo contains only the original source. The third-party binaries it builds against are
**not** committed (they aren't ours to redistribute), so to build you must first supply:

1. **`libs/`** — these mod jars, used compile-only:
   `create_aeronautics_toolgun-0.1.9.jar`, `sable-neoforge-1.21.1-1.2.2.jar`,
   `sable-companion-common-1.21.1-1.6.0.jar` (from Sable's `META-INF/jarjar/`),
   `create-aeronautics-bundled-1.21.1-1.2.1.jar`, `create-1.21.1-6.0.10.jar`.
2. **`bundle-classes/com/enxv/aeronauticsstructuretool/`** — the toolgun's `.class` files,
   all of them **except** `AeronauticsStructureToolMod.class` and `ModPayloads.class` (those
   two are replaced by stubs in `src`). They get packaged into the output jar.

Then `./gradlew build` → `build/libs/aeroshipsaver-1.0.0.jar`.
