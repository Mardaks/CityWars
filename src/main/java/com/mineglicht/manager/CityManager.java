
package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.CityFlag;
import com.mineglicht.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages all city-related operations including creation, deletion,
 * and modification of cities.
 */
public class CityManager {
    private final cityWars plugin;
    private final Map<UUID, City> cities;
    private final File citiesFile;
    private FileConfiguration citiesConfig;
    private final EconomyManager economyManager;
    private final RegionManager regionManager;

    public CityManager(cityWars plugin, EconomyManager economyManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.cities = new HashMap<>();
        this.citiesFile = new File(plugin.getDataFolder(), "cities.yml");
        this.economyManager = economyManager;
        this.regionManager = regionManager;

        loadCities();
    }

    /**
     * Creates a new city with the specified owner, name and center location.
     * 
     * @param owner          The owner of the city
     * @param name           The name of the city
     * @param centerLocation The center location of the city
     * @return The created city or null if creation failed
     */
    public City createCity(Player owner, String name, Location centerLocation) {
        // Check if city name already exists
        if (getCityByName(name) != null) {
            return null;
        }

        // Check if player already owns a city
        if (getCityByOwner(owner.getUniqueId()) != null) {
            return null;
        }

        // Define default city size (you can make this configurable)
        int defaultCityRadius = 50; // 50 blocks in each direction from center

        // Calculate min and max points based on center location
        double centerX = centerLocation.getX();
        double centerY = centerLocation.getY();
        double centerZ = centerLocation.getZ();

        Vector minPoint = new Vector(
                centerX - defaultCityRadius,
                Math.max(centerY - defaultCityRadius, 0), // Don't go below Y=0
                centerZ - defaultCityRadius);

        Vector maxPoint = new Vector(
                centerX + defaultCityRadius,
                Math.min(centerY + defaultCityRadius, 255), // Don't go above build limit
                centerZ + defaultCityRadius);

        // Create city
        World world = centerLocation.getWorld();
        City city = new City(name, owner.getUniqueId(), world, minPoint, maxPoint);

        // Create city region
        boolean regionCreated = regionManager.createCityRegion(city, centerLocation, defaultCityRadius);
        if (!regionCreated) {
            return null;
        }

        // Create city bank account
        boolean bankCreated = economyManager.createCityBank(city);
        if (!bankCreated) {
            // Cleanup region if bank creation fails
            regionManager.removeRegion(city.getId());
            return null;
        }

        // Add city to map
        cities.put(city.getId(), city);
        saveCities();

        return city;
    }

    /**
     * Deletes a city.
     * 
     * @param city The city to delete
     * @return true if deletion was successful
     */
    public boolean deleteCity(City city) {
        if (city == null || !cities.containsKey(city.getId())) {
            return false;
        }

        // Delete city region
        regionManager.removeRegion(city.getId());

        // Delete city bank account
        economyManager.deleteCityBank(city);

        // Remove city from map and save
        cities.remove(city.getId());
        saveCities();

        return true;
    }

    /**
     * Expands a city's territory in the direction the player is looking.
     * 
     * @param city   The city to expand
     * @param player The player determining expansion direction
     * @param blocks Number of blocks to expand
     * @return true if expansion was successful
     */
    public boolean expandCity(City city, Player player, int blocks) {
        if (city == null || !cities.containsKey(city.getId())) {
            return false;
        }

        // Expand region
        boolean expanded = regionManager.expandCityRegion(city, player, blocks);

        if (expanded) {
            saveCities();
        }

        return expanded;
    }

    /**
     * Sets a city flag value.
     * 
     * @param city  The city
     * @param flag  The flag to set
     * @param value The flag value
     */
    public void setCityFlag(City city, CityFlag flag, boolean value) {
        if (city == null || !cities.containsKey(city.getId())) {
            return;
        }

        city.setFlag(flag, value);
        regionManager.updateCityRegionFlags(city);
        saveCities();
    }

    /**
     * Gets a city by its ID.
     * 
     * @param id The city ID
     * @return The city or null if not found
     */
    public City getCity(UUID id) {
        return cities.get(id);
    }

    /**
     * Gets a city by its name.
     * 
     * @param name The city name
     * @return The city or null if not found
     */
    public City getCityByName(String name) {
        for (City city : cities.values()) {
            if (city.getName().equalsIgnoreCase(name)) {
                return city;
            }
        }
        return null;
    }

    public City getCityById(UUID cityId) {
        return getAllCities().stream()
                .filter(city -> city.getId().equals(cityId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a city by its owner's ID.
     * 
     * @param ownerId The owner's ID
     * @return The city or null if not found
     */
    public City getCityByOwner(UUID ownerId) {
        for (City city : cities.values()) {
            if (city.getOwnerUUID().equals(ownerId)) {
                return city;
            }
        }
        return null;
    }

    /**
     * Gets the city at a specific location.
     * 
     * @param location The location
     * @return The city at the location or null if not in a city
     */
    public City getCityAt(Location location) {
        for (City city : cities.values()) {
            if (regionManager.isInCityRegion(location, city)) {
                return city;
            }
        }
        return null;
    }
  
    /**
     * Obtiene la ciudad en una ubicación específica
     *
     * @param location Ubicación
     */
    public City getCityAtLocation(Location location) {
        if (location == null) {
            return null;
        }

        for (City city : getAllCities()) {
            if (city.isInCity(location)) {
                return city;
            }
        }
        return null;
    }

    /**
     * Gets all cities.
     * 
     * @return Collection of all cities
     */
    public Collection<City> getAllCities() {
        return Collections.unmodifiableCollection(cities.values());
    }

    /**
     * Loads cities from the configuration file.
     */
    private void loadCities() {
        cities.clear();

        if (!citiesFile.exists()) {
            plugin.saveResource("cities.yml", false);
        }

        citiesConfig = YamlConfiguration.loadConfiguration(citiesFile);

        for (String idStr : citiesConfig.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String name = citiesConfig.getString(idStr + ".name");
                UUID ownerId = UUID.fromString(citiesConfig.getString(idStr + ".owner"));
                Location center = LocationUtils.stringToLocation(citiesConfig.getString(idStr + ".center"));

                // Cargar min y max points
                Vector minPoint = LocationUtils.stringToVector(citiesConfig.getString(idStr + ".minPoint"));
                Vector maxPoint = LocationUtils.stringToVector(citiesConfig.getString(idStr + ".maxPoint"));

                // Usar el constructor correcto
                double taxRate = 0.18; // Valor por defecto o cargado desde config
                City city = new City(id, name, ownerId, center.getWorld(), minPoint, maxPoint, center, taxRate);

                // Load citizens
                List<String> citizenStrings = citiesConfig.getStringList(idStr + ".citizens");
                for (String citizenStr : citizenStrings) {
                    try {
                        city.addCitizen(UUID.fromString(citizenStr));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid citizen UUID: " + citizenStr);
                    }
                }

                // Load admins
                List<String> adminStrings = citiesConfig.getStringList(idStr + ".admins");
                Set<UUID> adminIds = adminStrings.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toSet());
                city.setAdminIds(adminIds);

                // Load other properties
                city.setBankBalance(citiesConfig.getDouble(idStr + ".bankBalance", 0.0));
                city.setLastTaxCollection(
                        citiesConfig.getLong(idStr + ".lastTaxCollection", System.currentTimeMillis()));
                city.setLevel(citiesConfig.getInt(idStr + ".level", 1));

                // Load flags
                for (CityFlag flag : CityFlag.values()) {
                    boolean value = citiesConfig.getBoolean(idStr + ".flags." + flag.name(), flag.getDefaultValue());
                    city.setFlag(flag, value);
                }

                cities.put(id, city);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load city: " + idStr, e);
            }
        }

        plugin.getLogger().info("Loaded " + cities.size() + " cities");
    }

    /**
     * Saves cities to the configuration file.
     */
    public void saveCities() {
        citiesConfig = new YamlConfiguration();

        for (Map.Entry<UUID, City> entry : cities.entrySet()) {
            UUID id = entry.getKey();
            City city = entry.getValue();
            String idStr = id.toString();

            citiesConfig.set(idStr + ".name", city.getName());
            citiesConfig.set(idStr + ".owner", city.getOwnerUUID().toString());
            citiesConfig.set(idStr + ".center", LocationUtils.locationToString(city.getCenter()));
            citiesConfig.set(idStr + ".minPoint", LocationUtils.vectorToString(city.getMinPoint()));
            citiesConfig.set(idStr + ".maxPoint", LocationUtils.vectorToString(city.getMaxPoint()));

            // Save citizens
            List<String> citizenStrings = city.getCitizens().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            citiesConfig.set(idStr + ".citizens", citizenStrings);

            // Save admins
            List<String> adminStrings = city.getAdminIds().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            citiesConfig.set(idStr + ".admins", adminStrings);

            // Save other properties
            citiesConfig.set(idStr + ".bankBalance", city.getBankBalance());
            citiesConfig.set(idStr + ".lastTaxCollection", city.getLastTaxCollection());
            citiesConfig.set(idStr + ".level", city.getLevel());

            // Save flags
            for (CityFlag flag : CityFlag.values()) {
                citiesConfig.set(idStr + ".flags." + flag.name(), city.hasFlag(flag));
            }
        }

        try {
            citiesConfig.save(citiesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save cities", e);
        }
    }
}