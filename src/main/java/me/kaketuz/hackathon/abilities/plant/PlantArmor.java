package me.kaketuz.hackathon.abilities.plant;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
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
import me.kaketuz.nightmarelib.lib.util.Pair;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class PlantArmor extends PlantAbility implements AddonAbility, MultiAbility {
    
    //General
    private final Set<LocalTask> TASKS = ConcurrentHashMap.newKeySet();
    private boolean isForming, isFormed;
    private BossBar durabilityBar;
    private int maxDurability, currentDurability;
    private ItemStack[] OLD_ARMOR;
    private String durabilityTitle;
    private int jumpBoost, speedBoost, dolphinGraceBoost;
    private static final NamespacedKey ARMOR_UNIQUE_KEY = new NamespacedKey(Hackathon.plugin, "ability.plantarmor.armorkey");

    //Ill add more soon
    private final Map<Biome, BlockData[]> VINE_SKINS = new HashMap<>() {{
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

    //VineGrabble
    private int grabbleRange, grabbleGrowInterval, grabbleDurabilityTakeCount;
    private long grabbleCooldown, grabbleDurationIfMissed;
    private double grabbleAnglePower, grabblePullSpeed;

    //RegeneratingAssembly
    private long regenCooldown;

    
    
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

        grabblePullSpeed = Hackathon.config.getDouble("Plant.PlantArmor.VineGrabble.PullSpeed");
        grabbleAnglePower = Hackathon.config.getDouble("Plant.PlantArmor.VineGrabble.AnglePower");
        grabbleRange = Hackathon.config.getInt("Plant.PlantArmor.VineGrabble.RangeInt");
        grabbleGrowInterval = Hackathon.config.getInt("Plant.PlantArmor.VineGrabble.GrowInterval");
        grabbleDurabilityTakeCount = Hackathon.config.getInt("Plant.PlantArmor.VineGrabble.DurabilityTakeCount");
        grabbleCooldown = Hackathon.config.getLong("Plant.PlantArmor.VineGrabble.Cooldown");
        grabbleDurationIfMissed = Hackathon.config.getLong("Plant.PlantArmor.VineGrabble.DurationIfMissed");

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

        durabilityBar.setProgress((double) currentDurability / maxDurability);
        String finalTitle = durabilityTitle
                .replace("{current}", String.valueOf(currentDurability))
                .replace("{max}", String.valueOf(maxDurability));
        durabilityBar.setTitle(GradientAPI.colorize(finalTitle));

        if (isRegenerating) regenerate();

        if (isForming && !player.isSneaking()) remove();
        if (isForming && player.isSneaking()) {
            if (!isRegenerating) isRegenerating = true;
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
            RayTraceResult result = player.rayTraceBlocks(collectRange, FluidCollisionMode.NEVER);
            Optional.ofNullable(result)
                    .map(RayTraceResult::getHitBlock)
                    .filter(b -> PLANT_VOLUMES.containsKey(b.getType()))
                    .ifPresent(MovingBlockTask::new);
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
                new MultiAbilityManager.MultiAbilityInfoSub("TralaleloTralala", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("BombardiroCorocodilo", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("VineGrabble", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("TunTunTunTunSagur", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("BallerinaCappuchina", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("LiriliLarila", Element.PLANT),
                new MultiAbilityManager.MultiAbilityInfoSub("RegeneratingAssembly", Element.PLANT),
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
            case 3 -> {
                if (type == InteractionType.LEFT_CLICK) new VineGrabbleTask();
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

    private class VineWhipTask extends AbstractVineTask {

        private int i;

        public VineWhipTask() {
            super("VineWhip", whipDurationTakeCount);
        }

        @Override
        public void update() {
            bPlayer.addCooldown("VineWhip", whipCooldown);

            if (rope.getRenderPoints().size() < whipRange && System.currentTimeMillis() > start + i) {
                rope.addSegment();
                renderNewSegments(displays.size());
                i += whipGrowInterval;
            }

            updateDisplays();
            updateLastDisplay();

            for (int i1 = 0; i1 < rope.getRenderPoints().size(); i1++) {
                List<LivingEntity> list = GeneralMethods.getEntitiesAroundPoint(
                                rope.getRenderPoints().get(i1), whipCollisionRadius)
                        .stream()
                        .filter(e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()))
                        .map(e -> (LivingEntity) e)
                        .toList();

                if (!list.isEmpty()) {
                    int finalI = i1;
                    list.forEach(e -> {
                        DamageHandler.damageEntity(e, player, whipDamage, PlantArmor.this);
                        e.setVelocity(GeneralMethods.getDirection(
                                rope.getRenderPoints().get(finalI), e.getEyeLocation()).normalize().multiply(whipKnockback));
                    });
                    remove();
                    return;
                }
            }

            rope.applyVelocity(player.getLocation().getDirection().multiply(whipAnglePower));
            rope.simulate(0.013);
            rope.setConstraintIterations(20);
            rope.setStartPosition(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4));

            if (System.currentTimeMillis() > start + whipDuration) remove();
        }
    }


    private class VineGrabbleTask extends AbstractVineTask {

        private int i;
        private boolean hit, flag;
        private long endGrowTiming;
        private Block hitBlock;

        public VineGrabbleTask() {
            super("VineGrabble", grabbleDurabilityTakeCount);
        }

        @Override
        public void update() {
            bPlayer.addCooldown("VineGrabble", grabbleCooldown);

            if (!hit) {
                if (rope.getRenderPoints().size() < grabbleRange && System.currentTimeMillis() > start + i) {
                    rope.addSegment();
                    renderNewSegments(displays.size());
                    i += grabbleGrowInterval;
                } else if (!flag) {
                    endGrowTiming = System.currentTimeMillis();
                    flag = true;
                } else if (System.currentTimeMillis() > endGrowTiming + grabbleDurationIfMissed && endGrowTiming != 0) {
                    remove();
                    return;
                }

                updateLastDisplay();


                rope.setStartPosition(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4));

                RayTraceResult result = displays.getLast().getWorld().rayTraceBlocks(
                        displays.getLast().getLocation(),
                        displays.getLast().getLocation().getDirection(), 0.2, FluidCollisionMode.NEVER, true);

                Optional.ofNullable(result)
                        .map(RayTraceResult::getHitBlock)
                        .filter(GeneralMethods::isSolid)
                        .filter(b -> result.getHitBlockFace() != BlockFace.UP)
                        .ifPresent(b -> {
                            hit = true;
                            hitBlock = b;
                            rope.setGravity(new Vector(0, -9.8, 0));
                        });

                rope.applyVelocity(player.getLocation().getDirection().multiply(grabbleAnglePower));
            } else {
                rope.lockPoint(rope.getRenderPoints().size() - 1, true);
                if (player.isSneaking() && player.getInventory().getHeldItemSlot() == 3) {
                    if (rope.getRenderPoints().getFirst().distance(player.getLocation()) <= 0.5) {
                        rope.removeSegmentFirst();
                        displays.getFirst().remove();
                        if (rope.getRenderPoints().size() <= 2) remove();
                    }
                    player.setVelocity(GeneralMethods.getDirection(player.getLocation(), rope.getRenderPoints().getFirst()).normalize().multiply(grabblePullSpeed));
                } else if (player.isSneaking()) {
                    remove();
                } else {
                    player.setVelocity(GeneralMethods.getDirection(player.getLocation(), rope.getRenderPoints().getFirst()).normalize().multiply(player.getLocation().distance(rope.getRenderPoints().getFirst()) / 2));
                }
            }

            updateDisplays();
            rope.simulate(0.013);
            rope.setConstraintIterations(20);
        }
    }


    private abstract class AbstractVineTask implements LocalTask {

        protected VerletRope rope;
        protected List<BlockDisplay> displays = new ArrayList<>();
        protected long start;
        protected BlockData currentSkin;

        public AbstractVineTask(String cooldownKey, int durabilityCost) {
            if (bPlayer.isOnCooldown(cooldownKey)) return;

            this.rope = new VerletRope(GeneralMethods.getRightSide(player.getLocation().add(0, 1, 0), 0.4),
                    GeneralMethods.getTargetedLocation(player, 2), 2, 1);
            this.rope.setCollisionEnabled(true);
            this.rope.setGravity(new Vector().zero());

            final Biome biome = player.getLocation().getBlock().getBiome();
            this.currentSkin = VINE_SKINS.containsKey(biome)
                    ? VINE_SKINS.get(biome)[ThreadLocalRandom.current().nextInt(0, VINE_SKINS.get(biome).length)]
                    : Material.BIG_DRIPLEAF_STEM.createBlockData();

            this.start = System.currentTimeMillis();
            currentDurability -= durabilityCost;

            TASKS.add(this);
        }

        protected void renderNewSegments(int fromIndex) {
            for (int j = fromIndex; j < rope.getRenderPoints().size(); j++) {
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
        }

        protected void updateDisplays() {
            for (int i = 0; i < rope.getRenderPoints().size(); i++) {
                Location target = rope.getRenderPoints().get(i).clone();
                List<Location> points = rope.getRenderPoints();
                Vector direction;

                if (i > 0 && i < points.size() - 1) {
                    Vector before = GeneralMethods.getDirection(points.get(i - 1), points.get(i));
                    Vector after = GeneralMethods.getDirection(points.get(i), points.get(i + 1));
                    direction = before.add(after).normalize();
                } else if (i < points.size() - 1) {
                    direction = GeneralMethods.getDirection(points.get(i), points.get(i + 1));
                } else {
                    direction = GeneralMethods.getDirection(points.get(i - 1), points.get(i));
                }

                target.setDirection(direction);
                float pitch = target.getPitch();
                pitch = (pitch >= 0) ? pitch - 90 : pitch + 90;
                target.setPitch(pitch);
                displays.get(i).teleport(target);

                final BlockData checkDat = displays.get(i).getBlock();
                if (checkDat instanceof CaveVinesPlant cvp) {
                    if (cvp.isBerries() && isAir(displays.get(i).getLocation().getBlock().getType())) {
                        new TempBlock(displays.get(i).getLocation().getBlock(),
                                Material.LIGHT.createBlockData(dat -> ((Levelled) dat).setLevel(13)))
                                .setRevertTime(10);
                    }
                }

                if (!displays.getLast().equals(displays.get(i))) {
                    BlockDisplay display = displays.get(i);
                    BlockData dat = display.getBlock();

                    if (dat instanceof CaveVines)
                        display.setBlock(Material.CAVE_VINES_PLANT.createBlockData(dat2 -> ((CaveVines) dat2).setBerries(((CaveVinesPlant) dat).isBerries())));
                    else if (dat.getMaterial() == Material.WEEPING_VINES)
                        display.setBlock(Material.WEEPING_VINES_PLANT.createBlockData());
                    else if (dat.getMaterial() == Material.TWISTING_VINES)
                        display.setBlock(Material.TWISTING_VINES_PLANT.createBlockData());
                }
            }
        }

        protected void updateLastDisplay() {
            BlockDisplay last = displays.getLast();
            BlockData lastDat = last.getBlock();
            if (lastDat instanceof CaveVinesPlant)
                last.setBlock(Material.CAVE_VINES.createBlockData(dat -> ((Ageable) dat).setAge(25)));
            else if (lastDat.getMaterial() == Material.WEEPING_VINES_PLANT)
                last.setBlock(Material.WEEPING_VINES.createBlockData(dat -> ((Ageable) dat).setAge(25)));
            else if (lastDat.getMaterial() == Material.TWISTING_VINES_PLANT)
                last.setBlock(Material.TWISTING_VINES.createBlockData(dat -> ((Ageable) dat).setAge(25)));
        }

        @Override
        public void remove() {
            displays.forEach(BlockDisplay::remove);
            TASKS.remove(this);
        }
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



}
