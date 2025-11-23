package gg.itzkatze.thehypixelrecreationmod.features;

import gg.itzkatze.thehypixelrecreationmod.utils.GUIUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.ItemStackUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class GetPlayerHeadSkin {
    public static void checkHoveredItemForSkin(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack hoveredStack = GUIUtils.getHoveredItem(client);
        String textureID = ItemStackUtils.getPlayerHeadTexture(hoveredStack);

        client.keyboard.setClipboard(textureID);

        client.player.sendMessage(Text.literal("Copied Texture-ID: ")
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromFormatting(Formatting.AQUA))
                ), false);
        client.player.sendMessage(Text.literal(textureID)
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.CopyToClipboard(textureID))
                ), false);
    }
}