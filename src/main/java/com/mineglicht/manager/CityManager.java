package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
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

public class CityManager {

    private final cityWars plugin;
    private final Map<UUID, City> cities;
    private final File citiesFile;
    private FileConfiguration citiesConfig;

    public CityManager(cityWars plugin) {
        this.plugin = plugin;
        this.cities = new HashMap<>();
        this.citiesFile = new File(plugin.getDataFolder(), "cities.yml");

        loadCities();
    }


    /**
     * Crear una ciudad con un nombre y una localizacion central
     *
     * @param name Nombre de la ciudad
     * @param centerLocation Centro de la localizacion de la ciudad
     * @param taxRate Cantidad de impuestos (por defecto 0.0)
     * @return Retorna la ciudad y la guarda
     */
    public City createCity(String name, Location centerLocation, Double taxRate) {
        // Validad si el nombre ya existe
        if (getCityByName(name) != null) {
            return null;
        }

        // Define el tamaño por default de la ciudad
        int defaultCityRadius = 50; // 50 bloques en todas direcciones desde el centro

        // Calcular min y max points en base al centro
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

        // Creamos la ciudad
        World world = centerLocation.getWorld();
        City city = new City(name, world, minPoint, maxPoint, taxRate);

//        // Creamos la cuenta bancaria de la ciudad
//        boolean bankCreated = economyManager.createCityBank(city);
//        if (!bankCreated) {
//            // Limpiamos la region si falla la creacion del banco
//            return null;
//        }

        cities.put(city.getId(), city);
        saveCities();

        return city;
    }

    public boolean deleteCity(City city) {
        if (city == null || !cities.containsKey(city.getId())) {
            return false;
        }

//        // Eliminar la region de la ciudad
//        regionManager.removeRegion(city.getId());

//        // Eliminamos la cuenta bancaria de la ciudad
//        economyManager.deleteCityBank(city.getId());

        // Eliminamos la ciudad del map (Lista de ciudades) y guardamos
        cities.remove(city.getId());
        saveCities();

        return true;
    }

//    /**
//     * Expandir el territorio de la ciudad en la direccion que el jugador mira
//     *
//     * @param city Ciudad donde se va a expandir el territorio
//     * @param player Jugador que mira a una direcion
//     * @param blocks Cantidad de bloques a expandir
//     * @return Retorna true si se pudo expandir
//     */
//    public boolean expandCity(City city, Player player, int blocks) {
//        if (city == null || !cities.containsKey(city.getId())) {
//            return false;
//        }
//
//        boolean expanded = regionManager.expandCityRegion(city, player, blocks);
//
//        if (expanded) {
//            saveCities();
//        }
//
//        return expanded;
//    }

    /**
     * Obtenemos una ciudad por UUID
     *
     * @param cityId UUID de la ciudad
     * @return Retorna la ciudad
     */
    public City getCity(UUID cityId) {
        return cities.get(cityId);
    }

    /**
     * Obtener ciudad por nombre
     *
     * @param name Nombre de Ciudad
     * @return Retorna la ciudad si coincide con alguno que ya tenga guardado en su lista
     */
    public City getCityByName(String name) {
        for (City city : cities.values()) {
            if (city.getName().equalsIgnoreCase(name)) {
                return city;
            }
        }
        return null;
    }

    /**
     * Verificar si un jugador es ciudadano de una ciudad
     *
     * @param cityId UUID de la ciudad
     * @param citizenId UUID del jugador
     * @return Retorna
     */
    public boolean isCitizenByCity(UUID cityId, UUID citizenId) {
        City city = getCity(cityId);
        return city != null && city.isCitizen(citizenId);
    }

    /**
     * Obtener la cantidad de ciudadanos de la ciudad
     *
     * @param cityId UUID de la ciudad
     * @return Retorna la cantidad de ciudadanos que tiene la ciudad
     */
    public int getCitizenCount(UUID cityId) {
        City city = getCity(cityId);
        return city != null ? city.getCitizenCount() : 0;
    }

    // Metodos para gestionar los fondos bancarios de la ciudad

    /**
     * Obtener el balance total de la cuenta de la Ciudad
     *
     * @param cityId UUID de la ciudad
     * @return Retorna el balance de la ciudad
     */
    public double getBankBalance(UUID cityId) {
        City city = getCity(cityId);
        return city != null ? city.getBankBalance() : 0.0;
    }

    /**
     * Cambiar la cantidad del balance de la cuenta de la ciudad
     *
     * @param cityId UUID de la ciudad
     * @param amount Cantidad por la que se cambia el balance
     */
    public void setBankBalance(UUID cityId, double amount) {
        City city = getCity(cityId);
        if (city != null) {
            city.setBankBalance(amount);
        }
    }

    /**
     * Cambiar el nivel de la ciudad
     *
     * @param cityId UUID de la ciudad
     * @param level Nivel a cambiar de la ciudad
     */
    public void setCityLevel(UUID cityId, int level) {
        City city = getCity(cityId);
        if (city != null) {
            city.setLevel(level);
        }
    }

    /**
     * Cambiar el estado de asedio de la ciudad
     *
     * @param cityId UUID de la ciudad
     * @param siegeState Estado de asedio a cambiar de la ciudad
     */
    public void setSiegeState(UUID cityId, SiegeState siegeState) {
        City city = getCity(cityId);
        if (city != null) {
            city.setSiegeState(siegeState);
        }
    }

    /**
     * Cargar las ciudades desde el archivo "cities.yml"
     */
    public void loadCities() {
        cities.clear();

        if (!citiesFile.exists()) {
            plugin.saveResource("cities.yml", false);
        }

        citiesConfig = YamlConfiguration.loadConfiguration(citiesFile);

        for (String idStr : citiesConfig.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String name = citiesConfig.getString(idStr + ".name");
                Location center = LocationUtils.stringToLocation(citiesConfig.getString(idStr + ".center"));

                // Cargar min y max points
                Vector minPoint = LocationUtils.stringToVector(citiesConfig.getString(idStr + ".minPoint"));
                Vector maxPoint = LocationUtils.stringToVector(citiesConfig.getString(idStr + ".maxPoint"));

                // Usar el constructor correcto
                double taxRate = 0.18; // Valor por defecto o cargado desde config
                assert center != null; // Valor por defecto o cargado desde config
                City city = new City(id, name, center.getWorld(), minPoint, maxPoint, center, taxRate);

                // Cargar ciudadanos
                List<String> citizenStrings = citiesConfig.getStringList(idStr + ".citizens");
                for (String citizenStr : citizenStrings) {
                    try {
                        city.addCitizen(UUID.fromString(citizenStr));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid citizen UUID: " + citizenStr);
                    }
                }

                // Cargar otras propiedades
                city.setBankBalance(citiesConfig.getDouble(idStr + ".bankBalance", 0.0));
                city.setLevel(citiesConfig.getInt(idStr + ".level", 1));

                cities.put(id, city);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load city: " + idStr, e);
            }
        }

        plugin.getLogger().info("Loaded " + cities.size() + " cities");
    }

    /**
     * Guardar las ciudades en el archivo "cities.yml"
     */
    public void saveCities() {
        citiesConfig = new YamlConfiguration();

        for (Map.Entry<UUID, City> entry : cities.entrySet()) {
            UUID id = entry.getKey();
            City city = entry.getValue();
            String idStr = id.toString();

            citiesConfig.set(idStr + ".name", city.getName());
            citiesConfig.set(idStr + ".center", LocationUtils.locationToString(city.getCenter()));
            citiesConfig.set(idStr + ".minPoint", LocationUtils.vectorToString(city.getMinPoint()));
            citiesConfig.set(idStr + ".maxPoint", LocationUtils.vectorToString(city.getMaxPoint()));

            // Guardar ciudadanos
            List<String> citizenStrings = city.getCitizens().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            citiesConfig.set(idStr + ".citizens", citizenStrings);

            // Guardar otras propiedades
            citiesConfig.set(idStr + ".bankBalance", city.getBankBalance());
            citiesConfig.set(idStr + ".level", city.getLevel());
        }

        try {
            citiesConfig.save(citiesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save cities", e);
        }
    }
}
