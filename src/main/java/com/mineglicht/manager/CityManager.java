package com.mineglicht.manager;

import com.mineglicht.models.City;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.math.BlockVector3;
import me.xanium.gemseconomy.api.GemsEconomyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CityManager {
    
    private final JavaPlugin plugin;
    private final Map<String, City> cities;
    private final Map<UUID, String> playerCities;
    private final File citiesFile;
    private FileConfiguration citiesConfig;
    
    // Configuración
    private int initialCitySize;
    private double expansionCost;
    private String expansionCurrency;
    private int maxPlayersPerCity;
    private LocalTime taxCollectionTime;
    private double defaultTaxRate;
    private int maxCityLevel;
    private double levelUpCostMultiplier;
    
    public CityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cities = new ConcurrentHashMap<>();
        this.playerCities = new ConcurrentHashMap<>();
        this.citiesFile = new File(plugin.getDataFolder(), "cities.yml");
        
        loadConfiguration();
        loadCities();
        startTaxCollectionScheduler();
    }
    
    /**
     * Carga la configuración del plugin
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        
        // Configuración de ciudades
        this.initialCitySize = config.getInt("cities.initial-size", 50);
        this.expansionCost = config.getDouble("cities.expansion-cost", 1000.0);
        this.expansionCurrency = config.getString("cities.expansion-currency", "gems");
        this.maxPlayersPerCity = config.getInt("cities.max-players", 20);
        this.defaultTaxRate = config.getDouble("cities.default-tax-rate", 0.05);
        this.maxCityLevel = config.getInt("cities.max-level", 10);
        this.levelUpCostMultiplier = config.getDouble("cities.level-up-cost-multiplier", 1.5);
        
        // Configuración de impuestos
        String taxTimeStr = config.getString("cities.tax-collection-time", "12:00");
        this.taxCollectionTime = LocalTime.parse(taxTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
        
        plugin.getLogger().info("Configuración de ciudades cargada:");
        plugin.getLogger().info("- Tamaño inicial: " + initialCitySize);
        plugin.getLogger().info("- Costo de expansión: " + expansionCost + " " + expansionCurrency);
        plugin.getLogger().info("- Máximo jugadores: " + maxPlayersPerCity);
        plugin.getLogger().info("- Hora de impuestos: " + taxCollectionTime);
    }
    
    /**
     * Crea una nueva ciudad
     */
    public boolean createCity(String name, Player owner, Location location) {
        if (cities.containsKey(name.toLowerCase())) {
            return false; // Ciudad ya existe
        }
        
        if (playerCities.containsKey(owner.getUniqueId())) {
            return false; // El jugador ya tiene una ciudad
        }
        
        if (!isValidCityLocation(location)) {
            return false; // Ubicación no válida
        }
        
        // Crear la ciudad
        City city = new City(name, owner.getUniqueId(), location);
        city.setMaxCitizens(maxPlayersPerCity);
        city.setTaxRate(defaultTaxRate);
        
        // Agregar al mapa
        cities.put(name.toLowerCase(), city);
        playerCities.put(owner.getUniqueId(), name.toLowerCase());
        
        // Crear región de WorldGuard
        createWorldGuardRegion(city);
        
        // Guardar datos
        saveCities();
        
        plugin.getLogger().info("Ciudad '" + name + "' creada por " + owner.getName() + " en " + locationToString(location));
        return true;
    }
    
    /**
     * Valida si una ubicación es válida para crear una ciudad
     */
    private boolean isValidCityLocation(Location location) {
        int minDistance = plugin.getConfig().getInt("cities.min-distance-between-cities", 200);
        
        for (City city : cities.values()) {
            if (city.getCenterLocation().getWorld().equals(location.getWorld())) {
                double distance = city.getCenterLocation().distance(location);
                if (distance < minDistance) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Crea una región de WorldGuard para la ciudad
     */
    private void createWorldGuardRegion(City city) {
        try {
            Location center = city.getCenterLocation();
            World world = center.getWorld();
            
            if (world == null) return;
            
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) return;
            
            // Calcular área de la ciudad
            int size = getCurrentCitySize(city);
            int halfSize = size / 2;
            
            BlockVector3 min = BlockVector3.at(
                    center.getBlockX() - halfSize,
                    0,
                    center.getBlockZ() - halfSize
            );
            
            BlockVector3 max = BlockVector3.at(
                    center.getBlockX() + halfSize,
                    world.getMaxHeight(),
                    center.getBlockZ() + halfSize
            );
            
            // Crear región
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(
                    "city_" + city.getName().toLowerCase(),
                    min,
                    max
            );
            
            // Configurar región
            region.getOwners().addPlayer(city.getOwner());
            region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PVP, false);
            region.setFlag(com.sk89q.worldguard.protection.flags.Flags.BUILD, com.sk89q.worldguard.protection.flags.StateFlag.State.DENY);
            
            // Agregar región
            regionManager.addRegion(region);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando región de WorldGuard para la ciudad " + city.getName(), e);
        }
    }
    
    /**
     * Calcula el tamaño actual de la ciudad basado en expansiones
     */
    private int getCurrentCitySize(City city) {
        int expansionSize = plugin.getConfig().getInt("cities.expansion-size-per-level", 25);
        return initialCitySize + (city.getExpansionCount() * expansionSize);
    }
    
    /**
     * Permite a un jugador unirse a una ciudad
     */
    public boolean joinCity(Player player, String cityName) {
        City city = cities.get(cityName.toLowerCase());
        if (city == null) {
            return false; // Ciudad no existe
        }
        
        if (playerCities.containsKey(player.getUniqueId())) {
            return false; // El jugador ya está en una ciudad
        }
        
        if (city.getCitizens().size() >= city.getMaxCitizens()) {
            return false; // Ciudad llena
        }
        
        // Agregar ciudadano
        city.addCitizen(player.getUniqueId());
        playerCities.put(player.getUniqueId(), cityName.toLowerCase());
        
        // Actualizar región de WorldGuard
        updateWorldGuardRegion(city);
        
        saveCities();
        
        plugin.getLogger().info("Jugador " + player.getName() + " se unió a la ciudad " + cityName);
        return true;
    }
    
    /**
     * Permite a un jugador abandonar su ciudad
     */
    public boolean leaveCity(Player player) {
        String cityName = playerCities.get(player.getUniqueId());
        if (cityName == null) {
            return false; // El jugador no está en una ciudad
        }
        
        City city = cities.get(cityName);
        if (city == null) {
            return false; // Error de consistencia
        }
        
        // Si es el dueño, no puede abandonar (debe transferir o eliminar)
        if (city.getOwner().equals(player.getUniqueId())) {
            return false;
        }
        
        // Remover ciudadano
        city.removeCitizen(player.getUniqueId());
        playerCities.remove(player.getUniqueId());
        
        // Actualizar región de WorldGuard
        updateWorldGuardRegion(city);
        
        saveCities();
        
        plugin.getLogger().info("Jugador " + player.getName() + " abandonó la ciudad " + cityName);
        return true;
    }
    
    /**
     * Actualiza la región de WorldGuard con los miembros actuales
     */
    private void updateWorldGuardRegion(City city) {
        try {
            Location center = city.getCenterLocation();
            World world = center.getWorld();
            
            if (world == null) return;
            
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) return;
            
            ProtectedRegion region = regionManager.getRegion("city_" + city.getName().toLowerCase());
            if (region == null) return;
            
            // Limpiar miembros actuales
            region.getMembers().clear();
            
            // Agregar todos los ciudadanos
            for (UUID citizenId : city.getCitizens()) {
                region.getMembers().addPlayer(citizenId);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error actualizando región de WorldGuard para la ciudad " + city.getName(), e);
        }
    }
    
    /**
     * Expande una ciudad (solo administradores pueden hacerlo gratis)
     */
    public boolean expandCity(String cityName, Player player, boolean isAdmin) {
        City city = cities.get(cityName.toLowerCase());
        if (city == null) {
            return false;
        }
        
        if (!isAdmin && !city.getOwner().equals(player.getUniqueId())) {
            return false; // Solo el dueño o admin puede expandir
        }
        
        // Verificar límites de expansión
        int maxExpansions = plugin.getConfig().getInt("cities.max-expansions-per-level", 2);
        if (city.getExpansionCount() >= (city.getLevel() * maxExpansions)) {
            return false; // Debe subir de nivel primero
        }
        
        // Verificar y cobrar costo (solo si no es admin)
        if (!isAdmin) {
            if (!canAffordExpansion(player)) {
                return false;
            }
            chargeExpansionCost(player);
        }
        
        // Realizar expansión
        city.setExpansionCount(city.getExpansionCount() + 1);
        
        // Actualizar región de WorldGuard
        updateCityRegionSize(city);
        
        saveCities();
        
        plugin.getLogger().info("Ciudad " + cityName + " expandida por " + player.getName() + 
                               (isAdmin ? " (admin)" : " (pagó " + expansionCost + " " + expansionCurrency + ")"));
        return true;
    }
    
    /**
     * Verifica si el jugador puede pagar la expansión
     */
    private boolean canAffordExpansion(Player player) {
        if (expansionCurrency.equalsIgnoreCase("gems")) {
            return GemsEconomyAPI.getBalance(player.getUniqueId()) >= expansionCost;
        }
        // Aquí puedes agregar más tipos de moneda
        return false;
    }
    
    /**
     * Cobra el costo de expansión al jugador
     */
    private void chargeExpansionCost(Player player) {
        if (expansionCurrency.equalsIgnoreCase("gems")) {
            GemsEconomyAPI.withdrawBalance(player.getUniqueId(), expansionCost);
        }
    }
    
    /**
     * Actualiza el tamaño de la región de WorldGuard
     */
    private void updateCityRegionSize(City city) {
        try {
            Location center = city.getCenterLocation();
            World world = center.getWorld();
            
            if (world == null) return;
            
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) return;
            
            // Remover región anterior
            regionManager.removeRegion("city_" + city.getName().toLowerCase());
            
            // Crear nueva región con tamaño actualizado
            createWorldGuardRegion(city);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error actualizando tamaño de región para la ciudad " + city.getName(), e);
        }
    }
    
    /**
     * Sube el nivel de una ciudad
     */
    public boolean levelUpCity(String cityName) {
        City city = cities.get(cityName.toLowerCase());
        if (city == null) {
            return false;
        }
        
        if (city.getLevel() >= maxCityLevel) {
            return false; // Nivel máximo alcanzado
        }
        
        double levelUpCost = calculateLevelUpCost(city.getLevel());
        if (city.getFunds() < levelUpCost) {
            return false; // Fondos insuficientes
        }
        
        // Subir nivel
        city.setFunds(city.getFunds() - levelUpCost);
        city.setLevel(city.getLevel() + 1);
        
        // Aumentar límite de ciudadanos
        int newMaxCitizens = maxPlayersPerCity + (city.getLevel() * 5);
        city.setMaxCitizens(newMaxCitizens);
        
        saveCities();
        
        plugin.getLogger().info("Ciudad " + cityName + " subió al nivel " + city.getLevel());
        return true;
    }
    
    /**
     * Calcula el costo para subir al siguiente nivel
     */
    private double calculateLevelUpCost(int currentLevel) {
        double baseCost = plugin.getConfig().getDouble("cities.base-level-up-cost", 5000.0);
        return baseCost * Math.pow(levelUpCostMultiplier, currentLevel - 1);
    }
    
    /**
     * Elimina una ciudad (solo administradores)
     */
    public boolean deleteCity(String cityName) {
        City city = cities.get(cityName.toLowerCase());
        if (city == null) {
            return false;
        }
        
        // Remover todos los ciudadanos del mapa
        for (UUID citizenId : city.getCitizens()) {
            playerCities.remove(citizenId);
        }
        
        // Remover región de WorldGuard
        removeWorldGuardRegion(city);
        
        // Remover ciudad
        cities.remove(cityName.toLowerCase());
        
        saveCities();
        
        plugin.getLogger().info("Ciudad " + cityName + " eliminada");
        return true;
    }
    
    /**
     * Remueve la región de WorldGuard de una ciudad
     */
    private void removeWorldGuardRegion(City city) {
        try {
            Location center = city.getCenterLocation();
            World world = center.getWorld();
            
            if (world == null) return;
            
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) return;
            
            regionManager.removeRegion("city_" + city.getName().toLowerCase());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error removiendo región de WorldGuard para la ciudad " + city.getName(), e);
        }
    }
    
    /**
     * Inicia el sistema de recolección de impuestos
     */
    private void startTaxCollectionScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                
                // Verificar si es hora de recolectar impuestos (con margen de 1 minuto)
                if (Math.abs(now.getHour() - taxCollectionTime.getHour()) == 0 && 
                    Math.abs(now.getMinute() - taxCollectionTime.getMinute()) <= 1) {
                    
                    collectTaxes();
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L); // Cada minuto
    }
    
    /**
     * Recolecta impuestos de todas las ciudades
     */
    private void collectTaxes() {
        LocalDateTime now = LocalDateTime.now();
        
        for (City city : cities.values()) {
            // Verificar si ya se recolectaron impuestos hoy
            if (city.getLastTaxCollection() != null && 
                city.getLastTaxCollection().toLocalDate().equals(now.toLocalDate())) {
                continue;
            }
            
            double totalTaxes = 0;
            int taxpayers = 0;
            
            for (UUID citizenId : city.getCitizens()) {
                double playerBalance = GemsEconomyAPI.getBalance(citizenId);
                double tax = playerBalance * city.getTaxRate();
                
                if (tax > 0) {
                    GemsEconomyAPI.withdrawBalance(citizenId, tax);
                    totalTaxes += tax;
                    taxpayers++;
                }
            }
            
            // Agregar impuestos al fondo de la ciudad
            city.setFunds(city.getFunds() + totalTaxes);
            city.setLastTaxCollection(now);
            
            plugin.getLogger().info("Impuestos recolectados para " + city.getName() + 
                                   ": " + totalTaxes + " gems de " + taxpayers + " ciudadanos");
        }
        
        saveCities();
    }
    
    /**
     * Carga las ciudades desde el archivo YAML
     */
    private void loadCities() {
        if (!citiesFile.exists()) {
            plugin.getLogger().info("Archivo de ciudades no encontrado, creando uno nuevo...");
            saveCities();
            return;
        }
        
        citiesConfig = YamlConfiguration.loadConfiguration(citiesFile);
        
        if (citiesConfig.getConfigurationSection("cities") == null) {
            plugin.getLogger().info("No hay ciudades guardadas");
            return;
        }
        
        for (String cityName : citiesConfig.getConfigurationSection("cities").getKeys(false)) {
            try {
                City city = loadCityFromConfig(cityName);
                if (city != null) {
                    cities.put(cityName.toLowerCase(), city);
                    
                    // Mapear jugadores
                    for (UUID citizenId : city.getCitizens()) {
                        playerCities.put(citizenId, cityName.toLowerCase());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error cargando ciudad: " + cityName, e);
            }
        }
        
        plugin.getLogger().info("Cargadas " + cities.size() + " ciudades");
    }
    
    /**
     * Carga una ciudad específica desde la configuración
     */
    private City loadCityFromConfig(String cityName) {
        String path = "cities." + cityName + ".";
        
        // Datos básicos
        String name = citiesConfig.getString(path + "name");
        UUID owner = UUID.fromString(citiesConfig.getString(path + "owner"));
        Location location = stringToLocation(citiesConfig.getString(path + "location"));
        
        if (name == null || owner == null || location == null) {
            return null;
        }
        
        // Crear ciudad
        City city = new City(name, owner, location);
        
        // Cargar datos adicionales
        city.setLevel(citiesConfig.getInt(path + "level", 1));
        city.setFunds(citiesConfig.getDouble(path + "funds", 0.0));
        city.setTaxRate(citiesConfig.getDouble(path + "tax-rate", defaultTaxRate));
        city.setMaxCitizens(citiesConfig.getInt(path + "max-citizens", maxPlayersPerCity));
        city.setExpansionCount(citiesConfig.getInt(path + "expansion-count", 0));
        city.setProtected(citiesConfig.getBoolean(path + "protection-enabled", true));
        
        // Cargar fecha de creación
        String creationDateStr = citiesConfig.getString(path + "creation-date");
        if (creationDateStr != null) {
            city.setCreationDate(LocalDateTime.parse(creationDateStr));
        }
        
        // Cargar última recolección de impuestos
        String lastTaxStr = citiesConfig.getString(path + "last-tax-collection");
        if (lastTaxStr != null) {
            city.setLastTaxCollection(LocalDateTime.parse(lastTaxStr));
        }
        
        // Cargar ciudadanos
        List<String> citizenStrings = citiesConfig.getStringList(path + "citizens");
        for (String citizenStr : citizenStrings) {
            try {
                UUID citizenId = UUID.fromString(citizenStr);
                city.addCitizen(citizenId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID inválido para ciudadano: " + citizenStr);
            }
        }
        
        return city;
    }
    
    /**
     * Guarda todas las ciudades en el archivo YAML
     */
    public void saveCities() {
        if (citiesConfig == null) {
            citiesConfig = new YamlConfiguration();
        }
        
        // Limpiar configuración anterior
        citiesConfig.set("cities", null);
        
        // Guardar cada ciudad
        for (City city : cities.values()) {
            saveCityToConfig(city);
        }
        
        try {
            citiesConfig.save(citiesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando ciudades", e);
        }
    }
    
    /**
     * Guarda una ciudad específica en la configuración
     */
    private void saveCityToConfig(City city) {
        String path = "cities." + city.getName().toLowerCase() + ".";
        
        citiesConfig.set(path + "name", city.getName());
        citiesConfig.set(path + "owner", city.getOwner().toString());
        citiesConfig.set(path + "location", locationToString(city.getCenterLocation()));
        citiesConfig.set(path + "level", city.getLevel());
        citiesConfig.set(path + "funds", city.getFunds());
        citiesConfig.set(path + "tax-rate", city.getTaxRate());
        citiesConfig.set(path + "max-citizens", city.getMaxCitizens());
        citiesConfig.set(path + "expansion-count", city.getExpansionCount());
        citiesConfig.set(path + "protection-enabled", city.isProtected());
        citiesConfig.set(path + "creation-date", city.getCreationDate().toString());
        
        if (city.getLastTaxCollection() != null) {
            citiesConfig.set(path + "last-tax-collection", city.getLastTaxCollection().toString());
        }
        
        // Guardar ciudadanos
        List<String> citizenStrings = new ArrayList<>();
        for (UUID citizenId : city.getCitizens()) {
            citizenStrings.add(citizenId.toString());
        }
        citiesConfig.set(path + "citizens", citizenStrings);
    }
    
    /**
     * Convierte una ubicación a string para guardar
     */
    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + 
               location.getX() + "," + 
               location.getY() + "," + 
               location.getZ() + "," + 
               location.getYaw() + "," + 
               location.getPitch();
    }
    
    /**
     * Convierte un string a ubicación para cargar
     */
    private Location stringToLocation(String locationStr) {
        try {
            String[] parts = locationStr.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            
            return new Location(
                world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    // Getters y métodos de utilidad
    public City getCity(String name) {
        return cities.get(name.toLowerCase());
    }
    
    public City getCityByPlayer(UUID playerId) {
        String cityName = playerCities.get(playerId);
        return cityName != null ? cities.get(cityName) : null;
    }
    
    public Collection<City> getAllCities() {
        return cities.values();
    }
    
    public boolean isPlayerInCity(UUID playerId) {
        return playerCities.containsKey(playerId);
    }
    
    public int getCityCount() {
        return cities.size();
    }
    
    public double getNextLevelCost(City city) {
        return calculateLevelUpCost(city.getLevel());
    }
    
    public void shutdown() {
        saveCities();
        plugin.getLogger().info("CityManager guardado y cerrado correctamente");
    }
}