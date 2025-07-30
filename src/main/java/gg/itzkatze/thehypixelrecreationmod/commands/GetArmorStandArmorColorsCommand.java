package gg.itzkatze.thehypixelrecreationmod.commands;

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
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;

public class GetArmorStandArmorColorsCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getArmorStandColors")
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
                            player.sendMessage(Text.literal("No armor stands found nearby."), false);
                            return 1;
                        }

                        for (ArmorStandEntity armorStand : armorStands) {
                            processArmorStand(player, armorStand);
                        }

                        return 1;
                    }));
        });
    }

    private static void processArmorStand(PlayerEntity player, ArmorStandEntity armorStand) {
        ItemStack[] slots = new ItemStack[] {
                armorStand.getEquippedStack(EquipmentSlot.HEAD),
                armorStand.getEquippedStack(EquipmentSlot.CHEST),
                armorStand.getEquippedStack(EquipmentSlot.LEGS),
                armorStand.getEquippedStack(EquipmentSlot.FEET),
        };

        Text line = Text.literal("------------------------------------")
                .setStyle(Style.EMPTY
                        .withBold(true)
                        .withColor(TextColor.fromFormatting(Formatting.GOLD))
                );

        player.sendMessage(line, false);

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
                player.sendMessage(stack.getItemName(), false);
                player.sendMessage(colorMessage, false);
            }
        }

        player.sendMessage(line, false);
    }
}
