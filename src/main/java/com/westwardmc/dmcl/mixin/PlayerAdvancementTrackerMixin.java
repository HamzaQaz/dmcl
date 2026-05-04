package com.westwardmc.dmcl.mixin;

import com.westwardmc.dmcl.DmclMod;
import com.westwardmc.dmcl.core.port.PlayerEvent;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void dmcl$onGrant(AdvancementEntry adv, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (owner == null || adv.value().display().isEmpty()) return;
        var fabric = DmclMod.fabricAdapter();
        if (fabric == null) return;
        fabric.playerHandler().accept(new PlayerEvent.Advanced(
            owner.getUuid(),
            owner.getName().getString(),
            adv.value().display().get().getTitle().getString()));
    }
}
