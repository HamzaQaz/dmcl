package com.westwardmc.dmcl.adapter.fabric;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;

/** Minecraft APIs that differ between versions; one copy per src/compat-* dir. */
final class McCompat {
    private McCompat() {}

    static ClickEvent openUrl(String url) { return new ClickEvent.OpenUrl(URI.create(url)); }
    static ClickEvent copy(String value) { return new ClickEvent.CopyToClipboard(value); }
    static HoverEvent showText(Component c) { return new HoverEvent.ShowText(c); }
    static boolean isGamemaster(CommandSourceStack s) { return s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER); }
}
