package me.st4r.DCM;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;



public class DCM extends JavaPlugin implements Listener{
    
//Hashmaps, cooldowns, this and that. (Combat Abilities)
  private static final long SWORD_PARRY_WINDOW_MS = 200;
    private static final long SHIELD_PARRY_WINDOW_MS = 250;
    
    private static final long SWORD_COOLDOWN_MS = 10000;
    private static final long SHIELD_COOLDOWN_MS = 30000;
    
    private static final int STUN_DURATION_TICKS = 100; // 5 seconds
    private static final double SLAM_TRUE_DAMAGE = 6.0; // 3 Hearts


    // Adrenaline Constants
    private static final long ADRENALINE_COOLDOWN_MS = 180000; // 3 minutes
    private static final int ADRENALINE_DURATION_TICKS = 200; // 10 seconds
    private static final double ADRENALINE_HEALTH_THRESHOLD = 8.0; // 4 Hearts

    // Cooldown Maps
    private final Map<UUID, Long> swordCooldowns = new HashMap<>();
    private final Map<UUID, Long> shieldCooldowns = new HashMap<>();
    private final Map<UUID, Long> adrenalineCooldowns = new HashMap<>();
    // Dash Constants
    private static final long DASH_COOLDOWN_MS = 5000; // 5 seconds
    private static final long DASH_WINDOW_MS = 1000; // Time to press shift 3 times
    private static final long INVULN_DURATION_MS = 300; // 0.3 seconds
    
    // Timing Maps
    private final Map<UUID, Long> lastSwingTimes = new HashMap<>();
    private final Map<UUID, Long> lastBlockTimes = new HashMap<>();
    private final Map<UUID, Boolean> dashEnabled = new HashMap<>();
    private final Map<UUID, java.util.List<Long>> sneakTimestamps = new HashMap<>();
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> invulnerablePlayers = new HashMap<>();
    
    // Combo Maps
    private final Map<UUID, Integer> axeCombos = new HashMap<>();
    
    // Shield Break Tracking (shared with DoubleWeapons)
    private final Map<UUID, Long> brokenShields = new HashMap<>();
//-------------------------------------------------------------------------------------------------------------------------------------------
//DoubleWeapons, again stuff like hashmaps and timings

    private final Map<UUID, Long> meleeCooldowns = new HashMap<>();
    private final Map<UUID, Long> bowDrawStarts = new HashMap<>();
    
    // Shield Breaking System
    private final Map<UUID, Integer> shieldHitStreak = new HashMap<>();
    private final Map<UUID, UUID> lastTargets = new HashMap<>();
    private final Map<UUID, Long> shieldStreakTimestamps = new HashMap<>(); 
    
    private static final long COOLDOWN_MS = 3000; 
    private static final long DOUBLE_CHARGE_TIME_MS = 3000;
    private static final long SHIELD_BREAK_DURATION_MS = 5000;
    private static final int SHIELD_BREAK_THRESHOLD = 4;

    private Plugin combatAbilitiesPlugin;
    private Method breakShieldMethod;







}