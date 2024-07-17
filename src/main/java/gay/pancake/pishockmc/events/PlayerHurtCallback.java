package gay.pancake.pishockmc.events;

import gay.pancake.pishockmc.mixin.MixinPlayerHurtCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;


/**
 * Callback for when a player is hurt
 *
 * @see MixinPlayerHurtCallback
 */
public interface PlayerHurtCallback {

    /** Event instance */
    Event<PlayerHurtCallback> EVENT = EventFactory.createArrayBacked(PlayerHurtCallback.class,
            (listeners) -> (player, damageSource, damage) -> {
                for (PlayerHurtCallback event : listeners) {
                    event.onHurt(player, damageSource, damage);
                }
            });

    /**
     * Called when the local player is hurt
     *
     * @param player Server player
     * @param damageSource Damage source
     * @param damage Damage amount
     */
    void onHurt(ServerPlayer player, DamageSource damageSource, float damage);

}
