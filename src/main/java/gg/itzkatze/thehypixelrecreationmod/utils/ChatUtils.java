package gg.itzkatze.thehypixelrecreationmod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class ChatUtils {

    public static void sendLine() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        mc.player.sendSystemMessage(
                Component.literal("------------------------------------")
                        .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(0xFFD700)))
        );
    }

    public static void message(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.sendSystemMessage(Component.literal(message));
    }

    public static void log(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.sendSystemMessage(
                Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080)))
        );
    }

    public static void error(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.sendSystemMessage(
                Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)))
        );
    }

    public static void warn(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.sendSystemMessage(
                Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00)))
        );
    }
}
