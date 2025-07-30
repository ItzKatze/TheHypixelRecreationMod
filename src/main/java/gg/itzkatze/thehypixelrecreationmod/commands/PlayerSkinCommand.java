package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

public class PlayerSkinCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getSkins")
                    .requires(source -> source.hasPermissionLevel(0))
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        PlayerEntity sender = client.player;
                        if (sender == null || client.world == null) return 1;

                        Vec3d pos = sender.getPos();

                        List<Entity> nearbyEntities = client.world.getEntitiesByClass(
                                Entity.class,
                                new Box(pos.add(-2, -2, -2), pos.add(2, 2, 2)),
                                entity -> entity.getType() == EntityType.PLAYER
                        );

                        if (nearbyEntities.isEmpty()) {
                            sender.sendMessage(Text.literal("No players found nearby."), false);
                            return 1;
                        }

                        for (Entity entity : nearbyEntities) {
                            if (entity instanceof PlayerEntity player) {
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

                                    Text line = Text.literal("------------------------------------")
                                            .setStyle(Style.EMPTY
                                                    .withBold(true)
                                                    .withColor(TextColor.fromFormatting(Formatting.GOLD))
                                            );

                                    sender.sendMessage(line, false);
                                    sender.sendMessage(Text.literal("Skin data for ").append(name), false);
                                    sender.sendMessage(textureMessage, false);
                                    sender.sendMessage(signatureMessage, false);
                                    sender.sendMessage(line, false);
                                } else {
                                    sender.sendMessage(Text.literal("No skin data for " + name), false);
                                }
                            }
                        }

                        return 1;
                    }));
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