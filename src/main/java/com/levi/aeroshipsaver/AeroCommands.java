package com.levi.aeroshipsaver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.enxv.aeronauticsstructuretool.SubLevelFileStore;
import com.levi.aeroshipsaver.net.AeroNetwork;
import com.mojang.brigadier.CommandDispatcher;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The {@code /aeroship} command tree. Saving and loading delegate to the toolgun's
 * {@link SubLevelFileStore}, so they operate on real Aeronautics physical structures
 * (sublevels) - not just blocks. Registered on the server dispatcher, so in singleplayer
 * the integrated server handles it and a client-only install works.
 */
@EventBusSubscriber(modid = AeroShipSaver.MODID)
public final class AeroCommands {
    /**
     * Max gap (blocks) for treating an adjacent sub-level as "connected" and capturing it
     * too - so swivel bearings, sub-contraptions and other linked structures save with the
     * ship. This is the toolgun's own default.
     */
    private static final double CONNECTED_PROXIMITY = 0.5;

    private AeroCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("aeroship")
            .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
            .then(Commands.literal("save")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> save(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("load")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(savedNames(), b))
                    .executes(ctx -> load(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(savedNames(), b))
                    .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            // Remove a live physical structure from the world (used by the Nearby-ships menu).
            .then(Commands.literal("removeid")
                .then(Commands.argument("id", UuidArgument.uuid())
                    .executes(ctx -> removeById(ctx.getSource(), UuidArgument.getUuid(ctx, "id")))))
            .then(Commands.literal("removenear")
                .then(Commands.argument("range", IntegerArgumentType.integer(1, 256))
                    .executes(ctx -> removeNear(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "range"))))));
    }

    /** Remove a single live physical structure by its sub-level id. */
    private static int removeById(CommandSourceStack source, java.util.UUID id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        if (SubLevelContainer.getContainer(level).getSubLevel(id) instanceof ServerSubLevel sl) {
            SubLevelContainer.getContainer(level).removeSubLevel(sl, SubLevelRemovalReason.REMOVED);
            source.sendSuccess(() -> Component.literal("Removed physical structure " + id.toString().substring(0, 8) + ".")
                .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        source.sendFailure(Component.literal("No physical structure with that id is loaded."));
        return 0;
    }

    /** Remove every live physical structure within {@code range} blocks of the player. */
    private static int removeNear(CommandSourceStack source, int range) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 pp = player.position();
        double r2 = (double) range * range;
        List<ServerSubLevel> targets = new ArrayList<>();
        for (ServerSubLevel sl : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            Vec3 c = center(sl);
            if (c != null && pp.distanceToSqr(c) <= r2) {
                targets.add(sl);
            }
        }
        for (ServerSubLevel sl : targets) {
            SubLevelContainer.getContainer(level).removeSubLevel(sl, SubLevelRemovalReason.REMOVED);
        }
        int removed = targets.size();
        source.sendSuccess(() -> Component.literal("Removed " + removed + " physical structure(s) within " + range + " blocks.")
            .withStyle(ChatFormatting.YELLOW), false);
        return removed;
    }

    /** The folder load() reads from: <gameDir>/.../ (toolgun's local blueprint store). */
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

    private static int save(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        ServerSubLevel ship = findShip(player, level);
        if (ship == null) {
            source.sendFailure(Component.literal("No physical ship found - look directly at your assembled ship "
                + "(or stand on it), then run /aeroship save again."));
            return 0;
        }
        try {
            // Capture by the ship's id, pulling in connected sub-levels (bearings, sub-contraptions).
            SubLevelFileStore.SavedBlueprint saved =
                SubLevelFileStore.capture(level, ship.getUniqueId(), name, CONNECTED_PROXIMITY);
            // Server-side copy: this is what /aeroship load reads back.
            SubLevelFileStore.writeClientBlueprint(saved.fileName(), saved.fileContents());
            // ...and hand it to the saving player so it also lands on THEIR machine.
            // (Under Essential/LAN the command runs on the host, so without this the file
            // would only ever exist on the host's disk.)
            AeroNetwork.sendBlueprint(player, saved.fileName(), saved.fileContents());
            int bytes = saved.fileContents().length;
            int subs = countSubLevels(saved.fileContents());
            source.sendSuccess(() -> Component.literal("Saved ship '" + SubLevelFileStore.sanitize(name)
                + "' (" + subs + " sub-levels, " + bytes + " bytes).").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Couldn't save: " + e.getMessage()));
            return 0;
        }
    }

    /** Find the physical ship the player is looking at, standing in/on, or nearest to. */
    private static ServerSubLevel findShip(ServerPlayer player, ServerLevel level) {
        // 1) Raycast along the look vector - aim at the ship from anywhere within ~48 blocks.
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        for (double d = 0.0; d <= 48.0; d += 0.5) {
            if (Sable.HELPER.getContaining(level, eye.add(view.scale(d))) instanceof ServerSubLevel ssl) {
                return ssl;
            }
        }
        // 2) The sublevel the player is currently inside / riding.
        if (Sable.HELPER.getContaining(player) instanceof ServerSubLevel ssl) {
            return ssl;
        }
        // 3) Fallback: the nearest physical structure within 64 blocks of the player.
        ServerSubLevel nearest = null;
        double best = 64.0 * 64.0;
        Vec3 pp = player.position();
        for (ServerSubLevel sl : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            Vec3 c = center(sl);
            if (c == null) {
                continue;
            }
            double d2 = pp.distanceToSqr(c);
            if (d2 < best) {
                best = d2;
                nearest = sl;
            }
        }
        return nearest;
    }

    /** How many sub-levels a saved blueprint contains (for diagnostics). */
    private static int countSubLevels(byte[] bytes) {
        try {
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
            return root.contains("sublevels", Tag.TAG_LIST) ? root.getList("sublevels", Tag.TAG_COMPOUND).size() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** World-space centre of a ship's bounding box, or null if unavailable. */
    private static Vec3 center(ServerSubLevel sl) {
        try {
            BoundingBox3i bb = new BoundingBox3i(new BoundingBox3d(sl.boundingBox()));
            return new Vec3((bb.minX() + bb.maxX()) / 2.0, (bb.minY() + bb.maxY()) / 2.0, (bb.minZ() + bb.maxZ()) / 2.0);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int load(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        if (!Files.exists(saveDir().resolve(SubLevelFileStore.sanitize(name) + SubLevelFileStore.FILE_EXTENSION))) {
            source.sendFailure(Component.literal("The server has no saved ship named '"
                + SubLevelFileStore.sanitize(name) + "'."));
            source.sendFailure(Component.literal("If it's saved on YOUR machine (another world, or you're a guest "
                + "on a hosted world), press G and use the menu's Load button - that sends your copy to the server."));
            return 0;
        }
        try {
            int before = SubLevelContainer.getContainer(level).getAllSubLevels().size();
            SubLevelFileStore.load(level, player.blockPosition(), Direction.UP, name);
            int after = SubLevelContainer.getContainer(level).getAllSubLevels().size();
            int added = after - before;
            source.sendSuccess(() -> Component.literal("Loaded ship '" + SubLevelFileStore.sanitize(name)
                + "' (+" + added + " physical structures) at your position.").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Couldn't load: " + e.getMessage()));
            return 0;
        }
    }

    private static int list(CommandSourceStack source) {
        List<String> ships = savedNames();
        if (ships.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No saved ships yet. Stand on an assembled ship and /aeroship save <name>.")
                .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Saved ships (" + ships.size() + "): " + String.join(", ", ships))
            .withStyle(ChatFormatting.AQUA), false);
        return ships.size();
    }

    private static int delete(CommandSourceStack source, String name) {
        Path file = saveDir().resolve(SubLevelFileStore.sanitize(name) + SubLevelFileStore.FILE_EXTENSION);
        try {
            if (Files.deleteIfExists(file)) {
                source.sendSuccess(() -> Component.literal("Deleted ship '" + SubLevelFileStore.sanitize(name) + "'.")
                    .withStyle(ChatFormatting.YELLOW), false);
                return 1;
            }
        } catch (IOException e) {
            source.sendFailure(Component.literal("Couldn't delete: " + e.getMessage()));
            return 0;
        }
        source.sendFailure(Component.literal("No saved ship named '" + SubLevelFileStore.sanitize(name) + "'."));
        return 0;
    }
}
