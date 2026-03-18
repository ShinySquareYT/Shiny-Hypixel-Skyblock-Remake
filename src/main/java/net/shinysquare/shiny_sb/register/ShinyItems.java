package net.shinysquare.shiny_sb.register;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

import static net.shinysquare.shiny_sb.ShinysHypixelSBremake.REG;

@SuppressWarnings("unused")
public class ShinyItems {
    public static final ItemEntry<Item> TESTITEM = REG.item("testitem", Item::new).register();
    public static void register() {
    }
}
