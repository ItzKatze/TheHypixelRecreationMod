package gg.itzkatze.thehypixelrecreationmod.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

public class ItemStackUtils {

    public static String getPlayerHeadTexture(ItemStack stack) {
        String textureID = "";

        if (!stack.isOf(Items.PLAYER_HEAD)) {
            ChatUtils.message("Hovered item is not a player head.");
            return textureID;
        }

        ProfileComponent profileComp = stack.get(DataComponentTypes.PROFILE);
        if (profileComp == null) {
            ChatUtils.message("No player profile found.");
            return textureID;
        }

        GameProfile profile = profileComp.gameProfile();
        if (profile == null) {
            ChatUtils.message("Profile is empty.");
            return textureID;
        }

        Property textureProp = profile.getProperties()
                .get("textures")
                .stream()
                .findFirst()
                .orElse(null);

        if (textureProp == null || textureProp.value().isEmpty()) {
            ChatUtils.message("No texture property found.");
            return textureID;
        }

        try {
            String json = new String(Base64.getDecoder().decode(textureProp.value()));
            int start = json.indexOf("http");
            String url = (start >= 0) ? json.substring(start, json.indexOf('"', start)) : "<no URL>";

            if (url.equals("<no URL>")) {
                ChatUtils.message("No URL found in decoded texture.");
                return textureID;
            }

            String[] parts = url.split("/");
            textureID = parts[parts.length - 1];

            return textureID;

        } catch (Exception ex) {
            ChatUtils.message("Error decoding texture: " + ex.getMessage());
        }
        return textureID;
    }

    /**
     * Gets the lore from an ItemStack as a list of Text components.
     * Returns an empty list if no lore is present.
     */
    public static List<Text> getLore(ItemStack stack) {
        LoreComponent loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return new ArrayList<>();
        }
        return loreComponent.lines();
    }

    /**
     * Gets the lore from an ItemStack as a list of plain strings.
     * Returns an empty list if no lore is present.
     */
    public static List<String> getLoreAsStrings(ItemStack stack) {
        List<Text> loreLines = getLore(stack);
        List<String> result = new ArrayList<>();
        for (Text line : loreLines) {
            result.add(StringUtilities.toLegacyString(line));
        }
        return result;
    }
}