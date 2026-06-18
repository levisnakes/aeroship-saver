package com.enxv.aeronauticsstructuretool;

import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders a saved-blueprint {@link PortableStructurePreviewData} as a rotatable 3D model
 * inside a GUI region. Reimplemented from the toolgun's (private) printer-screen preview so
 * it can be driven by our own menu. Lives in the toolgun's package to reach the
 * package-private {@code PreviewBlock} record.
 */
public final class AeroPreviewRenderer {
    private AeroPreviewRenderer() {}

    public static void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                              Object previewObj, float yaw, float pitch, float zoom) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(previewObj instanceof PortableStructurePreviewData preview)) {
            return;
        }
        guiGraphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        guiGraphics.flush();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        guiGraphics.pose().pushPose();
        float fitScale = computePreviewScale(width, height, preview);
        guiGraphics.pose().translate((float) x + (float) width / 2.0F, (float) y + (float) height * 0.62F, 180.0F);
        guiGraphics.pose().scale(fitScale * zoom, -fitScale * zoom, fitScale * zoom);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(pitch));
        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(yaw));
        BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        BlockEntityRenderDispatcher beDispatcher = minecraft.getBlockEntityRenderDispatcher();

        for (PortableStructurePreviewData.PreviewBlock block : preview.previewBlocks()) {
            PreviewRenderState renderState = createPreviewRenderState(minecraft, block);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(block.position().x, block.position().y, block.position().z);
            guiGraphics.pose().mulPose(new Quaternionf(block.orientation()));
            guiGraphics.pose().translate(-0.5F, -0.5F, -0.5F);
            renderPreviewBlockModel(minecraft, guiGraphics, bufferSource, block, renderState);
            Lighting.setupForEntityInInventory();
            renderPreviewBlockEntity(beDispatcher, bufferSource, guiGraphics, renderState);
            Lighting.setupFor3DItems();
            guiGraphics.pose().popPose();
        }

        bufferSource.endBatch();
        guiGraphics.pose().popPose();
        guiGraphics.flush();
        guiGraphics.disableScissor();
    }

    private static float computePreviewScale(int panelWidth, int panelHeight, PortableStructurePreviewData preview) {
        double span = Math.max(1.0, preview.maxSpan());
        float usable = (float) Math.min(panelWidth, panelHeight) * 0.46F;
        return (float) ((double) usable / span);
    }

    private static void renderPreviewBlockModel(Minecraft minecraft, GuiGraphics guiGraphics, BufferSource bufferSource,
                                                PortableStructurePreviewData.PreviewBlock block, PreviewRenderState renderState) {
        BlockPos renderPos = BlockPos.ZERO;
        BlockState state = block.state();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        ModelData modelData = renderState.modelData();
        PreviewBlockAndTintGetter getter = new PreviewBlockAndTintGetter(
            minecraft.level, renderPos, state, renderState.blockEntity(), modelData);
        modelData = model.getModelData(getter, renderPos, state, modelData);
        long seed = state.getSeed(renderPos);

        for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(seed), modelData)) {
            minecraft.getBlockRenderer().getModelRenderer().tesselateBlock(
                getter, model, state, renderPos, guiGraphics.pose(), bufferSource.getBuffer(renderType),
                false, RandomSource.create(), seed, OverlayTexture.NO_OVERLAY, modelData, renderType);
        }
    }

    private static PreviewRenderState createPreviewRenderState(Minecraft minecraft, PortableStructurePreviewData.PreviewBlock block) {
        CompoundTag beTag = block.blockEntityTag();
        if (beTag != null && !beTag.isEmpty() && minecraft.level != null) {
            BlockEntity be = BlockEntity.loadStatic(BlockPos.ZERO, block.state(), beTag, minecraft.level.registryAccess());
            if (be == null) {
                return new PreviewRenderState(null, ModelData.EMPTY);
            }
            be.setLevel(minecraft.level);
            return new PreviewRenderState(be, be.getModelData());
        }
        return new PreviewRenderState(null, ModelData.EMPTY);
    }

    private static void renderPreviewBlockEntity(BlockEntityRenderDispatcher dispatcher, BufferSource bufferSource,
                                                 GuiGraphics guiGraphics, PreviewRenderState renderState) {
        BlockEntity be = renderState.blockEntity();
        if (be != null) {
            dispatcher.renderItem(be, guiGraphics.pose(), bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        }
    }

    private record PreviewRenderState(BlockEntity blockEntity, ModelData modelData) {}

    /** A fully-lit block view so single blocks render correctly in the GUI. */
    private static final class PreviewBlockAndTintGetter implements BlockAndTintGetter {
        private final BlockAndTintGetter delegate;
        private final BlockPos renderPos;
        private final BlockState renderState;
        private final BlockEntity blockEntity;
        private final ModelData modelData;

        private PreviewBlockAndTintGetter(BlockAndTintGetter delegate, BlockPos renderPos, BlockState renderState,
                                          BlockEntity blockEntity, ModelData modelData) {
            this.delegate = delegate;
            this.renderPos = renderPos;
            this.renderState = renderState;
            this.blockEntity = blockEntity;
            this.modelData = modelData;
        }

        public float getShade(Direction direction, boolean shade) {
            return this.delegate.getShade(direction, shade);
        }

        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            return this.delegate.getBlockTint(pos, colorResolver);
        }

        public BlockEntity getBlockEntity(BlockPos pos) {
            return pos.equals(this.renderPos) ? this.blockEntity : this.delegate.getBlockEntity(pos);
        }

        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(this.renderPos) ? this.renderState : this.delegate.getBlockState(pos);
        }

        public FluidState getFluidState(BlockPos pos) {
            return pos.equals(this.renderPos) ? this.renderState.getFluidState() : this.delegate.getFluidState(pos);
        }

        public int getHeight() {
            return this.delegate.getHeight();
        }

        public int getMinBuildHeight() {
            return this.delegate.getMinBuildHeight();
        }

        public int getBrightness(LightLayer lightLayer, BlockPos pos) {
            return 15;
        }

        public int getRawBrightness(BlockPos pos, int amount) {
            return 15;
        }

        public ModelData getModelData(BlockPos pos) {
            return pos.equals(this.renderPos) ? this.modelData : ModelData.EMPTY;
        }

        public LevelLightEngine getLightEngine() {
            return this.delegate.getLightEngine();
        }
    }
}
