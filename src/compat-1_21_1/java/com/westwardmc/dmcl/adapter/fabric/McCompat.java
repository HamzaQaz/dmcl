package com.westwardmc.dmcl.adapter.fabric;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Minecraft APIs that differ between versions; one copy per src/compat-* dir. */
final class McCompat {
    private McCompat() {}

    static ClickEvent openUrl(String url) { return new ClickEvent(ClickEvent.Action.OPEN_URL, url); }
    static ClickEvent copy(String value) { return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value); }
    static HoverEvent showText(Component c) { return new HoverEvent(HoverEvent.Action.SHOW_TEXT, c); }
    static boolean isGamemaster(CommandSourceStack s) { return s.hasPermission(2); }
}
