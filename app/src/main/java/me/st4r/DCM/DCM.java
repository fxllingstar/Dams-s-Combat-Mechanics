package me.st4r.DCM;
 
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.geysermc.floodgate.api.FloodgateApi;
 
import java.util.HashMap;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
 
/**
 * DCM - Dams's Combat Mechanics
 * A comprehensive combat overhaul plugin combining dual-wielding, parry mechanics,
 * combo systems, and mobility enhancements.
 * 
 * @author st4r
 * @version 2.0.6
 */
public class DCM extends JavaPlugin implements Listener {
 
    private boolean debug = false;
 
    // ===========================
    // WEAPON CONFIGURATION
    // ===========================
    private static final long DUAL_MELEE_COOLDOWN_MS = 3000;
    private static final long DUAL_BOW_CHARGE_TIME_MS = 1000;
    private static final int SHIELD_BREAK_THRESHOLD = 4;
    private static final long SHIELD_BREAK_DURATION_MS = 5000;
    private static final long SHIELD_STREAK_TIMEOUT_MS = 4000;
    private static final long MACE_GUARD_WINDOW_MS = 1500;
    private static final double MACE_GUARD_DAMAGE_MULTIPLIER = 0.6;
    private static final long MACE_GUARD_COOLDOWN_MS = 5000;
    private static final double DUAL_WIELD_OFFHAND_DAMAGE_SCALE = 0.05;
    private static final double MELEE_EXHAUSTION_DAMAGE_MULTIPLIER = 0.4;

 
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
    private static final double AXE_SLAM_BONUS_DAMAGE = 6.0; 
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
    private static final long ADRENALINE_COOLDOWN_MS = 180000; 
    private static final int ADRENALINE_DURATION_TICKS = 200; 
    private static final double ADRENALINE_HEALTH_THRESHOLD = 8.0; 
 
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
    private final Map<UUID, Long> parryInputCooldowns = new HashMap<>();
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
    private final Map<UUID, Boolean> dashToggleStates = new HashMap<>();
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> invulnerablePlayers = new HashMap<>();

    // ===========================
    // UI STATE
    // ===========================
    private final Map<UUID, BukkitRunnable> blockingIndicatorTasks = new HashMap<>();
 
    // ===========================
    // ADRENALINE STATE
    // ===========================
    private final Map<UUID, Long> adrenalineCooldowns = new HashMap<>();

    // ===========================
    // OPTIONAL DEPENDENCY FLAGS
    // ===========================
    private boolean floodgateAvailable = false;
    private boolean dvplus = false;
    
    // ===========================
    // STAMINA MANAGER
    // ===========================
    private StaminaManager staminaManager;
   

    // ===========================
    // DEBUG HELPER METHOD
    // ===========================


    private void sendActionBar(Player player, ChatColor color, ChatColor style, String message) {
        player.sendActionBar(color + "" + style + message);
    }

    private boolean spendStamina(Player player, double amount, String failMessage) {
        if (!staminaManager.trySpend(player, amount)) {
            player.sendActionBar(ChatColor.RED + failMessage);
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateAvailable = true;
        } else {
            getLogger().warning("Floodgate not found! Bedrock player support is disabled. Install Floodgate + Geyser for Bedrock compatibility.");
        }

        //This is for future integrations. DO NOT REMOVE!!!
        if (getServer().getPluginManager().getPlugin("DVPlus") != null){
        dvplus = true;
        getLogger().info("Dams's Vanilla+ Detected!");
        } else {
            getLogger().warning("Dams's Vanilla+ Not found.");
        }


        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("dash") != null) {
            getCommand("dash").setExecutor(this);
    
        } else {
            getLogger().warning("Dash command is missing from plugin.yml");
        }
        
        startMemoryCleanupTask();

       
        staminaManager = new StaminaManager(this);
        StaminaListener staminaListener = new StaminaListener(staminaManager);
        getServer().getPluginManager().registerEvents(staminaListener, this);

        getLogger().info("-----------------------------------------------------------------------");
        getLogger().info("DCM (Dams's Combat Mechanics) v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("DEBUG MODE: " + (debug ? "ENABLED" : "DISABLED"));
        getLogger().info("Have fun and Good Luck!");
        getLogger().info("-----------------------------------------------------------------------");
        


    new BukkitRunnable() {
        @Override
        public void run(){
            for (Player p : Bukkit.getOnlinePlayers()){
                staminaManager.regenTick(p, 3.0);
            }
        }
    }.runTaskTimer(this, 0L, 10L);
    }
 
    @Override
    public void onDisable() {
        
        for (BukkitRunnable task : maceGuardCountdownTasks.values()) {
            task.cancel();
        }
        maceGuardCountdownTasks.clear();

        for (BukkitRunnable task : blockingIndicatorTasks.values()) {
            task.cancel();
        }
        blockingIndicatorTasks.clear();

        getLogger().severe("Oh.. server is dead?");
        getLogger().info("DCM has been disabled! :>");
        getLogger().info("0x6B696E646E657373 <3");
    }
 
    /**
     * Periodic cleanup task to prevent memory leaks from stale entries
     */
    private void startMemoryCleanupTask() {
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long staleTime = now - 10000;
                int cleanedCount = 0;
 
                // Clean up timing maps
                int beforeSize = lastSwingTimes.size();
                lastSwingTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                int removed = beforeSize - lastSwingTimes.size();
                cleanedCount += removed;

                beforeSize = lastBlockTimes.size();
                lastBlockTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                removed = beforeSize - lastBlockTimes.size();
                cleanedCount += removed;

                beforeSize = parryInputCooldowns.size();
                parryInputCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - parryInputCooldowns.size();
                cleanedCount += removed;

                beforeSize = shieldStreakTimestamps.size();
                shieldStreakTimestamps.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                removed = beforeSize - shieldStreakTimestamps.size();
                cleanedCount += removed;

                beforeSize = lastExhaustionMsgTimes.size();
                lastExhaustionMsgTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                removed = beforeSize - lastExhaustionMsgTimes.size();
                cleanedCount += removed;

                beforeSize = riposteWindows.size();
                riposteWindows.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - riposteWindows.size();
                cleanedCount += removed;

           
                beforeSize = brokenShields.size();
                brokenShields.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - brokenShields.size();
                cleanedCount += removed;

                beforeSize = maceGuardTimes.size();
                maceGuardTimes.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - maceGuardTimes.size();
                cleanedCount += removed;

                beforeSize = maceGuardCooldowns.size();
                maceGuardCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - maceGuardCooldowns.size();
                cleanedCount += removed;

             
                beforeSize = invulnerablePlayers.size();
                invulnerablePlayers.entrySet().removeIf(entry -> entry.getValue() < now);
                removed = beforeSize - invulnerablePlayers.size();
                cleanedCount += removed;

             
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
            }
        }.runTaskTimer(this, 200L, 200L);
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
        
        BukkitRunnable task = maceGuardCountdownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
 
        meleeCooldowns.remove(playerId);
        bowDrawStarts.remove(playerId);
        shieldHitStreak.remove(playerId);
        lastTargets.remove(playerId);
        shieldStreakTimestamps.remove(playerId);
        lastExhaustionMsgTimes.remove(playerId);
        lastSwingTimes.remove(playerId);
        lastBlockTimes.remove(playerId);
        parryInputCooldowns.remove(playerId);
        swordParryCooldowns.remove(playerId);
        shieldParryCooldowns.remove(playerId);
        brokenShields.remove(playerId);
        maceGuardTimes.remove(playerId);
        maceGuardCooldowns.remove(playerId);
        riposteWindows.remove(playerId);
        axeCombos.remove(playerId);
        axeComboTimestamps.remove(playerId);
        dashToggleStates.remove(playerId);
        dashCooldowns.remove(playerId);
        invulnerablePlayers.remove(playerId);
        adrenalineCooldowns.remove(playerId);
        stopBlockingIndicator(playerId);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("dash")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /dash.");
            return true;
        }

        UUID playerId = player.getUniqueId();
        boolean enabled = dashToggleStates.getOrDefault(playerId, true);
        boolean nextState = !enabled;
        dashToggleStates.put(playerId, nextState);

        if (nextState) {
            player.sendActionBar(ChatColor.AQUA + "" + ChatColor.BOLD + "Dash enabled");
            player.sendMessage(ChatColor.GREEN + "Dash has been enabled.");
        } else {
            player.sendActionBar(ChatColor.GRAY + "" + ChatColor.BOLD + "Dash disabled");
            player.sendMessage(ChatColor.RED + "Dash has been disabled.");
        }

        return true;
    }
 
    /**
     * Handles melee attacks for dual wielding and parry mechanics
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        
        if (event.isCancelled()) {
            return;
        }
        
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        UUID attackerId = attacker.getUniqueId();

        long now = System.currentTimeMillis();

        UUID victimId = null;

        Long invulnExpire = invulnerablePlayers.get(attackerId);
        if (invulnExpire != null && now <= invulnExpire) {
        }

        // ===========================
        // VICTIM INVULNERABILITY CHECK
        // ===========================
        if (victim instanceof Player victimPlayer) {
            victimId = victimPlayer.getUniqueId();
            
            Long victimInvulnExpire = invulnerablePlayers.get(victimId);
            if (victimInvulnExpire != null && now <= victimInvulnExpire) {
                event.setCancelled(true);
                return;
            }
            
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        Material weaponType = weapon.getType();


// ===========================
// SWORD PARRY CHECK
// ===========================
if (victim instanceof Player victimPlayer) {
    ItemStack victimMainHand = victimPlayer.getInventory().getItemInMainHand();

    if (victimMainHand.getType().name().endsWith("_SWORD")) {
        long lastBlock = lastBlockTimes.getOrDefault(victimId, 0L);
        long timeSinceBlock = now - lastBlock;

        boolean isBedrockPlayer = floodgateAvailable && FloodgateApi.getInstance().isFloodgatePlayer(victimId);
        long parryWindow = isBedrockPlayer ? SWORD_PARRY_WINDOW_BEDROCK_MS : SWORD_PARRY_WINDOW_MS;

        if (timeSinceBlock <= parryWindow) {
            long parryCooldown = swordParryCooldowns.getOrDefault(victimId, 0L);
            if (now >= parryCooldown) {
                event.setCancelled(true); // negate the hit
                riposteWindows.put(victimId, now + RIPOSTE_WINDOW_MS);
                swordParryCooldowns.put(victimId, now + SWORD_PARRY_COOLDOWN_MS);
                victimPlayer.playSound(victimPlayer.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.8f);
                victimPlayer.sendActionBar(ChatColor.GREEN + "" + ChatColor.BOLD + "PARRY! " + ChatColor.YELLOW + "(Riposte ready for 1.5s)");
            } else {
                long remaining = (parryCooldown - now) / 1000;
                victimPlayer.sendActionBar(ChatColor.GRAY + "Parry on cooldown (" + remaining + "s)");
            }
        }
    }
}

        // ===========================
        // RIPOSTE DAMAGE MULTIPLIER
        // ===========================
        Long riposteExpire = riposteWindows.get(attackerId);
        if (riposteExpire != null && now <= riposteExpire) {
            if (spendStamina(attacker, 20, "Not enough stamina for riposte!")) {
                double originalDamage = event.getDamage();
                double riposteDamage = originalDamage * RIPOSTE_DAMAGE_MULTIPLIER;
                event.setDamage(riposteDamage);
                
                Vector direction = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
                direction.multiply(RIPOSTE_KNOCKBACK_HORIZONTAL).setY(RIPOSTE_KNOCKBACK_VERTICAL);
                victim.setVelocity(direction);
                
                attacker.playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f);
                sendActionBar(attacker, ChatColor.RED, ChatColor.BOLD, "RIPOSTE! (" + String.format("%.1f", riposteDamage) + " damage)");
                
                riposteWindows.remove(attackerId);
            }
        }

        Long guardExpire = null;
        if (victimId != null) {
            guardExpire = maceGuardTimes.get(victimId);
        }
        if (guardExpire != null && now <= guardExpire) {
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * MACE_GUARD_DAMAGE_MULTIPLIER;
            event.setDamage(reducedDamage);
        }

        // ===========================
        // AXE COMBO SYSTEM
        // ===========================
        if (weaponType.name().endsWith("_AXE")) {
            handleAxeCombo(attacker, victim, event, now);
        }

        // ===========================
        // DUAL WIELDING MELEE
        // ===========================
        ItemStack offhand = attacker.getInventory().getItemInOffHand();
        Material offhandType = offhand.getType();

        boolean isMainhandWeapon = weaponType.name().endsWith("_SWORD") || 
                                   weaponType.name().endsWith("_AXE") || 
                                   weaponType == Material.MACE;
        boolean isOffhandWeapon = offhandType.name().endsWith("_SWORD") || 
                                  offhandType.name().endsWith("_AXE") || 
                                  offhandType == Material.MACE;

        long cooldown = meleeCooldowns.getOrDefault(attackerId, 0L);
        boolean meleeExhausted = now < cooldown;
        if (meleeExhausted && isMainhandWeapon) {
            long remaining = (cooldown - now) / 1000;
            double originalDamage = event.getDamage();
            double exhaustedDamage = originalDamage * MELEE_EXHAUSTION_DAMAGE_MULTIPLIER;
            event.setDamage(exhaustedDamage);

            long lastMsgTime = lastExhaustionMsgTimes.getOrDefault(attackerId, 0L);
            if (now - lastMsgTime >= 1000) {
                sendActionBar(attacker, ChatColor.GRAY, ChatColor.BOLD, "Exhausted... (wait " + remaining + "s)");
                lastExhaustionMsgTimes.put(attackerId, now);
            }
        }

        if (isMainhandWeapon && isOffhandWeapon) {

            if (!meleeExhausted) {
                if (spendStamina(attacker, 10, "Not enough stamina for dual strike!")) {
                    double baseDamage = event.getDamage();
                    double offhandDamage = getWeaponAttackDamage(offhand);
                    double dualWieldMultiplier = 1.0 + (offhandDamage * DUAL_WIELD_OFFHAND_DAMAGE_SCALE);
                    double totalDamage = baseDamage * dualWieldMultiplier;

                    event.setDamage(totalDamage);

                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                    sendActionBar(attacker, ChatColor.GOLD, ChatColor.BOLD, "DUAL STRIKE! (" + String.format("%.1f", totalDamage) + " damage)");

                    meleeCooldowns.put(attackerId, now + DUAL_MELEE_COOLDOWN_MS);
                }
            }
        }

        // ===========================
        // SHIELD HIT STREAK TRACKING
        // ===========================
        if (victim instanceof Player victimPlayer) {
            if (victimPlayer.isBlocking() && !isShieldBroken(victimId)) {
                
                UUID lastTarget = lastTargets.get(attackerId);
                Long lastStreakTime = shieldStreakTimestamps.get(attackerId);

                if (lastTarget == null || !lastTarget.equals(victimId) || 
                    lastStreakTime == null || (now - lastStreakTime) > SHIELD_STREAK_TIMEOUT_MS) {
                    shieldHitStreak.put(attackerId, 1);
                } else {
                    int streak = shieldHitStreak.getOrDefault(attackerId, 0) + 1;
                    shieldHitStreak.put(attackerId, streak);

                    if (streak >= SHIELD_BREAK_THRESHOLD) {
                        breakShield(victimId, SHIELD_BREAK_DURATION_MS);
                        sendActionBar(attacker, ChatColor.RED, ChatColor.BOLD, "SHIELD SHATTERED!");
                        shieldHitStreak.remove(attackerId);
                    }
                }

                lastTargets.put(attackerId, victimId);
                shieldStreakTimestamps.put(attackerId, now);
            } else if (isShieldBroken(victimId)) {
                shieldHitStreak.remove(attackerId);
            }
        }
    }


    private void handleAxeCombo(Player attacker, LivingEntity victim, EntityDamageByEntityEvent event, long now) {
        UUID attackerId = attacker.getUniqueId();
        
        Long lastComboTime = axeComboTimestamps.get(attackerId);
        
        if (lastComboTime != null && (now - lastComboTime) > AXE_COMBO_TIMEOUT_MS) {
            axeCombos.remove(attackerId);
        }

        int currentCombo = axeCombos.getOrDefault(attackerId, 0) + 1;
        double staminaCost = currentCombo < AXE_COMBO_MAX ? 5 : 15;

        if (!spendStamina(attacker, staminaCost, "Not enough stamina for axe combo!")) {
            return;
        }

        axeCombos.put(attackerId, currentCombo);
        axeComboTimestamps.put(attackerId, now);

        if (currentCombo < AXE_COMBO_MAX) {
            attacker.sendActionBar(ChatColor.GOLD + "" + ChatColor.BOLD + "Axe Combo: " + ChatColor.YELLOW + currentCombo + "/" + AXE_COMBO_MAX);
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f + (currentCombo * 0.1f));
        } else {
            
            double baseDamage = event.getDamage();
            double slamDamage = baseDamage + AXE_SLAM_BONUS_DAMAGE;
            event.setDamage(slamDamage);
            
            Vector knockback = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector())
                    .normalize()
                    .multiply(2.0)
                    .setY(0.8);
            victim.setVelocity(knockback);

            attacker.playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
            attacker.sendActionBar(ChatColor.RED + "" + ChatColor.BOLD + "AXE SLAM! " + ChatColor.YELLOW + "(+" + AXE_SLAM_BONUS_DAMAGE + " bonus damage)");
            
            axeCombos.remove(attackerId);
        }
    }
 
 
    @EventHandler
    public void onBowDraw(PlayerInteractEvent event) {
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW) {
            if (offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW) {
                long now = System.currentTimeMillis();
                bowDrawStarts.put(playerId, now);
            }
        }
    }
 
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean mainIsBow = mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW;
        boolean offIsBow = offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW;

        if (mainIsBow && offIsBow) {
            
            Long drawStart = bowDrawStarts.get(playerId);
            if (drawStart == null) {
                return;
            }

            long now = System.currentTimeMillis();
            long drawTime = now - drawStart;

            if (drawTime >= DUAL_BOW_CHARGE_TIME_MS) {
                if (!spendStamina(player, 10, "Not enough stamina for dual shot!")) {
                    bowDrawStarts.remove(playerId);
                    return;
                }

                if (!(event.getProjectile() instanceof AbstractArrow mainArrow)) {
                } else {

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
                            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.3f);
                            sendActionBar(player, ChatColor.AQUA, ChatColor.BOLD, "DUAL SHOT!");
                        }
                    }.runTaskLater(this, 1L);
                }
            } else {
            }

            bowDrawStarts.remove(playerId);
        }
    }
 
   
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        long now = System.currentTimeMillis();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // ===========================
        // MACE GUARD ACTIVATION
        // ===========================
        if ((mainHand.getType() == Material.MACE || offHand.getType() == Material.MACE) &&
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            
            if (!player.isSneaking()) {
            } else {
                long cooldown = maceGuardCooldowns.getOrDefault(playerId, 0L);
                
                if (now < cooldown) {
                    long remaining = (cooldown - now) / 1000;
                    player.sendActionBar(ChatColor.GRAY + "Guard on cooldown (" + remaining + "s)");
                } else {
                    if (spendStamina(player, 10, "Not enough stamina for mace guard!")) {
                        long guardExpire = now + MACE_GUARD_WINDOW_MS;
                        maceGuardTimes.put(playerId, guardExpire);
                        maceGuardCooldowns.put(playerId, now + MACE_GUARD_COOLDOWN_MS);
                        
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.2f);
                        startMaceGuardCountdown(player, playerId);
                    }
                }
            }
        }

        // ===========================
        // SWORD PARRY
        // ===========================
        if (mainHand.getType().name().endsWith("_SWORD") && rightClick) {

            long parryCooldown = parryInputCooldowns.getOrDefault(playerId, 0L);

            if (now >= parryCooldown) {
                lastBlockTimes.put(playerId, now);
                parryInputCooldowns.put(playerId, now + SWORD_PARRY_COOLDOWN_MS);
                player.sendActionBar(ChatColor.AQUA + "" + ChatColor.BOLD + "PARRY!");
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.6f);
            } else {
                long remaining = (parryCooldown - now) / 1000;
                player.sendActionBar(ChatColor.GRAY + "Parry on cooldown (" + remaining + "s)");
            }
        }

        // ===========================
        // SHIELD PARRY
        // ===========================
        if (player.getInventory().getItemInMainHand().getType() == Material.SHIELD ||
            player.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
            if (!rightClick) {
            } else {
                long inputCooldown = parryInputCooldowns.getOrDefault(playerId, 0L);

                if (now < inputCooldown) {
                    long remaining = (inputCooldown - now) / 1000;
                    player.sendActionBar(ChatColor.GRAY + "Parry on cooldown (" + remaining + "s)");
                    return;
                }
                if (!spendStamina(player, 15, "Not enough stamina for shield parry!")) {
                    return;
                }

                parryInputCooldowns.put(playerId, now + SHIELD_PARRY_COOLDOWN_MS);
                player.sendActionBar(ChatColor.AQUA + "" + ChatColor.BOLD + "PARRY!");
            
                if (isShieldBroken(playerId)) {
                    stopBlockingIndicator(playerId);
                    return;
                }
            
                lastBlockTimes.put(playerId, now);
                startBlockingIndicator(player);
            
                long lastSwing = lastSwingTimes.getOrDefault(playerId, 0L);
                long timeSinceSwing = now - lastSwing;

                if (timeSinceSwing <= SHIELD_PARRY_WINDOW_MS) {
                    long parryCooldown = shieldParryCooldowns.getOrDefault(playerId, 0L);
                
                    if (now >= parryCooldown) {
                    
                        player.getNearbyEntities(3, 3, 3).stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .filter(entity -> entity != player)
                            .map(entity -> (LivingEntity) entity)
                            .forEach(target -> {
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
                                    targetPlayer.sendActionBar(ChatColor.GRAY + "" + ChatColor.BOLD + "STUNNED!");
                                }
                            });

                        shieldParryCooldowns.put(playerId, now + SHIELD_PARRY_COOLDOWN_MS);
                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
                        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.7f);
                        player.sendActionBar(ChatColor.AQUA + "" + ChatColor.BOLD + "SHIELD PARRY! " + ChatColor.YELLOW + "(Nearby enemies stunned)");
                    } else {
                        long remaining = (parryCooldown - now) / 1000;
                        player.sendActionBar(ChatColor.GRAY + "Shield parry on cooldown (" + remaining + "s)");
                    }
                }
            }
        }
    }
 

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!dashToggleStates.getOrDefault(playerId, true)) {
            player.sendActionBar(ChatColor.GRAY + "Dash is disabled. Use /dash to re-enable it.");
            return;
        }

        if (!player.isSprinting()) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = dashCooldowns.getOrDefault(playerId, 0L);

        if (now >= cooldown) {
            executeDash(player, playerId, now);
        } else {
            long remaining = (cooldown - now) / 1000;
            player.sendActionBar(ChatColor.GRAY + "Dash on cooldown (" + remaining + "s)");
        }
    }
 
    private void executeDash(Player player, UUID id, long now) {
        
        // ===========================
        // DASH VELOCITY
        // ===========================
        Vector direction = player.getLocation().getDirection().normalize();
        
        direction.multiply(DASH_VELOCITY_MULTIPLIER).setY(DASH_VERTICAL_BOOST);
        
        player.setVelocity(direction);
        invulnerablePlayers.put(id, now + DASH_INVULN_DURATION_MS);
 
        // ===========================
        // EFFECTS
        // ===========================
        CombatFX.playDashEffects(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
        player.sendActionBar(ChatColor.AQUA + "DASH! (" + ChatColor.GOLD + "-Energy" + ChatColor.AQUA + ")");
 
        // ===========================
        // SET COOLDOWN
        // ===========================
        dashCooldowns.put(id, now + DASH_COOLDOWN_MS);
    }

    private void startBlockingIndicator(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable existing = blockingIndicatorTasks.remove(playerId);
        if (existing != null) {
            existing.cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isBlocking() || isShieldBroken(playerId)) {
                    stopBlockingIndicator(playerId);
                    cancel();
                    return;
                }

                player.sendActionBar(ChatColor.BLUE + "" + ChatColor.BOLD + "BLOCKING");
            }
        };

        blockingIndicatorTasks.put(playerId, task);
        task.runTaskTimer(this, 0L, 10L);
        player.sendActionBar(ChatColor.BLUE + "" + ChatColor.BOLD + "BLOCKING");
    }

    private void stopBlockingIndicator(UUID playerId) {
        BukkitRunnable task = blockingIndicatorTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
 
    // ===============================================
    // ADRENALINE RUSH
    // ===============================================
 
   @EventHandler(priority = EventPriority.HIGH)
    public void onAnyDamage(EntityDamageEvent event) {
        
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        UUID victimId = victim.getUniqueId();

        long now = System.currentTimeMillis();

        handleVictimDamageReactions(victim, event, now);
    }

    private void handleVictimDamageReactions(Player victim, EntityDamageEvent event, long now) {
        UUID victimId = victim.getUniqueId();
        handleAdrenalineIfNeeded(victim, now, event.getFinalDamage());
    }
    
    @SuppressWarnings("removal")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVictimKnockback(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID victimId = victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long guardExpire = maceGuardTimes.get(victimId);
        if (guardExpire == null || now > guardExpire) {
            return;
        }

        event.setCancelled(true);
    }

    private void handleAdrenalineIfNeeded(Player victim, long now, double incomingDamage) {
        UUID victimId = victim.getUniqueId();

    
        double finalHealth = victim.getHealth() - incomingDamage;

        if (finalHealth > 0 && finalHealth <= ADRENALINE_HEALTH_THRESHOLD) {

            long cd = adrenalineCooldowns.getOrDefault(victimId, 0L);

            if (now >= cd) {
                triggerAdrenaline(victim, now);
            } else {
                long remaining = (cd - now) / 1000;
            }
        }
    }

    private void triggerAdrenaline(Player victim, long now) {
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ADRENALINE_DURATION_TICKS, 1));
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ADRENALINE_DURATION_TICKS, 1));
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ADRENALINE_DURATION_TICKS, 0));
 
        CombatFX.playAdrenalineEffects(victim);
        victim.sendActionBar(ChatColor.RED + "" + ChatColor.BOLD + "ADRENALINE RUSH!");
 
        adrenalineCooldowns.put(victim.getUniqueId(), now + ADRENALINE_COOLDOWN_MS);
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
        
        long expireTime = System.currentTimeMillis() + durationMs;
        brokenShields.put(playerId, expireTime);

        Player player = getServer().getPlayer(playerId);
        if (player != null) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            player.sendActionBar(ChatColor.RED + "" + ChatColor.BOLD + "SHIELD BROKEN! (" + String.format("%.1f", durationMs / 1000.0) + "s)");
            
            int cooldownTicks = (int) (durationMs / 50);
            player.setCooldown(Material.SHIELD, cooldownTicks);
            
            forceStopShieldUse(player);
        } else {
        }
    }

    private void forceStopShieldUse(Player player) {
        UUID playerId = player.getUniqueId();
        
        player.clearActiveItem();

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                
                if (!player.isOnline() || !isShieldBroken(playerId) || !player.isBlocking() || attempts++ >= 8) {
                    cancel();
                    return;
                }

                player.clearActiveItem();
            }
        }.runTaskTimer(this, 1L, 1L);
    }
 
    /**
     * Checks if a player's shield is currently broken
     * @param playerId The UUID of the player to check
     * @return true if the shield is broken, false otherwise
     */
    private boolean isShieldBroken(UUID playerId) {
        
        if (!brokenShields.containsKey(playerId)) {
            return false;
        }
 
        long expireTime = brokenShields.get(playerId);
        long now = System.currentTimeMillis();
        
        if (now >= expireTime) {
            brokenShields.remove(playerId);
            return false;
        }
        return true;
    }
 
    // ========================================================
    // MACEGUARD COOLDOWN
    // ========================================================
    private void startMaceGuardCountdown(Player player, UUID playerId) {
        
        BukkitRunnable existingTask = maceGuardCountdownTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    maceGuardTimes.remove(playerId);
                    maceGuardCountdownTasks.remove(playerId);
                    cancel();
                    return;
                }

                long now = System.currentTimeMillis();
                Long expireAt = maceGuardTimes.get(playerId);

                if (expireAt == null || now >= expireAt) {
                    if (expireAt != null) {
                        player.sendActionBar(ChatColor.GOLD + "Standing Guard! " + ChatColor.YELLOW + "(0.0s remaining)");
                    }
                    maceGuardTimes.remove(playerId);
                    maceGuardCountdownTasks.remove(playerId);
                    cancel();
                    return;
                }

                Material mainHand = player.getInventory().getItemInMainHand().getType();
                Material offHand = player.getInventory().getItemInOffHand().getType();
                if (mainHand != Material.MACE && offHand != Material.MACE) {
                    maceGuardTimes.remove(playerId);
                    maceGuardCountdownTasks.remove(playerId);
                    player.sendActionBar(ChatColor.GRAY + "Guard cancelled.");
                    cancel();
                    return;
                }

                double secondsLeft = (expireAt - now) / 1000.0;
                player.sendActionBar(ChatColor.GOLD + "Standing Guard! " + ChatColor.YELLOW + "(" + String.format("%.1f", secondsLeft) + "s remaining)");
            }
        };

        maceGuardCountdownTasks.put(playerId, task);
        task.runTaskTimer(this, 0L, 2L);
    }

    // ===============================================
    // UTILITY METHODS
    // ===============================================
 
    /**
     * Reads weapon attack damage from item attributes first, then falls back to a
     * vanilla-style material lookup if the item does not expose an attack damage modifier.
     */
    private double getWeaponAttackDamage(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0.0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE);
            if (modifiers != null && !modifiers.isEmpty()) {
                double damage = 0.0;
                for (AttributeModifier modifier : modifiers) {
                    damage += modifier.getAmount();
                }
                damage += getWeaponEnchantmentDamageBonus(item);
    
                return damage;
            }
        }

        double fallbackDamage = getFallbackWeaponDamage(item.getType());
        fallbackDamage += getWeaponEnchantmentDamageBonus(item);
        return fallbackDamage;
    }

    private double getWeaponEnchantmentDamageBonus(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0.0;
        }

        int sharpness = item.getEnchantmentLevel(Enchantment.SHARPNESS);
        int smite = item.getEnchantmentLevel(Enchantment.SMITE);
        int bane = item.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);

        double bonus = 0.0;
        if (sharpness > 0) {
            bonus = Math.max(bonus, 0.5 * sharpness + 0.5);
        }
        if (smite > 0) {
            bonus = Math.max(bonus, 2.5 * smite);
        }
        if (bane > 0) {
            bonus = Math.max(bonus, 2.5 * bane);
        }

        return bonus;
    }

    private double getFallbackWeaponDamage(Material material) {
        String name = material.name();

        if (name.endsWith("_SWORD")) {
            if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) return 4.0;
            if (name.startsWith("STONE")) return 5.0;
            if (name.startsWith("IRON")) return 6.0;
            if (name.startsWith("DIAMOND")) return 7.0;
            if (name.startsWith("NETHERITE")) return 8.0;
        } else if (name.endsWith("_AXE")) {
            if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) return 7.0;
            if (name.startsWith("STONE") || name.startsWith("IRON") || name.startsWith("DIAMOND")) return 9.0;
            if (name.startsWith("NETHERITE")) return 10.0;
        } else if (material == Material.MACE) {
            return 7.0;
        }

        return 1.0;
    }
}
