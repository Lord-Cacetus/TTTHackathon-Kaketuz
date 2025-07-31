package me.kaketuz.hackathon.abilities.plant;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.util.TempBlock;
import me.kaketuz.hackathon.Hackathon;
import me.kaketuz.hackathon.util.GradientAPI;
import me.kaketuz.hackathon.util.HackathonMethods;
import me.kaketuz.nightmarelib.lib.logger.Logger;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bamboo;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class PlantArmor extends PlantAbility implements AddonAbility, MultiAbility {
    
    //General
    private final Set<LocalTask> TASKS = new HashSet<>();
    private boolean isForming, isFormed;
    private BossBar durabilityBar;
    private int maxDurability, currentDurability;
    private ItemStack[] OLD_ARMOR;
    private String durabilityTitle;
    private int jumpBoost, speedBoost, dolphinGraceBoost;

    //Ill add more soon
    public static Map<Biome, BlockData> VINE_SKINS = new HashMap<>() {{
        if (GeneralMethods.getMCVersion() >= 1214) put(Biome.PALE_GARDEN, Material.PALE_HANGING_MOSS.createBlockData());
        put(Biome.CRIMSON_FOREST, Material.WEEPING_VINES_PLANT.createBlockData());
        put(Biome.WARPED_FOREST, Material.TWISTING_VINES_PLANT.createBlockData());
        put(Biome.JUNGLE, Material.BAMBOO.createBlockData(dat -> ((Bamboo)dat).setLeaves(Bamboo.Leaves.values()[ThreadLocalRandom.current().nextInt(0, Bamboo.Leaves.values().length)])));
        put(Biome.BAMBOO_JUNGLE, Material.BAMBOO.createBlockData(dat -> ((Bamboo)dat).setLeaves(Bamboo.Leaves.values()[ThreadLocalRandom.current().nextInt(0, Bamboo.Leaves.values().length)])));
        put(Biome.SPARSE_JUNGLE, Material.BAMBOO.createBlockData(dat -> ((Bamboo)dat).setLeaves(Bamboo.Leaves.values()[ThreadLocalRandom.current().nextInt(0, Bamboo.Leaves.values().length)])));
        put(Biome.LUSH_CAVES, Material.CAVE_VINES_PLANT.createBlockData(dat -> ((CaveVinesPlant)dat).setBerries(ThreadLocalRandom.current().nextBoolean())));
    }};
    
    //Collection Fields
    public static final Map<Material, Integer> PLANT_VOLUMES = new HashMap<>();
    private final List<Color> COLLECTED_COLORS = new ArrayList<>();
    private final Set<TempBlock> AFFECTED_BLOCKS = new HashSet<>();
    private double collectSpeed, collectRange;

    
    
    public PlantArmor(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, PlantArmor.class)) return;

        collectRange = Hackathon.config.getDouble("Plant.PlantArmor.Collection.CollectRange");
        collectSpeed = Hackathon.config.getDouble("Plant.PlantArmor.Collection.CollectSpeed");
        durabilityTitle = Hackathon.config.getString("Plant.PlantArmor.General.DurabilityTitle");
        jumpBoost = Hackathon.config.getInt("Plant.PlantArmor.General.JumpBoost");
        speedBoost = Hackathon.config.getInt("Plant.PlantArmor.General.SpeedBoost");
        dolphinGraceBoost = Hackathon.config.getInt("Plant.PlantArmor.General.DolphinGraceBoost");
        maxDurability = Hackathon.config.getInt("Plant.PlantArmor.General.MaxDurability");
        isForming = true;

        durabilityBar = Bukkit.createBossBar(durabilityTitle, BarColor.GREEN, BarStyle.SOLID);
        durabilityBar.setProgress(0);
        durabilityBar.addPlayer(player);

        start();
    }

    @Override
    public void remove() {
        super.remove();
        durabilityBar.removeAll();
        MultiAbilityManager.unbindMultiAbility(player);
        TASKS.forEach(LocalTask::remove);
        AFFECTED_BLOCKS.forEach(TempBlock::revertBlock);
        if (isFormed) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            player.removePotionEffect(GeneralMethods.getMCVersion() >= 1205 ? PotionEffectType.JUMP_BOOST : PotionEffectType.getByName("JUMP"));
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            bPlayer.addCooldown(this);
            remove();
        }

        TASKS.forEach(LocalTask::update);

        if (maxDurability < currentDurability) {
            TASKS.stream()
                    .filter(lt -> lt instanceof MovingBlockTask)
                    .map(lt -> ((MovingBlockTask)lt))
                    .forEach(MovingBlockTask::remove);
            if (isForming) {
                form();
            }
            currentDurability = maxDurability;
        }

        durabilityBar.setProgress(Math.max(0, Math.min(1, currentDurability / maxDurability)));
        String finalTitle = durabilityTitle
                .replace("{current}", String.valueOf(currentDurability))
                .replace("{max}", String.valueOf(maxDurability));
        durabilityBar.setTitle(GradientAPI.colorize(finalTitle));

        if (isForming && !player.isSneaking()) remove();
        if (isForming && player.isSneaking()) {
            GeneralMethods.getBlocksAroundPoint(player.getLocation(), collectRange).stream()
                    .filter(b -> HackathonMethods.hasSolidBlocksBetween(b.getLocation(), player.getLocation(), b2 -> !GeneralMethods.isSolid(b2) || isPlant(b2)))
                    .filter(b -> PLANT_VOLUMES.containsKey(b.getType()))
                    .forEach(MovingBlockTask::new);
        }
        if (isFormed) {
            if (jumpBoost > 0) player.addPotionEffect(new PotionEffect(GeneralMethods.getMCVersion() >= 1205 ? PotionEffectType.JUMP_BOOST : PotionEffectType.getByName("JUMP"), 20, jumpBoost - 1, true, false, false));
            if (speedBoost > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, speedBoost - 1, true, false, false));
            if (dolphinGraceBoost > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20, dolphinGraceBoost - 1, true, false, false));
        }
    }

    public void form() {
        OLD_ARMOR = new ItemStack[] {
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots()
        };

        Color finalColor = HackathonMethods.averageColor(COLLECTED_COLORS);
        ItemStack helmet = new ItemStack(ThreadLocalRandom.current().nextBoolean() ? Material.AZALEA : Material.FLOWERING_AZALEA);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        setupArmor(chestplate, finalColor, "<#247A4E>- ᴅᴇᴄɪᴅᴜᴏᴜs ᴄʜᴇsᴛᴘʟᴀᴛᴇ -</#7CCF36>");
        setupArmor(leggings, finalColor, "<#247A4E>- ʙʀᴏᴀᴅʟᴇᴀꜰ ʟᴇɢɢɪɴɢs -</#7CCF36>");
        setupArmor(boots, finalColor, "<#247A4E>- ᴘʟᴀɴᴛ ʙᴏᴏᴛs -</#7CCF36>");

        ItemMeta hMeta = helmet.getItemMeta();
        assert hMeta != null;
        hMeta.setUnbreakable(true);
        hMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        hMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTMENT_GLINT_OVERRIDE, ItemFlag.HIDE_ENCHANTS);
        hMeta.setItemName(GradientAPI.colorize("<#247A4E>- ʜᴀɴɢɪɴɢ ᴍᴏss -</#7CCF36>"));
        hMeta.setLore(List.of("", ChatColor.of("#247A4E") + " - Just a piece of moss that you put on your head for fun :/"));
        helmet.setItemMeta(hMeta);

        MultiAbilityManager.bindMultiAbility(player, getName());
        isForming = false;
        isFormed = true;
    }

    private void setupArmor(ItemStack item, Color color, String name) {
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta == null) return;

        meta.setColor(color);
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTMENT_GLINT_OVERRIDE, ItemFlag.HIDE_ENCHANTS);
        meta.setItemName(GradientAPI.colorize(name));

        item.setItemMeta(meta);
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "PlantArmor";
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void load() {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getAuthor() {
        return GradientAPI.colorize("<#247A4E>Lᴏʀᴅ_Cᴀᴄᴇᴛᴜs_ & Sɪᴍᴘʟɪᴄɪᴛᴇᴇ</#7CCF36>");
    }

    @Override
    public String getVersion() {
        return GradientAPI.colorize("<#247A4E>Hᴀᴄᴋᴀᴛʜᴏɴ - KᴀᴋᴇᴛᴜZ</#7CCF36>");
    }

    @Override
    public ArrayList<MultiAbilityManager.MultiAbilityInfoSub> getMultiAbilities() {
        return new ArrayList<>(Arrays.asList(
                new MultiAbilityManager.MultiAbilityInfoSub("VineWhip", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("TralaleloTralala", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("BombardiroCorocodilo", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("BrBrPatapim", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("TunTunTunTunSagur", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("BallerinaCappuchina", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("LiriliLarila", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("ShpioniroGolubiro", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("WithdrawalPlants", Element.PLANT)
        ));
    }
    

    private interface LocalTask {
        void update();
        void remove();
         default boolean isRemoved() {
             try {
                 Field listField = PlantArmor.class.getDeclaredField("TASKS");
                 listField.setAccessible(true);
                 List<LocalTask> list = (List<LocalTask>) listField.get(this);
                 return !list.contains(this);
             } catch (Exception e) {
                 Logger.sendError(e.getMessage());
                 remove();
             }
             return true;
         }
    }

    public boolean isFormed() {
        return isFormed;
    }

    public boolean isForming() {
        return isForming;
    }

    private class MovingBlockTask implements LocalTask {

        private BlockDisplay display;
        private Block block;

        public int carryingVolume;


        public MovingBlockTask(Block block) {
            this.block = block;
            if (PLANT_VOLUMES.containsKey(block.getType())) {
                AFFECTED_BLOCKS.add(new TempBlock(block, Material.AIR));
                display = (BlockDisplay) block.getWorld().spawnEntity(block.getLocation().add(0.5, 0.5, 0.5), EntityType.BLOCK_DISPLAY);
                display.setPersistent(false);
                display.setBlock(block.getBlockData());
                display.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new Quaternionf(),
                        new Vector3f(1, 1, 1),
                        new Quaternionf()
                ));
                player.sendMessage(block.getType() + "/" + PLANT_VOLUMES.containsKey(block.getType()) + "/" + PLANT_VOLUMES.size());
                carryingVolume = PLANT_VOLUMES.get(block.getType()); //HOW ITS THROWS AN ERROR JUST HOOOOOW

                TASKS.add(this);
            }
        }

        @Override
        public void update() {
            if (!PLANT_VOLUMES.containsKey(block.getType())) {
                remove();
                return;
            }
            display.setTeleportDuration(2);
            Location target = display.getLocation();
            target.setPitch(target.getPitch() + 0.1f);
            target.setYaw(target.getYaw() + 1);
            display.teleport(target.add(GeneralMethods.getDirection(display.getLocation(), player.getEyeLocation()).normalize().multiply(collectSpeed)));
            if (player.getEyeLocation().distance(target) < 1) {
                COLLECTED_COLORS.add(block.getBlockData().getMapColor());
                currentDurability += carryingVolume;
                remove();
            }
        }

        @Override
        public void remove() {
            display.remove();
            TASKS.remove(this);
        }

    }

}
