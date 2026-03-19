package net.shinysquare.shiny_sb.content.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;

public class Utils {

    /**
     * ResourceKey for The Rift custom dimension.
     * Matches data/shsbm/dimension/the_rift.json
     */
    public static final ResourceKey<Level> THE_RIFT = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ShinysHypixelSBRemake.MOD_ID, "the_rift")
    );

    /**
     * Returns true when the client player is currently inside The Rift dimension.
     * Used by ExperienceStatusBar to swap to the clock icon and time display,
     * and by FancyStatusBars to switch the health/experience bar behaviour.
     */
    public static boolean isInTheRift() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(THE_RIFT);
    }
}
