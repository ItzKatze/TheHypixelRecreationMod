package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;

public class GetArmorStandArmorColorsCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getArmorStandColors")
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
        ItemStack[] slots = new ItemStack[] {
                armorStand.getEquippedStack(EquipmentSlot.HEAD),
                armorStand.getEquippedStack(EquipmentSlot.CHEST),
                armorStand.getEquippedStack(EquipmentSlot.LEGS),
                armorStand.getEquippedStack(EquipmentSlot.FEET),
        };

        ChatUtils.sendLine();

        for (ItemStack stack : slots) {
            if (stack.isEmpty()) continue;

            int color = DyedColorComponent.getColor(stack, DyedColorComponent.DEFAULT_COLOR);
            if (color != DyedColorComponent.DEFAULT_COLOR) {
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                Text colorMessage = Text.literal("Copy Color (click)")
                        .setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent.CopyToClipboard(red + ", " + green + ", " + blue))
                                .withColor(color)
                        );
                client.player.sendMessage(stack.getItemName(), false);
                client.player.sendMessage(colorMessage, false);
            }
        }

        ChatUtils.sendLine();
    }
}
