package com.mineglicht.config;

import com.mineglicht.cityWars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gestor principal de configuración del plugin CityWars
 * Maneja la carga, guardado y recarga de archivos de configuración
 */
public class ConfigManager {
    
    private final cityWars plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private final File messagesFile;
    
    // Configuración por defecto para impuestos
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.18"); // 18%
    private static final int DEFAULT_TAX_DECIMAL_PLACES = 2;
    private static final String DEFAULT_ROUNDING_MODE = "HALF_UP";
    
    public ConfigManager(cityWars plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadConfig();
    }
    
    /**
     * Calcula el impuesto a recaudar basado en el balance del jugador
     * @param playerBalance El balance actual del jugador
     * @return El monto de impuesto a cobrar (18% por defecto)
     */
    public BigDecimal getTaxCollected(BigDecimal playerBalance) {
        // Validación de entrada
        if (playerBalance == null || playerBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Obtener configuración de impuestos
        BigDecimal taxRate = getTaxRate();
        int decimalPlaces = getTaxDecimalPlaces();
        RoundingMode roundingMode = getTaxRoundingMode();
        
        // Calcular impuesto
        BigDecimal taxAmount = playerBalance.multiply(taxRate);
        
        // Aplicar redondeo según configuración
        return taxAmount.setScale(decimalPlaces, roundingMode);
    }
    
    /**
     * Obtiene la tasa de impuesto desde la configuración
     * @return Tasa de impuesto (por defecto 0.18 = 18%)
     */
    public BigDecimal getTaxRate() {
        String taxRateStr = config.getString("economy.tax.rate", "0.18");
        try {
            return new BigDecimal(taxRateStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Valor de tasa de impuesto inválido en config.yml: " + taxRateStr + 
                                     ". Usando valor por defecto: " + DEFAULT_TAX_RATE);
            return DEFAULT_TAX_RATE;
        }
    }
    
    /**
     * Obtiene el número de decimales para el cálculo de impuestos
     * @return Número de decimales (por defecto 2)
     */
    public int getTaxDecimalPlaces() {
        return config.getInt("economy.tax.decimal_places", DEFAULT_TAX_DECIMAL_PLACES);
    }
    
    /**
     * Obtiene el modo de redondeo para impuestos
     * @return Modo de redondeo (por defecto HALF_UP)
     */
    public RoundingMode getTaxRoundingMode() {
        String roundingModeStr = config.getString("economy.tax.rounding_mode", DEFAULT_ROUNDING_MODE);
        try {
            return RoundingMode.valueOf(roundingModeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Modo de redondeo inválido en config.yml: " + roundingModeStr + 
                                     ". Usando valor por defecto: " + DEFAULT_ROUNDING_MODE);
            return RoundingMode.valueOf(DEFAULT_ROUNDING_MODE);
        }
    }
    
    /**
     * Verifica si los impuestos están habilitados
     * @return true si los impuestos están habilitados
     */
    public boolean isTaxEnabled() {
        return config.getBoolean("economy.tax.enabled", true);
    }
    
    /**
     * Obtiene el intervalo de recaudación de impuestos en horas
     * @return Intervalo en horas (por defecto 24)
     */
    public int getTaxCollectionIntervalHours() {
        return config.getInt("economy.tax.collection_interval_hours", 24);
    }
    
    /**
     * Obtiene el balance mínimo requerido para cobrar impuestos
     * @return Balance mínimo (por defecto 0)
     */
    public BigDecimal getMinimumTaxableBalance() {
        String minBalanceStr = config.getString("economy.tax.minimum_balance", "0");
        try {
            return new BigDecimal(minBalanceStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Balance mínimo gravable inválido en config.yml: " + minBalanceStr);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Método auxiliar para validar si un jugador debe pagar impuestos
     * @param playerBalance Balance del jugador
     * @return true si debe pagar impuestos
     */
    public boolean shouldPayTax(BigDecimal playerBalance) {
        if (!isTaxEnabled()) {
            return false;
        }
        
        if (playerBalance == null) {
            return false;
        }
        
        return playerBalance.compareTo(getMinimumTaxableBalance()) > 0;
    }
    
    /**
     * Carga la configuración desde el archivo
     */
    public void loadConfig() {
        // Cargar config.yml principal
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Cargar messages.yml
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Recarga la configuración
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Obtiene la configuración principal (config.yml)
     * @return FileConfiguration del config.yml
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Obtiene la configuración de mensajes (messages.yml)
     * @return FileConfiguration del messages.yml
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Guarda la configuración principal
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Guarda la configuración de mensajes
     */
    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar messages.yml: " + e.getMessage());
        }
    }
}
