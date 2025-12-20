package gg.itzkatze.thehypixelrecreationmod.features;

import gg.itzkatze.thehypixelrecreationmod.utils.GUIUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.ItemStackUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;

public class GetPlayerHeadSkin {
    public static void checkHoveredItemForSkin(Minecraft client) {
        if (client.player == null) return;

        ItemStack hoveredStack = GUIUtils.getHoveredItem(client);
        String textureID = ItemStackUtils.getPlayerHeadTexture(hoveredStack);

        if (textureID.isEmpty()) {
            Component errorMessage = Component.literal("No texture found on hovered item!")
                    .withStyle(ChatFormatting.RED);
            client.player.displayClientMessage(errorMessage, false);
            return;
        }

        // Copy to clipboard
        client.keyboardHandler.setClipboard(textureID);

        // Create main message
        Component mainMessage = Component.literal("✓ Copied Texture-ID: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(textureID)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.CopyToClipboard(textureID))
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy again")))
                        )
                );

        client.player.displayClientMessage(mainMessage, false);

        // Optional: Show a smaller hint
        Component hint = Component.literal("(Texture ID has been copied to clipboard)")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        client.player.displayClientMessage(hint, false);
    }
}