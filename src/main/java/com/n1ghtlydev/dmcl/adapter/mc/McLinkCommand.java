package com.n1ghtlydev.dmcl.adapter.mc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.n1ghtlydev.dmcl.bootstrap.DmclBootstrap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** /link, /unlink and /dmcl status. The bridge is resolved lazily: commands are registered before it starts. */
public final class McLinkCommand {
    private McLinkCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                Supplier<DmclBootstrap.Running> bridge) {
        dispatcher.register(Commands.literal("link").executes(ctx -> {
            var src = ctx.getSource();
            var b = bridge.get();
            if (b == null) { src.sendFailure(Component.literal("DMCL is not running; check the server log.")); return 0; }
            if (!src.isPlayer()) {
                src.sendFailure(Component.literal("Players only"));
                return 0;
            }
            b.orchestrator().startLink(src.getPlayer().getUUID());
            return Command.SINGLE_SUCCESS;
        }));
        dispatcher.register(Commands.literal("unlink").executes(ctx -> {
            var src = ctx.getSource();
            var b = bridge.get();
            if (b == null || !src.isPlayer()) return 0;
            b.links().unlinkByMc(src.getPlayer().getUUID());
            src.sendSuccess(() -> Component.literal("Unlinked."), false);
            return Command.SINGLE_SUCCESS;
        }));
        dispatcher.register(Commands.literal("dmcl")
            .then(Commands.literal("status").requires(McCompat::isGamemaster)
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(bridge.get() == null ? "DMCL: not running" : "DMCL: running"), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
