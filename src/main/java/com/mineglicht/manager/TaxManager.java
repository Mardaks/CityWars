package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor del sistema de impuestos de ciudades.
 * Maneja la recolección automática de impuestos de los ciudadanos hacia el
 * fondo
 * bancario de su ciudad.
 */
public class TaxManager {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final EconomyManager economyManager;

    // Almacena la última vez que cada jugador pagó impuestos
    private final Map<UUID, Long> lastTaxPayment = new HashMap<>();

    // Tarea programada para cobrar impuestos
    private BukkitTask taxCollectionTask;

    // Tasa de impuesto predeterminada (18% según requerimientos)
    private final double DEFAULT_TAX_RATE = 0.18;

    public TaxManager(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.economyManager = plugin.getEconomyManager();

        // Iniciar la tarea de recolección de impuestos
        startTaxCollection();
    }

    /**
     * Inicia la tarea programada para cobrar impuestos cada cierto tiempo
     */
    private void startTaxCollection() {
        // Cancelar tarea previa si existe
        if (taxCollectionTask != null) {
            taxCollectionTask.cancel();
        }

        // Intervalo en ticks (20 ticks = 1 segundo)
        long checkInterval = 20L * 60L * 5L; // Verificar cada 5 minutos

        taxCollectionTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkTaxCollection, checkInterval,
                checkInterval);
        plugin.getLogger().info("Sistema de recolección de impuestos iniciado");
    }

    /**
     * Verifica qué jugadores online deben pagar impuestos
     */
    private void checkTaxCollection() {
        long currentTime = Instant.now().getEpochSecond();
        long taxIntervalSeconds = 86400; // 24 horas en segundos

        // Verificar todos los jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            Citizen citizen = citizenManager.getCitizen(playerId);

            // Saltar si el jugador no pertenece a una ciudad
            if (citizen == null || citizen.getCityId() == null) {
                continue;
            }

            // Verificar si es hora de pagar impuestos
            long lastPayment = lastTaxPayment.getOrDefault(playerId, 0L);
            if (currentTime - lastPayment >= taxIntervalSeconds) {
                collectTaxFromPlayer(playerId);
            }
        }
    }

    /**
     * Cobra impuestos a un jugador específico
     * 
     * @param playerId UUID del jugador
     * @return true si se cobraron los impuestos correctamente
     */
    public boolean collectTaxFromPlayer(UUID playerId) {
        Citizen citizen = citizenManager.getCitizen(playerId);
        if (citizen == null || citizen.getCityId() == null)
            return false;

        City city = cityManager.getCity(citizen.getCityId());
        if (city == null)
            return false;

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline())
            return false;

        // Si usas una moneda específica para impuestos (ej: "glichtcoins")
        String taxCurrency = "glichtcoins"; // O desde config
        double playerBalance = economyManager.getPlayerBalance(player, taxCurrency);

        if (playerBalance <= 0)
            return false;

        double taxRate = DEFAULT_TAX_RATE;
        double taxAmount = playerBalance * taxRate;

        // Retirar usando la moneda específica
        if (economyManager.withdrawCurrency(player, taxCurrency, taxAmount)) {
            // Depositar al banco de la ciudad
            if (economyManager.depositCityBank(city, taxAmount)) {
                lastTaxPayment.put(playerId, Instant.now().getEpochSecond());

                player.sendMessage(plugin.getMessages().getTaxCollectedMessage(BigDecimal.valueOf(taxAmount)));
                return true;
            } else {
                // Devolver dinero si falla
                economyManager.depositCurrency(player, taxCurrency, taxAmount);
            }
        }

        return false;
    }

    /**
     * Fuerza la recaudación de impuestos para todos los ciudadanos de una ciudad
     * 
     * @param cityId ID de la ciudad
     * @return Número de ciudadanos que pagaron impuestos
     */
    public int forceCollectCityTaxes(UUID cityId) {
        City city = cityManager.getCity(cityId);
        if (city == null)
            return 0;

        int totalPaid = 0;

        for (UUID citizenId : city.getCitizens()) {
            if (collectTaxFromPlayer(citizenId)) {
                totalPaid++;
            }
        }

        return totalPaid;
    }

    /**
     * Establece la tasa de impuestos para una ciudad
     * 
     * @param cityId  ID de la ciudad
     * @param taxRate Nueva tasa de impuestos (0.0 - 1.0)
     * @return true si se estableció correctamente
     */
    public boolean setTaxRate(UUID cityId, double taxRate) {
        if (taxRate < 0.0 || taxRate > 1.0)
            return false;

        City city = cityManager.getCity(cityId);
        if (city == null)
            return false;

        city.setTaxRate(taxRate);
        cityManager.saveCities();
        return true;
    }

    /**
     * Obtiene el tiempo restante hasta el próximo pago de impuestos
     * 
     * @param playerId UUID del jugador
     * @return Segundos restantes, o -1 si el jugador no tiene ciudad
     */
    public long getTimeUntilNextTax(UUID playerId) {
        Citizen citizen = citizenManager.getCitizen(playerId);
        if (citizen == null || citizen.getCityId() == null)
            return -1;

        long lastPayment = lastTaxPayment.getOrDefault(playerId, 0L);
        long currentTime = Instant.now().getEpochSecond();
        long taxIntervalSeconds = 86400; // 24 horas

        // Calcular tiempo restante
        long timeElapsed = currentTime - lastPayment;
        long timeRemaining = taxIntervalSeconds - timeElapsed;

        return Math.max(0, timeRemaining);
    }

    /**
     * Exime a un jugador de pagar impuestos por un tiempo específico
     * 
     * @param playerId         UUID del jugador
     * @param exemptionSeconds Duración de la exención en segundos
     */
    public void exemptFromTaxes(UUID playerId, long exemptionSeconds) {
        // Actualizamos la marca de tiempo como si hubiera pagado recientemente
        long newTimestamp = Instant.now().getEpochSecond() - 86400 + exemptionSeconds;
        lastTaxPayment.put(playerId, newTimestamp);
    }

    /**
     * Limpia los datos de impuestos para un jugador (al salir de una ciudad)
     * 
     * @param playerId UUID del jugador
     */
    public void clearTaxData(UUID playerId) {
        lastTaxPayment.remove(playerId);
    }

    /**
     * Limpia los datos de impuestos para todos los ciudadanos de una ciudad (al
     * eliminar la ciudad)
     * 
     * @param cityId ID de la ciudad
     */
    public void clearCityTaxData(UUID cityId) {
        City city = cityManager.getCity(cityId);
        if (city == null)
            return;

        for (UUID citizenId : city.getCitizens()) {
            lastTaxPayment.remove(citizenId);
        }
    }

    /**
     * Deshabilita el sistema de impuestos
     */
    public void shutdown() {
        if (taxCollectionTask != null) {
            taxCollectionTask.cancel();
            taxCollectionTask = null;
        }
    }
}
