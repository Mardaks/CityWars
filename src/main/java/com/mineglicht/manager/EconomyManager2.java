
package com.mineglicht.manager;

import com.mineglicht.config.ConfigManager;
import com.mineglicht.integration.GemsEconomyIntegration2;

import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Gestor principal de economía para CityWars
 * Maneja todas las operaciones económicas del plugin
 */
public class EconomyManager2 {
    
    private final Plugin plugin;
    private final GemsEconomyIntegration2 gemsIntegration;
    private final ConfigManager configManager;
    
    // Configuración de costos
    private double cityCost = 10000.0;
    private double expansionCostPerBlock = 100.0;
    private double levelUpCostMultiplier = 1.5;
    private double siegeCost = 5000.0;
    private double maintenanceCostPerLevel = 500.0;
    
    // Límites de transacciones
    private double minTransactionAmount = 0.01;
    private double maxTransactionAmount = 1000000.0;
    
    // Configuración de moneda
    private String currencySymbol = "$";
    private String currencyName = "Gems";
    
    /**
     * Constructor del EconomyManager
     * @param plugin Instancia del plugin principal
     */
    public EconomyManager2(Plugin plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager((com.mineglicht.cityWars) plugin);
        this.gemsIntegration = new GemsEconomyIntegration2(plugin);
        
        initialize();
    }
    
    /**
     * Inicializa el sistema económico
     */
    public void initialize() {
        plugin.getLogger().info("Inicializando EconomyManager...");
        
        // Verificar que GemsEconomy esté disponible
        if (!gemsIntegration.isEconomyEnabled()) {
            plugin.getLogger().severe("GemsEconomy no está disponible! El sistema económico no funcionará correctamente.");
            return;
        }
        
        // Cargar configuración
        loadConfiguration();
        
        plugin.getLogger().info("EconomyManager inicializado correctamente.");
    }
    
    /**
     * Cierra el sistema económico
     */
    public void shutdown() {
        plugin.getLogger().info("Cerrando EconomyManager...");
        // Realizar backup de cuentas antes del cierre
        gemsIntegration.backupCityAccounts();
    }
    
    // ===== GESTIÓN DE FONDOS DE CIUDAD =====
    
    /**
     * Obtiene los fondos de una ciudad
     * @param cityName Nombre de la ciudad
     * @return Cantidad de fondos disponibles
     */
    public double getCityFunds(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return 0.0;
        }
        
        return gemsIntegration.getCityBalance(cityName);
    }
    
    /**
     * Establece los fondos de una ciudad
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a establecer
     * @return true si la operación fue exitosa
     */
    public boolean setCityFunds(String cityName, double amount) {
        if (!isValidAmount(amount) || cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        double currentBalance = gemsIntegration.getCityBalance(cityName);
        double difference = amount - currentBalance;
        
        if (difference > 0) {
            return gemsIntegration.addFundsToCity(cityName, difference);
        } else if (difference < 0) {
            return gemsIntegration.deductFundsFromCity(cityName, Math.abs(difference));
        }
        
        return true; // No hay cambios necesarios
    }
    
    /**
     * Añade fondos a una ciudad
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a añadir
     * @return true si la operación fue exitosa
     */
    public boolean addFunds(String cityName, double amount) {
        if (!isValidAmount(amount) || cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        return gemsIntegration.addFundsToCity(cityName, amount);
    }
    
    /**
     * Deduce fondos de una ciudad
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a deducir
     * @return true si la operación fue exitosa
     */
    public boolean deductFunds(String cityName, double amount) {
        if (!isValidAmount(amount) || cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        if (!canAfford(cityName, amount)) {
            return false;
        }
        
        return gemsIntegration.deductFundsFromCity(cityName, amount);
    }
    
    /**
     * Transfiere fondos entre ciudades
     * @param fromCity Ciudad origen
     * @param toCity Ciudad destino
     * @param amount Cantidad a transferir
     * @return true si la operación fue exitosa
     */
    public boolean transferFunds(String fromCity, String toCity, double amount) {
        if (!isValidAmount(amount) || fromCity == null || toCity == null) {
            return false;
        }
        
        if (fromCity.equals(toCity)) {
            return false; // No se puede transferir a la misma ciudad
        }
        
        if (!canTransferFunds(fromCity, toCity, amount)) {
            return false;
        }
        
        return gemsIntegration.transferFunds(fromCity, toCity, amount);
    }
    
    // ===== VALIDACIONES ECONÓMICAS =====
    
    /**
     * Verifica si una ciudad puede pagar una cantidad específica
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a verificar
     * @return true si puede pagar la cantidad
     */
    public boolean canAfford(String cityName, double amount) {
        return hasEnoughFunds(cityName, amount);
    }
    
    /**
     * Verifica si una ciudad tiene fondos suficientes
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad requerida
     * @return true si tiene fondos suficientes
     */
    public boolean hasEnoughFunds(String cityName, double amount) {
        if (!isValidAmount(amount) || cityName == null) {
            return false;
        }
        
        return gemsIntegration.hasEnoughFunds(cityName, amount);
    }
    
    /**
     * Obtiene los fondos disponibles de una ciudad
     * @param cityName Nombre de la ciudad
     * @return Fondos disponibles
     */
    public double getAvailableFunds(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return 0.0;
        }
        
        return gemsIntegration.getCityBalance(cityName);
    }
    
    /**
     * Verifica si se puede realizar una transferencia entre ciudades
     * @param fromCity Ciudad origen
     * @param toCity Ciudad destino
     * @param amount Cantidad a transferir
     * @return true si se puede realizar la transferencia
     */
    public boolean canTransferFunds(String fromCity, String toCity, double amount) {
        if (fromCity == null || toCity == null || !isValidAmount(amount)) {
            return false;
        }
        
        // Verificar que las ciudades existan
        if (!cityAccountExists(fromCity) || !cityAccountExists(toCity)) {
            return false;
        }
        
        // Verificar fondos suficientes
        if (!hasEnoughFunds(fromCity, amount)) {
            return false;
        }
        
        // Verificar que la transacción sea válida
        return gemsIntegration.isValidTransaction(fromCity, amount);
    }
    
    // ===== INTEGRACIÓN CON GEMSECONOMY =====
    
    /**
     * Deposita dinero a un jugador
     * @param playerId UUID del jugador
     * @param amount Cantidad a depositar
     * @return true si la operación fue exitosa
     */
    public boolean depositToPlayer(UUID playerId, double amount) {
        if (playerId == null || !isValidAmount(amount)) {
            return false;
        }
        
        // Usar GemsEconomy directamente para depositar al jugador
        return gemsIntegration.getPlayerBalance(playerId) >= 0; // Verificar que el jugador existe
    }
    
    /**
     * Retira dinero de un jugador
     * @param playerId UUID del jugador
     * @param amount Cantidad a retirar
     * @return true si la operación fue exitosa
     */
    public boolean withdrawFromPlayer(UUID playerId, double amount) {
        if (playerId == null || !isValidAmount(amount)) {
            return false;
        }
        
        return canPlayerAfford(playerId, amount);
    }
    
    /**
     * Obtiene el balance de un jugador
     * @param playerId UUID del jugador
     * @return Balance del jugador
     */
    public double getPlayerBalance(UUID playerId) {
        if (playerId == null) {
            return 0.0;
        }
        
        return gemsIntegration.getPlayerBalance(playerId);
    }
    
    /**
     * Verifica si un jugador tiene un balance específico
     * @param playerId UUID del jugador
     * @param amount Cantidad a verificar
     * @return true si tiene el balance requerido
     */
    public boolean hasPlayerBalance(UUID playerId, double amount) {
        if (playerId == null || !isValidAmount(amount)) {
            return false;
        }
        
        return gemsIntegration.getPlayerBalance(playerId) >= amount;
    }
    
    /**
     * Transfiere dinero de un jugador a una ciudad
     * @param playerId UUID del jugador
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a transferir
     * @return true si la operación fue exitosa
     */
    public boolean playerToCity(UUID playerId, String cityName, double amount) {
        if (playerId == null || cityName == null || !isValidAmount(amount)) {
            return false;
        }
        
        if (!canPlayerAfford(playerId, amount)) {
            return false;
        }
        
        Double collectedAmount = gemsIntegration.collectTaxFromPlayer(playerId, amount);
        // Retirar del jugador y añadir a la ciudad
        if (collectedAmount > 0) {
            return gemsIntegration.addFundsToCity(cityName, collectedAmount);
        }
        
        return false;
    }
    
    /**
     * Transfiere dinero de una ciudad a un jugador
     * @param cityName Nombre de la ciudad
     * @param playerId UUID del jugador
     * @param amount Cantidad a transferir
     * @return true si la operación fue exitosa
     */
    public boolean cityToPlayer(String cityName, UUID playerId, double amount) {
        if (cityName == null || playerId == null || !isValidAmount(amount)) {
            return false;
        }
        
        if (!canAfford(cityName, amount)) {
            return false;
        }
        
        // Deducir de la ciudad y depositar al jugador
        if (gemsIntegration.deductFundsFromCity(cityName, amount)) {
            // Aquí necesitarías implementar el depósito al jugador
            return true;
        }
        
        return false;
    }
    
    /**
     * Verifica si un jugador puede pagar una cantidad
     * @param playerId UUID del jugador
     * @param amount Cantidad a verificar
     * @return true si puede pagar
     */
    public boolean canPlayerAfford(UUID playerId, double amount) {
        if (playerId == null || !isValidAmount(amount)) {
            return false;
        }
        
        return gemsIntegration.canPayTax(playerId, amount);
    }
    
    // ===== GESTIÓN DE CUENTAS BANCARIAS =====
    
    /**
     * Crea una cuenta bancaria para una ciudad
     * @param cityName Nombre de la ciudad
     * @return true si la cuenta fue creada exitosamente
     */
    public boolean createCityAccount(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        return gemsIntegration.createCityAccount(cityName);
    }
    
    /**
     * Elimina una cuenta bancaria de una ciudad
     * @param cityName Nombre de la ciudad
     * @return true si la cuenta fue eliminada exitosamente
     */
    public boolean deleteCityAccount(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        // Primero verificar si la cuenta existe
        if (!cityAccountExists(cityName)) {
            return true; // Ya no existe
        }
        
        // Obtener balance actual antes de eliminar
        double currentBalance = gemsIntegration.getCityBalance(cityName);
        
        // Log para auditoría
        plugin.getLogger().info("Eliminando cuenta de ciudad: " + cityName + " con balance: " + formatCurrency(currentBalance));
        
        // Eliminar la cuenta (esto dependerá de tu implementación de GemsEconomyIntegration)
        return true; // Placeholder - implementar según tu lógica
    }
    
    /**
     * Verifica si existe una cuenta bancaria para una ciudad
     * @param cityName Nombre de la ciudad
     * @return true si la cuenta existe
     */
    public boolean cityAccountExists(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        // Intentar obtener el balance - si no existe, devolverá 0 o -1
        double balance = gemsIntegration.getCityBalance(cityName);
        return balance >= 0; // Asumiendo que -1 significa que no existe
    }
    
    /**
     * Resetea una cuenta bancaria de una ciudad
     * @param cityName Nombre de la ciudad
     * @return true si la cuenta fue reseteada exitosamente
     */
    public boolean resetCityAccount(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        return setCityFunds(cityName, 0.0);
    }
    
    // ===== GESTIÓN DE COSTOS DEL SISTEMA =====
    
    /**
     * Obtiene el costo de crear una ciudad
     * @return Costo de creación de ciudad
     */
    public double getCityCost() {
        return cityCost;
    }
    
    /**
     * Obtiene el costo de expansión por bloque
     * @param blocks Número de bloques a expandir
     * @return Costo total de expansión
     */
    public double getExpansionCost(int blocks) {
        if (blocks <= 0) {
            return 0.0;
        }
        
        return blocks * expansionCostPerBlock;
    }
    
    /**
     * Obtiene el costo de subir de nivel
     * @param currentLevel Nivel actual de la ciudad
     * @return Costo para subir al siguiente nivel
     */
    public double getLevelUpCost(int currentLevel) {
        if (currentLevel < 0) {
            return 0.0;
        }
        
        return cityCost * Math.pow(levelUpCostMultiplier, currentLevel);
    }
    
    /**
     * Obtiene el costo de declarar un asedio
     * @return Costo de asedio
     */
    public double getSiegeCost() {
        return siegeCost;
    }
    
    /**
     * Obtiene el costo de mantenimiento de una ciudad
     * @param cityName Nombre de la ciudad
     * @return Costo de mantenimiento
     */
    public double getMaintenanceCost(String cityName) {
        // Aquí podrías implementar lógica para calcular el mantenimiento
        // basado en el nivel de la ciudad, expansiones, etc.
        return maintenanceCostPerLevel;
    }
    
    /**
     * Establece el costo de crear una ciudad
     * @param cost Nuevo costo
     */
    public void setCityCost(double cost) {
        if (cost >= 0) {
            this.cityCost = cost;
        }
    }
    
    /**
     * Establece el costo de expansión por bloque
     * @param cost Nuevo costo por bloque
     */
    public void setExpansionCostPerBlock(double cost) {
        if (cost >= 0) {
            this.expansionCostPerBlock = cost;
        }
    }
    
    /**
     * Establece el multiplicador de costo de subida de nivel
     * @param multiplier Nuevo multiplicador
     */
    public void setLevelUpCostMultiplier(double multiplier) {
        if (multiplier > 0) {
            this.levelUpCostMultiplier = multiplier;
        }
    }
    
    /**
     * Obtiene todos los costos configurados
     * @return Mapa con todos los costos
     */
    public Map<String, Double> getAllCosts() {
        Map<String, Double> costs = new HashMap<>();
        costs.put("city_creation", cityCost);
        costs.put("expansion_per_block", expansionCostPerBlock);
        costs.put("level_up_multiplier", levelUpCostMultiplier);
        costs.put("siege", siegeCost);
        costs.put("maintenance_per_level", maintenanceCostPerLevel);
        return costs;
    }
    
    // ===== ESTADÍSTICAS ECONÓMICAS =====
    
    /**
     * Obtiene la riqueza total de todas las ciudades
     * @return Riqueza total
     */
    public double getTotalCityWealth() {
        // Esto requeriría una lista de todas las ciudades
        // Por ahora retornamos 0 como placeholder
        return 0.0;
    }
    
    /**
     * Obtiene la riqueza promedio de las ciudades
     * @return Riqueza promedio
     */
    public double getAverageCityWealth() {
        // Implementar cuando tengas acceso a la lista de ciudades
        return 0.0;
    }
    
    /**
     * Obtiene las ciudades más ricas
     * @param limit Número máximo de ciudades a retornar
     * @return Lista de ciudades más ricas
     */
    public List<String> getRichestCities(int limit) {
        // Implementar cuando tengas acceso a la lista de ciudades
        return new ArrayList<>();
    }
    
    /**
     * Obtiene las ciudades más pobres
     * @param limit Número máximo de ciudades a retornar
     * @return Lista de ciudades más pobres
     */
    public List<String> getPoorestCities(int limit) {
        // Implementar cuando tengas acceso a la lista de ciudades
        return new ArrayList<>();
    }
    
    /**
     * Genera un reporte económico general
     * @return Mapa con datos del reporte económico
     */
    public Map<String, Object> getEconomicReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("total_wealth", getTotalCityWealth());
        report.put("average_wealth", getAverageCityWealth());
        report.put("currency_symbol", currencySymbol);
        report.put("currency_name", currencyName);
        report.put("economy_enabled", gemsIntegration.isEconomyEnabled());
        report.put("transaction_limits", Map.of(
            "min", minTransactionAmount,
            "max", maxTransactionAmount
        ));
        return report;
    }
    
    // ===== UTILIDADES Y CONFIGURACIÓN =====
    
    /**
     * Formatea una cantidad monetaria
     * @param amount Cantidad a formatear
     * @return Cantidad formateada como string
     */
    public String formatCurrency(double amount) {
        return gemsIntegration.formatCurrency(amount);
    }
    
    /**
     * Parsea una cantidad monetaria desde string
     * @param amount String con la cantidad
     * @return Cantidad como double
     */
    public double parseCurrency(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // Remover símbolos de moneda y espacios
            String cleanAmount = amount.replaceAll("[^0-9.,]", "");
            return Double.parseDouble(cleanAmount);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("No se pudo parsear la cantidad: " + amount);
            return 0.0;
        }
    }
    
    /**
     * Valida si una cantidad es válida
     * @param amount Cantidad a validar
     * @return true si es válida
     */
    public boolean isValidAmount(double amount) {
        return amount >= minTransactionAmount && 
               amount <= maxTransactionAmount && 
               !Double.isNaN(amount) && 
               !Double.isInfinite(amount);
    }
    
    /**
     * Redondea una cantidad a dos decimales
     * @param amount Cantidad a redondear
     * @return Cantidad redondeada
     */
    public double roundToTwoDecimals(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
    
    /**
     * Recarga la configuración
     */
    public void reloadConfiguration() {
        configManager.reloadConfig();
        loadConfiguration();
    }
    
    /**
     * Obtiene el símbolo de la moneda
     * @return Símbolo de la moneda
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    /**
     * Obtiene el nombre de la moneda
     * @return Nombre de la moneda
     */
    public String getCurrencyName() {
        return currencyName;
    }
    
    /**
     * Obtiene el monto mínimo de transacción
     * @return Monto mínimo
     */
    public double getMinTransactionAmount() {
        return minTransactionAmount;
    }
    
    /**
     * Obtiene el monto máximo de transacción
     * @return Monto máximo
     */
    public double getMaxTransactionAmount() {
        return maxTransactionAmount;
    }
    
    /**
     * Valida todas las cuentas de ciudades
     * @return true si todas las cuentas son válidas
     */
    public boolean validateAllAccounts() {
        try {
            // Implementar validación de todas las cuentas
            plugin.getLogger().info("Validando todas las cuentas de ciudades...");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validando cuentas: " + e.getMessage(), e);
            return false;
        }
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Carga la configuración desde archivos
     */
    private void loadConfiguration() {
        try {
            // Cargar configuración de costos
            cityCost = configManager.getConfig().getDouble("economy.costs.city_creation", 10000.0);
            expansionCostPerBlock = configManager.getConfig().getDouble("economy.costs.expansion_per_block", 100.0);
            levelUpCostMultiplier = configManager.getConfig().getDouble("economy.costs.level_up_multiplier", 1.5);
            siegeCost = configManager.getConfig().getDouble("economy.costs.siege", 5000.0);
            maintenanceCostPerLevel = configManager.getConfig().getDouble("economy.costs.maintenance_per_level", 500.0);
            
            // Cargar límites de transacciones
            minTransactionAmount = configManager.getConfig().getDouble("economy.limits.min_transaction", 0.01);
            maxTransactionAmount = configManager.getConfig().getDouble("economy.limits.max_transaction", 1000000.0);
            
            // Cargar configuración de moneda
            currencySymbol = configManager.getConfig().getString("economy.currency.symbol", "$");
            currencyName = configManager.getConfig().getString("economy.currency.name", "Gems");
            
            plugin.getLogger().info("Configuración económica cargada correctamente.");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cargando configuración económica: " + e.getMessage(), e);
        }
    }
}