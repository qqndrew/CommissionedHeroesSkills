package net.kingdomsofarden.andrew2060.heroes.skills;
import org.bukkit.plugin.java.JavaPlugin;

import com.herocraftonline.heroes.api.SkillRegistrar;

public class KingdomsOfArdenSkills extends JavaPlugin {
    @Override
    public void onLoad() {
        SkillRegistrar.registerSkill(this, SkillCleave.class);
        SkillRegistrar.registerSkill(this, SkillExecute.class);
        SkillRegistrar.registerSkill(this, SkillRend.class);
        SkillRegistrar.registerSkill(this, SkillStampede.class);
        SkillRegistrar.registerSkill(this, SkillWarStomp.class);
    }
}
