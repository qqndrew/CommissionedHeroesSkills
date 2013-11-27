package net.kingdomsofarden.andrew2060.heroes.skills;

import java.text.DecimalFormat;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillCleave extends ActiveSkill {

    public SkillCleave(Heroes plugin) {
        super(plugin, "Cleave");
        this.setDescription("On use, cleaves up to $0 blocks in front of the user in a $1 degree arc, dealing $2 damage. CD: $3 seconds");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill cleave");
        this.setTypes(SkillType.DAMAGING, SkillType.HARMFUL, SkillType.PHYSICAL);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        boolean threeDimensionalArc = SkillConfigManager.getUseSetting(hero, this, "three-dimensional-arc", false);
        int dist = (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 3, false) + 
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.02, false) * hero.getLevel());
        double arc = SkillConfigManager.getUseSetting(hero, this, "arc-angle-base", 15, false) 
                + SkillConfigManager.getUseSetting(hero, this, "arc-angle-per-level", 0.1, false) * hero.getLevel();
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 30, false) 
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 0, false) * hero.getLevel();
        //The input is in degrees, we must convert to radians because that is what the Math acos function returns
        arc *= Math.PI/180.00;
        Location castLoc = hero.getPlayer().getLocation();
        Vector attackVec = castLoc.getDirection();
        if(!threeDimensionalArc) {
            attackVec.setY(0);
        }
        attackVec.normalize(); //Length to zero for future dot product calculations
        for(Entity e : hero.getPlayer().getNearbyEntities(dist,dist,dist)) {
            if(e instanceof LivingEntity) {
                LivingEntity lE = (LivingEntity)e;
                Location targetLoc = e.getLocation();
                Vector targetVec = targetLoc.toVector().subtract(castLoc.toVector());
                if(!threeDimensionalArc) {
                    targetVec.setY(0);
                }
                targetVec.normalize();
                //Math - get dot product
                double dot = attackVec.getX() * targetVec.getX() + attackVec.getY() * targetVec.getY() + attackVec.getZ() * targetVec.getZ();
                //Dot product of two normalized vectors represents the cosine value of the angle between them
                double deviation = Math.acos(dot);
                if(deviation < arc) { //The target is within the arc we wish to affect
                    if(Skill.damageCheck(hero.getPlayer(), lE)) {
                        addSpellTarget(lE, hero);
                        Skill.damageEntity(lE, hero.getEntity(), damage, DamageCause.ENTITY_ATTACK);
                    }
                }
            }
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int dist = (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 3, false) + 
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.02, false) * hero.getLevel());
        double arc = SkillConfigManager.getUseSetting(hero, this, "arc-angle-base", 15, false) 
                + SkillConfigManager.getUseSetting(hero, this, "arc-angle-per-level", 0.1, false) * hero.getLevel();
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 30, false) 
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 0, false) * hero.getLevel();
        double cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 60000, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 500, false);
        DecimalFormat dF = new DecimalFormat("##.##");
        return getDescription().replace("$0",dist+"")
                .replace("$1",dF.format(arc*2))
                .replace("$2", dF.format(damage))
                .replace("$3", dF.format(cooldown * 0.001));
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(),0.02);
        node.set(SkillSetting.DAMAGE.node(), 30);
        node.set(SkillSetting.DAMAGE_INCREASE.node(),0);
        node.set(SkillSetting.COOLDOWN.node(), 60000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(), 500);
        node.set("arc-angle-base", 15);
        node.set("arc-angle-per-level", 0.1);
        node.set("three-dimensional-arc",false);
        return node;
    }
    
}
