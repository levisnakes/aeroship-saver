package com.levi.aeroshipsaver.client;

import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * While a delete is being confirmed, smoothly swings the camera to face the targeted ship.
 * Purely cosmetic: it overrides the render camera's yaw/pitch each frame and never touches
 * the player entity, so nothing is sent to the server. Cleared when the confirm ends.
 */
@EventBusSubscriber(modid = AeroShipSaver.MODID, value = Dist.CLIENT)
public final class AeroFocus {
    private static volatile Vec3 target;
    private static float curYaw;
    private static float curPitch;
    private static boolean initialized;

    private AeroFocus() {}

    /** Begin facing the given world point. */
    public static void start(Vec3 worldTarget) {
        target = worldTarget;
        initialized = false;
    }

    public static void stop() {
        target = null;
    }

    public static boolean active() {
        return target != null;
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Vec3 t = target;
        if (t == null || Minecraft.getInstance().player == null) {
            return;
        }
        Vec3 eye = event.getCamera().getPosition();
        double dx = t.x - eye.x;
        double dy = t.y - eye.y;
        double dz = t.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float desiredYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float desiredPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));

        if (!initialized) {
            curYaw = event.getYaw();
            curPitch = event.getPitch();
            initialized = true;
        }
        // Smooth glide toward the target angle (wrap yaw so it takes the short way round).
        curYaw += Mth.wrapDegrees(desiredYaw - curYaw) * 0.2F;
        curPitch += (desiredPitch - curPitch) * 0.2F;
        event.setYaw(curYaw);
        event.setPitch(curPitch);
    }
}
