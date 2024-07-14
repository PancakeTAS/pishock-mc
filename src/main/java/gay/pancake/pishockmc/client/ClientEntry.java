package gay.pancake.pishockmc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import gay.pancake.pishockmc.client.util.CustomToast;
import gay.pancake.pishockmc.packets.PlayerHurtPacket;
import gay.pancake.pishockmc.packets.PlayerZapPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client entry point
 */
@Environment(EnvType.CLIENT)
public class ClientEntry implements ClientModInitializer, ModMenuApi {

    /**
     * Initialize the client mod
     */
    @Override
    public void onInitializeClient() {
        // Register configuration
        AutoConfig.register(ModConfiguration.class, GsonConfigSerializer::new);

        // Register events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onWorldConnect(client));

        // Register packet receivers
        ClientPlayNetworking.registerGlobalReceiver(PlayerHurtPacket.ID, ((payload, context) -> onPlayerDamage(payload.damage(), payload.die())));
    }

    /**
     * Get the configuration screen factory for integration with ModMenu
     *
     * @return Configuration screen factory
     */
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(ModConfiguration.class, parent).get();
    }

    /**
     * Handle world connect event.
     *
     * @param handler Client packet listener
     * @param sender Packet sender
     * @param client Minecraft client
     */
    private void onWorldConnect(Minecraft client) {
        // Validate configuration
        var config = AutoConfig.getConfigHolder(ModConfiguration.class).getConfig();
        if (!config.enableMod)
            return;

        try {
            // Validate secrets
            UUID.fromString(config.secrets.apiKey);
            if (!config.secrets.sharecode.matches("[A-Z0-9]*"))
                throw new IllegalArgumentException("Invalid sharecode");

            // Validate damage punishment
            if (config.damagePunishment.enable) {
                if (config.damagePunishment.maxIntensity > 100 || config.damagePunishment.maxIntensity < 0)
                    throw new IllegalArgumentException("Invalid damage punishment max intensity");
                if (config.damagePunishment.minIntensity > 100 || config.damagePunishment.minIntensity < 0)
                    throw new IllegalArgumentException("Invalid damage punishment min intensity");
                if (config.damagePunishment.maxIntensity < config.damagePunishment.minIntensity)
                    throw new IllegalArgumentException("Damage punishment max intensity must be greater than min intensity");
            }

            // Validate death punishment
            if (config.deathPunishment.enable) {
                if (config.deathPunishment.shockIntensity > 100 || config.deathPunishment.shockIntensity < 0)
                    throw new IllegalArgumentException("Invalid death punishment shock intensity");
            }
        } catch (IllegalArgumentException e) {
            client.getToasts().addToast(new CustomToast(Component.translatable("text.toast.login.fail")));

        }
    }

    /**
     * Handle player damage packets
     *
     * @param damage Damage amount
     * @param die Whether the player died
     */
    private void onPlayerDamage(float damage, boolean die) {
        // Grab the configuration
        var config = AutoConfig.getConfigHolder(ModConfiguration.class).getConfig();

        // Check if the player died
        CompletableFuture<HttpResponse<String>> future;
        PlayerZapPacket packet;
        if (die && config.deathPunishment.enable) {
            // Handle death punishment
            future = PiShockAPI.call(
                    config.secrets.username,
                    config.secrets.apiKey,
                    config.secrets.sharecode,
                    PiShockAPI.ActionType.SHOCK,
                    config.deathPunishment.shockIntensity,
                    config.deathPunishment.shockDuration
            );
            packet = new PlayerZapPacket(damage, config.deathPunishment.shockIntensity, config.deathPunishment.shockDuration.ordinal(), true);
        } else if (config.damagePunishment.enable) {
            // Handle damage punishment
            int intensity = (int) (config.damagePunishment.minIntensity + (config.damagePunishment.maxIntensity - config.damagePunishment.minIntensity) * (Math.min(20.0, damage) / 20.0));
            future = PiShockAPI.call(
                    config.secrets.username,
                    config.secrets.apiKey,
                    config.secrets.sharecode,
                    PiShockAPI.ActionType.SHOCK,
                    intensity,
                    config.damagePunishment.shockDuration
            );
            packet = new PlayerZapPacket(damage, intensity, config.damagePunishment.shockDuration.ordinal(), false);
        } else {
            return;
        }

        // Handle the response
        future.thenAccept(response -> {
            if (response.statusCode() != 200) {
                System.err.println("Failed to call PiShock API: " + response.statusCode());
                System.err.println(response.body());

                Minecraft.getInstance().getToasts().addToast(new CustomToast(Component.translatable("text.toast.api.fail")));
                return;
            }

            ClientPlayNetworking.send(packet);
        });

    }

}
