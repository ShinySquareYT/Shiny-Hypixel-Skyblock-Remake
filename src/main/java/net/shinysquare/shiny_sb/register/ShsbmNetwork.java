package net.shinysquare.shiny_sb.register;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;
import net.shinysquare.shiny_sb.world.rift.RiftTimeSyncPacket;

/**
 * Registers all custom network packets.
 *
 * New packets: add a playToClient / playToServer line here and create
 * the corresponding record implementing CustomPacketPayload.
 */
@EventBusSubscriber(modid = ShinysHypixelSBRemake.MOD_ID, value = Dist.CLIENT)
public class ShsbmNetwork {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // "1" is the protocol version string — bump this if you change a packet's shape
        PayloadRegistrar registrar = event.registrar("1");

        // Rift time countdown: server → client, once per second while in The Rift
        registrar.playToClient(
                RiftTimeSyncPacket.TYPE,
                RiftTimeSyncPacket.STREAM_CODEC,
                RiftTimeSyncPacket::handle);
    }
}
