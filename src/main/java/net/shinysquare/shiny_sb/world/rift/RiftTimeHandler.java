package net.shinysquare.shiny_sb.world.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;
import net.shinysquare.shiny_sb.register.ShsbmAttachments;
import net.shinysquare.shiny_sb.content.utils.Utils;

/**
 * Server-side handler for The Rift time mechanic.
 *
 * ── How it works ────────────────────────────────────────────────────────────
 *  1. When a player enters The Rift (PlayerChangedDimensionEvent) they are
 *     given RIFT_ENTRY_SECONDS of rift time if they have none.
 *
 *  2. Every server tick we check whether the player is in The Rift.
 *     Every 20 ticks (1 second) we decrement the stored time by 1 and
 *     sync it to the client via RiftTimeSyncPacket.
 *
 *  3. When time reaches 0 the player is immediately teleported back to
 *     the overworld at their stored spawn point (or the world spawn if
 *     they have no bed/respawn anchor set).
 *
 * ── Entry time ───────────────────────────────────────────────────────────────
 *  Change RIFT_ENTRY_SECONDS to whatever you want the default stay to be.
 *  You can also call player.setData(ShsbmAttachments.RIFT_TIME, n) from any
 *  other system (e.g. a portal block, an ability, a quest reward) to adjust
 *  the timer at any point.
 *
 * ── Server-side only ─────────────────────────────────────────────────────────
 *  This class is annotated with Bus.GAME and has no Dist restriction because
 *  it must also run on a dedicated server. All code here is server-side;
 *  the client only receives the sync packet.
 */
public class RiftTimeHandler {

    /** Default time (in seconds) a player gets when first entering The Rift. */
    public static final int RIFT_ENTRY_SECONDS = 360; // 6 minutes

    /**
     * Counts ticks per player. We only update every 20 ticks to avoid
     * sending a packet every single tick.
     * Stored as a thread-local counter via a simple mod-arithmetic check
     * on the server's tick count.
     */
    private static final int TICKS_PER_SECOND = 20;

    // ── Player enters a dimension ─────────────────────────────────────────────

    /**
     * When a player enters The Rift for the first time (or with no stored time),
     * assign them the default rift time. This fires AFTER the dimension change,
     * so player.level() already reflects the new dimension.
     */
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean enteringRift = event.getTo().equals(Utils.THE_RIFT);
        boolean leavingRift  = event.getFrom().equals(Utils.THE_RIFT);

        if (enteringRift) {
            // Only assign time if the player has none left (fresh entry)
            int current = player.getData(ShsbmAttachments.RIFT_TIME);
            if (current <= 0) {
                player.setData(ShsbmAttachments.RIFT_TIME, RIFT_ENTRY_SECONDS);
            }
            // Immediately sync so the client bar shows the correct value
            syncTime(player);
        }

        if (leavingRift) {
            // Clear stored time when the player exits normally (e.g. via a portal)
            player.setData(ShsbmAttachments.RIFT_TIME, 0);
            // Tell the client the bar should show 0
            syncTime(player);
        }
    }

    // ── Per-tick update ───────────────────────────────────────────────────────

    /**
     * Runs every server tick for every online player.
     * We decrement rift time by 1 once per second (every 20 ticks).
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(Utils.THE_RIFT)) return;

        // Fire once per second: when the server tick count is divisible by 20
        long serverTick = player.serverLevel().getServer().getTickCount();
        if (serverTick % TICKS_PER_SECOND != 0) return;

        int current = player.getData(ShsbmAttachments.RIFT_TIME);

        if (current <= 0) {
            // Already at zero — eject if not already being ejected
            ejectToOverworld(player);
            return;
        }

        int newTime = current - 1;
        player.setData(ShsbmAttachments.RIFT_TIME, newTime);
        syncTime(player);

        if (newTime <= 0) {
            // Warn the player the tick they hit zero, then eject next tick
            player.sendSystemMessage(
                    Component.translatable("shsbm.rift.time_expired"));
            ejectToOverworld(player);
        } else if (newTime <= 30) {
            // Warn every 10 seconds when under 30s remaining
            if (newTime % 10 == 0 || newTime == 5 || newTime == 3 || newTime == 1) {
                player.sendSystemMessage(
                        Component.translatable("shsbm.rift.time_warning", newTime));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Sends the current rift time to the client so StatusBarTracker
     * and the experience bar can display it.
     */
    private static void syncTime(ServerPlayer player) {
        int time = player.getData(ShsbmAttachments.RIFT_TIME);
        PacketDistributor.sendToPlayer(player, new RiftTimeSyncPacket(time));
    }

    /**
     * Teleports the player back to the overworld.
     *
     * Priority for destination:
     *  1. The player's personal spawn point (bed / respawn anchor), if valid.
     *  2. The overworld's world spawn.
     */
    private static void ejectToOverworld(ServerPlayer player) {
        // Safety: don't double-eject if the player is already leaving
        if (!player.level().dimension().equals(Utils.THE_RIFT)) return;

        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        // Clear rift time so we don't keep firing eject
        player.setData(ShsbmAttachments.RIFT_TIME, 0);

        // Try personal spawn first
        BlockPos respawn = player.getRespawnPosition();
        ServerLevel respawnLevel = respawn != null
                ? player.getServer().getLevel(player.getRespawnDimension())
                : null;

        if (respawn != null && respawnLevel != null) {
            player.teleportTo(respawnLevel,
                    respawn.getX() + 0.5,
                    respawn.getY(),
                    respawn.getZ() + 0.5,
                    player.getRespawnAngle(), 0f);
            return;
        }

        // Fall back to overworld spawn
        BlockPos worldSpawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld,
                worldSpawn.getX() + 0.5,
                worldSpawn.getY(),
                worldSpawn.getZ() + 0.5,
                0f, 0f);
    }
}
