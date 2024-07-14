package gay.pancake.pishockmc.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent when a player is hurt or killed
 *
 * @param damage Damage amount
 * @param die Whether the player dies
 */
public record PlayerHurtPacket(float damage, boolean die) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerHurtPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pishockmc", "damage_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerHurtPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PlayerHurtPacket::damage,
            ByteBufCodecs.BOOL, PlayerHurtPacket::die,
            PlayerHurtPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
