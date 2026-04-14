package net.shinysquare.shiny_sb.register;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.shinysquare.shiny_sb.skyblock.StatusBarTracker;
import net.shinysquare.shiny_sb.content.items.ShinySBItem;
import java.util.function.Function;

/**
 * Central registry for all Skyblock stats.
 *
 * ── How to add a new stat ────────────────────────────────────────────────────
 * 1. Add a StatDefinition field below using StatDefinition.of(...)
 * 2. Add it to the ALL array
 * 3. ShinyStats.updateAll(player) is called automatically every tick
 *
 * ── Formula syntax ───────────────────────────────────────────────────────────
 * ctx.base(STAT)    → base value of another stat
 * ctx.total(STAT)   → total value of another stat
 * ctx.bonus("Name") → total gear bonus for a stat by name
 * ctx.player        → the Player entity
 * ctx.vanillaHp()   → current vanilla HP
 * ctx.vanillaMaxHp()→ vanilla max HP
 */
public class ShinyStats {

    // ── Core stats ────────────────────────────────────────────────────────────

    public static final StatDefinition HEALTH = StatDefinition.of(
            "Health",
            ctx -> (int) ctx.vanillaMaxHp(),
            (base, ctx) -> base
    );
    public static final StatDefinition DEFENSE = StatDefinition.of(
            "Defense",
            ctx -> 0,
            (base, ctx) -> base
    );
    public static final StatDefinition INTELLIGENCE = StatDefinition.of(
            "Intelligence",
            ctx -> 100,
            (base, ctx) -> base
    );
    public static final StatDefinition SPEED = StatDefinition.of(
            "Speed",
            ctx -> 100,
            (base, ctx) -> base
    );
    public static final StatDefinition EHP = StatDefinition.of(
            "EHP",
            ctx -> (int) ctx.vanillaMaxHp(),
            (base, ctx) -> ctx.total(HEALTH) * ((ctx.total(DEFENSE) + 100) / 100)
    );

    // ── Melee stats ───────────────────────────────────────────────────────────

    public static final StatDefinition PLAYERSTRENGTH = StatDefinition.of(
            "PlayerStrength", ctx -> 0, (base, ctx) -> 0);
    public static final StatDefinition MELEEWEAPONBASEDMG = StatDefinition.of(
            "MeleeWeaponBaseDMG", ctx -> 0, (base, ctx) -> base + ctx.bonus("MeleeWeaponBaseDMG"));
    public static final StatDefinition MELEEWEAPONBASESTR = StatDefinition.of(
            "MeleeWeaponBaseSTR", ctx -> 0, (base, ctx) -> base + ctx.bonus("MeleeWeaponBaseSTR"));
    public static final StatDefinition MELEEWEAPONBASECRITDMG = StatDefinition.of(
            "MeleeWeaponBaseCritDMG", ctx -> 0, (base, ctx) -> base + ctx.bonus("MeleeWeaponBaseCritDMG"));
    public static final StatDefinition MELEEWEAPONBASECRITCHANCE = StatDefinition.of(
            "MeleeWeaponBaseCritChance", ctx -> 0, (base, ctx) -> base + ctx.bonus("MeleeWeaponBaseCritChance"));
    public static final StatDefinition FINALSTRENGTH = StatDefinition.of(
            "FinalStrength", ctx -> 0, (base, ctx) -> ctx.total(PLAYERSTRENGTH) + ctx.total(MELEEWEAPONBASESTR));
    public static final StatDefinition FINALMELEEDMG = StatDefinition.of(
            "FinalMeleeDMG", ctx -> 0, (base, ctx) -> 0);

    // ── Global farming fortune ────────────────────────────────────────────────

    public static final StatDefinition
            BASEFARMINGFORTUNE                   = StatDefinition.of("BaseFarmingFortune",                   ctx -> 0, (base, ctx) -> 0),
            GREENHOUSEFARMINGFORTUNE             = StatDefinition.of("GreenhouseFarmingFortune",             ctx -> 0, (base, ctx) -> 0),
            BONUSARMORFARMINGFORTUNE             = StatDefinition.of("BonusArmorFarmingFortune",             ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLEFARMINGFORTUNE             = StatDefinition.of("BonusToolFortune",                     ctx -> 0, (base, ctx) -> 0),
            BONUSEQUIPMENTFARMINGFORTUNE         = StatDefinition.of("BonusEquipmentFarmingFortune",         ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTSFARMINGFORTUNE          = StatDefinition.of("BonusEnchantsFarmingFortune",          ctx -> 0, (base, ctx) -> 0),
            BONUSREFORGEFARMINGFORTUNE           = StatDefinition.of("BonusReforgeFarmingFortune",           ctx -> 0, (base, ctx) -> 0),
            BONUSPETFARMINGFORTUNE               = StatDefinition.of("BonusPetFarmingFortune",               ctx -> 0, (base, ctx) -> 0),
            BONUSPETITEMFARMINGFORTUNE           = StatDefinition.of("BonusPetItemFarmingFortune",           ctx -> 0, (base, ctx) -> 0),
            BONUSACCFARMINGFORTUNE               = StatDefinition.of("BonusACCFarmingFortune",               ctx -> 0, (base, ctx) -> 0),
            BONUSFARMINGFORDUMMIESFARMINGFORTUNE = StatDefinition.of("BonusFarmingForDummiesFarmingFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSCAKEFARMINGFORTUNE              = StatDefinition.of("BonusCakeFarmingFortune",              ctx -> 0, (base, ctx) -> 0),
            BONUSPOTIONFARMINGFORTUNE            = StatDefinition.of("BonusPotionFarmingFortune",            ctx -> 0, (base, ctx) -> 0),
            BONUSPESTHUNTERFARMINGFORTUNE        = StatDefinition.of("BonusPesthunterFarmingFortune",        ctx -> 0, (base, ctx) -> 0),
            BONUSDARKCOCOAFARMINGFORTUNE         = StatDefinition.of("BonusDarkCocoaFarmingFortune",         ctx -> 0, (base, ctx) -> 0),
            BONUSREFINEDDARKCOCOAFARMINGFORTUNE  = StatDefinition.of("BonusRefinedDarkCocoaFarmingFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSROSEWATERFLASKFARMINGFORTUNE    = StatDefinition.of("BonusRosewaterFlaskFarmingFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSSKILLLEVELFARMINGFORTUNE        = StatDefinition.of("BonusSkillLevelFarmingFortune",        ctx -> 0, (base, ctx) -> 0),
            FINALGLOBALFARMINGFORTUNE = StatDefinition.of("FinalGlobalFarmingFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BASEFARMINGFORTUNE) + ctx.total(GREENHOUSEFARMINGFORTUNE) +
                            ctx.total(BONUSARMORFARMINGFORTUNE) + ctx.total(BONUSTOOLEFARMINGFORTUNE) +
                            ctx.total(BONUSEQUIPMENTFARMINGFORTUNE) + ctx.total(BONUSENCHANTSFARMINGFORTUNE) +
                            ctx.total(BONUSREFORGEFARMINGFORTUNE) + ctx.total(BONUSPETFARMINGFORTUNE) +
                            ctx.total(BONUSPETITEMFARMINGFORTUNE) + ctx.total(BONUSACCFARMINGFORTUNE) +
                            ctx.total(BONUSFARMINGFORDUMMIESFARMINGFORTUNE) + ctx.total(BONUSCAKEFARMINGFORTUNE) +
                            ctx.total(BONUSPOTIONFARMINGFORTUNE) + ctx.total(BONUSPESTHUNTERFARMINGFORTUNE) +
                            ctx.total(BONUSDARKCOCOAFARMINGFORTUNE) + ctx.total(BONUSREFINEDDARKCOCOAFARMINGFORTUNE) +
                            ctx.total(BONUSROSEWATERFLASKFARMINGFORTUNE) + ctx.total(BONUSSKILLLEVELFARMINGFORTUNE));
    // ── BONUSTOOL(crop)FORTUNE — all crops ───────────────────────────────────

    public static final StatDefinition
            BONUSTOOLWHEATFORTUNE      = StatDefinition.of("BonusToolWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLCARROTFORTUNE     = StatDefinition.of("BonusToolCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLPOTATOFORTUNE     = StatDefinition.of("BonusToolPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLPUMPKINFORTUNE    = StatDefinition.of("BonusToolPumpkinFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLSUGARCANEFORTUNE  = StatDefinition.of("BonusToolSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLMELONSLICEFORTUNE = StatDefinition.of("BonusToolMelonSliceFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLCACTUSFORTUNE     = StatDefinition.of("BonusToolCactusFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLCOCOABEANSFORTUNE = StatDefinition.of("BonusToolCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLMUSHROOMFORTUNE   = StatDefinition.of("BonusToolMushroomFortune",   ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLNETHERWARTFORTUNE = StatDefinition.of("BonusToolNetherWartFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLSUNFLOWERFORTUNE  = StatDefinition.of("BonusToolSunflowerFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLMOONFLOWERFORTUNE = StatDefinition.of("BonusToolMoonflowerFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSTOOLWILDROSEFORTUNE   = StatDefinition.of("BonusToolWildRoseFortune",   ctx -> 0, (base, ctx) -> 0);

    // ── BONUSLOGARITHM(crop)FORTUNE — Wheat, Carrot, Potato, SugarCane, NetherWart only ──

    public static final StatDefinition
            BONUSLOGARITHMWHEATFORTUNE      = StatDefinition.of("BonusLogarithmWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSLOGARITHMCARROTFORTUNE     = StatDefinition.of("BonusLogarithmCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSLOGARITHMPOTATOFORTUNE     = StatDefinition.of("BonusLogarithmPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSLOGARITHMSUGARCANEFORTUNE  = StatDefinition.of("BonusLogarithmSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSLOGARITHMNETHERWARTFORTUNE = StatDefinition.of("BonusLogarithmNetherWartFortune", ctx -> 0, (base, ctx) -> 0);

    // ── BONUSACC(crop)FORTUNE — all crops ────────────────────────────────────

    public static final StatDefinition
            BONUSACCWHEATFORTUNE      = StatDefinition.of("BonusACCWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSACCCARROTFORTUNE     = StatDefinition.of("BonusACCCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSACCPOTATOFORTUNE     = StatDefinition.of("BonusACCPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSACCPUMPKINFORTUNE    = StatDefinition.of("BonusACCPumpkinFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSACCSUGARCANEFORTUNE  = StatDefinition.of("BonusACCSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSACCMELONSLICEFORTUNE = StatDefinition.of("BonusACCMelonSliceFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSACCCACTUSFORTUNE     = StatDefinition.of("BonusACCCactusFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSACCCOCOABEANSFORTUNE = StatDefinition.of("BonusACCCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSACCMUSHROOMFORTUNE   = StatDefinition.of("BonusACCMushroomFortune",   ctx -> 0, (base, ctx) -> 0),
            BONUSACCNETHERWARTFORTUNE = StatDefinition.of("BonusACCNetherWartFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSACCSUNFLOWERFORTUNE  = StatDefinition.of("BonusACCSunflowerFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSACCMOONFLOWERFORTUNE = StatDefinition.of("BonusACCMoonflowerFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSACCWILDROSEFORTUNE   = StatDefinition.of("BonusACCWildRoseFortune",   ctx -> 0, (base, ctx) -> 0);

    // ── BONUSENCHANT(crop)FORTUNE — all crops ─────────────────────────────────

    public static final StatDefinition
            BONUSENCHANTWHEATFORTUNE      = StatDefinition.of("BonusEnchantWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTCARROTFORTUNE     = StatDefinition.of("BonusEnchantCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTPOTATOFORTUNE     = StatDefinition.of("BonusEnchantPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTPUMPKINFORTUNE    = StatDefinition.of("BonusEnchantPumpkinFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTSUGARCANEFORTUNE  = StatDefinition.of("BonusEnchantSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTMELONSLICEFORTUNE = StatDefinition.of("BonusEnchantMelonSliceFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTCACTUSFORTUNE     = StatDefinition.of("BonusEnchantCactusFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTCOCOABEANSFORTUNE = StatDefinition.of("BonusEnchantCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTMUSHROOMFORTUNE   = StatDefinition.of("BonusEnchantMushroomFortune",   ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTNETHERWARTFORTUNE = StatDefinition.of("BonusEnchantNetherWartFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTSUNFLOWERFORTUNE  = StatDefinition.of("BonusEnchantSunflowerFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTMOONFLOWERFORTUNE = StatDefinition.of("BonusEnchantMoonflowerFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTWILDROSEFORTUNE   = StatDefinition.of("BonusEnchantWildRoseFortune",   ctx -> 0, (base, ctx) -> 0);

    // ── BONUSANITAPERSONALBESTS(crop)FORTUNE — all crops ─────────────────────

    public static final StatDefinition
            BONUSANITAPERSONALBESTSWHEATFORTUNE      = StatDefinition.of("BonusAnitaPersonalBestsWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSCARROTFORTUNE     = StatDefinition.of("BonusAnitaPersonalBestsCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSPOTATOFORTUNE     = StatDefinition.of("BonusAnitaPersonalBestsPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSPUMPKINFORTUNE    = StatDefinition.of("BonusAnitaPersonalBestsPumpkinFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSSUGARCANEFORTUNE  = StatDefinition.of("BonusAnitaPersonalBestsSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSMELONSLICEFORTUNE = StatDefinition.of("BonusAnitaPersonalBestsMelonSliceFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSCACTUSFORTUNE     = StatDefinition.of("BonusAnitaPersonalBestsCactusFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSCOCOABEANSFORTUNE = StatDefinition.of("BonusAnitaPersonalBestsCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSMUSHROOMFORTUNE   = StatDefinition.of("BonusAnitaPersonalBestsMushroomFortune",   ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSNETHERWARTFORTUNE = StatDefinition.of("BonusAnitaPersonalBestsNetherWartFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSSUNFLOWERFORTUNE  = StatDefinition.of("BonusAnitaPersonalBestsSunflowerFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSMOONFLOWERFORTUNE = StatDefinition.of("BonusAnitaPersonalBestsMoonflowerFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITAPERSONALBESTSWILDROSEFORTUNE   = StatDefinition.of("BonusAnitaPersonalBestsWildRoseFortune",   ctx -> 0, (base, ctx) -> 0);

    // ── BONUSCOLLECTIONANALYSIS(crop)FORTUNE — Wheat, Carrot, Potato, SugarCane, NetherWart only ──

    public static final StatDefinition
            BONUSCOLLECTIONANALYSISWHEATFORTUNE     = StatDefinition.of("BonusCollectionAnalysisWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSCOLLECTIONANALYSISCARROTFORTUNE    = StatDefinition.of("BonusCollectionAnalysisCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSCOLLECTIONANALYSISPOTATOFORTUNE    = StatDefinition.of("BonusCollectionAnalysisPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSCOLLECTIONANALYSISSUGARCANEFORTUNE = StatDefinition.of("BonusCollectionAnalysisSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSCOLLECTIONANALYSISNETHERWARTFORTUNE= StatDefinition.of("BonusCollectionAnalysisNetherWartFortune", ctx -> 0, (base, ctx) -> 0);

    // ── BONUSCHOCOLATECOCOABEANSFORTUNE — CocoaBeans only ────────────────────

    public static final StatDefinition
            BONUSCHOCOLATECOCOABEANSFORTUNE = StatDefinition.of("BonusChocolateCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0);

    // ── BONUSCARROYLNEXPORTABLECROPS(crop)FORTUNE — Carrot, Pumpkin, CocoaBeans, Wheat, Mushroom, NetherWart, WildRose ──

    public static final StatDefinition
            BONUSCARROYLNEXPORTABLECROPSCARROTFORTUNE     = StatDefinition.of("BonusCarroLynExportableCropsCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSCARROYLNEXPORTABLECROPSPUMPKINFORTUNE    = StatDefinition.of("BonusCarroLynExportableCropsPumpkinFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSCARROYLNEXPORTABLECROPSCOCOABEANSFORTUNE = StatDefinition.of("BonusCarroLynExportableCropsCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSCARROYLNEXPORTABLECROPSWHEATFORTUNE      = StatDefinition.of("BonusCarroLynExportableCropsWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSCARROYLNEXPORTABLECROPSMUSHROOMFORTUNE   = StatDefinition.of("BonusCarroLynExportableCropsMushroomFortune",   ctx -> 0, (base, ctx) -> 0),
            BONUSCARROYLNEXPORTABLECROPSNETHERWARTFORTUNE = StatDefinition.of("BonusCarroLynExportableCropsNetherWartFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSCARROYLNEXPORTABLECROPSWILDROSEFORTUNE   = StatDefinition.of("BonusCarroLynExportableCropsWildRoseFortune",   ctx -> 0, (base, ctx) -> 0);

    // ── BONUSANITATALISMANS(crop)FORTUNE — all crops ──────────────────────────

    public static final StatDefinition
            BONUSANITATALISMANSWHEATFORTUNE         = StatDefinition.of("BonusAnitaTalismansWheatFortune",      ctx -> 0, (base, ctx) -> 0),
            BONUSANITATALISMANSCARROTFORTUNE        = StatDefinition.of("BonusAnitaTalismansCarrotFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSANITATALISMANSPOTATOFORTUNE        = StatDefinition.of("BonusAnitaTalismansPotatoFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSANITASMANSISMSPLANSPUMPKINFORTUNE  = StatDefinition.of("BonusAnitaTalismansPumpkinFortune",    ctx -> 0, (base, ctx) -> 0),
            BONUSANITATASLISMANSSUGARCANEFORTUNE    = StatDefinition.of("BonusAnitaTalismansSugarCaneFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSANITATALISMANSMELONSLICEFORTUNE    = StatDefinition.of("BonusAnitaTalismansMelonSliceFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITATASLISMANSCACTUSFORTUNE       = StatDefinition.of("BonusAnitaTalismansCactusFortune",     ctx -> 0, (base, ctx) -> 0),
            BONUSANITATALISMANSCOCOABEANSFORTUNE    = StatDefinition.of("BonusAnitaTalismansCocoaBeansFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITATALISMANSMUSHROOMFORTUNE      = StatDefinition.of("BonusAnitaTalismansMushroomFortune",   ctx -> 0, (base, ctx) -> 0),
            BONUSANITATASLISMANSNETHERWARTFORTUNE   = StatDefinition.of("BonusAnitaTalismansNetherWartFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITATASLISMANSSUNFLOWERFORTUNE    = StatDefinition.of("BonusAnitaTalismansSunflowerFortune",  ctx -> 0, (base, ctx) -> 0),
            BONUSANITATASLISMANSMOONFLOWERFORTUNE   = StatDefinition.of("BonusAnitaTalismansMoonflowerFortune", ctx -> 0, (base, ctx) -> 0),
            BONUSANITATASLISMANSWILDROSEFORTUNE     = StatDefinition.of("BonusAnitaTalismansWildRoseFortune",   ctx -> 0, (base, ctx) -> 0);

    // ── FINAL(crop)FORTUNE — all crops ───────────────────────────────────────────

    public static final StatDefinition
            FINALWHEATFORTUNE = StatDefinition.of("FinalWheatFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLWHEATFORTUNE) + ctx.total(BONUSLOGARITHMWHEATFORTUNE) +
                            ctx.total(BONUSACCWHEATFORTUNE) + ctx.total(BONUSENCHANTWHEATFORTUNE) +
                            ctx.total(BONUSANITAPERSONALBESTSWHEATFORTUNE) + ctx.total(BONUSCOLLECTIONANALYSISWHEATFORTUNE) +
                            ctx.total(BONUSCARROYLNEXPORTABLECROPSWHEATFORTUNE) + ctx.total(BONUSANITATALISMANSWHEATFORTUNE)),

            FINALCARROTFORTUNE = StatDefinition.of("FinalCarrotFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLCARROTFORTUNE) + ctx.total(BONUSLOGARITHMCARROTFORTUNE) +
                            ctx.total(BONUSACCCARROTFORTUNE) + ctx.total(BONUSENCHANTCARROTFORTUNE) +
                            ctx.total(BONUSANITAPERSONALBESTSCARROTFORTUNE) + ctx.total(BONUSCOLLECTIONANALYSISCARROTFORTUNE) +
                            ctx.total(BONUSCARROYLNEXPORTABLECROPSCARROTFORTUNE) + ctx.total(BONUSANITATALISMANSCARROTFORTUNE)),

            FINALPOTATOFORTUNE = StatDefinition.of("FinalPotatoFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLPOTATOFORTUNE) + ctx.total(BONUSLOGARITHMPOTATOFORTUNE) +
                            ctx.total(BONUSACCPOTATOFORTUNE) + ctx.total(BONUSENCHANTPOTATOFORTUNE) +
                            ctx.total(BONUSANITAPERSONALBESTSPOTATOFORTUNE) + ctx.total(BONUSCOLLECTIONANALYSISPOTATOFORTUNE) +
                            ctx.total(BONUSANITATALISMANSPOTATOFORTUNE)),

            FINALPUMPKINFORTUNE = StatDefinition.of("FinalPumpkinFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLPUMPKINFORTUNE) + ctx.total(BONUSACCPUMPKINFORTUNE) +
                            ctx.total(BONUSENCHANTPUMPKINFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSPUMPKINFORTUNE) +
                            ctx.total(BONUSCARROYLNEXPORTABLECROPSPUMPKINFORTUNE) + ctx.total(BONUSANITASMANSISMSPLANSPUMPKINFORTUNE)),

            FINALSUGARCANEFORTUNE = StatDefinition.of("FinalSugarCaneFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLSUGARCANEFORTUNE) + ctx.total(BONUSLOGARITHMSUGARCANEFORTUNE) +
                            ctx.total(BONUSACCSUGARCANEFORTUNE) + ctx.total(BONUSENCHANTSUGARCANEFORTUNE) +
                            ctx.total(BONUSANITAPERSONALBESTSSUGARCANEFORTUNE) + ctx.total(BONUSCOLLECTIONANALYSISSUGARCANEFORTUNE) +
                            ctx.total(BONUSANITATASLISMANSSUGARCANEFORTUNE)),

            FINALMELONSLICEFORTUNE = StatDefinition.of("FinalMelonSliceFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLMELONSLICEFORTUNE) + ctx.total(BONUSACCMELONSLICEFORTUNE) +
                            ctx.total(BONUSENCHANTMELONSLICEFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSMELONSLICEFORTUNE) +
                            ctx.total(BONUSANITATALISMANSMELONSLICEFORTUNE)),

            FINALCACTUSFORTUNE = StatDefinition.of("FinalCactusFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLCACTUSFORTUNE) + ctx.total(BONUSACCCACTUSFORTUNE) +
                            ctx.total(BONUSENCHANTCACTUSFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSCACTUSFORTUNE) +
                            ctx.total(BONUSANITATASLISMANSCACTUSFORTUNE)),

            FINALCOCOABEANSFORTUNE = StatDefinition.of("FinalCocoaBeansFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLCOCOABEANSFORTUNE) + ctx.total(BONUSACCCOCOABEANSFORTUNE) +
                            ctx.total(BONUSENCHANTCOCOABEANSFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSCOCOABEANSFORTUNE) +
                            ctx.total(BONUSCHOCOLATECOCOABEANSFORTUNE) + ctx.total(BONUSCARROYLNEXPORTABLECROPSCOCOABEANSFORTUNE) +
                            ctx.total(BONUSANITATALISMANSCOCOABEANSFORTUNE)),

            FINALMUSHROOMFORTUNE = StatDefinition.of("FinalMushroomFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLMUSHROOMFORTUNE) + ctx.total(BONUSACCMUSHROOMFORTUNE) +
                            ctx.total(BONUSENCHANTMUSHROOMFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSMUSHROOMFORTUNE) +
                            ctx.total(BONUSCARROYLNEXPORTABLECROPSMUSHROOMFORTUNE) + ctx.total(BONUSANITATALISMANSMUSHROOMFORTUNE)),

            FINALNETHERWARTFORTUNE = StatDefinition.of("FinalNetherWartFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLNETHERWARTFORTUNE) + ctx.total(BONUSLOGARITHMNETHERWARTFORTUNE) +
                            ctx.total(BONUSACCNETHERWARTFORTUNE) + ctx.total(BONUSENCHANTNETHERWARTFORTUNE) +
                            ctx.total(BONUSANITAPERSONALBESTSNETHERWARTFORTUNE) + ctx.total(BONUSCOLLECTIONANALYSISNETHERWARTFORTUNE) +
                            ctx.total(BONUSCARROYLNEXPORTABLECROPSNETHERWARTFORTUNE) + ctx.total(BONUSANITATASLISMANSNETHERWARTFORTUNE)),

            FINALSUNFLOWERFORTUNE = StatDefinition.of("FinalSunflowerFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLSUNFLOWERFORTUNE) + ctx.total(BONUSACCSUNFLOWERFORTUNE) +
                            ctx.total(BONUSENCHANTSUNFLOWERFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSSUNFLOWERFORTUNE) +
                            ctx.total(BONUSANITATASLISMANSSUNFLOWERFORTUNE)),

            FINALMOONFLOWERFORTUNE = StatDefinition.of("FinalMoonflowerFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLMOONFLOWERFORTUNE) + ctx.total(BONUSACCMOONFLOWERFORTUNE) +
                            ctx.total(BONUSENCHANTMOONFLOWERFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSMOONFLOWERFORTUNE) +
                            ctx.total(BONUSANITATASLISMANSMOONFLOWERFORTUNE)),

            FINALWILDROSEFORTUNE = StatDefinition.of("FinalWildRoseFortune", ctx -> 0, (base, ctx) ->
                    ctx.total(BONUSTOOLWILDROSEFORTUNE) + ctx.total(BONUSACCWILDROSEFORTUNE) +
                            ctx.total(BONUSENCHANTWILDROSEFORTUNE) + ctx.total(BONUSANITAPERSONALBESTSWILDROSEFORTUNE) +
                            ctx.total(BONUSCARROYLNEXPORTABLECROPSWILDROSEFORTUNE) + ctx.total(BONUSANITATASLISMANSWILDROSEFORTUNE));

    // ── Fishing Speed ─────────────────────────────────────────────────────────────

    public static final StatDefinition
            BASEFISHINGSPEED                    = StatDefinition.of("BaseFishingSpeed",                    ctx -> 0, (base, ctx) -> 0),
            BONUSPETSFISHINGSPEED               = StatDefinition.of("BonusPetsFishingSpeed",               ctx -> 0, (base, ctx) -> 0),
            BONUSACCFISHINGSPEED                = StatDefinition.of("BonusACCFishingSpeed",                ctx -> 0, (base, ctx) -> 0),
            BONUSEQUIPMENTFISHINGSPEED          = StatDefinition.of("BonusEquipmentFishingSpeed",          ctx -> 0, (base, ctx) -> 0),
            BONUSRODFISHINGSPEED                = StatDefinition.of("BonusRodFishingSpeed",                ctx -> 0, (base, ctx) -> 0),
            BONUSBAITFISHINGSPEED               = StatDefinition.of("BonusBaitFishingSpeed",               ctx -> 0, (base, ctx) -> 0),
            BONUSREFORGEFISHINGSPEED            = StatDefinition.of("BonusReforgeFishingSpeed",            ctx -> 0, (base, ctx) -> 0),
            BONUSENCHANTFISHINGSPEED            = StatDefinition.of("BonusEnchantFishingSpeed",            ctx -> 0, (base, ctx) -> 0),
            BONUSATTRIBUTESFISHINGSPEED         = StatDefinition.of("BonusAttributesFishingSpeed",         ctx -> 0, (base, ctx) -> 0),
            BONUSBOBBINTIMEFISHINGSPEED         = StatDefinition.of("BonusBobberTimeFishingSpeed",         ctx -> 0, (base, ctx) -> 0),
            BONUSSPIDERDENRAINFISHINGSPEED      = StatDefinition.of("BonusSpiderDenRainFishingSpeed",      ctx -> 0, (base, ctx) -> 0),
            BONUSPETITEMFISHINGSPEED            = StatDefinition.of("BonusPetItemFishingSpeed",            ctx -> 0, (base, ctx) -> 0),
            BONUSEMPTYCHUMCAPBUCKETFISHINGSPEED = StatDefinition.of("BonusEmptyChumCapBucketFishingSpeed", ctx -> 0, (base, ctx) -> 0),
            BONUSCORRUPTBAITFISHINGSPEED        = StatDefinition.of("BonusCorruptBaitFishingSpeed",        ctx -> 0, (base, ctx) -> 0),

    FINALFISHINGSPEED = StatDefinition.of("FinalFishingSpeed", ctx -> 0, (base, ctx) -> {
        int sum = ctx.total(BASEFISHINGSPEED)                    +
                  ctx.total(BONUSPETSFISHINGSPEED)               +
                  ctx.total(BONUSACCFISHINGSPEED)                +
                  ctx.total(BONUSEQUIPMENTFISHINGSPEED)          +
                  ctx.total(BONUSRODFISHINGSPEED)                +
                  ctx.total(BONUSBAITFISHINGSPEED)               +
                  ctx.total(BONUSREFORGEFISHINGSPEED)            +
                  ctx.total(BONUSENCHANTFISHINGSPEED)            +
                  ctx.total(BONUSATTRIBUTESFISHINGSPEED)         +
                  ctx.total(BONUSSPIDERDENRAINFISHINGSPEED)      +
                  ctx.total(BONUSPETITEMFISHINGSPEED)            +
                  ctx.total(BONUSEMPTYCHUMCAPBUCKETFISHINGSPEED);
        double bobbin  = ctx.total(BONUSBOBBINTIMEFISHINGSPEED);
        double corrupt = ctx.total(BONUSCORRUPTBAITFISHINGSPEED);
        double divisor = corrupt == 0 ? 1 : corrupt;
        return (int) (sum * (1.0 + bobbin / 100.0) / divisor);
    });



    // ── ALL ───────────────────────────────────────────────────────────────────

    private static final StatDefinition[] ALL = {
            // Core
            HEALTH, DEFENSE, INTELLIGENCE, SPEED, EHP,
            // Melee
            PLAYERSTRENGTH, MELEEWEAPONBASEDMG, MELEEWEAPONBASESTR,
            MELEEWEAPONBASECRITDMG, MELEEWEAPONBASECRITCHANCE,
            FINALSTRENGTH, FINALMELEEDMG,
            // Global farming fortune
            BASEFARMINGFORTUNE, GREENHOUSEFARMINGFORTUNE, BONUSARMORFARMINGFORTUNE,
            BONUSTOOLEFARMINGFORTUNE, BONUSEQUIPMENTFARMINGFORTUNE,
            BONUSENCHANTSFARMINGFORTUNE, BONUSREFORGEFARMINGFORTUNE,
            BONUSPETFARMINGFORTUNE, BONUSPETITEMFARMINGFORTUNE,
            BONUSACCFARMINGFORTUNE, BONUSFARMINGFORDUMMIESFARMINGFORTUNE,
            BONUSCAKEFARMINGFORTUNE, BONUSPOTIONFARMINGFORTUNE,
            BONUSPESTHUNTERFARMINGFORTUNE, BONUSDARKCOCOAFARMINGFORTUNE,
            BONUSREFINEDDARKCOCOAFARMINGFORTUNE, BONUSROSEWATERFLASKFARMINGFORTUNE,
            BONUSSKILLLEVELFARMINGFORTUNE, FINALGLOBALFARMINGFORTUNE,
            // BonusTool
            BONUSTOOLWHEATFORTUNE, BONUSTOOLCARROTFORTUNE, BONUSTOOLPOTATOFORTUNE,
            BONUSTOOLPUMPKINFORTUNE, BONUSTOOLSUGARCANEFORTUNE, BONUSTOOLMELONSLICEFORTUNE,
            BONUSTOOLCACTUSFORTUNE, BONUSTOOLCOCOABEANSFORTUNE, BONUSTOOLMUSHROOMFORTUNE,
            BONUSTOOLNETHERWARTFORTUNE, BONUSTOOLSUNFLOWERFORTUNE,
            BONUSTOOLMOONFLOWERFORTUNE, BONUSTOOLWILDROSEFORTUNE,
            // BonusLogarithm
            BONUSLOGARITHMWHEATFORTUNE, BONUSLOGARITHMCARROTFORTUNE,
            BONUSLOGARITHMPOTATOFORTUNE, BONUSLOGARITHMSUGARCANEFORTUNE,
            BONUSLOGARITHMNETHERWARTFORTUNE,
            // BonusACC
            BONUSACCWHEATFORTUNE, BONUSACCCARROTFORTUNE, BONUSACCPOTATOFORTUNE,
            BONUSACCPUMPKINFORTUNE, BONUSACCSUGARCANEFORTUNE, BONUSACCMELONSLICEFORTUNE,
            BONUSACCCACTUSFORTUNE, BONUSACCCOCOABEANSFORTUNE, BONUSACCMUSHROOMFORTUNE,
            BONUSACCNETHERWARTFORTUNE, BONUSACCSUNFLOWERFORTUNE,
            BONUSACCMOONFLOWERFORTUNE, BONUSACCWILDROSEFORTUNE,
            // BonusEnchant
            BONUSENCHANTWHEATFORTUNE, BONUSENCHANTCARROTFORTUNE, BONUSENCHANTPOTATOFORTUNE,
            BONUSENCHANTPUMPKINFORTUNE, BONUSENCHANTSUGARCANEFORTUNE, BONUSENCHANTMELONSLICEFORTUNE,
            BONUSENCHANTCACTUSFORTUNE, BONUSENCHANTCOCOABEANSFORTUNE, BONUSENCHANTMUSHROOMFORTUNE,
            BONUSENCHANTNETHERWARTFORTUNE, BONUSENCHANTSUNFLOWERFORTUNE,
            BONUSENCHANTMOONFLOWERFORTUNE, BONUSENCHANTWILDROSEFORTUNE,
            // BonusAnitaPersonalBests
            BONUSANITAPERSONALBESTSWHEATFORTUNE, BONUSANITAPERSONALBESTSCARROTFORTUNE,
            BONUSANITAPERSONALBESTSPOTATOFORTUNE, BONUSANITAPERSONALBESTSPUMPKINFORTUNE,
            BONUSANITAPERSONALBESTSSUGARCANEFORTUNE, BONUSANITAPERSONALBESTSMELONSLICEFORTUNE,
            BONUSANITAPERSONALBESTSCACTUSFORTUNE, BONUSANITAPERSONALBESTSCOCOABEANSFORTUNE,
            BONUSANITAPERSONALBESTSMUSHROOMFORTUNE, BONUSANITAPERSONALBESTSNETHERWARTFORTUNE,
            BONUSANITAPERSONALBESTSSUNFLOWERFORTUNE, BONUSANITAPERSONALBESTSMOONFLOWERFORTUNE,
            BONUSANITAPERSONALBESTSWILDROSEFORTUNE,
            // BonusCollectionAnalysis
            BONUSCOLLECTIONANALYSISWHEATFORTUNE, BONUSCOLLECTIONANALYSISCARROTFORTUNE,
            BONUSCOLLECTIONANALYSISPOTATOFORTUNE, BONUSCOLLECTIONANALYSISSUGARCANEFORTUNE,
            BONUSCOLLECTIONANALYSISNETHERWARTFORTUNE,
            // BonusChocolate
            BONUSCHOCOLATECOCOABEANSFORTUNE,
            // BonusCarroLynExportableCrops
            BONUSCARROYLNEXPORTABLECROPSCARROTFORTUNE, BONUSCARROYLNEXPORTABLECROPSPUMPKINFORTUNE,
            BONUSCARROYLNEXPORTABLECROPSCOCOABEANSFORTUNE, BONUSCARROYLNEXPORTABLECROPSWHEATFORTUNE,
            BONUSCARROYLNEXPORTABLECROPSMUSHROOMFORTUNE, BONUSCARROYLNEXPORTABLECROPSNETHERWARTFORTUNE,
            BONUSCARROYLNEXPORTABLECROPSWILDROSEFORTUNE,
            // BonusAnitaTalismans
            BONUSANITATALISMANSWHEATFORTUNE, BONUSANITATALISMANSCARROTFORTUNE,
            BONUSANITATALISMANSPOTATOFORTUNE, BONUSANITASMANSISMSPLANSPUMPKINFORTUNE,
            BONUSANITATASLISMANSSUGARCANEFORTUNE, BONUSANITATALISMANSMELONSLICEFORTUNE,
            BONUSANITATASLISMANSCACTUSFORTUNE, BONUSANITATALISMANSCOCOABEANSFORTUNE,
            BONUSANITATALISMANSMUSHROOMFORTUNE, BONUSANITATASLISMANSNETHERWARTFORTUNE,
            BONUSANITATASLISMANSSUNFLOWERFORTUNE, BONUSANITATASLISMANSMOONFLOWERFORTUNE,
            BONUSANITATASLISMANSWILDROSEFORTUNE,
            // FinalFortune
            FINALWHEATFORTUNE, FINALCARROTFORTUNE, FINALPOTATOFORTUNE,
            FINALPUMPKINFORTUNE, FINALSUGARCANEFORTUNE, FINALMELONSLICEFORTUNE,
            FINALCACTUSFORTUNE, FINALCOCOABEANSFORTUNE, FINALMUSHROOMFORTUNE,
            FINALNETHERWARTFORTUNE, FINALSUNFLOWERFORTUNE,
            FINALMOONFLOWERFORTUNE, FINALWILDROSEFORTUNE,
            // Fishing Speed
            BASEFISHINGSPEED, BONUSPETSFISHINGSPEED, BONUSACCFISHINGSPEED,
            BONUSEQUIPMENTFISHINGSPEED, BONUSRODFISHINGSPEED, BONUSBAITFISHINGSPEED,
            BONUSREFORGEFISHINGSPEED, BONUSENCHANTFISHINGSPEED, BONUSATTRIBUTESFISHINGSPEED,
            BONUSBOBBINTIMEFISHINGSPEED, BONUSSPIDERDENRAINFISHINGSPEED, BONUSPETITEMFISHINGSPEED,
            BONUSEMPTYCHUMCAPBUCKETFISHINGSPEED, BONUSCORRUPTBAITFISHINGSPEED,
            FINALFISHINGSPEED
    };

    // ── updateAll ─────────────────────────────────────────────────────────────

    public static void updateAll(Player player) {
        StatContext ctx = new StatContext(player);

        int health       = HEALTH.calculateTotal(ctx);
        int defense      = DEFENSE.calculateTotal(ctx);
        int intelligence = INTELLIGENCE.calculateTotal(ctx);
        int speed        = SPEED.calculateTotal(ctx);

        StatusBarTracker.setHealth((int) player.getHealth(), health);
        StatusBarTracker.setDefense(defense);
        StatusBarTracker.setMana(StatusBarTracker.getMana().value(), intelligence);
        StatusBarTracker.setSpeed(speed, 400);
    }

    // ── StatDefinition ────────────────────────────────────────────────────────

    public static class StatDefinition {
        private final String name;
        private final Function<StatContext, Integer> baseFormula;
        private final TotalFormula totalFormula;

        private StatDefinition(String name,
                               Function<StatContext, Integer> baseFormula,
                               TotalFormula totalFormula) {
            this.name = name;
            this.baseFormula = baseFormula;
            this.totalFormula = totalFormula;
        }

        public static StatDefinition of(String name,
                                        Function<StatContext, Integer> baseFormula,
                                        TotalFormula totalFormula) {
            return new StatDefinition(name, baseFormula, totalFormula);
        }

        public int calculateBase(StatContext ctx)  { return baseFormula.apply(ctx); }
        public int calculateTotal(StatContext ctx) { return totalFormula.calculate(calculateBase(ctx), ctx); }
        public String getName()                    { return name; }
    }

    // ── StatContext ───────────────────────────────────────────────────────────

    public static class StatContext {
        public final Player player;
        private final java.util.Map<StatDefinition, Integer> bonuses = new java.util.HashMap<>();

        public StatContext(Player player) {
            this.player = player;

            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof ShinySBItem item) {
                item.applyStatBonuses(this::addBonus);
            }
            for (ItemStack stack : player.getArmorSlots()) {
                if (stack.getItem() instanceof ShinySBItem item) {
                    item.applyStatBonuses(this::addBonus);
                }
            }
        }

        public void addBonus(StatDefinition stat, int amount) {
            bonuses.merge(stat, amount, Integer::sum);
        }

        public int bonus(String statName) {
            return bonuses.entrySet().stream()
                    .filter(e -> e.getKey().getName().equals(statName))
                    .mapToInt(java.util.Map.Entry::getValue)
                    .sum();
        }

        public int base(StatDefinition stat)  { return stat.calculateBase(this); }
        public int total(StatDefinition stat) { return stat.calculateTotal(this); }
        public float vanillaHp()              { return player.getHealth(); }
        public float vanillaMaxHp()           { return player.getMaxHealth(); }
    }

    // ── TotalFormula ──────────────────────────────────────────────────────────

    @FunctionalInterface
    public interface TotalFormula {
        int calculate(int base, StatContext ctx);
    }
}