package net.kingdomsofarden.andrew2060.heroes.skills;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
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

public class SkillWarStomp extends ActiveSkill {

    public SkillWarStomp(Heroes plugin) {
        super(plugin, "WarStomp");
        this.setDescription("On use, strikes the ground with a tremendous amount of energy, drawing hostile targets within $1 inwards and dealing $2 magic damage. CD: $3 seconds");
        this.setTypes(SkillType.HARMFUL, SkillType.EARTH);
        this.setIdentifiers("skill warstomp");
        this.setUsage("/skill warstomp");
        this.setArgumentRange(0, 0);
    }

    @Override
    public SkillResult use(Hero hero, String[] arg1) {
        broadcastExecuteText(hero);
        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 5, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE.node(), 0.1, false) * hero.getLevel();
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 20, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 1, false) * hero.getLevel();
        Location loc = hero.getPlayer().getLocation();
        for(Entity e : hero.getPlayer().getNearbyEntities(range, range, range)) {
            if(e instanceof LivingEntity) {
                LivingEntity lE = (LivingEntity)e;
                if(lE.getLocation().distanceSquared(loc) <= Math.pow(range, 2)) {
                    if(Skill.damageCheck(hero.getPlayer(), lE)) {
                        Skill.damageEntity(lE, hero.getEntity(), damage, DamageCause.MAGIC, false);
                        Vector v = hero.getPlayer().getLocation().toVector().subtract(lE.getLocation().toVector()).normalize();
                        lE.setVelocity(v);
                    }
                }
            }
        }
        for(int i = (int) Math.ceil(range); i >= 0 && i > range - 3; i--) { //Limit to 3 circles
            List<Location> explosionLoc = circle(loc,10,1,false,false,0);
            long ticksPerFirework = Math.round(40.00/((double)explosionLoc.size()));
            for(final Location expLoc : explosionLoc) {
                Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        expLoc.getWorld().createExplosion(expLoc, 0.0F);
                    }
                
                }, Math.round(ticksPerFirework*(range - i)));    
            }
                     
        }

        return SkillResult.NORMAL;
    }

    protected List<Location> circle(Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
        List<Location> circleblocks = new ArrayList<Location>();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - r; x <= cx +r; x++)
            for (int z = cz - r; z <= cz +r; z++)
                for (int y = (sphere ? cy - r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r*r && !(hollow && dist < (r-1)*(r-1))) {
                        Location l = new Location(loc.getWorld(), x, y + plus_y, z);
                        circleblocks.add(l);
                        }
                    }
     
        return circleblocks;
    }

    @Override
    public String getDescription(Hero hero) {
        DecimalFormat dF = new DecimalFormat("##.##");
        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 5, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE.node(), 0.1, false) * hero.getLevel();
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 20, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 1, false) * hero.getLevel();
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 60000, false) 
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 100, false) * hero.getLevel(); 
        return getDescription()
                .replace("$1", dF.format(range))
                .replace("$2",dF.format(damage))
                .replace("$3",dF.format(cooldown * 0.001));
    }

    @Override 
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.RADIUS_INCREASE.node(), 0.1);
        node.set(SkillSetting.DAMAGE.node(),20);
        node.set(SkillSetting.DAMAGE_INCREASE.node(),1);
        node.set(SkillSetting.COOLDOWN.node(), 60000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(), 100);
        return node;
    }
}
