package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.config.Settings;
import com.mineglicht.models.City;
import com.mineglicht.models.CityFlag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gestor de regiones para las ciudades.
 * Maneja la creación, expansión y protección de las regiones que definen los
 * límites de las ciudades.
 */
public class RegionManager {

    private final cityWars plugin;
    private final CityManager cityManager;

    // Almacena las regiones como puntos min/max por ciudad
    private final Map<UUID, RegionBounds> cityRegions = new HashMap<>();

    public RegionManager(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        loadRegions();
    }

    /**
     * Carga todas las regiones de ciudades desde la configuración
     */
    private void loadRegions() {
        FileConfiguration config = plugin.getConfig();
        if (config == null)
            return;

        for (String cityIdStr : config.getConfigurationSection("regions").getKeys(false)) {
            try {
                UUID cityId = UUID.fromString(cityIdStr);
                String worldName = config.getString("regions." + cityIdStr + ".world");
                World world = plugin.getServer().getWorld(worldName);

                if (world != null) {
                    Location min = new Location(
                            world,
                            config.getDouble("regions." + cityIdStr + ".min.x"),
                            config.getDouble("regions." + cityIdStr + ".min.y"),
                            config.getDouble("regions." + cityIdStr + ".min.z"));

                    Location max = new Location(
                            world,
                            config.getDouble("regions." + cityIdStr + ".max.x"),
                            config.getDouble("regions." + cityIdStr + ".max.y"),
                            config.getDouble("regions." + cityIdStr + ".max.z"));

                    cityRegions.put(cityId, new RegionBounds(min, max));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando región para ciudad: " + cityIdStr);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Cargadas " + cityRegions.size() + " regiones de ciudades");
    }

    // METODO PARA ACTUALIAAZR LAS FLAGS
    public void updateCityRegionFlags(City city) {
        // Este método puede estar vacío por ahora o simplemente guardar
        saveRegions();
    }

    public boolean isInCityRegion(Location location, City city) {
        return isInCity(location, city.getId());
    }

    /**
     * Guarda todas las regiones de ciudades en la configuración
     */
    public void saveRegions() {
        FileConfiguration config = plugin.getConfig();

        // Limpia la sección de regiones
        config.set("regions", null);

        // Guarda cada región
        for (Map.Entry<UUID, RegionBounds> entry : cityRegions.entrySet()) {
            UUID cityId = entry.getKey();
            RegionBounds bounds = entry.getValue();

            String path = "regions." + cityId.toString();
            config.set(path + ".world", bounds.getMin().getWorld().getName());

            config.set(path + ".min.x", bounds.getMin().getX());
            config.set(path + ".min.y", bounds.getMin().getY());
            config.set(path + ".min.z", bounds.getMin().getZ());

            config.set(path + ".max.x", bounds.getMax().getX());
            config.set(path + ".max.y", bounds.getMax().getY());
            config.set(path + ".max.z", bounds.getMax().getZ());
        }

        plugin.saveConfig();
    }

    /**
     * Crea una nueva región para una ciudad
     * 
     * @param city   La ciudad
     * @param center El centro de la región
     * @param radius El radio inicial de la región
     * @return true si se creó correctamente
     */
    public boolean createCityRegion(City city, Location center, int radius) {
        if (city == null || center == null)
            return false;

        int minY = Settings.MIN_REGION_Y;
        int maxY = Settings.MAX_REGION_Y;

        Location min = new Location(
                center.getWorld(),
                center.getX() - radius,
                minY,
                center.getZ() - radius);

        Location max = new Location(
                center.getWorld(),
                center.getX() + radius,
                maxY,
                center.getZ() + radius);

        // Verificar colisiones con otras regiones
        if (overlapsWithExistingRegion(min, max, null)) {
            return false;
        }

        cityRegions.put(city.getId(), new RegionBounds(min, max));
        saveRegions();
        return true;
    }

    /**
     * Expande la región de una ciudad en la dirección en que mira el jugador
     * 
     * @param city   La ciudad a expandir
     * @param player El jugador que realiza la expansión
     * @param blocks Número de bloques a expandir
     * @return true si se expandió correctamente
     */
    public boolean expandCityRegion(City city, Player player, int blocks) {
        if (city == null || player == null || blocks <= 0)
            return false;
        if (!cityRegions.containsKey(city.getId()))
            return false;

        RegionBounds currentBounds = cityRegions.get(city.getId());
        RegionBounds newBounds = new RegionBounds(
                currentBounds.getMin().clone(),
                currentBounds.getMax().clone());

        // Determinar dirección de expansión basado en donde mira el jugador
        float yaw = player.getLocation().getYaw();

        // Normalizar yaw a 0-360
        while (yaw < 0)
            yaw += 360;
        while (yaw > 360)
            yaw -= 360;

        // Expandir en la dirección apropiada
        if (yaw >= 315 || yaw < 45) {
            // Sur (+Z)
            newBounds.getMax().add(0, 0, blocks);
        } else if (yaw >= 45 && yaw < 135) {
            // Oeste (-X)
            newBounds.getMin().add(-blocks, 0, 0);
        } else if (yaw >= 135 && yaw < 225) {
            // Norte (-Z)
            newBounds.getMin().add(0, 0, -blocks);
        } else {
            // Este (+X)
            newBounds.getMax().add(blocks, 0, 0);
        }

        // Verificar que la nueva región no colisione con otras
        if (overlapsWithExistingRegion(newBounds.getMin(), newBounds.getMax(), city.getId())) {
            return false;
        }

        // Aplicar expansión
        cityRegions.put(city.getId(), newBounds);
        saveRegions();
        return true;
    }

    /**
     * Verifica si una ubicación está dentro de alguna región de ciudad
     * 
     * @param location La ubicación a verificar
     * @return El ID de la ciudad, o null si no está en ninguna
     */
    public UUID getCityAtLocation(Location location) {
        for (Map.Entry<UUID, RegionBounds> entry : cityRegions.entrySet()) {
            if (isInRegion(location, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Verifica si una ubicación está dentro de una región específica
     * 
     * @param location La ubicación a verificar
     * @param cityId   El ID de la ciudad a comprobar
     * @return true si la ubicación está dentro de la región de la ciudad
     */
    public boolean isInCity(Location location, UUID cityId) {
        if (!cityRegions.containsKey(cityId))
            return false;
        return isInRegion(location, cityRegions.get(cityId));
    }

    /**
     * Elimina la región de una ciudad
     * 
     * @param cityId El ID de la ciudad
     */
    public void removeRegion(UUID cityId) {
        cityRegions.remove(cityId);
        saveRegions();
    }

    /**
     * Verifica si un bloque está protegido por una bandera de ciudad
     * 
     * @param block El bloque a verificar
     * @param flag  La bandera de protección a comprobar
     * @return true si el bloque está protegido
     */
    public boolean isBlockProtected(Block block, CityFlag flag) {
        UUID cityId = getCityAtLocation(block.getLocation());
        if (cityId == null)
            return false;

        City city = cityManager.getCity(cityId);
        return city != null && city.hasFlag(flag);
    }

    /**
     * Comprueba si dos regiones se solapan
     * 
     * @param min1          Punto mínimo de la primera región
     * @param max1          Punto máximo de la primera región
     * @param excludeCityId ID de ciudad a excluir (para expansiones)
     * @return true si hay solapamiento
     */
    private boolean overlapsWithExistingRegion(Location min1, Location max1, UUID excludeCityId) {
        for (Map.Entry<UUID, RegionBounds> entry : cityRegions.entrySet()) {
            if (excludeCityId != null && entry.getKey().equals(excludeCityId)) {
                continue;
            }

            RegionBounds bounds = entry.getValue();
            Location min2 = bounds.getMin();
            Location max2 = bounds.getMax();

            // Verificar si las regiones están en mundos diferentes
            if (!min1.getWorld().equals(min2.getWorld())) {
                continue;
            }

            // Verificar si una región está completamente fuera de la otra
            if (max1.getX() < min2.getX() || min1.getX() > max2.getX())
                continue;
            if (max1.getY() < min2.getY() || min1.getY() > max2.getY())
                continue;
            if (max1.getZ() < min2.getZ() || min1.getZ() > max2.getZ())
                continue;

            // Si llegamos aquí, hay solapamiento
            return true;
        }

        return false;
    }

    /**
     * Verifica si una ubicación está dentro de una región
     * 
     * @param location La ubicación a verificar
     * @param bounds   Los límites de la región
     * @return true si la ubicación está dentro de la región
     */
    private boolean isInRegion(Location location, RegionBounds bounds) {
        if (!location.getWorld().equals(bounds.getMin().getWorld()))
            return false;

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= bounds.getMin().getX() && x <= bounds.getMax().getX() &&
                y >= bounds.getMin().getY() && y <= bounds.getMax().getY() &&
                z >= bounds.getMin().getZ() && z <= bounds.getMax().getZ();
    }

    /**
     * Obtiene el tamaño de una región de ciudad
     * 
     * @param cityId El ID de la ciudad
     * @return El tamaño de la región en bloques cúbicos, o 0 si la ciudad no existe
     */
    public long getRegionSize(UUID cityId) {
        if (!cityRegions.containsKey(cityId))
            return 0;

        RegionBounds bounds = cityRegions.get(cityId);
        long dx = (long) (bounds.getMax().getX() - bounds.getMin().getX());
        long dy = (long) (bounds.getMax().getY() - bounds.getMin().getY());
        long dz = (long) (bounds.getMax().getZ() - bounds.getMin().getZ());

        return dx * dy * dz;
    }

    /**
     * Obtiene todos los IDs de ciudades con regiones
     * 
     * @return Conjunto de IDs de ciudades
     */
    public Set<UUID> getAllRegionCityIds() {
        return cityRegions.keySet();
    }

    /**
     * Clase interna para representar los límites de una región
     */
    private static class RegionBounds {
        private final Location min;
        private final Location max;

        public RegionBounds(Location min, Location max) {
            this.min = min;
            this.max = max;
        }

        public Location getMin() {
            return min;
        }

        public Location getMax() {
            return max;
        }
    }
}