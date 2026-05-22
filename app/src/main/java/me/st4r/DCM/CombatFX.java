package me.st4r.DCM;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class CombatFX {

    private CombatFX() {
    }

    public static void playParryEffects(Location location, boolean isBedrock) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.CRIT, location, 18, 0.35, 0.4, 0.35, 0.04);
        world.spawnParticle(Particle.SWEEP_ATTACK, location, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticle(Particle.FLASH, location.clone().add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.0);

        float pitch = isBedrock ? 0.9f : 1.1f;
        world.playSound(location, Sound.BLOCK_ANVIL_PLACE, 1.0f, pitch);
    }

    public static void playDashEffects(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.CLOUD, location.clone().add(0.0, 0.5, 0.0), 12, 0.2, 0.15, 0.2, 0.05);
        world.spawnParticle(Particle.CRIT, location.clone().add(0.0, 1.0, 0.0), 8, 0.3, 0.3, 0.3, 0.02);
    }

    public static void playRiposteEffects(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.CRIT, location.clone().add(0.0, 1.0, 0.0), 24, 0.3, 0.4, 0.3, 0.06);
        world.spawnParticle(Particle.SWEEP_ATTACK, location.clone().add(0.0, 1.0, 0.0), 3, 0.2, 0.2, 0.2, 0.0);
        world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
    }

    public static void playAdrenalineEffects(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticle(Particle.CRIMSON_SPORE, location.clone().add(0.0, 1.0, 0.0), 30, 0.55, 0.65, 0.55, 0.01);

        player.spawnParticle(Particle.WITCH, location.clone().add(0.0, 1.0, 0.0), 14, 0.35, 0.55, 0.35, 0.0);
        player.spawnParticle(Particle.CLOUD, location.clone().add(0.0, 1.1, 0.0), 8, 0.2, 0.25, 0.2, 0.01);

        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
    }
}
