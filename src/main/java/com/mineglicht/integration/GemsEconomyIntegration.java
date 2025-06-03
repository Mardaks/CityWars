package com.mineglicht.integration;

import me.xanium.gemseconomy.api.GemsEconomyAPI;
import me.xanium.gemseconomy.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Integración con GemsEconomy para el manejo de múltiples economías
 */
public class GemsEconomyIntegration {

    private final Plugin plugin;
    private final GemsEconomyAPI gemsAPI;
    private boolean isEnabled = false;

    // Configuración de economías
    private String siegeCurrency = "jp"; // Moneda para asedios (configurable)
    private String taxCurrency = "glichtcoins"; // Moneda para impuestos (configurable)

    public GemsEconomyIntegration(Plugin plugin) {
        this.plugin = plugin;
        this.gemsAPI = new GemsEconomyAPI();
        setupIntegration();
    }

    /**
     * Configura la integración con GemsEconomy
     */
    private void setupIntegration() {
        try {
            if (Bukkit.getPluginManager().getPlugin("GemsEconomy") == null) {
                plugin.getLogger().warning("GemsEconomy no está instalado. La integración económica está deshabilitada.");
                return;
            }

            if (!Bukkit.getPluginManager().getPlugin("GemsEconomy").isEnabled()) {
                plugin.getLogger().warning("GemsEconomy no está habilitado. La integración económica está deshabilitada.");
                return;
            }

            // Verificar que las monedas configuradas existen
            if (getCurrency(siegeCurrency) == null) {
                plugin.getLogger().warning("La moneda de asedio '" + siegeCurrency + "' no existe en GemsEconomy.");
            }

            if (getCurrency(taxCurrency) == null) {
                plugin.getLogger().warning("La moneda de impuestos '" + taxCurrency + "' no existe en GemsEconomy.");
            }

            isEnabled = true;
            plugin.getLogger().info("Integración con GemsEconomy habilitada exitosamente.");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al configurar la integración con GemsEconomy: " + e.getMessage());
            isEnabled = false;
        }
    }

    /**
     * Verifica si la integración está disponible
     * @return true si GemsEconomy está disponible
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Obtiene una moneda por su nombre
     * @param currencyName Nombre de la moneda
     * @return La moneda o null si no existe
     */
    public Currency getCurrency(String currencyName) {
        try {
            return gemsAPI.getCurrency(currencyName);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al obtener la moneda '" + currencyName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el balance de un jugador en una moneda específica
     * @param playerUUID UUID del jugador
     * @param currencyName Nombre de la moneda
     * @return El balance o 0 si hay error
     */
    public double getBalance(UUID playerUUID, String currencyName) {
        if (!isEnabled) return 0.0;

        try {
            Currency currency = getCurrency(currencyName);
            if (currency == null) {
                // Si no se especifica moneda, usar la predeterminada
                return gemsAPI.getBalance(playerUUID);
            }
            
            return gemsAPI.getBalance(playerUUID, currency);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al obtener balance: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Obtiene el balance de un jugador en una moneda específica
     * @param player El jugador
     * @param currencyName Nombre de la moneda
     * @return El balance o 0 si hay error
     */
    public double getBalance(Player player, String currencyName) {
        return getBalance(player.getUniqueId(), currencyName);
    }

    /**
     * Establece el balance de un jugador (mediante withdraw/deposit)
     * @param playerUUID UUID del jugador
     * @param currencyName Nombre de la moneda
     * @param amount Cantidad a establecer
     * @return true si fue exitoso
     */
    public boolean setBalance(UUID playerUUID, String currencyName, double amount) {
        if (!isEnabled) return false;

        try {
            double currentBalance = getBalance(playerUUID, currencyName);
            
            if (currentBalance == amount) {
                return true; // Ya tiene el balance correcto
            }
            
            // Si necesita más dinero, depositar la diferencia
            if (currentBalance < amount) {
                return addBalance(playerUUID, currencyName, amount - currentBalance);
            } 
            // Si tiene más dinero, retirar la diferencia
            else {
                return removeBalance(playerUUID, currencyName, currentBalance - amount);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al establecer balance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Añade dinero al balance de un jugador
     * @param playerUUID UUID del jugador
     * @param currencyName Nombre de la moneda
     * @param amount Cantidad a añadir
     * @return true si fue exitoso
     */
    public boolean addBalance(UUID playerUUID, String currencyName, double amount) {
        if (!isEnabled) return false;

        try {
            Currency currency = getCurrency(currencyName);
            double balanceBefore = getBalance(playerUUID, currencyName);
            
            if (currency == null) {
                // Usar moneda predeterminada
                gemsAPI.deposit(playerUUID, amount);
            } else {
                gemsAPI.deposit(playerUUID, amount, currency);
            }
            
            // Verificar si el balance cambió correctamente
            double balanceAfter = getBalance(playerUUID, currencyName);
            return Math.abs((balanceAfter - balanceBefore) - amount) < 0.01; // Tolerancia para decimales
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error al añadir balance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remueve dinero del balance de un jugador
     * @param playerUUID UUID del jugador
     * @param currencyName Nombre de la moneda
     * @param amount Cantidad a remover
     * @return true si fue exitoso
     */
    public boolean removeBalance(UUID playerUUID, String currencyName, double amount) {
        if (!isEnabled) return false;

        if (!hasBalance(playerUUID, currencyName, amount)) {
            return false;
        }

        try {
            Currency currency = getCurrency(currencyName);
            double balanceBefore = getBalance(playerUUID, currencyName);
            
            if (currency == null) {
                // Usar moneda predeterminada
                gemsAPI.withdraw(playerUUID, amount);
            } else {
                gemsAPI.withdraw(playerUUID, amount, currency);
            }
            
            // Verificar si el balance cambió correctamente
            double balanceAfter = getBalance(playerUUID, currencyName);
            return Math.abs((balanceBefore - balanceAfter) - amount) < 0.01; // Tolerancia para decimales
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error al remover balance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un jugador tiene suficiente dinero
     * @param playerUUID UUID del jugador
     * @param currencyName Nombre de la moneda
     * @param amount Cantidad requerida
     * @return true si tiene suficiente dinero
     */
    public boolean hasBalance(UUID playerUUID, String currencyName, double amount) {
        return getBalance(playerUUID, currencyName) >= amount;
    }

    /**
     * Verifica si un jugador tiene suficiente dinero para un asedio
     * @param player El jugador
     * @param requiredAmount Cantidad requerida
     * @return true si tiene suficiente dinero
     */
    public boolean canAffordSiege(Player player, double requiredAmount) {
        return hasBalance(player.getUniqueId(), siegeCurrency, requiredAmount);
    }

    /**
     * Cobra el costo de un asedio
     * @param player El jugador
     * @param cost Costo del asedio
     * @return true si el pago fue exitoso
     */
    public boolean chargeSiegeCost(Player player, double cost) {
        return removeBalance(player.getUniqueId(), siegeCurrency, cost);
    }

    /**
     * Recolecta impuestos de un jugador (18% del balance)
     * @param playerUUID UUID del jugador
     * @return La cantidad de impuestos recolectados
     */
    public double collectTax(UUID playerUUID) {
        if (!isEnabled) return 0.0;

        double currentBalance = getBalance(playerUUID, taxCurrency);
        if (currentBalance <= 0) return 0.0;

        double taxAmount = currentBalance * 0.18; // 18% de impuestos

        if (removeBalance(playerUUID, taxCurrency, taxAmount)) {
            return taxAmount;
        }

        return 0.0;
    }

    /**
     * Deposita dinero en el fondo bancario de una ciudad
     * @param cityBankAccount Cuenta bancaria de la ciudad (UUID ficticio)
     * @param amount Cantidad a depositar
     * @return true si fue exitoso
     */
    public boolean depositToCityBank(UUID cityBankAccount, double amount) {
        return addBalance(cityBankAccount, taxCurrency, amount);
    }

    /**
     * Retira dinero del fondo bancario de una ciudad
     * @param cityBankAccount Cuenta bancaria de la ciudad
     * @param amount Cantidad a retirar
     * @return true si fue exitoso
     */
    public boolean withdrawFromCityBank(UUID cityBankAccount, double amount) {
        return removeBalance(cityBankAccount, taxCurrency, amount);
    }

    /**
     * Obtiene el balance del fondo bancario de una ciudad
     * @param cityBankAccount Cuenta bancaria de la ciudad
     * @return El balance del fondo bancario
     */
    public double getCityBankBalance(UUID cityBankAccount) {
        return getBalance(cityBankAccount, taxCurrency);
    }

    /**
     * Transfiere dinero entre cuentas
     * @param fromUUID Cuenta origen
     * @param toUUID Cuenta destino
     * @param currencyName Nombre de la moneda
     * @param amount Cantidad a transferir
     * @return true si fue exitoso
     */
    public boolean transferMoney(UUID fromUUID, UUID toUUID, String currencyName, double amount) {
        if (!hasBalance(fromUUID, currencyName, amount)) {
            return false;
        }

        if (removeBalance(fromUUID, currencyName, amount)) {
            return addBalance(toUUID, currencyName, amount);
        }

        return false;
    }

    /**
     * Distribuye el botín de guerra (50% del fondo bancario)
     * @param defeatedCityBank Cuenta de la ciudad derrotada
     * @param attackers Lista de atacantes para dividir el botín
     * @return true si la distribución fue exitosa
     */
    public boolean distributeSiegeLoot(UUID defeatedCityBank, java.util.List<UUID> attackers) {
        if (attackers == null || attackers.isEmpty()) return false;

        double cityBalance = getCityBankBalance(defeatedCityBank);
        double lootAmount = cityBalance * 0.5; // 50% del fondo bancario

        if (lootAmount <= 0) return false;

        // Retirar el dinero de la ciudad derrotada
        if (!withdrawFromCityBank(defeatedCityBank, lootAmount)) {
            return false;
        }

        // Distribuir equitativamente entre los atacantes
        double sharePerAttacker = lootAmount / attackers.size();

        for (UUID attackerUUID : attackers) {
            addBalance(attackerUUID, taxCurrency, sharePerAttacker);
        }

        return true;
    }

    /**
     * Formatea una cantidad de dinero con el símbolo de la moneda
     * @param amount Cantidad
     * @param currencyName Nombre de la moneda
     * @return String formateado
     */
    public String formatMoney(double amount, String currencyName) {
        Currency currency = getCurrency(currencyName);
        if (currency == null) {
            return String.format("%.2f %s", amount, currencyName);
        }

        // Verificar si la moneda tiene métodos para formatear
        try {
            return currency.getSymbol() + String.format("%.2f", amount);
        } catch (Exception e) {
            return String.format("%.2f %s", amount, currencyName);
        }
    }

    /**
     * Crea una cuenta bancaria para una ciudad
     * @param cityName Nombre de la ciudad
     * @return UUID de la cuenta bancaria
     */
    public UUID createCityBankAccount(String cityName) {
        // Generar un UUID determinístico basado en el nombre de la ciudad
        return UUID.nameUUIDFromBytes(("citybank_" + cityName.toLowerCase()).getBytes());
    }

    // Getters y Setters para configuración
    public String getSiegeCurrency() {
        return siegeCurrency;
    }

    public void setSiegeCurrency(String siegeCurrency) {
        this.siegeCurrency = siegeCurrency;
    }

    public String getTaxCurrency() {
        return taxCurrency;
    }

    public void setTaxCurrency(String taxCurrency) {
        this.taxCurrency = taxCurrency;
    }
}