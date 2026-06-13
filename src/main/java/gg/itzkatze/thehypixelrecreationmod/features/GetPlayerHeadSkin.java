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
        Component M = Component.literal("✓ Copied Texture-ID: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(textureID)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.CopyToClipboard(textureID))
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy again")))
                        )
                );
        client.player.sendSystemMessage(M);
        var prop = ItemStackUtils.getPlayerHeadTextureProperty(hoveredStack);
        if (prop == null) {
            Component errorMessage = Component.literal("No texture found on hovered item!")
                    .withStyle(ChatFormatting.RED);
            client.player.sendSystemMessage(errorMessage);
            return;
        }

        // Build the payload the user can directly copy/paste.
        // Includes signature if present.
        String payload = ItemStackUtils.buildTexturesPropertiesJson(prop, true);

        // Copy to clipboard
        client.keyboardHandler.setClipboard(payload);

        // Create main message
        Component mainMessage = Component.literal("✓ Copied head textures JSON: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal("[click to copy]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.CopyToClipboard(payload))
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy again")))
                        )
                );

        client.player.sendSystemMessage(mainMessage);

        // Also print the actual JSON in chat (may be long, but that's what you asked for)
        client.player.sendSystemMessage(
                Component.literal(payload)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.CopyToClipboard(payload))
                                .withColor(ChatFormatting.GRAY)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
                        )
        );
    }
}