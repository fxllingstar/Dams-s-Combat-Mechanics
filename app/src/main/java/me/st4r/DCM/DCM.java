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
 * @version 1.1
 */
public class DCM extends JavaPlugin implements Listener {
 
    // ===========================
    // DUAL WIELDING CONFIGURATION
    // ===========================
    private static final long DUAL_MELEE_COOLDOWN_MS = 3000;
    private static final long DUAL_BOW_CHARGE_TIME_MS = 2000;
    private static final int SHIELD_BREAK_THRESHOLD = 4;
    private static final long SHIELD_BREAK_DURATION_MS = 5000;
    private static final long SHIELD_STREAK_TIMEOUT_MS = 4000;
    private static final long MACE_GUARD_WINDOW_MS = 1500;
    private static final double MACE_GUARD_DAMAGE_MULTIPLIER = 0.6;

 
    // ===========================
    // PARRY CONFIGURATION
    // ===========================
    private static final long SWORD_PARRY_WINDOW_MS = 200;
    private static final long SWORD_PARRY_WINDOW_BEDROCK_MS = 400;
    private static final long SHIELD_PARRY_WINDOW_MS = 250;
    private static final long SWORD_PARRY_COOLDOWN_MS = 10000;
    private static final long SHIELD_PARRY_COOLDOWN_MS = 30000;
    private static final int SHIELD_STUN_DURATION_TICKS = 100; // 5 seconds
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

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateAvailable = true;
        } else {
            getLogger().warning("Floodgate not found! Bedrock player support is disabled. Install Floodgate + Geyser for Bedrock compatibility.");
        }

        getServer().getPluginManager().registerEvents(this, this);
        startMemoryCleanupTask();
        getLogger().info("-----------------------------------------------------------------------");
        getLogger().info("DCM (Dams's Combat Mechanics) v2.0 has been enabled!");
        getLogger().info("Have fun and Good Luck!");
        getLogger().info("-----------------------------------------------------------------------");
    }
 
    @Override
    public void onDisable() {
        for (BukkitRunnable task : maceGuardCountdownTasks.values()) {
            task.cancel();
        }
        maceGuardCountdownTasks.clear();

        getLogger().info("Oh.. server is dead?");
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
                long staleTime = now - 10000; // 10 seconds ago
 
                // Clean up timing maps
                lastSwingTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                lastBlockTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                shieldStreakTimestamps.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                lastExhaustionMsgTimes.entrySet().removeIf(entry -> entry.getValue() < staleTime);
                riposteWindows.entrySet().removeIf(entry -> entry.getValue() < now);
                // Clean up expired shield breaks
                brokenShields.entrySet().removeIf(entry -> entry.getValue() < now);
                maceGuardTimes.entrySet().removeIf(entry -> entry.getValue() < now);
                
                // Clean up expired invulnerability
                invulnerablePlayers.entrySet().removeIf(entry -> entry.getValue() < now);

                // Clean up stale axe combos
                axeComboTimestamps.entrySet().removeIf(entry -> {
                    if (now - entry.getValue() > AXE_COMBO_TIMEOUT_MS) {
                        axeCombos.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(this, 1200L, 1200L);
    }
 
    // ===========================
    // COMMAND HANDLING
    // ===========================
 
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
 
        if (command.getName().equalsIgnoreCase("dash")) {
            boolean currentState = dashEnabled.getOrDefault(player.getUniqueId(), true);
            dashEnabled.put(player.getUniqueId(), !currentState);
            player.sendMessage("§eDash ability: " + (!currentState ? "§aENABLED" : "§cDISABLED"));
            return true;
        }
 
        return false;
    }
 
    // ===========================
    // PLAYER QUIT CLEANUP
    // ===========================
 
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        
        // Dual wielding cleanup
        meleeCooldowns.remove(id);
        bowDrawStarts.remove(id);
        shieldHitStreak.remove(id);
        lastTargets.remove(id);
        shieldStreakTimestamps.remove(id);
        lastExhaustionMsgTimes.remove(id);

        // Parry cleanup
        lastSwingTimes.remove(id);
        lastBlockTimes.remove(id);
        swordParryCooldowns.remove(id);
        shieldParryCooldowns.remove(id);
        brokenShields.remove(id);
        riposteWindows.remove(id);
        maceGuardTimes.remove(id);
        BukkitRunnable guardTask = maceGuardCountdownTasks.remove(id);
        if (guardTask != null) {
            guardTask.cancel();
        }
        // Axe combo cleanup
        axeCombos.remove(id);
        axeComboTimestamps.remove(id);
        
        // Dash cleanup
        dashEnabled.remove(id);
        dashCooldowns.remove(id);
        invulnerablePlayers.remove(id);
        
        // Adrenaline cleanup
        adrenalineCooldowns.remove(id);
    }
 
    // ===============================================
    // DUAL WIELDING: MELEE WEAPONS (SWORDS & AXES)
    // ===============================================
 
    @EventHandler(priority = EventPriority.LOW)
    public void onDualWieldMelee(EntityDamageByEntityEvent event) {

        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        ItemStack offHand = attacker.getInventory().getItemInOffHand();

        // Validate both hands have weapons
        if (mainHand.getType().isAir() || offHand.getType().isAir()) return;

        // Exclude maces from dual wielding
        if (mainHand.getType() == Material.MACE || offHand.getType() == Material.MACE) return;

        boolean isDualSwords = mainHand.getType().name().endsWith("_SWORD") && offHand.getType().name().endsWith("_SWORD");
        boolean isDualAxes = mainHand.getType().name().endsWith("_AXE") && offHand.getType().name().endsWith("_AXE");

        if (!isDualSwords && !isDualAxes) return;

        UUID attackerId = attacker.getUniqueId();
        UUID victimId = victim.getUniqueId();
        long currentTime = System.currentTimeMillis();

       //=========================
       // Dual Yield Cooldown Check
       // ========================
       if (meleeCooldowns.containsKey(attackerId)){
        long timeLeft = meleeCooldowns.get(attackerId) - currentTime;
        if (timeLeft > 0){
            long lastMsgTime = lastExhaustionMsgTimes.getOrDefault(attackerId, 0L);
            if (currentTime - lastMsgTime > 1000){
                attacker.sendMessage(ChatColor.RED + "You are exhausted! Wait " + String.format("%.1f", timeLeft / 1000.0) + "s for double strike.");
                lastExhaustionMsgTimes.put(attackerId, currentTime);
            }
            return;
        }
       }



        // ===========================
        // SHIELD BREAKING (DUAL SWORDS, PLAYER TARGETS ONLY)
        // ===========================
        if (isDualSwords && victim instanceof Player playerVictim) {
            // Reset streak if target changes or too much time has passed since last shield hit
            UUID lastTarget = lastTargets.get(attackerId);
            if (lastTarget == null || !lastTarget.equals(victimId)) {
                shieldHitStreak.put(attackerId, 0);
                lastTargets.put(attackerId, victimId);
            } else {
                long lastStreakTime = shieldStreakTimestamps.getOrDefault(attackerId, 0L);
                if (currentTime - lastStreakTime > SHIELD_STREAK_TIMEOUT_MS) {
                    shieldHitStreak.put(attackerId, 0);
                }
            }

            if (playerVictim.isBlocking()) {
                int streak = shieldHitStreak.getOrDefault(attackerId, 0) + 1;
                shieldHitStreak.put(attackerId, streak);
                shieldStreakTimestamps.put(attackerId, currentTime);

                if (streak >= SHIELD_BREAK_THRESHOLD) {
                    breakShield(victimId, SHIELD_BREAK_DURATION_MS);
                    shieldHitStreak.put(attackerId, 0);
                    attacker.sendMessage(ChatColor.GOLD + "Shield Breaker! Enemy shield disabled for 5 seconds!");
                } else {
                    attacker.sendActionBar("§6Shield Hits: " + streak + "/" + SHIELD_BREAK_THRESHOLD);
                }
            } else {
                shieldHitStreak.put(attackerId, 0);
            }
        }
 

        // ===========================
        // CALCULATE OFF-HAND DAMAGE
        // ===========================
        double offHandBaseDamage = getBaseDamage(offHand.getType());
        double offHandEnchantBonus = 0;
 
        if (offHand.hasItemMeta()) {
            int sharpnessLevel = offHand.getEnchantmentLevel(Enchantment.SHARPNESS);
            if (sharpnessLevel > 0) {
                offHandEnchantBonus = 0.5 + (0.5 * sharpnessLevel);
            }
        }
 
        double totalExtraDamage = offHandBaseDamage + offHandEnchantBonus;
        event.setDamage(event.getDamage() + totalExtraDamage);
 
        // ===========================
        // APPLY COOLDOWN & FEEDBACK
        // ===========================
        meleeCooldowns.put(attackerId, currentTime + DUAL_MELEE_COOLDOWN_MS);
        attacker.setCooldown(mainHand.getType(), 60); // 3 seconds visual cooldown
        attacker.sendMessage(ChatColor.GOLD + "Double Strike! " + ChatColor.GRAY + "(3.0s Cooldown)");
    }
 
    // ===============================================
    // DUAL WIELDING: BOWS
    // ===============================================
 
    @EventHandler
    public void onBowDrawStart(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
 
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
 
        if (mainHand.getType() == Material.BOW && offHand.getType() == Material.BOW) {
            bowDrawStarts.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
 
    @EventHandler
    public void onBowItemSwap(PlayerItemHeldEvent event) {
        // Reset bow draw timer if player swaps items
        bowDrawStarts.remove(event.getPlayer().getUniqueId());
    }
 
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof AbstractArrow mainArrow)) return;
 
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
 
        if (mainHand.getType() != Material.BOW || offHand.getType() != Material.BOW) return;
 
        UUID id = player.getUniqueId();
        boolean isCreative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;
 
        // ===========================
        // CHARGE TIME CHECK (SURVIVAL ONLY)
        // ===========================
        if (!isCreative) {
            if (!bowDrawStarts.containsKey(id)) {
                event.setCancelled(true);
                return;
            }
 
            long drawDuration = System.currentTimeMillis() - bowDrawStarts.get(id);
 
            if (drawDuration < DUAL_BOW_CHARGE_TIME_MS) {
                event.setCancelled(true);
                long timeLeft = DUAL_BOW_CHARGE_TIME_MS - drawDuration;
                player.sendMessage(ChatColor.RED + "Draw longer! (" + String.format("%.1f", timeLeft / 1000.0) + "s remaining)");
                return;
            }
        }
 
        // ===========================
        // DOUBLE SHOT EXECUTION
        // ===========================
        double combinedDamage = mainArrow.getDamage() * 2;
        mainArrow.setDamage(combinedDamage);
 
        // Launch second arrow with slight spread
        Arrow secondArrow = player.launchProjectile(Arrow.class);
        Vector velocity = mainArrow.getVelocity();
        double spread = 0.15;
        Vector randomizedVelocity = velocity.clone().add(new Vector(
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * spread
        ));
 
        secondArrow.setVelocity(randomizedVelocity.normalize().multiply(velocity.length()));
        secondArrow.setDamage(combinedDamage);
        secondArrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
 
        bowDrawStarts.remove(id);
        player.sendMessage(ChatColor.AQUA + "Double Shot fired!");
    }
 
    // ===============================================
    // PARRY SYSTEM
    // ===============================================
 
    @EventHandler
    public void onSwordGuardAttempt(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        if (!mainHand.name().endsWith("_SWORD")) return;

        lastSwingTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }
 
    @EventHandler
    public void onShieldRaise(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
 
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Material offHand = player.getInventory().getItemInOffHand().getType();
 
        if (mainHand != Material.SHIELD && offHand != Material.SHIELD) return;
 
        // ===========================
        // SHIELD BREAK CHECK
        // ===========================
        if (isShieldBroken(player.getUniqueId())) {
            long timeLeft = brokenShields.get(player.getUniqueId()) - System.currentTimeMillis();
            player.sendActionBar("§c🛡 Shield Disabled! (" + String.format("%.1f", timeLeft / 1000.0) + "s)");
            event.setCancelled(true);
            return;
        }
 
        // Track shield raise timing for parry window
        if (!player.isBlocking()) {
            lastBlockTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onMaceGuardStart(PlayerInteractEvent event) {
        if (event.isCancelled()) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Material offHand = player.getInventory().getItemInOffHand().getType();

        if (mainHand != Material.MACE && offHand != Material.MACE) return;

        UUID playerId = player.getUniqueId();
        long expireAt = System.currentTimeMillis() + MACE_GUARD_WINDOW_MS;

        maceGuardTimes.put(playerId, expireAt);
        startMaceGuardCountdown(player, playerId);
    }
 
    @EventHandler(priority = EventPriority.HIGH)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.THORNS) return;
 
        long now = System.currentTimeMillis();
        UUID attackerId = attacker.getUniqueId();

        Long riposteExpiry = riposteWindows.get(attackerId);
        if (riposteExpiry != null) {
            if (now < riposteExpiry) {
                event.setDamage(event.getDamage() * RIPOSTE_DAMAGE_MULTIPLIER);
                Vector knockback = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
                if (knockback.lengthSquared() > 0.0001) {
                    victim.setVelocity(knockback.normalize().multiply(RIPOSTE_KNOCKBACK_HORIZONTAL).setY(RIPOSTE_KNOCKBACK_VERTICAL));
                }
                attacker.sendActionBar("§6⚔ RIPOSTE!");
                CombatFX.playRiposteEffects(attacker.getLocation());
                riposteWindows.remove(attackerId);
            } else {
                riposteWindows.remove(attackerId);
            }
        }
 
        // ===========================
        // DASH INVULNERABILITY CHECK
        // ===========================
        if (invulnerablePlayers.containsKey(victim.getUniqueId())) {
            if (now < invulnerablePlayers.get(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
 
        // ===========================
        // COMBO REDUCTION ON HIT
        // ===========================
        int victimCombo = axeCombos.getOrDefault(victim.getUniqueId(), 0);
        if (victimCombo > 0) {
            axeCombos.put(victim.getUniqueId(), victimCombo - 1);
            victim.sendActionBar("§cCombo Reduced: §l" + (victimCombo - 1) + "/" + AXE_COMBO_MAX);
        }
 
        Material victimWeapon = victim.getInventory().getItemInMainHand().getType();
        Material attackerWeapon = attacker.getInventory().getItemInMainHand().getType();

        // ===========================
        // MACE SHIELD CRUSH
        // ===========================
        if (attackerWeapon == Material.MACE && victim.isBlocking() && !isShieldBroken(victim.getUniqueId())) {
            breakShield(victim.getUniqueId(), SHIELD_BREAK_DURATION_MS);
            attacker.sendActionBar("§6Heavy Crush! Shield shattered.");
        }

        // ===========================
        // HEAVY GUARD DAMAGE REDUCTION
        // ===========================
        Long guardExpire = maceGuardTimes.get(victim.getUniqueId());
        if (guardExpire != null && now <= guardExpire) {
            event.setDamage(event.getDamage() * MACE_GUARD_DAMAGE_MULTIPLIER);
            victim.setVelocity(new Vector(0, 0, 0));
        }
 
        // ===========================
        // SWORD PARRY
        // ===========================
        if (victimWeapon.name().endsWith("_SWORD")) {
            long lastSwing = lastSwingTimes.getOrDefault(victim.getUniqueId(), 0L);
 
            // Platform-specific parry window
            long parryWindow = SWORD_PARRY_WINDOW_MS;
            if (floodgateAvailable && FloodgateApi.getInstance().isFloodgatePlayer(victim.getUniqueId())) {
                parryWindow = SWORD_PARRY_WINDOW_BEDROCK_MS;
            }
 
            if (now - lastSwing <= parryWindow) {
                long cd = swordParryCooldowns.getOrDefault(victim.getUniqueId(), 0L);
                if (now >= cd) {
                    event.setCancelled(true);
                    victim.setFireTicks(0);
 
                    // Knockup effect
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            victim.setVelocity(new Vector(0, 0.2, 0));
                        }
                    }.runTaskLater(this, 1L);
 
                    CombatFX.playParryEffects(victim.getLocation(), parryWindow == SWORD_PARRY_WINDOW_BEDROCK_MS);
 
                    String platformPrefix = (parryWindow == SWORD_PARRY_WINDOW_BEDROCK_MS) ? "§b[Bedrock] " : "§e[Java] ";
                    victim.sendActionBar(platformPrefix + "§6⚔ Sword Parry!");
 
                    lastSwingTimes.remove(victim.getUniqueId());
                    riposteWindows.put(victim.getUniqueId(), now + RIPOSTE_WINDOW_MS);
                    swordParryCooldowns.put(victim.getUniqueId(), now + SWORD_PARRY_COOLDOWN_MS);
                }
            }
        }
 
        // ===========================
        // SHIELD PARRY
        // ===========================
        if (victim.isBlocking() && !isShieldBroken(victim.getUniqueId())) {
            long lastBlock = lastBlockTimes.getOrDefault(victim.getUniqueId(), 0L);
 
            if (now - lastBlock <= SHIELD_PARRY_WINDOW_MS) {
                long cd = shieldParryCooldowns.getOrDefault(victim.getUniqueId(), 0L);
                if (now >= cd) {
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SHIELD_STUN_DURATION_TICKS, 1));
                    victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.5f);
                    victim.sendActionBar("§b🛡 Shield Parry! Attacker Stunned!");
                    shieldParryCooldowns.put(victim.getUniqueId(), now + SHIELD_PARRY_COOLDOWN_MS);
                }
            }
        }
 
        // ===========================
        // AXE COMBO SYSTEM
        // ===========================
        if (attackerWeapon.name().endsWith("_AXE") && event.isCritical()) {
            int combo = axeCombos.getOrDefault(attackerId, 0);
            long lastComboHit = axeComboTimestamps.getOrDefault(attackerId, 0L);

            if (combo > 0 && now - lastComboHit > AXE_COMBO_TIMEOUT_MS) {
                combo = 0;
            }
 
            if (combo >= 3) {
                // 4th critical hit: Execute Slam
                if (!victim.isBlocking() || isShieldBroken(victim.getUniqueId())) {
                    double newHealth = Math.max(0.0, victim.getHealth() - AXE_SLAM_TRUE_DAMAGE);
                    victim.setHealth(newHealth);
                    victim.damage(0.01, attacker);
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
                    attacker.sendActionBar("§c§l SLAM EXECUTED! (4/4)");
                } else {
                    attacker.sendActionBar("§7Slam Blocked!");
                }
                axeCombos.put(attackerId, 0);
                axeComboTimestamps.remove(attackerId);
            } else {
                combo++;
                axeCombos.put(attackerId, combo);
                axeComboTimestamps.put(attackerId, now);
                attacker.sendActionBar("§e⚔ Combo: §l" + combo + "/" + AXE_COMBO_MAX);
            }
        }
    }
 
    // ===============================================
    // DASH ABILITY
    // ===============================================
 
    @EventHandler
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
 
        if (!dashEnabled.getOrDefault(id, true)) return;
        if (!player.isSprinting()) return;

        triggerDash(player, player.getVelocity());
    }
 
    private void triggerDash(Player player, Vector movementVector) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        long cd = dashCooldowns.getOrDefault(id, 0L);
 
        // ===========================
        // COOLDOWN CHECK
        // ===========================
        if (now < cd) {
            player.sendActionBar("§cDash Cooldown: " + String.format("%.1f", (cd - now) / 1000.0) + "s");
            return;
        }
 
        // ===========================
        // RESOURCE CONSUMPTION
        // ===========================
        float currentSat = player.getSaturation();
        int currentFood = player.getFoodLevel();
 
        if (currentSat >= 5.0f) {
            player.setSaturation(currentSat - 5.0f);
        } else if (currentFood >= 4) {
            player.setFoodLevel(currentFood - 4);
        } else {
            player.sendActionBar("§cToo exhausted to dash!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.5f);
            return;
        }
 
        // ===========================
        // INVULNERABILITY FRAME
        // ===========================
        invulnerablePlayers.put(id, now + DASH_INVULN_DURATION_MS);
 
        // ===========================
        // MOVEMENT LOGIC
        // ===========================
        Vector dashVec = movementVector.clone().setY(0);
        if (dashVec.length() < 0.1) {
            dashVec = player.getLocation().getDirection().setY(0).normalize();
        } else {
            dashVec.normalize();
        }
 
        player.setVelocity(dashVec.multiply(DASH_VELOCITY_MULTIPLIER).setY(DASH_VERTICAL_BOOST));
        CombatFX.playDashEffects(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        player.sendActionBar("§b DASH! (§6-Energy§b)");
 
        // ===========================
        // SET COOLDOWN
        // ===========================
        dashCooldowns.put(id, now + DASH_COOLDOWN_MS);
    }
 
    // ===============================================
    // ADRENALINE RUSH
    // ===============================================
 
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnyDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event instanceof EntityDamageByEntityEvent) return;

        Long guardExpire = maceGuardTimes.get(victim.getUniqueId());
        long now = System.currentTimeMillis();
        if (guardExpire != null && now <= guardExpire) {
            event.setDamage(event.getDamage() * MACE_GUARD_DAMAGE_MULTIPLIER);
            victim.setVelocity(new Vector(0, 0, 0));
        }
 
        // Calculate post-damage health
        double finalHealth = victim.getHealth() - event.getFinalDamage();
 
        // Trigger adrenaline if dropping to/below threshold without dying
        if (finalHealth > 0 && finalHealth <= ADRENALINE_HEALTH_THRESHOLD) {
            long cd = adrenalineCooldowns.getOrDefault(victim.getUniqueId(), 0L);
 
            if (now >= cd) {
                // Apply buffs
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ADRENALINE_DURATION_TICKS, 1));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ADRENALINE_DURATION_TICKS, 1));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ADRENALINE_DURATION_TICKS, 0));
 
                CombatFX.playAdrenalineEffects(victim);
                victim.sendActionBar("§c§lADRENALINE RUSH!");
 
                adrenalineCooldowns.put(victim.getUniqueId(), now + ADRENALINE_COOLDOWN_MS);
            }
        }
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
            player.sendActionBar("§c§l🛡 SHIELD BROKEN! (" + String.format("%.1f", durationMs / 1000.0) + "s)");

            int cooldownTicks = (int) (durationMs / 50);
            player.setCooldown(Material.SHIELD, cooldownTicks);

            // Force out of blocking state by swapping items
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.getType() == Material.SHIELD) {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                player.getInventory().setItemInOffHand(mainHand);
                player.getInventory().setItemInMainHand(offHand);

                getServer().getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setItemInOffHand(offHand);
                        player.getInventory().setItemInMainHand(mainHand);
                    }
                }, 1L);
            }
        }
    }
 
    /**
     * Checks if a player's shield is currently broken
     * @param playerId The UUID of the player to check
     * @return true if the shield is broken, false otherwise
     */
    private boolean isShieldBroken(UUID playerId) {
        if (!brokenShields.containsKey(playerId)) return false;
 
        long expireTime = brokenShields.get(playerId);
        if (System.currentTimeMillis() >= expireTime) {
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
                        player.sendActionBar("§6Standing Guard! §e(0.0s remaining)");
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
        String name = material.name();
        
        if (name.endsWith("_SWORD")) {
            if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) return 4.0;
            if (name.startsWith("STONE")) return 5.0;
            if (name.startsWith("IRON")) return 6.0;
            if (name.startsWith("DIAMOND")) return 7.0;
            if (name.startsWith("NETHERITE")) return 8.0;
        }
        
        if (name.endsWith("_AXE")) {
            if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) return 7.0;
            if (name.startsWith("STONE") || name.startsWith("IRON") || name.startsWith("DIAMOND")) return 9.0;
            if (name.startsWith("NETHERITE")) return 10.0;
        }
        
        return 1.0;
    }
}
