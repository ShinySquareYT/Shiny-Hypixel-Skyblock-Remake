package net.shinysquare.shiny_sb;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.shinysquare.shiny_sb.content.utils.ShinyRegistry;
import net.shinysquare.shiny_sb.register.ShinyBlocks;
import net.shinysquare.shiny_sb.register.ShinyItems;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.shinysquare.shiny_sb.register.ShinyCreativeModeTabs.BASE_TAB;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ShinysHypixelSBRemake.MOD_ID)
public class ShinysHypixelSBRemake {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "shiny_sb";
    public static final String NAME = "Shiny's Hypixel Skyblock Remake";
    public static final ShinyRegistry REGISTER = new ShinyRegistry(MOD_ID);
    // Directly reference a slf4j logger
    public static final CreateRegistrate REG = CreateRegistrate.create(MOD_ID);
    public static final Logger LOGGER = LogUtils.getLogger();
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ShinysHypixelSBRemake(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        REG.defaultCreativeTab(BASE_TAB, "base_tab");
        ShinyItems.register();
        ShinyBlocks.register();
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ShinysHypixelSBRemake) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
