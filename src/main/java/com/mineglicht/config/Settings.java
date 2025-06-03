package com.mineglicht.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

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
     * Inicializa todas las configuraciones desde el archivo config.yml
     * 
     * @param configuration Configuración cargada del archivo
     */
    public static void initialize(FileConfiguration configuration) {
        config = configuration;
        loadAllSettings();
    }

    /**
     * Carga todas las configuraciones desde el archivo
     */
    private static void loadAllSettings() {
        // Configuraciones de ciudad
        CITY_MIN_SIZE = config.getInt("city.min-size", 50);
        CITY_MAX_SIZE = config.getInt("city.max-size", 500);
        MIN_REGION_Y = config.getInt("region.min-y", -64); // Valor por defecto para 1.18+
        MAX_REGION_Y = config.getInt("region.max-y", 320); // Valor por defecto para 1.18+
        CITY_DEFAULT_SIZE = config.getInt("city.default-size", 100);
        CITY_EXPANSION_COST = config.getInt("city.expansion-cost", 1000);
        CITY_AUTO_CLAIM = config.getBoolean("city.auto-claim", true);
        CITY_CREATION_COST = config.getDouble("city.creation-cost", 5000.0);
        CITY_MAX_CITIZENS = config.getInt("city.max-citizens", 50);

        // Configuraciones de impuestos
        TAX_RATE = config.getDouble("tax.rate", 0.18);
        TAX_COLLECTION_HOUR = config.getInt("tax.collection-hour", 12);
        TAX_NOTIFY_CITIZENS = config.getBoolean("tax.notify-citizens", true);
        TAX_MINIMUM_BALANCE = config.getDouble("tax.minimum-balance", 100.0);

        // Configuraciones de asedio
        SIEGE_MIN_DEFENDERS_PERCENTAGE = config.getInt("siege.min-defenders-percentage", 30);
        SIEGE_DURATION_MINUTES = config.getInt("siege.duration-minutes", 30);
        SIEGE_FIREWORK_INTERVAL_SECONDS = config.getInt("siege.firework-interval-seconds", 60);
        SIEGE_COOLDOWN_HOURS = config.getInt("siege.cooldown-hours", 24);
        SIEGE_ECONOMY_TYPE = config.getString("siege.economy-type", "jp");
        SIEGE_COST = config.getDouble("siege.cost", 10000.0);
        SIEGE_ANNOUNCE_GLOBAL = config.getBoolean("siege.announce-global", true);

        // Configuraciones de saqueo
        LOOT_PHASE_DURATION_MINUTES = config.getInt("loot.phase-duration-minutes", 5);
        LOOT_PERCENTAGE = config.getDouble("loot.percentage", 0.5);
        LOOT_BREAK_BLOCKS = config.getBoolean("loot.break-blocks", true);
        LOOT_OPEN_CHESTS = config.getBoolean("loot.open-chests", true);
        LOOT_PROTECTED_BLOCKS = config.getStringList("loot.protected-blocks");

        // Configuraciones del estandarte de asedio
        SIEGE_FLAG_MATERIAL = Material.valueOf(config.getString("siege-flag.material", "RED_BANNER"));
        SIEGE_FLAG_NAME = config.getString("siege-flag.name", "§c§lEstandarte de Asedio");
        SIEGE_FLAG_LORE = config.getStringList("siege-flag.lore");
        SIEGE_FLAG_GLOWING = config.getBoolean("siege-flag.glowing", true);
        SIEGE_FLAG_CUSTOM_MODEL_DATA = config.getInt("siege-flag.custom-model-data", 0);

        // Configuraciones de protección
        PROTECT_SPAWN_MOBS = config.getBoolean("protection.spawn-mobs", true);
        PROTECT_BLOCK_BREAK = config.getBoolean("protection.block-break", true);
        PROTECT_BLOCK_PLACE = config.getBoolean("protection.block-place", true);
        PROTECT_INTERACT = config.getBoolean("protection.interact", true);
        PROTECT_ENDERPEARL = config.getBoolean("protection.enderpearl", true);
        PROTECT_PVP_OUTSIDE_SIEGE = config.getBoolean("protection.pvp-outside-siege", true);

        // Configuraciones de residences
        RESIDENCE_INTEGRATION = config.getBoolean("residence.integration", true);
        RESIDENCE_DISABLE_DURING_SIEGE = config.getBoolean("residence.disable-during-siege", true);
        RESIDENCE_AUTO_CREATE = config.getBoolean("residence.auto-create", false);
        RESIDENCE_DEFAULT_SIZE = config.getInt("residence.default-size", 20);

        // Configuraciones de economía
        GEMS_ECONOMY_INTEGRATION = config.getBoolean("economy.gems-economy-integration", true);
        PRIMARY_ECONOMY = config.getString("economy.primary-economy", "default");
        SEPARATE_CITY_BANKS = config.getBoolean("economy.separate-city-banks", true);
        CITY_STARTING_FUNDS = config.getDouble("economy.city-starting-funds", 10000.0);

        // Configuraciones de eventos
        ENABLE_CUSTOM_EVENTS = config.getBoolean("events.enable-custom-events", true);
        LOG_ALL_EVENTS = config.getBoolean("events.log-all-events", false);
        BROADCAST_IMPORTANT_EVENTS = config.getBoolean("events.broadcast-important-events", true);

        // Configuraciones de debug
        DEBUG_MODE = config.getBoolean("debug.mode", false);
        DEBUG_SIEGE_EVENTS = config.getBoolean("debug.siege-events", false);
        DEBUG_ECONOMY_EVENTS = config.getBoolean("debug.economy-events", false);
        DEBUG_CITY_EVENTS = config.getBoolean("debug.city-events", false);
    }

    /**
     * Valida que todas las configuraciones sean válidas
     * 
     * @return true si las configuraciones son válidas
     */
    public static boolean validateSettings() {
        try {
            // Validar rangos de ciudad
            if (CITY_MIN_SIZE <= 0 || CITY_MAX_SIZE <= 0 || CITY_MIN_SIZE > CITY_MAX_SIZE) {
                return false;
            }

            // Validar tasa de impuestos
            if (TAX_RATE < 0.0 || TAX_RATE > 1.0) {
                return false;
            }

            // Validar porcentaje mínimo de defensores
            if (SIEGE_MIN_DEFENDERS_PERCENTAGE < 0 || SIEGE_MIN_DEFENDERS_PERCENTAGE > 100) {
                return false;
            }

            // Validar duración del asedio
            if (SIEGE_DURATION_MINUTES <= 0) {
                return false;
            }

            // Validar porcentaje de saqueo
            if (LOOT_PERCENTAGE < 0.0 || LOOT_PERCENTAGE > 1.0) {
                return false;
            }

            // Validar material del estandarte
            if (SIEGE_FLAG_MATERIAL == null) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recarga todas las configuraciones
     */
    public static void reload() {
        if (config != null) {
            loadAllSettings();
        }
    }

    /**
     * Obtiene un valor de configuración personalizado
     * 
     * @param path         Ruta de la configuración
     * @param defaultValue Valor por defecto
     * @return Valor de la configuración
     */
    public static <T> T get(String path, T defaultValue) {
        if (config == null)
            return defaultValue;

        Object value = config.get(path, defaultValue);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}
