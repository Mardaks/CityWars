package com.mineglicht.integration;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Integración con Residence para el manejo de protecciones durante asedios
 */
public class ResidenceIntegration {

    private final Plugin plugin;
    private boolean isEnabled = false;

    // Almacena el estado original de las protecciones durante el saqueo
    private final Map<String, Map<String, Boolean>> originalProtections = new HashMap<>();

    // Flags importantes que se deshabilitarán durante el saqueo
    private final List<String> siegeFlags = List.of(
            "build", "destroy", "use", "container", "pvp", "damage",
            "bucket", "ignite", "explode", "creeper", "tnt"
    );

    public ResidenceIntegration(Plugin plugin) {
        this.plugin = plugin;
        setupIntegration();
    }

    /**
     * Configura la integración con Residence
     */
    private void setupIntegration() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Residence") == null) {
                plugin.getLogger().warning("Residence no está instalado. La integración de protecciones está deshabilitada.");
                return;
            }

            if (!Bukkit.getPluginManager().getPlugin("Residence").isEnabled()) {
                plugin.getLogger().warning("Residence no está habilitado. La integración de protecciones está deshabilitada.");
                return;
            }

            isEnabled = true;
            plugin.getLogger().info("Integración con Residence habilitada exitosamente.");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al configurar la integración con Residence: " + e.getMessage());
            isEnabled = false;
        }
    }

    /**
     * Verifica si la integración está disponible
     * @return true si Residence está disponible
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Obtiene una residencia por su nombre
     * @param residenceName Nombre de la residencia
     * @return La residencia o null si no existe
     */
    public ClaimedResidence getResidence(String residenceName) {
        if (!isEnabled) return null;

        try {
            ResidenceManager manager = Residence.getInstance().getResidenceManager();
            return manager.getByName(residenceName);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al obtener la residencia '" + residenceName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la residencia en una ubicación específica
     * @param location La ubicación
     * @return La residencia o null si no hay ninguna
     */
    public ClaimedResidence getResidenceAt(Location location) {
        if (!isEnabled) return null;

        try {
            return ResidenceApi.getResidenceManager().getByLoc(location);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al obtener la residencia en la ubicación: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene todas las residencias dentro de una región de ciudad
     * @param cityName Región de la ciudad (puedes usar coordenadas o nombre)
     * @return Lista de residencias dentro de la ciudad
     */
    public List<ClaimedResidence> getResidencesInCity(String cityName) {
        List<ClaimedResidence> cityResidences = new ArrayList<>();

        if (!isEnabled) return cityResidences;

        try {
            ResidenceManager manager = Residence.getInstance().getResidenceManager();

            // Buscar residencias que contengan el nombre de la ciudad o estén dentro de la región
            for (ClaimedResidence residence : manager.getResidences().values()) {
                if (residence.getResidenceName().toLowerCase().contains(cityName.toLowerCase()) ||
                        isResidenceInCity(residence, cityName)) {
                    cityResidences.add(residence);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error al obtener las residencias de la ciudad '" + cityName + "': " + e.getMessage());
        }

        return cityResidences;
    }

    /**
     * Verifica si una residencia pertenece a una ciudad específica
     * @param residence La residencia
     * @param cityName Nombre de la ciudad
     * @return true si la residencia está en la ciudad
     */
    private boolean isResidenceInCity(ClaimedResidence residence, String cityName) {
        // Aquí puedes implementar la lógica para determinar si una residencia está dentro de una ciudad
        // Por ejemplo, verificar si está dentro de las coordenadas de la región de la ciudad
        // o si tiene un flag específico que la identifique como parte de la ciudad

        try {
            // Opción 1: Por nombre de residencia
            if (residence.getResidenceName().toLowerCase().startsWith(cityName.toLowerCase())) {
                return true;
            }

            // Opción 2: Por ubicación (necesitarías las coordenadas de la ciudad)
            // Location residenceLocation = residence.getMainArea().getLowLocation();
            // return isLocationInCity(residenceLocation, cityName);

            // Opción 3: Por flag personalizado
            // return residence.getPermissions().has("citywars.city", cityName, false);

        } catch (Exception e) {
            plugin.getLogger().warning("Error al verificar si la residencia pertenece a la ciudad: " + e.getMessage());
        }

        return false;
    }

    /**
     * Deshabilita las protecciones de todas las residencias en una ciudad durante el saqueo
     * @param cityName Nombre de la ciudad
     * @return true si fue exitoso
     */
    public boolean disableProtectionsForSiege(String cityName) {
        if (!isEnabled) return false;

        try {
            List<ClaimedResidence> cityResidences = getResidencesInCity(cityName);

            for (ClaimedResidence residence : cityResidences) {
                disableResidenceProtections(residence);
            }

            plugin.getLogger().info("Protecciones deshabilitadas para " + cityResidences.size() +
                    " residencias en la ciudad " + cityName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al deshabilitar protecciones para la ciudad '" + cityName + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Restaura las protecciones de todas las residencias en una ciudad después del saqueo
     * @param cityName Nombre de la ciudad
     * @return true si fue exitoso
     */
    public boolean restoreProtectionsAfterSiege(String cityName) {
        if (!isEnabled) return false;

        try {
            List<ClaimedResidence> cityResidences = getResidencesInCity(cityName);

            for (ClaimedResidence residence : cityResidences) {
                restoreResidenceProtections(residence);
            }

            plugin.getLogger().info("Protecciones restauradas para " + cityResidences.size() +
                    " residencias en la ciudad " + cityName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al restaurar protecciones para la ciudad '" + cityName + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Deshabilita las protecciones de una residencia específica
     * @param residence La residencia
     */
    private void disableResidenceProtections(ClaimedResidence residence) {
        try {
            String residenceName = residence.getResidenceName();
            FlagPermissions permissions = residence.getPermissions();

            // Guardar el estado original de las protecciones
            Map<String, Boolean> originalFlags = new HashMap<>();

            for (String flag : siegeFlags) {
                // Guardar el estado actual
                boolean currentState = permissions.has(flag, false);
                originalFlags.put(flag, currentState);

                // Deshabilitar la protección (permitir la acción)
                switch (flag) {
                    case "build":
                    case "destroy":
                    case "use":
                    case "container":
                        permissions.set(flag, true, false); // Permitir para todos
                        break;
                    case "pvp":
                    case "damage":
                        permissions.set(flag, true, false); // Permitir PvP y daño
                        break;
                    case "bucket":
                    case "ignite":
                        permissions.set(flag, true, false); // Permitir uso de buckets e ignición
                        break;
                    case "explode":
                    case "creeper":
                    case "tnt":
                        permissions.set(flag, true, false); // Permitir explosiones
                        break;
                }
            }

            // Guardar el estado original
            originalProtections.put(residenceName, originalFlags);

            plugin.getLogger().info("Protecciones deshabilitadas para la residencia: " + residenceName);

        } catch (Exception e) {
            plugin.getLogger().warning("Error al deshabilitar protecciones de la residencia: " + e.getMessage());
        }
    }

    /**
     * Restaura las protecciones originales de una residencia
     * @param residence La residencia
     */
    private void restoreResidenceProtections(ClaimedResidence residence) {
        try {
            String residenceName = residence.getResidenceName();

            if (!originalProtections.containsKey(residenceName)) {
                plugin.getLogger().warning("No se encontraron protecciones originales para: " + residenceName);
                return;
            }

            FlagPermissions permissions = residence.getPermissions();
            Map<String, Boolean> originalFlags = originalProtections.get(residenceName);

            // Restaurar cada flag a su estado original
            for (Map.Entry<String, Boolean> entry : originalFlags.entrySet()) {
                String flag = entry.getKey();
                boolean originalState = entry.getValue();

                permissions.set(flag, originalState, false);
            }

            // Limpiar el almacenamiento temporal
            originalProtections.remove(residenceName);

            plugin.getLogger().info("Protecciones restauradas para la residencia: " + residenceName);

        } catch (Exception e) {
            plugin.getLogger().warning("Error al restaurar protecciones de la residencia: " + e.getMessage());
        }
    }

    /**
     * Verifica si un jugador puede construir en una ubicación
     * @param player El jugador
     * @param location La ubicación
     * @return true si puede construir
     */
    public boolean canBuild(Player player, Location location) {
        if (!isEnabled) return true; // Si no hay Residence, permitir por defecto

        try {
            return ResidenceApi.getPermissionManager().canPlaceBlock(player, location, true);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al verificar permisos de construcción: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un jugador puede destruir en una ubicación
     * @param player El jugador
     * @param location La ubicación
     * @return true si puede destruir
     */
    public boolean canDestroy(Player player, Location location) {
        if (!isEnabled) return true;

        try {
            return ResidenceApi.getPermissionManager().canBreakBlock(player, location, true);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al verificar permisos de destrucción: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si hay PvP habilitado en una ubicación
     * @param location La ubicación
     * @return true si el PvP está habilitado
     */
    public boolean isPvPEnabled(Location location) {
        if (!isEnabled) return true;

        try {
            ClaimedResidence residence = getResidenceAt(location);
            if (residence == null) return true; // No hay residencia, PvP habilitado por defecto

            return residence.getPermissions().has("pvp", true);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al verificar estado de PvP: " + e.getMessage());
            return true;
        }
    }

    /**
     * Fuerza la habilitación de PvP en una residencia específica
     * @param residence La residencia
     */
    public void enablePvPForSiege(ClaimedResidence residence) {
        if (!isEnabled || residence == null) return;

        try {
            residence.getPermissions().set("pvp", true, false);
            plugin.getLogger().info("PvP habilitado para asedio en la residencia: " + residence.getResidenceName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error al habilitar PvP para asedio: " + e.getMessage());
        }
    }

    /**
     * Habilita PvP en todas las residencias de una ciudad durante el asedio
     * @param cityName Nombre de la ciudad
     */
    public void enablePvPForCitySiege(String cityName) {
        if (!isEnabled) return;

        List<ClaimedResidence> cityResidences = getResidencesInCity(cityName);
        for (ClaimedResidence residence : cityResidences) {
            enablePvPForSiege(residence);
        }
    }

    /**
     * Limpia todos los datos temporales de protecciones
     */
    public void clearTemporaryData() {
        originalProtections.clear();
        plugin.getLogger().info("Datos temporales de protecciones limpiados.");
    }

    /**
     * Obtiene información sobre una residencia
     * @param residence La residencia
     * @return String con información de la residencia
     */
    public String getResidenceInfo(ClaimedResidence residence) {
        if (residence == null) return "Residencia no encontrada";

        StringBuilder info = new StringBuilder();
        info.append("Nombre: ").append(residence.getResidenceName()).append("\n");
        info.append("Propietario: ").append(residence.getOwner()).append("\n");
        info.append("Área: ").append(residence.getTotalSize()).append(" bloques\n");

        // Mostrar algunos flags importantes
        FlagPermissions perms = residence.getPermissions();
        info.append("PvP: ").append(perms.has("pvp", false) ? "Habilitado" : "Deshabilitado").append("\n");
        info.append("Construcción: ").append(perms.has("build", false) ? "Permitida" : "Protegida").append("\n");
        info.append("Destrucción: ").append(perms.has("destroy", false) ? "Permitida" : "Protegida");

        return info.toString();
    }
}
