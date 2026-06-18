package com.enxv.aeronauticsstructuretool;

import java.io.ByteArrayInputStream;

import org.joml.Vector3d;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/**
 * Bridge into the toolgun's bundled blueprint parser. Lives in the toolgun's package so it
 * can reach the package-private {@code PortableStructurePreviewData.PreviewBlock} record.
 *
 * <p>Phase 1 exposes summary info (sub-levels, block count, bounding size) for the menu's
 * info panel. The parsed {@link PortableStructurePreviewData} is also what a future 3D
 * preview would render.
 */
public final class AeroPreview {
    private AeroPreview() {}

    /** Summary of a saved blueprint for the menu. {@code blocks < 0} means it failed to parse. */
    public record Info(int subLevels, int blocks, int sizeX, int sizeY, int sizeZ) {
        public static final Info UNKNOWN = new Info(-1, -1, 0, 0, 0);
        public boolean ok() {
            return blocks >= 0;
        }
        public String sizeString() {
            return sizeX + "×" + sizeY + "×" + sizeZ;
        }
    }

    /**
     * Parse a blueprint's bytes into the toolgun's preview data (for 3D rendering), returned
     * as {@link Object} because the underlying type is package-private. Pass it straight to
     * {@link AeroPreviewRenderer#render}. Null on failure.
     */
    public static Object parse(String name, byte[] bytes, Level level) {
        try {
            return PortableStructurePreviewData.fromBlueprintBytes(name, bytes, level);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Parse a blueprint's bytes into summary info, or {@link Info#UNKNOWN} on failure. */
    public static Info info(String name, byte[] bytes, Level level) {
        try {
            PortableStructurePreviewData data = PortableStructurePreviewData.fromBlueprintBytes(name, bytes, level);
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            int count = 0;
            for (PortableStructurePreviewData.PreviewBlock block : data.previewBlocks()) {
                Vector3d p = block.position();
                minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); minZ = Math.min(minZ, p.z);
                maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); maxZ = Math.max(maxZ, p.z);
                count++;
            }
            int sx = count == 0 ? 0 : (int) Math.round(maxX - minX) + 1;
            int sy = count == 0 ? 0 : (int) Math.round(maxY - minY) + 1;
            int sz = count == 0 ? 0 : (int) Math.round(maxZ - minZ) + 1;
            return new Info(countSubLevels(bytes), count, sx, sy, sz);
        } catch (Throwable t) {
            return Info.UNKNOWN;
        }
    }

    private static int countSubLevels(byte[] bytes) {
        try {
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
            return root.contains("sublevels", Tag.TAG_LIST) ? root.getList("sublevels", Tag.TAG_COMPOUND).size() : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
