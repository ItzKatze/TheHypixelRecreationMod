package gg.itzkatze.thehypixelrecreationmod.commands;

import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetArmorStandInfos {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getArmorStandInfos")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        PlayerEntity player = client.player;
                        if (player == null || client.world == null) return 1;

                        List<ArmorStandEntity> armorStands = client.world.getEntitiesByClass(
                                ArmorStandEntity.class,
                                player.getBoundingBox().expand(2),
                                armorStandEntity -> armorStandEntity.getType() == EntityType.ARMOR_STAND
                        );

                        if (armorStands.isEmpty()) {
                            ChatUtils.warn("No armor stands found nearby.");
                            return 1;
                        }

                        for (ArmorStandEntity armorStand : armorStands) {
                            processArmorStand(client, armorStand);
                        }

                        return 1;
                    })
            );
        });
    }

    private static void processArmorStand(MinecraftClient client, ArmorStandEntity armorStand) {

        Map<String, ItemStack> stacks = new HashMap<>();
        stacks.put("Head", armorStand.getEquippedStack(EquipmentSlot.HEAD));
        stacks.put("Chest", armorStand.getEquippedStack(EquipmentSlot.CHEST));
        stacks.put("Legs", armorStand.getEquippedStack(EquipmentSlot.LEGS));
        stacks.put("Feet", armorStand.getEquippedStack(EquipmentSlot.FEET));
        stacks.put("Mainhand", armorStand.getEquippedStack(EquipmentSlot.MAINHAND));
        stacks.put("Offhand", armorStand.getEquippedStack(EquipmentSlot.OFFHAND));

        ChatUtils.sendLine();

        Text colorMessage = Text.literal("Cords: X: " + armorStand.getX() + " Y: " + armorStand.getY() + " Z: " + armorStand.getZ() + " Yaw: " + armorStand.getYaw() + " Pitch: " + armorStand.getPitch())
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.CopyToClipboard(armorStand.getX() + ", " + armorStand.getY() + ", " + armorStand.getZ() + ", " + armorStand.getYaw() + ", " + armorStand.getPitch()))
                );
        client.player.sendMessage(colorMessage, false);

        for (Map.Entry<String, ItemStack> stack : stacks.entrySet()) {
            String name = stack.getKey();
            ItemStack itemStack = stack.getValue();

            if (itemStack.isEmpty()) continue;

            ChatUtils.message(name + ": " + itemStack.getItem().getName());
        }

        ChatUtils.sendLine();
    }
}
