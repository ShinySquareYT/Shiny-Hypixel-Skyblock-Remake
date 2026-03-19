package net.shinysquare.shiny_sb.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;

/**
 * Holds ResourceKeys for custom dimensions.
 *
 * The actual dimension definitions live in:
 *   src/main/resources/data/shsbm/dimension/the_rift.json       (chunk generator + biome)
 *   src/main/resources/data/shsbm/dimension_type/the_rift.json  (light, sky, effects)
 *
 * No Java-side registration is needed for data-driven dimensions in NeoForge 1.21.1 —
 * the JSON files are picked up automatically. This class simply provides the typed key
 * so the rest of the code can reference the dimension without string literals.
 *
 * To actually SEND the player to The Rift, call:
 *   ServerPlayer.teleportTo(ServerLevel for THE_RIFT, x, y, z, yaw, pitch)
 * from whatever game system handles zone transitions.
 */
public class ModDimensions {

    public static final ResourceKey<Level> THE_RIFT = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "the_rift")
    );

    // Keep in sync with Utils.THE_RIFT — both point to the same location.
    // You can collapse them into one later if you prefer a single source of truth.
}
