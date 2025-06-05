package com.mineglicht;

import com.mineglicht.api.CityWarsAPI;
import com.mineglicht.api.CityWarsAPIImpl;
import com.mineglicht.commands.*;
import com.mineglicht.config.*;
import com.mineglicht.integration.*;
import com.mineglicht.listener.*;
import com.mineglicht.manager.*;
import com.mineglicht.task.*;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * Clase principal del plugin CityWars
 * Gestiona la inicialización, configuración y lifecycle del plugin
 */
public class cityWars extends JavaPlugin {

    // === INSTANCIA SINGLETON ===
    private static cityWars instance;
    private AdminCommand adminCommand;
    private CityCommands cityCommands;
    private CitizenCommands citizenCommands;
    private SiegeCommands siegeCommands;

    // === MANAGERS ===
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private RegionManager regionManager;
    private CityManager cityManager;
    private CitizenManager citizenManager;
    private SiegeManager siegeManager;
    private TaxManager taxManager;

    // === INTEGRACIONES ===
    private GemsEconomyIntegration gemsEconomyIntegration;
    private ResidenceIntegration residenceIntegration;

    // === API ===
    private CityWarsAPI api;

    // === TAREAS PROGRAMADAS ===
    private BukkitTask taxCollectionTask;
    private BukkitTask siegeTimerTask;
    private BukkitTask siegeCooldownTask;

    // === ESTADO DEL PLUGIN ===
    private boolean fullyLoaded = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Establecer instancia singleton
        instance = this;

        try {
            // 1. Inicializar configuración
            if (!initializeConfiguration()) {
                getLogger().severe("Error al inicializar la configuración. Deshabilitando plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 2. Inicializar integraciones
            if (!initializeIntegrations()) {
                getLogger().severe("Error al inicializar integraciones críticas. Deshabilitando plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 3. Inicializar managers
            if (!initializeManagers()) {
                getLogger().severe("Error al inicializar managers. Deshabilitando plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 4. Cargar datos
            loadData();

            // 5. Registrar listeners
            registerListeners();

            // 6. Registrar comandos
            registerCommands();

            // 7. Inicializar API
            initializeAPI();

            // 8. Inicializar tareas programadas
            initializeTasks();

            // Plugin completamente cargado
            fullyLoaded = true;

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("&a╔══════════════════════════════════════╗");
            getLogger().info("&a║           CITYWARS PLUGIN            ║");
            getLogger().info("&a║                                      ║");
            getLogger().info("&a║  ✓ Configuración cargada             ║");
            getLogger().info("&a║  ✓ Integraciones inicializadas       ║");
            getLogger().info("&a║  ✓ Managers inicializados            ║");
            getLogger().info("&a║  ✓ Listeners registrados             ║");
            getLogger().info("&a║  ✓ Comandos registrados              ║");
            getLogger().info("&a║  ✓ API disponible                    ║");
            getLogger().info("&a║  ✓ Tareas programadas iniciadas      ║");
            getLogger().info("&a║                                      ║");
            getLogger().info("&a║  Tiempo de carga: " + loadTime + "ms"
                    + " ".repeat(Math.max(0, 16 - String.valueOf(loadTime).length())) + "║");
            getLogger().info("&a╚══════════════════════════════════════╝");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error crítico durante la inicialización del plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (fullyLoaded) {
            getLogger().info("&e╔══════════════════════════════════════╗");
            getLogger().info("&e║        DESHABILITANDO CITYWARS       ║");
            getLogger().info("&e╚══════════════════════════════════════╝");

            // Detener tareas programadas
            stopTasks();

            // Guardar datos
            saveData();

            // Limpiar recursos
            cleanup();

            getLogger().info("&c║  ✓ Plugin deshabilitado correctamente   ║");
        }

        // Limpiar instancia singleton
        instance = null;
    }

    /**
     * Inicializa la configuración del plugin
     */
    private boolean initializeConfiguration() {
        try {
            getLogger().info("&6Inicializando configuración...");

            // Crear carpeta de configuración si no existe
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // Inicializar ConfigManager
            configManager = new ConfigManager(this);

            // Cargar configuraciones
            configManager.loadConfig();

            // Inicializar Messages
            Messages.initialize(configManager.getMessagesConfig());

            // Inicializar Settings
            Settings.initialize(configManager.getConfig());

            getLogger().info("&a✓ Configuración inicializada correctamente");
            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al inicializar configuración", e);
            return false;
        }
    }

    /**
     * Inicializa las integraciones con otros plugins
     */
    private boolean initializeIntegrations() {
        try {
            getLogger().info("&6Inicializando integraciones...");

            PluginManager pm = getServer().getPluginManager();

            // GemsEconomy (CRÍTICO)
            if (pm.getPlugin("GemsEconomy") != null) {
                gemsEconomyIntegration = new GemsEconomyIntegration(this);
                if (gemsEconomyIntegration.isEnabled()) {
                    getLogger().info("&a✓ GemsEconomy integrado correctamente");
                } else {
                    getLogger().severe("&c✗ Error al integrar GemsEconomy - Plugin crítico");
                    return false;
                }
            } else {
                getLogger().severe("&c✗ GemsEconomy no encontrado - Plugin requerido");
                return false;
            }

            // Residence (OPCIONAL)
            if (pm.getPlugin("Residence") != null) {
                residenceIntegration = new ResidenceIntegration(this);
                if (residenceIntegration.isEnabled()) {
                    getLogger().info("&a✓ Residence integrado correctamente");
                } else {
                    getLogger().warning("&e⚠ Error al integrar Residence - Continuando sin integración");
                }
            } else {
                getLogger().info("&e⚠ Residence no encontrado - Funcionalidad limitada");
            }

            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al inicializar integraciones", e);
            return false;
        }
    }

    /**
     * Inicializa todos los managers del plugin
     */
    private boolean initializeManagers() {
        try {
            getLogger().info("&6Inicializando managers...");

            // Orden de inicialización importante
            economyManager = new EconomyManager(this);
            regionManager = new RegionManager(this);
            cityManager = new CityManager(this, economyManager, regionManager);
            citizenManager = new CitizenManager(this, cityManager);
            siegeManager = new SiegeManager(this, cityManager, economyManager, regionManager, citizenManager);
            taxManager = new TaxManager(this, cityManager, citizenManager, economyManager);

            getLogger().info("&a✓ Managers inicializados correctamente");
            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al inicializar managers", e);
            return false;
        }
    }

    /**
     * Carga todos los datos desde la configuración
     */
    private void loadData() {
        try {
            getLogger().info("&6Cargando datos...");

            // Cargar en orden de dependencias
            regionManager.loadRegions();
            cityManager.loadCities();
            citizenManager.loadCitizens();
            economyManager.loadEconomyData();
            siegeManager.loadCooldowns();

            getLogger().info("&a✓ Datos cargados correctamente");

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error al cargar algunos datos", e);
        }
    }

    /**
     * Registra todos los event listeners
     */
    private void registerListeners() {
        try {
            getLogger().info("&6Registrando listeners...");

            PluginManager pm = getServer().getPluginManager();

            // Descomentar cuando tengas los listeners implementados
            pm.registerEvents(new PlayerListener(this), this);
            // pm.registerEvents(new CityListener(this), this);
            // pm.registerEvents(new SiegeListener(this), this);
            // pm.registerEvents(new BlockListener(this), this);
            // pm.registerEvents(new ProtectionListener(this), this);

            getLogger().info("&a✓ Listeners registrados correctamente (actualmente comentados)");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al registrar listeners", e);
        }
    }

    /**
     * Registra todos los comandos del plugin
     */
    private void registerCommands() {
        try {
            getLogger().info("&6Registrando comandos...");

            // Inicializar AdminCommand con validaciones
            this.adminCommand = AdminCommand.initialize(this);
            if (this.adminCommand == null) {
                throw new RuntimeException("No se pudo inicializar AdminCommand");
            }

            // Inicializar CitizenCommands con validaciones
            this.citizenCommands = CitizenCommands.initialize(this);
            if (this.citizenCommands == null) {
                throw new RuntimeException("No se pudo inicializar CitizenCommands");
            }

            // Inicializar otros comandos (puedes aplicar el mismo patrón después)
            this.cityCommands = new CityCommands(this);
            this.siegeCommands = new SiegeCommands(this);

            // Registrar comandos principales
            this.getCommand("cityadmin").setExecutor(this.adminCommand);
            this.getCommand("city").setExecutor(this.cityCommands);
            this.getCommand("citizen").setExecutor(this.citizenCommands);
            this.getCommand("siege").setExecutor(this.siegeCommands);

            // Establecer tab completers
            this.getCommand("cityadmin").setTabCompleter(this.adminCommand);
            this.getCommand("city").setTabCompleter(this.cityCommands);
            this.getCommand("citizen").setTabCompleter(this.citizenCommands);
            this.getCommand("siege").setTabCompleter(this.siegeCommands);

            getLogger().info("&a✓ Comandos registrados correctamente");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al registrar comandos", e);
            // Deshabilitar el plugin si falla el registro de comandos
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Inicializa la API del plugin
     */
    private void initializeAPI() {
        try {
            getLogger().info("&6Inicializando API...");

            api = new CityWarsAPIImpl();

            getLogger().info("&a✓ API inicializada correctamente");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al inicializar API", e);
        }
    }

    /**
     * Inicializa las tareas programadas
     */
    private void initializeTasks() {
        try {
            getLogger().info("&6Inicializando tareas programadas...");

            // Tarea de recolección de impuestos (cada 24 horas)
            long taxInterval = Settings.TAX_COLLECTION_INTERVAL * 20L; // Convertir a ticks
            taxCollectionTask = new TaxCollectionTask(this).runTaskTimerAsynchronously(this, taxInterval, taxInterval);

            getLogger().info("&a✓ Tareas programadas iniciadas correctamente");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al inicializar tareas programadas", e);
        }
    }

    /**
     * Detiene todas las tareas programadas
     */
    private void stopTasks() {
        try {
            getLogger().info("&6Deteniendo tareas programadas...");

            if (taxCollectionTask != null && !taxCollectionTask.isCancelled()) {
                taxCollectionTask.cancel();
            }

            if (siegeTimerTask != null && !siegeTimerTask.isCancelled()) {
                siegeTimerTask.cancel();
            }

            if (siegeCooldownTask != null && !siegeCooldownTask.isCancelled()) {
                siegeCooldownTask.cancel();
            }

            getLogger().info("&a✓ Tareas programadas detenidas");

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error al detener tareas programadas", e);
        }
    }

    /**
     * Guarda todos los datos
     */
    private void saveData() {
        try {
            getLogger().info("&6Guardando datos...");

            cityManager.saveCities();
            citizenManager.saveCitizens();
            regionManager.saveRegions();
            economyManager.saveEconomyData();
            siegeManager.saveCooldowns();

            getLogger().info("&a✓ Datos guardados correctamente");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al guardar datos", e);
        }
    }

    /**
     * Limpia recursos y referencias
     */
    private void cleanup() {
        try {
            getLogger().info("&6Limpiando recursos...");

            // Limpiar referencias
            if (api != null) {
                api = null;
            }

            getLogger().info("&a✓ Recursos limpiados");

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error al limpiar recursos", e);
        }
    }

    /**
     * Recarga la configuración del plugin
     */
    public boolean reloadPlugin() {
        try {
            getLogger().info("&6Recargando plugin...");

            // Guardar datos actuales
            saveData();

            // Recargar configuración
            configManager.reloadConfig();
            Messages.initialize(configManager.getMessagesConfig());
            Settings.initialize(configManager.getConfig());

            // Recargar datos
            loadData();

            getLogger().info("&a✓ Plugin recargado correctamente");
            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al recargar plugin", e);
            return false;
        }
    }

    // === GETTERS PÚBLICOS ===

    public static cityWars getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public CityManager getCityManager() {
        return cityManager;
    }

    public CitizenManager getCitizenManager() {
        return citizenManager;
    }

    public SiegeManager getSiegeManager() {
        return siegeManager;
    }

    public TaxManager getTaxManager() {
        return taxManager;
    }

    public GemsEconomyIntegration getGemsEconomyIntegration() {
        return gemsEconomyIntegration;
    }

    public ResidenceIntegration getResidenceIntegration() {
        return residenceIntegration;
    }

    public CityWarsAPI getAPI() {
        return api;
    }

    public boolean isFullyLoaded() {
        return fullyLoaded;
    }
}