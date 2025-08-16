package me.kaketuz.hackathon;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.event.PlayerCooldownChangeEvent;
import com.projectkorra.projectkorra.util.TempBlock;
import me.kaketuz.hackathon.abilities.plant.PlantArmor;
import me.kaketuz.hackathon.abilities.plant.combos.VineWalk;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerLoadEvent;
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

        if (CoreAbility.hasAbility(player, PlantArmor.class) && event.getHand() == EquipmentSlot.HAND) {
            PlantArmor.redirectEvent(PlantArmor.InteractionType.LEFT_CLICK, player);
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        if (PlantArmor.isPlantArmor(current) ||
                (current.getItemMeta() != null &&
                        current.getItemMeta().getPersistentDataContainer().has(PlantArmor.ARMOR_UNIQUE_KEY))) {

            event.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(Hackathon.plugin, () -> {
                player.getInventory().setItem(event.getSlot(), current);
                player.updateInventory();
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) continue;
            ItemStack item = event.getOldCursor();
            if (PlantArmor.isPlantArmor(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }




    @EventHandler
    public void onBendingReload(BendingReloadEvent event) {
        Hackathon.plugin.reloadConfig();
        Hackathon.registerConfig();
    }

    @EventHandler
    public void onServerShutdown(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            CoreAbility.getAbilities(PlantArmor.class).forEach(PlantArmor::remove);
            CoreAbility.getAbilities(VineWalk.class).forEach(VineWalk::remove);
        }
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

    @EventHandler
    public void onCooldownAdd(PlayerCooldownChangeEvent event) {
        if (!event.getPlayer().isOnline()) return;
        if (event.getResult() == PlayerCooldownChangeEvent.Result.ADDED) {
            if (CoreAbility.hasAbility(event.getPlayer().getPlayer(), PlantArmor.class)) {
                PlantArmor plant = CoreAbility.getAbility(event.getPlayer().getPlayer(), PlantArmor.class);

                if (plant.isFormed()) {
                    plant.getMultiAbilities().stream()
                            .filter(m -> m.getName().equalsIgnoreCase(event.getAbility()))
                            .findFirst()
                            .ifPresent(m -> plant.addToCooldownList(event.getCooldown(), event.getAbility()));
                }
            }
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!TempBlock.isTempBlock(event.getBlock())) return;
        final TempBlock temp = TempBlock.get(event.getBlock());

        if (PlantArmor.getALL_COLLECTED_TEMP_BLOCKS().contains(temp)) {
            event.setCancelled(true);
        }

    }







}
