package com.westwardmc.dmcl.adapter.fabric;

import com.mojang.brigadier.Command;
import com.westwardmc.dmcl.core.orchestrator.BridgeOrchestrator;
import com.westwardmc.dmcl.core.port.LinkRepo;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class McLinkCommand {
    private McLinkCommand() {}

    public static void register(BridgeOrchestrator orch, LinkRepo links) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(Commands.literal("link").executes(ctx -> {
                var src = ctx.getSource();
                if (!src.isPlayer()) {
                    src.sendFailure(Component.literal("Players only"));
                    return 0;
                }
                orch.startLink(src.getPlayer().getUUID());
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(Commands.literal("unlink").executes(ctx -> {
                var src = ctx.getSource();
                if (!src.isPlayer()) return 0;
                links.unlinkByMc(src.getPlayer().getUUID());
                src.sendSuccess(() -> Component.literal("Unlinked."), false);
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(Commands.literal("dmcl")
                .then(Commands.literal("status").requires(s -> McCompat.hasPermission(s, 2))
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("DMCL: see logs for details"), false);
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }
}
