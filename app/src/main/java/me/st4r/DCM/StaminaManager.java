package me.st4r.DCM;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
public class StaminaManager {

    private final Plugin plugin;
    private final Map<UUID, BossBar> stamBars = new HashMap<>();
    private final Map<UUID, Double> stamina = new HashMap<>();
    private final Map<UUID, BukkitTask> fadeTasks = new HashMap<>();
    private final Set<UUID> waveMode = new HashSet<>();

    private static final double MAX_STAMINA = 100.0;
    private static final int FADE_DELAY_TICKS = 40;

    public StaminaManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initPlayer(Player player) {
        BossBar bar = Bukkit.createBossBar("§e⚡ Stamina", BarColor.GREEN, BarStyle.SOLID);
        bar.setProgress(1.0);
        stamBars.put(player.getUniqueId(), bar);
        stamina.put(player.getUniqueId(), MAX_STAMINA);
    }

    // --- Stamina drain ---

    public void drain(Player player, double amount) {
        UUID id = player.getUniqueId();
        cancelFade(id);

        double current = Math.max(0, stamina.getOrDefault(id, MAX_STAMINA) - amount);
        stamina.put(id, current);

        if (!waveMode.contains(id)) {
            showBossBar(player, current);
        }
       
    }

    // --- Regen tick  ---

    public void regenTick(Player player, double amount) {
        UUID id = player.getUniqueId();
        double current = stamina.getOrDefault(id, MAX_STAMINA);

        if (current >= MAX_STAMINA) return; 

        double newVal = Math.min(MAX_STAMINA, current + amount);
        stamina.put(id, newVal);

        if (waveMode.contains(id)) {
            updateActionBar(player, newVal);
        } else {
            updateBossBar(player, newVal);
            if (newVal >= MAX_STAMINA) {
                scheduleFade(player);
            }
        }
    }

    // --- Boss bar visibility ---

    private void showBossBar(Player player, double current) {
        BossBar bar = stamBars.get(player.getUniqueId());
        if (bar == null) return;
        bar.addPlayer(player); 
        updateBossBarVisuals(bar, current);
    }

    private void updateBossBar(Player player, double current) {
        BossBar bar = stamBars.get(player.getUniqueId());
        if (bar == null) return;
        updateBossBarVisuals(bar, current);
    }

    private void updateBossBarVisuals(BossBar bar, double current) {
        double progress = current / MAX_STAMINA;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

        if (progress < 0.25) {
            bar.setColor(BarColor.RED);
            bar.setTitle("§c⚡ Stamina");
        } else if (progress < 0.6) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("§e⚡ Stamina");
        } else {
            bar.setColor(BarColor.GREEN);
            bar.setTitle("§a⚡ Stamina");
        }
    }

    private void scheduleFade(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                BossBar bar = stamBars.get(id);
                if (bar != null) bar.removePlayer(player);
                fadeTasks.remove(id);
            }
        }.runTaskLater(plugin, FADE_DELAY_TICKS);

        fadeTasks.put(id, task);
    }

    private void cancelFade(UUID id) {
        BukkitTask task = fadeTasks.remove(id);
        if (task != null) task.cancel();
    }

    // --- Action bar (wave mode) ---

    private void updateActionBar(Player player, double current) {
        int filled = (int) Math.round((current / MAX_STAMINA) * 10);
        int empty = 10 - filled;
        String bar = "§e⚡ " + "§a█".repeat(filled) + "§8░".repeat(empty);
        player.sendActionBar(bar);
    }

    // --- Wave mode switching ---

    public void enterWaveMode(Player player) {
        UUID id = player.getUniqueId();
        waveMode.add(id);

    
        cancelFade(id);
        BossBar bar = stamBars.get(id);
        if (bar != null) bar.removePlayer(player);
        updateActionBar(player, stamina.getOrDefault(id, MAX_STAMINA));
    }

    public void exitWaveMode(Player player) {
        UUID id = player.getUniqueId();
        waveMode.remove(id);

        double current = stamina.getOrDefault(id, MAX_STAMINA);

        if (current < MAX_STAMINA) {
            showBossBar(player, current);
        }
    }

    // --- Cleanup ---

    public void removePlayer(Player player) {
        UUID id = player.getUniqueId();
        cancelFade(id);
        BossBar bar = stamBars.remove(id);
        if (bar != null) bar.removeAll();
        stamina.remove(id);
        waveMode.remove(id);
    }

    public double getStamina(Player player) {
        return stamina.getOrDefault(player.getUniqueId(), MAX_STAMINA);
    }

    public boolean hasStamina(Player player, double required) {
        return getStamina(player) >= required;
    }
}