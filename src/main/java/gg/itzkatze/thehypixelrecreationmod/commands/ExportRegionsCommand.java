package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import gg.itzkatze.thehypixelrecreationmod.features.region.RegionExporter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ExportRegionsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("exportregions")
                            .executes(context -> {
                                try {
                                    RegionExporter.exportWithTimestamp();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§aRegions exported successfully!"),
                                            false
                                    );
                                    return 1;
                                } catch (Exception e) {
                                    context.getSource().sendFailure(
                                            Component.literal("§cFailed to export regions: " + e.getMessage())
                                    );
                                    return 0;
                                }
                            })
                            .then(Commands.literal("summary")
                                    .executes(context -> {
                                        String summary = RegionExporter.getExportSummary();
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("§e" + summary),
                                                false
                                        );
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("to")
                                    .then(Commands.argument("filename", StringArgumentType.string())
                                            .executes(context -> {
                                                String filename = StringArgumentType.getString(context, "filename");
                                                try {
                                                    RegionExporter.exportToFile(filename);
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("§aRegions exported to: " + filename),
                                                            false
                                                    );
                                                    return 1;
                                                } catch (Exception e) {
                                                    context.getSource().sendFailure(
                                                            Component.literal("§cExport failed: " + e.getMessage())
                                                    );
                                                    return 0;
                                                }
                                            })
                                    )
                            )
            );
        });
    }
}