package com.westwardmc.dmcl.mixin;

import com.westwardmc.dmcl.DmclMod;
import com.westwardmc.dmcl.core.port.PlayerEvent;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void dmcl$onGrant(AdvancementHolder adv, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (player == null || adv.value().display().isEmpty()) return;
        var fabric = DmclMod.fabricAdapter();
        if (fabric == null) return;
        fabric.playerHandler().accept(new PlayerEvent.Advanced(
            player.getUUID(),
            player.getName().getString(),
            adv.value().display().get().getTitle().getString()));
    }
}
