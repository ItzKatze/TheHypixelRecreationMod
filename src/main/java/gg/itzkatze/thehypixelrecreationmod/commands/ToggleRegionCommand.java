package gg.itzkatze.thehypixelrecreationmod.commands;

import gg.itzkatze.thehypixelrecreationmod.features.region.RegionRenderer;
import gg.itzkatze.thehypixelrecreationmod.features.region.RegionTracker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public class ToggleRegionCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("toggleregion")
                    .executes(context -> {
                        RegionTracker.toggle();
                        RegionRenderer.setRenderEnabled(RegionTracker.isEnabled());

                        String status = RegionTracker.isEnabled() ? "§aENABLED" : "§cDISABLED";
                        context.getSource().sendFeedback(
                                Component.literal("§7Region tracking and rendering " + status)
                        );
                        return 1;
                    })
                    .then(ClientCommandManager.literal("tracking")
                            .executes(context -> {
                                RegionTracker.toggle();
                                String status = RegionTracker.isEnabled() ? "§aENABLED" : "§cDISABLED";
                                context.getSource().sendFeedback(
                                        Component.literal("§7Region tracking " + status)
                                );
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("rendering")
                            .executes(context -> {
                                RegionRenderer.toggleRender();
                                String status = RegionRenderer.isRenderEnabled() ? "§aENABLED" : "§cDISABLED";
                                context.getSource().sendFeedback(
                                        Component.literal("§7Region rendering " + status)
                                );
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("status")
                            .executes(context -> {
                                boolean tracking = RegionTracker.isEnabled();
                                boolean rendering = RegionRenderer.isRenderEnabled();

                                String trackStatus = tracking ? "§aENABLED" : "§cDISABLED";
                                String renderStatus = rendering ? "§aENABLED" : "§cDISABLED";

                                context.getSource().sendFeedback(
                                        Component.literal("§7Region Tracking: " + trackStatus + " §8| §7Rendering: " + renderStatus)
                                );

                                int totalBlocks = RegionTracker.getTotalBlocks();
                                int regions = RegionTracker.getRegionStatistics().size();

                                context.getSource().sendFeedback(
                                        Component.literal("§7Stats: §e" + totalBlocks + " §7blocks in §e" + regions + " §7regions")
                                );

                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("enable")
                            .executes(context -> {
                                RegionTracker.setEnabled(true);
                                RegionRenderer.setRenderEnabled(true);
                                context.getSource().sendFeedback(
                                        Component.literal("§aRegion tracking and rendering ENABLED")
                                );
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("disable")
                            .executes(context -> {
                                RegionTracker.setEnabled(false);
                                RegionRenderer.setRenderEnabled(false);
                                context.getSource().sendFeedback(
                                        Component.literal("§cRegion tracking and rendering DISABLED")
                                );
                                return 1;
                            })
                    )
            );
        });
    }
}