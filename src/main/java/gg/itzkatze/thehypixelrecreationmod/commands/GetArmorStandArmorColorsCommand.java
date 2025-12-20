package gg.itzkatze.thehypixelrecreationmod.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.List;

public class GetArmorStandArmorColorsCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getarmorstandcolors")
                    .then(ClientCommandManager.argument("radius", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {
                                double radius = DoubleArgumentType.getDouble(context, "radius");
                                Minecraft client = Minecraft.getInstance();
                                Player player = client.player;
                                if (player == null || client.level == null) return 1;

                                List<ArmorStand> armorStands = client.level.getEntities(
                                        EntityType.ARMOR_STAND,
                                        player.getBoundingBox().inflate(radius),
                                        armorStandEntity -> true
                                );

                                if (armorStands.isEmpty()) {
                                    ChatUtils.warn("No armor stands found nearby.");
                                    return 1;
                                }

                                for (ArmorStand armorStand : armorStands) {
                                    processArmorStand(client, armorStand);
                                }

                                return 1;
                            })
                    )
            );
        });
    }

    private static void processArmorStand(Minecraft client, ArmorStand armorStand) {
        ItemStack[] slots = new ItemStack[] {
                armorStand.getItemBySlot(EquipmentSlot.HEAD),
                armorStand.getItemBySlot(EquipmentSlot.CHEST),
                armorStand.getItemBySlot(EquipmentSlot.LEGS),
                armorStand.getItemBySlot(EquipmentSlot.FEET),
        };

        ChatUtils.sendLine();

        Player player = client.player;
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) continue;

            DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
            if (dyedColor != null) {
                int color = dyedColor.rgb();
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                Style style = Style.EMPTY
                        .withClickEvent(new ClickEvent.CopyToClipboard(red + ", " + green + ", " + blue))
                        .withColor(TextColor.fromRgb(color));

                Component colorMessage = Component.literal("Copy Color (click)").withStyle(style);

                player.displayClientMessage(stack.getHoverName(), false);
                player.displayClientMessage(colorMessage, false);
            }
        }

        ChatUtils.sendLine();
    }
}