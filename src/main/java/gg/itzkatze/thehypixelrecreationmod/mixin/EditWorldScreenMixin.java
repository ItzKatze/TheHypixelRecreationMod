package gg.itzkatze.thehypixelrecreationmod.mixin;

import com.mojang.logging.LogUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.PolarConvert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.OptimizeWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.io.IOException;
import java.nio.file.Path;

import static net.minecraft.client.gui.screens.worldselection.EditWorldScreen.makeBackupAndShowToast;

@Mixin(EditWorldScreen.class)
public abstract class EditWorldScreenMixin extends net.minecraft.client.gui.screens.Screen {

    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    @Final
    private LinearLayout layout;
    @Shadow
    @Final
    private LevelStorageSource.LevelStorageAccess levelAccess;

    @Shadow
    @Final
    private static Component OPTIMIZE_BUTTON;

    @Shadow
    @Final
    private static Component OPTIMIIZE_CONFIRMATION;

    @Shadow
    @Final
    private static Component OPTIMIIZE_DESCRIPTION;

    @Shadow
    @Final
    private static Component OPTIMIZE_TITLE;

    protected EditWorldScreenMixin(Component component) {
        super(component);
    }

    @ModifyArgs(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 9)
    )
    private void removeOriginalOptimizeButton(Args args) {
        LinearLayout buttonRow = LinearLayout.horizontal().spacing(4);

        Button polarButton = Button.builder(Component.literal("Convert to Polar"), btn -> {
            btn.setMessage(Component.literal("Processing..."));
            btn.active = false;

            Thread.ofVirtual().name("thrm-polar-convert").start(() -> {
                boolean success = convertToPolar(
                        levelAccess.getLevelDirectory().path(),
                        Path.of(levelAccess.getLevelDirectory().path().toString(), levelAccess.getLevelId() + ".polar")
                );
                Minecraft.getInstance().submit(() -> {
                    Minecraft.getInstance().getToastManager().addToast(
                            new SystemToast(
                                    SystemToast.SystemToastId.WORLD_BACKUP,
                                    Component.literal(success ? "Success" : "Failed to convert."),
                                    Component.literal(success ? "Opening folder" : "Check console for issues.")
                            ));
                    if (success) {
                        Util.getPlatform().openPath(levelAccess.getLevelDirectory().path());
                    }
                    btn.setMessage(Component.literal("Convert to Polar"));
                    btn.active = true;
                });
            });
        }).width(98).build();

        Button optimizeButton = Button.builder(OPTIMIZE_BUTTON, buttonx -> {
            Minecraft minecraft = Minecraft.getInstance(); // oder im Mixin zwischenspeichern, wenn du willst
            minecraft.setScreen(new BackupConfirmScreen(() -> minecraft.setScreen((EditWorldScreen)(Object)this),
                    (bl, bl2) -> {
                        if (bl) {
                            makeBackupAndShowToast(levelAccess);
                        }
                        minecraft.setScreen(OptimizeWorldScreen.create(minecraft, null, minecraft.getFixerUpper(), levelAccess, bl2));
                    },
                    OPTIMIZE_TITLE,
                    OPTIMIIZE_DESCRIPTION,
                    OPTIMIIZE_CONFIRMATION,
                    true));
        }).width(98).build();

        buttonRow.addChild(polarButton);
        buttonRow.addChild(optimizeButton);

        args.set(0, buttonRow);
    }

    @Unique
    public Boolean convertToPolar(Path anvilPath, Path outputPath) {
        try {
            return PolarConvert.convertWorldFolderToPolar(anvilPath, outputPath);
        } catch (IOException e) {
            LOGGER.warn("Fail to convert world", e);
            return false;
        }
    }
}
