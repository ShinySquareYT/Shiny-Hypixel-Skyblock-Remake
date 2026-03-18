package net.shinysquare.shiny_sb.register;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;

public class ShsbmAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ShinysHypixelSBRemake.MOD_ID);

    /**
     * Stores the number of rift-time seconds remaining for a player.
     *
     * Default 0 (not in The Rift / no time assigned yet).
     * Serialized to NBT so the value survives log-out/log-in.
     *
     * Set this when a player enters The Rift:
     *   player.setData(ShsbmAttachments.RIFT_TIME, 360);  // 6 minutes
     *
     * Read it:
     *   int seconds = player.getData(ShsbmAttachments.RIFT_TIME);
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> RIFT_TIME =
            ATTACHMENT_TYPES.register("rift_time",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
