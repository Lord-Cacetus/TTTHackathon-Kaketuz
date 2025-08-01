package me.kaketuz.hackathon.abilities.plant;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import me.kaketuz.hackathon.Hackathon;
import me.kaketuz.hackathon.util.GradientAPI;
import me.kaketuz.hackathon.util.HackathonMethods;
import me.kaketuz.hackathon.util.verlet.VerletRope;
import me.kaketuz.nightmarelib.lib.logger.Logger;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Bamboo;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PlantArmor extends PlantAbility implements AddonAbility, MultiAbility {
    
    //General
    private final Set<LocalTask> TASKS = ConcurrentHashMap.newKeySet();
    private boolean isForming, isFormed;
    private BossBar durabilityBar;
    private int maxDurability, currentDurability;
    private ItemStack[] OLD_ARMOR;
    private String durabilityTitle;
    private int jumpBoost, speedBoost, dolphinGraceBoost;

    //Ill add more soon
    private Map<Biome, BlockData[]> VINE_SKINS = new HashMap<>() {{
        //All potential forms
        BlockData[] bamboo_specials = new BlockData[] {
                Material.BAMBOO.createBlockData(dat -> ((Bamboo)dat).setLeaves(Bamboo.Leaves.NONE)),
                Material.BAMBOO.createBlockData(dat -> ((Bamboo)dat).setLeaves(Bamboo.Leaves.SMALL)),
                Material.BAMBOO.createBlockData(dat -> ((Bamboo)dat).setLeaves(Bamboo.Leaves.LARGE))
        };
        BlockData[] cave_vines_specials = new BlockData[] {
                Material.CAVE_VINES_PLANT.createBlockData(dat -> ((CaveVinesPlant)dat).setBerries(true)),
                Material.CAVE_VINES_PLANT.createBlockData(dat -> ((CaveVinesPlant)dat).setBerries(false))
        };
        if (GeneralMethods.getMCVersion() >= 1214) put(Biome.PALE_GARDEN, new BlockData[]{Material.PALE_HANGING_MOSS.createBlockData()});
        put(Biome.CRIMSON_FOREST, new BlockData[]{Material.WEEPING_VINES_PLANT.createBlockData()});
        put(Biome.WARPED_FOREST, new BlockData[]{Material.TWISTING_VINES_PLANT.createBlockData()});
        put(Biome.JUNGLE, bamboo_specials);
        put(Biome.BAMBOO_JUNGLE, bamboo_specials);
        put(Biome.SPARSE_JUNGLE, bamboo_specials);
        put(Biome.LUSH_CAVES, cave_vines_specials);
    }};
    
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
    private double whipDamage, whipKnockback, whipAnglePower, whipCollisionRadius;

    
    
    public PlantArmor(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, PlantArmor.class)) return;

        collectRange = Hackathon.config.getDouble("Plant.PlantArmor.Collection.CollectRange");
        collectSpeed = Hackathon.config.getDouble("Plant.PlantArmor.Collection.CollectSpeed");
        collectInterval = Hackathon.config.getInt("Plant.PlantArmor.Collection.CollectInterval");

        durabilityTitle = Hackathon.config.getString("Plant.PlantArmor.General.DurabilityTitle");
        jumpBoost = Hackathon.config.getInt("Plant.PlantArmor.General.JumpBoost");
        speedBoost = Hackathon.config.getInt("Plant.PlantArmor.General.SpeedBoost");
        dolphinGraceBoost = Hackathon.config.getInt("Plant.PlantArmor.General.DolphinGraceBoost");
        maxDurability = Hackathon.config.getInt("Plant.PlantArmor.General.MaxDurability");

        whipDamage = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.Damage");
        whipKnockback = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.Knockback");
        whipAnglePower = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.AnglePower");
        whipCollisionRadius = Hackathon.config.getDouble("Plant.PlantArmor.PlantWhip.CollisionRadius");
        whipRange = Hackathon.config.getInt("Plant.PlantArmor.PlantWhip.RangeInt");
        whipGrowInterval = Hackathon.config.getInt("Plant.PlantArmor.PlantWhip.GrowInterval");
        whipDurationTakeCount = Hackathon.config.getInt("Plant.PlantArmor.PlantWhip.DurabilityTake");
        whipDuration = Hackathon.config.getLong("Plant.PlantArmor.PlantWhip.WhipDuration");
        whipCooldown = Hackathon.config.getLong("Plant.PlantArmor.PlantWhip.WhipCooldown");

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
            if (System.currentTimeMillis() > getStartTime() + i) {
                Block foundBlock = null;
                for (Block block : GeneralMethods.getBlocksAroundPoint(player.getLocation(), collectRange)) {
                    if (foundBlock != null) break;
                    if (!PLANT_VOLUMES.containsKey(block.getType())) continue;
                    if (HackathonMethods.hasSolidBlocksBetween(block.getLocation(), player.getEyeLocation(), b -> !GeneralMethods.isSolid(b) || isPlant(b))) continue;
                    new MovingBlockTask(block);
                    foundBlock = block;
                }
                i += collectInterval;
            }

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
        Material helmetType = AFFECTED_MATERIALS_LEADERBOARD.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ThreadLocalRandom.current().nextBoolean() ? Material.AZALEA : Material.FLOWERING_AZALEA);
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
        helmet.setItemMeta(hMeta);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

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
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setItemName(GradientAPI.colorize(name));
        meta.setEnchantmentGlintOverride(false);

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

    public void setInteraction(int slot) {
        switch (slot) {
            case 0 -> new VineWhipTask();
            case 8 -> remove();
        }
    }

    private class MovingBlockTask implements LocalTask {

        private BlockDisplay display;
        private Block block;
        private final BlockData unmodifiableData;

        public int carryingVolume;


        public MovingBlockTask(Block block) {
            this.block = block;

                display = (BlockDisplay) this.block.getWorld().spawnEntity(this.block.getLocation().add(0.5, 0.5, 0.5), EntityType.BLOCK_DISPLAY);
                display.setPersistent(false);
                display.setBlock(this.block.getBlockData());
                display.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new Quaternionf(),
                        new Vector3f(1, 1, 1),
                        new Quaternionf()
                ));
                carryingVolume = PLANT_VOLUMES.get(this.block.getType());
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
                remove();
            }
        }

        @Override
        public void remove() {
            display.remove();
            TASKS.remove(this);
        }

    }

    private class VineWhipTask implements LocalTask {

        private VerletRope rope;
        private List<BlockDisplay> displays = new ArrayList<>();
        private long start;
        private int i;
        private BlockData currentSkin;

        public VineWhipTask() {
            if (bPlayer.isOnCooldown("VineWhip")) return;

            rope = new VerletRope(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4), GeneralMethods.getTargetedLocation(player, 2), 2, 1);
            rope.setCollisionEnabled(true);
            rope.setGravity(new Vector().zero());
            final Biome biome = player.getLocation().getBlock().getBiome();
            currentSkin = VINE_SKINS.containsKey(biome) ? VINE_SKINS.get(biome)[ThreadLocalRandom.current().nextInt(0, VINE_SKINS.get(biome).length)] : Material.BIG_DRIPLEAF_STEM.createBlockData();
            start = System.currentTimeMillis();
            currentDurability -= whipDurationTakeCount;
            TASKS.add(this);
        }

        @Override
        public void update() {
            bPlayer.addCooldown("VineWhip", whipCooldown);

            if (rope.getRenderPoints().size() < whipRange) {
                if (System.currentTimeMillis() > start + i) {
                    rope.addSegment();
                    for (int j = displays.size(); j < rope.getRenderPoints().size(); j++) {
                        Location loc = rope.getRenderPoints().get(j);
                        float pitch = loc.getPitch();
                        pitch = (pitch >= 0) ? pitch - 90 : pitch + 90;
                        loc.setPitch(pitch);
                        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
                        display.setPersistent(false);
                        Transformation t = display.getTransformation();
                        t.getTranslation().set(new Vector3f(-0.5f, -0.5f, -0.5f));
                        display.setTransformation(t);
                        display.setBlock(currentSkin);
                        displays.add(display);
                    }
                    i += whipGrowInterval;
                }
            }

            for (int i1 = 0; i1 < rope.getRenderPoints().size(); i1++) {
                Location target = rope.getRenderPoints().get(i1).clone();
                List<Location> points = rope.getRenderPoints();
                Vector direction;
                if (i1 > 0 && i1 < points.size() - 1) {

                    Vector before = GeneralMethods.getDirection(points.get(i1 - 1), points.get(i1));
                    Vector after = GeneralMethods.getDirection(points.get(i1), points.get(i1 + 1));
                    direction = before.add(after).normalize();
                } else if (i1 < points.size() - 1) {
                    direction = GeneralMethods.getDirection(points.get(i1), points.get(i1 + 1));
                } else {
                    direction = GeneralMethods.getDirection(points.get(i1 - 1), points.get(i1));
                }

                target.setDirection(direction);
                float pitch = target.getPitch();
                pitch = (pitch >= 0) ? pitch - 90 : pitch + 90;
                target.setPitch(pitch);
                displays.get(i1).teleport(target);

                List<LivingEntity> list = GeneralMethods.getEntitiesAroundPoint(
                                rope.getRenderPoints().get(i1),
                                whipCollisionRadius
                        ).stream()
                        .filter(e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()))
                        .map(e -> (LivingEntity) e)
                        .toList();

                int finalI = i1;
                list.forEach(e -> {
                    DamageHandler.damageEntity(e, player, whipDamage, PlantArmor.this);
                    e.setVelocity(GeneralMethods.getDirection(
                            rope.getRenderPoints().get(finalI),
                            e.getEyeLocation()
                    ).normalize().multiply(whipKnockback));
                });
                final BlockData checkDat = displays.get(i1).getBlock();
                if (checkDat instanceof CaveVinesPlant cvp) {
                    if (cvp.isBerries() && isAir(displays.get(i1).getLocation().getBlock().getType())) {
                        new TempBlock(displays.get(i1).getLocation().getBlock(), Material.LIGHT.createBlockData(dat -> ((Levelled)dat).setLevel(13))).setRevertTime(10);
                    }
                }

                //Removing old ends
                if (!displays.getLast().equals(displays.get(i1))) {
                    BlockDisplay display = displays.get(i1);
                    BlockData dat = display.getBlock();

                    if (dat instanceof CaveVines) display.setBlock(Material.CAVE_VINES_PLANT.createBlockData(dat2 -> ((CaveVines)dat2).setBerries(((CaveVinesPlant)dat).isBerries())));
                    else if (dat.getMaterial() == Material.WEEPING_VINES) display.setBlock(Material.WEEPING_VINES_PLANT.createBlockData());
                    else if (dat.getMaterial() == Material.TWISTING_VINES) display.setBlock(Material.TWISTING_VINES_PLANT.createBlockData());
                }

                if (!list.isEmpty()) {
                    remove();
                    return;
                }
            }

            //Checking last display for smoothly ending of rope
            final BlockDisplay last = displays.getLast();
            final BlockData lastDat = last.getBlock();
            if (lastDat instanceof CaveVinesPlant) last.setBlock(Material.CAVE_VINES.createBlockData(dat -> ((Ageable)dat).setAge(25)));
            else if (lastDat.getMaterial() == Material.WEEPING_VINES_PLANT) last.setBlock(Material.WEEPING_VINES.createBlockData(dat -> ((Ageable)dat).setAge(25)));
            else if (lastDat.getMaterial() == Material.TWISTING_VINES_PLANT) last.setBlock(Material.TWISTING_VINES.createBlockData(dat -> ((Ageable)dat).setAge(25)));

            rope.applyVelocity(player.getLocation().getDirection().multiply(whipAnglePower));
            rope.simulate(0.013);
            rope.setConstraintIterations(20);

            rope.setStartPosition(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4));

            if (System.currentTimeMillis() > start + whipDuration) {
                remove();
            }
        }


        @Override
        public void remove() {
            displays.forEach(BlockDisplay::remove);
            TASKS.remove(this);
        }
    }

}
