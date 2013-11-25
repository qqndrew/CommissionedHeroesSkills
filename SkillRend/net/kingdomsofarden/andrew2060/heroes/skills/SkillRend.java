package net.kingdomsofarden.andrew2060.heroes.skills;

import java.text.DecimalFormat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillRend extends TargettedSkill {

    public SkillRend(Heroes plugin) {
        super(plugin, "Rend");
        this.setTypes(SkillType.HARMFUL, SkillType.SILENCABLE, SkillType.DAMAGING);
        this.setDescription("Rends a target within $0 blocks, causing a bleed effect for $1 ticks dealing $2 damage per tick ($3 ticks per second). CD: $4 Seconds");
        this.setIdentifiers("skill rend");
        this.setUsage("/skill rend");
    }

    public class RendBleedEffect extends PeriodicDamageEffect {

        public RendBleedEffect(Skill skill, long period, long duration, double tickDamage, Player applier) {
            super(skill, "RendEffect", period, duration, tickDamage, applier);
        }
        
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if(target == null) {
            return SkillResult.INVALID_TARGET;
        }
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 10, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 0, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 1000, false);
        RendBleedEffect effect = new RendBleedEffect(this, period, duration, damage, hero.getPlayer());
        plugin.getCharacterManager().getCharacter(target).addEffect(effect);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 15, false)
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 10, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 0, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 1000, false);
        
        DecimalFormat dF = new DecimalFormat("##.###");
        
        double freq = 1 / (period * 0.001);
        
        int tickCount = (int) Math.floor(duration / period);
        
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 30000, false)
                - hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 500, false);
        
        return getDescription()
                .replace("$0", maxDist+"")
                .replace("$1", tickCount + "")
                .replace("$2", damage + "")
                .replace("$3", dF.format(freq))
                .replace("$4", dF.format(cooldown * 0.001));
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.DURATION_INCREASE.node(), 0);
        node.set(SkillSetting.DAMAGE_TICK.node(),10);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.1);
        node.set(SkillSetting.PERIOD.node(),1000);
        node.set(SkillSetting.COOLDOWN.node(), 30000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(), 500);
        return node;
    }

}
