package gg.itzkatze.thehypixelrecreationmod.commands;

import gg.itzkatze.thehypixelrecreationmod.features.SpraySchemaRecorder;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;

public class SpraySchemaRecorderCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("listensprays")
                    .executes(context -> {
                        SpraySchemaRecorder.start();
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("listenspraysstop")
                    .executes(context -> {
                        if (!SpraySchemaRecorder.isListening()) {
                            ChatUtils.warn("Spray listener is not running.");
                            return 1;
                        }
                        SpraySchemaRecorder.stop();
                        return 1;
                    }));
        });
    }
}
