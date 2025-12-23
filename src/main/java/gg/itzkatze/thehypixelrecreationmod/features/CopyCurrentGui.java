package gg.itzkatze.thehypixelrecreationmod.features;

import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.ItemStackUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.StringUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class CopyCurrentGui {

    public static void copyCurrentGui(Minecraft client) {
        if (client.player == null) return;

        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) {
            ChatUtils.warn("Not in a container GUI!");
            return;
        }

        String guiTitle = StringUtility.toLegacyString(screen.getTitle());
        String cleanTitle = StringUtility.stripColor(guiTitle);

        List<Slot> containerSlots = getContainerSlots(screen);
        String inventoryType = getInventoryType(containerSlots.size());

        StringBuilder code = new StringBuilder();
        String className = "GUI" + toPascalCase(cleanTitle);

        code.append("public class ").append(className).append(" extends HypixelInventoryGUI {\n\n");
        code.append("    public ").append(className).append("() {\n");
        code.append("        super(\"").append(escapeJavaString(guiTitle)).append("\", InventoryType.").append(inventoryType).append(");\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public void onOpen(InventoryGUIOpenEvent e) {\n");

        Integer closeItemSlot = null;
        List<String> itemEntries = new ArrayList<>();

        for (Slot slot : containerSlots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            int slotIndex = slot.index;

            if (stack.is(Items.BLACK_STAINED_GLASS)) {
                continue;
            }

            if (stack.is(Items.BARRIER)) {
                closeItemSlot = slotIndex;
                continue;
            }

            String itemEntry = generateGUIItem(slotIndex, stack);
            itemEntries.add(itemEntry);
        }

        for (String entry : itemEntries) {
            code.append(entry);
        }

        if (closeItemSlot != null) {
            code.append("        set(GUIClickableItem.getCloseItem(").append(closeItemSlot).append("));\n");
        }

        code.append("        updateItemStacks(getInventory(), getPlayer());\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public boolean allowHotkeying() {\n");
        code.append("        return false;\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public void onBottomClick(InventoryPreClickEvent e) {\n");
        code.append("        e.setCancelled(true);\n");
        code.append("    }\n");
        code.append("}\n");

        String generatedCode = code.toString();
        client.keyboardHandler.setClipboard(generatedCode);

        Component message = Component.literal("Copied GUI Code for: " + cleanTitle)
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent.CopyToClipboard(generatedCode))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to copy again\n\n" + className + ".java")
                        ))
                );

        client.player.displayClientMessage(message, false);
    }

    private static List<Slot> getContainerSlots(AbstractContainerScreen<?> screen) {
        List<Slot> containerSlots = new ArrayList<>();
        var menu = screen.getMenu();

        int totalSlots = menu.slots.size();
        int playerInventorySize = 36; // 27 main + 9 hotbar

        int containerSize = totalSlots - playerInventorySize;
        if (containerSize < 0) containerSize = totalSlots;

        for (int i = 0; i < containerSize && i < menu.slots.size(); i++) {
            containerSlots.add(menu.slots.get(i));
        }

        return containerSlots;
    }

    private static String getInventoryType(int slotCount) {
        return switch (slotCount) {
            case 9 -> "CHEST_1_ROW";
            case 18 -> "CHEST_2_ROW";
            case 27 -> "CHEST_3_ROW";
            case 36 -> "CHEST_4_ROW";
            case 45 -> "CHEST_5_ROW";
            default -> "CHEST_6_ROW";
        };
    }

    private static String generateGUIItem(int slotIndex, ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        String displayName = ItemStackUtils.getDisplayNameLegacy(stack);

        String cleanName = StringUtility.stripColor(displayName);
        if (cleanName.trim().isEmpty()) {
            return "";
        }

        String material = ItemStackUtils.toMinestomMaterial(stack);
        List<String> lore = ItemStackUtils.getLoreAsStrings(stack);
        int count = stack.getCount();
        boolean isPlayerHead = stack.is(Items.PLAYER_HEAD);
        String texture = isPlayerHead ? ItemStackUtils.getPlayerHeadTexture(stack) : "";

        sb.append("        set(new GUIItem(").append(slotIndex).append(") {\n");
        sb.append("            @Override\n");
        sb.append("            public ItemStack.Builder getItem(HypixelPlayer player) {\n");

        if (isPlayerHead && !texture.isEmpty()) {
            sb.append("                return ItemStackCreator.getStackHead(\n");
            sb.append("                        \"").append(escapeJavaString(displayName)).append("\",\n");
            sb.append("                        \"").append(escapeJavaString(texture)).append("\",\n");
            sb.append("                        ").append(count);
        } else {
            sb.append("                return ItemStackCreator.getStack(\n");
            sb.append("                        \"").append(escapeJavaString(displayName)).append("\",\n");
            sb.append("                        Material.").append(material).append(",\n");
            sb.append("                        ").append(count);
        }

        if (!lore.isEmpty()) {
            for (String loreLine : lore) {
                sb.append(",\n                        \"").append(escapeJavaString(loreLine)).append("\"");
            }
        }

        sb.append("\n                );\n");
        sb.append("            }\n");
        sb.append("        });\n");

        return sb.toString();
    }

    private static String escapeJavaString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String toPascalCase(String s) {
        if (s == null || s.isEmpty()) return "Generated";
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            } else {
                capitalizeNext = true;
            }
        }
        return !result.isEmpty() ? result.toString() : "Generated";
    }
}
