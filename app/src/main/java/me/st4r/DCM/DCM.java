package me.st4r.DCM;
 
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.geysermc.floodgate.api.FloodgateApi;
 
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
 
/**
 * DCM - Dams's Combat Mechanics
 * A comprehensive combat overhaul plugin combining dual-wielding, parry mechanics,
 * combo systems, and mobility enhancements.
 * 
 * @author st4r
 * @version 2.0.6-debug
 */
public class DCM extends JavaPlugin implements Listener {
 
    // ===========================
    // DEBUG CONFIGURATION
    // ===========================
    private boolean debug = true; // SET THIS TO false TO DISABLE DEBUGGING
 
    // ===========================
    // DUAL WIELDING CONFIGURATION
    // ===========================
    private static final long DUAL_MELEE_COOLDOWN_MS = 3000;
    private static final long DUAL_BOW_CHARGE_TIME_MS = 1000;
    private static final int SHIELD_BREAK_THRESHOLD = 4;
    private static final long SHIELD_BREAK_DURATION_MS = 5000;
    private static final long SHIELD_STREAK_TIMEOUT_MS = 4000;
    private static final long MACE_GUARD_WINDOW_MS = 1500;
    private static final double MACE_GUARD_DAMAGE_MULTIPLIER = 0.6;
    private static final long MACE_GUARD_COOLDOWN_MS = 5000;

 
    // ===========================
    // PARRY CONFIGURATION
    // ===========================
    private static final long SWORD_PARRY_WINDOW_MS = 200;
    private static final long SWORD_PARRY_WINDOW_BEDROCK_MS = 400;
    private static final long SHIELD_PARRY_WINDOW_MS = 250;
    private static final long SWORD_PARRY_COOLDOWN_MS = 4000;
    private static final long SHIELD_PARRY_COOLDOWN_MS = 10000;
    private static final int SHIELD_STUN_DURATION_TICKS = 100;
    private static final long RIPOSTE_WINDOW_MS = 1500;
    private static final double RIPOSTE_DAMAGE_MULTIPLIER = 1.6;
    private static final double RIPOSTE_KNOCKBACK_HORIZONTAL = 1.1;
    private static final double RIPOSTE_KNOCKBACK_VERTICAL = 0.25;
 
    // ===========================
    // AXE COMBO CONFIGURATION
    // ===========================
    private static final int AXE_COMBO_MAX = 4;
    private static final double AXE_SLAM_TRUE_DAMAGE = 6.0; // 3 hearts
    private static final long AXE_COMBO_TIMEOUT_MS = 8000;
 
    // ===========================
    // DASH CONFIGURATION
    // ===========================
    private static final long DASH_COOLDOWN_MS = 5000;
    private static final long DASH_INVULN_DURATION_MS = 300;
    private static final double DASH_VELOCITY_MULTIPLIER = 1.5;
    private static final double DASH_VERTICAL_BOOST = 0.2;
 
    // ===========================
    // ADRENALINE CONFIGURATION
    // ===========================
    private static final long ADRENALINE_COOLDOWN_MS = 180000; // 3 minutes
    private static final int ADRENALINE_DURATION_TICKS = 200; // 10 seconds
    private static final double ADRENALINE_HEALTH_THRESHOLD = 8.0; // 4 hearts
 
    // ===========================
    // DUAL WIELDING STATE
    // ===========================
    private final Map<UUID, Long> meleeCooldowns = new HashMap<>();
    private final Map<UUID, Long> bowDrawStarts = new HashMap<>();
    private final Map<UUID, Integer> shieldHitStreak = new HashMap<>();
    private final Map<UUID, UUID> lastTargets = new HashMap<>();
    private final Map<UUID, Long> shieldStreakTimestamps = new HashMap<>();
    private final Map<UUID, Long> lastExhaustionMsgTimes = new HashMap<>();
 
    // ===========================
    // PARRY & COMBAT STATE
    // ===========================
    private final Map<UUID, Long> lastSwingTimes = new HashMap<>();
    private final Map<UUID, Long> lastBlockTimes = new HashMap<>();
    private final Map<UUID, Long> swordParryCooldowns = new HashMap<>();
    private final Map<UUID, Long> shieldParryCooldowns = new HashMap<>();
    private final Map<UUID, Long> brokenShields = new HashMap<>();
    private final Map<UUID, Long> maceGuardTimes = new HashMap<>();
    private final Map<UUID, Long> maceGuardCooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> maceGuardCountdownTasks = new HashMap<>();
    private final Map<UUID, Long> riposteWindows = new HashMap<>();
 
    // ===========================
    // AXE COMBO STATE
    // ===========================
    private final Map<UUID, Integer> axeCombos = new HashMap<>();
    private final Map<UUID, Long> axeComboTimestamps = new HashMap<>();
 
    // ===========================
    // DASH STATE
    // ===========================
    private final Map<UUID, Boolean> dashEnabled = new HashMap<>();
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> invulnerablePlayers = new HashMap<>();
 
    // ===========================
    // ADRENALINE STATE
    // ===========================
    private final Map<UUID, Long> adrenalineCooldowns = new HashMap<>();

    // ===========================
    // OPTIONAL DEPENDENCY FLAGS
    // ===========================
    private boolean floodgateAvailable = false;

    // ===========================
    // DEBUG HELPER METHOD
    // ===========================
    private void debugLog(String message) {
        if (debug) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    @Override
    public void onEnable() {
        debugLog("=== onEnable() START ===");
        
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateAvailable = true;
            debugLog("Floodgate detected and enabled");
        } else {
            debugLog("Floodgate not found - Bedrock support disabled");
            getLogger().warning("Floodgate not found! Bedrock player support is disabled. Install Floodgate + Geyser for Bedrock compatibility.");
        }

        getServer().getPluginManager().registerEvents(this, this);
        debugLog("Event listeners registered");
        
        startMemoryCleanupTask();
        debugLog("Memory cleanup task started");
        
        getLogger().info("-----------------------------------------------------------------------");
        getLogger().info("DCM (Dams's Combat Mechanics) v2.1-debug has been enabled!");
        getLogger().info("DEBUG MODE: " + (debug ? "ENABLED" : "DISABLED"));
        getLogger().info("Have fun and Good Luck!");
        getLogger().info("-----------------------------------------------------------------------");
        
        debugLog("=== onEnable() END ===");
    }
 
    @Override
    public void onDisable() {
        debugLog("=== onDisable() START ===");
        debugLog("Cancelling " + maceGuardCountdownTasks.size() + " mace guard tasks");
        
        for (BukkitRunnable task : maceGuardCountdownTasks.values()) {
            task.cancel();
        }
        maceGuardCountdownTasks.clear();

        getLogger().severe("Oh.. server is dead?");
        getLogger().info("DCM has been disabled! :>");
        getLogger().info("0x6B696E646E657373 <3");
        
        debugLog("=== onDisable() END ===");
    }
 
    /**
     * Periodic cleanup task to prevent memory leaks from stale entries
     */
    private void startMemoryCleanupTask() {
        debugLog("=== startMemoryCleanupTask() START ===");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long staleTime = now - 10000;
                
                debugLog("--- Memory Cleanup Cycle ---");
                int cleanedCount = 0;
 
                // Clean up timing maps
                int beforeSize = lastSwingTimes.size();
                lastSwingTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                int removed = beforeSize - lastSwingTimes.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " stale lastSwingTimes entries");

                beforeSize = lastBlockTimes.size();
                lastBlockTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                removed = beforeSize - lastBlockTimes.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " stale lastBlockTimes entries");

                beforeSize = shieldStreakTimestamps.size();
                shieldStreakTimestamps.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                removed = beforeSize - shieldStreakTimestamps.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " stale shieldStreakTimestamps entries");

                beforeSize = lastExhaustionMsgTimes.size();
                lastExhaustionMsgTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                removed = beforeSize - lastExhaustionMsgTimes.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " stale lastExhaustionMsgTimes entries");

                beforeSize = riposteWindows.size();
                riposteWindows.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - riposteWindows.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " expired riposteWindows");

                // Clean up expired shield breaks
                beforeSize = brokenShields.size();
                brokenShields.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - brokenShields.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " expired brokenShields");

                beforeSize = maceGuardTimes.size();
                maceGuardTimes.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - maceGuardTimes.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " expired maceGuardTimes");

                beforeSize = maceGuardCooldowns.size();
                maceGuardCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - maceGuardCooldowns.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " expired maceGuardCooldowns");

                // Clean up expired invulnerability
                beforeSize = invulnerablePlayers.size();
                invulnerablePlayers.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - invulnerablePlayers.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " expired invulnerablePlayers");

                // Clean up stale axe combos
                beforeSize = axeComboTimestamps.size();
                axeComboTimestamps.entrySet().removeIf(entry -> {
                    boolean isStale = entry.getValue() < staleTime;
                    if (isStale) {
                        UUID playerId = entry.getKey();
                        axeCombos.remove(playerId);
                    }
                    return isStale;
                });
                removed = beforeSize - axeComboTimestamps.size();
                cleanedCount += removed;
                debugLog("Cleaned " + removed + " stale axe combos");
                
                debugLog("Total entries cleaned: " + cleanedCount);
            }
        }.runTaskTimer(this, 200L, 200L);
        
        debugLog("=== startMemoryCleanupTask() END ===");
    }
 
    // ===============================================
    // EVENT HANDLERS
    // ===============================================
 
    /**
     * Cleanup player state on disconnect
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        debugLog("=== onPlayerQuit() START === Player: " + event.getPlayer().getName() + " (" + playerId + ")");
        
        BukkitRunnable task = maceGuardCountdownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
            debugLog("Cancelled mace guard task for disconnecting player");
        }
        
        // Clean up all state for this player
        meleeCooldowns.remove(playerId);
        bowDrawStarts.remove(playerId);
        shieldHitStreak.remove(playerId);
        lastTargets.remove(playerId);
        shieldStreakTimestamps.remove(playerId);
        lastExhaustionMsgTimes.remove(playerId);
        lastSwingTimes.remove(playerId);
        lastBlockTimes.remove(playerId);
        swordParryCooldowns.remove(playerId);
        shieldParryCooldowns.remove(playerId);
        brokenShields.remove(playerId);
        maceGuardTimes.remove(playerId);
        maceGuardCooldowns.remove(playerId);
        riposteWindows.remove(playerId);
        axeCombos.remove(playerId);
        axeComboTimestamps.remove(playerId);
        dashEnabled.remove(playerId);
        dashCooldowns.remove(playerId);
        invulnerablePlayers.remove(playerId);
        adrenalineCooldowns.remove(playerId);
        
        debugLog("All state cleaned for player " + event.getPlayer().getName());
        debugLog("=== onPlayerQuit() END ===");
    }
 
    /**
     * Handles melee attacks for dual wielding and parry mechanics
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        debugLog("=== onPlayerAttack() START ===");
        
        if (event.isCancelled()) {
            debugLog("Event already cancelled, returning");
            debugLog("=== onPlayerAttack() END ===");
            return;
        }
        
        if (!(event.getDamager() instanceof Player attacker)) {
            debugLog("Damager is not a player, returning");
            debugLog("=== onPlayerAttack() END ===");
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            debugLog("Victim is not a living entity, returning");
            debugLog("=== onPlayerAttack() END ===");
            return;
        }

        UUID attackerId = attacker.getUniqueId();
        debugLog("Attacker: " + attacker.getName() + " (" + attackerId + ")");
        debugLog("Victim: " + victim.getName() + " (Type: " + victim.getType() + ")");

        long now = System.currentTimeMillis();
        debugLog("Current timestamp: " + now);

        // Check if attacker is invulnerable from dash
        Long invulnExpire = invulnerablePlayers.get(attackerId);
        if (invulnExpire != null && now <= invulnExpire) {
            debugLog("Attacker is invulnerable from dash, allowing attack");
        }

        // ===========================
        // VICTIM INVULNERABILITY CHECK
        // ===========================
        if (victim instanceof Player victimPlayer) {
            UUID victimId = victimPlayer.getUniqueId();
            debugLog("Victim is a player: " + victimPlayer.getName() + " (" + victimId + ")");
            
            Long victimInvulnExpire = invulnerablePlayers.get(victimId);
            if (victimInvulnExpire != null && now <= victimInvulnExpire) {
                event.setCancelled(true);
                debugLog("Victim is invulnerable from dash - attack cancelled");
                debugLog("=== onPlayerAttack() END ===");
                return;
            }
            
            // ===========================
            // MACE GUARD CHECK
            // ===========================
            Long guardExpire = maceGuardTimes.get(victimId);
            if (guardExpire != null && now <= guardExpire) {
                double originalDamage = event.getDamage();
                double reducedDamage = originalDamage * MACE_GUARD_DAMAGE_MULTIPLIER;
                event.setDamage(reducedDamage);
                debugLog("Mace guard active - damage reduced from " + originalDamage + " to " + reducedDamage);
                
                final Player guardedVictim = victimPlayer;
                new BukkitRunnable() {
                    @Override 
                    public void run() { 
                        guardedVictim.setVelocity(new Vector(0, 0, 0));
                        debugLog("Nullified knockback for guarded player");
                    }
                }.runTaskLater(DCM.this, 1L);
            }
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        Material weaponType = weapon.getType();
        debugLog("Weapon type: " + weaponType);

        // ===========================
        // RIPOSTE DAMAGE MULTIPLIER
        // ===========================
        Long riposteExpire = riposteWindows.get(attackerId);
        if (riposteExpire != null && now <= riposteExpire) {
            double originalDamage = event.getDamage();
            double riposteDamage = originalDamage * RIPOSTE_DAMAGE_MULTIPLIER;
            event.setDamage(riposteDamage);
            debugLog("RIPOSTE! Damage increased from " + originalDamage + " to " + riposteDamage);
            
            Vector direction = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
            direction.multiply(RIPOSTE_KNOCKBACK_HORIZONTAL).setY(RIPOSTE_KNOCKBACK_VERTICAL);
            victim.setVelocity(direction);
            debugLog("Applied riposte knockback: " + direction);
            
            attacker.playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f);
            attacker.sendActionBar("§c§l⚔ RIPOSTE! (" + String.format("%.1f", riposteDamage) + " damage)");
            
            riposteWindows.remove(attackerId);
            debugLog("Riposte window consumed");
        }

        // ===========================
        // AXE COMBO SYSTEM
        // ===========================
        if (weaponType.name().endsWith("_AXE")) {
            debugLog("Axe weapon detected - processing combo system");
            handleAxeCombo(attacker, victim, event, now);
        }

        // ===========================
        // DUAL WIELDING MELEE
        // ===========================
        ItemStack offhand = attacker.getInventory().getItemInOffHand();
        Material offhandType = offhand.getType();
        debugLog("Offhand type: " + offhandType);

        boolean isMainhandWeapon = weaponType.name().endsWith("_SWORD") || 
                                   weaponType.name().endsWith("_AXE") || 
                                   weaponType == Material.MACE;
        boolean isOffhandWeapon = offhandType.name().endsWith("_SWORD") || 
                                  offhandType.name().endsWith("_AXE") || 
                                  offhandType == Material.MACE;

        debugLog("Mainhand is weapon: " + isMainhandWeapon + ", Offhand is weapon: " + isOffhandWeapon);

        if (isMainhandWeapon && isOffhandWeapon) {
            long cooldown = meleeCooldowns.getOrDefault(attackerId, 0L);
            debugLog("Dual wield detected - cooldown expires at: " + cooldown);
            
            if (now < cooldown) {
                long remaining = (cooldown - now) / 1000;
                debugLog("Dual wield on cooldown - " + remaining + "s remaining");
                
                long lastMsgTime = lastExhaustionMsgTimes.getOrDefault(attackerId, 0L);
                if (now - lastMsgTime >= 1000) {
                    attacker.sendActionBar("§7Exhausted... (wait " + remaining + "s)");
                    lastExhaustionMsgTimes.put(attackerId, now);
                    debugLog("Sent exhaustion message to player");
                }
                debugLog("=== onPlayerAttack() END ===");
                return;
            }

            double mainDamage = getBaseDamage(weaponType);
            double offDamage = getBaseDamage(offhandType);
            double totalDamage = mainDamage + offDamage;
            
            debugLog("Calculating dual wield damage:");
            debugLog("  Main weapon damage: " + mainDamage);
            debugLog("  Off weapon damage: " + offDamage);
            debugLog("  Total damage: " + totalDamage);

            int sharpnessMain = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
            int sharpnessOff = offhand.getEnchantmentLevel(Enchantment.SHARPNESS);
            double sharpnessBonus = (sharpnessMain + sharpnessOff) * 0.5;
            totalDamage += sharpnessBonus;
            
            debugLog("Sharpness bonus: " + sharpnessBonus + " (total damage now: " + totalDamage + ")");

            event.setDamage(totalDamage);
            

            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
            attacker.sendActionBar("§6§lDUAL STRIKE! §e(" + String.format("%.1f", totalDamage) + " damage)");
            debugLog("Applied dual wield strike with total damage: " + totalDamage);

            meleeCooldowns.put(attackerId, now + DUAL_MELEE_COOLDOWN_MS);
            debugLog("Set dual wield cooldown until: " + (now + DUAL_MELEE_COOLDOWN_MS));
        }

        // ===========================
        // SHIELD HIT STREAK TRACKING
        // ===========================
        if (victim instanceof Player victimPlayer) {
            UUID victimId = victimPlayer.getUniqueId();
            
            if (victimPlayer.isBlocking()) {
                debugLog("Victim is blocking with shield");
                
                UUID lastTarget = lastTargets.get(attackerId);
                Long lastStreakTime = shieldStreakTimestamps.get(attackerId);
                
                debugLog("Last target: " + lastTarget + ", Last streak time: " + lastStreakTime);

                if (lastTarget == null || !lastTarget.equals(victimId) || 
                    lastStreakTime == null || (now - lastStreakTime) > SHIELD_STREAK_TIMEOUT_MS) {
                    shieldHitStreak.put(attackerId, 1);
                    debugLog("Reset shield streak to 1 (new target or timeout)");
                } else {
                    int streak = shieldHitStreak.getOrDefault(attackerId, 0) + 1;
                    shieldHitStreak.put(attackerId, streak);
                    debugLog("Incremented shield streak to: " + streak);

                    if (streak >= SHIELD_BREAK_THRESHOLD) {
                        breakShield(victimId, SHIELD_BREAK_DURATION_MS);
                        attacker.sendActionBar("§c§l⚔ SHIELD SHATTERED!");
                        shieldHitStreak.remove(attackerId);
                        debugLog("Shield broken! Streak threshold reached.");
                    }
                }

                lastTargets.put(attackerId, victimId);
                shieldStreakTimestamps.put(attackerId, now);
                debugLog("Updated last target and streak timestamp");
            }
        }
        
        debugLog("=== onPlayerAttack() END ===");
    }

    /**
     * Handles axe combo progression and finisher
     */
    private void handleAxeCombo(Player attacker, LivingEntity victim, EntityDamageByEntityEvent event, long now) {
        UUID attackerId = attacker.getUniqueId();
        debugLog("=== handleAxeCombo() START === Attacker: " + attacker.getName());
        
        Long lastComboTime = axeComboTimestamps.get(attackerId);
        debugLog("Last combo time: " + lastComboTime);
        
        if (lastComboTime != null && (now - lastComboTime) > AXE_COMBO_TIMEOUT_MS) {
            axeCombos.remove(attackerId);
            debugLog("Combo timed out - reset combo counter");
        }

        int currentCombo = axeCombos.getOrDefault(attackerId, 0) + 1;
        axeCombos.put(attackerId, currentCombo);
        axeComboTimestamps.put(attackerId, now);
        
        debugLog("Combo count: " + currentCombo + "/" + AXE_COMBO_MAX);

        if (currentCombo < AXE_COMBO_MAX) {
            attacker.sendActionBar("§6⚔ Axe Combo: §e" + currentCombo + "/" + AXE_COMBO_MAX);
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f + (currentCombo * 0.1f));
            debugLog("Combo in progress - played sound and sent message");
        } else {
            debugLog("COMBO FINISHER ACTIVATED");
            
            event.setDamage(0);
            victim.damage(AXE_SLAM_TRUE_DAMAGE);
            debugLog("Applied true damage: " + AXE_SLAM_TRUE_DAMAGE);
            
            Vector knockback = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector())
                    .normalize()
                    .multiply(2.0)
                    .setY(0.8);
            victim.setVelocity(knockback);
            debugLog("Applied finisher knockback: " + knockback);

            attacker.playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
            attacker.sendActionBar("§c§l AXE SLAM! §e(" + AXE_SLAM_TRUE_DAMAGE + " true damage)");
            
            axeCombos.remove(attackerId);
            debugLog("Combo finisher complete - reset combo");
        }
        
        debugLog("=== handleAxeCombo() END ===");
    }
 
    /**
     * Handles bow drawing for dual wielding mechanics
     */
    @EventHandler
    public void onBowDraw(PlayerInteractEvent event) {
        debugLog("=== onBowDraw() START ===");
        
        if (event.getHand() != EquipmentSlot.HAND) {
            debugLog("Not main hand interaction, returning");
            debugLog("=== onBowDraw() END ===");
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            debugLog("Not a right-click action, returning");
            debugLog("=== onBowDraw() END ===");
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        debugLog("Player: " + player.getName() + " (" + playerId + ")");

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        debugLog("Main hand: " + mainHand.getType() + ", Off hand: " + offHand.getType());

        if (mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW) {
            if (offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW) {
                long now = System.currentTimeMillis();
                bowDrawStarts.put(playerId, now);
                debugLog("Dual bow draw started at: " + now);
            }
        }
        
        debugLog("=== onBowDraw() END ===");
    }
 
    /**
     * Handles bow shooting for dual wielding mechanics
     */
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        debugLog("=== onBowShoot() START ===");
        
        if (!(event.getEntity() instanceof Player player)) {
            debugLog("Shooter is not a player, returning");
            debugLog("=== onBowShoot() END ===");
            return;
        }

        UUID playerId = player.getUniqueId();
        debugLog("Player: " + player.getName() + " (" + playerId + ")");

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        debugLog("Main hand: " + mainHand.getType() + ", Off hand: " + offHand.getType());

        boolean mainIsBow = mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW;
        boolean offIsBow = offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW;

        if (mainIsBow && offIsBow) {
            debugLog("Dual bow configuration detected");
            
            Long drawStart = bowDrawStarts.get(playerId);
            if (drawStart == null) {
                debugLog("No draw start time found, returning");
                debugLog("=== onBowShoot() END ===");
                return;
            }

            long now = System.currentTimeMillis();
            long drawTime = now - drawStart;
            debugLog("Draw time: " + drawTime + "ms (required: " + DUAL_BOW_CHARGE_TIME_MS + "ms)");

            if (drawTime >= DUAL_BOW_CHARGE_TIME_MS) {
                debugLog("Charge time sufficient - firing dual shot");
                
                Arrow mainArrow = (Arrow) event.getProjectile();
                debugLog("Main arrow: " + mainArrow);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Arrow offArrow = player.getWorld().spawnArrow(
                            player.getEyeLocation(),
                            player.getLocation().getDirection(),
                            (float) mainArrow.getVelocity().length(),
                            5.0f
                        );
                        
                        offArrow.setShooter(player);
                        offArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                        offArrow.setCritical(mainArrow.isCritical());
                        debugLog("Spawned offhand arrow with velocity: " + offArrow.getVelocity().length());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.3f);
                        player.sendActionBar("§b§lDUAL SHOT!");
                        debugLog("Dual shot effects played");
                    }
                }.runTaskLater(this, 1L);
            } else {
                debugLog("Insufficient charge time - single shot only");
            }

            bowDrawStarts.remove(playerId);
            debugLog("Removed draw start time");
        }
        
        debugLog("=== onBowShoot() END ===");
    }
 
    /**
     * Handles sword and shield parry mechanics
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        debugLog("=== onPlayerInteract() START ===");
        
        if (event.getHand() != EquipmentSlot.HAND) {
            debugLog("Not main hand, returning");
            debugLog("=== onPlayerInteract() END ===");
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        debugLog("Player: " + player.getName() + " (" + playerId + ")");

        Action action = event.getAction();
        debugLog("Action: " + action);

        long now = System.currentTimeMillis();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        debugLog("Main hand: " + mainHand.getType() + ", Off hand: " + offHand.getType());

        // ===========================
        // MACE GUARD ACTIVATION
        // ===========================
        if ((mainHand.getType() == Material.MACE || offHand.getType() == Material.MACE) &&
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            
            debugLog("Mace guard activation attempt");
            
            if (!player.isSneaking()) {
                debugLog("Player not sneaking, guard not activated");
            } else {
                long cooldown = maceGuardCooldowns.getOrDefault(playerId, 0L);
                debugLog("Guard cooldown expires at: " + cooldown);
                
                if (now < cooldown) {
                    long remaining = (cooldown - now) / 1000;
                    player.sendActionBar("§7Guard on cooldown (" + remaining + "s)");
                    debugLog("Guard on cooldown, " + remaining + "s remaining");
                } else {
                    long guardExpire = now + MACE_GUARD_WINDOW_MS;
                    maceGuardTimes.put(playerId, guardExpire);
                    maceGuardCooldowns.put(playerId, now + MACE_GUARD_COOLDOWN_MS);
                    
                    debugLog("Guard activated until: " + guardExpire);
                    debugLog("Guard cooldown set until: " + (now + MACE_GUARD_COOLDOWN_MS));
                    
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.2f);
                    startMaceGuardCountdown(player, playerId);
                    debugLog("Started guard countdown task");
                }
            }
        }

        // ===========================
        // SWORD PARRY
        // ===========================
        if (mainHand.getType().name().endsWith("_SWORD") && 
            (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            
            debugLog("Sword swing detected");
            lastSwingTimes.put(playerId, now);
            debugLog("Updated last swing time to: " + now);
            
            long lastBlock = lastBlockTimes.getOrDefault(playerId, 0L);
            long timeSinceBlock = now - lastBlock;
            
            debugLog("Time since last block: " + timeSinceBlock + "ms");

            boolean isBedrockPlayer = floodgateAvailable && FloodgateApi.getInstance().isFloodgatePlayer(playerId);
            long parryWindow = isBedrockPlayer ? SWORD_PARRY_WINDOW_BEDROCK_MS : SWORD_PARRY_WINDOW_MS;
            
            debugLog("Player is " + (isBedrockPlayer ? "Bedrock" : "Java") + ", parry window: " + parryWindow + "ms");

            if (timeSinceBlock <= parryWindow) {
                long parryCooldown = swordParryCooldowns.getOrDefault(playerId, 0L);
                debugLog("Parry cooldown expires at: " + parryCooldown);
                
                if (now >= parryCooldown) {
                    debugLog("SWORD PARRY SUCCESSFUL");
                    
                    riposteWindows.put(playerId, now + RIPOSTE_WINDOW_MS);
                    swordParryCooldowns.put(playerId, now + SWORD_PARRY_COOLDOWN_MS);
                    
                    debugLog("Riposte window set until: " + (now + RIPOSTE_WINDOW_MS));
                    debugLog("Parry cooldown set until: " + (now + SWORD_PARRY_COOLDOWN_MS));
                
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.8f);
                    player.sendActionBar("§a§l⚔ PARRY! §e(Riposte ready for 1.5s)");
                } else {
                    long remaining = (parryCooldown - now) / 1000;
                    player.sendActionBar("§7Parry on cooldown (" + remaining + "s)");
                    debugLog("Parry on cooldown, " + remaining + "s remaining");
                }
            }
        }

        // ===========================
        // SHIELD PARRY
        // ===========================
        if (player.isBlocking() && 
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            
            debugLog("Shield block detected");
            
            if (isShieldBroken(playerId)) {
                debugLog("Shield is broken, block not registered");
                debugLog("=== onPlayerInteract() END ===");
                return;
            }
            
            lastBlockTimes.put(playerId, now);
            debugLog("Updated last block time to: " + now);
            
            long lastSwing = lastSwingTimes.getOrDefault(playerId, 0L);
            long timeSinceSwing = now - lastSwing;
            
            debugLog("Time since last swing: " + timeSinceSwing + "ms");

            if (timeSinceSwing <= SHIELD_PARRY_WINDOW_MS) {
                long parryCooldown = shieldParryCooldowns.getOrDefault(playerId, 0L);
                debugLog("Shield parry cooldown expires at: " + parryCooldown);
                
                if (now >= parryCooldown) {
                    debugLog("SHIELD PARRY SUCCESSFUL");
                    
                    player.getNearbyEntities(3, 3, 3).stream()
                        .filter(entity -> entity instanceof LivingEntity)
                        .filter(entity -> entity != player)
                        .map(entity -> (LivingEntity) entity)
                        .forEach(target -> {
                            debugLog("Stunning target: " + target.getName());
                            target.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 
                                SHIELD_STUN_DURATION_TICKS, 
                                3
                            ));
                            target.addPotionEffect(new PotionEffect(
                                PotionEffectType.MINING_FATIGUE, 
                                SHIELD_STUN_DURATION_TICKS, 
                                2
                            ));
                            
                            if (target instanceof Player targetPlayer) {
                                targetPlayer.sendActionBar("§7§lSTUNNED!");
                                debugLog("Sent stun message to player target");
                            }
                        });

                    shieldParryCooldowns.put(playerId, now + SHIELD_PARRY_COOLDOWN_MS);
                    debugLog("Shield parry cooldown set until: " + (now + SHIELD_PARRY_COOLDOWN_MS));
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.7f);
                    player.sendActionBar("§b§l🛡 SHIELD PARRY! §e(Nearby enemies stunned)");
                } else {
                    long remaining = (parryCooldown - now) / 1000;
                    player.sendActionBar("§7Shield parry on cooldown (" + remaining + "s)");
                    debugLog("Shield parry on cooldown, " + remaining + "s remaining");
                }
            }
        }
        
        debugLog("=== onPlayerInteract() END ===");
    }
 
    /**
     * Handles dash activation via double-tap drop key
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        debugLog("=== onItemHeldChange() START ===");
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        debugLog("Player: " + player.getName() + " (" + playerId + ")");
        debugLog("Previous slot: " + event.getPreviousSlot() + ", New slot: " + event.getNewSlot());

        int prev = event.getPreviousSlot();
        int curr = event.getNewSlot();

        if ((prev == 8 && curr == 0) || (prev == 0 && curr == 8)) {
            debugLog("Detected hotbar wrap (0 <-> 8)");
            
            Boolean enabled = dashEnabled.get(playerId);
            long now = System.currentTimeMillis();
            
            debugLog("Dash enabled state: " + enabled + ", Current time: " + now);

            if (enabled == null) {
                dashEnabled.put(playerId, true);
                debugLog("First wrap detected, dash primed");
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (dashEnabled.get(playerId) != null && dashEnabled.get(playerId)) {
                            dashEnabled.put(playerId, false);
                            debugLog("Dash prime expired (timeout)");
                        }
                    }
                }.runTaskLater(this, 10L);
                
            } else if (enabled) {
                debugLog("Second wrap detected - attempting dash activation");
                
                long cooldown = dashCooldowns.getOrDefault(playerId, 0L);
                debugLog("Dash cooldown expires at: " + cooldown);
                
                if (now >= cooldown) {
                    executeDash(player, playerId, now);
                } else {
                    long remaining = (cooldown - now) / 1000;
                    player.sendActionBar("§7Dash on cooldown (" + remaining + "s)");
                    debugLog("Dash on cooldown, " + remaining + "s remaining");
                }
                
                dashEnabled.put(playerId, false);
                debugLog("Reset dash enabled state");
            }
        } else {
            debugLog("Normal slot change, no dash logic");
        }
        
        debugLog("=== onItemHeldChange() END ===");
    }
 
    /**
     * Executes the dash movement
     */
    private void executeDash(Player player, UUID id, long now) {
        debugLog("=== executeDash() START === Player: " + player.getName());
        
        // ===========================
        // DASH VELOCITY
        // ===========================
        Vector direction = player.getLocation().getDirection().normalize();
        debugLog("Dash direction (normalized): " + direction);
        
        direction.multiply(DASH_VELOCITY_MULTIPLIER).setY(DASH_VERTICAL_BOOST);
        debugLog("Final dash velocity: " + direction);
        
        player.setVelocity(direction);
        debugLog("Applied velocity to player");
 
        // ===========================
        // INVULNERABILITY
        // ===========================
        invulnerablePlayers.put(id, now + DASH_INVULN_DURATION_MS);
        debugLog("Set invulnerability until: " + (now + DASH_INVULN_DURATION_MS));
 
        // ===========================
        // EFFECTS
        // ===========================
        CombatFX.playDashEffects(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
        player.sendActionBar("§b DASH! (§6-Energy§b)");
        debugLog("Played dash effects and sounds");
 
        // ===========================
        // SET COOLDOWN
        // ===========================
        dashCooldowns.put(id, now + DASH_COOLDOWN_MS);
        debugLog("Set dash cooldown until: " + (now + DASH_COOLDOWN_MS));
        debugLog("=== executeDash() END ===");
    }
 
    // ===============================================
    // ADRENALINE RUSH
    // ===============================================
 
   @EventHandler(priority = EventPriority.HIGH)
    public void onAnyDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        debugLog("=== onAnyDamage() START ===");
        
        if (event.isCancelled()) {
            debugLog("Event cancelled, returning");
            debugLog("=== onAnyDamage() END ===");
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            debugLog("Entity is not a player, returning");
            debugLog("=== onAnyDamage() END ===");
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            debugLog("Entity damage by entity event, handled separately");
            debugLog("=== onAnyDamage() END ===");
            return;
        }

        UUID victimId = victim.getUniqueId();
        debugLog("Victim: " + victim.getName() + " (" + victimId + ")");
        debugLog("Damage: " + event.getDamage() + ", Final damage: " + event.getFinalDamage());

        Long guardExpire = maceGuardTimes.get(victimId);
        long now = System.currentTimeMillis();
        
        if (guardExpire != null && now <= guardExpire) {
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * MACE_GUARD_DAMAGE_MULTIPLIER;
            event.setDamage(reducedDamage);
            debugLog("Mace guard active - damage reduced from " + originalDamage + " to " + reducedDamage);
            
            final Player guardedVictim = victim;
            new BukkitRunnable() {
                @Override 
                public void run() { 
                    guardedVictim.setVelocity(new Vector(0, 0, 0));
                    debugLog("Nullified knockback for guarded player");
                }
            }.runTaskLater(DCM.this, 1L);
        }
 
        // Calculate post-damage health
        double finalHealth = victim.getHealth() - event.getFinalDamage();
        debugLog("Current health: " + victim.getHealth() + ", Final health after damage: " + finalHealth);
 
        if (finalHealth > 0 && finalHealth <= ADRENALINE_HEALTH_THRESHOLD) {
            debugLog("Health below adrenaline threshold (" + ADRENALINE_HEALTH_THRESHOLD + ")");
            
            long cd = adrenalineCooldowns.getOrDefault(victimId, 0L);
            debugLog("Adrenaline cooldown expires at: " + cd);
            
            if (now >= cd) {
                triggerAdrenaline(victim, now);
            } else {
                long remaining = (cd - now) / 1000;
                debugLog("Adrenaline on cooldown, " + remaining + "s remaining");
            }
        }
        
        debugLog("=== onAnyDamage() END ===");
    }

    private void triggerAdrenaline(Player victim, long now) {
        debugLog("=== triggerAdrenaline() START === Player: " + victim.getName());
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ADRENALINE_DURATION_TICKS, 1));
        debugLog("Applied Speed II for " + ADRENALINE_DURATION_TICKS + " ticks");
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ADRENALINE_DURATION_TICKS, 1));
        debugLog("Applied Resistance II for " + ADRENALINE_DURATION_TICKS + " ticks");
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ADRENALINE_DURATION_TICKS, 0));
        debugLog("Applied Strength I for " + ADRENALINE_DURATION_TICKS + " ticks");
 
        CombatFX.playAdrenalineEffects(victim);
        victim.sendActionBar("§c§lADRENALINE RUSH!");
        debugLog("Played adrenaline effects");
 
        adrenalineCooldowns.put(victim.getUniqueId(), now + ADRENALINE_COOLDOWN_MS);
        debugLog("Set adrenaline cooldown until: " + (now + ADRENALINE_COOLDOWN_MS));
        debugLog("=== triggerAdrenaline() END ===");
    }
    
 
    // ===============================================
    // SHIELD BREAKING SYSTEM
    // ===============================================
 
    /**
     * Breaks a player's shield for a specified duration
     * @param playerId The UUID of the player whose shield should be broken
     * @param durationMs Duration in milliseconds
     */
    private void breakShield(UUID playerId, long durationMs) {
        debugLog("=== breakShield() START === Player ID: " + playerId + ", Duration: " + durationMs + "ms");
        
        long expireTime = System.currentTimeMillis() + durationMs;
        brokenShields.put(playerId, expireTime);
        debugLog("Shield will be broken until: " + expireTime);

        Player player = getServer().getPlayer(playerId);
        if (player != null) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            player.sendActionBar("§c§l🛡 SHIELD BROKEN! (" + String.format("%.1f", durationMs / 1000.0) + "s)");
            
            int cooldownTicks = (int) (durationMs / 50);
            player.setCooldown(Material.SHIELD, cooldownTicks);
            debugLog("Set shield material cooldown for " + cooldownTicks + " ticks");
            
            forceStopShieldUse(player);
        } else {
            debugLog("Player not online, cannot apply shield break effects");
        }
        
        debugLog("=== breakShield() END ===");
    }

    private void forceStopShieldUse(Player player) {
        UUID playerId = player.getUniqueId();
        debugLog("=== forceStopShieldUse() START === Player: " + player.getName());
        
        player.clearActiveItem();
        debugLog("Cleared active item");

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                debugLog("Shield stop enforcement attempt: " + attempts);
                
                if (!player.isOnline() || !isShieldBroken(playerId) || !player.isBlocking() || attempts++ >= 8) {
                    debugLog("Shield enforcement ended: online=" + player.isOnline() + 
                            ", broken=" + isShieldBroken(playerId) + 
                            ", blocking=" + player.isBlocking() + 
                            ", attempts=" + attempts);
                    cancel();
                    return;
                }

                player.clearActiveItem();
                debugLog("Cleared active item (enforcement)");
            }
        }.runTaskTimer(this, 1L, 1L);
        
        debugLog("=== forceStopShieldUse() END ===");
    }
 
    /**
     * Checks if a player's shield is currently broken
     * @param playerId The UUID of the player to check
     * @return true if the shield is broken, false otherwise
     */
    private boolean isShieldBroken(UUID playerId) {
        debugLog("=== isShieldBroken() START === Player ID: " + playerId);
        
        if (!brokenShields.containsKey(playerId)) {
            debugLog("No broken shield entry found, returning false");
            debugLog("=== isShieldBroken() END ===");
            return false;
        }
 
        long expireTime = brokenShields.get(playerId);
        long now = System.currentTimeMillis();
        debugLog("Shield break expires at: " + expireTime + ", Current time: " + now);
        
        if (now >= expireTime) {
            brokenShields.remove(playerId);
            debugLog("Shield break expired, removed entry, returning false");
            debugLog("=== isShieldBroken() END ===");
            return false;
        }
        
        debugLog("Shield is still broken, returning true");
        debugLog("=== isShieldBroken() END ===");
        return true;
    }
 
    // ========================================================
    // MACEGUARD COOLDOWN
    // ========================================================
    private void startMaceGuardCountdown(Player player, UUID playerId) {
        debugLog("=== startMaceGuardCountdown() START === Player: " + player.getName());
        
        BukkitRunnable existingTask = maceGuardCountdownTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
            debugLog("Cancelled existing guard countdown task");
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    debugLog("Player offline, cancelling guard countdown");
                    maceGuardTimes.remove(playerId);
                    maceGuardCountdownTasks.remove(playerId);
                    cancel();
                    return;
                }

                long now = System.currentTimeMillis();
                Long expireAt = maceGuardTimes.get(playerId);

                if (expireAt == null || now >= expireAt) {
                    if (expireAt != null) {
                        player.sendActionBar("§6Standing Guard! §e(0.0s remaining)");
                        debugLog("Guard expired naturally");
                    }
                    maceGuardTimes.remove(playerId);
                    maceGuardCountdownTasks.remove(playerId);
                    cancel();
                    return;
                }

                Material mainHand = player.getInventory().getItemInMainHand().getType();
                Material offHand = player.getInventory().getItemInOffHand().getType();
                if (mainHand != Material.MACE && offHand != Material.MACE) {
                    debugLog("Mace no longer equipped, cancelling guard");
                    maceGuardTimes.remove(playerId);
                    maceGuardCountdownTasks.remove(playerId);
                    player.sendActionBar("§7Guard cancelled.");
                    cancel();
                    return;
                }

                double secondsLeft = (expireAt - now) / 1000.0;
                player.sendActionBar("§6Standing Guard! §e(" + String.format("%.1f", secondsLeft) + "s remaining)");
            }
        };

        maceGuardCountdownTasks.put(playerId, task);
        task.runTaskTimer(this, 0L, 2L);
        debugLog("Started new guard countdown task");
        debugLog("=== startMaceGuardCountdown() END ===");
    }

    // ===============================================
    // UTILITY METHODS
    // ===============================================
 
    /**
     * Gets the base damage value for a weapon material
     * @param material The weapon material
     * @return Base damage value
     */
    private double getBaseDamage(Material material) {
        debugLog("=== getBaseDamage() START === Material: " + material);
        
        String name = material.name();
        double damage = 1.0;
        
        if (name.endsWith("_SWORD")) {
            if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) damage = 4.0;
            else if (name.startsWith("STONE")) damage = 5.0;
            else if (name.startsWith("IRON")) damage = 6.0;
            else if (name.startsWith("DIAMOND")) damage = 7.0;
            else if (name.startsWith("NETHERITE")) damage = 8.0;
        }
        else if (name.endsWith("_AXE")) {
            if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) damage = 7.0;
            else if (name.startsWith("STONE") || name.startsWith("IRON") || name.startsWith("DIAMOND")) damage = 9.0;
            else if (name.startsWith("NETHERITE")) damage = 10.0;
        }
        
        debugLog("Calculated base damage: " + damage);
        debugLog("=== getBaseDamage() END ===");
        return damage;
    }
}