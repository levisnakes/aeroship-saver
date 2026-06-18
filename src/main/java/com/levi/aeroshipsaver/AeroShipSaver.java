package com.levi.aeroshipsaver;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.enxv.aeronauticsstructuretool.AeronauticsStructureToolMod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * AeroShip Saver - save and load real Create: Aeronautics ships via command/keybind,
 * with no items, blocks, or network packets of its own.
 *
 * <p>This mod bundles the save/load engine from the Create Aeronautics: Toolgun
 * (by enxv233, CC-BY-NC-4.0). The toolgun's {@code @Mod} class and networking class are
 * replaced with stubs ({@link com.enxv.aeronauticsstructuretool.AeronauticsStructureToolMod}
 * and {@code ModPayloads}) that register no content - so this mod adds nothing synced and
 * will not break logging in to a server that doesn't have it.
 *
 * <p>Most of the toolgun's restore managers are intentionally NOT registered (their static,
 * constraint-syncing tick handlers spammed "Cannot add a constraint between a sub-level and
 * itself"). The one exception is {@code RESTORE_MANAGER}: load queues mounted Create
 * contraptions (e.g. swivel-bearing assemblies) into it, and its instance tick handler
 * re-spawns them over the following ticks. We register that single instance below. Its
 * queue is empty until a load happens, so it does no work - and adds no constraints - at
 * rest, which keeps it clear of the spam.
 */
@Mod(AeroShipSaver.MODID)
public class AeroShipSaver {
    public static final String MODID = "aeroshipsaver";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroShipSaver(IEventBus modEventBus) {
        // Drain the runtime-contraption restore queue on server ticks so mounted
        // contraptions (swivel bearings, etc.) come back after a load.
        NeoForge.EVENT_BUS.register(AeronauticsStructureToolMod.RESTORE_MANAGER);
        LOGGER.info("AeroShip Saver loaded (content-free Aeronautics ship save/load).");
    }
}
