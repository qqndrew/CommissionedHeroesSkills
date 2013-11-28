package net.kingdomsofarden.andrew2060.heroes.skills;

import java.text.DecimalFormat;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillWarStomp extends ActiveSkill {

    public SkillWarStomp(Heroes plugin) {
        super(plugin, "Warstomp");
        setDescription("On use: for the next $0 seconds the user is propelled forward at a great rate dealing $1 damage to everyone within a $2 radius and knocking them up. CD: $3 seconds");
        setArgumentRange(0,0);
        setIdentifiers("skill warstomp", "skill seismicshard");
        setUsage("/skill warstomp");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL);
    }

    public class WarStompPropulsionEffect extends PeriodicExpirableEffect {

        private double damage;
        private int range;

        public WarStompPropulsionEffect(Skill skill, Heroes plugin, long duration, double damage, int range) {
            super(skill, plugin, "WarStompPropulsionEffect", 200, duration);
            this.damage = damage;
            this.range = range;
        }

        @Override
        public void tickMonster(Monster monster) {
            return;
        }

        @Override
        public void tickHero(Hero hero) {
            Location loc = hero.getPlayer().getLocation();
            loc.getWorld().createExplosion(loc, 0, false);
            Vector direction = loc.getDirection();
            Player p = hero.getPlayer();
            p.setVelocity(direction.normalize().multiply(3.0));
            for(Entity e : p.getNearbyEntities(range, range, range)) {
                if(e instanceof LivingEntity) {
                    LivingEntity lE = (LivingEntity)e;
                    if(Skill.damageCheck(hero.getPlayer(), lE)) {
                        CharacterTemplate cT = plugin.getCharacterManager().getCharacter(lE);
                        if(!cT.hasEffect("WarStompKnockupEffect")) {
                            skill.addSpellTarget(lE, hero);
                            Skill.damageEntity(lE, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK);
                            Vector v = new Vector(0,3,0);
                            lE.setVelocity(v);
                            cT.addEffect(new ExpirableEffect(skill, plugin, "WarStompKnockupEffect", 10000));
                        }
                    }
                }
            }
            
        }
        
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 20, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 1, false) * hero.getLevel();
        int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 5, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false) 
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 100, false) * hero.getLevel(); 
        hero.addEffect(new WarStompPropulsionEffect(this, plugin, duration, damage, range));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        DecimalFormat dF = new DecimalFormat("##.##");
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false) 
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 100, false) * hero.getLevel(); 
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 20, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 1, false) * hero.getLevel();
        int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 5, false);
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 3600000, false) 
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 100, false) * hero.getLevel(); 
        return getDescription()
                .replace("$0", dF.format(duration*0.001))
                .replace("$1", dF.format(damage))
                .replace("$2", range + "")
                .replace("$3", dF.format(cooldown*0.001));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.DURATION_INCREASE.node(), 100);
        node.set(SkillSetting.DAMAGE.node(), 20);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 1);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.COOLDOWN.node(),3600000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(),100);
        return node;
    }
    
}
