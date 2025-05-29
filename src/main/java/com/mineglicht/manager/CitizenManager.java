package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages all citizen-related operations including adding players to cities,
 * removing players from cities, and checking city membership.
 */
public class CitizenManager {
    private final cityWars plugin;
    private final Map<UUID, Citizen> citizens;
    private final Map<UUID, Set<UUID>> cityToCitizens; // Maps city ID to set of citizen IDs
    private final File citizensFile;
    private FileConfiguration citizensConfig;
    private final CityManager cityManager;

    public CitizenManager(cityWars plugin, CityManager cityManager) {
        this.plugin = plugin;
        this.cityManager = cityManager;
        this.citizens = new HashMap<>();
        this.cityToCitizens = new HashMap<>();
        this.citizensFile = new File(plugin.getDataFolder(), "citizens.yml");
        
        loadCitizens();
    }

    /**
     * Adds a player to a city.
     * 
     * @param playerId The player's UUID
     * @param city The city to add the player to
     * @return true if successfully added
     */
    public boolean addCitizen(UUID playerId, City city) {
        if (city == null || playerId == null) {
            return false;
        }
        
        // Check if player is already in a city
        Citizen existingCitizen = getCitizen(playerId);
        if (existingCitizen != null) {
            removeFromCity(existingCitizen);
        }
        
        // Create new citizen
        Citizen citizen = new Citizen(playerId, city.getId());
        citizens.put(playerId, citizen);
        
        // Add to city-to-citizens mapping
        Set<UUID> cityCitizens = cityToCitizens.getOrDefault(city.getId(), new HashSet<>());
        cityCitizens.add(playerId);
        cityToCitizens.put(city.getId(), cityCitizens);
        
        saveCitizens();
        return true;
    }
    
    /**
     * Removes a player from their city.
     * 
     * @param playerId The player's UUID
     * @return true if successfully removed
     */
    public boolean removeCitizen(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return false;
        }
        
        return removeFromCity(citizen);
    }
    
    /**
     * Helper method to remove a citizen from their city.
     * 
     * @param citizen The citizen to remove
     * @return true if successfully removed
     */
    private boolean removeFromCity(Citizen citizen) {
        UUID cityId = citizen.getCityId();
        UUID playerId = citizen.getPlayerId();
        
        // Remove from citizens map
        citizens.remove(playerId);
        
        // Remove from city-to-citizens mapping
        Set<UUID> cityCitizens = cityToCitizens.get(cityId);
        if (cityCitizens != null) {
            cityCitizens.remove(playerId);
            if (cityCitizens.isEmpty()) {
                cityToCitizens.remove(cityId);
            } else {
                cityToCitizens.put(cityId, cityCitizens);
            }
        }
        
        saveCitizens();
        return true;
    }
    
    /**
     * Changes a player's city.
     * 
     * @param playerId The player's UUID
     * @param newCity The new city
     * @return true if successfully changed
     */
    public boolean changeCitizenCity(UUID playerId, City newCity) {
        // Remove from current city
        boolean removed = removeCitizen(playerId);
        if (!removed && getCitizen(playerId) != null) {
            return false;
        }
        
        // Add to new city
        return addCitizen(playerId, newCity);
    }
    
    /**
     * Gets a citizen by their UUID.
     * 
     * @param playerId The player's UUID
     * @return The citizen or null if not found
     */
    public Citizen getCitizen(UUID playerId) {
        return citizens.get(playerId);
    }
    
    /**
     * Gets a player's city.
     * 
     * @param playerId The player's UUID
     * @return The city or null if player isn't in a city
     */
    public City getPlayerCity(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return null;
        }
        
        return cityManager.getCity(citizen.getCityId());
    }
    
    /**
     * Checks if a player is in a city.
     * 
     * @param playerId The player's UUID
     * @return true if player is in a city
     */
    public boolean isInCity(UUID playerId) {
        return getCitizen(playerId) != null;
    }
    
    /**
     * Checks if a player is in a specific city.
     * 
     * @param playerId The player's UUID
     * @param cityId The city's UUID
     * @return true if player is in the specified city
     */
    public boolean isInCity(UUID playerId, UUID cityId) {
        Citizen citizen = getCitizen(playerId);
        return citizen != null && citizen.getCityId().equals(cityId);
    }
    
    /**
     * Gets all citizens in a city.
     * 
     * @param cityId The city's UUID
     * @return Set of citizen UUIDs
     */
    public Set<UUID> getCitizensInCity(UUID cityId) {
        return Collections.unmodifiableSet(cityToCitizens.getOrDefault(cityId, new HashSet<>()));
    }
    
    /**
     * Gets all online citizens in a city.
     * 
     * @param cityId The city's UUID
     * @return Set of online citizen UUIDs
     */
    public Set<UUID> getOnlineCitizensInCity(UUID cityId) {
        Set<UUID> allCitizens = getCitizensInCity(cityId);
        Set<UUID> onlineCitizens = new HashSet<>();
        
        for (UUID citizenId : allCitizens) {
            Player player = Bukkit.getPlayer(citizenId);
            if (player != null && player.isOnline()) {
                onlineCitizens.add(citizenId);
            }
        }
        
        return onlineCitizens;
    }
    
    /**
     * Calculates the percentage of online citizens in a city.
     * 
     * @param cityId The city's UUID
     * @return Percentage (0.0 to 1.0) of citizens online
     */
    public double getOnlineCitizenPercentage(UUID cityId) {
        Set<UUID> allCitizens = getCitizensInCity(cityId);
        if (allCitizens.isEmpty()) {
            return 0.0;
        }
        
        Set<UUID> onlineCitizens = getOnlineCitizensInCity(cityId);
        return (double) onlineCitizens.size() / allCitizens.size();
    }
    
    /**
     * Checks if a player is an owner or admin of their city.
     * 
     * @param playerId The player's UUID
     * @return true if player is an owner or admin
     */
    public boolean isOwnerOrAdmin(UUID playerId) {
        City city = getPlayerCity(playerId);
        if (city == null) {
            return false;
        }
        
        return city.getOwnerUUID().equals(playerId) || city.getAdminIds().contains(playerId);
    }
    
    /**
     * Promotes a citizen to admin in their city.
     * 
     * @param playerId The player's UUID
     * @return true if successfully promoted
     */
    public boolean promoteToAdmin(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return false;
        }
        
        City city = cityManager.getCity(citizen.getCityId());
        if (city == null || city.getOwnerUUID().equals(playerId)) {
            return false;
        }
        
        Set<UUID> admins = new HashSet<>(city.getAdminIds());
        admins.add(playerId);
        city.setAdminIds(admins);
        
        return true;
    }
    
    /**
     * Demotes an admin to regular citizen.
     * 
     * @param playerId The player's UUID
     * @return true if successfully demoted
     */
    public boolean demoteFromAdmin(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return false;
        }
        
        City city = cityManager.getCity(citizen.getCityId());
        if (city == null || city.getOwnerUUID().equals(playerId)) {
            return false;
        }
        
        Set<UUID> admins = new HashSet<>(city.getAdminIds());
        boolean removed = admins.remove(playerId);
        if (removed) {
            city.setAdminIds(admins);
        }
        
        return removed;
    }
    
    /**
     * Loads citizens from the configuration file.
     */
    private void loadCitizens() {
        citizens.clear();
        cityToCitizens.clear();
        
        if (!citizensFile.exists()) {
            plugin.saveResource("citizens.yml", false);
        }
        
        citizensConfig = YamlConfiguration.loadConfiguration(citizensFile);
        
        for (String playerIdStr : citizensConfig.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                UUID cityId = UUID.fromString(citizensConfig.getString(playerIdStr + ".cityId"));
                long joinDate = citizensConfig.getLong(playerIdStr + ".joinDate", System.currentTimeMillis());
                
                // Validate city exists
                if (cityManager.getCity(cityId) == null) {
                    plugin.getLogger().warning("Skipping citizen " + playerIdStr + " - city does not exist");
                    continue;
                }
                
                // Create citizen
                Citizen citizen = new Citizen(playerId, cityId);
                citizen.setJoinDate(joinDate);
                
                citizens.put(playerId, citizen);
                
                // Update city-to-citizens mapping
                Set<UUID> cityCitizens = cityToCitizens.getOrDefault(cityId, new HashSet<>());
                cityCitizens.add(playerId);
                cityToCitizens.put(cityId, cityCitizens);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load citizen: " + playerIdStr, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + citizens.size() + " citizens");
    }
    
    /**
     * Saves citizens to the configuration file.
     */
    public void saveCitizens() {
        citizensConfig = new YamlConfiguration();
        
        for (Map.Entry<UUID, Citizen> entry : citizens.entrySet()) {
            UUID playerId = entry.getKey();
            Citizen citizen = entry.getValue();
            String playerIdStr = playerId.toString();
            
            citizensConfig.set(playerIdStr + ".cityId", citizen.getCityId().toString());
            citizensConfig.set(playerIdStr + ".joinDate", citizen.getJoinDate());
        }
        
        try {
            citizensConfig.save(citizensFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save citizens", e);
        }
    }
}