package gay.pancake.pishockmc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import gay.pancake.pishockmc.client.util.CustomToast;
import gay.pancake.pishockmc.packets.PlayerHurtPacket;
import gay.pancake.pishockmc.packets.PlayerZapPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client entry point
 */
@Environment(EnvType.CLIENT)
public class ClientEntry implements ClientModInitializer, ModMenuApi {

    /** Dropdown box entry for configuration gui, except cloth config is buggy */
    public DropdownBoxEntry<String> serialPort_gui;

    /** Serial instance if used */
    private PiShockSerial serial;

    /**
     * Initialize the client mod
     */
    @Override
    public void onInitializeClient() {
        // Register configuration and override serial port dropdown
        AutoConfig.register(ModConfiguration.class, GsonConfigSerializer::new).registerSaveListener((configHolder2, config) -> {
            config.secrets.serialPort = serialPort_gui.getValue();
            return InteractionResult.SUCCESS;
        });
        AutoConfig.getGuiRegistry(ModConfiguration.class).registerPredicateTransformer((guis, i18n, field, secrets, defaults, registry) -> List.of(
                serialPort_gui = ConfigEntryBuilder.create()
                        .startStringDropdownMenu(Component.translatable("text.autoconfig.pishock-mc.option.secrets.serialPort"), ((ModConfiguration.Secrets) secrets).serialPort, Component::literal)
                        .setSelections(PiShockSerial.list().values())
                        .setSuggestionMode(true)
                        .build()
        ), field -> field.getName().equals("serialPort"));

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
            if (config.secrets.useSerialPort) {
                var port = PiShockSerial.list().entrySet().stream().filter(e -> config.secrets.serialPort.contains(e.getValue())).findFirst();
                if (port.isEmpty())
                    throw new IllegalArgumentException("Invalid serial port");
                if (config.secrets.shockerId == 0)
                    throw new IllegalArgumentException("Invalid shocker ID");
                if (this.serial != null) {
                    this.serial.close();
                    this.serial = null;
                }
                this.serial = new PiShockSerial(port.get().getKey());
            } else {
                UUID.fromString(config.secrets.apiKey);
                if (config.secrets.username.isBlank())
                    throw new IllegalArgumentException("Invalid username");
                if (!config.secrets.sharecode.matches("[A-Z0-9]*"))
                    throw new IllegalArgumentException("Invalid sharecode");
            }

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
        } catch (Exception e) {
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
        CompletableFuture<?> future;
        PlayerZapPacket packet;
        if (die && config.deathPunishment.enable) {
            // Handle death punishment
            if (config.secrets.useSerialPort) {
                future = this.serial.call(
                        config.secrets.shockerId,
                        PiShockAPI.ActionType.SHOCK,
                        config.deathPunishment.shockIntensity,
                        config.deathPunishment.shockDuration
                );
            } else {
                future = PiShockAPI.call(
                        config.secrets.username,
                        config.secrets.apiKey,
                        config.secrets.sharecode,
                        PiShockAPI.ActionType.SHOCK,
                        config.deathPunishment.shockIntensity,
                        config.deathPunishment.shockDuration
                );
            }
            packet = new PlayerZapPacket(damage, config.deathPunishment.shockIntensity, config.deathPunishment.shockDuration.ordinal(), true);
        } else if (config.damagePunishment.enable) {
            // Handle damage punishment
            int intensity = (int) (config.damagePunishment.minIntensity + (config.damagePunishment.maxIntensity - config.damagePunishment.minIntensity) * (Math.min(20.0, damage) / 20.0));
            if (config.secrets.useSerialPort) {
                future = this.serial.call(
                        config.secrets.shockerId,
                        PiShockAPI.ActionType.SHOCK,
                        intensity,
                        config.damagePunishment.shockDuration
                );
            } else {
                future = PiShockAPI.call(
                        config.secrets.username,
                        config.secrets.apiKey,
                        config.secrets.sharecode,
                        PiShockAPI.ActionType.SHOCK,
                        intensity,
                        config.damagePunishment.shockDuration
                );
            }
            packet = new PlayerZapPacket(damage, intensity, config.damagePunishment.shockDuration.ordinal(), false);
        } else {
            return;
        }

        // Handle the response
        future.thenAccept(response -> {
            if (response instanceof HttpResponse<?> httpResponse) {
                if (httpResponse.statusCode() != 200) {
                    System.err.println("Failed to call PiShock API: " + httpResponse.statusCode());
                    System.err.println(httpResponse.body());

                    Minecraft.getInstance().getToasts().addToast(new CustomToast(Component.translatable("text.toast.api.fail")));
                    return;
                }
            } else if (!((boolean) response)) {
                return;
            }

            ClientPlayNetworking.send(packet);
        });

    }

}
