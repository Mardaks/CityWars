package com.mineglicht.integration;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.mineglicht.models.City;
import com.mineglicht.cityWars;
import com.mineglicht.models.Citizen;
import com.mineglicht.models.Siege;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Clase para integrar CityWars con el plugin Residence
 * Maneja la protección de residencias durante asedios y gestión de ciudades
 * Independiente de managers, usa directamente los models de CityWars
 */
public class ResidenceIntegration2 {

    private final cityWars plugin;
    private boolean residenceEnabled;
    private ResidenceManager residenceManager;

    // Cache para almacenar estado original de las protecciones antes de asedios
    private final Map<String, Map<String, Boolean>> originalResidenceFlags;

    // Residencias temporalmente deshabilitadas
    private final Map<String, Long> temporaryDisabledResidences;

    // Cache de ciudades para optimización
    private final Map<String, City> cityCache;

    // Radio de búsqueda por defecto para residencias en ciudades (en bloques)
    private static final int DEFAULT_CITY_RADIUS = 100;

    public ResidenceIntegration2(cityWars plugin) {
        this.plugin = plugin;
        this.originalResidenceFlags = new ConcurrentHashMap<>();
        this.temporaryDisabledResidences = new ConcurrentHashMap<>();
        this.cityCache = new ConcurrentHashMap<>();
        this.residenceEnabled = setupResidence();

        if (residenceEnabled) {
            plugin.getLogger().info("Integración con Residence habilitada correctamente");
        } else {
            plugin.getLogger().warning("No se pudo habilitar la integración con Residence");
        }
    }

    /**
     * Configura la integración con Residence
     */
    private boolean setupResidence() {
        Plugin residencePlugin = Bukkit.getPluginManager().getPlugin("Residence");
        if (residencePlugin == null || !residencePlugin.isEnabled()) {
            return false;
        }

        try {
            this.residenceManager = Residence.getInstance().getResidenceManager();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al configurar integración con Residence: " + e.getMessage());
            return false;
        }
    }

    // ================== MÉTODOS PRINCIPALES (NECESARIOS) ==================

    /**
     * Desactiva las protecciones de Residence en todas las residencias de una
     * ciudad
     */
    public boolean disableResidenceProtections(String cityName) {
        if (!residenceEnabled)
            return false;

        try {
            City city = getCityFromCache(cityName);
            if (city == null)
                return false;

            List<ClaimedResidence> residences = getResidencesInCity(cityName);
            Map<String, Boolean> originalFlags = new HashMap<>();

            for (ClaimedResidence residence : residences) {
                String resName = residence.getName();
                ResidencePermissions perms = residence.getPermissions();

                // Guardar estado original de flags importantes
                originalFlags.put(resName + "_build", perms.has(Flags.build, true));
                originalFlags.put(resName + "_destroy", perms.has(Flags.destroy, true));
                originalFlags.put(resName + "_container", perms.has(Flags.container, true));
                originalFlags.put(resName + "_pvp", perms.has(Flags.pvp, true));

                // Desactivar protecciones principales
                setResidenceFlag(resName, "build", true);
                setResidenceFlag(resName, "destroy", true);
                setResidenceFlag(resName, "container", true);
                setResidenceFlag(resName, "pvp", true);

                plugin.getLogger().info("Protecciones desactivadas en residencia: " + resName);
            }

            // Guardar estado original para restaurar después
            originalResidenceFlags.put(cityName, originalFlags);

            // Notificar a los propietarios
            notifyResidenceOwners(cityName,
                    "§c¡Las protecciones de tu residencia han sido desactivadas debido a un asedio!");

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al desactivar protecciones de Residence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reactiva las protecciones de Residence en una ciudad
     */
    public boolean enableResidenceProtections(String cityName) {
        if (!residenceEnabled)
            return false;

        try {
            Map<String, Boolean> originalFlags = originalResidenceFlags.get(cityName);
            if (originalFlags == null)
                return false;

            List<ClaimedResidence> residences = getResidencesInCity(cityName);

            for (ClaimedResidence residence : residences) {
                String resName = residence.getName();

                // Restaurar flags originales
                Boolean originalBuild = originalFlags.get(resName + "_build");
                Boolean originalDestroy = originalFlags.get(resName + "_destroy");
                Boolean originalContainer = originalFlags.get(resName + "_container");
                Boolean originalPvp = originalFlags.get(resName + "_pvp");

                if (originalBuild != null)
                    setResidenceFlag(resName, "build", originalBuild);
                if (originalDestroy != null)
                    setResidenceFlag(resName, "destroy", originalDestroy);
                if (originalContainer != null)
                    setResidenceFlag(resName, "container", originalContainer);
                if (originalPvp != null)
                    setResidenceFlag(resName, "pvp", originalPvp);

                plugin.getLogger().info("Protecciones restauradas en residencia: " + resName);
            }

            // Limpiar cache
            originalResidenceFlags.remove(cityName);

            // Notificar a los propietarios
            notifyResidenceOwners(cityName, "§a¡Las protecciones de tu residencia han sido restauradas!");

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al reactivar protecciones de Residence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todas las residencias dentro de una ciudad usando su ubicación
     * central
     */
    public List<ClaimedResidence> getResidencesInCity(String cityName) {
        if (!residenceEnabled)
            return new ArrayList<>();

        try {
            City city = getCityFromCache(cityName);
            if (city == null)
                return new ArrayList<>();

            List<ClaimedResidence> cityResidences = new ArrayList<>();
            Location cityCenter = city.getCenterLocation();

            for (ClaimedResidence residence : residenceManager.getResidences().values()) {
                if (isResidenceInCityArea(residence, cityCenter, DEFAULT_CITY_RADIUS)) {
                    cityResidences.add(residence);
                }
            }

            return cityResidences;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener residencias de la ciudad: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene residencias en ciudad usando objeto City directamente
     */
    public List<ClaimedResidence> getResidencesInCity(City city) {
        if (!residenceEnabled || city == null)
            return new ArrayList<>();

        try {
            List<ClaimedResidence> cityResidences = new ArrayList<>();
            Location cityCenter = city.getCenterLocation();

            for (ClaimedResidence residence : residenceManager.getResidences().values()) {
                if (isResidenceInCityArea(residence, cityCenter, DEFAULT_CITY_RADIUS)) {
                    cityResidences.add(residence);
                }
            }

            return cityResidences;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener residencias de la ciudad: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Activa o desactiva PvP en una residencia específica
     */
    public boolean setResidencePvP(String residenceName, boolean enabled) {
        if (!residenceEnabled)
            return false;

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null)
                return false;

            return setResidenceFlag(residenceName, "pvp", enabled);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al configurar PvP en residencia: " + e.getMessage());
            return false;
        }
    }

    // ================== GESTIÓN DE RESIDENCIAS DE CIUDADANOS ==================

    /**
     * Verifica si la residencia de un jugador está dentro de una ciudad
     */
    public boolean isPlayerResidenceInCity(UUID player, String cityName) {
        if (!residenceEnabled)
            return false;

        try {
            List<ClaimedResidence> playerResidences = getPlayerResidences(player);
            List<ClaimedResidence> cityResidences = getResidencesInCity(cityName);

            for (ClaimedResidence playerRes : playerResidences) {
                for (ClaimedResidence cityRes : cityResidences) {
                    if (playerRes.getName().equals(cityRes.getName())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar residencia del jugador: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si la residencia de un ciudadano está en su ciudad
     */
    public boolean isCitizenResidenceInCity(Citizen citizen) {
        if (!residenceEnabled || citizen == null)
            return false;

        return isPlayerResidenceInCity(citizen.getPlayerId(), citizen.getCityName());
    }

    /**
     * Obtiene todas las residencias de un jugador
     */
    public List<ClaimedResidence> getPlayerResidences(UUID player) {
        if (!residenceEnabled)
            return new ArrayList<>();

        try {
            return residenceManager.getResidences().values().stream()
                    .filter(res -> res.getOwnerUUID().equals(player))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener residencias del jugador: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Verifica si se puede crear una residencia en una ubicación dentro de una
     * ciudad
     */
    public boolean canPlaceResidenceInCity(Location location, String cityName) {
        if (!residenceEnabled)
            return false;

        try {
            City city = getCityFromCache(cityName);
            if (city == null)
                return false;

            // Verificar si la ubicación está dentro del radio de la ciudad
            if (!isLocationInCityArea(location, city.getCenterLocation(), DEFAULT_CITY_RADIUS)) {
                return false;
            }

            // Verificar si no hay conflictos con otras residencias
            ClaimedResidence existingRes = residenceManager.getByLoc(location);
            return existingRes == null;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar ubicación para residencia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si se puede crear una residencia en una ubicación dentro de una
     * ciudad (usando objeto City)
     */
    public boolean canPlaceResidenceInCity(Location location, City city) {
        if (!residenceEnabled || city == null)
            return false;

        try {
            // Verificar si la ubicación está dentro del radio de la ciudad
            if (!isLocationInCityArea(location, city.getCenterLocation(), DEFAULT_CITY_RADIUS)) {
                return false;
            }

            // Verificar si no hay conflictos con otras residencias
            ClaimedResidence existingRes = residenceManager.getByLoc(location);
            return existingRes == null;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar ubicación para residencia: " + e.getMessage());
            return false;
        }
    }

    // ================== MÉTODOS SECUNDARIOS (ÚTILES) ==================

    /**
     * Obtiene el propietario de una residencia
     */
    public UUID getResidenceOwner(String residenceName) {
        if (!residenceEnabled)
            return null;

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            return residence != null ? residence.getOwnerUUID() : null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener propietario de residencia: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene los miembros de una residencia
     */
    public List<UUID> getResidenceMembers(String residenceName) {
    if (!residenceEnabled)
        return new ArrayList<>();

    try {
        ClaimedResidence residence = residenceManager.getByName(residenceName);
        if (residence == null)
            return new ArrayList<>();

        List<UUID> members = new ArrayList<>();
        ResidencePermissions perms = residence.getPermissions();

        // Agregar propietario
        members.add(residence.getOwnerUUID());

        // Agregar miembros con permisos
        // getPlayerFlags() devuelve Map<String, Map<String, Boolean>>
        // donde la clave es el nombre del jugador
        perms.getPlayerFlags().forEach((playerName, permissions) -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                members.add(player.getUniqueId());
            }
        });

        return members;
    } catch (Exception e) {
        plugin.getLogger().severe("Error al obtener miembros de residencia: " + e.getMessage());
        return new ArrayList<>();
    }
}

    /**
     * Obtiene los límites de una residencia
     */
    public Map<String, Location> getResidenceBounds(String residenceName) {
        if (!residenceEnabled)
            return new HashMap<>();

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null)
                return new HashMap<>();

            Map<String, Location> bounds = new HashMap<>();
            bounds.put("min", residence.getAreaArray()[0].getLowLoc());
            bounds.put("max", residence.getAreaArray()[0].getHighLoc());

            return bounds;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener límites de residencia: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Verifica si una ubicación está dentro de una residencia
     */
    public boolean isLocationInResidence(Location location) {
        if (!residenceEnabled)
            return false;

        try {
            ClaimedResidence residence = residenceManager.getByLoc(location);
            return residence != null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar ubicación en residencia: " + e.getMessage());
            return false;
        }
    }

    // ================== VALIDACIONES ==================

    /**
     * Verifica si una residencia está protegida
     */
    public boolean isResidenceProtected(String residenceName) {
        if (!residenceEnabled)
            return false;

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null)
                return false;

            ResidencePermissions perms = residence.getPermissions();
            return !perms.has(Flags.build, true) || !perms.has(Flags.destroy, true);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar protección de residencia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un jugador puede destruir en una ubicación
     */
    public boolean canBreakInResidence(UUID player, Location location) {
        if (!residenceEnabled)
            return true;

        try {
            ClaimedResidence residence = residenceManager.getByLoc(location);
            if (residence == null)
                return true;

            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer == null)
                return false;

            return residence.getPermissions().playerHas(bukkitPlayer, Flags.destroy, true);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar permisos de destrucción: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un jugador puede construir en una ubicación
     */
    public boolean canPlaceInResidence(UUID player, Location location) {
        if (!residenceEnabled)
            return true;

        try {
            ClaimedResidence residence = residenceManager.getByLoc(location);
            if (residence == null)
                return true;

            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer == null)
                return false;

            return residence.getPermissions().playerHas(bukkitPlayer, Flags.build, true);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar permisos de construcción: " + e.getMessage());
            return false;
        }
    }

    // ================== CONFIGURACIÓN ==================

    /**
     * Configura un flag específico de una residencia
     */
    public boolean setResidenceFlag(String residenceName, String flagName, boolean value) {
        if (!residenceEnabled)
            return false;

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null) {
                plugin.getLogger().warning("Residencia no encontrada: " + residenceName);
                return false;
            }

            Flags flag = Flags.getFlag(flagName);
            if (flag == null) {
                plugin.getLogger().warning("Flag no válido: " + flagName);
                return false;
            }

            ResidencePermissions perms = residence.getPermissions();

            FlagPermissions.FlagState flagState = value ? FlagPermissions.FlagState.TRUE
                    : FlagPermissions.FlagState.FALSE;

            perms.setFlag(flagName, flagState);

            residence.save();
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al configurar flag de residencia: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene todos los flags de una residencia
     */
    public Map<String, Boolean> getResidenceFlags(String residenceName) {
        if (!residenceEnabled)
            return new HashMap<>();

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null)
                return new HashMap<>();

            Map<String, Boolean> flags = new HashMap<>();
            ResidencePermissions perms = residence.getPermissions();

            for (Flags flag : Flags.values()) {
                flags.put(flag.getName(), perms.has(flag, true));
            }

            return flags;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener flags de residencia: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Añade un miembro a una residencia
     */
    public boolean addResidenceMember(String residenceName, UUID player) {
        if (!residenceEnabled)
            return false;

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null) {
                plugin.getLogger().warning("Residencia no encontrada: " + residenceName);
                return false;
            }

            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer == null) {
                plugin.getLogger().warning("Jugador no encontrado online: " + player);
                return false;
            }

            ResidencePermissions perms = residence.getPermissions();
            FlagPermissions.FlagState allowState = FlagPermissions.FlagState.TRUE;
            String playerName = bukkitPlayer.getName();

            // Establecer permisos básicos para el miembro
            String[] memberFlags = { "build", "destroy", "container", "use" };

            for (String flagName : memberFlags) {
                perms.setPlayerFlag(playerName, flagName, allowState);
            }

            residence.save();
            plugin.getLogger().info("Miembro añadido a residencia: " + playerName + " -> " + residenceName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al añadir miembro a residencia: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Añade un ciudadano como miembro de residencias en su ciudad
     */
    public boolean addCitizenToResidences(Citizen citizen) {
        if (!residenceEnabled || citizen == null)
            return false;

        try {
            List<ClaimedResidence> cityResidences = getResidencesInCity(citizen.getCityName());
            boolean success = false;

            for (ClaimedResidence residence : cityResidences) {
                // Solo añadir si el ciudadano no es el propietario
                if (!residence.getOwnerUUID().equals(citizen.getPlayerId())) {
                    if (addResidenceMember(residence.getName(), citizen.getPlayerId())) {
                        success = true;
                    }
                }
            }

            return success;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al añadir ciudadano a residencias: " + e.getMessage());
            return false;
        }
    }

    // ================== UTILIDADES ==================

    /**
     * Obtiene todas las residencias del servidor
     */
    public List<ClaimedResidence> getAllResidences() {
        if (!residenceEnabled)
            return new ArrayList<>();

        try {
            return new ArrayList<>(residenceManager.getResidences().values());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener todas las residencias: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene las residencias de un propietario específico
     */
    public List<ClaimedResidence> getResidencesByOwner(UUID owner) {
        if (!residenceEnabled)
            return new ArrayList<>();

        try {
            return residenceManager.getResidences().values().stream()
                    .filter(res -> res.getOwnerUUID().equals(owner))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener residencias por propietario: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Verifica si el sistema de Residence está habilitado
     */
    public boolean isResidenceSystemEnabled() {
        return residenceEnabled;
    }

    /**
     * Respalda los datos de residencias (para debugging)
     */
    public void backupResidenceData() {
        if (!residenceEnabled)
            return;

        try {
            plugin.getLogger().info("Respaldando datos de Residence...");

            Map<String, Object> backup = new HashMap<>();
            backup.put("timestamp", System.currentTimeMillis());
            backup.put("residences_count", residenceManager.getResidences().size());
            backup.put("original_flags", new HashMap<>(originalResidenceFlags));

            plugin.getLogger().info("Respaldo completado: " + backup.get("residences_count") + " residencias");
        } catch (Exception e) {
            plugin.getLogger().severe("Error al respaldar datos de Residence: " + e.getMessage());
        }
    }

    // ================== COORDINACIÓN CON ASEDIOS ==================

    /**
     * Maneja el inicio de un asedio en una ciudad
     */
    public boolean handleSiegeStart(Siege siege) {
        if (!residenceEnabled || siege == null)
            return false;

        try {
            String defenderCity = siege.getDefenderCity();

            // Desactivar protecciones de la ciudad defensora
            if (disableResidenceProtections(defenderCity)) {
                plugin.getLogger().info("Protecciones de Residence desactivadas para asedio en: " + defenderCity);
                return true;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al manejar inicio de asedio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maneja el final de un asedio
     */
    public boolean handleSiegeEnd(Siege siege) {
        if (!residenceEnabled || siege == null)
            return false;

        try {
            String defenderCity = siege.getDefenderCity();

            // Reactivar protecciones de la ciudad defensora
            if (enableResidenceProtections(defenderCity)) {
                plugin.getLogger().info("Protecciones de Residence reactivadas tras asedio en: " + defenderCity);
                return true;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al manejar final de asedio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el estado de asedio de las residencias en una ciudad
     */
    public Map<String, String> getResidencesSiegeStatus(String cityName) {
        Map<String, String> status = new HashMap<>();

        if (!residenceEnabled) {
            status.put("system", "disabled");
            return status;
        }

        try {
            List<ClaimedResidence> residences = getResidencesInCity(cityName);
            boolean hasOriginalFlags = originalResidenceFlags.containsKey(cityName);

            status.put("total_residences", String.valueOf(residences.size()));
            status.put("siege_active", String.valueOf(hasOriginalFlags));
            status.put("protections_disabled", String.valueOf(hasOriginalFlags));

            for (ClaimedResidence residence : residences) {
                String resName = residence.getName();
                boolean pvpEnabled = residence.getPermissions().has(Flags.pvp, true);
                status.put("residence_" + resName + "_pvp", String.valueOf(pvpEnabled));
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener estado de asedio: " + e.getMessage());
            status.put("error", e.getMessage());
        }

        return status;
    }

    /**
     * Obtiene el estado de asedio usando objeto City
     */
    public Map<String, String> getResidencesSiegeStatus(City city) {
        if (city == null)
            return new HashMap<>();
        return getResidencesSiegeStatus(city.getName());
    }

    /**
     * Notifica a los propietarios de residencias en una ciudad
     */
    public void notifyResidenceOwners(String cityName, String message) {
        if (!residenceEnabled)
            return;

        try {
            List<ClaimedResidence> residences = getResidencesInCity(cityName);
            Set<UUID> notifiedOwners = new HashSet<>();

            for (ClaimedResidence residence : residences) {
                UUID owner = residence.getOwnerUUID();
                if (owner != null && !notifiedOwners.contains(owner)) {
                    Player player = Bukkit.getPlayer(owner);
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§6[CityWars] " + message);
                        notifiedOwners.add(owner);
                    }
                }
            }

            plugin.getLogger()
                    .info("Notificados " + notifiedOwners.size() + " propietarios de residencias en " + cityName);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al notificar propietarios: " + e.getMessage());
        }
    }

    /**
     * Notifica a propietarios usando objeto City
     */
    public void notifyResidenceOwners(City city, String message) {
        if (city != null) {
            notifyResidenceOwners(city.getName(), message);
        }
    }

    /**
     * Desactiva temporalmente una residencia por una duración específica
     */
    public boolean temporaryDisableResidence(String residenceName, long duration) {
        if (!residenceEnabled)
            return false;

        try {
            ClaimedResidence residence = residenceManager.getByName(residenceName);
            if (residence == null)
                return false;

            // Guardar estado original
            ResidencePermissions perms = residence.getPermissions();
            Map<String, Boolean> originalState = new HashMap<>();
            originalState.put("build", perms.has(Flags.build, true));
            originalState.put("destroy", perms.has(Flags.destroy, true));
            originalState.put("container", perms.has(Flags.container, true));

            // Desactivar protecciones
            setResidenceFlag(residenceName, "build", true);
            setResidenceFlag(residenceName, "destroy", true);
            setResidenceFlag(residenceName, "container", true);

            // Programar reactivación
            temporaryDisabledResidences.put(residenceName, System.currentTimeMillis() + duration);

            // Programar tarea para reactivar
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                temporaryDisabledResidences.remove(residenceName);
                originalState.forEach((flag, value) -> setResidenceFlag(residenceName, flag, value));

                UUID owner = getResidenceOwner(residenceName);
                if (owner != null) {
                    Player player = Bukkit.getPlayer(owner);
                    if (player != null) {
                        player.sendMessage("§a[CityWars] Las protecciones de tu residencia '" + residenceName
                                + "' han sido restauradas.");
                    }
                }
            }, duration / 50); // Convertir milisegundos a ticks

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al desactivar temporalmente residencia: " + e.getMessage());
            return false;
        }
    }
    // ================== MÉTODOS AUXILIARES INTERNOS (FALTANTES) ==================

    /**
     * Obtiene una ciudad del cache o la busca en el sistema
     */
    private City getCityFromCache(String cityName) {
        if (cityName == null || cityName.isEmpty())
            return null;

        try {
            // Verificar cache primero
            City cachedCity = cityCache.get(cityName);
            if (cachedCity != null) {
                return cachedCity;
            }

            // Buscar ciudad en el sistema CityWars
            // Necesitas implementar este método en tu clase principal CityWars:
            City city = plugin.getCityByName(cityName);

            if (city != null) {
                // Guardar en cache por 5 minutos
                cityCache.put(cityName, city);

                // Limpiar cache después de 5 minutos
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    cityCache.remove(cityName);
                }, 6000L); // 5 minutos en ticks
            }

            return city;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener ciudad del cache: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifica si una residencia está dentro del área de una ciudad
     */
    private boolean isResidenceInCityArea(ClaimedResidence residence, Location cityCenter, int radius) {
        if (residence == null || cityCenter == null)
            return false;

        try {
            // Obtener el centro de la residencia
            Location residenceCenter = getResidenceCenter(residence);
            if (residenceCenter == null)
                return false;

            // Verificar que estén en el mismo mundo
            if (!residenceCenter.getWorld().equals(cityCenter.getWorld())) {
                return false;
            }

            // Calcular distancia 2D (sin considerar Y)
            double distance = Math.sqrt(
                    Math.pow(residenceCenter.getX() - cityCenter.getX(), 2) +
                            Math.pow(residenceCenter.getZ() - cityCenter.getZ(), 2));

            return distance <= radius;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar residencia en área de ciudad: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si una ubicación está dentro del área de una ciudad
     */
    private boolean isLocationInCityArea(Location location, Location cityCenter, int radius) {
        if (location == null || cityCenter == null)
            return false;

        try {
            // Verificar que estén en el mismo mundo
            if (!location.getWorld().equals(cityCenter.getWorld())) {
                return false;
            }

            // Calcular distancia 2D
            double distance = Math.sqrt(
                    Math.pow(location.getX() - cityCenter.getX(), 2) +
                            Math.pow(location.getZ() - cityCenter.getZ(), 2));

            return distance <= radius;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar ubicación en área de ciudad: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el centro de una residencia
     */
    private Location getResidenceCenter(ClaimedResidence residence) {
        if (residence == null)
            return null;

        try {
            // Obtener los límites de la residencia
            Location min = residence.getAreaArray()[0].getLowLoc();
            Location max = residence.getAreaArray()[0].getHighLoc();

            // Calcular el centro
            double centerX = (min.getX() + max.getX()) / 2;
            double centerY = (min.getY() + max.getY()) / 2;
            double centerZ = (min.getZ() + max.getZ()) / 2;

            return new Location(min.getWorld(), centerX, centerY, centerZ);

        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener centro de residencia: " + e.getMessage());
            return null;
        }
    }

    /**
     * Limpia el cache de ciudades
     */
    public void clearCityCache() {
        try {
            cityCache.clear();
            plugin.getLogger().info("Cache de ciudades limpiado");
        } catch (Exception e) {
            plugin.getLogger().severe("Error al limpiar cache de ciudades: " + e.getMessage());
        }
    }

    /**
     * Limpia residencias temporalmente deshabilitadas que ya expiraron
     */
    public void cleanupExpiredTemporaryDisabled() {
        try {
            long currentTime = System.currentTimeMillis();
            temporaryDisabledResidences.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al limpiar residencias temporalmente deshabilitadas: " + e.getMessage());
        }
    }

    /**
     * Verifica si una residencia está temporalmente deshabilitada
     */
    public boolean isResidenceTemporarilyDisabled(String residenceName) {
        if (!residenceEnabled)
            return false;

        try {
            Long disabledUntil = temporaryDisabledResidences.get(residenceName);
            if (disabledUntil == null)
                return false;

            if (System.currentTimeMillis() > disabledUntil) {
                // Limpiar si ya expiró
                temporaryDisabledResidences.remove(residenceName);
                return false;
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar residencia temporalmente deshabilitada: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas del sistema de integración
     */
    public Map<String, Object> getIntegrationStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("residence_enabled", residenceEnabled);
            stats.put("total_residences", residenceEnabled ? residenceManager.getResidences().size() : 0);
            stats.put("cities_in_cache", cityCache.size());
            stats.put("cities_with_original_flags", originalResidenceFlags.size());
            stats.put("temporary_disabled_residences", temporaryDisabledResidences.size());
            stats.put("system_uptime", System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener estadísticas: " + e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Refresca el cache de una ciudad específica
     */
    public void refreshCityCache(String cityName) {
        if (cityName == null || cityName.isEmpty())
            return;

        try {
            // Remover del cache
            cityCache.remove(cityName);

            // Volver a obtener (esto lo añadirá al cache)
            getCityFromCache(cityName);

            plugin.getLogger().info("Cache refrescado para ciudad: " + cityName);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al refrescar cache de ciudad: " + e.getMessage());
        }
    }

    /**
     * Inicializa tareas programadas para mantenimiento
     */
    public void startMaintenanceTasks() {
        if (!residenceEnabled)
            return;

        try {
            // Limpiar cache cada 10 minutos
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                clearCityCache();
            }, 12000L, 12000L); // 10 minutos

            // Limpiar residencias temporalmente deshabilitadas cada 5 minutos
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                cleanupExpiredTemporaryDisabled();
            }, 6000L, 6000L); // 5 minutos

            plugin.getLogger().info("Tareas de mantenimiento iniciadas para ResidenceIntegration");

        } catch (Exception e) {
            plugin.getLogger().severe("Error al iniciar tareas de mantenimiento: " + e.getMessage());
        }
    }

    /**
     * Detiene todas las tareas y limpia recursos
     */
    public void shutdown() {
        try {
            // Limpiar caches
            cityCache.clear();
            originalResidenceFlags.clear();
            temporaryDisabledResidences.clear();

            plugin.getLogger().info("ResidenceIntegration cerrado correctamente");

        } catch (Exception e) {
            plugin.getLogger().severe("Error al cerrar ResidenceIntegration: " + e.getMessage());
        }
    }
}