package gay.pancake.pishockmc.mixin;

import gay.pancake.pishockmc.events.PlayerHurtCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for triggering player callbacks
 */
@Mixin(Player.class)
public class MixinPlayerHurtCallback {

    /**
     * Trigger the player hurt callback when a player is actually hurt
     *
     * @param damage Amount of damage
     * @param ci Callback info
     */
    @Inject(at = @At("TAIL"), method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V")
    private void onHurt(DamageSource damageSource, float damage, CallbackInfo ci) {
        if ((Player) (Object) this instanceof ServerPlayer p) {
            damage = (float) Math.floor(damage * 2.0f) / 2.0f; // round to nearest half
            PlayerHurtCallback.EVENT.invoker().onHurt(p, damageSource, damage);
        }
    }

}
