package gay.pancake.pishockmc;

import gay.pancake.pishockmc.events.PlayerHurtCallback;
import gay.pancake.pishockmc.packets.PlayerHurtPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Common entry point
 */
public class ModEntry implements ModInitializer {

    /**
     * Initialize the mod
     */
    @Override
    public void onInitialize() {
        // Register events
        PlayerHurtCallback.EVENT.register(this::onPlayerDamage);

        // Register packets
        PayloadTypeRegistry.playS2C().register(PlayerHurtPacket.ID, PlayerHurtPacket.CODEC);
    }

    /**
     * Handle player damage event
     *
     * @param player Player
     * @param damage Damage amount
     */
    private void onPlayerDamage(ServerPlayer player, float damage) {
        if (damage < 0.000001) // ignore small damage
            return;

        // send packet to client
        ServerPlayNetworking.send(player, new PlayerHurtPacket(damage, player.isDeadOrDying()));
    }
    }

}
