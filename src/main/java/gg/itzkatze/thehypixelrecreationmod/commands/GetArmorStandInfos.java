package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.ItemStackUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetArmorStandInfos {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getArmorStandInfos")
                    .then(ClientCommandManager.argument("radius", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {
                                double radius = DoubleArgumentType.getDouble(context, "radius");
                                MinecraftClient client = MinecraftClient.getInstance();
                                PlayerEntity player = client.player;
                                if (player == null || client.world == null) return 1;

                                List<ArmorStandEntity> armorStands = client.world.getEntitiesByClass(
                                        ArmorStandEntity.class,
                                        player.getBoundingBox().expand(radius),
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
                    )
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
            if (itemStack.getItem() == Items.PLAYER_HEAD) {
                String textureID = ItemStackUtils.getPlayerHeadTexture(itemStack);
                client.keyboard.setClipboard(textureID);

                client.player.sendMessage(Text.literal("Copied Texture-ID: ")
                        .setStyle(Style.EMPTY
                                .withColor(TextColor.fromFormatting(Formatting.AQUA))
                        ), false);
                client.player.sendMessage(Text.literal(textureID)
                        .setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent.CopyToClipboard(textureID))
                        ), false);
            }
        }

        ChatUtils.sendLine();
    }
}
