package me.kaketuz.hackathon.abilities.plant.combos;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import me.kaketuz.hackathon.Hackathon;
import me.kaketuz.hackathon.abilities.plant.PlantArmor;
import me.kaketuz.hackathon.util.GradientAPI;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.kaketuz.hackathon.abilities.plant.PlantArmor.Vine;

public class VineWalk extends PlantAbility implements AddonAbility, ComboAbility {

    private Vine[] vines;
    private double flySpeed, sourceRange, reachSpeed;
    private long duration, cooldown, plantsGrowInterval;
    private int vineRange, vinesAmount;

    private boolean grew;
    private long startFlyingTiming, i, startRemovingTiming;
    private float oldFlySpeed;
    private boolean allowFlight;
    private boolean removing;

    private Block source;

    public VineWalk(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, VineWalk.class)) return;

        flySpeed = Hackathon.config.getDouble("Plant.Combos.VineWalk.FlySpeed");
        sourceRange = Hackathon.config.getDouble("Plant.Combos.VineWalk.SourceRange");
        reachSpeed = Hackathon.config.getDouble("Plant.Combos.VineWalk.ReachSpeed");
        duration = Hackathon.config.getLong("Plant.Combos.VineWalk.Duration");
        cooldown = Hackathon.config.getLong("Plant.Combos.VineWalk.Cooldown");
        plantsGrowInterval = Hackathon.config.getLong("Plant.Combos.VineWalk.GrowInterval");
        vineRange = Hackathon.config.getInt("Plant.Combos.VineWalk.RangeInt");
        vinesAmount = Hackathon.config.getInt("Plant.Combos.VineWalk.VinesAmount");

        final Block source = BlockSource.getWaterSourceBlock(player, sourceRange, ClickType.SHIFT_DOWN, false, false, true, false, false);

        if (source == null) return;
        if (!isPlant(source)) return;

        this.source = source;

        vines = new Vine[vinesAmount];

        for (int i = 0; i < vinesAmount; i++) {
            vines[i] = PlantArmor.invokeUnlinkedVineTask(source.getLocation(), source.getLocation().add(0, 1, 0), 2);

            vines[i].getRope().applyVelocity(new Vector(
                    ThreadLocalRandom.current().nextDouble(-1, 1),
                    ThreadLocalRandom.current().nextDouble(0, 1),
                    ThreadLocalRandom.current().nextDouble(-1, 1)
            ));
            vines[i].getRope().lockPoint(vinesAmount - 1, false);
            vines[i].getRope().lockPoint(0, true);
        }
        i = plantsGrowInterval;
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || !bPlayer.canBendIgnoreBinds(this)) {
            bPlayer.addCooldown(this);
            remove();
        }

        if (!removing) {
            if (!grew) {
                if (vines[vinesAmount - 1].getRope().getPoints().size() < vineRange) {
                    if (System.currentTimeMillis() > getStartTime() + i) {
                        for (Vine vine : vines) {
                            vine.getRope().setStartPosition(source.getLocation().add(0, 1, 0));
                            vine.getRope().addSegment();
                            vine.getRope().applyVelocity(new Vector(
                                    ThreadLocalRandom.current().nextDouble(-0.25, 0.25),
                                    ThreadLocalRandom.current().nextDouble(-0.25, 0.25),
                                    ThreadLocalRandom.current().nextDouble(-0.25, 0.25)
                            ));
                        }
                        i += plantsGrowInterval;
                    }
                } else {
                    for (Vine vine : vines) {
                        vine.getRope().getPoints().getLast().applyVelocity(GeneralMethods.getDirection(vine.getRope().getRenderPoints().getLast(), player.getLocation()).normalize().multiply(reachSpeed));
                    }
                    if (Arrays.stream(vines).allMatch(v -> v.getRope().getRenderPoints().getLast().distance(player.getLocation()) < 0.5)) {
                        allowFlight = player.getAllowFlight();
                        if (!player.getAllowFlight()) player.setAllowFlight(true);
                        player.setFlying(true);
                        oldFlySpeed = player.getFlySpeed();
                        startFlyingTiming = System.currentTimeMillis();
                        player.setFlySpeed((float) flySpeed);
                        grew = true;
                    }
                    if (Arrays.stream(vines).allMatch(v -> v.getRope().getRenderPoints().getLast().distance(player.getLocation()) > vineRange * 2)) {
                        bPlayer.addCooldown(this);
                        remove();
                    }
                }
            } else {
                for (Vine vine : vines) {
                    vine.getRope().setStartPosition(source.getLocation().add(0, 1, 0));
                    vine.getRope().setEndPosition(player.getLocation());

                    if (player.getLocation().distance(source.getLocation()) > vineRange) {
                        player.setVelocity(GeneralMethods.getDirection(player.getLocation(), source.getLocation()).normalize());

                    }
                }
                if (System.currentTimeMillis() > startFlyingTiming + duration) {
                    if (grew) {
                        player.setAllowFlight(allowFlight);
                        player.setFlySpeed(oldFlySpeed);
                        player.setFlying(false);
                    }
                    for (Vine vine : vines) {
                        vine.getRope().setGravity(new Vector(0, -9.8, 0));
                        vine.getRope().lockPoint(vineRange - 1, false);
                    }
                    startRemovingTiming = System.currentTimeMillis();
                    i = 0;
                    removing = true;
                }
            }
        }
        else {
           if (System.currentTimeMillis() > startRemovingTiming + i) {
               for (Vine vine : vines) {
                   if (vine.getRope().getRenderPoints().size() <= 2) {
                       Arrays.stream(vines).forEach(Vine::cancel);
                       bPlayer.addCooldown(this);
                       remove();
                       return;
                   }
                   vine.getRope().removeSegment();
                   vine.getDisplays().getLast().remove();
                   vine.getDisplays().removeLast();
               }
               i += 150;
           }
        }

    }

    @Override
    public void remove() {
        super.remove();
        if (grew && !removing) {
            player.setAllowFlight(allowFlight);
            player.setFlySpeed(oldFlySpeed);
            player.setFlying(false);
        }
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "VineWalk";
    }

    @Override
    public Location getLocation() {
        return player == null ? null : player.getLocation();
    }

    @Override
    public List<Location> getLocations() {
        return Arrays.stream(vines)
                .flatMap(vine -> vine.getRope().getRenderPoints().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void load() {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getAuthor() {
        return GradientAPI.colorize("<#247A4E>Lᴏʀᴅ_Cᴀᴄᴇᴛᴜs_ & Hɪʀᴏ3</#7CCF36>");
    }

    @Override
    public String getVersion() {
        return GradientAPI.colorize("<#247A4E>Hᴀᴄᴋᴀᴛʜᴏɴ - KᴀᴋᴇᴛᴜZ</#7CCF36>");
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new VineWalk(player);
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        return ComboUtil.generateCombinationFromList(this, Hackathon.config.getStringList("Plant.Combos.VineWalk.Combination"));
    }

    @Override
    public String getDescription() {
        return Hackathon.config.getString("Plant.Combos.VineWalk.Description");
    }

    @Override
    public String getInstructions() {
        return Hackathon.config.getString("Plant.Combos.VineWalk.Instructions");
    }

    public long getDuration() {
        return duration;
    }

    public double getSourceRange() {
        return sourceRange;
    }

    public double getFlySpeed() {
        return flySpeed;
    }

    public double getReachSpeed() {
        return reachSpeed;
    }

    public int getVineRange() {
        return vineRange;
    }

    public int getVinesAmount() {
        return vinesAmount;
    }

    public long getPlantsGrowInterval() {
        return plantsGrowInterval;
    }

    public Vine[] getVines() {
        return vines;
    }

    public float getOldFlySpeed() {
        return oldFlySpeed;
    }

    public long getStartFlyingTiming() {
        return startFlyingTiming;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public void setSourceRange(double sourceRange) {
        this.sourceRange = sourceRange;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setFlySpeed(double flySpeed) {
        this.flySpeed = flySpeed;
    }

    public void setGrew(boolean grew) {
        this.grew = grew;
    }

    public void setPlantsGrowInterval(long plantsGrowInterval) {
        this.plantsGrowInterval = plantsGrowInterval;
    }

    public void setReachSpeed(double reachSpeed) {
        this.reachSpeed = reachSpeed;
    }

    public void setVines(Vine[] vines) {
        this.vines = vines;
    }

    public void setVineRange(int vineRange) {
        this.vineRange = vineRange;
    }

    public void setStartFlyingTiming(long startFlyingTiming) {
        this.startFlyingTiming = startFlyingTiming;
    }

    public void setVinesAmount(int vinesAmount) {
        this.vinesAmount = vinesAmount;
    }

    public void setAllowFlight(boolean allowFlight) {
        this.allowFlight = allowFlight;
    }

    public void setOldFlySpeed(float oldFlySpeed) {
        this.oldFlySpeed = oldFlySpeed;
    }
}
