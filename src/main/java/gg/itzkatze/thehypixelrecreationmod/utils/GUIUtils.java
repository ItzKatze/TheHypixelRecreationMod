package gg.itzkatze.thehypixelrecreationmod.utils;

import gg.itzkatze.thehypixelrecreationmod.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class GUIUtils {

    public static ItemStack getHoveredItem(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return ItemStack.EMPTY;

        Slot slot = ((HandledScreenAccessor<?>) screen).getFocusedSlot();
        return (slot != null && slot.hasStack()) ? slot.getStack() : ItemStack.EMPTY;
    }
}
