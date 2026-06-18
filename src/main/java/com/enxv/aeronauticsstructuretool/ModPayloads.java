package com.enxv.aeronauticsstructuretool;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stub replacement for the toolgun's networking class.
 *
 * <p>The original {@code ModPayloads} registers ~25 network channels and is the single
 * biggest reason the toolgun can't be installed client-only on a server that lacks it.
 * AeroShip Saver bundles the toolgun's <em>logic</em> classes but excludes the real
 * {@code ModPayloads}; this stub takes its place.
 *
 * <p>The only outbound calls the bundled logic makes into this class are the two
 * constraint-visual syncs below (from {@code ToolgunConstraintTracker}). Those only drove
 * the cosmetic glowing weld-lines on the client — the physics welds themselves are stored
 * in the blueprint NBT and restored without any packets — so making them no-ops costs
 * nothing functional and keeps this mod from registering a single network channel.
 */
public final class ModPayloads {
    private ModPayloads() {}

    /** No-op: weld-line visuals are not synced (the welds themselves still save/load). */
    public static void syncConstraintVisuals(ServerLevel level) {
        // intentionally empty
    }

    /** No-op: see {@link #syncConstraintVisuals(ServerLevel)}. */
    public static void syncConstraintVisualsTo(ServerPlayer player, ServerLevel level) {
        // intentionally empty
    }
}
