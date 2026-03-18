package net.shinysquare.shiny_sb.skyblock;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;
import net.shinysquare.shiny_sb.register.ShsbmAttributes;
import net.shinysquare.shiny_sb.utils.Utils;

/**
 * Central stat store for all fancy status bars.
 *
 * ── Design ──────────────────────────────────────────────────────────────────
 * Health and mana are NOT the same as vanilla HP and food. They are Skyblock
 * stats that scale far beyond vanilla limits. This class holds them as plain
 * integers and provides static setters so any game system (ability handlers,
 * network packets, scoreboard readers, etc.) can update them at any time.
 *
 * Values are read by FancyStatusBars every render frame.
 *
 * ── Mana vs Overflow Mana ───────────────────────────────────────────────────
 * Mana     — the normal pool, fills up to MAX_MANA.
 * Overflow  — bonus mana beyond the cap (e.g. from certain abilities). Shown
 *             on the bar as a second color layer on top of normal mana.
 *
 * ── Auto-sync from vanilla ──────────────────────────────────────────────────
 * On every client tick we read vanilla HP and the MAX_MANA attribute so the
 * bars always have a baseline value even if no ability system has run yet.
 * Any explicit setter call overrides these auto-synced values for that tick.
 */
@Mod.EventBusSubscriber(modid = ShinysHypixelSBRemake.MOD_ID, bus = Mod.EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class StatusBarTracker {

    // ── Internal state ────────────────────────────────────────────────────────

    private static int health        = 100;
    private static int maxHealth     = 100;

    private static int mana          = 100;
    private static int maxMana       = 100;
    private static int overflowMana  = 0;
    private static boolean manaEstimated = false;

    private static int defense       = 0;

    private static int speed         = 100;
    private static int maxSpeed      = 400;   // Skyblock speed cap

    private static int air           = 300;
    private static int maxAir        = 300;

    // Tracks whether "current" values were explicitly set this tick (true) or
    // came from auto-sync (false). Setters flip this; the tick event resets it.
    private static boolean healthSetThisTick = false;
    private static boolean manaSetThisTick   = false;

    // ── Resource record ───────────────────────────────────────────────────────

    /**
     * A snapshot of a stat: current value, maximum, and optional overflow.
     * Overflow is used by the mana bar to show bonus mana with a separate color.
     */
    public record Resource(int value, int max, int overflow) {
        /** Convenience: no overflow */
        public Resource(int value, int max) { this(value, max, 0); }
    }

    // ── Auto-sync on client tick ──────────────────────────────────────────────

    /**
     * Runs every client tick. Reads vanilla HP and the MAX_MANA attribute as a
     * baseline. Explicit setters called this tick take priority.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // --- Health ---
        if (!healthSetThisTick) {
            // In The Rift vanilla HP maps 1:1 (handled in FancyStatusBars.render)
            // Outside the rift we just use vanilla HP as a fallback baseline.
            health    = Math.round(player.getHealth());
            maxHealth = Math.round(player.getMaxHealth());
        }
        healthSetThisTick = false;

        // --- Mana cap from attribute ---
        if (!manaSetThisTick) {
            // Read MAX_MANA attribute if present; fall back to stored value
            var maxManaAttr = player.getAttributes().getInstance(ShsbmAttributes.MAX_MANA.get());
            if (maxManaAttr != null) {
                maxMana = (int) maxManaAttr.getValue();
                // Clamp current mana in case the cap was lowered
                mana = Math.min(mana, maxMana);
            }
        }
        manaSetThisTick = false;

        // --- Air ---
        // Sync air from vanilla when underwater; reset when surfaced.
        if (player.isUnderWater()) {
            air    = player.getAirSupply();
            maxAir = player.getMaxAirSupply();
        } else {
            air    = maxAir; // full when not underwater
        }
    }

    // ── Setters ── call these from ability handlers, packets, scoreboard, etc. ─

    /**
     * Override the health values for this tick (takes priority over auto-sync).
     */
    public static void setHealth(int value, int max) {
        health    = value;
        maxHealth = max;
        healthSetThisTick = true;
    }

    /**
     * Set both mana and overflow mana explicitly.
     *
     * @param value    current mana (0 … max)
     * @param max      maximum mana pool
     * @param overflow bonus mana above the cap (0 if none)
     */
    public static void setMana(int value, int max, int overflow) {
        mana         = value;
        maxMana      = max;
        overflowMana = overflow;
        manaSetThisTick = true;
    }

    /** Convenience: set mana with no overflow. */
    public static void setMana(int value, int max) {
        setMana(value, max, 0);
    }

    /**
     * Add or subtract mana. Negative delta spends mana (e.g. casting an ability).
     * Overflow is reduced first, then regular mana. Clamped to [0, max + overflow].
     *
     * @param delta amount to add (positive) or spend (negative)
     */
    public static void addMana(int delta) {
        if (delta < 0) {
            // Spend overflow first
            int spend = -delta;
            if (overflowMana > 0) {
                int fromOverflow = Math.min(spend, overflowMana);
                overflowMana -= fromOverflow;
                spend -= fromOverflow;
            }
            mana = Math.max(0, mana - spend);
        } else {
            // Fill regular mana first, remainder goes to overflow
            int space = maxMana - mana;
            if (delta <= space) {
                mana += delta;
            } else {
                mana = maxMana;
                overflowMana = Math.min(overflowMana + (delta - space), maxMana); // overflow cap = maxMana
            }
        }
        manaSetThisTick = true;
    }

    /**
     * Mark current mana display as estimated (shown as "~value" on the bar).
     * Use this when you can only approximate mana (e.g. reading from a scoreboard).
     */
    public static void setManaEstimated(boolean estimated) {
        manaEstimated = estimated;
    }

    public static void setDefense(int value) {
        defense = Math.max(0, value);
    }

    public static void setSpeed(int value, int max) {
        speed    = value;
        maxSpeed = max;
    }

    /** Override air supply directly (normally auto-synced from vanilla). */
    public static void setAir(int value, int max) {
        air    = value;
        maxAir = max;
    }

    // ── Getters (called by FancyStatusBars every render frame) ────────────────

    public static Resource getHealth()           { return new Resource(health, maxHealth, 0); }

    public static Resource getMana()             { return new Resource(mana, maxMana, overflowMana); }

    /** True if mana value is an estimate (e.g. read from scoreboard, not exact). */
    public static boolean isManaEstimated()      { return manaEstimated; }

    public static int getDefense()               { return defense; }

    public static Resource getSpeed()            { return new Resource(speed, maxSpeed, 0); }

    public static Resource getAir()              { return new Resource(air, maxAir, 0); }

    // ── Rift-specific helpers ─────────────────────────────────────────────────

    /**
     * In The Rift the experience bar repurposed as a "time remaining" display.
     * This is the number of seconds the player has left before being expelled.
     * Set this from whatever system manages rift time (e.g. a custom game rule
     * or a packet from the server).
     */
    private static int riftTimeSeconds = 0;

    public static void setRiftTime(int seconds) { riftTimeSeconds = seconds; }
    public static int  getRiftTime()             { return riftTimeSeconds; }
}
