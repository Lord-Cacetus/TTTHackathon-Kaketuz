package me.kaketuz.hackathon.abilities.spiritual;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import de.oliver.fancynpcs.api.*;
import me.kaketuz.hackathon.Hackathon;
import me.kaketuz.nightmarelib.lib.animation.AnimationBuilder;

import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class SoulSeparation extends SpiritualAbility implements AddonAbility {


    private NpcData dat;
    private Npc target;

    private long chargeDuration;

    public enum SeparationStates {
        CHARGING, FLYING, APPEARANCE, MOVING_TO_BODY
    }

    private SeparationStates current;

    private boolean canFlyByDefault;
    private GameMode oldGamemode;



    public SoulSeparation(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, SoulSeparation.class)) return;


        chargeDuration = Hackathon.config.getLong("Air.Spiritual.SoulSeparation.ChargeDuration");


        dat = new NpcData("#soulseparation(" + getAbilities(SoulSeparation.class).size() + ")", player.getUniqueId(), player.getLocation());
        dat.setSkin(player.getName());
        dat.setDisplayName(player.getDisplayName());
        dat.setShowInTab(false);
        dat.setCollidable(false);
        AttributeManager attributeManager = FancyNpcsPlugin.get().getAttributeManager();
        NpcAttribute attribute = attributeManager.getAttributeByName(dat.getType(), "pose");
        dat.addAttribute(attribute, "sitting");

        target = FancyNpcsPlugin.get().getNpcAdapter().apply(dat);

        FancyNpcsPlugin.get().getNpcManager().registerNpc(target);
        target.create();
        target.spawnForAll();


        current = SeparationStates.CHARGING;


        canFlyByDefault = player.getAllowFlight();

        start();
    }

    @Override
    public void remove() {
        super.remove();
        player.setGameMode(oldGamemode);
        if (target != null) {
            player.teleport(target.getData().getLocation());
            target.removeForAll();
            FancyNpcsPlugin.get().getNpcManager().removeNpc(target);
        }
    }

    @Override
    public void progress() {
        switch (current) {
            case CHARGING -> {
                player.setVelocity(new Vector(0, -2, 0));
                player.addPotionEffect(new PotionEffect(GeneralMethods.getMCVersion() >= 1205 ? PotionEffectType.SLOWNESS : PotionEffectType.getByName("SLOW"), 20, 255, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, true, false, false));
                if (System.currentTimeMillis() > getStartTime() + chargeDuration) {
                    oldGamemode = player.getGameMode();
                    player.setGameMode(GameMode.SPECTATOR);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    current = SeparationStates.FLYING;
                }
                if (!player.isSneaking()) remove();
            }
            case FLYING -> {
                if (Hackathon.factory.isGhost(player)) {
                    Hackathon.factory.setGhost(player, false);
                }
            }
            case APPEARANCE -> {
                player.setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.RESET + player.getDisplayName());
                player.setGlowing(true);
            }
            case MOVING_TO_BODY -> {
                if (player.getGameMode() != GameMode.SPECTATOR) player.setGameMode(GameMode.SPECTATOR);
                if (player.isGlowing()) player.setGlowing(false);
                if (player.isFlying()) {
                    if (!canFlyByDefault) player.setAllowFlight(false);
                    player.setFlying(false);
                }

                if (!target.getData().isGlowing()) {
                    target.getData().setGlowing(true);
                    target.getData().setGlowingColor(NamedTextColor.DARK_AQUA);
                }

                player.setVelocity(GeneralMethods.getDirection(player.getLocation(), target.getData().getLocation()).normalize());

                if (player.getLocation().distance(target.getData().getLocation()) < 2) remove();
            }

        }

    }


    public void setCurrent(SeparationStates current) {
        this.current = current;
    }

    public SeparationStates getCurrent() {
        return current;
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
        return "SoulSeparation";
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
        return "lol";
    }

    @Override
    public String getVersion() {
        return "hmm";
    }

    public static void sendEmoteToPlayer(Player receiver, int entityId, String emoteId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);

        try {
            data.writeInt(entityId);
            data.writeUTF(emoteId);
            data.writeLong(0L);
            data.writeBoolean(false);
            data.writeBoolean(false);
            data.writeBoolean(false);
            data.writeBoolean(false);

            receiver.sendPluginMessage(
                    Hackathon.plugin,
                    "emotecraft:emote",
                    out.toByteArray()
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
