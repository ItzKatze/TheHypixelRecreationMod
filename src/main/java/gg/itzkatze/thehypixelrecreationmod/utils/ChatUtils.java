package gg.itzkatze.thehypixelrecreationmod.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class ChatUtils {

    public static void sendLine() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        client.player.sendMessage(Text.literal("------------------------------------")
                .setStyle(Style.EMPTY
                        .withBold(true)
                        .withColor(TextColor.fromFormatting(Formatting.GOLD))
                ), false
        );
    }

    public static void message(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        client.player.sendMessage(Text.literal(message), false);
    }

    public static void log(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        client.player.sendMessage(Text.literal(message)
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromFormatting(Formatting.GRAY))
                ), false
        );
    }

    public static void error(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        client.player.sendMessage(Text.literal(message)
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromFormatting(Formatting.RED))
                ), false
        );
    }

    public static void warn(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        client.player.sendMessage(Text.literal(message)
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromFormatting(Formatting.YELLOW))
                ), false
        );
    }
}
