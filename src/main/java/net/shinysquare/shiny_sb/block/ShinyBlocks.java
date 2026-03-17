package net.shinysquare.shiny_sb.block;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;

import static net.shinysquare.shiny_sb.ShinysHypixelSBremake.REG;

public class ShinyBlocks {
    public static final BlockEntry<Block> TestBlock = REG.block("testblock", Block::new).register();
    public static void register() {
    }
}

