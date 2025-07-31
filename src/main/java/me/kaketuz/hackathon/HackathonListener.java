package me.kaketuz.hackathon;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import me.kaketuz.hackathon.abilities.plant.PlantArmor;
import me.kaketuz.nightmarelib.lib.vfx.VFX;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HackathonListener implements Listener {


    public static final Set<UUID> HAS_RESOURCE_PACK = new HashSet<>();

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null || event.isCancelled() || !event.isSneaking()) return;

        if (bPlayer.getBoundAbilityName().equalsIgnoreCase("PlantArmor")) new PlantArmor(player);
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null) return;

        if (CoreAbility.hasAbility(player, PlantArmor.class)) {
            PlantArmor armor = CoreAbility.getAbility(player, PlantArmor.class);
            if (armor.isFormed()) {
                switch (bPlayer.getBoundAbilityName()) {
                    case "WithdrawalPlants" -> armor.remove();
                }
            }
        }


    }




}
