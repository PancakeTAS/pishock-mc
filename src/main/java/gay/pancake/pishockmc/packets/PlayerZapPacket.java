package gay.pancake.pishockmc.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent by the client and boardcasted to all others when the user got zapped
 *
 * @param intensity Intensity of the zap
 * @param duration Duration of the zap (enum index)
 */
public record PlayerZapPacket(int intensity, int duration) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerZapPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pishockmc", "zap_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerZapPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PlayerZapPacket::intensity,
            ByteBufCodecs.INT, PlayerZapPacket::duration,
            PlayerZapPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
