# AeroShip Saver — Modrinth project description

Paste this into the Modrinth project "Description" (it accepts Markdown).

---

## AeroShip Saver

Save and load **real Create: Aeronautics ships** — physical structures with their mounted contraptions, swivel bearings and wireless redstone — using a simple command and an in-game menu. **No items, no blocks, no network packets of its own**, so installing it on your client **won't stop you logging in to a server that doesn't have it.**

### Features
- Save the ship you're standing on / looking at: `/aeroship save <name>`
- Load it back anywhere: `/aeroship load <name>`
- Press **G** for a clean menu with:
  - A **rotatable 3D preview** of each saved ship (drag to rotate, scroll to zoom)
  - A **Nearby ships** tab to delete loose physical structures, filtered by range
  - A delete flow that **swings the camera to the ship and highlights it** so you see exactly what you're removing — with confirmation
- Saves persist across worlds and restarts (`enxv_aeronautics_structures/*.excraft`)

### Requirements
- Minecraft **1.21.1**, **NeoForge 21.1.230+**
- **Create**, **Create: Aeronautics**, and **Sable** (this mod calls into their physics engine)

### Singleplayer vs servers
Aeronautics ships are server-side physics objects, so saving/loading runs on your singleplayer integrated server. The mod adds no synced content, so you can keep it in the same instance you use to join servers without it.

### Credits & license
This mod **bundles and adapts the ship save/load engine** from the **[Create Aeronautics: Toolgun](https://modrinth.com/mod/create-aeronautics-toolgun) by enxv233**, with all item/block/network registration removed so it stays content-free. Full credit for that engine goes to **enxv233**. Released under **CC BY-NC 4.0**, the same license as the bundled code — non-commercial use only.

---

### Modrinth project settings checklist
- **Project type:** Mod
- **License:** CC-BY-NC-4.0
- **Loaders:** NeoForge
- **Game versions:** 1.21.1
- **Environment:** Client (works via the integrated server in singleplayer); set "Server: optional/unsupported" as you prefer
- **Dependencies (add on the Versions page):**
  - Create: Aeronautics — required
  - Sable — required
  - Create — required (usually pulled by Aeronautics)
- **Tags/categories:** Utility, Storage, Management
- **Body:** the description above. Make sure the enxv233 credit stays visible.
