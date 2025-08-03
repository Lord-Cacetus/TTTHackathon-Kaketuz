package me.kaketuz.hackathon;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import me.kaketuz.hackathon.abilities.plant.PlantArmor;
import me.kaketuz.nightmarelib.lib.vfx.VFX;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HackathonListener implements Listener {


    public static final Set<UUID> HAS_RESOURCE_PACK = new HashSet<>();

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null || event.isCancelled()) return;

        if (bPlayer.getBoundAbilityName().equalsIgnoreCase("PlantArmor")) new PlantArmor(player);

        if (CoreAbility.hasAbility(player, PlantArmor.class)) PlantArmor.redirectEvent(event.isSneaking() ? PlantArmor.InteractionType.SNEAK_DOWN : PlantArmor.InteractionType.SNEAK_UP, player);
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null) return;

        if (CoreAbility.hasAbility(player, PlantArmor.class) && event.getAction() == Action.LEFT_CLICK_AIR && event.getHand() == EquipmentSlot.HAND) {
            PlantArmor.redirectEvent(PlantArmor.InteractionType.LEFT_CLICK, player);
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        if (PlantArmor.isPlantArmor(current)) {
            event.setCancelled(true);
            current.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack current = event.getCursor();
        if (current == null || current.getType() == Material.AIR) return;

        if (PlantArmor.isPlantArmor(current)) {
            event.setCancelled(true);
            current.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onInventoryDrop(PlayerDropItemEvent event) {
        ItemStack current = event.getItemDrop().getItemStack();

        if (PlantArmor.isPlantArmor(current)) {
            event.setCancelled(true);
            current.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onBendingReload(BendingReloadEvent event) {
        Hackathon.plugin.reloadConfig();
        Hackathon.registerConfig();
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.isCancelled()) return;

        if (CoreAbility.hasAbility(player, PlantArmor.class)) {
            final PlantArmor plant = CoreAbility.getAbility(player, PlantArmor.class);
            if (plant.isFormed()) {
                event.setCancelled(true);
                plant.invokeLandTask(event.getDamage());
            }
        }
    }







}
