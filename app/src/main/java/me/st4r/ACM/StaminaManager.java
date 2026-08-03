package me.st4r.ACM;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@SuppressWarnings("deprecation")
public class StaminaManager {

    private final Plugin plugin;
    private final Map<UUID, StaminaState> players = new HashMap<>();

    private static final double MAX_STAMINA = 100.0;
    private static final int FADE_DELAY_TICKS = 40;

    public StaminaManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Holds all per-player stamina state in one place. Consolidating this
     * (instead of 3 parallel maps + a set) means there's exactly one map
     * entry to create and one to remove per player — no risk of cleaning
     * up 3 out of 4 data structures and leaking the fourth.
     */
    private static class StaminaState {
        final BossBar bar;
        double value = MAX_STAMINA;
        BukkitTask fadeTask;
        boolean waveMode;

        StaminaState(BossBar bar) {
            this.bar = bar;
        }
    }

    public void initPlayer(Player player) {
        UUID id = player.getUniqueId();
        StaminaState state = players.get(id);

        if (state == null) {
            // Fresh join: create the bar once.
            BossBar bar = Bukkit.createBossBar("§e Stamina", BarColor.GREEN, BarStyle.SOLID);
            state = new StaminaState(bar);
            players.put(id, state);
        } else {
            // Re-init on an existing entry (e.g. plugin reload race, respawn
            // hook, etc.) — reuse the bar instead of creating an orphan.
            cancelFade(state);
            state.bar.removeAll();
        }

        state.value = MAX_STAMINA;
        state.waveMode = false;
        state.bar.setProgress(1.0);
        state.bar.setColor(BarColor.GREEN);
        state.bar.setTitle("§e Stamina");
    }

    // --- Stamina drain ---

    public void drain(Player player, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("drain amount must be non-negative: " + amount);
        }

        StaminaState state = players.get(player.getUniqueId());
        if (state == null) return;

        cancelFade(state);
        state.value = Math.max(0, state.value - amount);

        if (state.waveMode) {
            updateActionBar(player, state.value);
        } else {
            showBossBar(player, state.value);
        }
    }

    // --- Regen tick ---

    public void regenTick(Player player, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("regen amount must be non-negative: " + amount);
        }

        StaminaState state = players.get(player.getUniqueId());
        if (state == null || state.value >= MAX_STAMINA) return;

        state.value = Math.min(MAX_STAMINA, state.value + amount);

        if (state.waveMode) {
            updateActionBar(player, state.value);
        } else {
            updateBossBarVisuals(state.bar, state.value);
            if (state.value >= MAX_STAMINA) {
                scheduleFade(player, state);
            }
        }
    }

    // --- Boss bar visibility ---

    private void showBossBar(Player player, double current) {
        StaminaState state = players.get(player.getUniqueId());
        if (state == null) return;
        state.bar.addPlayer(player);
        updateBossBarVisuals(state.bar, current);
    }

    private void updateBossBarVisuals(BossBar bar, double current) {
        double progress = current / MAX_STAMINA;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

        if (progress < 0.25) {
            bar.setColor(BarColor.RED);
            bar.setTitle("§c Stamina");
        } else if (progress < 0.6) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("§e Stamina");
        } else {
            bar.setColor(BarColor.GREEN);
            bar.setTitle("§a Stamina");
        }
    }

    // --- Fade scheduling ---
    // Looks the Player up fresh via Bukkit.getPlayer(id) at execution time
    // instead of holding a Player reference in the closure. Bukkit Player
    // objects aren't guaranteed valid after the tick they were captured on
    // if the player disconnects in the meantime — this avoids that footgun.

    private void scheduleFade(Player player, StaminaState state) {
        UUID id = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(id);
                if (p != null) state.bar.removePlayer(p);
                state.fadeTask = null;
            }
        }.runTaskLater(plugin, FADE_DELAY_TICKS);

        state.fadeTask = task;
    }

    private void cancelFade(StaminaState state) {
        if (state.fadeTask != null) {
            state.fadeTask.cancel();
            state.fadeTask = null;
        }
    }

    // --- Action bar (wave mode) ---

    private void updateActionBar(Player player, double current) {
        int filled = (int) Math.round((current / MAX_STAMINA) * 10);
        int empty = 10 - filled;
        String bar = "§a█".repeat(filled) + "§8░".repeat(empty);
        player.sendActionBar(bar);
    }

    // --- Wave mode switching ---

    public void enterWaveMode(Player player) {
        StaminaState state = players.get(player.getUniqueId());
        if (state == null) return;

        state.waveMode = true;
        cancelFade(state);
        state.bar.removePlayer(player);
        updateActionBar(player, state.value);
    }

    public void exitWaveMode(Player player) {
        StaminaState state = players.get(player.getUniqueId());
        if (state == null) return;

        state.waveMode = false;

        if (state.value < MAX_STAMINA) {
            showBossBar(player, state.value);
        }
    }

    // --- Cleanup ---

    public void removePlayer(Player player) {
        StaminaState state = players.remove(player.getUniqueId());
        if (state == null) return;

        cancelFade(state);
        state.bar.removeAll();
    }

    /**
     * Call from onDisable() (and ideally before any /reload) to clean up
     * every active BossBar. Without this, a plugin reload while players
     * are online leaks a BossBar per online player — PlayerQuitEvent never
     * fires for them since they never actually quit.
     */
    public void shutdown() {
        for (StaminaState state : players.values()) {
            cancelFade(state);
            state.bar.removeAll();
        }
        players.clear();
    }

    public double getStamina(Player player) {
        StaminaState state = players.get(player.getUniqueId());
        return state != null ? state.value : MAX_STAMINA;
    }

    public boolean hasStamina(Player player, double required) {
        return getStamina(player) >= required;
    }

    public boolean trySpend(Player player, double amount) {
        if (!hasStamina(player, amount)) {
            return false;
        }

        drain(player, amount);
        return true;
    }
}