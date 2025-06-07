package com.mineglicht.manager;

import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Gestor de anulación de protecciones para ciudades durante asedios y saqueos.
 * Se encarga de coordinar con WorldGuard para manipular las flags de protección
 * según el estado del asedio (normal, asedio, saqueo).
 */
public class ProtectionOverrideManager {

    private static final Logger LOGGER = Bukkit.getLogger();

    // Almacena el estado original de las protecciones antes del asedio
    private final Map<UUID, CityProtectionState> originalProtections;

    // Referencia a otros managers necesarios

    private final SiegeManager siegeManager;

    public ProtectionOverrideManager(SiegeManager siegeManager) {
        this.siegeManager = siegeManager;
        this.originalProtections = new HashMap<>();
    }

    /**
     * Desactiva todas las protecciones de la ciudad durante un asedio.
     * Habilita PvP y permite construcción/destrucción.
     * 
     * @param city La ciudad objetivo del asedio
     */
    public void disableCityProtections(City city) {
        if (city == null || city.getName() == null) {
            LOGGER.warning("No se puede desactivar protecciones: ciudad o región nula");
            return;
        }

        try {
            // Guardar estado original antes de modificar
            saveOriginalProtections(city);

            // Habilitar PvP para el asedio
            setPvPFlag(city, true);

            // Permitir construcción durante el asedio (para banderas de asedio)
            setBuildFlag(city, true);

            // Permitir destrucción de bloques durante el asedio
            setBreakFlag(city, true);

            LOGGER.info("Protecciones desactivadas para la ciudad: " + city.getName() + " (Asedio iniciado)");

        } catch (Exception e) {
            LOGGER.severe("Error al desactivar protecciones de la ciudad " + city.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Restaura todas las protecciones de la ciudad después del asedio o saqueo.
     * 
     * @param city La ciudad a restaurar
     */
    public void restoreCityProtections(City city) {
        if (city == null) {
            LOGGER.warning("No se puede restaurar protecciones: ciudad nula");
            return;
        }

        try {
            CityProtectionState originalState = originalProtections.get(city.getId());

            if (originalState != null) {
                // Restaurar flags originales
                setPvPFlag(city, originalState.isPvpEnabled());
                setBuildFlag(city, originalState.isBuildEnabled());
                setBreakFlag(city, originalState.isBreakEnabled());
                setChestAccessFlag(city, originalState.isChestAccessEnabled());

                // Limpiar del mapa de estados originales
                originalProtections.remove(city.getId());

                LOGGER.info("Protecciones restauradas para la ciudad: " + city.getName());
            } else {
                // Si no hay estado original guardado, aplicar protecciones por defecto
                applyDefaultProtections(city);
                LOGGER.warning("No se encontró estado original para " + city.getName()
                        + ", aplicando protecciones por defecto");
            }

        } catch (Exception e) {
            LOGGER.severe("Error al restaurar protecciones de la ciudad " + city.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Desactiva las protecciones adicionales durante el saqueo.
     * Permite acceso a cofres y destrucción total.
     * 
     * @param city La ciudad en fase de saqueo
     */
    public void disableResidenceProtections(City city) {
        if (city == null) {
            LOGGER.warning("No se puede desactivar protecciones de saqueo: ciudad nula");
            return;
        }

        try {
            // Durante el saqueo, permitir acceso a cofres y contenedores
            setChestAccessFlag(city, true);

            // Permitir uso de items (puertas, botones, etc.)
            setUseFlag(city, true);

            // Permitir interacción con entidades (marcos de items, etc.)
            setInteractFlag(city, true);

            LOGGER.info("Protecciones de saqueo desactivadas para: " + city.getName());

        } catch (Exception e) {
            LOGGER.severe("Error al desactivar protecciones de saqueo para " + city.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Verifica si la ciudad está bajo ataque (asedio o saqueo).
     * 
     * @param city La ciudad a verificar
     * @return true si está bajo ataque, false en caso contrario
     */
    public boolean isCityUnderAttack(City city) {
        if (city == null) {
            return false;
        }

        SiegeState siegeState = siegeManager.getSiegeState(city);
        return siegeState == SiegeState.ACTIVE || siegeState == SiegeState.FLAG_CAPTURED;
    }

    /**
     * Restaura protecciones específicas como PvP y construcción.
     * Útil para transiciones entre fases del asedio.
     * 
     * @param city La ciudad a modificar
     */
    public void restoreSpecificProtections(City city) {
        if (city == null) {
            return;
        }

        try {
            CityProtectionState originalState = originalProtections.get(city.getId());

            if (originalState != null) {
                // Restaurar solo PvP y construcción, mantener otras modificaciones
                setPvPFlag(city, originalState.isPvpEnabled());
                setBuildFlag(city, originalState.isBuildEnabled());

                LOGGER.info("Protecciones específicas restauradas para: " + city.getName());
            }

        } catch (Exception e) {
            LOGGER.severe("Error al restaurar protecciones específicas: " + e.getMessage());
        }
    }

    /**
     * Habilita o deshabilita la flag de PvP para la ciudad.
     * 
     * @param city      La ciudad a modificar
     * @param isEnabled true para habilitar PvP, false para deshabilitarlo
     */
    public void setPvPFlag(City city, boolean isEnabled) {
        setWorldGuardFlag(city, Flags.PVP, isEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    /**
     * Habilita o deshabilita la flag de construcción para la ciudad.
     * 
     * @param city      La ciudad a modificar
     * @param isEnabled true para permitir construcción, false para denegarla
     */
    public void setBuildFlag(City city, boolean isEnabled) {
        setWorldGuardFlag(city, Flags.BUILD, isEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    /**
     * Habilita o deshabilita la flag de destrucción para la ciudad.
     * 
     * @param city      La ciudad a modificar
     * @param isEnabled true para permitir destrucción, false para denegarla
     */
    public void setBreakFlag(City city, boolean isEnabled) {
        setWorldGuardFlag(city, Flags.BLOCK_BREAK, isEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    /**
     * Habilita o deshabilita el acceso a cofres para la ciudad.
     * 
     * @param city      La ciudad a modificar
     * @param isEnabled true para permitir acceso, false para denegarlo
     */
    public void setChestAccessFlag(City city, boolean isEnabled) {
        setWorldGuardFlag(city, Flags.CHEST_ACCESS, isEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    /**
     * Habilita o deshabilita el uso de items (puertas, botones, etc.).
     * 
     * @param city      La ciudad a modificar
     * @param isEnabled true para permitir uso, false para denegarlo
     */
    public void setUseFlag(City city, boolean isEnabled) {
        setWorldGuardFlag(city, Flags.USE, isEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    /**
     * Habilita o deshabilita la interacción con entidades.
     * 
     * @param city      La ciudad a modificar
     * @param isEnabled true para permitir interacción, false para denegarla
     */
    public void setInteractFlag(City city, boolean isEnabled) {
        setWorldGuardFlag(city, Flags.INTERACT, isEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    /**
     * Método auxiliar para configurar flags de WorldGuard.
     * 
     * @param city  La ciudad
     * @param flag  La flag a modificar
     * @param state El nuevo estado de la flag
     */
    private void setWorldGuardFlag(City city, StateFlag flag, StateFlag.State state) {
        try {
            // Usar directamente el objeto World de la ciudad
            World world = city.getWorld();
            if (world == null) {
                LOGGER.warning("Mundo no encontrado para la ciudad: " + city.getName());
                return;
            }

            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                LOGGER.warning("RegionManager no encontrado para el mundo: " + world.getName());
                return;
            }

            ProtectedRegion region = regionManager.getRegion(city.getName());
            if (region == null) {
                LOGGER.warning("Región no encontrada: " + city.getName());
                return;
            }

            region.setFlag(flag, state);

        } catch (Exception e) {
            LOGGER.severe(
                    "Error al configurar flag " + flag.getName() + " para " + city.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Guarda el estado original de las protecciones antes de modificarlas.
     * 
     * @param city La ciudad cuyo estado se va a guardar
     */
    private void saveOriginalProtections(City city) {
        try {
            // Usar directamente el objeto World de la ciudad
            World world = city.getWorld();
            if (world == null)
                return;

            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null)
                return;

            ProtectedRegion region = regionManager.getRegion(city.getName());
            if (region == null)
                return;

            // Obtener estados actuales
            StateFlag.State pvpState = region.getFlag(Flags.PVP);
            StateFlag.State buildState = region.getFlag(Flags.BUILD);
            StateFlag.State breakState = region.getFlag(Flags.BLOCK_BREAK);
            StateFlag.State chestState = region.getFlag(Flags.CHEST_ACCESS);

            // Crear y guardar el estado
            CityProtectionState state = new CityProtectionState(
                    pvpState == StateFlag.State.ALLOW,
                    buildState == StateFlag.State.ALLOW,
                    breakState == StateFlag.State.ALLOW,
                    chestState == StateFlag.State.ALLOW);

            originalProtections.put(city.getId(), state);

        } catch (Exception e) {
            LOGGER.severe("Error al guardar estado original de protecciones: " + e.getMessage());
        }
    }

    /**
     * Aplica protecciones por defecto a una ciudad.
     * 
     * @param city La ciudad a proteger
     */
    private void applyDefaultProtections(City city) {
        setPvPFlag(city, false); // PvP deshabilitado por defecto
        setBuildFlag(city, false); // Construcción denegada por defecto
        setBreakFlag(city, false); // Destrucción denegada por defecto
        setChestAccessFlag(city, false); // Acceso a cofres denegado por defecto
        setUseFlag(city, false); // Uso de items denegado por defecto
        setInteractFlag(city, false); // Interacción denegada por defecto
    }

    /**
     * Limpia todos los estados guardados. Útil al reiniciar el plugin.
     */
    public void clearAllStates() {
        originalProtections.clear();
        LOGGER.info("Estados de protección limpiados");
    }

    /**
     * Clase auxiliar para almacenar el estado original de las protecciones.
     */
    private static class CityProtectionState {
        private final boolean pvpEnabled;
        private final boolean buildEnabled;
        private final boolean breakEnabled;
        private final boolean chestAccessEnabled;

        public CityProtectionState(boolean pvpEnabled, boolean buildEnabled,
                boolean breakEnabled, boolean chestAccessEnabled) {
            this.pvpEnabled = pvpEnabled;
            this.buildEnabled = buildEnabled;
            this.breakEnabled = breakEnabled;
            this.chestAccessEnabled = chestAccessEnabled;
        }

        public boolean isPvpEnabled() {
            return pvpEnabled;
        }

        public boolean isBuildEnabled() {
            return buildEnabled;
        }

        public boolean isBreakEnabled() {
            return breakEnabled;
        }

        public boolean isChestAccessEnabled() {
            return chestAccessEnabled;
        }
    }
}