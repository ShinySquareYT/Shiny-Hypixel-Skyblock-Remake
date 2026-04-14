package net.shinysquare.shiny_sb.content.items;

import net.shinysquare.shiny_sb.register.ShinyStats;

public interface ShinySBItem {
    void applyStatBonuses(java.util.function.BiConsumer<ShinyStats.StatDefinition, Integer> addBonus);
}
