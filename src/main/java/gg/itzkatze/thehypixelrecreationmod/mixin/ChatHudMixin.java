package gg.itzkatze.thehypixelrecreationmod.mixin;

import com.mojang.serialization.JsonOps;
import gg.itzkatze.thehypixelrecreationmod.features.KeybindRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

	@Shadow
	@Final
	private List<ChatHudLine.Visible> visibleMessages;

	@Shadow
	@Final
	private List<ChatHudLine> messages;

	@Shadow
	private int scrolledLines;

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	public abstract boolean isChatFocused();

	@Shadow
	public abstract int getWidth();

	@Shadow
	public abstract double getChatScale();

	@Shadow
	protected abstract boolean isChatHidden();

	@Inject(method = "render", at = @At("HEAD"))
	private void chatjsoncopy$onRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
		if (!KeybindRegistry.wasChatCopyPressed()) return;
		if (visibleMessages.isEmpty()) return;

		// the message at the mouse position
		double chatLineX = chatjsoncopy$toChatLineX(mouseX);
		double chatLineY = chatjsoncopy$toChatLineY(mouseY);
		int messageIndex = chatjsoncopy$getMessageIndex(chatLineX, chatLineY);

		if (messageIndex >= 0 && messageIndex < visibleMessages.size()) {
			ChatHudLine.Visible visibleLine = visibleMessages.get(messageIndex);

			// find the corresponding full message
			ChatHudLine fullMessage = null;
			for (ChatHudLine message : messages) {
				if (message.creationTick() == visibleLine.addedTime()) {
					fullMessage = message;
					break;
				}
			}

			if (fullMessage != null && client.world != null && client.player != null) {
				Text text = fullMessage.content();
				var registryManager = client.world.getRegistryManager();

				var result = TextCodecs.CODEC.encodeStart(registryManager.getOps(JsonOps.INSTANCE), text);
				String json = result.result().orElse(null) != null
					? result.result().get().toString()
					: text.getString();

				client.keyboard.setClipboard(json);
				client.player.sendMessage(
						Text.literal("Copied chat JSON to clipboard").formatted(net.minecraft.util.Formatting.GREEN),
						false
				);
			}
		}
	}

	@Unique
	private double chatjsoncopy$toChatLineX(double x) {
		return x / getChatScale() - 4.0;
	}

	@Unique
	private double chatjsoncopy$toChatLineY(double y) {
		double d = (double) client.getWindow().getScaledHeight() - y - 40.0;
		return d / (getChatScale() * (double) chatjsoncopy$getLineHeight());
	}

	@Unique
	private int chatjsoncopy$getMessageIndex(double chatLineX, double chatLineY) {
		int i = chatjsoncopy$getMessageLineIndex(chatLineX, chatLineY);
		if (i == -1) {
			return -1;
		}

		// find the start of the message
		while (i >= 0) {
			if (visibleMessages.get(i).endOfEntry()) {
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

		if (chatLineX < -4.0 || chatLineX > (double) MathHelper.floor((double) getWidth() / getChatScale())) {
			return -1;
		}

		int visibleLineCount = chatjsoncopy$getVisibleLineCount();
		int i = Math.min(visibleLineCount, visibleMessages.size());

		if (chatLineY >= 0.0 && chatLineY < (double) i) {
			int j = MathHelper.floor(chatLineY + (double) scrolledLines);
			if (j >= 0 && j < visibleMessages.size()) {
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
				? client.options.getChatHeightFocused().getValue()
				: client.options.getChatHeightUnfocused().getValue();
		return MathHelper.floor(heightOption * 160.0 + 20.0);
	}

	@Unique
	private int chatjsoncopy$getLineHeight() {
		return (int) (9.0 * (client.options.getChatLineSpacing().getValue() + 1.0));
	}
}
