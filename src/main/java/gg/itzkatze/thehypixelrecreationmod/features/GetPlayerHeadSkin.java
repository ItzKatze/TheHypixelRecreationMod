package gg.itzkatze.thehypixelrecreationmod.features;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.GUIUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Base64;

public class GetPlayerHeadSkin implements ClientModInitializer {
    private static KeyBinding checkSkinKey;

    @Override
    public void onInitializeClient() {
        checkSkinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.copyplayerheadskin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.thehypixelrecreationmod"
        ));

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (checkSkinKey.matchesKey(key, scancode)) {
                    checkHoveredItemForSkin(client);
                    return false;
                }
                return true;
            });
        });
    }

    private void checkHoveredItemForSkin(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack hoveredStack = GUIUtils.getHoveredItem(client);
        ChatUtils.sendMessage(client, "Hovered: " + hoveredStack.getItem().toString());

        if (!hoveredStack.isOf(Items.PLAYER_HEAD)) {
            ChatUtils.sendMessage(client, "Hovered item is not a player head.");
            return;
        }

        ProfileComponent profileComp = hoveredStack.get(DataComponentTypes.PROFILE);
        if (profileComp == null) {
            ChatUtils.sendMessage(client, "No player profile found.");
            return;
        }

        GameProfile profile = profileComp.gameProfile();
        if (profile == null) {
            ChatUtils.sendMessage(client, "Profile is empty.");
            return;
        }

        Property textureProp = profile.getProperties()
                .get("textures")
                .stream()
                .findFirst()
                .orElse(null);

        if (textureProp == null || textureProp.value().isEmpty()) {
            ChatUtils.sendMessage(client, "No texture property found.");
            return;
        }

        try {
            String json = new String(Base64.getDecoder().decode(textureProp.value()));
            int start = json.indexOf("http");
            String url = (start >= 0) ? json.substring(start, json.indexOf('"', start)) : "<no URL>";

            if (url.equals("<no URL>")) {
                ChatUtils.sendMessage(client, "No URL found in decoded texture.");
                return;
            }

            String[] parts = url.split("/");
            String textureId = parts[parts.length - 1];

            client.keyboard.setClipboard(textureId);

            client.player.sendMessage(Text.literal("Copied texture ID: ")
                    .setStyle(Style.EMPTY
                            .withColor(TextColor.fromFormatting(Formatting.AQUA))
                    ), false);
            client.player.sendMessage(Text.literal(textureId)
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.CopyToClipboard(textureId))
                    ), false);
        } catch (Exception ex) {
            ChatUtils.sendMessage(client, "Error decoding texture: " + ex.getMessage());
        }
    }
}