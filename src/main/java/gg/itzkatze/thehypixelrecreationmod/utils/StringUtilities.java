package gg.itzkatze.thehypixelrecreationmod.utils;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

public class StringUtilities {
    public static String toLegacyString(Text text) {
        StringBuilder legacy = new StringBuilder();
        final Formatting[] lastColor = new Formatting[1];

        text.visit((style, content) -> {
            if (!content.startsWith("§")) {
                Formatting color = null;

                if (style.getColor() != null && style.getColor().getName() != null) {
                    color = Formatting.byName(style.getColor().getName());
                }

                if (color != null && color != lastColor[0]) {
                    legacy.append('§').append(color.getCode());
                    lastColor[0] = color;
                }

                if (style.isBold()) legacy.append('§').append(Formatting.BOLD.getCode());
                if (style.isItalic()) legacy.append('§').append(Formatting.ITALIC.getCode());
                if (style.isUnderlined()) legacy.append('§').append(Formatting.UNDERLINE.getCode());
                if (style.isStrikethrough()) legacy.append('§').append(Formatting.STRIKETHROUGH.getCode());
                if (style.isObfuscated()) legacy.append('§').append(Formatting.OBFUSCATED.getCode());

                legacy.append(content);
            }

            return Optional.empty();
        }, Style.EMPTY);

        return legacy.toString();
    }

}
