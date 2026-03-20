package net.shinysquare.shiny_sb.register;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shinysquare.shiny_sb.ShinysHypixelSBRemake;

import net.minecraft.world.entity.EntityType;
public class ShsbmAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, ShinysHypixelSBRemake.MOD_ID);

    /**
     * Maximum mana pool. Default 100, cap 2048 (same scale Hypixel Skyblock uses).
     * Stored as an entity attribute so it integrates properly with the vanilla
     * attribute modifier system (gear bonuses, potions, etc.).
     * Marked syncable so the client always has the correct value.
     */
    public static final DeferredHolder<Attribute, RangedAttribute> MAX_MANA =
            ATTRIBUTES.register("max_mana",
                    () -> (RangedAttribute) new RangedAttribute(
                            "attribute.shsbm.max_mana", 100.0, 0.0, 2048.0)
                            .setSyncable(true));

    /**
     * Register the DeferredRegister on the MOD event bus.
     * Call this from your main mod constructor:
     *   ShsbmAttributes.register(modEventBus);
     */
    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        // Add MAX_MANA to all players once attributes are finalized
        NeoForge.EVENT_BUS.addListener(ShsbmAttributes::onAttributeModification);
    }

    /**
     * Adds MAX_MANA to every player entity so it's always available for
     * StatusBarTracker to read without null-checks.
     */
    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_MANA);
    }
}
