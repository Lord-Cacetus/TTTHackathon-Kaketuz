package me.kaketuz.hackathon;

import com.projectkorra.projectkorra.ability.CoreAbility;
import me.kaketuz.nightmarelib.lib.logger.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hackathon extends JavaPlugin {

    public static Hackathon plugin;

    @Override
    public void onEnable() {
        plugin = this;

        if (getServer().getPluginManager().getPlugin("ServersNPC") == null) {
            Logger.sendError("Error: The ZNPCs(ServersNPC) plugin is not installed on the server! The plugin cannot work without this plugin. Download link: https://www.spigotmc.org/resources/znpcs.80940/");
        }

        CoreAbility.registerPluginAbilities(this, "me.kaketuz.hackathon.abilities");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
