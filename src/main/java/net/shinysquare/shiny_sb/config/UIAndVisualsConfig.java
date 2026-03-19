package net.shinysquare.shiny_sb.config;

/**
 * Holds all UI and visual config values.
 * Loaded and saved by ShsbmConfigManager.
 */
public class UIAndVisualsConfig {

    public final BarsConfig bars = new BarsConfig();

    // ── Bar sub-config ────────────────────────────────────────────────────────

    public static class BarsConfig {
        /** Master toggle for fancy status bars. */
        public boolean enableBars = true;

        /** Show fancy bars in The Rift dimension too. */
        public boolean enableBarsRift = true;

        /** Show the vanilla notch-style mana bar instead of fancy bars. */
        public boolean enableVanillaStyleManaBar = false;

        /** In The Rift, show health as raw HP (true) or half-HP (false). */
        public boolean riftHealthHP = true;

        /** How to display intelligence/mana overflow on the bar. */
        public IntelligenceDisplay intelligenceDisplay = IntelligenceDisplay.IN_FRONT;

        /** Legacy bar positions — used as defaults before a saved config exists. */
        public LegacyBarPositions barPositions = new LegacyBarPositions();
    }

    // ── Intelligence display mode ─────────────────────────────────────────────

    public enum IntelligenceDisplay {
        /**
         * Overflow mana renders in front of (on top of) normal mana.
         * Gives the impression the bar is "overfilled".
         */
        IN_FRONT,

        /**
         * The bar width represents total mana including overflow;
         * each segment is proportional to the whole pool.
         */
        ACCURATE
    }

    // ── Legacy positions (used as initial defaults) ───────────────────────────

    public static class LegacyBarPositions {
        public LegacyBarPosition healthBarPosition     = LegacyBarPosition.LAYER1;
        public LegacyBarPosition manaBarPosition       = LegacyBarPosition.LAYER1;
        public LegacyBarPosition defenceBarPosition    = LegacyBarPosition.RIGHT;
        public LegacyBarPosition experienceBarPosition = LegacyBarPosition.LAYER2;
    }

    public enum LegacyBarPosition {
        /** Above the hotbar, first row. */
        LAYER1,
        /** Above the hotbar, second row. */
        LAYER2,
        /** To the right of the hotbar. */
        RIGHT
    }
}
