package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import gg.itzkatze.thehypixelrecreationmod.features.region.RegionTracker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class AutoFillCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("regionfill")
                    .executes(context -> {
                        RegionTracker.toggleAutoFill();
                        String status = RegionTracker.isAutoFillEnabled() ? "§aENABLED" : "§cDISABLED";
                        context.getSource().sendFeedback(
                                Component.literal("§7Auto-fill " + status)
                        );
                        return 1;
                    })
                    .then(ClientCommandManager.literal("enable")
                            .executes(context -> {
                                RegionTracker.setAutoFillEnabled(true);
                                context.getSource().sendFeedback(
                                        Component.literal("§aAuto-fill ENABLED")
                                );
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("disable")
                            .executes(context -> {
                                RegionTracker.setAutoFillEnabled(false);
                                context.getSource().sendFeedback(
                                        Component.literal("§cAuto-fill DISABLED")
                                );
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("fillnow")
                            .executes(context -> {
                                // Fill all existing regions
                                int totalFilled = 0;
                                for (String region : RegionTracker.getRegionStatistics().keySet()) {
                                    RegionTracker.fillEnclosedAreaManually(region);
                                    totalFilled++;
                                }
                                context.getSource().sendFeedback(
                                        Component.literal("§aTriggered fill for " + totalFilled + " regions")
                                );
                                return 1;
                            })
                            .then(ClientCommandManager.argument("region", StringArgumentType.string())
                                    .executes(context -> {
                                        String region = StringArgumentType.getString(context, "region");
                                        RegionTracker.fillEnclosedAreaManually(region.toUpperCase());
                                        context.getSource().sendFeedback(
                                                Component.literal("§aTriggered fill for region: " + region)
                                        );
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("status")
                            .executes(context -> {
                                boolean autoFill = RegionTracker.isAutoFillEnabled();
                                String status = autoFill ? "§aENABLED" : "§cDISABLED";
                                int totalBlocks = RegionTracker.getTotalBlocks();
                                int regions = RegionTracker.getRegionStatistics().size();

                                context.getSource().sendFeedback(
                                        Component.literal("§7Auto-fill: " + status)
                                );
                                context.getSource().sendFeedback(
                                        Component.literal("§7Stats: §e" + totalBlocks + " §7blocks in §e" + regions + " §7regions")
                                );
                                return 1;
                            })
                    )
            );
        });
    }
}