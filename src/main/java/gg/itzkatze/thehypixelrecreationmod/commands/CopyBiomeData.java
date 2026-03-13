package gg.itzkatze.thehypixelrecreationmod.commands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class CopyBiomeData {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("copybiomedata")
                    .executes(context -> {
                        // copy basic biome data for where the player currently is
                        try {
                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft == null || minecraft.player == null || minecraft.level == null) {
                                context.getSource().sendFeedback(Component.literal("§cClient not ready or player/world missing."));
                                return 1;
                            }

                            BlockPos pos = minecraft.player.blockPosition();
                            var holder = minecraft.level.getBiome(pos);
                            Biome biome = holder.value();

                            // Build a safe summary without relying on mapping-specific methods or private fields
                            StringBuilder sb = new StringBuilder();
                            sb.append('{');
                            sb.append("\n  \"holder\": \"").append(holder.toString()).append('\"');
                            sb.append(',').append("\n  \"biomeToString\": \"").append(biome == null ? "null" : biome.toString()).append('\"');

                            try {
                                var effects = biome == null ? null : biome.getSpecialEffects();
                                sb.append(',').append("\n  \"effects\": \"").append(effects == null ? "none" : effects.toString()).append('\"');
                            } catch (Throwable ignored) {
                                // Some mappings may have different method names; ignore and continue
                            }

                            sb.append('\n').append('}');

                            // Copy to clipboard
                            try {
                                Minecraft client = Minecraft.getInstance();
                                KeyboardHandler keyboard = client.keyboardHandler;
                                keyboard.setClipboard(sb.toString());
                                context.getSource().sendFeedback(Component.literal("§aBiome data copied to clipboard: " + holder.toString()));
                                return 0;
                            } catch (Exception e) {
                                context.getSource().sendFeedback(Component.literal("§cFailed to copy to clipboard: " + e.getMessage()));
                                return 1;
                            }

                        } catch (Exception e) {
                            context.getSource().sendFeedback(Component.literal("§cFailed to copy biome data: " + e.getMessage()));
                        }
                        return 1;
                    })
            );
        });
    }
}