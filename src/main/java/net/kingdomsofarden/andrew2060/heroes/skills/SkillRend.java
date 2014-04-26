package net.kingdomsofarden.andrew2060.heroes.skills;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillRend extends TargettedSkill {

    public SkillRend(Heroes plugin) {
        super(plugin, "Rend");
        this.setTypes(SkillType.HARMFUL, SkillType.SILENCABLE, SkillType.DAMAGING);
        this.setDescription("Rends a target within $0 blocks, dealing $1% weapon damage and causing a bleed effect for $2 ticks dealing $3 damage per tick ($4 ticks per second). CD: $5 Seconds");
        this.setIdentifiers("skill rend");
        this.setUsage("/skill rend");
    }

    public class RendBleedEffect extends PeriodicDamageEffect {

        public RendBleedEffect(Skill skill, long period, long duration, double tickDamage, Player applier) {
            super(skill, "RendEffect", period, duration, tickDamage, applier);
        }
        
    }
    
    //--Code taken from heroes TargettedSkill class for override purposes
    @Override
    public SkillResult use(Hero hero, String[] args) {
        int maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        final double distBonus = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        maxDistance += (int) distBonus;
        if (hero.hasEffectType(EffectType.BLIND)) {
            Messaging.send(hero.getPlayer(), "You can't target anything while blinded!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        final LivingEntity target = getTarget(hero, maxDistance, args);
        
        if (target == null) {
            hero.getPlayer().sendMessage(ChatColor.GRAY + "No valid targets found!"); //Send message if no valid targets
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        else if ((args.length > 1) && (target != null)) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (target != null && (target instanceof Player)) {
            final Hero tHero = plugin.getCharacterManager().getHero((Player) target);
            if (tHero.hasEffectType(EffectType.UNTARGETABLE)) {
                Messaging.send(hero.getPlayer(), "You cannot currently target this player!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
            else if (tHero.hasEffectType(EffectType.UNTARGETABLE_NO_MSG)) {
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        final SkillResult result = use(hero, target, args);
        if (this.isType(SkillType.INTERRUPT) && result.equals(SkillResult.NORMAL) && (target instanceof Player)) {
            final Hero tHero = plugin.getCharacterManager().getHero((Player) target);
            if (tHero.getDelayedSkill() != null) {
                tHero.cancelDelayedSkill();
                tHero.setCooldown("global", Heroes.properties.globalCooldown + System.currentTimeMillis());
            }
        }
        return result;
    }
    
    private LivingEntity getTarget(Hero hero, int maxDistance, String[] args) {
        final Player player = hero.getPlayer();
        LivingEntity target = null;
        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                Messaging.send(player, "Invalid target!");
                return null;
            }
            if (!target.getLocation().getWorld().equals(player.getLocation().getWorld())) {
                Messaging.send(player, "Target is in a different dimension.");
                return null;
            }
            final int distSq = maxDistance * maxDistance;
            if (target.getLocation().distanceSquared(player.getLocation()) > distSq) {
                Messaging.send(player, "Target is too far away.");
                return null;
            }
            if (!inLineOfSight(player, (Player) target)) {
                Messaging.send(player, "Sorry, target is not in your line of sight!");
                return null;
            }
            if (target.isDead() || (target.getHealth() == 0)) {
                Messaging.send(player, "You can't target the dead!");
                return null;
            }
        }
        if (target == null) {
            target = getPlayerTarget(player, maxDistance);
            if (this.isType(SkillType.HEAL)) {
                if ((target instanceof Player) && hero.hasParty() && hero.getParty().isPartyMember((Player) target)) {
                    return target;
                }
                else if (target instanceof Player) {
                    return null;
                }
                else {
                    target = null;
                }
            }
        }
        if (target == null) {
            // don't self-target harmful skills
            if (this.isType(SkillType.HARMFUL)) {
                return null;
            }
            target = player;
        }

        // Do a PvP check automatically for any harmful skill
        if (this.isType(SkillType.HARMFUL)) {
            if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
                Messaging.send(player, "Sorry, You can't damage that target!");
                return null;
            }
        }
        return target;
    }
    //--End Heroes Code
    
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if(target == null) {
            return SkillResult.INVALID_TARGET;
        }
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 0, false);
        double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 1, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 0.01, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 10, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, "tick-damage-increase", 0, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 1000, false);
        baseDamage *= plugin.getDamageManager().getItemDamage(hero.getPlayer().getItemInHand().getType(), hero.getPlayer());
        if(Skill.damageCheck(hero.getPlayer(),target)) {
            broadcastExecuteText(hero);
            addSpellTarget(target,hero);
            Skill.damageEntity(target,hero.getEntity(),baseDamage,DamageCause.ENTITY_ATTACK,true);
            RendBleedEffect effect = new RendBleedEffect(this, period, duration, damage, hero.getPlayer());
            plugin.getCharacterManager().getCharacter(target).addEffect(effect);
        } else {
            return SkillResult.INVALID_TARGET;
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 15, false)
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 0, false);
        double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 1, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE.node(), 0.01, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 10, false) 
                + hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, "tick-damage-increase", 0, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 1000, false);
        
        DecimalFormat dF = new DecimalFormat("##.###");
        
        double freq = 1 / (period * 0.001);
        
        int tickCount = (int) Math.floor(duration / period);
        
        long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 30000, false)
                - hero.getLevel() * SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 500, false);
        
        return getDescription()
                .replace("$0", maxDist+"")
                .replace("$1", dF.format(baseDamage * 100))
                .replace("$2", tickCount + "")
                .replace("$3", dF.format(damage))
                .replace("$4", dF.format(freq))
                .replace("$5", dF.format(cooldown * 0.001));
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.DURATION_INCREASE.node(), 0);
        node.set(SkillSetting.DAMAGE.node(),1);
        node.set(SkillSetting.DAMAGE_INCREASE.node(),0.01);
        node.set(SkillSetting.DAMAGE_TICK.node(),10);
        node.set("tick-damage-increase", 0.1);
        node.set(SkillSetting.PERIOD.node(),1000);
        node.set(SkillSetting.COOLDOWN.node(), 30000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(), 500);
        return node;
    }

}
