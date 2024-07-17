package gay.pancake.pishockmc.client;

import gay.pancake.pishockmc.client.PiShockAPI.ActionDuration;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Configuration for the mod. This class holds all user-adjustable settings, that control how the mod behaves.
 * Aside from behavior settings, this class also holds the API key and share code for the mod.
 */
@Config(name = "pishock-mc")
@Environment(EnvType.CLIENT)
public class ModConfiguration implements ConfigData {

    /** Whether the mod is enabled or not. */
    @Tooltip
    boolean enableMod = true;

    /** Damage punishment settings. */
    @CollapsibleObject @Tooltip
    DamagePunishment damagePunishment = new DamagePunishment();

    /**
     * Configuration settings related to the punishment for taking damage.
     * This includes the intensity and duration of the shock.
     */
    static class DamagePunishment {

        /** Whether the punishment is enabled or not. */
        @Tooltip
        boolean enable = true;

        /** The minimum intensity of the shock. Any hit will deal at least minIntensity damage */
        @BoundedDiscrete(min = 0, max = 100) @Tooltip
        int minIntensity = 0;

        /** The maximum intensity of the shock. Taking at 10 or more hearts of damage will deal maxIntensity damage. Must be larger or equal to minIntensity */
        @BoundedDiscrete(min = 0, max = 100)  @Tooltip
        int maxIntensity = 100;

        /** The duration of the shock. Can be either 100ms, 300ms or 1-10s */
        @Tooltip
        ActionDuration shockDuration = ActionDuration.MS_300;

    }

    /** Death punishment settings. */
    @CollapsibleObject @Tooltip
    DeathPunishment deathPunishment = new DeathPunishment();

    /**
     * Configuration settings related to the punishment for dying.
     * This includes the intensity and duration of the shock.
     */
    static class DeathPunishment {

        /** Whether the punishment is enabled or not. */
        @Tooltip
        boolean enable = true;

        /** The intensity of the shock. */
        @BoundedDiscrete(min = 0, max = 100) @Tooltip
        int shockIntensity = 25;

        /** The duration of the shock. Can be either 100ms, 300ms or 1-10s */
        @Tooltip
        ActionDuration shockDuration = ActionDuration.S_3;

    }

    /** Secrets settings. */
    @CollapsibleObject @Tooltip
    Secrets secrets = new Secrets();

    /**
     * Configuration settings related to secrets of the mod.
     * This includes the API key and share code.
     */
    static class Secrets {

        /** Whether to use the serial port or web api. */
        @Tooltip
        public boolean useSerialPort = false;

        /** Serial port to use. */
        @Tooltip
        public String serialPort = "";

        /** Id of the shocker to use. */
        @Tooltip
        public int shockerId = 0;

        /** PiShock username. */
        @Tooltip
        String username = "PancakeTAS";

        /** PiShock API key. */
        @Tooltip
        String apiKey = "00000000-0000-0000-0000-000000000000";

        /** PiShock share code. */
        @Tooltip
        String sharecode = "FFFFFFFFFFF";

    }

}
