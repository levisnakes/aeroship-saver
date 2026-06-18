package com.levi.aeroshipsaver.client;

import com.levi.aeroshipsaver.AeroShipSaver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws a bright outline box around the physical structure the player has selected in the
 * Nearby-ships menu, so they can see exactly which ship they're about to delete. The box is
 * world-space and rendered through the menu's dimmed background.
 */
@EventBusSubscriber(modid = AeroShipSaver.MODID, value = Dist.CLIENT)
public final class AeroHighlight {
    private static volatile AABB box;

    private AeroHighlight() {}

    public static void set(AABB b) {
        box = b;
    }

    public static void clear() {
        box = null;
    }

    public static AABB box() {
        return box;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        AABB b = box;
        if (b == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        // Pulse the outline so it stands out while you decide.
        float pulse = 0.55F + 0.45F * (float) Math.sin(System.currentTimeMillis() / 180.0);
        LevelRenderer.renderLineBox(pose, lines, b, 0.25F, 0.9F, 1.0F, pulse);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }
}
