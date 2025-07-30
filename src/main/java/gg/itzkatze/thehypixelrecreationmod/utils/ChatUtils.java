package gg.itzkatze.thehypixelrecreationmod.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ChatUtils {
    public static void sendMessage(MinecraftClient client, String message) {
        if (client.player == null || client.world == null) return;

        client.player.sendMessage(Text.literal(message), false);
    }
}
