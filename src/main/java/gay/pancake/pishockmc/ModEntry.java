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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

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
                onPlayerSuccessfulZap(context.player(), payload.intensity(), PiShockAPI.ActionDuration.values()[payload.duration()])
        ));
    }

    /** Map containing each player who took damage and the callback to be executed when the zap is successful */
    private final Map<ServerPlayer, BiConsumer<Integer, PiShockAPI.ActionDuration>> pendingZaps = new HashMap<>();

    /**
     * Handle player damage event
     *
     * @param player Player
     * @param source Damage source
     * @param damage Damage amount
     */
    private void onPlayerDamage(ServerPlayer player, DamageSource source, float damage) {
        var server = player.getServer();

        // ignore small damage
        if (damage < 0.000001 && !player.isDeadOrDying())
            return;

        // prepare zap future
        this.pendingZaps.put(player, (intensity, duration) -> printZapMessage(server, player, source, damage, intensity, duration));

        // send packet to client
        ServerPlayNetworking.send(player, new PlayerHurtPacket(damage, player.isDeadOrDying()));
    }

    /**
     * Handle player zap event
     *
     * @param player Player
     * @param intensity Intensity
     * @param duration Duration
     */
    private void onPlayerSuccessfulZap(ServerPlayer player, int intensity, PiShockAPI.ActionDuration duration) {
        var future = this.pendingZaps.remove(player);
        if (future == null)
            return;

        future.accept(intensity, duration);
    }

    /**
     * Print zap message
     *
     * @param server Server
     * @param player Player
     * @param source Damage source
     * @param damage Damage amount
     * @param intensity Intensity
     * @param duration Duration
     */
    private void printZapMessage(MinecraftServer server, Player player, DamageSource source, float damage, int intensity, PiShockAPI.ActionDuration duration) {
        // create damage components
        var damageComponent = Component.literal(String.valueOf(damage)).withStyle(damage >= 5.0 ? (damage >= 10.0 ? ChatFormatting.RED : ChatFormatting.YELLOW) : ChatFormatting.GREEN);
        var intensityComponent = Component.literal(String.valueOf(intensity)).withStyle(intensity >= 20 ? (intensity >= 35 ? (intensity >= 60 ? ChatFormatting.BLUE : ChatFormatting.RED) : ChatFormatting.YELLOW) : ChatFormatting.GREEN);
        var durationComponent = Component.translatable(duration.string).withStyle(duration.duration >= 1000 ? (duration.duration >= 3000 ? (duration.duration >= 5000 ? ChatFormatting.BLUE : ChatFormatting.RED) : ChatFormatting.YELLOW) : ChatFormatting.GREEN);

        // create base damage/death message
        MutableComponent component;
        var base = "damage.attack." + source.type().msgId();
        var damager = source.getEntity() == null ? source.getDirectEntity() : source.getEntity();
        var killer = player.getKillCredit();
        if (player.isDeadOrDying())
            component = Component.translatable("damage.attack.death", player.getDisplayName());
        else if (damager != null)
            component = Component.translatable(base, player.getDisplayName(), damageComponent, damager.getDisplayName());
        else if (killer != null)
            component = Component.translatable(base + ".player", player.getDisplayName(), damageComponent, killer.getDisplayName());
        else
            component = Component.translatable(base, player.getDisplayName(), damageComponent);

        // send message
        server.getPlayerList().broadcastSystemMessage(component.append(Component.translatable("damage.attack.append", intensityComponent, durationComponent)), false);
    }

}
