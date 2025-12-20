package gg.itzkatze.thehypixelrecreationmod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class ChatUtils {

    public static void sendLine() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        mc.player.displayClientMessage(
                Component.literal("------------------------------------")
                        .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(0xFFD700))),
                false
        );
    }

    public static void message(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.displayClientMessage(Component.literal(message), false);
    }

    public static void log(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.displayClientMessage(
                Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))),
                false
        );
    }

    public static void error(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.displayClientMessage(
                Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000))),
                false
        );
    }

    public static void warn(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.displayClientMessage(
                Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))),
                false
        );
    }
}
