package net.shinysquare.shsbm.world.rift;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shinysquare.shsbm.ShinyHypixelSBRemake;
import net.shinysquare.shsbm.skyblock.StatusBarTracker;

/**
 * Sent from server → client every second while the player is in The Rift.
 * The client side writes the value into StatusBarTracker so the experience
 * bar can display it as a countdown.
 */
public record RiftTimeSyncPacket(int seconds) implements CustomPacketPayload {

    public static final Type<RiftTimeSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ShinyHypixelSBRemake.MOD_ID, "rift_time_sync"));

    public static final StreamCodec<ByteBuf, RiftTimeSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, RiftTimeSyncPacket::seconds,
                    RiftTimeSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Runs on the client's main thread (enqueued via IPayloadContext).
     * Simply pushes the new time into StatusBarTracker.
     */
    public static void handle(RiftTimeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> StatusBarTracker.setRiftTime(packet.seconds()));
    }
}
