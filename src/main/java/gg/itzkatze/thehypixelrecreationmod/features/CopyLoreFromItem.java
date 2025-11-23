package gg.itzkatze.thehypixelrecreationmod.features;

import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.GUIUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.ItemStackUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;

public class CopyLoreFromItem {
    public static void copyLore(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack stack = GUIUtils.getHoveredItem(client);
        List<String> lore = ItemStackUtils.getLoreAsStrings(stack);

        if (lore.isEmpty()) {
            ChatUtils.warn("Lore is empty!");
            return;
        }

        String loreText = String.join("\n", lore);
        client.keyboard.setClipboard(loreText);

        client.player.sendMessage(Text.literal("Copied Lore")
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromFormatting(Formatting.AQUA))
                        .withClickEvent(new ClickEvent.CopyToClipboard(loreText))
                ), false);
    }
}