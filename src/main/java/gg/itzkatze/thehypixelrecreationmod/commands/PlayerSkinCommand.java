package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.List;

public class PlayerSkinCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("getskins")
                            .then(ClientCommandManager.argument("radius", DoubleArgumentType.doubleArg(0))
                                    .executes(context -> {
                                        double radius = DoubleArgumentType.getDouble(context, "radius");
                                        Minecraft client = Minecraft.getInstance();
                                        Player sender = client.player;

                                        if (sender == null || client.level == null) return 1;

                                        List<Player> nearby = client.level.getEntitiesOfClass(
                                                Player.class,
                                                sender.getBoundingBox().inflate(radius),
                                                ent -> ent.getType() == EntityType.PLAYER
                                        );

                                        if (nearby.isEmpty()) {
                                            ChatUtils.warn("No players found nearby.");
                                            return 1;
                                        }

                                        for (Player player : nearby) {
                                            if (player.getName().getString().equals(sender.getName().getString())) continue;

                                            GameProfile profile = player.getGameProfile();
                                            Collection<Property> props = profile.properties().get("textures");

                                            Component playerName = Component.literal(getOverheadName(player))
                                                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)));

                                            if (!props.isEmpty()) {
                                                Property texProp = props.iterator().next();
                                                String texture = texProp.value();
                                                String signature = texProp.signature();

                                                String hologram = getOverheadName(player).replace("\"", "\\\"");
                                                String posX = String.format("%.3f", player.getX());
                                                String posY = String.format("%.3f", player.getY());
                                                String posZ = String.format("%.3f", player.getZ());
                                                int yaw = Math.round(player.getYRot());
                                                int pitch = Math.round(player.getXRot());

                                                String safeSignature = signature == null ? "" : signature.replace("\"", "\\\"");
                                                String safeTexture = texture == null ? "" : texture.replace("\"", "\\\"");

                                                String npcParams = "super(new NPCParameters() {\n" +
                                                        "            @Override\n" +
                                                        "            public String[] holograms(HypixelPlayer player) {\n" +
                                                        "                return new String[]{\"" + hologram + "\"};\n" +
                                                        "            }\n\n" +
                                                        "            @Override\n" +
                                                        "            public String signature(HypixelPlayer player) {\n" +
                                                        "                return \"" + safeSignature + "\";\n" +
                                                        "            }\n\n" +
                                                        "            @Override\n" +
                                                        "            public String texture(HypixelPlayer player) {\n" +
                                                        "                return \"" + safeTexture + "\";\n" +
                                                        "            }\n\n" +
                                                        "            @Override\n" +
                                                        "            public Pos position(HypixelPlayer player) {\n" +
                                                        "                return new Pos(" + posX + ", " + posY + ", " + posZ + ", " + yaw + ", " + pitch + ");\n" +
                                                        "            }\n\n" +
                                                        "            @Override\n" +
                                                        "            public boolean looking() {\n" +
                                                        "                return true;\n" +
                                                        "            }\n" +
                                                        "        });";

                                                Component textureMsg = Component.literal("Copy Texture (click)")
                                                        .setStyle(Style.EMPTY
                                                                .withClickEvent(new ClickEvent.CopyToClipboard(texture))
                                                                .withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN))
                                                        );

                                                Component sigMsg = Component.literal("Copy Signature (click)")
                                                        .setStyle(Style.EMPTY
                                                                .withClickEvent(new ClickEvent.CopyToClipboard(signature))
                                                                .withColor(TextColor.fromLegacyFormat(ChatFormatting.AQUA))
                                                        );

                                                Component npcParaMsg = Component.literal("Copy premade NPCParameters (click)")
                                                        .setStyle(Style.EMPTY
                                                                .withClickEvent(new ClickEvent.CopyToClipboard(npcParams))
                                                                .withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW))
                                                        );

                                                ChatUtils.sendLine();
                                                sender.displayClientMessage(
                                                        Component.literal("Skin data for ").append(playerName),
                                                        false
                                                );
                                                sender.displayClientMessage(textureMsg, false);
                                                ChatUtils.sendLine();
                                                sender.displayClientMessage(sigMsg, false);
                                                ChatUtils.sendLine();
                                                sender.displayClientMessage(npcParaMsg, false);
                                                ChatUtils.sendLine();
                                            } else {
                                                sender.displayClientMessage(
                                                        Component.literal("No skin data for " + getOverheadName(player)),
                                                        false
                                                );
                                            }
                                        }

                                        return 1;
                                    })
                            )
            );
        });
    }

    private static String getOverheadName(Entity npc) {
        Level level = npc.level();
        if (!(level instanceof ClientLevel clientLevel)) return "Unknown";

        AABB boxAbove = npc.getBoundingBox().inflate(0.5, 2.5, 0.5);

        for (Entity e : clientLevel.entitiesForRendering()) {
            if (e == npc) continue;

            if (e.getBoundingBox().intersects(boxAbove)) {
                if ((e instanceof Display.TextDisplay || e instanceof ArmorStand)
                        && e.hasCustomName()) {
                    String raw = e.getCustomName().getString().replaceAll("§.", "").trim();
                    if (!raw.isEmpty()) return raw;
                }
            }
        }

        return "Unknown";
    }
}