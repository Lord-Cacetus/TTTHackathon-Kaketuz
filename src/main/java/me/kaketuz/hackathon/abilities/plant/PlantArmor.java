package me.kaketuz.hackathon.abilities.plant;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.board.BendingBoard;
import com.projectkorra.projectkorra.board.BendingBoardManager;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ActionBar;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import jdk.jshell.execution.LocalExecutionControl;
import me.kaketuz.hackathon.Hackathon;
import me.kaketuz.hackathon.util.GradientAPI;
import me.kaketuz.hackathon.util.HackathonMethods;
import me.kaketuz.hackathon.util.Pair;
import me.kaketuz.hackathon.util.logger.Logger;
import me.kaketuz.hackathon.util.verlet.VerletPoint;
import me.kaketuz.hackathon.util.verlet.VerletRope;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2dc;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PlantArmor extends PlantAbility implements AddonAbility, MultiAbility {



     //                        ------------- PlantArmor --------------
     //                                    - Code Guide -
     //           MovingBlockTask - 442 line
     //
     //
     //
    
    //General
    private final Map<String, Pair<Long, Long>> COOLDOWNS = new HashMap<>();
    private final Set<LocalTask> TASKS = ConcurrentHashMap.newKeySet();
    private boolean isForming, isFormed;
    private BossBar durabilityBar;
    private int maxDurability, currentDurability;
    private ItemStack[] OLD_ARMOR;
    private String durabilityTitle;
    private int jumpBoost, speedBoost, dolphinGraceBoost;
    private static final NamespacedKey ARMOR_UNIQUE_KEY = new NamespacedKey(Hackathon.plugin, "ability.plantarmor.armorkey");
    private BlockData mostCommonBlock;
    private final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    //Ill add more soon
    private static final Map<Biome, BlockData[][]> VINE_SKINS = new HashMap<>() {{
        //All potential forms
        BlockData[][] bamboo_specials = new BlockData[][] {{
                Material.BAMBOO.createBlockData(dat -> ((Bamboo) dat).setLeaves(Bamboo.Leaves.NONE)),
                Material.BAMBOO.createBlockData(dat -> ((Bamboo) dat).setLeaves(Bamboo.Leaves.SMALL)),
                Material.BAMBOO.createBlockData(dat -> ((Bamboo) dat).setLeaves(Bamboo.Leaves.LARGE))
        }};
        BlockData[][] cave_vines_specials = new BlockData[][] {{
                Material.CAVE_VINES_PLANT.createBlockData(dat -> ((CaveVinesPlant) dat).setBerries(true)),
                Material.CAVE_VINES_PLANT.createBlockData(dat -> ((CaveVinesPlant) dat).setBerries(false))
        }};

        BlockData[][] warm_ocean_specials = new BlockData[][] {
                {Material.TUBE_CORAL.createBlockData()},
                {Material.BRAIN_CORAL.createBlockData()},
                {Material.BUBBLE_CORAL.createBlockData()},
                {Material.FIRE_CORAL.createBlockData()},
                {Material.HORN_CORAL.createBlockData()}
        };

        if (GeneralMethods.getMCVersion() >= 1214) put(Biome.PALE_GARDEN, new BlockData[][]{{Material.PALE_HANGING_MOSS.createBlockData()}});
        put(Biome.CRIMSON_FOREST, new BlockData[][]{{Material.WEEPING_VINES_PLANT.createBlockData()}});
        put(Biome.WARPED_FOREST, new BlockData[][]{{Material.TWISTING_VINES_PLANT.createBlockData()}});
        put(Biome.JUNGLE, bamboo_specials);
        put(Biome.BAMBOO_JUNGLE, bamboo_specials);
        put(Biome.SPARSE_JUNGLE, bamboo_specials);
        put(Biome.LUSH_CAVES, cave_vines_specials);
        put(Biome.WARM_OCEAN, warm_ocean_specials);
        put(Biome.OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.LUKEWARM_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.DEEP_COLD_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.DEEP_FROZEN_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.DEEP_LUKEWARM_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.FROZEN_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.DEEP_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.COLD_OCEAN, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.RIVER, new BlockData[][]{{Material.KELP_PLANT.createBlockData()}});
        put(Biome.DEEP_DARK, new BlockData[][]{{Material.SCULK_VEIN.createBlockData()}});
        put(Biome.MANGROVE_SWAMP, new BlockData[][]{{Material.VINE.createBlockData()}, {Material.SUGAR_CANE.createBlockData()}});
        put(Biome.SWAMP, new BlockData[][]{{Material.VINE.createBlockData()}, {Material.SUGAR_CANE.createBlockData()}});
        put(Biome.DRIPSTONE_CAVES, new BlockData[][]{{Material.HANGING_ROOTS.createBlockData()}});

    }};

    public static Map<List<Material>, List<Biome>> BIOME_CONTENTS = new HashMap<>() {{
            put(List.of(
                    Material.MOSS_BLOCK,
                    Material.MOSS_CARPET,
                    Material.CAVE_VINES,
                    Material.CAVE_VINES_PLANT,
                    Material.AZALEA,
                    Material.AZALEA_LEAVES,
                    Material.FLOWERING_AZALEA,
                    Material.FLOWERING_AZALEA_LEAVES,
                    Material.HANGING_ROOTS,
                    Material.BIG_DRIPLEAF,
                    Material.BIG_DRIPLEAF_STEM,
                    Material.SMALL_DRIPLEAF
            ), List.of(Biome.LUSH_CAVES));
            put(List.of(
                    Material.BAMBOO
            ), List.of(Biome.JUNGLE, Biome.BAMBOO_JUNGLE, Biome.SPARSE_JUNGLE));
            if (GeneralMethods.getMCVersion() >= 1214) put(List.of(
                    Material.PALE_HANGING_MOSS,
                    Material.PALE_MOSS_BLOCK,
                    Material.PALE_MOSS_CARPET,
                    Material.PALE_OAK_LEAVES,
                    Material.PALE_OAK_SAPLING,
                    Material.CLOSED_EYEBLOSSOM,
                    Material.OPEN_EYEBLOSSOM
            ), List.of(Biome.PALE_GARDEN));

            put(List.of(
                    Material.WEEPING_VINES,
                    Material.WEEPING_VINES_PLANT,
                    Material.CRIMSON_FUNGUS,
                    Material.NETHER_WART_BLOCK,
                    Material.CRIMSON_ROOTS
            ), List.of(Biome.CRIMSON_FOREST));

            put(List.of(
                    Material.WARPED_ROOTS,
                    Material.WARPED_WART_BLOCK,
                    Material.NETHER_SPROUTS,
                    Material.WARPED_FUNGUS,
                    Material.TWISTING_VINES,
                    Material.TWISTING_VINES_PLANT
            ), List.of(Biome.WARPED_FOREST));

            put(List.of(
                    Material.BUBBLE_CORAL,
                    Material.TUBE_CORAL,
                    Material.BRAIN_CORAL,
                    Material.HORN_CORAL,
                    Material.FIRE_CORAL,
                    Material.BUBBLE_CORAL_BLOCK,
                    Material.TUBE_CORAL_BLOCK,
                    Material.BRAIN_CORAL_BLOCK,
                    Material.HORN_CORAL_BLOCK,
                    Material.FIRE_CORAL_BLOCK,
                    Material.BUBBLE_CORAL_FAN,
                    Material.TUBE_CORAL_FAN,
                    Material.BRAIN_CORAL_FAN,
                    Material.HORN_CORAL_FAN,
                    Material.FIRE_CORAL_FAN,
                    Material.BUBBLE_CORAL_WALL_FAN,
                    Material.TUBE_CORAL_WALL_FAN,
                    Material.BRAIN_CORAL_WALL_FAN,
                    Material.HORN_CORAL_WALL_FAN,
                    Material.FIRE_CORAL_WALL_FAN
            ), List.of(Biome.WARM_OCEAN));
            put(List.of(
                    Material.KELP_PLANT,
                    Material.SEA_PICKLE,
                    Material.SEAGRASS,
                    Material.TALL_SEAGRASS
            ), List.of(Biome.FROZEN_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.OCEAN, Biome.DEEP_COLD_OCEAN, Biome.LUKEWARM_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.RIVER));
            put(List.of(
                    Material.MANGROVE_PROPAGULE,
                    Material.MANGROVE_LEAVES,
                    Material.MANGROVE_ROOTS,
                    Material.MUDDY_MANGROVE_ROOTS,
                    Material.VINE
            ), List.of(Biome.MANGROVE_SWAMP, Biome.SWAMP));
    }};

    private final Map<Biome, Pair<BlockData, Class<? extends BlockData>>> PASSIVE_SKINS = GeneralMethods.getMCVersion() >= 1215 ? new HashMap<>() {{
        put(Biome.BIRCH_FOREST, new Pair<>(Material.WILDFLOWERS.createBlockData(), FlowerBed.class));
        put(Biome.OLD_GROWTH_BIRCH_FOREST, new Pair<>(Material.WILDFLOWERS.createBlockData(), FlowerBed.class));
        put(Biome.MEADOW, new Pair<>(Material.WILDFLOWERS.createBlockData(), FlowerBed.class));
        put(Biome.FOREST, new Pair<>(Material.LEAF_LITTER.createBlockData(), LeafLitter.class));
        put(Biome.DARK_FOREST, new Pair<>(Material.LEAF_LITTER.createBlockData(), LeafLitter.class));
        put(Biome.PALE_GARDEN, new Pair<>(Material.LEAF_LITTER.createBlockData(), LeafLitter.class));
        put(Biome.WOODED_BADLANDS, new Pair<>(Material.LEAF_LITTER.createBlockData(), LeafLitter.class));
    }} : new HashMap<>();



    private boolean isRegenerating;

    private String cooldownMessage, notEnoughDurabilityMessage;
    
    //Collection Fields
    public static final Map<Material, Integer> PLANT_VOLUMES = new HashMap<>();
    private final List<Color> COLLECTED_COLORS = new ArrayList<>();
    private final Set<TempBlock> AFFECTED_BLOCKS = new HashSet<>();
    private final Map<Material, Integer> AFFECTED_MATERIALS_LEADERBOARD = new EnumMap<>(Material.class);
    private double collectSpeed, collectRange;
    private int collectInterval, i;

    //VineWhip
    private int whipRange, whipGrowInterval, whipDurationTakeCount;
    private long whipDuration, whipCooldown;
    private double whipDamage, whipKnockback, whipAnglePower, whipCollisionRadius, whipMinSlapPower;

    //SharpLeafs
    private int leafsAmount, leafsDurabilityTakeCount;
    private double leafsSpeed, leafsDamage, leafsCollisionRadius, leafsAngleDirection, leafsRange;
    private long leafsCooldown;
    private String leafLeftAmountMessage;

    //TenaciousVine
    private int tenaciousVineRange, tenaciousVineDurabilityTakeCount;
    private long tenaciousVineStunDuration, tenaciousVineCooldown;
    private String tenaciousVineStunMessage;
    private double tenaciousVinePower;

    //VineGrabble
    private int grappleRange, grappleGrowInterval, grappleDurabilityTakeCount;
    private long grappleCooldown, grappleDurationIfMissed;
    private double grappleAnglePower, grapplePullSpeed;

    //Leap
    private int leapMaxStages, leapDurabilityTakeCount;
    private double leapPowerPerPower, leapMinPower;

    private long leapCooldown, leapLevelupInterval;

    //LeafShield
    private double shieldRadius, shieldOffset, shieldThrowSpeed, shieldThrowDamage, shieldThrowRange, shieldSphereRadius;
    private long shieldSphereDuration, shieldThrowCooldown, shieldCooldown;
    private int shieldDurabilityTakeCount;

    //Dome
    private int domeGrowInterval, domeDurabilityTakeCount;
    private double domeRadius, domeSegmentDamage;
    private long domeDuration, domeCooldown;


    //RegeneratingAssembly
    private long regenCooldown;

    
    
    public PlantArmor(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, PlantArmor.class)) return;

        collectRange = Hackathon.config.getDouble("Plant.PlantArmor.Collection.CollectRange");
        collectSpeed = Hackathon.config.getDouble("Plant.PlantArmor.Collection.CollectSpeed");
        collectInterval = Hackathon.config.getInt("Plant.PlantArmor.Collection.CollectInterval");

        durabilityTitle = Hackathon.config.getString("Plant.PlantArmor.General.DurabilityTitle");
        cooldownMessage = Hackathon.config.getString("Plant.PlantArmor.General.CooldownMessage");
        notEnoughDurabilityMessage = Hackathon.config.getString("Plant.PlantArmor.General.NotEnoughDurabilityMessage");
        jumpBoost = Hackathon.config.getInt("Plant.PlantArmor.General.JumpBoost");
        speedBoost = Hackathon.config.getInt("Plant.PlantArmor.General.SpeedBoost");
        dolphinGraceBoost = Hackathon.config.getInt("Plant.PlantArmor.General.DolphinGraceBoost");
        maxDurability = Hackathon.config.getInt("Plant.PlantArmor.General.MaxDurability");

        whipDamage = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.Damage");
        whipMinSlapPower = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.MinimalSlapPower");
        whipKnockback = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.Knockback");
        whipAnglePower = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.AnglePower");
        whipCollisionRadius = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.CollisionRadius");
        whipRange = Hackathon.config.getInt("Plant.PlantArmor.PlantWhip.RangeInt");
        whipGrowInterval = Hackathon.config.getInt("Plant.PlantArmor.PlantWhip.GrowInterval");
        whipDurationTakeCount = Hackathon.config.getInt("Plant.PlantArmor.PlantWhip.DurabilityTake");
        whipDuration = Hackathon.config.getLong("Plant.PlantArmor.PlantWhip.WhipDuration");
        whipCooldown = Hackathon.config.getLong("Plant.PlantArmor.PlantWhip.WhipCooldown");

        leafsSpeed = Hackathon.config.getDouble("Plant.PlantArmor.SharpLeaf.Speed");
        leafsDamage = Hackathon.config.getDouble("Plant.PlantArmor.SharpLeaf.Damage");
        leafsRange = Hackathon.config.getDouble("Plant.PlantArmor.SharpLeaf.Range");
        leafsAngleDirection = Hackathon.config.getDouble("Plant.PlantArmor.SharpLeaf.AngleDirection");
        leafsCollisionRadius = Hackathon.config.getDouble("Plant.PlantArmor.SharpLeaf.CollisionRadius");
        leafsAmount = Hackathon.config.getInt("Plant.PlantArmor.SharpLeaf.Amount");
        leafsDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.SharpLeaf.DurabilityTakeCount");
        leafsCooldown = Hackathon.config.getLong("Plant.PlantArmor.SharpLeaf.Cooldown");
        leafLeftAmountMessage = Hackathon.config.getString("Plant.PlantArmor.SharpLeaf.Message");

        tenaciousVineCooldown = Hackathon.config.getLong("Plant.PlantArmor.TenaciousVine.Cooldown");
        tenaciousVinePower = Hackathon.config.getDouble("Plant.PlantArmor.TenaciousVine.ThrowPower");
        tenaciousVineRange = Hackathon.config.getInt("Plant.PlantArmor.TenaciousVine.VineSizeInt");
        tenaciousVineStunDuration = Hackathon.config.getLong("Plant.PlantArmor.TenaciousVine.StunDuration");
        tenaciousVineDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.TenaciousVine.DurabilityTakeCount");
        tenaciousVineStunMessage = Hackathon.config.getString("Plant.PlantArmor.TenaciousVine.Message");

        grapplePullSpeed = Hackathon.config.getDouble("Plant.PlantArmor.VineGrapple.PullSpeed");
        grappleAnglePower = Hackathon.config.getDouble("Plant.PlantArmor.VineGrapple.AnglePower");
        grappleRange = Hackathon.config.getInt("Plant.PlantArmor.VineGrapple.RangeInt");
        grappleGrowInterval = Hackathon.config.getInt("Plant.PlantArmor.VineGrapple.GrowInterval");
        grappleDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.VineGrapple.DurabilityTakeCount");
        grappleCooldown = Hackathon.config.getLong("Plant.PlantArmor.VineGrapple.Cooldown");
        grappleDurationIfMissed = Hackathon.config.getLong("Plant.PlantArmor.VineGrapple.DurationIfMissed");

        leapPowerPerPower = Hackathon.config.getDouble("Plant.PlantArmor.Leap.PowerUpFactor");
        leapMinPower = Hackathon.config.getDouble("Plant.PlantArmor.Leap.MinimalPower");
        leapMaxStages = Hackathon.config.getInt("Plant.PlantArmor.Leap.MaxStages");
        leapCooldown = Hackathon.config.getLong("Plant.PlantArmor.Leap.Cooldown");
        leapLevelupInterval = Hackathon.config.getInt("Plant.PlantArmor.Leap.LevelUpInterval");
        leapDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.Leap.DurabilityTakeCount");

        shieldOffset = Hackathon.config.getDouble("Plant.PlantArmor.LeafShield.Offset");
        shieldRadius = Hackathon.config.getDouble("Plant.PlantArmor.LeafShield.Radius");
        shieldThrowRange = Hackathon.config.getDouble("Plant.PlantArmor.LeafShield.Throw.Range");
        shieldThrowDamage = Hackathon.config.getDouble("Plant.PlantArmor.LeafShield.Throw.Damage");
        shieldThrowSpeed = Hackathon.config.getDouble("Plant.PlantArmor.LeafShield.Throw.Speed");
        shieldThrowCooldown = Hackathon.config.getLong("Plant.PlantArmor.LeafShield.Throw.Cooldown");
        shieldSphereRadius = Hackathon.config.getDouble("Plant.PlantArmor.LeafShield.Sphere.Radius");
        shieldSphereDuration = Hackathon.config.getLong("Plant.PlantArmor.LeafShield.Sphere.Duration");
        shieldDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.LeafShield.DurabilityTakeCount");
        shieldCooldown = Hackathon.config.getLong("Plant.PlantArmor.LeafShield.Cooldown");

        domeRadius = Hackathon.config.getDouble("Plant.PlantArmor.Dome.Radius");
        domeSegmentDamage = Hackathon.config.getDouble("Plant.PlantArmor.Dome.SegmentDamage");
        domeCooldown = Hackathon.config.getLong("Plant.PlantArmor.Dome.Cooldown");
        domeDuration = Hackathon.config.getLong("Plant.PlantArmor.Dome.Duration");
        domeGrowInterval = Hackathon.config.getInt("Plant.PlantArmor.Dome.GrowInterval");
        domeDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.Dome.DurabilityTakeCount");

        regenCooldown = Hackathon.config.getLong("Plant.PlantArmor.RegeneratingAssembly.Cooldown");

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
            player.getInventory().setHelmet(OLD_ARMOR[0]);
            player.getInventory().setChestplate(OLD_ARMOR[1]);
            player.getInventory().setLeggings(OLD_ARMOR[2]);
            player.getInventory().setBoots(OLD_ARMOR[3]);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            player.removePotionEffect(GeneralMethods.getMCVersion() >= 1205 ? PotionEffectType.JUMP_BOOST : PotionEffectType.getByName("JUMP"));
        }
    }

    public void updateActionBarMessages() {

        COOLDOWNS.keySet().removeIf(s -> !bPlayer.isOnCooldown(s));

        getMultiAbilities().forEach(m -> {

            if (ChatColor.stripColor(bPlayer.getBoundAbilityName()).equalsIgnoreCase(m.getName())) {
               if (!bPlayer.isOnCooldown("PlantArmorActionMsg")) {
                    if (bPlayer.isOnCooldown(m.getName())) {
                        if (player.getInventory().getHeldItemSlot() == 1 && hasPlantArmorSubAbility(SharpLeafsTask.class)) {
                            ActionBar.sendActionBar(getElement().getColor() + leafLeftAmountMessage.replace("{amount}", String.valueOf(leafsAmount - getPlantArmorSubAbility(SharpLeafsTask.class).orElseThrow().counter + 1)), player);
                        }
                        else {
                            final Pair<Long, Long> cooldown = COOLDOWNS.get(m.getName());
                            ActionBar.sendActionBar(ChatColor.RED + "❌ " + ChatColor.STRIKETHROUGH + m.getName() + ChatColor.RESET + ChatColor.RED + " - " + HackathonMethods.round((double) ((cooldown.getLeft() + cooldown.getRight()) - System.currentTimeMillis()) / 1000, 1) + "s", player);
                        }
                    }
                    else {
                        ActionBar.sendActionBar(ChatColor.GREEN + "✅ " + m.getAbilityColor() + m.getName(), player);
                    }
                }
            }
        });
    }



    public void updateBossbarDurabilityColor() {
        if (durabilityBar.getProgress() <= 0.15) {
            durabilityBar.setColor(BarColor.RED);
        }
        else if (durabilityBar.getProgress() <= 0.5) {
            durabilityBar.setColor(BarColor.YELLOW);
        } else {
            durabilityBar.setColor(BarColor.GREEN);
        }
    }

    private void buildTitle() {
        durabilityBar.setProgress((double) currentDurability / maxDurability);
        String finalTitle = durabilityTitle
                .replace("{current}", String.valueOf(currentDurability))
                .replace("{max}", String.valueOf(maxDurability));
        durabilityBar.setTitle(GradientAPI.colorize("<#247A4E>" + finalTitle + "</#7CCF36>"));
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
            if (isRegenerating) isRegenerating = false;
            currentDurability = maxDurability;
        }

        buildTitle();
        updateBossbarDurabilityColor();

        if (isRegenerating) regenerate();

        if (isForming && !player.isSneaking()) remove();
        if (isForming && player.isSneaking()) {
            if (!isRegenerating) isRegenerating = true;
        }
        if (isFormed) {
            if (hasPlantArmorSubAbility(SharpLeafsTask.class) && bPlayer.getBoundAbilityName().equalsIgnoreCase("SharpLeafs")) {
                String message = leafLeftAmountMessage.replace("{amount}", String.valueOf(getPlantArmorSubAbility(SharpLeafsTask.class).orElseThrow().counter));
                ActionBar.sendActionBar(getElement().getColor() + message);
            } else updateActionBarMessages();
            if (jumpBoost > 0) player.addPotionEffect(new PotionEffect(GeneralMethods.getMCVersion() >= 1205 ? PotionEffectType.JUMP_BOOST : PotionEffectType.getByName("JUMP"), 20, jumpBoost - 1, true, false, false));
            if (speedBoost > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, speedBoost - 1, true, false, false));
            if (dolphinGraceBoost > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20, dolphinGraceBoost - 1, true, false, false));
        }
    }

    private List<Block> cachedBlocks = new ArrayList<>();
    private long lastCacheUpdate = 0;

    private void updateCacheIfNeeded() {
        if (System.currentTimeMillis() - lastCacheUpdate > 250) {
            cachedBlocks.clear();
            for (Block b : GeneralMethods.getBlocksAroundPoint(player.getEyeLocation(), collectRange)) {
                if (!PLANT_VOLUMES.containsKey(b.getType())) continue;
                if (TempBlock.isTempBlock(b)) continue;
                cachedBlocks.add(b);
            }
            lastCacheUpdate = System.currentTimeMillis();
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
        Material helmetType = AFFECTED_MATERIALS_LEADERBOARD.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ThreadLocalRandom.current().nextBoolean() ? Material.AZALEA : Material.FLOWERING_AZALEA);
        mostCommonBlock = helmetType.createBlockData();
        ItemStack helmet = new ItemStack(!helmetType.isItem() ? (ThreadLocalRandom.current().nextBoolean() ? Material.AZALEA : Material.FLOWERING_AZALEA) : helmetType);
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
        hMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        hMeta.setItemName(GradientAPI.colorize("<#247A4E>- ʜᴀɴɢɪɴɢ ᴍᴏss -</#7CCF36>"));
        hMeta.setLore(List.of("", ChatColor.of("#247A4E") + " - Just a piece of moss that you put on your head for fun :/"));
        hMeta.setEnchantmentGlintOverride(false);
        hMeta.getPersistentDataContainer().set(ARMOR_UNIQUE_KEY, PersistentDataType.BYTE, (byte)1);
        helmet.setItemMeta(hMeta);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        MultiAbilityManager.bindMultiAbility(player, getName());
        isRegenerating = false;
        isForming = false;
        isFormed = true;
    }

    private void setupArmor(ItemStack item, Color color, String name) {
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta == null) return;

        meta.setColor(color);
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setItemName(GradientAPI.colorize(name));
        meta.setEnchantmentGlintOverride(false);
        meta.getPersistentDataContainer().set(ARMOR_UNIQUE_KEY, PersistentDataType.BYTE, (byte)1);

        item.setItemMeta(meta);
    }

    public void regenerate() {
        if (bPlayer.isOnCooldown("RegeneratingAssembly")) return;
        if (System.currentTimeMillis() > getStartTime() + i) {
            updateCacheIfNeeded();
            if (!cachedBlocks.isEmpty()) {
                Block chosen = cachedBlocks.get(ThreadLocalRandom.current().nextInt(cachedBlocks.size()));
                new MovingBlockTask(chosen);
            }
            i += collectInterval;
        }
    }

    public static boolean isPlantArmor(ItemStack stack) {
        return Optional.ofNullable(stack)
                .map(ItemStack::getItemMeta)
                .map(ItemMeta::getPersistentDataContainer)
                .filter(pdc -> pdc.has(ARMOR_UNIQUE_KEY))
                .isPresent();
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
                new MultiAbilityManager.MultiAbilityInfoSub("SharpLeafs", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("TenaciousVines", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("VineGrapple", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("Leap", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("PlantShield", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("LeafDome", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("RegeneratingAssembly", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("WithdrawalPlants", Element.PLANT)
        ));
    }
    

    public interface LocalTask {
        void update();
        void remove();
         default boolean isRemoved() {
             try {
                 Field listField = PlantArmor.class.getDeclaredField("TASKS");
                 listField.setAccessible(true);
                 @SuppressWarnings("unchecked")
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

    public void setInteraction(int slot, InteractionType type) {
        switch (slot) {
            case 0 -> {
               if (type == InteractionType.LEFT_CLICK) new VineWhipTask();
            }
            case 1 -> {
                if (type == InteractionType.LEFT_CLICK) {
                    if (!hasPlantArmorSubAbility(SharpLeafsTask.class)) new SharpLeafsTask();
                    else {
                        getPlantArmorSubAbility(SharpLeafsTask.class).orElseThrow().launch();
                    }
                }
            }
            case 2 -> {
                if (type == InteractionType.LEFT_CLICK) new TenaciousVinesTask();
            }
            case 3 -> {
                if (type == InteractionType.LEFT_CLICK) new VineGrappleTask();
            }
            case 4 -> {
                if (type == InteractionType.LEFT_CLICK || type == InteractionType.SNEAK_DOWN) new LeapTask(type);
            }
            case 5 -> {
                if (type == InteractionType.SNEAK_DOWN) new PlantShieldTask();
                else if (type == InteractionType.LEFT_CLICK && hasPlantArmorSubAbility(PlantShieldTask.class)) getPlantArmorSubAbility(PlantShieldTask.class).orElseThrow().launch();
            }
            case 6 -> {
                if (type == InteractionType.SNEAK_DOWN) new DomeTask();
                else if (type == InteractionType.LEFT_CLICK && hasPlantArmorSubAbility(DomeTask.class)) getPlantArmorSubAbility(DomeTask.class).orElseThrow().explode();
            }
            case 7 -> {
                if (type == InteractionType.SNEAK_DOWN) isRegenerating = true;
                else if (type == InteractionType.SNEAK_UP && !bPlayer.isOnCooldown("RegeneratingAssembly")) {
                    isRegenerating = false;
                    bPlayer.addCooldown("RegeneratingAssembly", regenCooldown);
                }
            }
            case 8 -> {
                if (type == InteractionType.LEFT_CLICK) remove();
            }
        }
    }

    public enum InteractionType { LEFT_CLICK, RIGHT_CLICK, SNEAK_DOWN, SNEAK_UP }

    public static void redirectEvent(InteractionType type, Player player) {
        if (hasAbility(player, PlantArmor.class)) {
            PlantArmor armor = getAbility(player, PlantArmor.class);
            if (armor.isFormed()) {
                armor.setInteraction(player.getInventory().getHeldItemSlot(), type);
            }
        }
    }

    private class MovingBlockTask implements LocalTask {

        private BlockDisplay display;
        private Block block;
        private BlockData unmodifiableData;

        public int carryingVolume;


        public MovingBlockTask(Block block) {
            this.block = block;

                display = (BlockDisplay) this.block.getWorld().spawnEntity(this.block.getLocation(), EntityType.BLOCK_DISPLAY);
                display.setPersistent(false);
                display.setBlock(this.block.getBlockData());
                display.setTransformation(new Transformation(
                        new Vector3f(0.5f, 0.5f, 0.5f),
                        new Quaternionf(),
                        new Vector3f(1, 1, 1),
                        new Quaternionf()
                ));
                player.getWorld().spawnParticle(GeneralMethods.getMCVersion() > 1205 ? Particle.BLOCK : Particle.valueOf("BLOCK_CRACK"), block.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0, display.getBlock());
                if (!PLANT_VOLUMES.containsKey(block.getType())) return;
                else carryingVolume = PLANT_VOLUMES.get(this.block.getType());
                unmodifiableData = block.getBlockData();
                if (AFFECTED_MATERIALS_LEADERBOARD.containsKey(block.getType())) {
                    final int newInt = AFFECTED_MATERIALS_LEADERBOARD.get(block.getType());
                    AFFECTED_MATERIALS_LEADERBOARD.replace(block.getType(), newInt + 1);
                }
                else AFFECTED_MATERIALS_LEADERBOARD.put(block.getType(), 1);
                AFFECTED_BLOCKS.add(new TempBlock(block, block.getBlockData() instanceof Waterlogged lg && lg.isWaterlogged() ? Material.WATER : Material.AIR));
                TASKS.add(this);

        }

        @Override
        public void update() {
            display.setTeleportDuration(2);
            Location target = display.getLocation();
            target.setPitch(target.getPitch() + 0.5f);
            target.setYaw(target.getYaw() + 2);
            display.teleport(target.add(GeneralMethods.getDirection(display.getLocation(), player.getEyeLocation()).normalize().multiply(collectSpeed)));
            if (player.getEyeLocation().distance(target) < 0.5) {
                COLLECTED_COLORS.add(unmodifiableData.getMapColor());
                currentDurability += carryingVolume;
                if (isFormed) {
                    final PlayerInventory inventory = player.getInventory();
                    final LeatherArmorMeta meta = (LeatherArmorMeta) Objects.requireNonNull(inventory.getChestplate()).getItemMeta();
                    if (meta != null) {
                        meta.setColor(HackathonMethods.averageColor(COLLECTED_COLORS));
                        inventory.getChestplate().setItemMeta(meta);
                        inventory.getLeggings().setItemMeta(meta);
                        inventory.getBoots().setItemMeta(meta);
                    }
                    Material helmetType = AFFECTED_MATERIALS_LEADERBOARD.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(ThreadLocalRandom.current().nextBoolean() ? Material.AZALEA : Material.FLOWERING_AZALEA);
                    mostCommonBlock = helmetType.createBlockData();
                    inventory.getHelmet().setType(mostCommonBlock.getMaterial());
                }
                remove();
            }
        }

        @Override
        public void remove() {
            display.remove();
            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_COMPOSTER_FILL, 1, 0);
            new TextDisplayTask(ChatColor.GREEN + "+ " + carryingVolume);
            TASKS.remove(this);
        }

    }



    private class VineWhipTask implements LocalTask {

        private Vine vine;
        private long start, i;

        public VineWhipTask() {
            if (bPlayer.isOnCooldown("VineWhip")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability - whipDurationTakeCount <= 0) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            if (hasPlantArmorSubAbility(VineWhipTask.class)) return;
            vine = new Vine(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4), GeneralMethods.getTargetedLocation(player, 3), 2, PlantArmor.this);
            start = System.currentTimeMillis();
            i = whipGrowInterval;
            currentDurability -= whipDurationTakeCount;
            TASKS.add(this);
        }

        @Override
        public void update() {


            vine.getRope().setStartPosition(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4));
            vine.getRope().applyVelocity(player.getLocation().getDirection().multiply(whipAnglePower));

            if (vine.getRope().getRenderPoints().size() < whipRange) {
                if (System.currentTimeMillis() > start + i) {
                    vine.getRope().addSegment();
                    player.getWorld().playSound(vine.getRope().getRenderPoints().getLast(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1, 1);
                    player.getWorld().spawnParticle(Particle.COMPOSTER, vine.getRope().getRenderPoints().getLast(), 4, 0.5, 0.5, 0.5, 0);
                    i += whipGrowInterval;
                }
            }


            for (int i1 = 0; i1 < vine.getRope().getRenderPoints().size(); i1++) {
                List<LivingEntity> list = GeneralMethods.getEntitiesAroundPoint(
                                vine.getRope().getRenderPoints().get(i1), whipCollisionRadius)
                        .stream()
                        .filter(e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()))
                        .map(e -> (LivingEntity) e)
                        .toList();

                VerletPoint point = vine.getRope().getPoints().get(i1);
                if (!list.isEmpty() && point.getPreviousLoc().distance(point.getPositionLoc()) >= whipMinSlapPower) {
                    int finalI = i1;
                    list.forEach(e -> {
                        DamageHandler.damageEntity(e, player, whipDamage, PlantArmor.this);
                        e.setVelocity(GeneralMethods.getDirection(
                                vine.getRope().getRenderPoints().get(finalI), e.getEyeLocation()).normalize().multiply(whipKnockback));
                    });
                }
            }

            if (System.currentTimeMillis() > start + whipDuration) remove();
        }

        @Override
        public void remove() {
            bPlayer.addCooldown("VineWhip", whipCooldown);
            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_BREAK, 1, 1);
            TASKS.remove(this);
            vine.cancel();
        }
    }

    public void addToCooldownList(long cooldown, String ability) {
        COOLDOWNS.put(ability, new Pair<>(System.currentTimeMillis(), cooldown));
    }


    private class VineGrappleTask implements LocalTask {

        private int i;
        private boolean hit, flag;
        private long endGrowTiming;
        private Block hitBlock;
        private Vine vine;
        private long start;

        public VineGrappleTask() {
            if (bPlayer.isOnCooldown("VineGrapple")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability - grappleDurabilityTakeCount <= 0) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            TASKS.stream()
                    .filter(t -> t.getClass() == VineGrappleTask.class)
                    .findAny()
                    .ifPresent(t -> {
                        t.remove();
                        player.setVelocity(player.getLocation().getDirection().multiply(2));
                    });
            vine = new Vine(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4), GeneralMethods.getTargetedLocation(player, 3), 2, PlantArmor.this);
            i = grappleGrowInterval;
            currentDurability -= grappleDurabilityTakeCount;
            vine.getRope().getPoints().getLast().setCollisionEnabled(false);
            start = System.currentTimeMillis();
            TASKS.add(this);
        }

        @Override
        public void update() {
            if (!hit) {
                bPlayer.addCooldown("VineGrapple", grappleCooldown);
                if (vine.getRope().getRenderPoints().size() < grappleRange && System.currentTimeMillis() > start + i) {
                    vine.getRope().addSegment();
                    player.getWorld().playSound(vine.getRope().getRenderPoints().getLast(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1, 1);
                    player.getWorld().spawnParticle(Particle.COMPOSTER, vine.getRope().getRenderPoints().getLast(), 4, 0.5, 0.5, 0.5, 0);
                    i += grappleGrowInterval;
                } else if (!flag) {
                    endGrowTiming = System.currentTimeMillis();
                    flag = true;
                }


                if (System.currentTimeMillis() > endGrowTiming + grappleDurationIfMissed && endGrowTiming != 0) {
                    remove();
                    return;
                }

                vine.getRope().setStartPosition(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4));

                RayTraceResult result = vine.getRope().getRenderPoints().getLast().getWorld().rayTraceBlocks(
                        vine.getRope().getRenderPoints().getLast(),
                        player.getLocation().getDirection(), 1, FluidCollisionMode.NEVER, true);

                Optional.ofNullable(result)
                        .map(RayTraceResult::getHitBlock)
                        .filter(GeneralMethods::isSolid)
                        .filter(b -> result.getHitBlockFace() != BlockFace.UP)
                        .ifPresent(b -> {
                            hit = true;
                            hitBlock = b;
                            vine.getRope().lockPoint(vine.getRope().getRenderPoints().size() - 1, true);
                            vine.getRope().lockPoint(0, false);
                            vine.getRope().setEndPosition(b.getLocation());
                            vine.getRope().setGravity(new Vector(0, -9.8, 0));

                        });
                vine.getRope().normalizeRope(false);
                vine.getRope().applyVelocity(player.getLocation().getDirection().multiply(grappleAnglePower));
            } else {
                if (player.isSneaking() && player.getInventory().getHeldItemSlot() == 3) {
                    if (vine.getRope().getRenderPoints().getFirst().distance(player.getLocation()) <= 0.5) {
                        vine.getRope().removeSegmentFirst();
                        vine.getDisplays().getFirst().remove();
                        if (vine.getRope().getRenderPoints().size() <= 2) remove();
                    }
                    if (getRunningTicks() % 20 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_VINE_STEP, 1, 1);
                    }
                    player.setVelocity(GeneralMethods.getDirection(player.getLocation(), vine.getRope().getRenderPoints().getFirst()).normalize().multiply(player.getLocation().distance(vine.getRope().getRenderPoints().getFirst()) > 5 ? grapplePullSpeed + player.getLocation().distance(vine.getRope().getRenderPoints().getFirst()) / 2 : grapplePullSpeed));
                } else if (player.isSneaking()) {
                    remove();
                } else {
                    final Location right = GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4);
                    player.setVelocity(GeneralMethods.getDirection(right, vine.getRope().getRenderPoints().getFirst()).normalize().multiply(right.distance(vine.getRope().getRenderPoints().getFirst()) / 2));
                }
            }
        }

        @Override
        public void remove() {
            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_BREAK, 1, 1);
            TASKS.remove(this);
            vine.cancel();
        }
    }

    public static class Vine extends BukkitRunnable {
        private VerletRope rope;
        private List<BlockDisplay> displays = new ArrayList<>();
        private long start;
        private BlockData currentSkin;
        private Location startLoc, end;
        private Ability ability;
        private Biome originBiome;
        private Integer selectedSkin;

        public Vine(Location start, Location end, int segments, @Nullable Ability ability) {
            startLoc = start;
            this.ability = ability;
            this.end = end;
            this.rope = new VerletRope(start, end, segments, 1);
            this.rope.setCollisionEnabled(true);
            this.rope.setGravity(new Vector().zero());
            this.start = System.currentTimeMillis();

            Biome biome = start.getBlock().getBiome();

            if (ability instanceof PlantArmor armor) {
                List<Material> materials = null;
                for (List<Material> key : BIOME_CONTENTS.keySet()) {
                    if (key.contains(armor.mostCommonBlock.getMaterial())) {
                        materials = key;
                        break;
                    }
                }

                if (materials != null) {
                    List<Biome> biomes = BIOME_CONTENTS.get(materials);
                    if (biomes != null && !biomes.isEmpty()) {
                        biome = (biomes.size() == 1)
                                ? biomes.getFirst()
                                : biomes.get(ThreadLocalRandom.current().nextInt(biomes.size()));
                    }
                }
            }


            currentSkin = getSkinByStandingBiome(biome);
            originBiome = biome;

            runTaskTimer(Hackathon.plugin, 1L, 0);
        }


        private BlockData getSkinByStandingBiome(Biome biome) {
            BlockData[][] skins = VINE_SKINS.get(biome);

            if (skins != null && skins.length > 0) {
                int outerIndex;

                if (selectedSkin == null) {
                    outerIndex = (skins.length == 1) ? 0
                            : ThreadLocalRandom.current().nextInt(skins.length);
                    selectedSkin = outerIndex;
                }
                else outerIndex = selectedSkin;

                BlockData[] inner = skins[outerIndex];
                if (inner.length == 0) {
                    return Material.BIG_DRIPLEAF_STEM.createBlockData();
                }

                int innerIndex = (inner.length == 1) ? 0
                        : ThreadLocalRandom.current().nextInt(inner.length);
                return inner[innerIndex];
            } else {
                return Material.BIG_DRIPLEAF_STEM.createBlockData();
            }
        }



        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            for (int i = 0; i < rope.getRenderPoints().size() - 2; i++) {
                Vector dir = GeneralMethods.getDirection(rope.getRenderPoints().get(i), rope.getRenderPoints().get(i + 1));
                Location pLoc = rope.getRenderPoints().get(i).clone();

                for (float k = 0; k <= dir.length(); k += 0.3f) {
                    pLoc.getWorld().spawnParticle(GeneralMethods.getMCVersion() >= 1205 ? Particle.BLOCK : Particle.valueOf("BLOCK_CRACK"), pLoc, 2, 0.5, 0.5, 0.5, 0.1, displays.get(i).getBlock());
                    pLoc.add(dir.clone().normalize().multiply(0.3));
                }
            }
            displays.forEach(BlockDisplay::remove);
        }

        public VerletRope getRope() {
            return rope;
        }

        public BlockData getCurrentSkin() {
            return currentSkin;
        }

        public List<BlockDisplay> getDisplays() {
            return displays;
        }

        public long getStart() {
            return start;
        }

        public Location getStartLoc() {
            return startLoc;
        }

        public Location getEnd() {
            return end;
        }

        @Override
        public void run() {
            rope.setConstraintIterations(10);
            rope.simulate(0.016);
            if (rope.getRenderPoints().size() > displays.size()) {
                for (int i = displays.size(); i < rope.getRenderPoints().size(); i++) {
                    BlockDisplay display = (BlockDisplay) rope.getRenderPoints().get(i).getWorld().spawnEntity(rope.getRenderPoints().get(i), EntityType.BLOCK_DISPLAY);
                    currentSkin = getSkinByStandingBiome(originBiome);
                    display.setBlock(currentSkin);
                    display.setPersistent(false);
                    Transformation t = display.getTransformation();
                    t.getTranslation().set(new Vector3f(-0.5f, -0.5f, -0.5f));
                    display.setTransformation(t);
                    displays.add(display);
                }
            }
            List<Location> points = rope.getRenderPoints();
            for (int i = 0; i < points.size(); i++) {
                Location loc = points.get(i).clone();

                Vector direction;
                if (i > 0 && i < points.size() - 1) {
                    Vector before = GeneralMethods.getDirection(points.get(i - 1), points.get(i));
                    Vector after = GeneralMethods.getDirection(points.get(i), points.get(i + 1));
                    direction = before.add(after).normalize();
                } else if (i < points.size() - 1) {
                    direction = GeneralMethods.getDirection(points.get(i), points.get(i + 1)).normalize();
                } else {
                    direction = GeneralMethods.getDirection(points.get(i - 1), points.get(i)).normalize();
                }

                if (i < displays.size()) {

                    final Vector3f dir3 = direction.toVector3f();
                    if (dir3.lengthSquared() < 1e-6f) {
                        dir3.set(0, 0, 1);
                    } else {
                        dir3.normalize();
                    }
                    final Vector3f worldUp = new Vector3f(0, 1, 0);
                    if (Math.abs(dir3.dot(worldUp)) > 0.999f) {
                        worldUp.set(1, 0, 0);
                    }
                    final Quaternionf lookRot = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dir3);
                    final Vector3f localX = new Vector3f(1, 0, 0);
                    lookRot.transform(localX).normalize();

                    final Quaternionf pitchRot = new Quaternionf().fromAxisAngleRad(localX, (float) Math.toRadians(-90));
                    final Quaternionf combined = new Quaternionf(pitchRot).mul(lookRot);

                    final Location currentPoint = rope.getRenderPoints().get(i);
                    final Location next = rope.getRenderPoints().get(i == rope.getRenderPoints().size() -1 ? i : i + 1);

                    final double distance = currentPoint.distance(next);
                    final float size = distance == 0 ? 1 : (float) distance;

                    final Matrix4f mat = new Matrix4f()
                            .rotate(combined)
                            .scale(1, size, 1)
                            .translate(-0.5f, -0.5f, -0.5f);

                    displays.get(i).setTransformationMatrix(mat);
                    displays.get(i).setTeleportDuration(2);
                    displays.get(i).teleport(loc);
                }

                BlockDisplay display = displays.get(i);
                BlockData dat = display.getBlock();

                if (dat instanceof CaveVinesPlant cvp) {
                    if (cvp.isBerries() && isAir(display.getLocation().getBlock().getType())) {
                        new TempBlock(display.getLocation().getBlock(),
                                Material.LIGHT.createBlockData(d -> ((Levelled) d).setLevel(13)))
                                .setRevertTime(10);
                    }
                }


            }
        }
    }

    /**
     * @param ability can be null
     * */
    public static Vine invokeUnlinkedVineTask(Location start, Location end, int segments, @Nullable Ability ability) {
        return new Vine(start, end, segments, ability);
    }

    public static Vine invokeUnlinkedVineTask(Location start, Location end, int segments) {
        return new Vine(start, end, segments, null);
    }




    public void invokeLandTask(double fallDmg) {
        new LandTask(fallDmg);
    }

    private class LandTask implements LocalTask {

        private Location location;
        private TempBlock main;
        private int i;
        private long start;
        private boolean mainGrew;
        private Material currentSkin;
        private Class<? extends BlockData> castClass;
        private final Map<TempBlock, Integer> AROUND_PETALS = new HashMap<>();
        private final BlockFace[] AFFECTED_FACES = new BlockFace[]{
                BlockFace.SOUTH,
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.WEST
        };
        public LandTask(double falldmg) {
            this.location = player.getLocation();
            Biome biome = location.getBlock().getBiome();
            if (!PASSIVE_SKINS.containsKey(biome)) {
                castClass = FlowerBed.class;
                currentSkin = Material.PINK_PETALS;
            } else {
                //why not ?_?
                var skin = PASSIVE_SKINS.get(biome);
                castClass = skin.getRight();
                currentSkin = skin.getLeft().getMaterial();
            }
            main = new TempBlock(location.getBlock(), createData(1));
            start = System.currentTimeMillis();
            currentDurability -= (int) (falldmg * 10);
            TASKS.add(this);
        }

        @Override
        public void update() {
            if (!mainGrew) {
                if (System.currentTimeMillis() > start + i) {
                    int amount = getAmount(main.getBlockData()) + 1;
                    main.setType(createData(amount));

                    if (amount >= getMaxAmount(main.getBlockData())) {
                        for (BlockFace face : AFFECTED_FACES) {
                            Block temp = location.getBlock().getRelative(face);
                            if (!GeneralMethods.isSolid(temp.getRelative(BlockFace.DOWN))) continue;
                            if (GeneralMethods.isSolid(temp)) continue;

                            TempBlock t = new TempBlock(temp, currentSkin);
                            int rand = ThreadLocalRandom.current().nextInt(1, getMaxAmount(t.getBlockData()));
                            AROUND_PETALS.put(t, rand);
                        }
                        main.setRevertTime(10000);
                        mainGrew = true;
                    }
                    player.getWorld().playSound(main.getLocation(), Sound.BLOCK_CHERRY_LEAVES_BREAK, 1, 1);
                    i += 250;
                }
            } else {
                if (System.currentTimeMillis() > start + i) {
                    Iterator<TempBlock> iterator = AROUND_PETALS.keySet().iterator();
                    while (iterator.hasNext()) {
                        TempBlock temp = iterator.next();
                        int amount = getAmount(temp.getBlockData()) + 1;
                        temp.setType(createData(amount));
                        if (amount >= AROUND_PETALS.get(temp)) {
                            temp.setRevertTime(10000);
                            iterator.remove();
                        }
                    }
                    i += 500;
                }
                if (AROUND_PETALS.isEmpty()) remove();
            }
        }

        @Override
        public void remove() {
            TASKS.remove(this);
        }

        private BlockData createData(int amount) {
            BlockData data = currentSkin.createBlockData();
            if (castClass.isInstance(data)) {
                if (castClass == FlowerBed.class) {
                    ((FlowerBed) data).setFlowerAmount(Math.min(amount, ((FlowerBed) data).getMaximumFlowerAmount()));
                } else if (castClass == LeafLitter.class) {
                    ((LeafLitter) data).setSegmentAmount(Math.min(amount, ((LeafLitter) data).getMaximumSegmentAmount()));
                }
            }
            return data;
        }

        private int getAmount(BlockData data) {
            if (castClass.isInstance(data)) {
                if (castClass == FlowerBed.class) return ((FlowerBed) data).getFlowerAmount();
                if (castClass == LeafLitter.class) return ((LeafLitter) data).getMaximumSegmentAmount();
            }
            return 1;
        }

        private int getMaxAmount(BlockData data) {
            if (castClass.isInstance(data)) {
                if (castClass == FlowerBed.class) return ((FlowerBed) data).getMaximumFlowerAmount();
                if (castClass == LeafLitter.class) return ((LeafLitter) data).getMaximumSegmentAmount();
            }
            return 4;
        }
    }

    private class LeapTask implements LocalTask {

        private Vine left, right;
        private InteractionType type;
        private long start, startLeapTiming;
        private int currStage, leapI;
        private double currPower;
        private boolean flag;

        public LeapTask(InteractionType type) {
            if (type != InteractionType.LEFT_CLICK && type != InteractionType.SNEAK_DOWN) return;
            if (bPlayer.isOnCooldown("Leap")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability <= leapDurabilityTakeCount) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            if (!player.isOnGround()) return;
            this.type = type;

            if (type == InteractionType.LEFT_CLICK) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_BREAK, 1, 1);
                bPlayer.addCooldown("Leap", leapCooldown);
                Location leftLoc = GeneralMethods.getLeftSide(player.getLocation().add(0, 1, 0), 0.4);
                Location rightLoc = GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4);
                left = new Vine(leftLoc, player.getLocation(), 10, PlantArmor.this);
                right = new Vine(rightLoc, player.getLocation(), 10, PlantArmor.this);
                left.getRope().setCollisionEnabled(false);
                right.getRope().setCollisionEnabled(false);
                left.getRope().setGravity(new Vector(0, -9.8, 0));
                right.getRope().setGravity(new Vector(0, -9.8, 0));
                player.setVelocity(player.getLocation().getDirection().multiply(leapMinPower * 2));
            }
            currPower = leapMinPower;
            currStage = 1;
            leapI = Math.toIntExact(leapLevelupInterval);
            start = System.currentTimeMillis();
            currentDurability -= leapDurabilityTakeCount;
            TASKS.add(this);
        }

        @Override
        public void update() {
            if (type == InteractionType.LEFT_CLICK && System.currentTimeMillis() > start + 100) if (player.isOnGround()) remove();
            if (type == InteractionType.SNEAK_DOWN) {
                if (player.isSneaking() && !flag) {
                    player.sendTitle("", buildTitleText(), 0, 10, 0);
                    if (!player.isOnGround()) remove();
                    if (currStage <= leapMaxStages) {
                        if (System.currentTimeMillis() > start + leapI) {
                            currPower += leapPowerPerPower;
                            currStage++;
                            leapI += (int) leapLevelupInterval;
                        }
                        if (currStage > leapMaxStages) currStage = leapMaxStages;
                    }
                }
                else {
                    if (!flag) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_BREAK, 1, 1);
                        Location leftLoc = GeneralMethods.getLeftSide(player.getLocation().add(0, 1, 0), 0.4);
                        Location rightLoc = GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4);
                        left = new Vine(leftLoc, player.getLocation(), 10, PlantArmor.this);
                        right = new Vine(rightLoc, player.getLocation(), 10, PlantArmor.this);
                        left.getRope().setCollisionEnabled(false);
                        right.getRope().setCollisionEnabled(false);
                        left.getRope().setGravity(new Vector(0, -9.8, 0));
                        right.getRope().setGravity(new Vector(0, -9.8, 0));
                        player.setVelocity(player.getLocation().getDirection().multiply(currPower));
                        startLeapTiming = System.currentTimeMillis();
                        flag = true;
                    }
                }

                if (flag) {
                    if (System.currentTimeMillis() > startLeapTiming + 100 && player.isOnGround()) remove();
                }

            }

            if (left != null && right != null) {
                Location leftLoc = GeneralMethods.getLeftSide(player.getLocation().add(0, 1, 0), 0.4);
                Location rightLoc = GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4);
                left.getRope().setStartPosition(leftLoc);
                right.getRope().setStartPosition(rightLoc);
            }
        }

        private String buildTitleText() {
            StringBuilder builder = new StringBuilder();
            for (int j = 1; j <= leapMaxStages; j++) {
                ChatColor color = j > currStage ? ChatColor.YELLOW : ChatColor.GREEN;
                builder.append(color).append("_");
            }
            return builder.toString();
        }

        @Override
        public void remove() {
            bPlayer.addCooldown("Leap", leapCooldown);
            if (left != null) left.cancel();
            if (right != null) right.cancel();
            TASKS.remove(this);
        }
    }

    private class TenaciousVinesTask implements LocalTask {

        private Vine vine;
        private long start;

        public TenaciousVinesTask() {
            if (bPlayer.isOnCooldown("TenaciousVines")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability - tenaciousVineDurabilityTakeCount <= 0) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            if (hasPlantArmorSubAbility(TenaciousVinesTask.class)) return;

            player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0);

            final Location start = GeneralMethods.getLeftSide(GeneralMethods.getTargetedLocation(player, 2), (double) tenaciousVineRange / 2);
            final Location end = GeneralMethods.getRightSide(GeneralMethods.getTargetedLocation(player, 2), (double) tenaciousVineRange / 2);

            vine = new Vine(start, end, tenaciousVineRange, PlantArmor.this);
            vine.getRope().lockPoint(0, false);
            vine.getRope().setGravity(new Vector(0, -9.8, 0));
            vine.getRope().getPoints().forEach(vp -> {
                vp.applyVelocity(player.getLocation().getDirection().multiply(tenaciousVinePower + vp.getPositionLoc().distance(GeneralMethods.getTargetedLocation(player, 2)) / 10));
            });
            this.start = System.currentTimeMillis();
            TASKS.add(this);
        }

        @Override
        public void update() {

            for (VerletPoint point : vine.getRope().getPoints()) {
                GeneralMethods.getEntitiesAroundPoint(point.getPositionLoc(), 1).stream()
                        .filter(e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()))
                        .map(e -> ((LivingEntity)e))
                        .findFirst()
                        .ifPresent(le -> {
                            point.lock(true);
                            player.getWorld().playSound(le.getEyeLocation(), Sound.BLOCK_HANGING_ROOTS_PLACE, 1, 1);
                            final MovementHandler mv = new MovementHandler(le, PlantArmor.this);
                            mv.stopWithDuration(tenaciousVineStunDuration / 1000 * 20, PlantArmor.this.getElement().getColor() + tenaciousVineStunMessage);
                            Bukkit.getScheduler().runTaskLater(Hackathon.plugin, () -> {
                                vine.getRope().lockPoint(0, true);
                                vine.getRope().lockPoint(vine.getRope().getPoints().size() - 1, true);
                            }, 40);
                            Bukkit.getScheduler().runTaskLater(Hackathon.plugin, this::remove, tenaciousVineStunDuration / 1000 * 20);
                        });
            }
            if (vine.getRope().getRenderPoints().stream().allMatch(l -> GeneralMethods.isSolid(l.clone().subtract(0, 0.1, 0).getBlock()))) remove();
            if (System.currentTimeMillis() > start + 10000) remove();
        }

        @Override
        public void remove() {
            vine.cancel();
            bPlayer.addCooldown("TenaciousVines", tenaciousVineCooldown);
            TASKS.remove(this);
        }
    }


    private class Leaf implements LocalTask {

        private Location origin, location;
        private Vector direction;
        private BlockDisplay display;

        public Leaf() {
            origin = player.getEyeLocation();
            location = origin.clone().add(HackathonMethods.getRandom().multiply(0.25));
            direction = player.getLocation().getDirection().multiply(leafsSpeed);

            display = (BlockDisplay) origin.getWorld().spawnEntity(location.clone().add(0.5, 0.5, 0.5), EntityType.BLOCK_DISPLAY);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new Quaternionf().rotateLocalX(RANDOM.nextFloat()).rotateLocalY(RANDOM.nextFloat()).rotateLocalZ(RANDOM.nextFloat()),
                    new Vector3f(0.3f, 0.01f, 0.4f),
                    new Quaternionf()
            ));
            display.setBlock(mostCommonBlock);
            display.setPersistent(false);
            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_AZALEA_LEAVES_HIT, 1, 1);
            TASKS.add(this);
        }

        @Override
        public void update() {
            location = location.add(direction);
            display.setTeleportDuration(1);
            display.teleport(location);
            if (origin.distance(location) >= leafsRange) {
                if (GeneralMethods.getMCVersion() < 1215) {
                    direction = direction.subtract(new Vector(0, 0.06, 0));
                }
                else {
                    location.getWorld().spawnParticle(Particle.TINTED_LEAVES, location, 1, 0, 0, 0, 0, mostCommonBlock.getMapColor());
                    removeLeaf(true);
                }
            }
            else direction.normalize().add(player.getLocation().getDirection().multiply(leafsAngleDirection));

            RayTraceResult result = player.getWorld().rayTrace(location, direction, leafsSpeed, FluidCollisionMode.NEVER, true, leafsCollisionRadius, e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()));

            Optional.ofNullable(result)
                    .ifPresent(r -> {
                        Optional.ofNullable(r.getHitBlock())
                                .ifPresent(b -> {
                                    player.getWorld().playSound(display.getLocation(), Sound.ENTITY_BEE_STING, 1, 1);
                                    removeLeaf(false);
                                });
                        Optional.ofNullable(r.getHitEntity())
                                .map(e -> ((LivingEntity)e))
                                .ifPresent(le -> {
                                    DamageHandler.damageEntity(le, player, leafsDamage, PlantArmor.this);
                                    le.setNoDamageTicks(0);
                                    le.setVelocity(new Vector().zero());
                                    player.getWorld().playSound(le.getLocation(), Sound.ENTITY_BEE_STING, 1, 1);
                                    removeLeaf(true);
                                });
                    });
            if (!display.isValid()) remove();
        }


        public void removeLeaf(boolean forced) {
            if (!forced) Bukkit.getScheduler().runTaskLater(Hackathon.plugin, display::remove, 120);
            else display.remove();
            remove();
        }

        @Override
        public void remove() {
            TASKS.remove(this);
        }
    }

    private class SharpLeafsTask implements LocalTask {

        private int counter;

        public SharpLeafsTask() {
            if (bPlayer.isOnCooldown("SharpLeafs")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability - leafsDurabilityTakeCount <= 0) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            if (hasPlantArmorSubAbility(SharpLeafsTask.class)) return;
            counter++;
            launch();
            currentDurability -= leafsDurabilityTakeCount;
            TASKS.add(this);
        }

        public void launch() {
            counter++;
            new Leaf();
        }

        @Override
        public void update() {

            if (counter > leafsAmount) remove();
        }

        @Override
        public void remove() {
            bPlayer.addCooldown("SharpLeafs", leafsCooldown);
            TASKS.remove(this);
        }
    }

    public boolean hasPlantArmorSubAbility(Class<? extends LocalTask> clazz) {
        return TASKS.stream().anyMatch(lt -> lt.getClass().equals(clazz));
    }


    public <T> Optional<T> getPlantArmorSubAbility(Class<T> clazz) {
        if (!LocalTask.class.isAssignableFrom(clazz)) {
            return Optional.empty();
        }

        for (LocalTask task : TASKS) {
            if (clazz.isInstance(task)) {
                return Optional.of(clazz.cast(task));
            }
        }
        return Optional.empty();
    }

    private class PlantShieldTask implements LocalTask {

        private List<TempBlock> blocks;
        private Vector dir;
        private Location origin, location;
        private boolean launched;
        private List<TempBlock> shpere;

        public PlantShieldTask() {
            if (bPlayer.isOnCooldown("PlantShield")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability - shieldDurabilityTakeCount <= 0) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            if (hasPlantArmorSubAbility(PlantShieldTask.class)) return;
            blocks = new ArrayList<>();
            shpere = new ArrayList<>();

            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1, 1);

            TASKS.add(this);
        }

        @Override
        public void update() {




            if (!launched) {
                final Location targetLoc = GeneralMethods.getTargetedLocation(player, shieldOffset, true, false, mostCommonBlock.getPlacementMaterial());
                final Vector eyeDir = player.getEyeLocation().getDirection();
                updateShield(targetLoc, eyeDir);

                if (!player.isSneaking() || player.getInventory().getHeldItemSlot() != 5) {
                    blocks.forEach(tb -> {
                        FallingBlock fb = tb.getBlock().getWorld().spawnFallingBlock(tb.getLocation(), tb.getBlockData());
                        fb.setCancelDrop(true);
                        fb.setDropItem(false);
                        tb.revertBlock();
                    });
                    remove();
                }
            }
            else if (shpere.isEmpty()) {

                if (location.getBlock().getType() == Material.VOID_AIR) remove();
                location = location.add(dir);
                if (origin.distance(location) >= shieldThrowRange) {
                    blocks.forEach(tb -> {
                        FallingBlock fb = tb.getBlock().getWorld().spawnFallingBlock(tb.getLocation(), tb.getBlockData());
                        fb.setCancelDrop(true);
                        fb.setDropItem(false);
                        fb.setVelocity(dir);
                        tb.revertBlock();
                    });
                    remove();
                    return;
                }
                blocks.forEach(tb -> {
                    RayTraceResult result = location.getWorld().rayTraceEntities(location, dir, 1, 1, e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()));
                    Optional.ofNullable(result)
                            .map(RayTraceResult::getHitEntity)
                            .map(e -> ((LivingEntity)e))
                            .ifPresent(e -> {
                                player.getWorld().playSound(e.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1, 1);
                                DamageHandler.damageEntity(e, player, shieldThrowDamage, PlantArmor.this);
                                GeneralMethods.getCircle(e.getLocation(), (int) shieldSphereRadius, 0, true, true, 0)
                                        .forEach(b -> new TempBlock(b.getBlock(), !mostCommonBlock.getMaterial().isSolid() ? Material.AZALEA_LEAVES.createBlockData() : mostCommonBlock, shieldSphereDuration));
                            });
                });
                updateShield(location, dir);
            }
            else if (shpere.stream().allMatch(TempBlock::isReverted)) remove();

        }



        private void updateShield(Location origin, Vector dir) {
            blocks.forEach(TempBlock::revertBlock);
            blocks.clear();


            Vector vec;
            Location loc;
            for (double i = 0; i <= shieldRadius; i += 0.5) {
                for (double angle = 0; angle < 360; angle += 10) {
                    vec = GeneralMethods.getOrthogonalVector(dir.clone(), angle, i);
                    loc = origin.clone().add(vec);
                    if (GeneralMethods.isSolid(loc.getBlock())) continue;
                    blocks.add(new TempBlock(loc.getBlock(), !mostCommonBlock.getMaterial().isSolid() ? Material.AZALEA_LEAVES.createBlockData() : mostCommonBlock));
                }
            }
            blocks.add(new TempBlock(origin.getBlock(), !mostCommonBlock.getMaterial().isSolid() ? Material.AZALEA_LEAVES.createBlockData() : mostCommonBlock));
            if (blocks.isEmpty()) remove();
        }

        public void launch() {
            if (bPlayer.isOnCooldown("PlantShieldThrow") || launched) return;
            Location origin = GeneralMethods.getTargetedLocation(player, shieldOffset);
            location = origin.clone();
            this.origin = origin;
            dir = player.getLocation().getDirection().multiply(shieldThrowSpeed);
            bPlayer.addCooldown("PlantShieldThrow", shieldThrowCooldown);
            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1, 1);
            launched = true;
        }



        @Override
        public void remove() {
            TASKS.remove(this);

            bPlayer.addCooldown("PlantShield", shieldCooldown);
            blocks.forEach(TempBlock::revertBlock);
        }
    }

    private class DomeTask implements LocalTask {

        private List<TempBlock> blocks;
        private long start, in;
        private int counter, counterHeight;
        private boolean grew;

        public DomeTask() {
            if (bPlayer.isOnCooldown("LeafDome")) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + cooldownMessage, player);
                return;
            }
            if (currentDurability - domeDurabilityTakeCount <= 0) {
                bPlayer.addCooldown("PlantArmorActionMsg", 2000);
                ActionBar.sendActionBar(ChatColor.RED + notEnoughDurabilityMessage, player);
                return;
            }
            if (hasPlantArmorSubAbility(DomeTask.class)) return;
            blocks = new ArrayList<>();
            counterHeight = 1;
            currentDurability -= domeDurabilityTakeCount;
            start = System.currentTimeMillis();
            TASKS.add(this);
        }

        public void explode() {
            if (!grew) return;
            player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 2, 0);
            blocks.forEach(tb -> {
                FallingBlock fb = tb.getBlock().getWorld().spawnFallingBlock(tb.getLocation(), tb.getBlockData());
                fb.setDropItem(false);
                fb.setCancelDrop(true);
                fb.setDamagePerBlock((float) domeSegmentDamage);
                fb.setVelocity(GeneralMethods.getDirection(player.getEyeLocation(), fb.getLocation()).normalize());
                tb.revertBlock();
            });
            remove();
        }

        @Override
        public void update() {
            if (!grew) {
                if (System.currentTimeMillis() > start + in) {
                    player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1, 1);
                    blocks.forEach(TempBlock::revertBlock);
                    blocks.clear();
                    GeneralMethods.getCircle(player.getLocation(), counter, 0, true, true, 0).stream()
                            .filter(l -> l.getBlockY() <= player.getLocation().getBlockY() + counterHeight || l.getBlockY() >= player.getLocation().getBlockY() - counterHeight && !GeneralMethods.isSolid(l.getBlock()) && !TempBlock.isTempBlock(l.getBlock()))
                            .forEach(l -> blocks.add(new TempBlock(l.getBlock(), !mostCommonBlock.getMaterial().isSolid() ? Material.AZALEA_LEAVES.createBlockData() : mostCommonBlock)));
                    if (counter >= domeRadius) {
                        counterHeight++;
                    }
                    else counter++;
                    if (counterHeight >= domeRadius) {
                        blocks.forEach(TempBlock::revertBlock);
                        blocks.clear();
                        GeneralMethods.getCircle(player.getLocation(), (int) domeRadius, 0, true, true, 0).stream()
                                .filter(l -> !GeneralMethods.isSolid(l.getBlock()) && !TempBlock.isTempBlock(l.getBlock()))
                                .forEach(l -> blocks.add(new TempBlock(l.getBlock(), !mostCommonBlock.getMaterial().isSolid() ? Material.AZALEA_LEAVES.createBlockData() : mostCommonBlock, domeDuration)));
                        grew = true;
                    }
                    if (!player.isSneaking() || player.getInventory().getHeldItemSlot() != 6) {
                        remove();
                    }
                    in += domeGrowInterval;
                }
            }
            else if (blocks.stream().allMatch(TempBlock::isReverted)) remove();
        }

        @Override
        public void remove() {
            TASKS.remove(this);
            bPlayer.addCooldown("LeafDome", domeCooldown);
            blocks.forEach(TempBlock::revertBlock);
        }
    }


    public int getCurrentDurability() {
        return currentDurability;
    }

    public void decreaseDurability(int count) {
        if (currentDurability - count <= 0) return;
        currentDurability -= count;
    }

    public BlockData getMostCommonBlock() {
        return mostCommonBlock;
    }

    private class TextDisplayTask implements LocalTask {

        private TextDisplay display;
        private long start;

        public TextDisplayTask(String message) {
            TASKS.add(this);
            display = (TextDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(HackathonMethods.getRandom()), EntityType.TEXT_DISPLAY);
            display.setText(message);
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(1, 1, 1),
                    new Quaternionf()
            ));
            display.setBackgroundColor(Color.fromARGB(0));
            display.setVisibleByDefault(false);
            display.setBrightness(new Display.Brightness(15, 15));
            player.showEntity(Hackathon.plugin, display);
            start = System.currentTimeMillis();

        }

        @Override
        public void update() {
            if (System.currentTimeMillis() < start + 2000) {
                display.setTeleportDuration(2);
                display.teleport(display.getLocation().add(0, 0.01, 0));
            }
            else {
                display.setTeleportDuration(2);
                display.teleport(display.getLocation().add(0, 0.1, 0));
                Transformation t = display.getTransformation();
                t.getScale().set(t.getScale().sub(0.05f, 0.05f, 0.05f));
                display.setTransformation(t);
                if (display.getTransformation().getScale().x < 0.01) remove();
            }
        }

        @Override
        public void remove() {
            if (GeneralMethods.getMCVersion() > 1214) {
                for (int j = 0; j < 10; j++) {
                    Location loc = display.getLocation().add(HackathonMethods.getRandom().multiply(ThreadLocalRandom.current().nextDouble(0, 0.5)));
                    player.getWorld().spawnParticle(Particle.TRAIL, loc, 10, 0, 0, 0, 0, new Particle.Trail(loc.clone().add(0, 1, 0), Color.fromRGB(35, 158, 98), 35));
                }
            }
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.1f, 1);
            display.remove();
            TASKS.remove(this);

        }
    }
}
