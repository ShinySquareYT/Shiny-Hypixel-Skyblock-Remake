package net.shinysquare.shiny_sb.content.items;

import net.minecraft.world.item.Item;
import net.shinysquare.shiny_sb.register.ShinyStats;

import java.util.function.BiConsumer;

public class ShinySwordItem extends Item implements ShinySBItem{

    private final int damage;
    private final int strength;
    private final int critdmg; // add whatever stats you want
    private final int critchance;

    public ShinySwordItem(int damage, int strength, int critdmg, int critchance, Properties properties) {
        super(properties);
        this.damage = damage;
        this.strength = strength;
        this.critdmg = critdmg;
        this.critchance = critchance;
    }
    public int getStatDamage()   { return damage; }
    public int getStatStrength() { return strength; }
    @Override
    public void applyStatBonuses(BiConsumer<ShinyStats.StatDefinition, Integer> addBonus) {
        addBonus.accept(ShinyStats.MELEEWEAPONBASEDMG,    damage);
        addBonus.accept(ShinyStats.MELEEWEAPONBASESTR,  strength);
        addBonus.accept(ShinyStats.MELEEWEAPONBASECRITDMG, critdmg);
        addBonus.accept(ShinyStats.MELEEWEAPONBASECRITCHANCE, critchance);
    }
}