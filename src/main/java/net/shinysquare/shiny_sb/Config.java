package net.shinysquare.shiny_sb;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public enum IntelligenceDisplay { IN_FRONT, ACCURATE }

    public static final ModConfigSpec.BooleanValue ENABLE_BARS = BUILDER
            .comment("Enable fancy status bars (replaces vanilla health/food/XP bars)")
            .define("enableBars", true);

    public static final ModConfigSpec.BooleanValue ENABLE_BARS_RIFT = BUILDER
            .comment("Show fancy bars while inside The Rift dimension")
            .define("enableBarsRift", true);

    public static final ModConfigSpec.BooleanValue ENABLE_VANILLA_MANA_BAR = BUILDER
            .comment("Show vanilla notch-style mana bar when fancy bars are disabled")
            .define("enableVanillaStyleManaBar", false);

    public static final ModConfigSpec.BooleanValue RIFT_HEALTH_HP = BUILDER
            .comment("In The Rift, display health as full HP (true) or half-hearts (false)")
            .define("riftHealthHP", true);

    public static final ModConfigSpec.EnumValue<IntelligenceDisplay> INTELLIGENCE_DISPLAY = BUILDER
            .comment("How overflow mana is shown. IN_FRONT: renders on top. ACCURATE: proportional width.")
            .defineEnum("intelligenceDisplay", IntelligenceDisplay.IN_FRONT);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}