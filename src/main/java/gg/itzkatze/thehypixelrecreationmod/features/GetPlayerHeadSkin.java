package gg.itzkatze.thehypixelrecreationmod.features;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.GUIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Base64;

public class GetPlayerHeadSkin {

    public static void checkHoveredItemForSkin(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack hoveredStack = GUIUtils.getHoveredItem(client);
        ChatUtils.message("Hovered: " + hoveredStack.getItem().toString());

        if (!hoveredStack.isOf(Items.PLAYER_HEAD)) {
            ChatUtils.message("Hovered item is not a player head.");
            return;
        }

        ProfileComponent profileComp = hoveredStack.get(DataComponentTypes.PROFILE);
        if (profileComp == null) {
            ChatUtils.message("No player profile found.");
            return;
        }

        GameProfile profile = profileComp.gameProfile();
        if (profile == null) {
            ChatUtils.message("Profile is empty.");
            return;
        }

        Property textureProp = profile.getProperties()
                .get("textures")
                .stream()
                .findFirst()
                .orElse(null);

        if (textureProp == null || textureProp.value().isEmpty()) {
            ChatUtils.message("No texture property found.");
            return;
        }

        try {
            String json = new String(Base64.getDecoder().decode(textureProp.value()));
            int start = json.indexOf("http");
            String url = (start >= 0) ? json.substring(start, json.indexOf('"', start)) : "<no URL>";

            if (url.equals("<no URL>")) {
                ChatUtils.message("No URL found in decoded texture.");
                return;
            }

            String[] parts = url.split("/");
            String textureId = parts[parts.length - 1];

            client.keyboard.setClipboard(textureId);

            client.player.sendMessage(Text.literal("Copied Texture-ID: ")
                    .setStyle(Style.EMPTY
                            .withColor(TextColor.fromFormatting(Formatting.AQUA))
                    ), false);
            client.player.sendMessage(Text.literal(textureId)
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.CopyToClipboard(textureId))
                    ), false);
        } catch (Exception ex) {
            ChatUtils.message("Error decoding texture: " + ex.getMessage());
        }
    }
}