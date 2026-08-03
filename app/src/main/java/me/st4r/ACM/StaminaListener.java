package me.st4r.ACM;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
}
