package me.kaketuz.hackathon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import me.kaketuz.hackathon.abilities.plant.PlantArmor;
import me.kaketuz.nightmarelib.lib.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Hackathon extends JavaPlugin {

    public static Hackathon plugin;
    public static FileConfiguration config;
    public static boolean hasOraxen;
    public static final Gson plantArmor_values = new GsonBuilder().setPrettyPrinting().create();


    @Override
    public void onEnable() {
        plugin = this;
        config = this.getConfig();

        hasOraxen = getServer().getPluginManager().getPlugin("Oraxen") != null;


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

    public static void registerConfig() {
        config.addDefault("Plant.PlantArmor.Collection.CollectRange", 12);
        config.addDefault("Plant.PlantArmor.Collection.CollectSpeed", 0.4);
        config.addDefault("Plant.PlantArmor.Collection.CollectInterval", 100);

        config.addDefault("Plant.PlantArmor.General.DurabilityTitle", "Dᴜʀᴀʙɪʟɪᴛʏ - {current}/{max}");
        config.addDefault("Plant.PlantArmor.General.JumpBoost", 3);
        config.addDefault("Plant.PlantArmor.General.SpeedBoost", 3);
        config.addDefault("Plant.PlantArmor.General.DolphinGraceBoost", 2);
        config.addDefault("Plant.PlantArmor.General.MaxDurability", 2000);
        config.addDefault("Plant.PlantArmor.General.CooldownMessage", "<! Cooldown !>");
        config.addDefault("Plant.PlantArmor.General.NotEnoughDurabilityMessage", "<! Not enough durability !>");

        config.addDefault("Plant.PlantArmor.PlantWhip.Damage", 2);
        config.addDefault("Plant.PlantArmor.PlantWhip.Knockback", 1.5);
        config.addDefault("Plant.PlantArmor.PlantWhip.AnglePower", 0.2);
        config.addDefault("Plant.PlantArmor.PlantWhip.DurabilityTake", 200);
        config.addDefault("Plant.PlantArmor.PlantWhip.CollisionRadius", 1);
        config.addDefault("Plant.PlantArmor.PlantWhip.RangeInt", 14);
        config.addDefault("Plant.PlantArmor.PlantWhip.GrowInterval", 100);
        config.addDefault("Plant.PlantArmor.PlantWhip.WhipDuration", 6000);
        config.addDefault("Plant.PlantArmor.PlantWhip.WhipCooldown", 2000);

        config.addDefault("Plant.PlantArmor.SharpLeaf.Speed", 1.5);
        config.addDefault("Plant.PlantArmor.SharpLeaf.Damage", 0.5);
        config.addDefault("Plant.PlantArmor.SharpLeaf.Range", 20);
        config.addDefault("Plant.PlantArmor.SharpLeaf.AngleDirection", 0.07);
        config.addDefault("Plant.PlantArmor.SharpLeaf.CollisionRadius", 0.5);
        config.addDefault("Plant.PlantArmor.SharpLeaf.Amount", 7);
        config.addDefault("Plant.PlantArmor.SharpLeaf.DurabilityTakeCount", 400);
        config.addDefault("Plant.PlantArmor.SharpLeaf.Cooldown", 8000);
        config.addDefault("Plant.PlantArmor.SharpLeaf.Message", "- Leafs left: {amount} -");

        config.addDefault("Plant.PlantArmor.TenaciousVine.Cooldown", 6000);
        config.addDefault("Plant.PlantArmor.TenaciousVine.ThrowPower", 2.5);
        config.addDefault("Plant.PlantArmor.TenaciousVine.VineSizeInt", 6);
        config.addDefault("Plant.PlantArmor.TenaciousVine.StunDuration", 2000);
        config.addDefault("Plant.PlantArmor.TenaciousVine.DurabilityTakeCount", 200);
        config.addDefault("Plant.PlantArmor.TenaciousVine.Message", "* Entangled in Vines *");

        config.addDefault("Plant.PlantArmor.VineGrapple.PullSpeed", 0.7);
        config.addDefault("Plant.PlantArmor.VineGrapple.AnglePower", 0.2);
        config.addDefault("Plant.PlantArmor.VineGrapple.RangeInt", 100);
        config.addDefault("Plant.PlantArmor.VineGrapple.GrowInterval", 35);
        config.addDefault("Plant.PlantArmor.VineGrapple.DurabilityTakeCount", 300);
        config.addDefault("Plant.PlantArmor.VineGrapple.Cooldown", 2000);
        config.addDefault("Plant.PlantArmor.VineGrapple.DurationIfMissed", 3000);

        config.addDefault("Plant.PlantArmor.Leap.PowerUpFactor", 0.5);
        config.addDefault("Plant.PlantArmor.Leap.MinimalPower", 1);
        config.addDefault("Plant.PlantArmor.Leap.MaxStages", 5);
        config.addDefault("Plant.PlantArmor.Leap.Cooldown", 7000);
        config.addDefault("Plant.PlantArmor.Leap.LevelUpInterval", 1000);
        config.addDefault("Plant.PlantArmor.Leap.DurabilityTakeCount", 200);

        config.addDefault("Plant.PlantArmor.RegeneratingAssembly.Cooldown", 10000);

        config.addDefault("Plant.Combos.VineWalk.FlySpeed", 0.1);
        config.addDefault("Plant.Combos.VineWalk.SourceRange", 15);
        config.addDefault("Plant.Combos.VineWalk.ReachSpeed", 0.5);
        config.addDefault("Plant.Combos.VineWalk.Duration", 10000);
        config.addDefault("Plant.Combos.VineWalk.Cooldown", 20000);
        config.addDefault("Plant.Combos.VineWalk.GrowInterval", 150);
        config.addDefault("Plant.Combos.VineWalk.RangeInt", 20);
        config.addDefault("Plant.Combos.VineWalk.VinesAmount", 5);
        config.addDefault("Plant.Combos.VineWalk.Description", "This combination will allow you to create vines from the plant that will help you move through the air");
        config.addDefault("Plant.Combos.VineWalk.Instructions", "(Torrent): sneak -> (WaterSpout): -> sneak, Look at the plant");
        config.addDefault("Plant.Combos.VineWalk.Combination", List.of("Torrent:SNEAK_DOWN", "Torrent:SNEAK_UP", "WaterSpout:SNEAK_DOWN", "WaterSpout:SNEAK_UP"));



        config.options().copyDefaults(true);
        plugin.saveConfig();


        //PlantArmor Specials ;-;
        final Map<Material, Integer> DEFAULT_VALUES = new HashMap<>();

        DEFAULT_VALUES.put(Material.MOSS_BLOCK, 100);
        DEFAULT_VALUES.put(Material.MOSS_CARPET, 50);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.PALE_MOSS_BLOCK, 100);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.PALE_MOSS_CARPET, 50);
        DEFAULT_VALUES.put(Material.OAK_LEAVES, 100);
        DEFAULT_VALUES.put(Material.BIRCH_LEAVES, 100);
        DEFAULT_VALUES.put(Material.SPRUCE_LEAVES, 100);
        DEFAULT_VALUES.put(Material.JUNGLE_LEAVES, 100);
        DEFAULT_VALUES.put(Material.ACACIA_LEAVES, 100);
        DEFAULT_VALUES.put(Material.DARK_OAK_LEAVES, 100);
        DEFAULT_VALUES.put(Material.MANGROVE_LEAVES, 100);
        DEFAULT_VALUES.put(Material.CHERRY_LEAVES, 100);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.PALE_OAK_LEAVES, 100);
        DEFAULT_VALUES.put(Material.AZALEA_LEAVES, 100);
        DEFAULT_VALUES.put(Material.FLOWERING_AZALEA_LEAVES, 100);
        DEFAULT_VALUES.put(Material.BROWN_MUSHROOM_BLOCK, 100);
        DEFAULT_VALUES.put(Material.RED_MUSHROOM_BLOCK, 100);
        DEFAULT_VALUES.put(Material.OAK_SAPLING, 75);
        DEFAULT_VALUES.put(Material.SPRUCE_SAPLING, 75);
        DEFAULT_VALUES.put(Material.BIRCH_SAPLING, 75);
        DEFAULT_VALUES.put(Material.JUNGLE_SAPLING, 75);
        DEFAULT_VALUES.put(Material.ACACIA_SAPLING, 75);
        DEFAULT_VALUES.put(Material.DARK_OAK_SAPLING, 75);
        DEFAULT_VALUES.put(Material.MANGROVE_PROPAGULE, 75);
        DEFAULT_VALUES.put(Material.CHERRY_SAPLING, 75);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.PALE_OAK_SAPLING, 75);
        DEFAULT_VALUES.put(Material.AZALEA, 75);
        DEFAULT_VALUES.put(Material.FLOWERING_AZALEA, 75);
        DEFAULT_VALUES.put(Material.BROWN_MUSHROOM, 25);
        DEFAULT_VALUES.put(Material.RED_MUSHROOM, 25);
        DEFAULT_VALUES.put(Material.CRIMSON_FUNGUS, 25);
        DEFAULT_VALUES.put(Material.WARPED_FUNGUS, 25);
        DEFAULT_VALUES.put(Material.SHORT_GRASS, 50);
        DEFAULT_VALUES.put(Material.TALL_GRASS, 60);
        DEFAULT_VALUES.put(Material.FERN, 50);
        if (GeneralMethods.getMCVersion() >= 1215) DEFAULT_VALUES.put(Material.BUSH, 50);
        DEFAULT_VALUES.put(Material.LARGE_FERN, 60);
        DEFAULT_VALUES.put(Material.DANDELION, 40);
        DEFAULT_VALUES.put(Material.POPPY, 40);
        DEFAULT_VALUES.put(Material.BLUE_ORCHID, 40);
        DEFAULT_VALUES.put(Material.ALLIUM, 40);
        DEFAULT_VALUES.put(Material.AZURE_BLUET, 40);
        DEFAULT_VALUES.put(Material.RED_TULIP, 40);
        DEFAULT_VALUES.put(Material.ORANGE_TULIP, 40);
        DEFAULT_VALUES.put(Material.WHITE_TULIP, 40);
        DEFAULT_VALUES.put(Material.PINK_TULIP, 40);
        DEFAULT_VALUES.put(Material.OXEYE_DAISY, 40);
        DEFAULT_VALUES.put(Material.CORNFLOWER, 40);
        DEFAULT_VALUES.put(Material.LILY_OF_THE_VALLEY, 40);
        DEFAULT_VALUES.put(Material.TORCHFLOWER, 40);
        DEFAULT_VALUES.put(Material.TORCHFLOWER_CROP, 40);
        if (GeneralMethods.getMCVersion() >= 1215) DEFAULT_VALUES.put(Material.CACTUS_FLOWER, 40);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.CLOSED_EYEBLOSSOM, 40);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.OPEN_EYEBLOSSOM, 40);
        DEFAULT_VALUES.put(Material.PINK_PETALS, 40);
        if (GeneralMethods.getMCVersion() >= 1215) DEFAULT_VALUES.put(Material.WILDFLOWERS, 40);
        DEFAULT_VALUES.put(Material.SPORE_BLOSSOM, 50);
        if (GeneralMethods.getMCVersion() >= 1215) DEFAULT_VALUES.put(Material.FIREFLY_BUSH, 50);
        DEFAULT_VALUES.put(Material.BAMBOO, 70);
        DEFAULT_VALUES.put(Material.SUGAR_CANE, 60);
        DEFAULT_VALUES.put(Material.CACTUS, 100);
        DEFAULT_VALUES.put(Material.CRIMSON_ROOTS, 40);
        DEFAULT_VALUES.put(Material.WARPED_ROOTS, 50);
        DEFAULT_VALUES.put(Material.NETHER_SPROUTS, 20);
        DEFAULT_VALUES.put(Material.WEEPING_VINES_PLANT, 50);
        DEFAULT_VALUES.put(Material.TWISTING_VINES_PLANT, 50);
        DEFAULT_VALUES.put(Material.VINE, 40);
        DEFAULT_VALUES.put(Material.SUNFLOWER, 60);
        DEFAULT_VALUES.put(Material.LILAC, 60);
        DEFAULT_VALUES.put(Material.ROSE_BUSH, 60);
        DEFAULT_VALUES.put(Material.PITCHER_PLANT, 60);
        DEFAULT_VALUES.put(Material.PITCHER_CROP, 40);
        DEFAULT_VALUES.put(Material.BIG_DRIPLEAF, 60);
        DEFAULT_VALUES.put(Material.BIG_DRIPLEAF_STEM, 20);
        DEFAULT_VALUES.put(Material.SMALL_DRIPLEAF, 30);
        DEFAULT_VALUES.put(Material.CHORUS_PLANT, 60);
        DEFAULT_VALUES.put(Material.CHORUS_FLOWER, 60);
        DEFAULT_VALUES.put(Material.GLOW_LICHEN, 10);
        DEFAULT_VALUES.put(Material.HANGING_ROOTS, 40);
        DEFAULT_VALUES.put(Material.CAVE_VINES_PLANT, 40);
        DEFAULT_VALUES.put(Material.CAVE_VINES, 40);
        DEFAULT_VALUES.put(Material.SWEET_BERRY_BUSH, 40);
        DEFAULT_VALUES.put(Material.NETHER_WART_BLOCK, 40);
        DEFAULT_VALUES.put(Material.LILY_PAD, 40);
        DEFAULT_VALUES.put(Material.SEAGRASS, 40);
        DEFAULT_VALUES.put(Material.TALL_SEAGRASS, 70);
        DEFAULT_VALUES.put(Material.KELP_PLANT, 50);
        DEFAULT_VALUES.put(Material.PUMPKIN, 70);
        DEFAULT_VALUES.put(Material.ATTACHED_PUMPKIN_STEM, 20);
        DEFAULT_VALUES.put(Material.PUMPKIN_STEM, 20);
        DEFAULT_VALUES.put(Material.CARVED_PUMPKIN, 30);
        DEFAULT_VALUES.put(Material.MELON, 100);
        DEFAULT_VALUES.put(Material.MELON_STEM, 50);
        DEFAULT_VALUES.put(Material.ATTACHED_MELON_STEM, 50);
        DEFAULT_VALUES.put(Material.TUBE_CORAL_BLOCK, 100);
        DEFAULT_VALUES.put(Material.BRAIN_CORAL_BLOCK, 100);
        DEFAULT_VALUES.put(Material.BUBBLE_CORAL_BLOCK, 100);
        DEFAULT_VALUES.put(Material.FIRE_CORAL_BLOCK, 100);
        DEFAULT_VALUES.put(Material.HORN_CORAL_BLOCK, 100);
        DEFAULT_VALUES.put(Material.TUBE_CORAL, 50);
        DEFAULT_VALUES.put(Material.BRAIN_CORAL, 50);
        DEFAULT_VALUES.put(Material.BUBBLE_CORAL, 50);
        DEFAULT_VALUES.put(Material.FIRE_CORAL, 50);
        DEFAULT_VALUES.put(Material.HORN_CORAL, 50);
        DEFAULT_VALUES.put(Material.TUBE_CORAL_FAN, 50);
        DEFAULT_VALUES.put(Material.BRAIN_CORAL_FAN, 50);
        DEFAULT_VALUES.put(Material.BUBBLE_CORAL_FAN, 50);
        DEFAULT_VALUES.put(Material.FIRE_CORAL_FAN, 50);
        DEFAULT_VALUES.put(Material.HORN_CORAL_FAN, 50);
        DEFAULT_VALUES.put(Material.TUBE_CORAL_WALL_FAN, 50);
        DEFAULT_VALUES.put(Material.BRAIN_CORAL_WALL_FAN, 50);
        DEFAULT_VALUES.put(Material.BUBBLE_CORAL_WALL_FAN, 50);
        DEFAULT_VALUES.put(Material.FIRE_CORAL_WALL_FAN, 50);
        DEFAULT_VALUES.put(Material.HORN_CORAL_WALL_FAN, 50);
        if (GeneralMethods.getMCVersion() >= 1214) DEFAULT_VALUES.put(Material.PALE_HANGING_MOSS, 50);



        File file = new File(plugin.getDataFolder(), "PlantArmor_contents.json");

        if (!file.exists()) {
            try (Writer writer = new FileWriter(file)) {
                plantArmor_values.toJson(DEFAULT_VALUES, writer);
                PlantArmor.PLANT_VOLUMES.putAll(DEFAULT_VALUES);
            } catch (IOException e) {
                shutdown();
            }
        } else {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Integer>>() {}.getType();
                Map<String, Integer> rawMap = plantArmor_values.fromJson(reader, type);

                Map<Material, Integer> safeMap = new HashMap<>();
                for (Map.Entry<String, Integer> entry : rawMap.entrySet()) {
                    try {
                        Material mat = Material.valueOf(entry.getKey().toUpperCase());
                        safeMap.put(mat, entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                PlantArmor.PLANT_VOLUMES.putAll(safeMap);

            } catch (IOException e) {
                shutdown();
            }
        }



    }
}
