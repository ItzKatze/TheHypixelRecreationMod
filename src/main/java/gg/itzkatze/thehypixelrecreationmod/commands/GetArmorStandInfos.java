package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.ItemStackUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetArmorStandInfos {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("getarmorstandinfos")
                            .then(ClientCommandManager.argument("radius", DoubleArgumentType.doubleArg(0))
                                    .executes(context -> {
                                        double radius = DoubleArgumentType.getDouble(context, "radius");
                                        Minecraft client = Minecraft.getInstance();
                                        Player player = client.player;

                                        if (player == null || client.level == null) return 1;

                                        List<ArmorStand> armorStands = client.level.getEntities(
                                                EntityType.ARMOR_STAND,
                                                player.getBoundingBox().inflate(radius),
                                                armorStand -> true
                                        );

                                        if (armorStands.isEmpty()) {
                                            ChatUtils.warn("No armor stands found nearby.");
                                            return 1;
                                        }

                                        for (ArmorStand armorStand : armorStands) {
                                            processArmorStand(client, armorStand);
                                        }

                                        ChatUtils.message(
                                                "Total amount of armor stands: " + armorStands.size()
                                        );

                                        return 1;
                                    })
                            )
            );
        });
    }

    private static void processArmorStand(Minecraft client, ArmorStand armorStand) {
        Map<String, ItemStack> stacks = new HashMap<>();
        stacks.put("Head", armorStand.getItemBySlot(EquipmentSlot.HEAD));
        stacks.put("Chest", armorStand.getItemBySlot(EquipmentSlot.CHEST));
        stacks.put("Legs", armorStand.getItemBySlot(EquipmentSlot.LEGS));
        stacks.put("Feet", armorStand.getItemBySlot(EquipmentSlot.FEET));
        stacks.put("Mainhand", armorStand.getItemBySlot(EquipmentSlot.MAINHAND));
        stacks.put("Offhand", armorStand.getItemBySlot(EquipmentSlot.OFFHAND));

        ChatUtils.sendLine();

        Player player = client.player;
        if (player == null) return;

        Component coordsMessage = Component.literal(
                        "Cords: X: " + armorStand.getX()
                                + " Y: " + armorStand.getY()
                                + " Z: " + armorStand.getZ()
                                + " Yaw: " + armorStand.getYRot()
                                + " Pitch: " + armorStand.getXRot()
                )
                .withStyle(
                        Style.EMPTY.withClickEvent(
                                new ClickEvent.CopyToClipboard(
                                        armorStand.getX() + ", "
                                                + armorStand.getY() + ", "
                                                + armorStand.getZ() + ", "
                                                + armorStand.getYRot() + ", "
                                                + armorStand.getXRot()
                                )
                        )
                );

        player.displayClientMessage(coordsMessage, false);

        for (Map.Entry<String, ItemStack> entry : stacks.entrySet()) {
            String name = entry.getKey();
            ItemStack stack = entry.getValue();

            if (stack.isEmpty()) continue;

            ChatUtils.message(name + ": " + stack.getItem().getName(stack).getString());

            if (stack.is(Items.PLAYER_HEAD)) {
                String textureId = ItemStackUtils.getPlayerHeadTexture(stack);
                client.keyboardHandler.setClipboard(textureId);

                player.displayClientMessage(
                        Component.literal("Copied Texture-ID: ")
                                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))),
                        false
                );

                player.displayClientMessage(
                        Component.literal(textureId)
                                .withStyle(
                                        Style.EMPTY.withClickEvent(
                                                new ClickEvent.CopyToClipboard(textureId)
                                        )
                                ),
                        false
                );
            }
        }

        ChatUtils.sendLine();
    }
}
