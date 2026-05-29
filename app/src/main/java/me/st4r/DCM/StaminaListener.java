package me.st4r.DCM;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class StaminaListener implements Listener {

    private final StaminaManager staminaManager;

    public StaminaListener(StaminaManager staminaManager) {
        this.staminaManager = staminaManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        staminaManager.initPlayer(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        staminaManager.removePlayer(e.getPlayer());
    }

@EventHandler(priority = EventPriority.HIGHEST)
public void onAttack(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof Player player)) return;
    if (e.isCancelled()) return;

    ItemStack main = player.getInventory().getItemInMainHand();
    ItemStack off = player.getInventory().getItemInOffHand();

    boolean mainWeapon = main.getType().name().endsWith("_SWORD") ||
                         main.getType().name().endsWith("_AXE") ||
                         main.getType() == Material.MACE;
    boolean offWeapon  = off.getType().name().endsWith("_SWORD") ||
                         off.getType().name().endsWith("_AXE") ||
                         off.getType() == Material.MACE;

    
    if (mainWeapon && offWeapon) {
        if (!staminaManager.hasStamina(player, 10)) {
            e.setCancelled(true);
            player.sendActionBar("§c Not enough stamina to dual strike!");
            return;
        }
        staminaManager.drain(player, 10);
    }
}


    public void onParry(Player player) {
        staminaManager.drain(player, 15.0);
    }
}