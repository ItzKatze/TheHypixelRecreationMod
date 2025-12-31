package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public class CopyMapTextureCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("copymaptexture")
                            .executes(ctx -> executeSingle(ctx.getSource().getClient()))
                            .then(ClientCommandManager.argument("radius", DoubleArgumentType.doubleArg(0))
                                    .executes(context -> executeAll(context.getSource().getClient(),
                                            (int) DoubleArgumentType.getDouble(context, "radius")))
            ));
        });
    }

    private static ItemFrame getTargetedItemFrame(Player player) {
        double reachDistance = 5.0;
        Vec3 eyePosition = player.getEyePosition(1.0f);
        Vec3 lookVector = player.getLookAngle();
        Vec3 reachVector = eyePosition.add(lookVector.scale(reachDistance));

        AABB searchBox = player.getBoundingBox().expandTowards(lookVector.scale(reachDistance)).inflate(1.0);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                eyePosition,
                reachVector,
                searchBox,
                entity -> entity instanceof ItemFrame,
                reachDistance * reachDistance
        );

        if (hitResult != null && hitResult.getEntity() instanceof ItemFrame itemFrame) {
            return itemFrame;
        }
        return null;
    }

    private static int executeAll(Minecraft client, int radius) {
        Player player = client.player;

        if (player == null || client.level == null) {
            ChatUtils.error("Player or level is null.");
            return 1;
        }

        ItemFrame anchor = getTargetedItemFrame(player);
        if (anchor == null) {
            ChatUtils.warn("You must be looking at an item frame.");
            return 1;
        }

        // Get facing vectors from player POV
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();

        // Collect nearby item frames on same plane
        AABB box = anchor.getBoundingBox().inflate(radius);
        var frames = client.level.getEntitiesOfClass(ItemFrame.class, box, f ->
                f.getDirection() == anchor.getDirection()
        );

        record Entry(ItemFrame frame, double x, double y) {}

        var entries = frames.stream().map(frame -> {
            Vec3 delta = frame.position().subtract(anchor.position());
            double sx = delta.dot(right); // left → right
            double sy = delta.dot(up);    // bottom → top
            return new Entry(frame, sx, sy);
        }).toList();

        // Sort: top → bottom, left → right
        var sorted = entries.stream()
                .sorted((a, b) -> {
                    int y = Double.compare(b.y, a.y);
                    if (y != 0) return y;
                    return Double.compare(a.x, b.x);
                })
                .toList();

        StringBuilder out = new StringBuilder();

        for (Entry e : sorted) {
            ItemStack stack = e.frame.getItem();
            if (!(stack.getItem() instanceof MapItem)) continue;

            MapId id = stack.get(net.minecraft.core.component.DataComponents.MAP_ID);
            if (id == null) continue;

            MapItemSavedData data = client.level.getMapData(id);
            if (data == null || data.colors.length != 128 * 128) continue;

            try {
                String compressed = compressAndEncode(data.colors);
                out.append("\"").append(compressed).append("\",\n");
            } catch (IOException ignored) {}
        }

        if (out.isEmpty()) {
            ChatUtils.error("No map data found.");
            return 1;
        }

        // Remove trailing comma
        out.setLength(out.length() - 2);

        client.keyboardHandler.setClipboard(out.toString());
        ChatUtils.message("Copied " + sorted.size() + " map tiles to clipboard.");

        return 1;
    }


    private static int executeSingle(Minecraft client) {
        Player player = client.player;

        if (player == null || client.level == null) {
            ChatUtils.error("Player or level is null.");
            return 1;
        }

        ItemFrame itemFrame = getTargetedItemFrame(player);
        if (itemFrame == null) {
            ChatUtils.warn("You must be looking at an item frame.");
            return 1;
        }

        ItemStack itemStack = itemFrame.getItem();
        if (!(itemStack.getItem() instanceof MapItem)) {
            ChatUtils.warn("The item frame does not contain a map.");
            return 1;
        }

        MapId mapId = itemStack.get(net.minecraft.core.component.DataComponents.MAP_ID);
        if (mapId == null) {
            ChatUtils.error("Could not get map ID from the map item.");
            return 1;
        }

        MapItemSavedData mapData = client.level.getMapData(mapId);
        if (mapData == null) {
            ChatUtils.error("Could not retrieve map data.");
            return 1;
        }

        byte[] colors = mapData.colors;
        if (colors.length != 128 * 128) {
            ChatUtils.error("Invalid map data.");
            return 1;
        }

        String compressedBase64;
        try {
            compressedBase64 = compressAndEncode(colors);
        } catch (IOException e) {
            ChatUtils.error("Failed to compress map data: " + e.getMessage());
            return 1;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(compressedBase64).append("\"");

        String data = sb.toString();
        client.keyboardHandler.setClipboard(data);

        ChatUtils.message("Map data copied to clipboard! (Compressed: " + compressedBase64.length() + " chars)");
        ChatUtils.log("Map ID: " + mapId.id() + ", Scale: " + mapData.scale + ", Locked: " + mapData.locked);

        return 1;
    }

    private static String compressAndEncode(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
