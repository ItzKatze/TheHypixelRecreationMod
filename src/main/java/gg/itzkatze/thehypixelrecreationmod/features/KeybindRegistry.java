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

    @Override
    public void onInitializeClient() {
        checkSkinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.copyplayerheadskin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.thehypixelrecreationmod"
        ));

        //Check keybinds inside of gui's
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (KeybindRegistry.checkSkinKey.matchesKey(key, scancode)) {
                    GetPlayerHeadSkin.checkHoveredItemForSkin(client);
                    return false;
                }
                return true;
            });
        });
    }
}