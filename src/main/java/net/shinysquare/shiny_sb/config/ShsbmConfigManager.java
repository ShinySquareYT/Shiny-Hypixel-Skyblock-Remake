package net.shinysquare.shiny_sb.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Manages mod config via NeoForge's TOML config system.
 *
 * Usage anywhere in the mod:
 *   ShsbmConfigManager.get().uiAndVisuals.bars.enableBars
 *
 * Call ShsbmConfigManager.register(modEventBus) from ShinysHypixelSBRemake constructor.
 */
public class ShsbmConfigManager {

    // ── Spec + instance built once at class-load time ─────────────────────────

    private static final ModConfigSpec SPEC;
    private static final ShsbmConfig   CONFIG;

    static {
        Pair<ShsbmConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(ShsbmConfig::new);
        CONFIG = pair.getLeft();
        SPEC   = pair.getRight();
    }

    // ── Public accessor ───────────────────────────────────────────────────────

    public static ShsbmConfig get() {
        return CONFIG;
    }

    public static ModConfigSpec getSpec() {
        return SPEC;
    }

    /**
     * Call this from your mod constructor:
     *   ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ShsbmConfigManager.getSpec());
     * or in NeoForge 1.21.1+:
     *   modEventBus.register(ShsbmConfigManager.class); // already done via @EventBusSubscriber
     *   // then in your constructor:
     *   ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ShsbmConfigManager.getSpec());
     */
    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        // Config values are live immediately after this — nothing extra needed
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        // Hot-reload support — values update automatically via ModConfigSpec
    }

    // ── Root config class ─────────────────────────────────────────────────────

    public static class ShsbmConfig {

        public final UIAndVisualsConfig uiAndVisuals;

        // Individual spec values stored here for NeoForge to bind
        // UI + Visuals
        private final ModConfigSpec.BooleanValue enableBars;
        private final ModConfigSpec.BooleanValue enableBarsRift;
        private final ModConfigSpec.BooleanValue enableVanillaStyleManaBar;
        private final ModConfigSpec.BooleanValue riftHealthHP;
        private final ModConfigSpec.EnumValue<UIAndVisualsConfig.IntelligenceDisplay> intelligenceDisplay;

        public ShsbmConfig(ModConfigSpec.Builder builder) {
            uiAndVisuals = new UIAndVisualsConfig();

            builder.comment("UI and Visual settings").push("uiAndVisuals");
            builder.push("bars");

            enableBars = builder
                    .comment("Enable fancy status bars (replaces vanilla health/food/XP bars).")
                    .define("enableBars", true);

            enableBarsRift = builder
                    .comment("Show fancy status bars while inside The Rift dimension.")
                    .define("enableBarsRift", true);

            enableVanillaStyleManaBar = builder
                    .comment("Show a vanilla-notch-style mana bar when fancy bars are disabled.")
                    .define("enableVanillaStyleManaBar", false);

            riftHealthHP = builder
                    .comment("In The Rift, display health as full HP (true) or half-HP hearts (false).")
                    .define("riftHealthHP", true);

            intelligenceDisplay = builder
                    .comment("How overflow mana is shown on the intelligence bar.",
                             "IN_FRONT: overflow renders on top of normal mana.",
                             "ACCURATE: bar width represents total pool including overflow.")
                    .defineEnum("intelligenceDisplay", UIAndVisualsConfig.IntelligenceDisplay.IN_FRONT);

            builder.pop(); // bars
            builder.pop(); // uiAndVisuals
        }

        /**
         * Reads live values from the spec into the UIAndVisualsConfig POJO.
         * Called automatically by NeoForge after load/reload events.
         * You can also call it manually if needed.
         */
        public void bake() {
            uiAndVisuals.bars.enableBars               = enableBars.get();
            uiAndVisuals.bars.enableBarsRift            = enableBarsRift.get();
            uiAndVisuals.bars.enableVanillaStyleManaBar = enableVanillaStyleManaBar.get();
            uiAndVisuals.bars.riftHealthHP              = riftHealthHP.get();
            uiAndVisuals.bars.intelligenceDisplay       = intelligenceDisplay.get();
        }
    }
}
