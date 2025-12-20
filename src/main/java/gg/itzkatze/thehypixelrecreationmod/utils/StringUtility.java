package gg.itzkatze.thehypixelrecreationmod.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtility {
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]");

    /**
     * Converts a Component (Text) into a legacy-formatted string with '§' codes
     * Works for vanilla ChatFormatting colors and formatting.
     */
    public static String toLegacyString(Component component) {
        StringBuilder legacy = new StringBuilder();
        final ChatFormatting[] lastColor = new ChatFormatting[1];

        component.visit((style, content) -> {
            if (!content.startsWith("§")) {
                ChatFormatting color = null;

                TextColor textColor = style.getColor();
                if (textColor != null) {
                    // Only map if it's a legacy color
                    for (ChatFormatting cf : ChatFormatting.values()) {
                        if (!cf.isFormat() && cf.getColor() != null && cf.getColor() == textColor.getValue()) {
                            color = cf;
                            break;
                        }
                    }
                }

                if (color != null && color != lastColor[0]) {
                    legacy.append('§').append(color.getChar());
                    lastColor[0] = color;
                }

                if (style.isBold()) legacy.append('§').append(ChatFormatting.BOLD.getChar());
                if (style.isItalic()) legacy.append('§').append(ChatFormatting.ITALIC.getChar());
                if (style.isUnderlined()) legacy.append('§').append(ChatFormatting.UNDERLINE.getChar());
                if (style.isStrikethrough()) legacy.append('§').append(ChatFormatting.STRIKETHROUGH.getChar());
                if (style.isObfuscated()) legacy.append('§').append(ChatFormatting.OBFUSCATED.getChar());

                legacy.append(content);
            }

            return Optional.empty();
        }, Style.EMPTY);

        return legacy.toString();
    }

    public static String stripColor(String s) {
        Matcher matcher = COLOR_PATTERN.matcher(s);
        return matcher.replaceAll("");
    }
}
