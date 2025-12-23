package gg.itzkatze.thehypixelrecreationmod.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

public class ItemStackUtils {

    public static String getPlayerHeadTexture(ItemStack stack) {
        String textureID = "";

        if (!stack.is(Items.PLAYER_HEAD)) {
            return textureID;
        }

        GameProfile profile = stack.get(DataComponents.PROFILE).partialProfile();

        Property textureProp = profile.properties()
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
    public static List<Component> getLore(ItemStack stack) {
        ItemLore loreComponent = stack.get(DataComponents.LORE);
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
        List<Component> loreLines = getLore(stack);
        List<String> result = new ArrayList<>();
        for (Component line : loreLines) {
            result.add(StringUtility.toLegacyString(line));
        }
        return result;
    }

    public static String getDisplayNameLegacy(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            return StringUtility.toLegacyString(customName);
        }
        // fallback
        return stack.getHoverName().getString();
    }

    public static String toMinestomMaterial(ItemStack stack) {
        String id = stack.getItemHolder().unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("stone");
        return id.toUpperCase();
    }
}