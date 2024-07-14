package gay.pancake.pishockmc;

import gay.pancake.pishockmc.client.PiShockAPI;
import gay.pancake.pishockmc.events.PlayerHurtCallback;
import gay.pancake.pishockmc.packets.PlayerHurtPacket;
import gay.pancake.pishockmc.packets.PlayerZapPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
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
        PayloadTypeRegistry.playC2S().register(PlayerZapPacket.ID, PlayerZapPacket.CODEC);

        // Register packet receivers
        ServerPlayNetworking.registerGlobalReceiver(PlayerZapPacket.ID, ((payload, context) ->
                onPlayerSuccessfulZap(context.server(), context.player(), payload.damage(), payload.intensity(), PiShockAPI.ActionDuration.values()[payload.duration()], payload.die())
        ));
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

    /**
     * Handle player zap event
     *
     * @param server Server
     * @param player Player
     * @param damage Damage
     * @param intensity Intensity
     * @param duration Duration
     * @param die Die
     */
    private void onPlayerSuccessfulZap(MinecraftServer server, ServerPlayer player, float damage, int intensity, PiShockAPI.ActionDuration duration, boolean die) {
        // broadcast message to all players
        Component message;
        if (die) {
            message = Component.translatable("text.message.zapdeath", player.getName())
                    .withStyle(style -> style.withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("text.message.zapdeath.@Tooltip", intensity, duration.string)
                    )));
        } else {
            message = Component.translatable("text.message.zap", player.getName(), Component.literal(String.valueOf(damage)))
                    .withStyle(style -> style.withColor(damage >= 5.0 ? (damage >= 10.0 ? ChatFormatting.RED : ChatFormatting.YELLOW) : ChatFormatting.GREEN).withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("text.message.zap.@Tooltip", intensity, duration.string)
                    )));
        }
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

}
