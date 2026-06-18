package com.levi.aeroshipsaver.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.enxv.aeronauticsstructuretool.AeroPreview;
import com.enxv.aeronauticsstructuretool.AeroPreviewRenderer;
import com.enxv.aeronauticsstructuretool.SubLevelFileStore;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The G-key menu. Two views (Saved / Nearby) toggled by tab buttons. Saving/loading/deleting
 * sends the matching {@code /aeroship} command so the real work stays server-side; listings
 * are read locally. Destructive actions go through an inline confirm prompt.
 */
public class AeroShipScreen extends Screen {
    private enum Tab { SAVED, NEARBY }

    private static final int PANEL_W = 440;
    private static final int PANEL_H = 250;
    private static final int ROWS = 6;

    private Tab tab = Tab.SAVED;
    private String nameText = "";
    private String selected;
    private AeroPreview.Info selectedInfo;
    private Object selectedPreview;
    private float previewYaw = -35.0F;
    private float previewPitch = 25.0F;
    private float previewZoom = 1.0F;
    private int range = 32;
    private UUID selectedNearby;

    private Runnable confirmAction;
    private String confirmText;
    private boolean focusMode;
    private int savedBlur = -1;

    private EditBox nameBox;
    private int left;
    private int top;

    public AeroShipScreen() {
        super(Component.literal("AeroShip Saver"));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;

        if (this.confirmAction != null) {
            if (this.focusMode) {
                initFocusConfirm();
            } else {
                initConfirm();
            }
            return;
        }
        // Tab switcher
        this.addRenderableWidget(Button.builder(Component.literal("Saved ships"), b -> switchTab(Tab.SAVED))
            .bounds(left + 12, top + 26, 120, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Nearby ships"), b -> switchTab(Tab.NEARBY))
            .bounds(left + 138, top + 26, 120, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
            .bounds(left + PANEL_W - 70, top + 26, 58, 18).build());

        if (this.tab == Tab.SAVED) {
            initSaved();
        } else {
            initNearby();
        }
    }

    private void switchTab(Tab t) {
        saveNameText();
        if (t != Tab.NEARBY) {
            AeroHighlight.clear();
            this.selectedNearby = null;
        }
        this.tab = t;
        this.rebuildWidgets();
    }

    @Override
    public void onClose() {
        AeroHighlight.clear();
        AeroFocus.stop();
        restoreBlur();
        super.onClose();
    }

    private void saveNameText() {
        if (this.nameBox != null) {
            this.nameText = this.nameBox.getValue();
        }
    }

    // ---- Saved ships view ---------------------------------------------------

    private void initSaved() {
        int colW = PANEL_W / 2 - 18;
        int x = left + 12;
        int y = top + 54;

        this.nameBox = new EditBox(this.font, x, y, colW - 64, 18, Component.literal("Ship name"));
        this.nameBox.setHint(Component.literal("ship name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setValue(this.nameText);
        this.nameBox.setResponder(v -> this.nameText = v);
        this.addRenderableWidget(this.nameBox);
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
                String name = this.nameText.trim();
                if (!name.isEmpty()) {
                    runCommand("aeroship save " + name);
                    this.onClose();
                }
            }).bounds(x + colW - 60, y, 60, 18).build());

        List<String> ships = savedNames();
        int ly = y + 26;
        for (int i = 0; i < Math.min(ships.size(), ROWS); i++) {
            String name = ships.get(i);
            boolean sel = name.equals(this.selected);
            this.addRenderableWidget(Button.builder(Component.literal((sel ? "» " : "") + name), b -> select(name))
                .bounds(x, ly, colW, 18).build());
            ly += 20;
        }

        // Right column: Load / Delete for the selection (info drawn in render()).
        int rx = left + PANEL_W / 2 + 6;
        int rw = PANEL_W / 2 - 18;
        int by = top + PANEL_H - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Load"), b -> {
                if (this.selected != null) { runCommand("aeroship load " + this.selected); this.onClose(); }
            }).bounds(rx, by, rw - 66, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
                if (this.selected != null) {
                    askConfirm("Delete saved ship '" + this.selected + "'?", () -> {
                        runCommand("aeroship delete " + this.selected);
                        this.selected = null; this.selectedInfo = null;
                    });
                }
            }).bounds(rx + rw - 62, by, 62, 18).build());
    }

    private void select(String name) {
        this.selected = name;
        this.selectedInfo = null;
        this.selectedPreview = null;
        this.previewYaw = -35.0F;
        this.previewPitch = 25.0F;
        this.previewZoom = 1.0F;
        try {
            byte[] bytes = Files.readAllBytes(saveDir().resolve(name + SubLevelFileStore.FILE_EXTENSION));
            var level = this.minecraft != null ? this.minecraft.level : null;
            this.selectedInfo = AeroPreview.info(name, bytes, level);
            this.selectedPreview = AeroPreview.parse(name, bytes, level);
        } catch (Exception ignored) {
            this.selectedInfo = AeroPreview.Info.UNKNOWN;
        }
        this.rebuildWidgets();
    }

    /** The right-hand 3D preview region [x, y, w, h]. */
    private int[] previewRect() {
        int rx = left + PANEL_W / 2 + 6;
        int rw = PANEL_W / 2 - 18;
        return new int[]{rx, top + 64, rw, 96};
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.tab == Tab.SAVED && this.selectedPreview != null && button == 0 && inPreview(mouseX, mouseY)) {
            this.previewYaw += (float) dragX * 0.8F;
            this.previewPitch = Mth.clamp(this.previewPitch + (float) dragY * 0.5F, -70.0F, 70.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.tab == Tab.SAVED && this.selectedPreview != null && inPreview(mouseX, mouseY)) {
            this.previewZoom = Mth.clamp(this.previewZoom + (float) scrollY * 0.08F, 0.45F, 2.2F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean inPreview(double mx, double my) {
        int[] r = previewRect();
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    // ---- Nearby ships view --------------------------------------------------

    private record Nearby(UUID id, double dist, AeroPreview.Info size, AABB box) {}

    private void initNearby() {
        int x = left + 12;
        int y = top + 54;
        this.addRenderableWidget(Button.builder(Component.literal("Range -"), b -> { range = Math.max(8, range - 8); rebuildWidgets(); })
            .bounds(x, y, 60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Range +"), b -> { range = Math.min(128, range + 8); rebuildWidgets(); })
            .bounds(x + 64, y, 60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Delete all in range"), b ->
                askConfirm("Delete ALL physical structures within " + range + " blocks?", () -> runCommand("aeroship removenear " + range)))
            .bounds(left + PANEL_W - 162, y, 150, 18).build());

        List<Nearby> nearby = nearbyShips();
        int ly = y + 26;
        for (int i = 0; i < Math.min(nearby.size(), ROWS); i++) {
            Nearby n = nearby.get(i);
            boolean sel = n.id.equals(this.selectedNearby);
            String label = (sel ? "» " : "") + n.id.toString().substring(0, 8) + "   "
                + Math.round(Math.sqrt(n.dist)) + "m   " + n.size.sizeString();
            // Clicking the row highlights the structure in the world so you can see it.
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> selectNearby(n))
                .bounds(x, ly, PANEL_W - 100, 18).build());
            this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
                    selectNearby(n);
                    askFocusConfirm("Delete this structure?  " + n.size.sizeString() + "  ·  "
                            + Math.round(Math.sqrt(n.dist)) + "m away", n.box,
                        () -> { runCommand("aeroship removeid " + n.id); AeroHighlight.clear(); this.selectedNearby = null; });
                }).bounds(x + PANEL_W - 96, ly, 72, 18).build());
            ly += 20;
        }
    }

    private void selectNearby(Nearby n) {
        this.selectedNearby = n.id;
        AeroHighlight.set(n.box);
        this.rebuildWidgets();
    }

    private List<Nearby> nearbyShips() {
        List<Nearby> out = new ArrayList<>();
        Minecraft mc = this.minecraft;
        if (mc == null || mc.level == null || mc.player == null) {
            return out;
        }
        Vec3 pp = mc.player.position();
        try {
            for (SubLevel sl : SubLevelContainer.getContainer(mc.level).getAllSubLevels()) {
                Vec3 c = center(sl);
                AABB box = aabbOf(sl);
                if (c == null || box == null || pp.distanceToSqr(c) > (double) range * range) {
                    continue;
                }
                out.add(new Nearby(sl.getUniqueId(), pp.distanceToSqr(c), sizeOf(sl), box));
            }
        } catch (Throwable ignored) {
            // Sable client container may be unavailable in some states
        }
        out.sort((a, b) -> Double.compare(a.dist, b.dist));
        return out;
    }

    private static Vec3 center(SubLevel sl) {
        try {
            BoundingBox3i bb = new BoundingBox3i(new BoundingBox3d(sl.boundingBox()));
            return new Vec3((bb.minX() + bb.maxX()) / 2.0, (bb.minY() + bb.maxY()) / 2.0, (bb.minZ() + bb.maxZ()) / 2.0);
        } catch (Throwable t) {
            return null;
        }
    }

    private static AeroPreview.Info sizeOf(SubLevel sl) {
        try {
            BoundingBox3i bb = new BoundingBox3i(new BoundingBox3d(sl.boundingBox()));
            return new AeroPreview.Info(1, 0, bb.maxX() - bb.minX() + 1, bb.maxY() - bb.minY() + 1, bb.maxZ() - bb.minZ() + 1);
        } catch (Throwable t) {
            return AeroPreview.Info.UNKNOWN;
        }
    }

    /** World-space bounding box of a live physical structure, for the highlight outline. */
    private static AABB aabbOf(SubLevel sl) {
        try {
            BoundingBox3i bb = new BoundingBox3i(new BoundingBox3d(sl.boundingBox()));
            return new AABB(bb.minX(), bb.minY(), bb.minZ(), bb.maxX() + 1.0, bb.maxY() + 1.0, bb.maxZ() + 1.0);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- Confirmation prompt ------------------------------------------------

    private void askConfirm(String text, Runnable action) {
        this.confirmText = text;
        this.confirmAction = action;
        this.focusMode = false;
        this.rebuildWidgets();
    }

    /** Confirm that collapses the menu to a bottom bar and swings the camera to face the ship. */
    private void askFocusConfirm(String text, AABB box, Runnable action) {
        this.confirmText = text;
        this.confirmAction = action;
        this.focusMode = true;
        setBlur(0);
        AeroFocus.start(box.getCenter());
        this.rebuildWidgets();
    }

    private void endConfirm(boolean run) {
        Runnable a = this.confirmAction;
        this.confirmAction = null;
        this.confirmText = null;
        this.focusMode = false;
        restoreBlur();
        AeroFocus.stop();
        if (run && a != null) {
            a.run();
        }
        this.rebuildWidgets();
    }

    /** Temporarily disable the vanilla menu-background blur so the ship reads clearly. */
    private void setBlur(int value) {
        if (this.minecraft != null && this.savedBlur < 0) {
            var opt = this.minecraft.options.menuBackgroundBlurriness();
            this.savedBlur = opt.get();
            opt.set(value);
        }
    }

    private void restoreBlur() {
        if (this.minecraft != null && this.savedBlur >= 0) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.savedBlur);
            this.savedBlur = -1;
        }
    }

    private void initConfirm() {
        int cx = this.width / 2;
        int y = top + PANEL_H / 2 + 6;
        this.addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> endConfirm(true))
            .bounds(cx - 104, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> endConfirm(false))
            .bounds(cx + 4, y, 100, 20).build());
    }

    private void initFocusConfirm() {
        int cx = this.width / 2;
        int y = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> endConfirm(true))
            .bounds(cx - 104, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> endConfirm(false))
            .bounds(cx + 4, y, 100, 20).build());
    }

    // ---- Shared -------------------------------------------------------------

    private static Path saveDir() {
        return SubLevelFileStore.getSaveDirectory(FMLPaths.GAMEDIR.get());
    }

    private static List<String> savedNames() {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(saveDir())) {
            files.filter(p -> p.getFileName().toString().endsWith(SubLevelFileStore.FILE_EXTENSION))
                 .forEach(p -> {
                     String f = p.getFileName().toString();
                     names.add(f.substring(0, f.length() - SubLevelFileStore.FILE_EXTENSION.length()));
                 });
        } catch (IOException ignored) {
            // folder may not exist yet
        }
        names.sort(String::compareTo);
        return names;
    }

    private void runCommand(String command) {
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.connection != null) {
            this.minecraft.player.connection.sendCommand(command);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Focus-confirm: keep the world (and the ship) visible, just show a bottom bar.
        if (this.confirmAction != null && this.focusMode) {
            int barY = this.height - 64;
            g.fill(0, barY, this.width, this.height, 0xE6090C10);
            g.fill(0, barY, this.width, barY + 1, 0xFF2A3340);
            g.drawCenteredString(this.font, this.confirmText, this.width / 2, barY + 9, 0xFF8A8A);
            g.drawCenteredString(this.font, Component.literal("The highlighted ship. This cannot be undone."),
                this.width / 2, barY + 21, 0x9AA4AE);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        this.renderBackground(g, mouseX, mouseY, partialTick);
        // Panel
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xCC101418);
        g.fill(left, top, left + PANEL_W, top + 1, 0xFF3A4655);
        g.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF3A4655);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(this.font, this.title, left + 12, top + 9, 0xE6ECF2);

        if (this.confirmAction != null) {
            g.drawCenteredString(this.font, this.confirmText, this.width / 2, top + PANEL_H / 2 - 16, 0xFF6B6B);
            g.drawCenteredString(this.font, Component.literal("This cannot be undone."), this.width / 2, top + PANEL_H / 2 - 4, 0x9AA4AE);
            return;
        }

        if (this.tab == Tab.SAVED) {
            renderSavedInfo(g);
        } else {
            renderNearbyHeader(g);
        }
    }

    private void renderSavedInfo(GuiGraphics g) {
        int rx = left + PANEL_W / 2 + 6;
        if (this.selected == null) {
            g.drawString(this.font, Component.literal("Select a ship on the left."), rx, top + 60, 0x788591);
            return;
        }
        g.drawString(this.font, Component.literal(this.selected), rx, top + 54, 0x6FB3FF);

        int[] r = previewRect();
        // Preview box backdrop + border, then the rotatable model inside.
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0xFF0B0F13);
        g.renderOutline(r[0], r[1], r[2], r[3], 0xFF2A3340);
        if (this.selectedPreview != null) {
            AeroPreviewRenderer.render(g, r[0], r[1], r[2], r[3], this.selectedPreview,
                this.previewYaw, this.previewPitch, this.previewZoom);
        } else {
            g.drawCenteredString(this.font, Component.literal("(no preview)"), r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, 0x5A6570);
        }
        g.drawString(this.font, Component.literal("drag to rotate · scroll to zoom"), r[0], r[1] + r[3] + 3, 0x5A6570);

        AeroPreview.Info info = this.selectedInfo;
        int sy = r[1] + r[3] + 16;
        if (info != null && info.ok()) {
            g.drawString(this.font, Component.literal("Sub-levels " + info.subLevels() + "   Blocks " + info.blocks()), rx, sy, 0xC8D0D8);
            g.drawString(this.font, Component.literal("Size " + info.sizeString()), rx, sy + 12, 0xC8D0D8);
        }
    }

    private void renderNearbyHeader(GuiGraphics g) {
        int x = left + 12;
        g.drawString(this.font, Component.literal("Range: " + range + " blocks"), x + 130, top + 58, 0xC8D0D8);
        List<Nearby> nearby = nearbyShips();
        if (nearby.isEmpty()) {
            g.drawString(this.font, Component.literal("No physical structures loaded nearby."), x, top + 84, 0x788591);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
