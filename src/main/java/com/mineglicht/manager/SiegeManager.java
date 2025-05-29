package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeFlag;
import com.mineglicht.models.SiegeState;
import com.mineglicht.task.SiegeCooldownTask;
import com.mineglicht.task.SiegeTimerTask;
import com.mineglicht.util.FireworkUtils;
import com.mineglicht.util.ItemUtils;
import com.mineglicht.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages siege operations including starting/ending sieges,
 * handling flag captures, and siege cooldowns.
 */
public class SiegeManager {
    private final cityWars plugin;
    private final Map<UUID, UUID> activeSieges; // CityId -> AttackerCityId
    private final Map<UUID, SiegeState> siegeStates; // CityId -> SiegeState
    private final Map<UUID, Location> siegeFlags; // CityId -> Flag location
    private final Map<String, Long> siegeCooldowns; // "AttackerCityId:DefenderCityId" -> Cooldown end time
    private final File siegesFile;
    private FileConfiguration siegesConfig;

    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final EconomyManager economyManager;
    private final RegionManager regionManager;

    // Siege configuration
    private int minOnlinePercentage;
    private long siegeDuration;
    private long sackingDuration;
    private long cooldownDuration;
    private int fireworkInterval;
    private String requiredCurrency;
    private double siegeCost;

    public SiegeManager(cityWars plugin, CityManager cityManager,
            CitizenManager citizenManager, EconomyManager economyManager,
            RegionManager regionManager) {
        this.plugin = plugin;
        this.cityManager = cityManager;
        this.citizenManager = citizenManager;
        this.economyManager = economyManager;
        this.regionManager = regionManager;

        this.activeSieges = new HashMap<>();
        this.siegeStates = new HashMap<>();
        this.siegeFlags = new HashMap<>();
        this.siegeCooldowns = new HashMap<>();
        this.siegesFile = new File(plugin.getDataFolder(), "sieges.yml");

        loadConfig();
        loadSieges();
    }

    /**
     * Loads siege configuration values from config.yml
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        // Load siege settings
        minOnlinePercentage = config.getInt("siege.minOnlinePercentage", 30);
        siegeDuration = config.getLong("siege.duration", 20 * 60); // Default: 20 minutes in seconds
        sackingDuration = config.getLong("siege.sackingDuration", 5 * 60); // Default: 5 minutes in seconds
        cooldownDuration = config.getLong("siege.cooldown", 24 * 60 * 60); // Default: 24 hours in seconds
        fireworkInterval = config.getInt("siege.fireworkInterval", 30); // Default: 30 seconds
        requiredCurrency = config.getString("siege.requiredCurrency", "jp");
        siegeCost = config.getDouble("siege.cost", 1000.0);
    }

    /**
     * Checks if a siege can be started against the target city.
     * 
     * @param attackerPlayer The player initiating the attack
     * @param targetCity     The city being attacked
     * @return true if siege can be started
     */
    public boolean canStartSiege(Player attackerPlayer, City targetCity) {
        if (attackerPlayer == null || targetCity == null) {
            return false;
        }

        // Check if player is in a city
        City attackerCity = citizenManager.getPlayerCity(attackerPlayer.getUniqueId());
        if (attackerCity == null) {
            return false;
        }

        // Check if player is owner or admin of their city
        if (!citizenManager.isOwnerOrAdmin(attackerPlayer.getUniqueId())) {
            return false;
        }

        // Check if target city has enough online players
        double onlinePercentage = citizenManager.getOnlineCitizenPercentage(targetCity.getId());
        if (onlinePercentage * 100 < minOnlinePercentage) {
            return false;
        }

        // Check if target city is already under siege
        if (siegeStates.containsKey(targetCity.getId())) {
            return false;
        }

        // Check if attacker city is already attacking another city
        if (activeSieges.containsValue(attackerCity.getId())) {
            return false;
        }

        // Check cooldown
        String cooldownKey = getCooldownKey(attackerCity.getId(), targetCity.getId());
        if (siegeCooldowns.containsKey(cooldownKey)) {
            long cooldownEnd = siegeCooldowns.get(cooldownKey);
            if (System.currentTimeMillis() < cooldownEnd) {
                return false;
            }
        }

        // Check if player has enough currency
        if (!economyManager.hasCurrency(attackerPlayer, requiredCurrency, siegeCost)) {
            return false;
        }

        return true;
    }

    /**
     * Starts a siege against a target city.
     * 
     * @param attackerPlayer The player initiating the attack
     * @param targetCity     The city being attacked
     * @param flagLocation   Location where the siege flag is placed
     * @return true if siege started successfully
     */
    public boolean startSiege(Player attackerPlayer, City targetCity, Location flagLocation) {
        if (!canStartSiege(attackerPlayer, targetCity)) {
            return false;
        }

        // Charge the player
        if (!economyManager.withdrawCurrency(attackerPlayer, requiredCurrency, siegeCost)) {
            return false;
        }

        // Get attacker city
        City attackerCity = citizenManager.getPlayerCity(attackerPlayer.getUniqueId());

        // Create siege flag in the world
        SiegeFlag siegeFlag = createSiegeFlag(flagLocation, attackerCity, targetCity);
        if (siegeFlag == null) {
            economyManager.depositCurrency(attackerPlayer, requiredCurrency, siegeCost); // Refund cost
            return false;
        }

        // Record siege data
        activeSieges.put(targetCity.getId(), attackerCity.getId());
        siegeStates.put(targetCity.getId(), SiegeState.ACTIVE);
        siegeFlags.put(targetCity.getId(), flagLocation);

        // Allow PvP in the target city
        regionManager.setSiegePvPEnabled(targetCity, true);

        // Start siege timer
        startSiegeTimer(targetCity.getId(), attackerCity.getId());

        // Send notification to all players in both cities
        notifySiegeStart(attackerCity, targetCity);

        // Start firework effect at flag
        scheduleFireworks(flagLocation, targetCity.getId());

        // Save state
        saveSieges();

        // Trigger event (would be implemented via API)
        // plugin.getAPI().fireEvent(new SiegeStartEvent(attackerCity, targetCity));

        return true;
    }

    /**
     * Creates a siege flag at the specified location.
     * 
     * @param location     Flag location
     * @param attackerCity Attacking city
     * @param targetCity   Defending city
     * @return The created siege flag or null if failed
     */
    private SiegeFlag createSiegeFlag(Location location, City attackerCity, City targetCity, Player attackerPlayer) {
        World world = location.getWorld();
        Block block = world.getBlockAt(location);

        // Check if the block can be replaced
        if (!block.isEmpty() && !block.isLiquid()) {
            return null;
        }

        // Create a physical flag in the world using the configured item
        // Check if the block contains a siege flag item

        // Implementar validación con el plugin de items elegido
        // (ExecutableItems/ItemEditor/custom)
        if (!ItemUtils.isSiegeFlag(plugin, null)) {
            return null;
        }
        // Implementar validación con el plugin de items elegido
        // (ExecutableItems/ItemEditor/custom)
        return new SiegeFlag(attackerCity.getId(), targetCity.getId(), attackerPlayer, location, (int)siegeDuration);
    }

    /**
     * Starts the timer for siege duration.
     * 
     * @param targetCityId   Target city ID
     * @param attackerCityId Attacker city ID
     */
    private void startSiegeTimer(UUID targetCityId, UUID attackerCityId) {
        City targetCity = cityManager.getCity(targetCityId);
        Location flagLocation = siegeFlags.get(targetCityId);

        new SiegeTimerTask(plugin, targetCity.getName(), flagLocation).runTaskLater(plugin, siegeDuration * 20);
    }

    /**
     * Schedules fireworks at the flag location.
     * 
     * @param location     Flag location
     * @param targetCityId Target city ID
     */
    private void scheduleFireworks(Location location, UUID targetCityId) {
        // Schedule repeating task to launch fireworks
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            // Check if siege is still active
            if (!siegeStates.getOrDefault(targetCityId, SiegeState.NONE).equals(SiegeState.ACTIVE)) {
                task.cancel();
                return;
            }

            // Launch firework
            FireworkUtils.createSiegeStartFirework(location);
        }, fireworkInterval * 20, fireworkInterval * 20);
    }

    /**
     * Notifies all players in both cities about siege start.
     * 
     * @param attackerCity Attacker city
     * @param targetCity   Target city
     */
    private void notifySiegeStart(City attackerCity, City targetCity) {
        // Notify attacking city
        for (UUID citizenId : citizenManager.getCitizensInCity(attackerCity.getId())) {
            Player player = Bukkit.getPlayer(citizenId);
            if (player != null && player.isOnline()) {
                MessageUtils.sendTitle(player, "§4¡ASEDIO INICIADO!", "Atacando a " + targetCity.getName(), 10, 70, 20);
            }
        }

        // Notify defending city
        for (UUID citizenId : citizenManager.getCitizensInCity(targetCity.getId())) {
            Player player = Bukkit.getPlayer(citizenId);
            if (player != null && player.isOnline()) {
                MessageUtils.sendTitle(player, "§c¡ESTÁS BAJO ATAQUE!", attackerCity.getName() + " te está asediando",
                        10, 70, 20);
            }
        }
    }

    /**
     * Ends a siege with a specified outcome.
     * 
     * @param targetCityId Target city ID
     * @param siegeState   The final state of the siege
     */
    public void endSiege(UUID targetCityId, SiegeState siegeState) {
        if (!siegeStates.containsKey(targetCityId)) {
            return;
        }

        UUID attackerCityId = activeSieges.get(targetCityId);
        City targetCity = cityManager.getCity(targetCityId);
        City attackerCity = cityManager.getCity(attackerCityId);

        if (targetCity == null || attackerCity == null) {
            return;
        }

        // Handle different siege outcomes
        switch (siegeState) {
            case DEFENDED:
                handleSiegeVictory(attackerCity, targetCity);
                break;
            case FLAG_CAPTURED:
                handleSiegeDefeat(attackerCity, targetCity);
                break;
            case CANCELLED:
                handleSiegeExpired(attackerCity, targetCity);
                break;
            default:
                break;
        }

        // Clean up
        activeSieges.remove(targetCityId);
        siegeStates.remove(targetCityId);

        // Remove the flag from the world if it still exists
        Location flagLocation = siegeFlags.remove(targetCityId);
        if (flagLocation != null) {
            ItemUtils.removeSiegeFlag(flagLocation.getBlock());
        }

        // Restore region settings
        regionManager.setSiegePvPEnabled(targetCity, false);
        regionManager.setSiegeSackingEnabled(targetCity, false);

        // Set cooldown
        String cooldownKey = getCooldownKey(attackerCityId, targetCityId);
        siegeCooldowns.put(cooldownKey, System.currentTimeMillis() + (cooldownDuration * 1000));

        // Schedule cooldown task
        new SiegeCooldownTask(plugin, this, cooldownKey, cooldownDuration).runTaskLater(plugin, cooldownDuration * 20);

        // Save state
        saveSieges();

        // Trigger event (would be implemented via API)
        // plugin.getAPI().fireEvent(new SiegeEndEvent(attackerCity, targetCity,
        // siegeState));
    }

    /**
     * Handles victory scenario when attackers capture the flag.
     * 
     * @param attackerCity Attacker city
     * @param targetCity   Target city
     */
    private void handleSiegeVictory(City attackerCity, City targetCity) {
        // Enable sacking mode
        regionManager.setSiegeSackingEnabled(targetCity, true);
        siegeStates.put(targetCity.getId(), SiegeState.SACKING);

        // Transfer 50% of target city's funds
        double targetFunds = economyManager.getCityBankBalance(targetCity);
        double transferAmount = targetFunds * 0.5;
        economyManager.transferCityBankFunds(targetCity, attackerCity, transferAmount);

        // Notify all players
        String message = "§4¡La ciudad " + attackerCity.getName() + " ha conquistado a " + targetCity.getName() + "!";
        String subtitle = "§cFase de saqueo: 5 minutos";

        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendTitle(player, message, subtitle, 10, 70, 20);
        }

        // Schedule end of sacking phase
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endSackingPhase(targetCity.getId());
        }, sackingDuration * 20);
    }

    /**
     * Handles defeat scenario when defenders destroy the flag.
     * 
     * @param attackerCity Attacker city
     * @param targetCity   Target city
     */
    private void handleSiegeDefeat(City attackerCity, City targetCity) {
        // Notify all players
        String message = "§2¡La ciudad " + targetCity.getName() + " ha repelido el ataque de " + attackerCity.getName()
                + "!";

        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendTitle(player, message, "", 10, 70, 20);
        }
    }

    /**
     * Handles case where siege timer expires.
     * 
     * @param attackerCity Attacker city
     * @param targetCity   Target city
     */
    private void handleSiegeExpired(City attackerCity, City targetCity) {
        // Notify all players
        String message = "§6El asedio de " + attackerCity.getName() + " contra " + targetCity.getName()
                + " ha expirado";

        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendTitle(player, message, "", 10, 70, 20);
        }
    }

    /**
     * Ends the sacking phase.
     * 
     * @param targetCityId Target city ID
     */
    private void endSackingPhase(UUID targetCityId) {
        if (!siegeStates.containsKey(targetCityId) ||
                siegeStates.get(targetCityId) != SiegeState.SACKING) {
            return;
        }

        City targetCity = cityManager.getCity(targetCityId);
        if (targetCity == null) {
            return;
        }

        // Disable sacking mode
        regionManager.setSiegeSackingEnabled(targetCity, false);

        // Clean up
        siegeStates.remove(targetCityId);

        // Notify players
        String message = "§6La fase de saqueo en " + targetCity.getName() + " ha terminado";

        for (UUID citizenId : citizenManager.getCitizensInCity(targetCityId)) {
            Player player = Bukkit.getPlayer(citizenId);
            if (player != null && player.isOnline()) {
                MessageUtils.sendTitle(player, message, "", 10, 70, 20);
            }
        }
    }

    /**
     * Checks if a flag capture is possible.
     * 
     * @param player Player attempting to capture
     * @param city   City whose flag is being captured
     * @return true if capture is possible
     */
    public boolean canCaptureFlag(Player player, City city) {
        if (player == null || city == null) {
            return false;
        }

        // Check if city is under siege
        if (!siegeStates.containsKey(city.getId()) ||
                siegeStates.get(city.getId()) != SiegeState.ACTIVE) {
            return false;
        }

        // Check if player is from attacking city
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
        UUID attackerCityId = activeSieges.get(city.getId());

        return playerCity != null && playerCity.getId().equals(attackerCityId);
    }

    /**
     * Processes a flag capture.
     * 
     * @param player Player capturing the flag
     * @param city   City whose flag is being captured
     */
    public void captureFlag(Player player, City city) {
        if (!canCaptureFlag(player, city)) {
            return;
        }

        // End siege with victory
        endSiege(city.getId(), SiegeState.VICTORY);
    }

    /**
     * Checks if a city is under siege.
     * 
     * @param cityId City ID
     * @return true if city is under siege
     */
    public boolean isUnderSiege(UUID cityId) {
        return siegeStates.containsKey(cityId);
    }

    /**
     * Gets the state of a siege.
     * 
     * @param cityId City ID
     * @return Siege state or NONE if not under siege
     */
    public SiegeState getSiegeState(UUID cityId) {
        return siegeStates.getOrDefault(cityId, SiegeState.NONE);
    }

    /**
     * Creates a cooldown key.
     * 
     * @param attackerCityId Attacker city ID
     * @param targetCityId   Target city ID
     * @return Cooldown key string
     */
    private String getCooldownKey(UUID attackerCityId, UUID targetCityId) {
        return attackerCityId.toString() + ":" + targetCityId.toString();
    }

    /**
     * Removes a cooldown.
     * 
     * @param cooldownKey Cooldown key
     */
    public void removeCooldown(String cooldownKey) {
        siegeCooldowns.remove(cooldownKey);
        saveSieges();
    }

    /**
     * Loads siege data from configuration file.
     */
    private void loadSieges() {
        activeSieges.clear();
        siegeStates.clear();
        siegeFlags.clear();
        siegeCooldowns.clear();

        if (!siegesFile.exists()) {
            plugin.saveResource("sieges.yml", false);
        }

        siegesConfig = YamlConfiguration.loadConfiguration(siegesFile);

        // Load active sieges
        if (siegesConfig.contains("activeSieges")) {
            for (String targetCityIdStr : siegesConfig.getConfigurationSection("activeSieges").getKeys(false)) {
                try {
                    UUID targetCityId = UUID.fromString(targetCityIdStr);
                    UUID attackerCityId = UUID.fromString(siegesConfig.getString("activeSieges." + targetCityIdStr));

                    // Validate cities exist
                    if (cityManager.getCity(targetCityId) == null || cityManager.getCity(attackerCityId) == null) {
                        continue;
                    }

                    activeSieges.put(targetCityId, attackerCityId);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load siege: " + targetCityIdStr, e);
                }
            }
        }

        // Load siege states
        if (siegesConfig.contains("siegeStates")) {
            for (String targetCityIdStr : siegesConfig.getConfigurationSection("siegeStates").getKeys(false)) {
                try {
                    UUID targetCityId = UUID.fromString(targetCityIdStr);
                    String stateStr = siegesConfig.getString("siegeStates." + targetCityIdStr);
                    SiegeState state = SiegeState.valueOf(stateStr);

                    siegeStates.put(targetCityId, state);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load siege state: " + targetCityIdStr, e);
                }
            }
        }

        // Load siege flags
        if (siegesConfig.contains("siegeFlags")) {
            for (String targetCityIdStr : siegesConfig.getConfigurationSection("siegeFlags").getKeys(false)) {
                try {
                    UUID targetCityId = UUID.fromString(targetCityIdStr);
                    String worldName = siegesConfig.getString("siegeFlags." + targetCityIdStr + ".world");
                    double x = siegesConfig.getDouble("siegeFlags." + targetCityIdStr + ".x");
                    double y = siegesConfig.getDouble("siegeFlags." + targetCityIdStr + ".y");
                    double z = siegesConfig.getDouble("siegeFlags." + targetCityIdStr + ".z");

                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location location = new Location(world, x, y, z);
                        siegeFlags.put(targetCityId, location);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load siege flag: " + targetCityIdStr, e);
                }
            }
        }

        // Load cooldowns
        if (siegesConfig.contains("cooldowns")) {
            for (String key : siegesConfig.getConfigurationSection("cooldowns").getKeys(false)) {
                try {
                    long endTime = siegesConfig.getLong("cooldowns." + key);

                    // Skip expired cooldowns
                    if (System.currentTimeMillis() < endTime) {
                        siegeCooldowns.put(key, endTime);

                        // Schedule remaining cooldown
                        long remainingSeconds = (endTime - System.currentTimeMillis()) / 1000;
                        new SiegeCooldownTask(plugin, this, key, remainingSeconds).runTaskLater(plugin,
                                remainingSeconds * 20);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load cooldown: " + key, e);
                }
            }
        }

        plugin.getLogger().info("Loaded " + activeSieges.size() + " active sieges");
    }

    /**
     * Saves siege data to configuration file.
     */
    public void saveSieges() {
        siegesConfig = new YamlConfiguration();

        // Save active sieges
        for (Map.Entry<UUID, UUID> entry : activeSieges.entrySet()) {
            siegesConfig.set("activeSieges." + entry.getKey().toString(), entry.getValue().toString());
        }

        // Save siege states
        for (Map.Entry<UUID, SiegeState> entry : siegeStates.entrySet()) {
            siegesConfig.set("siegeStates." + entry.getKey().toString(), entry.getValue().name());
        }

        // Save siege flags
        for (Map.Entry<UUID, Location> entry : siegeFlags.entrySet()) {
            Location loc = entry.getValue();
            siegesConfig.set("siegeFlags." + entry.getKey().toString() + ".world", loc.getWorld().getName());
            siegesConfig.set("siegeFlags." + entry.getKey().toString() + ".x", loc.getX());
            siegesConfig.set("siegeFlags." + entry.getKey().toString() + ".y", loc.getY());
            siegesConfig.set("siegeFlags." + entry.getKey().toString() + ".z", loc.getZ());
        }

        // Save cooldowns
        for (Map.Entry<String, Long> entry : siegeCooldowns.entrySet()) {
            siegesConfig.set("cooldowns." + entry.getKey(), entry.getValue());
        }

        try {
            siegesConfig.save(siegesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save sieges", e);
        }
    }
}
