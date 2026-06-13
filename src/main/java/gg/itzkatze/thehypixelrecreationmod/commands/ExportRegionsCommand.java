package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import gg.itzkatze.thehypixelrecreationmod.features.region.RegionExporter;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ExportRegionsCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("exportregions")
                    .executes(context -> {
                        try {
                            RegionExporter.exportWithTimestamp();
                            context.getSource().sendFeedback(Component.literal("§aRegions exported successfully!"));
                            return -1;
                        } catch (Exception e) {
                            context.getSource().sendFeedback(Component.literal("§cFailed to export regions: " + e.getMessage()));
                            return 0;
                        }
                    })
                    .then(ClientCommands.literal("summary")
                        .executes(context -> {
                            String summary = RegionExporter.getExportSummary();
                            context.getSource().sendFeedback(Component.literal("§e" + summary));
                            return -1;
                        })
                    )
                    .then(ClientCommands.literal("to")
                        .then(ClientCommands.argument("filename", StringArgumentType.string()))
                        .executes(context -> {
                            String filename = StringArgumentType.getString(context, "filename");
                            try {
                                RegionExporter.exportToFile(filename);
                                context.getSource().sendFeedback(Component.literal("§aRegions exported to: " + filename));
                                return 1;
                            } catch (Exception e) {
                                context.getSource().sendFeedback(Component.literal("§cExport failed: " + e.getMessage()));
                                return 0;
                            }
                        })
                    )
            );
        });
    }
}