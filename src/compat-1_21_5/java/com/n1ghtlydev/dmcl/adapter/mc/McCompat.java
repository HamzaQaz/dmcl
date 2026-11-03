package com.n1ghtlydev.dmcl.adapter.mc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;

/** Minecraft APIs that differ between versions; one copy per src/compat-* dir. */
public final class McCompat {
    private McCompat() {}

    public static ClickEvent openUrl(String url) { return new ClickEvent.OpenUrl(URI.create(url)); }
    public static ClickEvent copy(String value) { return new ClickEvent.CopyToClipboard(value); }
    public static HoverEvent showText(Component c) { return new HoverEvent.ShowText(c); }
    public static boolean isGamemaster(CommandSourceStack s) { return s.hasPermission(2); }
    public static Component advancementTitle(net.minecraft.advancements.DisplayInfo d) { return d.getTitle(); }
}
