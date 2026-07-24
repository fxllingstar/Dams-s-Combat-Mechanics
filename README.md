# Dams's Combat Mechanics (DCM) 

**Dams's Combat Mechanics (DCM)** is a full combat overhaul plugin for Paper 1.21.11 servers. It expands vanilla combat with timing-based defense, weapon synergy, high-impact combo systems, mobility tools, and passive clutch mechanics.

This `2.0` branch is a full redesign of the plugin systems and combat flow.

---

## Features & Abilities

### Tactical Movement
- **Dash**: Sprint + sneak to perform a fast directional dash.
- **Invulnerability Frames**: Grants **0.3s invulnerability** during dash.
- **Cost**: Uses **5 saturation** first, then **4 hunger** if saturation is too low.
- **Cooldown**: **5 seconds**.
- **Toggle**: `/dash` enables/disables dash per player.
- **Feedback**: Action bar cooldown, sound, and particle effects.

### Dual-Wield Combat
- **Double Strike (Swords/Axes)**: Using two swords or two axes adds off-hand base damage + Sharpness bonus to the main hit.
- **Cooldown**: **3 seconds** per boosted dual-melee strike.
- **Mace Exclusion**: Maces are intentionally excluded from dual-melee Double Strike.
- **Exhaustion Messaging**: Cooldown warning messages are rate-limited to prevent chat spam.

### Shield Breaker (Dual Swords)
- Hitting a **blocking player** with dual swords builds a shield-hit streak.
- At **4 consecutive shield hits**, the target shield is disabled for **5 seconds**.
- Streak resets on target swap, timeout, or when the target stops blocking.
- Shield disable is enforced with vanilla-style cooldown + block prevention.

### Dual-Bow Archery
- Holding **two bows** enables charged double-shot behavior.
- Requires **2.0s draw time** in non-creative modes.
- On release:
- First arrow damage is doubled.
- A second arrow is launched with slight spread and matching boosted damage.
- Includes draw-time validation and cancellation if requirements are not met.

### Parry System
- **Sword Parry**: Right-click timing with sword before impact to negate incoming damage.
- **Parry Window**:
- Java: **200ms**
- Bedrock (Floodgate): **400ms**
- **Sword Parry Cooldown**: **4 seconds**.
- **Effects**: Damage cancel, fire tick clear, short vertical bump, sound/particles, action-bar feedback.

- **Shield Parry**: Timed shield raise window.
- **Parry Window**: **250ms**.
- **Shield Parry Cooldown**: **10 seconds**.
- **Effect**: Applies Slowness II stun to attacker for **5 seconds**.

### Riposte
- Successful sword parry opens a **1.5s riposte window**.
- Next melee hit during that window gains:
- **1.6x damage multiplier**
- Enhanced knockback (horizontal + vertical)
- Uses dedicated visual/audio combat effects.

### Mace Systems
- **Mace Heavy Crush**: Mace attacks against a blocking target instantly shield-break for **5 seconds**.
- **Standing Guard (Mace Guard)**:
- Right-click with a mace activates a **1.5s guard window**.
- Incoming damage is reduced to **60%** during active guard.
- Knockback is neutralized during guard.
- Real-time action-bar countdown shows remaining guard time.

### Axe Combo / Slam
- Critical hits with axes build combo stacks.
- Combo timeout: **8 seconds** between valid crits.
- On the **4th critical hit**, Slam triggers:
- Deals **3 hearts true damage** (`6.0` health), bypassing normal mitigation flow.
- If target is actively blocking with an unbroken shield, Slam is blocked.
- Combo can be reduced when the combo owner is hit. (This will be reworked for easier slams.)

### Adrenaline Rush
- Triggers when player would drop to **4 hearts or lower** (and survives).
- Also triggers on a critical hit (A hit which deals 5 hearts in a single blow)
- Duration: **10 seconds**.
- Buffs:
- Speed II
- Resistance II
- Strength I
- Cooldown: **3 minutes**.
- Includes dedicated audiovisual effect burst.

---

##  Commands

| Command | Description |
| :--- | :--- |
| `/dash` | Toggles dash for the player using the command. |

---

##  Technical Details

- **Server Software**: Paper API `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`.
- **Java Version**: Java **21** target/toolchain.
- **Optional Dependency**: Floodgate `org.geysermc.floodgate:api:2.2.5-SNAPSHOT` for Bedrock player detection and parry-window adaptation.
- **Bedrock Handling**: If Floodgate is missing, plugin remains functional and logs a warning, but Bedrock-specific timing adjustments are disabled.
- **CombatFX Module**: Centralized particle/sound effect handling for parry, dash, riposte, and adrenaline.
- **Memory Hygiene**:
- Periodic cleanup of cooldown/timeout state maps.
- Quit-event cleanup for all tracked player combat states.
- **Shield Disable Enforcement**:
- Tracks per-player broken shield windows.
- Applies shield cooldown ticks.
- Temporarily disrupts off-hand shield blocking to enforce disable behavior.

---

## Installation

1. Build or download the latest DCM `.jar`.
2. Place the jar in `/plugins/`.
3. (Optional) Install **Floodgate** (and Geyser setup) for Bedrock-aware parry timing.
4. Start or restart the server.

---

## License

This plugin is licensed under the APGL-v3 LICENSE, if you fork this repository and build your own version, you MUST follow the license file.

---

## Author

Developed by **Dams/st4r/star, whatever is easier and you recognize me with lmao :>**.
Co-developed by **TheSandrone**
