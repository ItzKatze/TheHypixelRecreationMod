package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

public class PlayerSkinCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getSkins")
                    .then(ClientCommandManager.argument("radius", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {
                                double radius = DoubleArgumentType.getDouble(context, "radius");
                                MinecraftClient client = MinecraftClient.getInstance();
                                PlayerEntity sender = client.player;

                                if (sender == null || client.world == null) return 1;

                                List<PlayerEntity> nearbyPlayers = client.world.getEntitiesByClass(
                                        PlayerEntity.class,
                                        sender.getBoundingBox().expand(radius),
                                        entity -> entity.getType() == EntityType.PLAYER
                                );

                                if (nearbyPlayers.isEmpty()) {
                                    ChatUtils.warn("No players found nearby.");
                                    return 1;
                                }

                                for (PlayerEntity player : nearbyPlayers) {
                                    if (player.getName().equals(sender.getName())) return 1;
                                    GameProfile profile = player.getGameProfile();
                                    Collection<Property> props = profile.getProperties().get("textures");
                                    Text name = Text.literal(getOverheadName(player)).setStyle(Style.EMPTY.withColor(Formatting.RED));

                                    if (!props.isEmpty()) {
                                        Property prop = props.iterator().next();
                                        String texture = prop.value();
                                        String signature = prop.signature();

                                        Text textureMessage = Text.literal("Copy Texture (click)")
                                                .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent.CopyToClipboard(texture))
                                                        .withColor(TextColor.fromFormatting(Formatting.GREEN))
                                                );

                                        Text signatureMessage = Text.literal("Copy Signature (click)")
                                                .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent.CopyToClipboard(signature))
                                                        .withColor(TextColor.fromFormatting(Formatting.AQUA))
                                                );

                                        ChatUtils.sendLine();
                                        sender.sendMessage(Text.literal("Skin data for ").append(name), false);
                                        sender.sendMessage(textureMessage, false);
                                        sender.sendMessage(signatureMessage, false);
                                        ChatUtils.sendLine();
                                    } else {
                                        sender.sendMessage(Text.literal("No skin data for " + name), false);
                                    }
                                }

                            return 1;
                            })
                    )
            );
        });
    }

    private static String getOverheadName(Entity npc) {
        World world = npc.getWorld();
        if (!(world instanceof ClientWorld clientWorld)) return "Unknown";

        Box boxAbove = npc.getBoundingBox().expand(0.5, 2.5, 0.5);

        for (Entity entity : clientWorld.getEntities()) {
            if (entity == npc) continue;

            if (entity.getBoundingBox().intersects(boxAbove)) {
                if ((entity instanceof DisplayEntity.TextDisplayEntity || entity instanceof ArmorStandEntity)
                        && entity.hasCustomName()) {
                    String raw = entity.getCustomName().getString().replaceAll("§.", "").trim();
                    if (!raw.isEmpty()) {
                        return raw;
                    }
                }
            }
        }

        return "Unknown";
    }
}