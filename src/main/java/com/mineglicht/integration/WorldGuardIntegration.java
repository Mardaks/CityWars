package com.mineglicht.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.*;
import java.util.logging.Logger;

/**
 * Integración con WorldGuard para gestionar regiones de ciudades
 * Maneja la creación, expansión y protección de territorios urbanos
 */
public class WorldGuardIntegration {

    private static final String CITY_REGION_PREFIX = "city_";
    private static final Logger logger = Logger.getLogger("CityWars");
    private final RegionContainer regionContainer;

    public WorldGuardIntegration() {
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    // ==================== MÉTODOS PRINCIPALES (NECESARIOS) ====================

    // GESTIÓN DE REGIONES

    /**
     * Crea una región protegida para una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @param center   Ubicación central de la región
     * @param radius   Radio de protección en bloques
     * @return true si la región se creó exitosamente
     */
    public boolean createCityRegion(String cityName, Location center, int radius) {
        if (center == null || center.getWorld() == null) {
            logger.warning("Ubicación o mundo inválido para crear región de ciudad: " + cityName);
            return false;
        }

        String regionName = CITY_REGION_PREFIX + cityName.toLowerCase();
        World world = center.getWorld();
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            logger.severe("No se pudo obtener RegionManager para el mundo: " + world.getName());
            return false;
        }

        // Verificar si ya existe una región con ese nombre
        if (regionManager.hasRegion(regionName)) {
            logger.warning("La región " + regionName + " ya existe");
            return false;
        }

        // Crear los puntos de la región cúbica
        BlockVector3 min = BlockVector3.at(
                center.getBlockX() - radius,
                0, // Desde bedrock
                center.getBlockZ() - radius);

        BlockVector3 max = BlockVector3.at(
                center.getBlockX() + radius,
                world.getMaxHeight(), // Hasta altura máxima
                center.getBlockZ() + radius);

        try {
            // Crear la región protegida
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);

            // Configurar flags básicos de protección
            region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
            region.setFlag(Flags.PVP, StateFlag.State.DENY);
            region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
            region.setFlag(Flags.USE, StateFlag.State.DENY);

            // Añadir la región al manager
            regionManager.addRegion(region);

            logger.info("Región creada exitosamente: " + regionName + " en " + world.getName());
            return true;

        } catch (Exception e) {
            logger.severe("Error al crear región " + regionName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Expande una región de ciudad en la dirección especificada
     * 
     * @param cityName  Nombre de la ciudad
     * @param direction Dirección de expansión (north, south, east, west)
     * @param blocks    Cantidad de bloques a expandir
     * @return true si la expansión fue exitosa
     */
    public boolean expandCityRegion(String cityName, String direction, int blocks) {
        String regionName = CITY_REGION_PREFIX + cityName.toLowerCase();
        ProtectedRegion region = getCityRegion(cityName);

        if (region == null) {
            logger.warning("No se encontró región para la ciudad: " + cityName);
            return false;
        }
        try {
            if (blocks <= 0) {
                logger.warning("Número de bloques debe ser mayor a 0");
                return false;
            }
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            // Expandir según la dirección
            switch (direction.toLowerCase()) {
                case "north":
                    min = min.subtract(0, 0, blocks);
                    break;
                case "south":
                    max = max.add(0, 0, blocks);
                    break;
                case "east":
                    max = max.add(blocks, 0, 0);
                    break;
                case "west":
                    min = min.subtract(blocks, 0, 0);
                    break;
                default:
                    logger.warning("Dirección inválida: " + direction);
                    return false;
            }

            // Actualizar los puntos de la región
            if (region instanceof ProtectedCuboidRegion) {
                ProtectedCuboidRegion cuboidRegion = (ProtectedCuboidRegion) region;
                cuboidRegion.setMinimumPoint(min);
                cuboidRegion.setMaximumPoint(max);

                logger.info("Región expandida: " + regionName + " en dirección " + direction + " por " + blocks
                        + " bloques");
                return true;
            }

        } catch (Exception e) {
            logger.severe("Error al expandir región " + regionName + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Elimina completamente una región de ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @return true si se eliminó exitosamente
     */
    public boolean deleteCityRegion(String cityName) {
        String regionName = CITY_REGION_PREFIX + cityName.toLowerCase();

        // Buscar en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null && regionManager.hasRegion(regionName)) {
                regionManager.removeRegion(regionName);
                logger.info("Región eliminada: " + regionName + " del mundo " + world.getName());
                return true;
            }
        }

        logger.warning("No se encontró región para eliminar: " + regionName);
        return false;
    }

    /**
     * Obtiene la región protegida de una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @return La región protegida o null si no existe
     */
    public ProtectedRegion getCityRegion(String cityName) {
        String regionName = CITY_REGION_PREFIX + cityName.toLowerCase();

        // Buscar en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    return region;
                }
            }
        }

        return null;
    }

    /**
     * Verifica si una ubicación está dentro de una región de ciudad
     * 
     * @param location Ubicación a verificar
     * @return true si está dentro de alguna región de ciudad
     */
    public boolean isLocationInCityRegion(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return false;
        }

        BlockVector3 position = BlockVector3.at(location.getX(), location.getY(), location.getZ());

        // Verificar todas las regiones que contienen esta posición
        for (ProtectedRegion region : regionManager.getApplicableRegions(position)) {
            if (region.getId().startsWith(CITY_REGION_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    // CONTROL DE PROTECCIONES DURANTE ASEDIOS

    /**
     * Desactiva las protecciones de una ciudad durante un asedio
     * 
     * @param cityName Nombre de la ciudad
     * @return true si se desactivaron las protecciones
     */
    public boolean disableCityProtections(String cityName) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null) {
            return false;
        }

        try {
            // Desactivar flags de protección
            region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.ALLOW);
            region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
            region.setFlag(Flags.USE, StateFlag.State.ALLOW);

            logger.info("Protecciones desactivadas para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al desactivar protecciones de " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Reactiva las protecciones de una ciudad después de un asedio
     * 
     * @param cityName Nombre de la ciudad
     * @return true si se reactivaron las protecciones
     */
    public boolean enableCityProtections(String cityName) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null) {
            return false;
        }

        try {
            // Reactivar flags de protección
            region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
            region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
            region.setFlag(Flags.USE, StateFlag.State.DENY);

            logger.info("Protecciones reactivadas para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al reactivar protecciones de " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Activa o desactiva el PvP en una región de ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @param enabled  true para activar PvP, false para desactivar
     * @return true si se cambió el estado exitosamente
     */
    public boolean setCityPvPEnabled(String cityName, boolean enabled) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null) {
            return false;
        }

        try {
            StateFlag.State state = enabled ? StateFlag.State.ALLOW : StateFlag.State.DENY;
            region.setFlag(Flags.PVP, state);

            logger.info("PvP " + (enabled ? "activado" : "desactivado") + " para la ciudad: " + cityName);
            return true;

        } catch (Exception e) {
            logger.severe("Error al cambiar PvP de " + cityName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si una ciudad está protegida
     * 
     * @param cityName Nombre de la ciudad
     * @return true si la ciudad tiene protecciones activas
     */
    public boolean isCityProtected(String cityName) {
        ProtectedRegion region = getCityRegion(cityName);
        if (region == null) {
            return false;
        }

        StateFlag.State buildFlag = region.getFlag(Flags.BUILD);
        return buildFlag == StateFlag.State.DENY;
    }

    // ==================== MÉTODOS SECUNDARIOS (ÚTILES) ====================

    // VALIDACIONES

    /**
     * Verifica si se puede crear una región en la ubicación especificada
     * 
     * @param location Ubicación propuesta
     * @param radius   Radio de la región
     * @return true si no hay conflictos
     */
    public boolean canCreateRegionAt(Location location, int radius) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return false;
        }

        BlockVector3 min = BlockVector3.at(
                location.getBlockX() - radius,
                0,
                location.getBlockZ() - radius);

        BlockVector3 max = BlockVector3.at(
                location.getBlockX() + radius,
                location.getWorld().getMaxHeight(),
                location.getBlockZ() + radius);

        // Verificar conflictos usando regiones aplicables
        BlockVector3 center = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // Verificar si ya existe alguna región en el área
        for (ProtectedRegion existingRegion : regionManager.getApplicableRegions(center)) {
            if (existingRegion != null) {
                return false;
            }
        }

        // Verificar también las esquinas del área propuesta
        BlockVector3[] corners = {
                min, max,
                BlockVector3.at(min.getBlockX(), min.getBlockY(), max.getBlockZ()),
                BlockVector3.at(max.getBlockX(), min.getBlockY(), min.getBlockZ())
        };

        // Verificar conflictos con regiones existentes
        for (BlockVector3 corner : corners) {
            ApplicableRegionSet cornerRegions = regionManager.getApplicableRegions(corner);
            if (cornerRegions.size() > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtiene una lista de regiones que entran en conflicto con el área propuesta
     * 
     * @param center Centro de la región propuesta
     * @param radius Radio de la región
     * @return Lista de nombres de regiones en conflicto
     */
    public List<String> getRegionConflicts(Location center, int radius) {
        List<String> conflicts = new ArrayList<>();

        if (center == null || center.getWorld() == null) {
            return conflicts;
        }

        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(center.getWorld()));
        if (regionManager == null) {
            return conflicts;
        }

        BlockVector3 min = BlockVector3.at(
                center.getBlockX() - radius,
                0,
                center.getBlockZ() - radius);

        BlockVector3 max = BlockVector3.at(
                center.getBlockX() + radius,
                center.getWorld().getMaxHeight(),
                center.getBlockZ() + radius);

        // Verificar conflictos usando regiones aplicables en múltiples puntos
        BlockVector3[] testPoints = {
                min, max,
                BlockVector3.at(center.getBlockX(), center.getBlockY(), center.getBlockZ()),
                BlockVector3.at(min.getBlockX(), min.getBlockY(), max.getBlockZ()),
                BlockVector3.at(max.getBlockX(), min.getBlockY(), min.getBlockZ())
        };

        for (BlockVector3 point : testPoints) {
            for (ProtectedRegion region : regionManager.getApplicableRegions(point)) {
                if (!conflicts.contains(region.getId())) {
                    conflicts.add(region.getId());
                }
            }
        }

        return conflicts;
    }

    /**
     * Verifica si una región específica se superpone con el área propuesta
     * 
     * @param regionName Nombre de la región a verificar
     * @param center     Centro del área propuesta
     * @param radius     Radio del área propuesta
     * @return true si hay superposición
     */
    public boolean isRegionOverlapping(String regionName, Location center, int radius) {
        if (center == null || center.getWorld() == null) {
            return false;
        }

        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(center.getWorld()));
        if (regionManager == null) {
            return false;
        }

        ProtectedRegion existingRegion = regionManager.getRegion(regionName);
        if (existingRegion == null) {
            return false;
        }

        BlockVector3 min = BlockVector3.at(
                center.getBlockX() - radius,
                0,
                center.getBlockZ() - radius);

        BlockVector3 max = BlockVector3.at(
                center.getBlockX() + radius,
                center.getWorld().getMaxHeight(),
                center.getBlockZ() + radius);

        // Verificar si el área propuesta intersecta con la región existente
        BlockVector3[] testPoints = {
                min, max,
                BlockVector3.at(center.getBlockX(), center.getBlockY(), center.getBlockZ()),
                BlockVector3.at(min.getBlockX(), min.getBlockY(), max.getBlockZ()),
                BlockVector3.at(max.getBlockX(), min.getBlockY(), min.getBlockZ())
        };

        for (BlockVector3 point : testPoints) {
            for (ProtectedRegion region : regionManager.getApplicableRegions(point)) {
                if (region.getId().equals(regionName)) {
                    return true;
                }
            }
        }

        return false;
    }

    // CONFIGURACIÓN

    /**
     * Configura un flag específico para una región
     * 
     * @param regionName Nombre de la región
     * @param flag       Nombre del flag
     * @param value      Valor del flag
     * @return true si se configuró exitosamente
     */
    public boolean setRegionFlag(String regionName, String flag, boolean value) {
        // Buscar la región en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    try {
                        StateFlag.State state = value ? StateFlag.State.ALLOW : StateFlag.State.DENY;

                        switch (flag.toLowerCase()) {
                            case "build":
                                region.setFlag(Flags.BUILD, state);
                                break;
                            case "pvp":
                                region.setFlag(Flags.PVP, state);
                                break;
                            case "chest-access":
                                region.setFlag(Flags.CHEST_ACCESS, state);
                                break;
                            case "use":
                                region.setFlag(Flags.USE, state);
                                break;
                            default:
                                logger.warning("Flag desconocido: " + flag);
                                return false;
                        }

                        return true;

                    } catch (Exception e) {
                        logger.severe("Error al configurar flag " + flag + " para región " + regionName + ": "
                                + e.getMessage());
                        return false;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Añade un jugador como miembro de una región
     * 
     * @param regionName Nombre de la región
     * @param player     UUID del jugador
     * @return true si se añadió exitosamente
     */
    public boolean addRegionMember(String regionName, UUID player) {
        // Buscar la región en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    DefaultDomain members = region.getMembers();
                    members.addPlayer(player);

                    logger.info("Jugador " + player + " añadido como miembro de la región " + regionName);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remueve un jugador como miembro de una región
     * 
     * @param regionName Nombre de la región
     * @param player     UUID del jugador
     * @return true si se removió exitosamente
     */
    public boolean removeRegionMember(String regionName, UUID player) {
        // Buscar la región en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    DefaultDomain members = region.getMembers();
                    members.removePlayer(player);

                    logger.info("Jugador " + player + " removido como miembro de la región " + regionName);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Obtiene la lista de miembros de una región
     * 
     * @param regionName Nombre de la región
     * @return Set de UUIDs de los miembros
     */
    public Set<UUID> getRegionMembers(String regionName) {
        // Buscar la región en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    return region.getMembers().getUniqueIds();
                }
            }
        }

        return new HashSet<>();
    }

    // INFORMACIÓN

    /**
     * Obtiene el tamaño total de una región en bloques
     * 
     * @param regionName Nombre de la región
     * @return Área de la región en bloques cuadrados
     */
    public long getRegionSize(String regionName) {
        // Buscar la región en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    return region.volume();
                }
            }
        }

        return 0;
    }

    /**
     * Obtiene los límites de una región
     * 
     * @param regionName Nombre de la región
     * @return Mapa con las coordenadas de los límites
     */
    public Map<String, Integer> getRegionBounds(String regionName) {
        Map<String, Integer> bounds = new HashMap<>();

        // Buscar la región en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    BlockVector3 min = region.getMinimumPoint();
                    BlockVector3 max = region.getMaximumPoint();

                    bounds.put("minX", min.getBlockX());
                    bounds.put("minY", min.getBlockY());
                    bounds.put("minZ", min.getBlockZ());
                    bounds.put("maxX", max.getBlockX());
                    bounds.put("maxY", max.getBlockY());
                    bounds.put("maxZ", max.getBlockZ());

                    return bounds;
                }
            }
        }

        return bounds;
    }

    /**
     * Obtiene todas las regiones de ciudades en el servidor
     * 
     * @return Lista de nombres de regiones de ciudades
     */
    public List<String> getAllCityRegions() {
        List<String> cityRegions = new ArrayList<>();

        // Buscar en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.startsWith(CITY_REGION_PREFIX)) {
                        cityRegions.add(regionName);
                    }
                }
            }
        }

        return cityRegions;
    }

    /**
     * Verifica si WorldGuard está disponible y funcionando
     * 
     * @return true si WorldGuard está disponible
     */
    public boolean isWorldGuardAvailable() {
        try {
            return WorldGuard.getInstance() != null && regionContainer != null;
        } catch (Exception e) {
            logger.severe("WorldGuard no está disponible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el nombre de la ciudad a partir del nombre de la región
     * 
     * @param regionName Nombre de la región
     * @return Nombre de la ciudad o null si no es una región de ciudad
     */
    public String getCityNameFromRegion(String regionName) {
        if (regionName != null && regionName.startsWith(CITY_REGION_PREFIX)) {
            return regionName.substring(CITY_REGION_PREFIX.length());
        }
        return null;
    }
}
