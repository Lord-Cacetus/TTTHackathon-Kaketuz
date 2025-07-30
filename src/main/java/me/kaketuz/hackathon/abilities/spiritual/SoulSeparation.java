package me.kaketuz.hackathon.abilities.spiritual;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import io.github.gonalez.znpcs.ServersNPC;
import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCType;
import io.github.gonalez.znpcs.user.ZUser;
import me.kaketuz.nightmarelib.lib.animation.AnimationBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;

import java.util.UUID;

public class SoulSeparation extends SpiritualAbility implements AddonAbility {

    private NPC playerVisualizer;
    private int NPC_ID;
    private Vex sitEntity;


    public SoulSeparation(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, SoulSeparation.class)) return;

        if (!player.isSneaking()) return;

        NPC_ID = NPC.all().size() + 1;
        playerVisualizer = ServersNPC.createNPC(NPC_ID, NPCType.PLAYER, player.getLocation(), player.getDisplayName());

        try {
            AnimationBuilder.playAnimation((Player) playerVisualizer.getBukkitEntity(), "\"SoulSeparation_start\"");
        } catch (IllegalArgumentException e) {

        }

        start();
    }

    @Override
    public void remove() {
        super.remove();
        if (playerVisualizer != null) {
            player.teleport(playerVisualizer.getLocation());
            ServersNPC.deleteNPC(NPC_ID);
        }
    }

    @Override
    public void progress() {

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
        return "";
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
        return "";
    }

    @Override
    public String getVersion() {
        return "";
    }
}
