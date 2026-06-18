package com.enxv.aeronauticsstructuretool;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Stub replacement for the toolgun's {@code @Mod} entry class.
 *
 * <p>The real class is annotated {@code @Mod} and its constructor registers all the items,
 * blocks, block-entities, creative tabs and network payloads - exactly the things that
 * would break logging in to a server without the toolgun. We exclude it from the bundle and
 * put this in its place: same static fields the bundled logic reads (the manager singletons
 * and logger), but <em>no</em> {@code @Mod} annotation and <em>no</em> registration calls.
 *
 * <p>The managers are merely instantiated so {@code SubLevelFileStore} can read their state
 * during save/load; they are deliberately NOT registered to the event bus, so they don't
 * tick (which is what spammed "Cannot add a constraint between a sub-level and itself").
 */
public class AeronauticsStructureToolMod {
    public static final String MOD_ID = "create_aeronautics_toolgun";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean DIAGNOSTIC_LOGGING = false;
    public static final boolean DRIVEBYWIRE_DIAGNOSTIC_LOGGING = false;

    public static final RuntimeContraptionRestoreManager RESTORE_MANAGER =
        new RuntimeContraptionRestoreManager();
    public static final MagneticGunServerController MAGNETIC_GUN_SERVER_CONTROLLER =
        new MagneticGunServerController();
    public static final SubLevelCollisionToggleManager SUBLEVEL_COLLISION_TOGGLE_MANAGER =
        new SubLevelCollisionToggleManager();
    public static final DriveByWireWorldSourceRefreshManager DRIVEBYWIRE_WORLD_SOURCE_REFRESH_MANAGER =
        new DriveByWireWorldSourceRefreshManager();
    public static final DriveByWireFullSyncManager DRIVEBYWIRE_FULL_SYNC_MANAGER =
        new DriveByWireFullSyncManager();
    public static final SynaxisControllerWireRestoreManager SYNAXIS_CONTROLLER_WIRE_RESTORE_MANAGER =
        new SynaxisControllerWireRestoreManager();
    public static final HardBlockMissileCleanupManager HARDBLOCK_MISSILE_CLEANUP_MANAGER =
        new HardBlockMissileCleanupManager();

    private AeronauticsStructureToolMod() {}
}
