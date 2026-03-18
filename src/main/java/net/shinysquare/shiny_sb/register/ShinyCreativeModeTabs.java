package net.shinysquare.shiny_sb.register;

import com.simibubi.create.AllCreativeModeTabs;
import net.shinysquare.shiny_sb.content.items.packages.BurstPackageItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import static net.shinysquare.shiny_sb.ShinysHypixelSBRemake.*;

@SuppressWarnings("all")
public class ShinyCreativeModeTabs {

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_TAB = REGISTER.creativeTab().register("base_tab", () -> CreativeModeTab.builder()
            .title(Component.translatableWithFallback("itemGroup." + MOD_ID + ".base", NAME))
            .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
            .icon(ShinyBlocks.TESTBLOCK::asStack)
            .build());

    public static void register() {}

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ShinyCreativeModeTabs.BASE_TAB.getKey())) for (var entry : REG.getAll(Registries.ITEM)) if (entry.get() instanceof BurstPackageItem item) event.remove(item.getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}