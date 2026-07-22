# Changelog

## 1.1.0

### Multiplayer (Essential / LAN)
- **Saved ships now land on your own machine, not just the host's.** `/aeroship save` runs
  server-side, so on a hosted world the file used to be written only to the host's game
  directory. The blueprint is now synced back to the player who saved it.
- **Loading works for ships the server has never seen.** The menu's Load button sends the
  blueprint's contents with the request, so a ship saved in another world - or by a guest on
  someone else's world - loads correctly.
- Both networking channels are registered as **optional**, so the mod still doesn't prevent
  you joining a server that doesn't have it.
- Host and guest must be on the **same version** of the mod for the sync to work.

### New menu (press G)
- Redesigned with two tabs: **Saved ships** and **Nearby ships**.
- **Rotatable 3D preview** of the selected saved ship - drag to rotate, scroll to zoom -
  plus sub-level count, block count and dimensions.
- **Nearby ships** tab lists loaded physical structures with distance and size, filtered by
  an adjustable range, so you can clean up stray structures.
- **Delete confirmation now shows you what you're deleting**: the menu collapses to a bar,
  the camera swings round to face the structure, and its outline pulses in the world.
  Deleting also works on everything in range at once.
- Failed loads now report the actual reason instead of a misleading "not found".

### Fixes
- Fixed the range control not filtering the nearby list.
- Removed dead code from the old block-copy prototype.

### Known limitation
- After loading, some swivel bearings may revert to **locked** even if they were unlocked.
  Re-toggle them after loading. This is a timing quirk in the engine's contraption
  re-assembly.

## 1.0.0

- Initial release. Save and load real Create: Aeronautics ships (physical structures with
  their connected sub-levels and mounted contraptions) via `/aeroship` and a G-key menu.
- Registers no items, blocks or network packets, so it does not stop you logging in to a
  server that doesn't have it.
- Bundles and adapts the ship save/load engine from the Create Aeronautics: Toolgun by
  enxv233 (CC-BY-NC-4.0).
