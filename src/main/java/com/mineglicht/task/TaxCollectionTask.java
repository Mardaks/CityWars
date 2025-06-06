package com.mineglicht.task;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import com.mineglicht.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Tarea programada para la recolección automática de impuestos diarios
 * Se ejecuta cada 24 horas y cobra el 18% del balance de cada ciudadano
 */
public class TaxCollectionTask extends BukkitRunnable {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final EconomyManager economyManager;
    private static final double TAX_RATE = 0.18; // 18%

    public TaxCollectionTask(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @Override
    public void run() {
        plugin.getLogger().info("Iniciando recolección de impuestos diarios...");

        int totalCitizens = 0;
        double totalTaxesCollected = 0.0;

        // Iterar sobre todas las ciudades
        for (City city : cityManager.getAllCities()) {
            double cityTaxes = collectCityTaxes(city);
            totalTaxesCollected += cityTaxes;
        }

        plugin.getLogger().info(String.format(
                "Recolección de impuestos completada. Ciudadanos: %d, Total recaudado: %.2f",
                totalCitizens, totalTaxesCollected));
    }

    /**
     * Recolecta impuestos de todos los ciudadanos de una ciudad
     */
    private double collectCityTaxes(City city) {
        Set<UUID> citizenUuids = citizenManager.getCitizensInCity(city.getId());
        double totalCityTaxes = 0.0;
        int taxedCitizens = 0;

        for (UUID citizenUuid : citizenUuids) {
            Citizen citizen = citizenManager.getCitizen(citizenUuid);
            if (citizen != null) {
                double taxCollected = collectCitizenTax(citizen, city);
                if (taxCollected > 0) {
                    totalCityTaxes += taxCollected;
                    taxedCitizens++;
                }
            }
        }

        // Depositar los impuestos al banco de la ciudad usando el nuevo método
        if (totalCityTaxes > 0) {
            if (economyManager.depositCityBank(city, totalCityTaxes)) {
                plugin.getLogger().info(String.format(
                        "Ciudad %s: %d ciudadanos pagaron %.2f en impuestos",
                        city.getName(), taxedCitizens, totalCityTaxes));
            } else {
                plugin.getLogger().warning(String.format(
                        "Error al depositar %.2f impuestos en el banco de la ciudad %s",
                        totalCityTaxes, city.getName()));
            }
        }

        return totalCityTaxes;
    }

    /**
     * Recolecta impuestos de un ciudadano específico
     */
    private double collectCitizenTax(Citizen citizen, City city) {
    try {
        Player player = Bukkit.getPlayer(citizen.getPlayerId());
        
        // Verificar si el jugador está conectado (necesario para los métodos de currency)
        if (player == null || !player.isOnline()) {
            return 0.0;
        }
        
        // Obtener el balance del jugador (necesitarás implementar este método o usar una alternativa)
        double playerBalance = economyManager.getPlayerBalance(player, "gems"); // Asumiendo "gems" como moneda
        
        // Verificar si el jugador tiene suficiente dinero para pagar impuestos
        if (playerBalance <= 0) {
            return 0.0;
        }
        
        double taxAmount = playerBalance * TAX_RATE;
        
        // Obtener nombre del jugador
        String playerName = player.getName();
        
        // Verificar si el jugador tiene suficiente dinero para pagar los impuestos
        if (!economyManager.hasCurrency(player, "gems", taxAmount)) {
            plugin.getLogger().warning(String.format(
                "El jugador %s no tiene suficientes gems para pagar impuestos (%.2f)",
                playerName, taxAmount
            ));
            return 0.0;
        }
        
        // Descontar los impuestos del jugador
        if (economyManager.withdrawCurrency(player, "gems", taxAmount)) {
            // Notificar al jugador
            MessageUtils.sendMessage(player, 
                String.format("&e¡Impuestos cobrados! Se han descontado &c%.2f gems &ede tu balance para la ciudad &b%s&e.", 
                taxAmount, city.getName()));
            
            return taxAmount;
        } else {
            plugin.getLogger().warning(String.format(
                "No se pudieron cobrar impuestos a %s (UUID: %s) - Error en withdrawCurrency",
                playerName, citizen.getPlayerId()
            ));
            return 0.0;
        }
        
    } catch (Exception e) {
        String playerName = Bukkit.getOfflinePlayer(citizen.getPlayerId()).getName();
        plugin.getLogger().log(Level.SEVERE, 
            String.format("Error al cobrar impuestos a %s: %s", 
            playerName != null ? playerName : citizen.getPlayerId().toString(), e.getMessage()), e);
        return 0.0;
    }
}

    /**
     * Inicia la tarea de recolección de impuestos
     * Se ejecuta cada 24 horas (1728000 ticks)
     */
    public void start() {
        // Programar para ejecutarse cada 24 horas
        long period = 20L * 60L * 60L * 24L; // 24 horas en ticks (20 ticks = 1 segundo)

        // Obtener el delay inicial desde la configuración (por defecto 1 hora)
        long initialDelay = plugin.getConfig().getLong("tax-collection.initial-delay-hours", 1) * 20L * 60L * 60L;

        this.runTaskTimerAsynchronously(plugin, initialDelay, period);

        plugin.getLogger().info("Tarea de recolección de impuestos iniciada. " +
                "Primer cobro en " + (initialDelay / (20L * 60L * 60L)) + " horas, " +
                "luego cada 24 horas.");
    }

    /**
     * Detiene la tarea de recolección de impuestos
     */
    public void stop() {
        if (!this.isCancelled()) {
            this.cancel();
            plugin.getLogger().info("Tarea de recolección de impuestos detenida.");
        }
    }

    /**
     * Fuerza una recolección manual de impuestos (para comandos de admin)
     */
    public void forceCollection() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::run);
        plugin.getLogger().info("Recolección de impuestos forzada por administrador.");
    }
}
