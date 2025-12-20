package gg.itzkatze.thehypixelrecreationmod.features;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindRegistry implements ClientModInitializer {
    public static KeyBinding checkSkinKey;
    public static KeyBinding copyLoreKey;
    public static KeyBinding copyChatKey;
    private static boolean chatCopyPressed = false;

    @Override
    public void onInitializeClient() {
        checkSkinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.copyplayerheadskin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.thehypixelrecreationmod"
        ));

        copyLoreKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.copylore",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.thehypixelrecreationmod"
        ));

        copyChatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.copychat",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.thehypixelrecreationmod"
        ));

        //Check keybinds inside GUI's
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (KeybindRegistry.checkSkinKey.matchesKey(key, scancode)) {
                    GetPlayerHeadSkin.checkHoveredItemForSkin(client);
                    return false;
                }
                if (KeybindRegistry.copyLoreKey.matchesKey(key, scancode)) {
                    CopyLoreFromItem.copyLore(client);
                    return false;
                }
                if (KeybindRegistry.copyChatKey.matchesKey(key, scancode)) {
                    chatCopyPressed = true;
                    return false;
                }
                return true;
            });
        });
    }

    public static boolean wasChatCopyPressed() {
        if (chatCopyPressed) {
            chatCopyPressed = false;
            return true;
        }
        return false;
    }
}