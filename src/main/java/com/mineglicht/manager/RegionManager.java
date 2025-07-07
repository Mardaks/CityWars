package com.mineglicht.manager;

import com.mineglicht.integration.WorldGuardIntegration;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Gestor de regiones para CityWars usando WorldGuard
 * Maneja la creación, modificación y protección de ciudades
 */
public class RegionManager {

    private final Plugin plugin;
    private final Logger logger;
    private final String CITY_REGION_PREFIX = "city_";
    private final Map<String, Boolean> siegeModeRegions;
    private final WorldGuardIntegration worldGuardIntegration;
    private boolean isEnabled;

    public enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN
    }

    // ================== CONSTRUCTORES Y CONFIGURACIÓN ==================

    public RegionManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.siegeModeRegions = new HashMap<>();
        this.isEnabled = false;
        this.worldGuardIntegration = new WorldGuardIntegration();
    }

    public boolean initialize() {
        try {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
                logger.severe("WorldGuard no está habilitado. RegionManager no puede inicializarse.");
                return false;
            }

            if (!plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
                logger.severe("WorldEdit no está habilitado. RegionManager no puede inicializarse.");
                return false;
            }

            this.isEnabled = true;
            logger.info("RegionManager inicializado correctamente.");
            return true;
        } catch (Exception e) {
            logger.severe("Error al inicializar RegionManager: " + e.getMessage());
            return false;
        }
    }

    public void shutdown() {
        siegeModeRegions.clear();
        isEnabled = false;
        logger.info("RegionManager deshabilitado.");
    }

    public void reload() {
        shutdown();
        initialize();
    }

    // ================== GESTIÓN BÁSICA DE REGIONES ==================

    public boolean createCityRegion(String cityName, Location center, int radius) {
        if (!isEnabled || center == null || radius <= 0) {
            return false;
        }

        try {
            World world = center.getWorld();
            if (world == null)
                return false;

            com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(world));

            if (regionManager == null)
                return false;

            String regionId = CITY_REGION_PREFIX + cityName.toLowerCase();

            // Verificar si la región ya existe
            if (regionManager.hasRegion(regionId)) {
                logger.warning("La región de la ciudad " + cityName + " ya existe.");
                return false;
            }

            // Crear los puntos de la región
            BlockVector3 min = BlockVector3.at(
                    center.getBlockX() - radius,
                    0,
                    center.getBlockZ() - radius);
            BlockVector3 max = BlockVector3.at(
                    center.getBlockX() + radius,
                    world.getMaxHeight(),
                    center.getBlockZ() + radius);

            // Crear la región protegida
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);

            // Configurar flags de protección
            region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
            region.setFlag(Flags.PVP, StateFlag.State.DENY);
            region.setFlag(Flags.ENTRY, StateFlag.State.ALLOW);
            region.setFlag(Flags.EXIT, StateFlag.State.ALLOW);

            // Añadir la región
            regionManager.addRegion(region);

            logger.info("Región de ciudad creada: " + cityName + " en " + world.getName());
            return true;

        } catch (Exception e) {
            logger.severe("Error al crear región de ciudad " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCityRegion(String cityName) {
        if (!isEnabled)
            return false;

        try {
            String regionId = CITY_REGION_PREFIX + cityName.toLowerCase();
            boolean deleted = false;

            // Eliminar de todos los mundos
            for (World world : Bukkit.getWorlds()) {
                com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer().get(BukkitAdapter.adapt(world));

                if (regionManager != null && regionManager.hasRegion(regionId)) {
                    regionManager.removeRegion(regionId);
                    deleted = true;
                }
            }

            // Limpiar del modo asedio
            siegeModeRegions.remove(cityName.toLowerCase());

            if (deleted) {
                logger.info("Región de ciudad eliminada: " + cityName);
            }

            return deleted;

        } catch (Exception e) {
            logger.severe("Error al eliminar región de ciudad " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean updateCityRegion(String cityName, Location newCenter, int newRadius) {
        if (!deleteCityRegion(cityName)) {
            return false;
        }
        return createCityRegion(cityName, newCenter, newRadius);
    }

    public boolean expandCityRegion(String cityName, int blocks, Direction direction) {
        if (!isEnabled || blocks <= 0)
            return false;
        return worldGuardIntegration.expandCityRegion(cityName, direction.name().toLowerCase(), blocks);
    }

    public boolean cityRegionExists(String cityName) {
        if (!isEnabled)
            return false;
        return getCityRegion(cityName) != null;
    }

    // ================== INFORMACIÓN DE REGIONES ==================

    public ProtectedRegion getCityRegion(String cityName) {
        if (!isEnabled)
            return null;

        try {
            String regionId = CITY_REGION_PREFIX + cityName.toLowerCase();

            // Buscar en todos los mundos
            for (World world : Bukkit.getWorlds()) {
                com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer().get(BukkitAdapter.adapt(world));

                if (regionManager != null) {
                    ProtectedRegion region = regionManager.getRegion(regionId);
                    if (region != null) {
                        return region;
                    }
                }
            }

        } catch (Exception e) {
            logger.severe("Error al obtener región de ciudad " + cityName + ": " + e.getMessage());
        }

        return null;
    }

    public Location getCityRegionCenter(String cityName) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null)
            return null;

        try {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            int centerX = (min.getBlockX() + max.getBlockX()) / 2;
            int centerZ = (min.getBlockZ() + max.getBlockZ()) / 2;
            int centerY = (min.getBlockY() + max.getBlockY()) / 2;

            // Encontrar el mundo de la región
            World world = getRegionWorld(cityName);
            if (world == null)
                return null;

            return new Location(world, centerX, centerY, centerZ);

        } catch (Exception e) {
            logger.severe("Error al obtener centro de región de ciudad " + cityName + ": " + e.getMessage());
            return null;
        }
    }

    public int getCityRegionRadius(String cityName) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null)
            return 0;

        try {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            int width = max.getBlockX() - min.getBlockX();
            int length = max.getBlockZ() - min.getBlockZ();

            return Math.max(width, length) / 2;

        } catch (Exception e) {
            logger.severe("Error al obtener radio de región de ciudad " + cityName + ": " + e.getMessage());
            return 0;
        }
    }

    public String getCityAtLocation(Location location) {
        if (!isEnabled || location == null)
            return null;

        try {
            World world = location.getWorld();
            if (world == null)
                return null;

            com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(world));

            if (regionManager == null)
                return null;

            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            ApplicableRegionSet regions = regionManager.getApplicableRegions(vector);

            for (ProtectedRegion region : regions) {
                if (region.getId().startsWith(CITY_REGION_PREFIX)) {
                    return region.getId().substring(CITY_REGION_PREFIX.length());
                }
            }

        } catch (Exception e) {
            logger.severe("Error al obtener ciudad en ubicación: " + e.getMessage());
        }

        return null;
    }

    public boolean isLocationInCity(Location location) {
        return getCityAtLocation(location) != null;
    }

    // ================== PROTECCIONES DURANTE ASEDIOS ==================

    public boolean enableSiegeMode(String cityName) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            // Activar PvP durante el asedio
            region.setFlag(Flags.PVP, StateFlag.State.ALLOW);

            siegeModeRegions.put(cityName.toLowerCase(), true);
            logger.info("Modo asedio activado para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al activar modo asedio para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean disableSiegeMode(String cityName) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            // Desactivar PvP después del asedio
            region.setFlag(Flags.PVP, StateFlag.State.DENY);

            siegeModeRegions.put(cityName.toLowerCase(), false);
            logger.info("Modo asedio desactivado para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al desactivar modo asedio para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean enableCityProtections(String cityName) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            // Activar todas las protecciones
            region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);

            logger.info("Protecciones activadas para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al activar protecciones para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean disableCityProtections(String cityName) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            // Desactivar protecciones para permitir saqueo
            region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.ALLOW);

            logger.info("Protecciones desactivadas para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al desactivar protecciones para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean areCityProtectionsEnabled(String cityName) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            StateFlag.State buildFlag = region.getFlag(Flags.BUILD);
            return buildFlag == StateFlag.State.DENY;

        } catch (Exception e) {
            logger.severe("Error al verificar protecciones para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    // ================== INTEGRACIÓN CON RESIDENCE ==================

    public boolean disableResidenceProtections(String cityName) {
        if (!isEnabled)
            return false;

        try {
            // Obtener plugin de Residence
            Plugin residencePlugin = plugin.getServer().getPluginManager().getPlugin("Residence");
            if (residencePlugin == null || !residencePlugin.isEnabled()) {
                logger.warning("Residence no está disponible para desactivar protecciones.");
                return false;
            }

            // Aquí implementarías la lógica específica de Residence
            // Por ahora, solo loggeamos la acción
            logger.info("Protecciones de Residence desactivadas para: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al desactivar protecciones de Residence para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean enableResidenceProtections(String cityName) {
        if (!isEnabled)
            return false;

        try {
            Plugin residencePlugin = plugin.getServer().getPluginManager().getPlugin("Residence");
            if (residencePlugin == null || !residencePlugin.isEnabled()) {
                logger.warning("Residence no está disponible para activar protecciones.");
                return false;
            }

            // Aquí implementarías la lógica específica de Residence
            logger.info("Protecciones de Residence activadas para: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al activar protecciones de Residence para " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    // ================== GESTIÓN DE MIEMBROS ==================

    public boolean addRegionMember(String cityName, UUID playerId) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            DefaultDomain members = region.getMembers();
            members.addPlayer(playerId);
            region.setMembers(members);

            logger.info("Jugador añadido a la región de " + cityName + ": " + playerId);
            return true;

        } catch (Exception e) {
            logger.severe("Error al añadir miembro a región de " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean removeRegionMember(String cityName, UUID playerId) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            DefaultDomain members = region.getMembers();
            members.removePlayer(playerId);
            region.setMembers(members);

            logger.info("Jugador removido de la región de " + cityName + ": " + playerId);
            return true;

        } catch (Exception e) {
            logger.severe("Error al remover miembro de región de " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    public Set<UUID> getRegionMembers(String cityName) {
        if (!isEnabled)
            return new HashSet<>();

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return new HashSet<>();

            return region.getMembers().getUniqueIds();

        } catch (Exception e) {
            logger.severe("Error al obtener miembros de región de " + cityName + ": " + e.getMessage());
            return new HashSet<>();
        }
    }

    public boolean isRegionMember(String cityName, UUID playerId) {
        if (!isEnabled)
            return false;

        try {
            ProtectedRegion region = getCityRegion(cityName);
            if (region == null)
                return false;

            return region.getMembers().contains(playerId);

        } catch (Exception e) {
            logger.severe("Error al verificar miembro de región de " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    // ================== MÉTODOS AUXILIARES ==================

    private World getRegionWorld(String cityName) {
        String regionId = CITY_REGION_PREFIX + cityName.toLowerCase();

        for (World world : Bukkit.getWorlds()) {
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(world));
            if (regionManager != null && regionManager.hasRegion(regionId)) {
                return world;
            }
        }

        return null;
    }

    public boolean isInSiegeMode(String cityName) {
        return siegeModeRegions.getOrDefault(cityName.toLowerCase(), false);
    }

    public List<String> getAllCityRegions() {
        List<String> cities = new ArrayList<>();

        try {
            for (World world : Bukkit.getWorlds()) {
                com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer().get(BukkitAdapter.adapt(world));

                if (regionManager != null) {
                    for (ProtectedRegion region : regionManager.getRegions().values()) {
                        if (region.getId().startsWith(CITY_REGION_PREFIX)) {
                            String cityName = region.getId().substring(CITY_REGION_PREFIX.length());
                            if (!cities.contains(cityName)) {
                                cities.add(cityName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error al obtener todas las regiones de ciudades: " + e.getMessage());
        }

        return cities;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    // ================== VALIDACIONES ==================

    public boolean canCreateRegionAt(Location location, int radius) {
    if (!isEnabled || location == null)
        return false;

    try {
        World world = location.getWorld();
        if (world == null)
            return false;

        com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(world));

        if (regionManager == null)
            return false;

        // Verificar si se superpone con otras regiones de ciudades
        for (ProtectedRegion existingRegion : regionManager.getRegions().values()) {
            if (existingRegion.getId().startsWith(CITY_REGION_PREFIX)) {
                // Usar el método del Integration que ya tienes
                if (worldGuardIntegration.isRegionOverlapping(existingRegion.getId(), location, radius)) {
                    return false;
                }
            }
        }

        return true;

    } catch (Exception e) {
        logger.severe("Error al validar ubicación para región: " + e.getMessage());
        return false;
    }
}

    public int getRegionArea(String cityName) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null)
            return 0;

        try {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            int width = max.getBlockX() - min.getBlockX() + 1;
            int length = max.getBlockZ() - min.getBlockZ() + 1;

            return width * length;

        } catch (Exception e) {
            logger.severe("Error al calcular área de región de " + cityName + ": " + e.getMessage());
            return 0;
        }
    }
}
