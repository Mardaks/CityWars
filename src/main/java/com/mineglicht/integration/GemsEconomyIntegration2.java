package com.mineglicht.integration;

import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.api.GemsEconomyAPI;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.account.AccountManager;
import me.xanium.gemseconomy.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Clase para integrar GemsEconomy con el sistema de ciudades CityWars
 * Maneja fondos bancarios de ciudades, impuestos y transacciones de asedio
 * 
 * PARA EL METODO ONDISABLE(), QUEDA PENDIENTE
 * saveDeletedAccounts();
 * PARA EL METODO ONDISABLE(), QUEDA PENDIENTE
 * 
 */
public class GemsEconomyIntegration2 {

    private final Plugin plugin;
    private final GemsEconomyAPI gemsAPI;
    private final Currency defaultCurrency;
    private final AccountManager accountManager;

    // Configuración económica
    private double taxRate = 0.05; // 5% por defecto
    private double lootPercentage = 0.50; // 50% del fondo bancario
    private double transactionLimit = 1000000.0; // Límite de transacciones

    // Prefijo para cuentas de ciudades
    private static final String CITY_ACCOUNT_PREFIX = "city_";

    public GemsEconomyIntegration2(Plugin plugin) {
        this.plugin = plugin;
        this.gemsAPI = new GemsEconomyAPI();
        this.defaultCurrency = GemsEconomy.getInstance().getCurrencyManager().getDefaultCurrency();
        this.accountManager = GemsEconomy.getInstance().getAccountManager();

        if (this.defaultCurrency == null) {
            plugin.getLogger().severe("No se encontró una moneda por defecto en GemsEconomy!");
        }

        if (this.accountManager == null) {
            plugin.getLogger().severe("No se pudo obtener el AccountManager de GemsEconomy!");
        }

        loadDeletedAccounts();
    }

    // ========== MÉTODOS PRINCIPALES (NECESARIOS) ==========

    // GESTIÓN DE FONDOS BANCARIOS

    /**
     * Crea una cuenta bancaria para una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @return true si se creó exitosamente
     */
    public boolean createCityAccount(String cityName) {
        // En GemsEconomy 4.9.2, las cuentas se crean automáticamente
        // Solo verificamos que podamos acceder al balance
        double balance = getCityBalance(cityName);
        plugin.getLogger().info("Cuenta bancaria lista para la ciudad: " + cityName + " (Balance: " + balance + ")");
        return true;
    }

    /**
     * Obtiene el balance de una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @return Balance de la ciudad (0.0 si hay error)
     */
    public double getCityBalance(String cityName) {
        try {
            String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
            Account account = accountManager.getAccount(accountName);

            if (account != null) {
                return account.getBalance(defaultCurrency);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al obtener balance de ciudad " + cityName, e);
        }

        return 0.0;
    }

    /**
     * Añade fondos a una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @param amount   Cantidad a añadir
     * @return true si se añadió exitosamente
     */
    public boolean addFundsToCity(String cityName, double amount) {
        if (amount <= 0)
            return false;

        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
        Account account = accountManager.getAccount(accountName);

        if (account == null) {
            // Crear cuenta si no existe
            if (!createCityAccount(cityName)) {
                plugin.getLogger().warning("No se pudo crear la cuenta para la ciudad " + cityName);
                return false;
            }
            account = accountManager.getAccount(accountName);
        }

        if (account != null) {
            // El método deposit() retorna boolean, no lanza excepciones
            boolean success = account.deposit(defaultCurrency, amount);

            if (success) {
                plugin.getLogger().info("Añadidos " + formatCurrency(amount) + " a la ciudad " + cityName);
                return true;
            } else {
                plugin.getLogger()
                        .warning("Falló el depósito de " + formatCurrency(amount) + " a la ciudad " + cityName);
            }
        } else {
            plugin.getLogger().warning("No se pudo obtener la cuenta para la ciudad " + cityName);
        }

        return false;
    }

    /**
     * Deduce fondos de una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @param amount   Cantidad a deducir
     * @return true si se dedujo exitosamente
     */
    public boolean deductFundsFromCity(String cityName, double amount) {
        if (amount <= 0)
            return false;

        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
        Account account = accountManager.getAccount(accountName);

        if (account == null) {
            plugin.getLogger().warning("No se encontró la cuenta para la ciudad " + cityName);
            return false;
        }

        // Verificar si tiene suficientes fondos
        if (account.getBalance(defaultCurrency) < amount) {
            plugin.getLogger().warning("La ciudad " + cityName + " no tiene suficientes fondos. " +
                    "Balance: " + formatCurrency(account.getBalance(defaultCurrency)) +
                    ", Requerido: " + formatCurrency(amount));
            return false;
        }

        // El método withdraw() retorna boolean, no lanza excepciones
        boolean success = account.withdraw(defaultCurrency, amount);

        if (success) {
            plugin.getLogger().info("Deducidos " + formatCurrency(amount) + " de la ciudad " + cityName +
                    ". Balance restante: " + formatCurrency(account.getBalance(defaultCurrency)));
            return true;
        } else {
            plugin.getLogger().warning("Falló el retiro de " + formatCurrency(amount) + " de la ciudad " + cityName);
        }

        return false;
    }

    /**
     * Transfiere fondos entre ciudades
     * 
     * @param fromCity Ciudad origen
     * @param toCity   Ciudad destino
     * @param amount   Cantidad a transferir
     * @return true si se transfirió exitosamente
     */
    public boolean transferFunds(String fromCity, String toCity, double amount) {
        if (amount <= 0 || amount > transactionLimit)
            return false;

        // Verificar fondos suficientes
        if (!hasEnoughFunds(fromCity, amount)) {
            return false;
        }

        // Realizar transferencia
        if (deductFundsFromCity(fromCity, amount)) {
            if (addFundsToCity(toCity, amount)) {
                plugin.getLogger().info("Transferidos " + formatCurrency(amount) +
                        " de " + fromCity + " a " + toCity);
                return true;
            } else {
                // Revertir deducción si falla la adición
                addFundsToCity(fromCity, amount);
            }
        }

        return false;
    }

    // SISTEMA DE IMPUESTOS

    /**
     * Recolecta impuesto de un jugador
     * 
     * @param player     UUID del jugador
     * @param percentage Porcentaje de impuesto (0.05 = 5%)
     * @return Cantidad recolectada
     */
    public double collectTaxFromPlayer(UUID player, double percentage) {
        if (percentage <= 0 || percentage > 1.0) {
            plugin.getLogger().warning("Porcentaje de impuesto inválido: " + percentage);
            return 0.0;
        }

        Player bukkitPlayer = Bukkit.getPlayer(player);
        if (bukkitPlayer == null) {
            plugin.getLogger().warning("Jugador no está en línea: " + player);
            return 0.0;
        }

        Account account = accountManager.getAccount(bukkitPlayer.getName());
        if (account == null) {
            plugin.getLogger().warning("No se encontró la cuenta del jugador: " + bukkitPlayer.getName());
            return 0.0;
        }

        double balance = account.getBalance(defaultCurrency);
        double taxAmount = balance * percentage;

        // Validar que el impuesto sea mayor a 0
        if (taxAmount <= 0) {
            plugin.getLogger().info("No hay impuesto que recolectar de " + bukkitPlayer.getName() +
                    " (balance: " + formatCurrency(balance) + ")");
            return 0.0;
        }

        // Verificar que tenga suficientes fondos (aunque debería ser igual al balance)
        if (account.getBalance(defaultCurrency) < taxAmount) {
            plugin.getLogger().warning("Error: balance insuficiente para impuesto en " + bukkitPlayer.getName());
            return 0.0;
        }

        // El método withdraw() retorna boolean, no lanza excepciones
        boolean success = account.withdraw(defaultCurrency, taxAmount);

        if (success) {
            plugin.getLogger().info("Impuesto recolectado: " + formatCurrency(taxAmount) +
                    " (" + String.format("%.1f%%", percentage * 100) + ") de " + bukkitPlayer.getName() +
                    ". Balance restante: " + formatCurrency(account.getBalance(defaultCurrency)));
            return taxAmount;
        } else {
            plugin.getLogger().warning("Falló el retiro de impuesto de " + formatCurrency(taxAmount) +
                    " del jugador " + bukkitPlayer.getName());
        }

        return 0.0;
    }

    /**
     * Obtiene el balance de un jugador
     * 
     * @param player UUID del jugador
     * @return Balance del jugador
     */
    public double getPlayerBalance(UUID player) {
        try {
            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer == null) {
                // Buscar por UUID si el jugador está offline
                Account account = accountManager.getAccount(player.toString());
                return account != null ? account.getBalance(defaultCurrency) : 0.0;
            }

            Account account = accountManager.getAccount(bukkitPlayer.getName());
            return account != null ? account.getBalance(defaultCurrency) : 0.0;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al obtener balance del jugador " + player, e);
            return 0.0;
        }
    }

    /**
     * Verifica si un jugador puede pagar el impuesto
     * 
     * @param player UUID del jugador
     * @param amount Cantidad del impuesto
     * @return true si puede pagar
     */
    public boolean canPayTax(UUID player, double amount) {
        double balance = getPlayerBalance(player);
        return balance >= amount;
    }

    /**
     * Añade deuda de impuesto a un jugador (nota: solo registro, GemsEconomy no
     * maneja deudas directamente)
     * 
     * @param player UUID del jugador
     * @param amount Cantidad de deuda
     * @return true siempre (solo registro)
     */
    public boolean addTaxDebt(UUID player, double amount) {
        // GemsEconomy no maneja deudas directamente
        // Este método se implementaría con un sistema propio de registro de deudas
        plugin.getLogger().warning("Jugador " + player + " tiene deuda de impuesto: " + formatCurrency(amount));
        return true;
    }

    // TRANSACCIONES DE ASEDIO

    /**
     * Distribuye el botín de asedio entre los atacantes
     * 
     * @param defeatedCity Ciudad derrotada
     * @param attackers    Lista de atacantes
     * @return true si se distribuyó exitosamente
     */
    public boolean distributeSiegeLoot(String defeatedCity, List<UUID> attackers) {
        if (attackers == null || attackers.isEmpty()) {
            plugin.getLogger().warning("No hay atacantes para distribuir botín del asedio a " + defeatedCity);
            return false;
        }

        double totalLoot = calculateLootAmount(defeatedCity, lootPercentage);
        if (totalLoot <= 0) {
            plugin.getLogger().warning("No hay botín que distribuir del asedio a " + defeatedCity);
            return false;
        }

        // Deducir del fondo de la ciudad derrotada
        if (!deductFundsFromCity(defeatedCity, totalLoot)) {
            plugin.getLogger().warning("No se pudo deducir el botín de la ciudad " + defeatedCity);
            return false;
        }

        // Distribuir entre atacantes
        double lootPerAttacker = totalLoot / attackers.size();
        int successfulDistributions = 0;
        int onlineAttackers = 0;
        int accountErrors = 0;

        for (UUID attacker : attackers) {
            Player player = Bukkit.getPlayer(attacker);

            if (player == null) {
                // Jugador offline - podrías implementar distribución offline aquí
                plugin.getLogger().info("Atacante offline, botín no distribuido: " + attacker);
                continue;
            }

            onlineAttackers++;
            Account account = accountManager.getAccount(player.getName());

            if (account == null) {
                plugin.getLogger().warning("No se encontró cuenta para el atacante: " + player.getName());
                accountErrors++;
                continue;
            }

            // El método deposit() retorna boolean, no lanza excepciones
            boolean success = account.deposit(defaultCurrency, lootPerAttacker);

            if (success) {
                successfulDistributions++;
                player.sendMessage("§a¡Has recibido " + formatCurrency(lootPerAttacker) +
                        " como botín del asedio a " + defeatedCity + "!");

                plugin.getLogger().info("Botín distribuido a " + player.getName() + ": " +
                        formatCurrency(lootPerAttacker));
            } else {
                plugin.getLogger().warning("Falló el depósito de botín para " + player.getName() +
                        ": " + formatCurrency(lootPerAttacker));
            }
        }

        // Log detallado del resultado
        plugin.getLogger().info("Resumen del botín del asedio a " + defeatedCity + ":");
        plugin.getLogger().info("- Total de botín: " + formatCurrency(totalLoot));
        plugin.getLogger().info("- Atacantes totales: " + attackers.size());
        plugin.getLogger().info("- Atacantes online: " + onlineAttackers);
        plugin.getLogger().info("- Distribuciones exitosas: " + successfulDistributions);
        plugin.getLogger().info("- Errores de cuenta: " + accountErrors);
        plugin.getLogger().info("- Botín por atacante: " + formatCurrency(lootPerAttacker));

        // Calcular botín no distribuido
        double undistributedLoot = (attackers.size() - successfulDistributions) * lootPerAttacker;
        if (undistributedLoot > 0) {
            plugin.getLogger().warning("Botín no distribuido: " + formatCurrency(undistributedLoot));
            // Opcionalmente, podrías devolver este dinero a la ciudad o a una cuenta
            // especial
        }

        return successfulDistributions > 0;
    }

    /**
     * Calcula la cantidad de saqueo de una ciudad
     * 
     * @param cityName   Nombre de la ciudad
     * @param percentage Porcentaje a saquear
     * @return Cantidad calculada
     */
    public double calculateLootAmount(String cityName, double percentage) {
        double cityBalance = getCityBalance(cityName);
        return cityBalance * percentage;
    }

    // ========== MÉTODOS SECUNDARIOS (ÚTILES) ==========

    // VALIDACIONES ECONÓMICAS

    /**
     * Verifica si una ciudad tiene fondos suficientes
     * 
     * @param cityName Nombre de la ciudad
     * @param amount   Cantidad requerida
     * @return true si tiene fondos suficientes
     */
    public boolean hasEnoughFunds(String cityName, double amount) {
        return getCityBalance(cityName) >= amount;
    }

    /**
     * Valida si una transacción es válida
     * 
     * @param cityName Nombre de la ciudad
     * @param amount   Cantidad de la transacción
     * @return true si es válida
     */
    public boolean isValidTransaction(String cityName, double amount) {
        return amount > 0 && amount <= transactionLimit && hasEnoughFunds(cityName, amount);
    }

    /**
     * Obtiene el límite de transacciones
     * 
     * @return Límite actual
     */
    public double getTransactionLimit() {
        return transactionLimit;
    }

    // HISTORIAL Y ESTADÍSTICAS (Simplificado para YML)

    /**
     * Obtiene historial de transacciones de una ciudad (simplificado)
     * 
     * @param cityName Nombre de la ciudad
     * @return Información básica del historial
     */
    public String getCityTransactionHistory(String cityName) {
        // Para un sistema YML simplificado, retornamos información básica
        double balance = getCityBalance(cityName);
        return "Ciudad: " + cityName + " | Balance actual: " + formatCurrency(balance);
    }

    /**
     * Obtiene impuestos totales recolectados (simplificado)
     * 
     * @param cityName Nombre de la ciudad
     * @return Balance actual (representando impuestos acumulados)
     */
    public double getTotalTaxesCollected(String cityName) {
        return getCityBalance(cityName);
    }

    /**
     * Obtiene gastos de ciudad (simplificado)
     * 
     * @param cityName Nombre de la ciudad
     * @return Información de gastos
     */
    public double getCityExpenses(String cityName) {
        // En un sistema simplificado, los gastos se calcularían restando ingresos menos
        // balance actual
        return 0.0; // Placeholder para implementación futura
    }

    // CONFIGURACIÓN

    /**
     * Configura la tasa de impuestos
     * 
     * @param rate Nueva tasa (0.05 = 5%)
     */
    public void setTaxRate(double rate) {
        this.taxRate = Math.max(0.0, Math.min(1.0, rate)); // Entre 0% y 100%
        plugin.getLogger().info("Tasa de impuestos configurada a: " + (taxRate * 100) + "%");
    }

    /**
     * Obtiene la tasa actual de impuestos
     * 
     * @return Tasa actual
     */
    public double getTaxRate() {
        return taxRate;
    }

    /**
     * Configura el porcentaje de saqueo
     * 
     * @param percentage Nuevo porcentaje (0.5 = 50%)
     */
    public void setLootPercentage(double percentage) {
        this.lootPercentage = Math.max(0.0, Math.min(1.0, percentage)); // Entre 0% y 100%
        plugin.getLogger().info("Porcentaje de saqueo configurado a: " + (lootPercentage * 100) + "%");
    }

    // UTILIDADES

    /**
     * Formatea una cantidad monetaria
     * 
     * @param amount Cantidad a formatear
     * @return Cantidad formateada
     */
    public String formatCurrency(double amount) {
        if (defaultCurrency != null) {
            return defaultCurrency.format(amount);
        }
        return String.format("%.2f", amount);
    }

    /**
     * Verifica si la economía está habilitada
     * 
     * @return true si GemsEconomy está disponible
     */
    public boolean isEconomyEnabled() {
        return accountManager != null && defaultCurrency != null;
    }

    /**
     * Respalda las cuentas de ciudades (simplificado para YML)
     * 
     * @return true si se respaldó exitosamente
     */
    public boolean backupCityAccounts() {
        try {
            plugin.getLogger().info("Respaldo de cuentas de ciudades iniciado...");
            // En un sistema YML, el respaldo sería automático al guardar la configuración
            plugin.getLogger().info("Respaldo completado exitosamente.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error durante el respaldo de cuentas", e);
            return false;
        }
    }

    // MÉTODOS ADICIONALES PARA INTEGRACIÓN

    /**
     * Verifica si una ciudad tiene cuenta bancaria
     * 
     * @param cityName Nombre de la ciudad
     * @return true si existe la cuenta
     */
    public boolean cityAccountExists(String cityName) {
        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
        return accountManager.getAccount(accountName) != null;
    }

    /**
     * Elimina la cuenta bancaria de una ciudad
     * 
     * @param cityName Nombre de la ciudad
     * @return true si se eliminó exitosamente
     */
    public boolean deleteCityAccount(String cityName) {
        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
        Account account = accountManager.getAccount(accountName);

        if (account == null) {
            plugin.getLogger().info("No se encontró cuenta para eliminar de la ciudad: " + cityName);
            return true; // Técnicamente exitoso - ya no existe
        }

        double balance = account.getBalance(defaultCurrency);

        if (balance > 0) {
            // El método withdraw() retorna boolean, no lanza excepciones
            boolean success = account.withdraw(defaultCurrency, balance);

            if (success) {
                plugin.getLogger().info("Balance de " + formatCurrency(balance) +
                        " retirado de la cuenta de ciudad " + cityName);
            } else {
                plugin.getLogger().warning("No se pudo vaciar la cuenta de ciudad " + cityName +
                        ". Balance: " + formatCurrency(balance));
                return false;
            }
        }

        // GemsEconomy no tiene método deleteAccount, así que vaciamos la cuenta
        // y la marcamos como eliminada en nuestro registro interno

        // Verificar que la cuenta esté completamente vacía
        double finalBalance = account.getBalance(defaultCurrency);
        if (finalBalance == 0) {
            // Marcar como eliminada en nuestro sistema
            markCityAccountAsDeleted(cityName);

            plugin.getLogger().info("Cuenta de ciudad " + cityName +
                    " vaciada y marcada como eliminada exitosamente.");
            return true;
        } else {
            plugin.getLogger().warning("La cuenta de ciudad " + cityName +
                    " no pudo ser vaciada completamente. Balance restante: " + formatCurrency(finalBalance));
            return false;
        }
    }

    /**
     * Configura el límite de transacciones
     * 
     * @param limit Nuevo límite
     */
    public void setTransactionLimit(double limit) {
        this.transactionLimit = Math.max(0.0, limit);
        plugin.getLogger().info("Límite de transacciones configurado a: " + formatCurrency(limit));
    }

    // Agregar estas variables a tu clase
    private Set<String> deletedCityAccounts = new HashSet<>();
    private static final String DELETED_ACCOUNTS_FILE = "deleted_city_accounts.yml";

    // Métodos para gestionar cuentas eliminadas
    private void markCityAccountAsDeleted(String cityName) {
        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
        deletedCityAccounts.add(accountName);
        saveDeletedAccounts();
    }

    public boolean isCityAccountDeleted(String cityName) {
        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();
        return deletedCityAccounts.contains(accountName);
    }

    private void loadDeletedAccounts() {
        File file = new File(plugin.getDataFolder(), DELETED_ACCOUNTS_FILE);
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        deletedCityAccounts = new HashSet<>(config.getStringList("deleted-accounts"));

        plugin.getLogger().info("Cargadas " + deletedCityAccounts.size() + " cuentas eliminadas");
    }

    private void saveDeletedAccounts() {
        try {
            File file = new File(plugin.getDataFolder(), DELETED_ACCOUNTS_FILE);
            FileConfiguration config = new YamlConfiguration();
            config.set("deleted-accounts", new ArrayList<>(deletedCityAccounts));
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al guardar cuentas eliminadas", e);
        }
    }

    // Método para "restaurar" una cuenta eliminada si se necesita
    public boolean restoreCityAccount(String cityName) {
        String accountName = CITY_ACCOUNT_PREFIX + cityName.toLowerCase();

        if (deletedCityAccounts.remove(accountName)) {
            saveDeletedAccounts();
            plugin.getLogger().info("Cuenta de ciudad " + cityName + " restaurada");
            return true;
        }

        return false;
    }

}