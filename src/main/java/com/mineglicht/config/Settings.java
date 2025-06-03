package com.mineglicht.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.function.Predicate;

/**
 * Clase que maneja todas las configuraciones generales del plugin
 * Proporciona acceso estático a los valores de configuración
 */
public class Settings {

    private static FileConfiguration config;

    // === CONFIGURACIONES DE CIUDAD ===
    public static int CITY_MIN_SIZE;
    public static int CITY_MAX_SIZE;
    public static int MIN_REGION_Y;
    public static int MAX_REGION_Y;
    public static int CITY_DEFAULT_SIZE;
    public static int CITY_EXPANSION_COST;
    public static boolean CITY_AUTO_CLAIM;
    public static double CITY_CREATION_COST;
    public static int CITY_MAX_CITIZENS;

    // === CONFIGURACIONES DE IMPUESTOS ===
    public static double TAX_RATE;
    public static int TAX_COLLECTION_HOUR;
    public static boolean TAX_NOTIFY_CITIZENS;
    public static double TAX_MINIMUM_BALANCE;

    // === CONFIGURACIONES DE ASEDIO ===
    public static int SIEGE_MIN_DEFENDERS_PERCENTAGE;
    public static int SIEGE_DURATION_MINUTES;
    public static int SIEGE_FIREWORK_INTERVAL_SECONDS;
    public static int SIEGE_COOLDOWN_HOURS;
    public static String SIEGE_ECONOMY_TYPE;
    public static double SIEGE_COST;
    public static boolean SIEGE_ANNOUNCE_GLOBAL;

    // === CONFIGURACIONES DE SAQUEO ===
    public static int LOOT_PHASE_DURATION_MINUTES;
    public static double LOOT_PERCENTAGE;
    public static boolean LOOT_BREAK_BLOCKS;
    public static boolean LOOT_OPEN_CHESTS;
    public static List<String> LOOT_PROTECTED_BLOCKS;

    // === CONFIGURACIONES DEL ESTANDARTE DE ASEDIO ===
    public static Material SIEGE_FLAG_MATERIAL;
    public static String SIEGE_FLAG_NAME;
    public static List<String> SIEGE_FLAG_LORE;
    public static boolean SIEGE_FLAG_GLOWING;
    public static int SIEGE_FLAG_CUSTOM_MODEL_DATA;

    // === CONFIGURACIONES DE PROTECCIÓN ===
    public static boolean PROTECT_SPAWN_MOBS;
    public static boolean PROTECT_BLOCK_BREAK;
    public static boolean PROTECT_BLOCK_PLACE;
    public static boolean PROTECT_INTERACT;
    public static boolean PROTECT_ENDERPEARL;
    public static boolean PROTECT_PVP_OUTSIDE_SIEGE;

    // === CONFIGURACIONES DE RESIDENCES ===
    public static boolean RESIDENCE_INTEGRATION;
    public static boolean RESIDENCE_DISABLE_DURING_SIEGE;
    public static boolean RESIDENCE_AUTO_CREATE;
    public static int RESIDENCE_DEFAULT_SIZE;

    // === CONFIGURACIONES DE ECONOMÍA ===
    public static boolean GEMS_ECONOMY_INTEGRATION;
    public static String PRIMARY_ECONOMY;
    public static boolean SEPARATE_CITY_BANKS;
    public static double CITY_STARTING_FUNDS;

    // === CONFIGURACIONES DE EVENTOS ===
    public static boolean ENABLE_CUSTOM_EVENTS;
    public static boolean LOG_ALL_EVENTS;
    public static boolean BROADCAST_IMPORTANT_EVENTS;

    // === CONFIGURACIONES DE DEBUG ===
    public static boolean DEBUG_MODE;
    public static boolean DEBUG_SIEGE_EVENTS;
    public static boolean DEBUG_ECONOMY_EVENTS;
    public static boolean DEBUG_CITY_EVENTS;

    /**
     * Configuración de parámetros con valores por defecto y validaciones
     */
    private static final ConfigParam[] CONFIG_PARAMS = {
        // Ciudad
        new ConfigParam("city.min-size", () -> CITY_MIN_SIZE, v -> CITY_MIN_SIZE = v, 50, v -> v > 0),
        new ConfigParam("city.max-size", () -> CITY_MAX_SIZE, v -> CITY_MAX_SIZE = v, 500, v -> v > 0),
        new ConfigParam("region.min-y", () -> MIN_REGION_Y, v -> MIN_REGION_Y = v, -64),
        new ConfigParam("region.max-y", () -> MAX_REGION_Y, v -> MAX_REGION_Y = v, 320),
        new ConfigParam("city.default-size", () -> CITY_DEFAULT_SIZE, v -> CITY_DEFAULT_SIZE = v, 100, v -> v > 0),
        new ConfigParam("city.expansion-cost", () -> CITY_EXPANSION_COST, v -> CITY_EXPANSION_COST = v, 1000, v -> v >= 0),
        new ConfigParam("city.auto-claim", () -> CITY_AUTO_CLAIM, v -> CITY_AUTO_CLAIM = v, true),
        new ConfigParam("city.creation-cost", () -> CITY_CREATION_COST, v -> CITY_CREATION_COST = v, 5000.0, v -> v >= 0),
        new ConfigParam("city.max-citizens", () -> CITY_MAX_CITIZENS, v -> CITY_MAX_CITIZENS = v, 50, v -> v > 0),
        
        // Impuestos
        new ConfigParam("tax.rate", () -> TAX_RATE, v -> TAX_RATE = v, 0.18, v -> v >= 0.0 && v <= 1.0),
        new ConfigParam("tax.collection-hour", () -> TAX_COLLECTION_HOUR, v -> TAX_COLLECTION_HOUR = v, 12, v -> v >= 0 && v <= 23),
        new ConfigParam("tax.notify-citizens", () -> TAX_NOTIFY_CITIZENS, v -> TAX_NOTIFY_CITIZENS = v, true),
        new ConfigParam("tax.minimum-balance", () -> TAX_MINIMUM_BALANCE, v -> TAX_MINIMUM_BALANCE = v, 100.0, v -> v >= 0),
        
        // Asedio
        new ConfigParam("siege.min-defenders-percentage", () -> SIEGE_MIN_DEFENDERS_PERCENTAGE, v -> SIEGE_MIN_DEFENDERS_PERCENTAGE = v, 30, v -> v >= 0 && v <= 100),
        new ConfigParam("siege.duration-minutes", () -> SIEGE_DURATION_MINUTES, v -> SIEGE_DURATION_MINUTES = v, 30, v -> v > 0),
        new ConfigParam("siege.firework-interval-seconds", () -> SIEGE_FIREWORK_INTERVAL_SECONDS, v -> SIEGE_FIREWORK_INTERVAL_SECONDS = v, 60, v -> v > 0),
        new ConfigParam("siege.cooldown-hours", () -> SIEGE_COOLDOWN_HOURS, v -> SIEGE_COOLDOWN_HOURS = v, 24, v -> v >= 0),
        new ConfigParam("siege.economy-type", () -> SIEGE_ECONOMY_TYPE, v -> SIEGE_ECONOMY_TYPE = v, "jp"),
        new ConfigParam("siege.cost", () -> SIEGE_COST, v -> SIEGE_COST = v, 10000.0, v -> v >= 0),
        new ConfigParam("siege.announce-global", () -> SIEGE_ANNOUNCE_GLOBAL, v -> SIEGE_ANNOUNCE_GLOBAL = v, true),
        
        // Saqueo
        new ConfigParam("loot.phase-duration-minutes", () -> LOOT_PHASE_DURATION_MINUTES, v -> LOOT_PHASE_DURATION_MINUTES = v, 5, v -> v > 0),
        new ConfigParam("loot.percentage", () -> LOOT_PERCENTAGE, v -> LOOT_PERCENTAGE = v, 0.5, v -> v >= 0.0 && v <= 1.0),
        new ConfigParam("loot.break-blocks", () -> LOOT_BREAK_BLOCKS, v -> LOOT_BREAK_BLOCKS = v, true),
        new ConfigParam("loot.open-chests", () -> LOOT_OPEN_CHESTS, v -> LOOT_OPEN_CHESTS = v, true),
        
        // Protección
        new ConfigParam("protection.spawn-mobs", () -> PROTECT_SPAWN_MOBS, v -> PROTECT_SPAWN_MOBS = v, true),
        new ConfigParam("protection.block-break", () -> PROTECT_BLOCK_BREAK, v -> PROTECT_BLOCK_BREAK = v, true),
        new ConfigParam("protection.block-place", () -> PROTECT_BLOCK_PLACE, v -> PROTECT_BLOCK_PLACE = v, true),
        new ConfigParam("protection.interact", () -> PROTECT_INTERACT, v -> PROTECT_INTERACT = v, true),
        new ConfigParam("protection.enderpearl", () -> PROTECT_ENDERPEARL, v -> PROTECT_ENDERPEARL = v, true),
        new ConfigParam("protection.pvp-outside-siege", () -> PROTECT_PVP_OUTSIDE_SIEGE, v -> PROTECT_PVP_OUTSIDE_SIEGE = v, true),
        
        // Residences
        new ConfigParam("residence.integration", () -> RESIDENCE_INTEGRATION, v -> RESIDENCE_INTEGRATION = v, true),
        new ConfigParam("residence.disable-during-siege", () -> RESIDENCE_DISABLE_DURING_SIEGE, v -> RESIDENCE_DISABLE_DURING_SIEGE = v, true),
        new ConfigParam("residence.auto-create", () -> RESIDENCE_AUTO_CREATE, v -> RESIDENCE_AUTO_CREATE = v, false),
        new ConfigParam("residence.default-size", () -> RESIDENCE_DEFAULT_SIZE, v -> RESIDENCE_DEFAULT_SIZE = v, 20, v -> v > 0),
        
        // Economía
        new ConfigParam("economy.gems-economy-integration", () -> GEMS_ECONOMY_INTEGRATION, v -> GEMS_ECONOMY_INTEGRATION = v, true),
        new ConfigParam("economy.primary-economy", () -> PRIMARY_ECONOMY, v -> PRIMARY_ECONOMY = v, "default"),
        new ConfigParam("economy.separate-city-banks", () -> SEPARATE_CITY_BANKS, v -> SEPARATE_CITY_BANKS = v, true),
        new ConfigParam("economy.city-starting-funds", () -> CITY_STARTING_FUNDS, v -> CITY_STARTING_FUNDS = v, 10000.0, v -> v >= 0),
        
        // Eventos
        new ConfigParam("events.enable-custom-events", () -> ENABLE_CUSTOM_EVENTS, v -> ENABLE_CUSTOM_EVENTS = v, true),
        new ConfigParam("events.log-all-events", () -> LOG_ALL_EVENTS, v -> LOG_ALL_EVENTS = v, false),
        new ConfigParam("events.broadcast-important-events", () -> BROADCAST_IMPORTANT_EVENTS, v -> BROADCAST_IMPORTANT_EVENTS = v, true),
        
        // Debug
        new ConfigParam("debug.mode", () -> DEBUG_MODE, v -> DEBUG_MODE = v, false),
        new ConfigParam("debug.siege-events", () -> DEBUG_SIEGE_EVENTS, v -> DEBUG_SIEGE_EVENTS = v, false),
        new ConfigParam("debug.economy-events", () -> DEBUG_ECONOMY_EVENTS, v -> DEBUG_ECONOMY_EVENTS = v, false),
        new ConfigParam("debug.city-events", () -> DEBUG_CITY_EVENTS, v -> DEBUG_CITY_EVENTS = v, false)
    };

    public static void initialize(FileConfiguration configuration) {
        config = configuration;
        loadAllSettings();
    }

    private static void loadAllSettings() {
        // Cargar parámetros básicos
        for (ConfigParam param : CONFIG_PARAMS) {
            param.load(config);
        }
        
        // Cargar configuraciones especiales
        loadSpecialConfigurations();
    }

    private static void loadSpecialConfigurations() {
        LOOT_PROTECTED_BLOCKS = config.getStringList("loot.protected-blocks");
        
        // Estandarte de asedio
        try {
            SIEGE_FLAG_MATERIAL = Material.valueOf(config.getString("siege-flag.material", "RED_BANNER"));
        } catch (IllegalArgumentException e) {
            SIEGE_FLAG_MATERIAL = Material.RED_BANNER;
        }
        SIEGE_FLAG_NAME = config.getString("siege-flag.name", "§c§lEstandarte de Asedio");
        SIEGE_FLAG_LORE = config.getStringList("siege-flag.lore");
        SIEGE_FLAG_GLOWING = config.getBoolean("siege-flag.glowing", true);
        SIEGE_FLAG_CUSTOM_MODEL_DATA = config.getInt("siege-flag.custom-model-data", 0);
    }

    public static boolean validateSettings() {
        try {
            // Validar todos los parámetros configurados
            for (ConfigParam param : CONFIG_PARAMS) {
                if (!param.isValid()) {
                    return false;
                }
            }
            
            // Validaciones cruzadas
            return validateCrossReferences();
            
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateCrossReferences() {
        // Validar que min-size <= max-size
        if (CITY_MIN_SIZE > CITY_MAX_SIZE) return false;
        
        // Validar que default-size esté en el rango válido
        if (CITY_DEFAULT_SIZE < CITY_MIN_SIZE || CITY_DEFAULT_SIZE > CITY_MAX_SIZE) return false;
        
        // Validar material del estandarte
        return SIEGE_FLAG_MATERIAL != null;
    }

    public static void reload() {
        if (config != null) {
            loadAllSettings();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String path, T defaultValue) {
        if (config == null) return defaultValue;

        Object value = config.get(path, defaultValue);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Clase interna para manejar parámetros de configuración de forma genérica
     */
    private static class ConfigParam {
        private final String path;
        private final Object defaultValue;
        private final Predicate<Object> validator;
        private final Runnable loader;

        public <T> ConfigParam(String path, java.util.function.Supplier<T> getter, 
                              java.util.function.Consumer<T> setter, T defaultValue) {
            this(path, getter, setter, defaultValue, null);
        }

        @SuppressWarnings("unchecked")
        public <T> ConfigParam(String path, java.util.function.Supplier<T> getter, 
                              java.util.function.Consumer<T> setter, T defaultValue, 
                              Predicate<T> validator) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.validator = validator != null ? (Predicate<Object>) validator : obj -> true;
            this.loader = () -> {
                T value;
                if (defaultValue instanceof Integer) {
                    value = (T) Integer.valueOf(config.getInt(path, (Integer) defaultValue));
                } else if (defaultValue instanceof Double) {
                    value = (T) Double.valueOf(config.getDouble(path, (Double) defaultValue));
                } else if (defaultValue instanceof Boolean) {
                    value = (T) Boolean.valueOf(config.getBoolean(path, (Boolean) defaultValue));
                } else if (defaultValue instanceof String) {
                    value = (T) config.getString(path, (String) defaultValue);
                } else {
                    value = (T) config.get(path, defaultValue);
                }
                setter.accept(value);
            };
        }

        public void load(FileConfiguration config) {
            loader.run();
        }

        public boolean isValid() {
            return validator.test(getCurrentValue());
        }

        private Object getCurrentValue() {
            return config.get(path, defaultValue);
        }
    }
}