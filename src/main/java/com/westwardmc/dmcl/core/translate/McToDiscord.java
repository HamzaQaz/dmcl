package com.westwardmc.dmcl.core.translate;

public final class McToDiscord {
    private McToDiscord() {}

    public static String stripColorCodes(String s) {
        if (s == null) return "";
        var sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public static String escapeMarkdown(String s) {
        if (s == null) return "";
        var sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '*', '_', '~', '|', '>', '`', '\\' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String translate(String mcText) {
        return escapeMarkdown(stripColorCodes(mcText));
    }
}
