package com.lucalabs.naturescompass.utils;

import net.minecraft.client.font.TextRenderer;

public abstract class TextUtils {
    public static String abbreviateText(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String[] words = text.split(" ");
        for (int i = words.length - 1; i >= 0; i--) {
            words[i] = words[i].charAt(0) + ".";
            String shortened = String.join(" ", words);

            if (textRenderer.getWidth(shortened) <= maxWidth) {
                return shortened;
            }
        }
        return words[0].charAt(0) + ".";
    }
}
