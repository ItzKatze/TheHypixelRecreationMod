package gg.itzkatze.thehypixelrecreationmod.mixin;

import com.mojang.serialization.JsonOps;
import gg.itzkatze.thehypixelrecreationmod.features.KeybindRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	@Final
	private List<GuiMessage> allMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	public abstract boolean isChatFocused();

	@Shadow
	public abstract int getWidth();

	@Shadow
	public abstract double getScale();

	@Shadow
	protected abstract boolean isChatHidden();

	@Inject(method = "render", at = @At("HEAD"))
	private void chatjsoncopy$onRender(GuiGraphics guiGraphics, Font font, int currentTick, int mouseX, int mouseY, boolean bl, boolean bl2, CallbackInfo ci) {
		if (!KeybindRegistry.wasChatCopyPressed()) return;
		if (trimmedMessages.isEmpty()) return;

		// Get the message at the mouse position
		double chatLineX = chatjsoncopy$toChatLineX(mouseX);
		double chatLineY = chatjsoncopy$toChatLineY(mouseY);
		int messageIndex = chatjsoncopy$getMessageIndex(chatLineX, chatLineY);

		if (messageIndex >= 0 && messageIndex < trimmedMessages.size()) {
			GuiMessage.Line visibleLine = trimmedMessages.get(messageIndex);

			// Find the corresponding full message
			GuiMessage fullMessage = null;
			for (GuiMessage message : allMessages) {
				if (message.addedTime() == visibleLine.addedTime()) {
					fullMessage = message;
					break;
				}
			}

			if (fullMessage != null && minecraft.level != null && minecraft.player != null) {
				Component text = fullMessage.content();
				var registryManager = minecraft.level.registryAccess();

				var result = ComponentSerialization.CODEC.encodeStart(registryManager.createSerializationContext(JsonOps.INSTANCE), text);
				String json = result.result().orElse(null) != null
						? result.result().get().toString()
						: text.getString();

				minecraft.keyboardHandler.setClipboard(json);
				minecraft.player.displayClientMessage(
						Component.literal("Copied chat JSON to clipboard").withStyle(net.minecraft.ChatFormatting.GREEN),
						false
				);
			}
		}
	}

	@Unique
	private double chatjsoncopy$toChatLineX(double x) {
		return x / getScale() - 4.0;
	}

	@Unique
	private double chatjsoncopy$toChatLineY(double y) {
		double d = (double) minecraft.getWindow().getGuiScaledHeight() - y - 40.0;
		return d / (getScale() * (double) chatjsoncopy$getLineHeight());
	}

	@Unique
	private int chatjsoncopy$getMessageIndex(double chatLineX, double chatLineY) {
		int i = chatjsoncopy$getMessageLineIndex(chatLineX, chatLineY);
		if (i == -1) {
			return -1;
		}

		// Find the start of the message
		while (i >= 0) {
			if (trimmedMessages.get(i).endOfEntry()) {
				return i;
			}
			--i;
		}
		return i;
	}

	@Unique
	private int chatjsoncopy$getMessageLineIndex(double chatLineX, double chatLineY) {
		if (!isChatFocused() || isChatHidden()) {
			return -1;
		}

		if (chatLineX < -4.0 || chatLineX > (double) net.minecraft.util.Mth.floor((double) getWidth() / getScale())) {
			return -1;
		}

		int visibleLineCount = chatjsoncopy$getVisibleLineCount();
		int i = Math.min(visibleLineCount, trimmedMessages.size());

		if (chatLineY >= 0.0 && chatLineY < (double) i) {
			int j = net.minecraft.util.Mth.floor(chatLineY + (double) chatScrollbarPos);
			if (j >= 0 && j < trimmedMessages.size()) {
				return j;
			}
		}

		return -1;
	}

	@Unique
	private int chatjsoncopy$getVisibleLineCount() {
		return chatjsoncopy$getHeight() / chatjsoncopy$getLineHeight();
	}

	@Unique
	private int chatjsoncopy$getHeight() {
		double heightOption = isChatFocused()
				? minecraft.options.chatHeightFocused().get()
				: minecraft.options.chatHeightUnfocused().get();
		return net.minecraft.util.Mth.floor(heightOption * 160.0 + 20.0);
	}

	@Unique
	private int chatjsoncopy$getLineHeight() {
		return (int) (9.0 * (minecraft.options.chatLineSpacing().get() + 1.0));
	}
}