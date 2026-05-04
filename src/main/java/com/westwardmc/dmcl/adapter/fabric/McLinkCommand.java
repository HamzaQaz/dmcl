package com.westwardmc.dmcl.adapter.fabric;

import com.mojang.brigadier.Command;
import com.westwardmc.dmcl.core.orchestrator.BridgeOrchestrator;
import com.westwardmc.dmcl.core.port.LinkRepo;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public final class McLinkCommand {
    private McLinkCommand() {}

    public static void register(BridgeOrchestrator orch, LinkRepo links) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(CommandManager.literal("link").executes(ctx -> {
                var src = ctx.getSource();
                if (!src.isExecutedByPlayer()) {
                    src.sendError(Text.literal("Players only"));
                    return 0;
                }
                orch.startLink(src.getPlayer().getUuid());
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(CommandManager.literal("unlink").executes(ctx -> {
                var src = ctx.getSource();
                if (!src.isExecutedByPlayer()) return 0;
                links.unlinkByMc(src.getPlayer().getUuid());
                src.sendFeedback(() -> Text.literal("Unlinked."), false);
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(CommandManager.literal("dmcl")
                .then(CommandManager.literal("status").requires(s -> s.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(() ->
                            Text.literal("DMCL: see logs for details"), false);
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }
}
