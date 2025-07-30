package me.kaketuz.hackathon;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import me.kaketuz.hackathon.abilities.spiritual.SoulSeparation;
import me.kaketuz.hackathon.util.GhostFactory;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class HackathonListener implements Listener {

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null || event.isCancelled() || !event.isSneaking()) return;

        if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SoulSeparation")) {
            new SoulSeparation(player);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null) return;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SoulSeparation")) {
                if (CoreAbility.hasAbility(player, SoulSeparation.class)) {
                    SoulSeparation separation = CoreAbility.getAbility(player, SoulSeparation.class);
                    if (separation.getCurrent() == SoulSeparation.SeparationStates.FLYING) separation.remove();
                }
            }
        }
    }

    @EventHandler
    public void onSpec(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null || event.isCancelled()) return;

        if (CoreAbility.hasAbility(player, SoulSeparation.class)) {
            SoulSeparation separation = CoreAbility.getAbility(player, SoulSeparation.class);
            if (separation.getCurrent() == SoulSeparation.SeparationStates.FLYING) {
                event.setCancelled(true);
                separation.setCurrent(SoulSeparation.SeparationStates.APPEARANCE);
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(true);
                player.setFlying(true);
                Hackathon.factory.setGhost(player, true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null || event.isCancelled()) return;

        if (CoreAbility.hasAbility(player, SoulSeparation.class)) {
            SoulSeparation separation = CoreAbility.getAbility(player, SoulSeparation.class);
            if (separation.getCurrent() == SoulSeparation.SeparationStates.APPEARANCE) {
                separation.setCurrent(SoulSeparation.SeparationStates.MOVING_TO_BODY);
            }
        }
    }


}
