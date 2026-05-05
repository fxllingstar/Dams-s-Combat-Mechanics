# Dams's Combat Mechanics (DCM) ⚔️

**Dams's Combat Mechanics (DCM)** is a comprehensive combat overhaul for Minecraft servers, designed to add depth, skill-expression, and variety to PvP and PvE. It introduces tactical movement, timed defensive maneuvers, specialized weapon combos, and dual-wielding capabilities.

DCM is built with cross-platform play in mind, featuring native support for Bedrock players via Floodgate.

---

##  Features & Abilities

###  Tactical Movement
* **Dash Ability**: Double-tap (or triple-tap) sneak within 1 second to perform a swift dash in your looking direction.
    * **Effect**: Grants a velocity boost and **0.3s of invulnerability**.
    * **Cost**: Consumes 5 Saturation points or 2 Hunger bars.
    * **Cooldown**: 5 seconds.
    * **Toggle**: Use `/dcm dash` to enable or disable this for yourself.

###  Defensive Maneuvers
* **Sword Parry**: Swing your sword just before being hit to negate incoming damage.
    * **Window**: 200ms for Java players / 400ms for Bedrock players.
    * **Effect**: Cancels damage, removes fire ticks, and provides a small vertical "bounce."
    * **Cooldown**: 10 seconds.
* **Shield Parry**: Raise your shield at the perfect moment.
    * **Window**: 250ms.
    * **Effect**: Stuns the attacker (Slowness II for 5 seconds).
    * **Cooldown**: 30 seconds.

###  Advanced Weaponry
* **Axe Slam (True Damage)**: Land 3 critical hits to build a combo.
    * **The Slam**: The 4th critical hit executes a Slam, dealing **3 Hearts of True Damage** (ignores armor and shields).
    * **Note**: Missing hits or being hit yourself reduces your combo count.
* **Shield Breaker**: Hitting a blocking opponent 4 times consecutively with Dual Swords will disable their shield entirely for 5 seconds.

###  Dual Wielding (Double Strike)
Wielding two Swords or two Axes enables the **Double Strike** mechanic.
* **Damage**: Adds the base damage and enchantments (like Sharpness) of your off-hand weapon to your main-hand attack.
* **Exhaustion**: This powerful strike has a 3-second internal cooldown.

###  Archery
* **Double Shot**: Hold two bows simultaneously to prepare a massive volley.
    * **Trigger**: Draw for at least 2 seconds.
    * **Effect**: Fires two arrows at once, with each arrow dealing **Double Damage**.

###  Passive Skills
* **Adrenaline Rush**: When your health drops to 4 hearts or lower, your survival instincts kick in.
    * **Buffs**: Speed II, Resistance II, and Strength I for 10 seconds.
    * **Cooldown**: 3 minutes.

---

## 🛠️ Commands
| Command | Description |
| :--- | :--- |
| `/dcm dash` | Toggles your ability to use the Dash mechanic. |

---

## ⚙️ Technical Details
* **Bedrock Compatibility**: Uses the Floodgate API to detect Bedrock players and automatically doubles the Parry window to account for latency and touch controls.
* **Shield Logic**: Includes a custom shield-disable system that visually grays out the shield in the hotbar (Vanilla Cooldown) and prevents interaction.
* **Performance**: Includes automated memory cleanup tasks to ensure cooldown and combo maps stay lean.

---

## 📥 Installation
1.  Download the latest `.jar` file.
2.  Place it in your server's `/plugins/` folder.
3.  (Optional) Install **Floodgate** to enable Bedrock-specific balancing.
4.  Restart your server.
5.  For bedrock, I will have seperate files later, labelled "Bedrock"

---

## 👨‍💻 Author
Developed by **Dams/st4r/star, whatever is easier and you recognize me with lmao :>**. 
