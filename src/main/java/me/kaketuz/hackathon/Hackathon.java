package me.kaketuz.hackathon;

import com.projectkorra.projectkorra.ability.CoreAbility;
import me.kaketuz.hackathon.util.GhostFactory;
import me.kaketuz.nightmarelib.lib.logger.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hackathon extends JavaPlugin {

    public static Hackathon plugin;
    public static FileConfiguration config;
    public static GhostFactory factory;

    @Override
    public void onEnable() {
        plugin = this;
        config = this.getConfig();
        factory = new GhostFactory(this);
        if (getServer().getPluginManager().getPlugin("FancyNpcs") == null) {
            Logger.sendError("Error: The FancyNpcs plugin is not installed on the server! The plugin cannot work without this plugin. Download link: https://modrinth.com/plugin/fancynpcs");
            shutdown();
            return;
        }


        CoreAbility.registerPluginAbilities(this, "me.kaketuz.hackathon.abilities");

        getServer().getPluginManager().registerEvents(new HackathonListener(), this);

        registerConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static void shutdown() {
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }

    private void registerConfig() {
        config.addDefault("Air.Spiritual.SoulSeparation.ChargeDuration", 5000);
        config.options().copyDefaults(true);
        saveConfig();
    }
}
