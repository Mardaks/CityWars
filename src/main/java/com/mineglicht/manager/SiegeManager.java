package com.mineglicht.manager;

import com.mineglicht.models.City;
import com.mineglicht.models.Siege;
import com.mineglicht.models.SiegeState;
import com.mineglicht.api.event.SiegeStartEvent;
import com.mineglicht.api.event.SiegeEndEvent;
import com.mineglicht.event.LootPhaseStartEvent;
import com.mineglicht.task.LootTimerTask;
import com.mineglicht.task.SiegeCooldownTask;
import com.mineglicht.integration.ExecutableItemsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gestor principal de asedios en CityWars.
 * Se encarga de iniciar, gestionar y finalizar asedios entre ciudades.
 * Coordina con otros managers para manejar protecciones, economía y recompensas.
 */
public class SiegeManager {
    
    private static final Logger LOGGER = Bukkit.getLogger();
    
    // Configuración de asedios
    private static final int MIN_ATTACKERS = 3;
    private static final int SIEGE_DURATION_MINUTES = 30;
    private static final int LOOT_DURATION_MINUTES = 5;
    private static final double SIEGE_REWARD_PERCENTAGE = 0.5; // 50% del fondo de la ciudad
    private static final int COOLDOWN_HOURS = 24;
    
    // Dependencias
    private final ProtectionOverrideManager protectionManager;
    private final EconomyManager economyManager;
    private final ExecutableItemsIntegration executableItems;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    
    // Estado de asedios activos
    private final Map<UUID, Siege> activeSieges;
    private final Map<UUID, BukkitTask> siegeTimers;
    private final Map<UUID, BukkitTask> lootTimers;
    private final Map<String, LocalDateTime> siegeCooldowns; // "cityA-cityB" -> cooldown end time
    
    public SiegeManager(ProtectionOverrideManager protectionManager, 
                       EconomyManager economyManager,
                       ExecutableItemsIntegration executableItems,
                       CityManager cityManager) {
        this.protectionManager = protectionManager;
        this.economyManager = economyManager;
        this.executableItems = executableItems;
        this.cityManager = cityManager;
        this.activeSieges = new ConcurrentHashMap<>();
        this.siegeTimers = new ConcurrentHashMap<>();
        this.lootTimers = new ConcurrentHashMap<>();
        this.siegeCooldowns = new ConcurrentHashMap<>();
    }
    
    /**
     * Inicia un asedio contra una ciudad.
     * 
     * @param attackedCity La ciudad objetivo del asedio
     * @param attackers Conjunto de UUIDs de los atacantes
     * @return true si el asedio se inició correctamente, false en caso contrario
     */
    public boolean startSiege(City attackedCity, Set<UUID> attackers) {
        if (!canStartSiege(attackedCity, attackers)) {
            return false;
        }
        
        try {
            // Crear el modelo del asedio
            Siege siege = new Siege(
                UUID.randomUUID(),
                attackedCity,
                attackers,
                LocalDateTime.now(),
                SiegeState.ACTIVE
            );
            
            // Registrar el asedio activo
            activeSieges.put(attackedCity.getId(), siege);
            
            // Desactivar protecciones de la ciudad
            protectionManager.disableCityProtections(attackedCity);
            
            // Programar finalización automática del asedio
            scheduleSiegeEnd(attackedCity, siege);
            
            // Disparar evento de inicio de asedio
            SiegeStartEvent startEvent = new SiegeStartEvent(attackedCity, attackers, siege);
            Bukkit.getPluginManager().callEvent(startEvent);
            
            // Notificar a todos los jugadores online
            notifyPlayersOfSiegeStart(attackedCity, attackers);
            
            LOGGER.info("Asedio iniciado contra " + attackedCity.getName() + 
                       " por " + attackers.size() + " atacantes");
            
            return true;
            
        } catch (Exception e) {
            LOGGER.severe("Error al iniciar asedio contra " + attackedCity.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si un asedio puede comenzar.
     * 
     * @param attackedCity La ciudad objetivo
     * @param attackers Los atacantes
     * @return true si el asedio puede comenzar, false en caso contrario
     */
    public boolean canStartSiege(City attackedCity, Set<UUID> attackers) {
        // Verificar que la ciudad no esté ya bajo asedio
        if (isSiegeActive(attackedCity)) {
            LOGGER.info("No se puede iniciar asedio: " + attackedCity.getName() + " ya está bajo asedio");
            return false;
        }
        
        // Verificar número mínimo de atacantes
        if (attackers.size() < MIN_ATTACKERS) {
            LOGGER.info("No se puede iniciar asedio: se requieren al menos " + MIN_ATTACKERS + " atacantes");
            return false;
        }
        
        // Verificar que los atacantes estén online
        long onlineAttackers = attackers.stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .count();
        
        if (onlineAttackers < MIN_ATTACKERS) {
            LOGGER.info("No se puede iniciar asedio: se requieren al menos " + MIN_ATTACKERS + " atacantes online");
            return false;
        }
        
        // Verificar que ningún atacante sea ciudadano de la ciudad atacada
        for (UUID attackerId : attackers) {
            if (attackedCity.isCitizen(attackerId)) {
                LOGGER.info("No se puede iniciar asedio: un atacante es ciudadano de la ciudad objetivo");
                return false;
            }
        }
        
        // Verificar cooldowns entre ciudades
        for (UUID attackerId : attackers) {
            City attackerCity = citizenManager.getPlayerCity(attackerId);
            if (attackerCity != null && isCooldownActive(attackerCity, attackedCity)) {
                LOGGER.info("No se puede iniciar asedio: cooldown activo entre ciudades");
                return false;
            }
        }
        
        // Verificar que la bandera de protección (Estandarte) esté presente
        if (!executableItems.hasProtectionFlag(attackedCity)) {
            LOGGER.info("No se puede iniciar asedio: la ciudad no tiene bandera de protección");
            return false;
        }
        
        // Verificar que se haya colocado una bandera de asedio válida
        if (!executableItems.hasSiegeFlagInCity(attackedCity, attackers)) {
            LOGGER.info("No se puede iniciar asedio: no hay bandera de asedio válida en la ciudad");
            return false;
        }
        
        return true;
    }
    
    /**
     * Finaliza un asedio y restaura las protecciones.
     * 
     * @param attackedCity La ciudad atacada
     * @param siege El modelo del asedio
     */
    public void endSiege(City attackedCity, Siege siege) {
        if (siege == null) {
            LOGGER.warning("Intento de finalizar asedio nulo para " + attackedCity.getName());
            return;
        }
        
        try {
            // Cancelar timers activos
            cancelSiegeTimers(attackedCity.getId());
            
            // Restaurar protecciones de la ciudad
            protectionManager.restoreCityProtections(attackedCity);
            
            // Actualizar estado del asedio
            siege.setState(SiegeState.ENDED);
            siege.setEndTime(LocalDateTime.now());
            
            // Remover de asedios activos
            activeSieges.remove(attackedCity.getId());
            
            // Establecer cooldown entre ciudades
            setCooldownBetweenCities(siege);
            
            // Disparar evento de fin de asedio
            SiegeEndEvent endEvent = new SiegeEndEvent(attackedCity, siege.getAttackers(), siege);
            Bukkit.getPluginManager().callEvent(endEvent);
            
            // Notificar a los jugadores
            notifyPlayersOfSiegeEnd(attackedCity, siege);
            
            LOGGER.info("Asedio finalizado para " + attackedCity.getName());
            
        } catch (Exception e) {
            LOGGER.severe("Error al finalizar asedio para " + attackedCity.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Distribuye los fondos del asedio entre los atacantes.
     * 
     * @param attackedCity La ciudad derrotada
     * @param attackers Los atacantes victoriosos
     */
    public void distributeSiegeRewards(City attackedCity, Set<UUID> attackers) {
        try {
            double cityFunds = economyManager.getCityBankBalance(attackedCity);
            double rewardAmount = cityFunds * SIEGE_REWARD_PERCENTAGE;
            
            if (rewardAmount <= 0) {
                LOGGER.info("No hay fondos para distribuir en " + attackedCity.getName());
                return;
            }
            
            // Calcular recompensa por atacante
            double individualReward = rewardAmount / attackers.size();
            
            // Retirar fondos de la ciudad
            economyManager.withdrawCityBank(attackedCity, rewardAmount);
            
            // Distribuir entre atacantes online
            for (UUID attackerId : attackers) {
                Player attacker = Bukkit.getPlayer(attackerId);
                if (attacker != null && attacker.isOnline()) {
                    economyManager.depositToPlayer(attacker, individualReward);
                    attacker.sendMessage("§6[CityWars] §aHas recibido §e" + String.format("%.2f", individualReward) + 
                                       " §agemas por el asedio exitoso!");
                }
            }
            
            LOGGER.info("Distribuidas " + String.format("%.2f", rewardAmount) + 
                       " gemas entre " + attackers.size() + " atacantes");
            
        } catch (Exception e) {
            LOGGER.severe("Error al distribuir recompensas del asedio: " + e.getMessage());
        }
    }
    
    /**
     * Activa la fase de saqueo después de la destrucción del Estandarte.
     * 
     * @param attackedCity La ciudad en fase de saqueo
     */
    public void startLooting(City attackedCity) {
        Siege siege = activeSieges.get(attackedCity.getId());
        if (siege == null) {
            LOGGER.warning("Intento de iniciar saqueo sin asedio activo en " + attackedCity.getName());
            return;
        }
        
        try {
            // Cambiar estado a saqueo
            cityManager.setSiegeState(attackedCity.getId(), SiegeState.FLAG_CAPTURED);
            
            // Desactivar protecciones adicionales (acceso a cofres, etc.)
            protectionManager.disableResidenceProtections(attackedCity);
            
            // Programar fin del saqueo
            scheduleLootEnd(attackedCity, siege);
            
            // Disparar evento de inicio de saqueo
            LootPhaseStartEvent lootEvent = new LootPhaseStartEvent(attackedCity, siege.getAttackers(), siege);
            Bukkit.getPluginManager().callEvent(lootEvent);
            
            // Notificar a los jugadores
            notifyPlayersOfLootStart(attackedCity, siege);
            
            LOGGER.info("Fase de saqueo iniciada en " + attackedCity.getName());
            
        } catch (Exception e) {
            LOGGER.severe("Error al iniciar saqueo en " + attackedCity.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Verifica si un asedio está activo en una ciudad.
     * 
     * @param attackedCity La ciudad a verificar
     * @return true si hay un asedio activo, false en caso contrario
     */
    public boolean isSiegeActive(City attackedCity) {
        Siege siege = activeSieges.get(attackedCity.getId());
        return siege != null && 
               (siege.getState() == SiegeState.ACTIVE || siege.getState() == SiegeState.FLAG_CAPTURED);
    }
    
    /**
     * Obtiene el estado del asedio de una ciudad.
     * 
     * @param city La ciudad
     * @return El estado del asedio o NONE si no hay asedio
     */
    public SiegeState getSiegeState(City city) {
        Siege siege = activeSieges.get(city.getId());
        return siege != null ? siege.getState() : SiegeState.NONE;
    }
    
    /**
     * Obtiene el modelo de asedio activo de una ciudad.
     * 
     * @param city La ciudad
     * @return El modelo de asedio o null si no hay asedio activo
     */
    public Siege getActiveSiege(City city) {
        return activeSieges.get(city.getId());
    }
    
    /**
     * Obtiene todos los asedios activos.
     * 
     * @return Mapa de asedios activos
     */
    public Map<UUID, Siege> getActiveSieges() {
        return new HashMap<>(activeSieges);
    }
    
    /**
     * Programa la finalización automática del asedio.
     */
    private void scheduleSiegeEnd(City attackedCity, Siege siege) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("CityWars"),
            () -> {
                if (isSiegeActive(attackedCity)) {
                    // Asedio terminó por tiempo límite (los defensores ganaron)
                    siege.setState(SiegeState.DEFENDED);
                    endSiege(attackedCity, siege);
                }
            },
            SIEGE_DURATION_MINUTES * 60 * 20L // Convertir minutos a ticks
        );
        
        siegeTimers.put(attackedCity.getId(), task);
    }
    
    /**
     * Programa el fin de la fase de saqueo.
     */
    private void scheduleLootEnd(City attackedCity, Siege siege) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("CityWars"),
            () -> {
                // Fin del saqueo - los atacantes ganaron
                siege.setState(SiegeState.FLAG_CAPTURED);
                distributeSiegeRewards(attackedCity, siege.getAttackers());
                endSiege(attackedCity, siege);
            },
            LOOT_DURATION_MINUTES * 60 * 20L // Convertir minutos a ticks
        );
        
        lootTimers.put(attackedCity.getId(), task);
    }
    
    /**
     * Cancela todos los timers de un asedio.
     */
    private void cancelSiegeTimers(UUID cityId) {
        BukkitTask siegeTimer = siegeTimers.remove(cityId);
        BukkitTask lootTimer = lootTimers.remove(cityId);
        
        if (siegeTimer != null && !siegeTimer.isCancelled()) {
            siegeTimer.cancel();
        }
        
        if (lootTimer != null && !lootTimer.isCancelled()) {
            lootTimer.cancel();
        }
    }
    
    /**
     * Establece cooldown entre ciudades después del asedio.
     */
    private void setCooldownBetweenCities(Siege siege) {
        for (UUID attackerId : siege.getAttackers()) {
            City attackerCity = cityManager.getCity(attackerId);
            if (attackerCity != null) {
                String cooldownKey = getCooldownKey(attackerCity, siege.getAttackedCity());
                siegeCooldowns.put(cooldownKey, LocalDateTime.now().plusHours(COOLDOWN_HOURS));
                break; // Solo necesitamos establecer el cooldown una vez
            }
        }
    }
    
    /**
     * Verifica si hay cooldown activo entre dos ciudades.
     */
    private boolean isCooldownActive(City city1, City city2) {
        String cooldownKey = getCooldownKey(city1, city2);
        LocalDateTime cooldownEnd = siegeCooldowns.get(cooldownKey);
        
        if (cooldownEnd == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(cooldownEnd)) {
            siegeCooldowns.remove(cooldownKey);
            return false;
        }
        
        return true;
    }
    
    /**
     * Genera una clave única para el cooldown entre dos ciudades.
     */
    private String getCooldownKey(City city1, City city2) {
        // Ordenar los nombres para que A-B y B-A sean la misma clave
        String name1 = city1.getName();
        String name2 = city2.getName();
        
        if (name1.compareTo(name2) < 0) {
            return name1 + "-" + name2;
        } else {
            return name2 + "-" + name1;
        }
    }
    
    /**
     * Notifica a los jugadores sobre el inicio del asedio.
     */
    private void notifyPlayersOfSiegeStart(City attackedCity, Set<UUID> attackers) {
        String message = "§c[CityWars] §eAsedio iniciado contra §6" + attackedCity.getName() + 
                        " §epor §c" + attackers.size() + " §eatacanates!";
        
        Bukkit.broadcastMessage(message);
    }
    
    /**
     * Notifica a los jugadores sobre el fin del asedio.
     */
    private void notifyPlayersOfSiegeEnd(City attackedCity, Siege siege) {
        String message;
        
        switch (siege.getState()) {
            case ATTACKERS_WON:
                message = "§c[CityWars] §6" + attackedCity.getName() + " §eha sido conquistada!";
                break;
            case DEFENDERS_WON:
                message = "§c[CityWars] §6" + attackedCity.getName() + " §eha resistido el asedio!";
                break;
            default:
                message = "§c[CityWars] §eEl asedio contra §6" + attackedCity.getName() + " §eha terminado.";
        }
        
        Bukkit.broadcastMessage(message);
    }
    
    /**
     * Notifica a los jugadores sobre el inicio del saqueo.
     */
    private void notifyPlayersOfLootStart(City attackedCity, Siege siege) {
        String message = "§c[CityWars] §4¡SAQUEO INICIADO! §6" + attackedCity.getName() + 
                        " §eestá siendo saqueada. Tiempo restante: §c" + LOOT_DURATION_MINUTES + " minutos";
        
        Bukkit.broadcastMessage(message);
    }
    
    /**
     * Limpia todos los asedios activos. Útil para reinicios del plugin.
     */
    public void shutdown() {
        // Cancelar todos los timers
        siegeTimers.values().forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });
        
        lootTimers.values().forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });
        
        // Restaurar protecciones de todas las ciudades bajo asedio
        for (Map.Entry<UUID, Siege> entry : activeSieges.entrySet()) {
            protectionManager.restoreCityProtections(entry.getValue().getAttackedCity());
        }
        
        // Limpiar mapas
        activeSieges.clear();
        siegeTimers.clear();
        lootTimers.clear();
        
        LOGGER.info("SiegeManager cerrado correctamente");
    }
}