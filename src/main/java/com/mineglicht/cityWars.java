package com.mineglicht;

import com.mineglicht.api.CityWarsAPI;
import com.mineglicht.commands.AdminCommand;
import com.mineglicht.commands.CitizenCommand;
import com.mineglicht.config.ConfigManager;
import com.mineglicht.config.Messages;
import com.mineglicht.config.Settings;
import com.mineglicht.integration.GemsEconomyIntegration;
import com.mineglicht.integration.ResidenceIntegration;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.EconomyManager;
import com.mineglicht.manager.RegionManager;
import com.mineglicht.manager.SiegeManager;
import com.mineglicht.models.Citizen;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
import com.mineglicht.task.SiegeCooldownTask;
import com.mineglicht.task.SiegeTimerTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * CityWars Plugin - Sistema avanzado de ciudades con mecánicas de asedio PvP
 *
 * Características principales:
 * - Sistema de ciudades con protecciones y economía
 * - Modo asedio con PvP y captura de banderas
 * - Integración con GemsEconomy y Residence
 * - Sistema de impuestos automático
 * - API para otros plugins
 *
 * @author Tu Nombre
 * @version 1.0.0
 * @since Java 21, Minecraft 1.21.5
 */
public final class cityWars extends JavaPlugin {
    //SETTINGS
    private Settings settings;

    // Instancia singleton
    private static cityWars instance;

    // Gestores principales
    private ConfigManager configManager;
    private CityManager cityManager;
    private CitizenManager citizenManager;
    private SiegeManager siegeManager;
    private EconomyManager economyManager;
    private RegionManager regionManager;
    private TaxManager taxManager;

    // Integraciones
    private GemsEconomyIntegration gemsEconomyIntegration;
    private ResidenceIntegration residenceIntegration;
    private ProtectorIntegration protectorIntegration;

    // Comandos
    private AdminCommand adminCommands;
    private CitizenCommands citizenCommands;
    private CityCommands cityCommands;
    private SiegeCommands siegeCommands;

    // Tareas programadas (usando tu estructura)
    private TaxCollectionTask taxTask;
    private SiegeCooldownTask cooldownTask;
    private BukkitTask siegeTimerTask;

    // Estado de inicialización
    private boolean isFullyEnabled = false;
    private final Object initializationLock = new Object();

    /**
     * Método de habilitación del plugin
     */
    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("""
                ╔══════════════════════════════════════╗
                ║         CityWars v1.0.0              ║
                ║    Sistema de Ciudades y Asedios     ║
                ║         Java 21 + MC 1.21.5          ║
                ╚══════════════════════════════════════╝
                """);

        try {
            // Inicialización asíncrona para mejor rendimiento
            CompletableFuture.runAsync(this::initializePlugin)
                    .thenRun(() -> {
                        synchronized (initializationLock) {
                            isFullyEnabled = true;
                            getLogger().info("✅ CityWars habilitado completamente!");
                        }
                    })
                    .exceptionally(throwable -> {
                        getLogger().log(Level.SEVERE, "❌ Error durante la inicialización:", throwable);
                        getServer().getPluginManager().disablePlugin(this);
                        return null;
                    });

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "❌ Error crítico durante onEnable:", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Inicialización completa del plugin
     */
    private void initializePlugin() {
        try {
            // 1. Verificar dependencias
            if (!checkDependencies()) {
                throw new IllegalStateException("Dependencias faltantes");
            }

            // 2. Cargar configuraciones
            initializeConfig();

            // 3. Inicializar integraciones
            initializeIntegrations();

            // 4. Inicializar gestores
            initializeManagers();

            // 5. Registrar listeners
            registerListeners();

            // 6. Registrar comandos
            registerCommands();

            // 7. Iniciar tareas programadas
            startScheduledTasks();

            // 8. Inicializar API
            initializeAPI();

            getLogger().info("🚀 Inicialización completada exitosamente");

        } catch (Exception e) {
            throw new RuntimeException("Error durante la inicialización del plugin", e);
        }
    }

    /**
     * Verificar dependencias necesarias
     */
    private boolean checkDependencies() {
        var pluginManager = getServer().getPluginManager();

        // Verificar GemsEconomy
        if (!pluginManager.isPluginEnabled("GemsEconomy")) {
            getLogger().severe("❌ GemsEconomy no encontrado - Es requerido!");
            return false;
        }

        // Verificar Residence (opcional pero recomendado)
        if (!pluginManager.isPluginEnabled("Residence")) {
            getLogger().warning("⚠️ Residence no encontrado - Algunas funciones no estarán disponibles");
        }

        getLogger().info("✅ Dependencias verificadas");
        return true;
    }

    /**
     * Inicializar configuraciones
     */
    private void initializeConfig() {
        configManager = new ConfigManager(this);

        // Crear archivos de configuración por defecto si no existen
        configManager.createDefaultConfigs();

        // Cargar configuraciones
        Settings.initialize(configManager);
        Messages.initialize(configManager);

        getLogger().info("✅ Configuraciones cargadas");
    }

    /**
     * Inicializar integraciones con otros plugins
     */
    private void initializeIntegrations() {
        // GemsEconomy (obligatorio)
        gemsEconomyIntegration = new GemsEconomyIntegration(this);
        if (!gemsEconomyIntegration.initialize()) {
            throw new IllegalStateException("No se pudo inicializar GemsEconomy");
        }

        // Residence (opcional)
        residenceIntegration = new ResidenceIntegration(this);
        residenceIntegration.initialize();

        // Protector Plugin (para mobs protectores)
        protectorIntegration = new ProtectorIntegration(this);
        protectorIntegration.initialize();

        getLogger().info("✅ Integraciones inicializadas");
    }

    /**
     * Inicializar todos los gestores
     */
    private void initializeManagers() {
        // Orden importante: algunos gestores dependen de otros
        economyManager = new EconomyManager(this, gemsEconomyIntegration);
        regionManager = new RegionManager(this);
        cityManager = new CityManager(this, economyManager, regionManager);
        citizenManager = new CitizenManager(this, cityManager);
        siegeManager = new SiegeManager(this, cityManager, regionManager, economyManager);
        taxManager = new TaxManager(this, economyManager, citizenManager);

        getLogger().info("✅ Gestores inicializados");
    }

    /**
     * Registrar todos los event listeners
     */
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new CityListener(this), this);
        pm.registerEvents(new SiegeListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new ProtectionListener(this), this);

        getLogger().info("✅ Event listeners registrados");
    }

    /**
     * Registrar comandos del plugin
     */
    private void registerCommands() {
        adminCommands = new AdminCommand(this);
        citizenCommands = new CitizenCommands(this);
        cityCommands = new CityCommands(this);
        siegeCommands = new SiegeCommands(this);

        // Registrar comandos principales
        var adminCmd = getCommand("cwa");
        if (adminCmd != null) {
            adminCmd.setExecutor(adminCommands);
            adminCmd.setTabCompleter(adminCommands);
        }

        var cityCmd = getCommand("city");
        if (cityCmd != null) {
            cityCmd.setExecutor(cityCommands);
            cityCmd.setTabCompleter(cityCommands);
        }

        var citizenCmd = getCommand("citizen");
        if (citizenCmd != null) {
            citizenCmd.setExecutor(citizenCommands);
            citizenCmd.setTabCompleter(citizenCommands);
        }

        var siegeCmd = getCommand("siege");
        if (siegeCmd != null) {
            siegeCmd.setExecutor(siegeCommands);
            siegeCmd.setTabCompleter(siegeCommands);
        }

        getLogger().info("✅ Comandos registrados");
    }

    /**
     * Iniciar tareas programadas (usando tu estructura)
     */
    private void startScheduledTasks() {
        // Inicializar tareas usando tu código proporcionado
        this.taxTask = new TaxCollectionTask(this);
        this.cooldownTask = new SiegeCooldownTask(this);

        // Iniciar tareas
        taxTask.start();
        cooldownTask.start();

        // Tarea de temporizador de asedio (cada segundo)
        var scheduler = getServer().getScheduler();
        siegeTimerTask = scheduler.runTaskTimer(
                this,
                new SiegeTimerTask(siegeManager),
                20L,
                20L
        );

        getLogger().info("✅ Tareas programadas iniciadas");
    }

    /**
     * Inicializar la API pública
     */
    private void initializeAPI() {
        // La API estará disponible para otros plugins
        getLogger().info("✅ CityWars API inicializada");
    }

    /**
     * Método de deshabilitación del plugin
     */
    @Override
    public void onDisable() {
        getLogger().info("🔄 Deshabilitando CityWars...");

        try {
            // Cancelar tareas programadas
            cancelScheduledTasks();

            // Guardar datos pendientes
            saveAllData();

            // Limpiar recursos
            cleanup();

            getLogger().info("✅ CityWars deshabilitado correctamente");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "❌ Error durante onDisable:", e);
        } finally {
            instance = null;
        }
    }

    /**
     * Cancelar todas las tareas programadas (usando tu estructura)
     */
    private void cancelScheduledTasks() {
        // Detener tareas usando tu código proporcionado
        if (taxTask != null) taxTask.stop();
        if (cooldownTask != null) cooldownTask.stop();

        if (siegeTimerTask != null && !siegeTimerTask.isCancelled()) {
            siegeTimerTask.cancel();
        }

        getLogger().info("✅ Tareas programadas canceladas");
    }

    /**
     * Guardar todos los datos pendientes
     */
    private void saveAllData() {
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    if (cityManager != null) cityManager.saveAllCities();
                }),
                CompletableFuture.runAsync(() -> {
                    if (citizenManager != null) citizenManager.saveAllCitizens();
                }),
                CompletableFuture.runAsync(() -> {
                    if (siegeManager != null) siegeManager.saveAllSieges();
                })
        ).join(); // Esperar a que terminen todas las tareas de guardado

        getLogger().info("✅ Datos guardados");
    }

    /**
     * Limpiar recursos
     */
    private void cleanup() {
        // Limpiar referencias para ayudar al garbage collector
        configManager = null;
        cityManager = null;
        citizenManager = null;
        siegeManager = null;
        economyManager = null;
        regionManager = null;
        taxManager = null;

        getLogger().info("✅ Recursos limpiados");
    }

    /**
     * Manejo de comandos por defecto
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Verificar si el plugin está completamente habilitado
        synchronized (initializationLock) {
            if (!isFullyEnabled) {
                sender.sendMessage("§c❌ CityWars aún se está inicializando. Espera un momento...");
                return true;
            }
        }

        return switch (command.getName().toLowerCase()) {
            case "cwa" -> adminCommands.onCommand(sender, command, label, args);
            case "city" -> cityCommands.onCommand(sender, command, label, args);
            case "citizen" -> citizenCommands.onCommand(sender, command, label, args);
            case "siege" -> siegeCommands.onCommand(sender, command, label, args);
            default -> false;
        };
    }

    /**
     * Tab completion por defecto
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        synchronized (initializationLock) {
            if (!isFullyEnabled) {
                return List.of();
            }
        }

        return switch (command.getName().toLowerCase()) {
            case "cwa" -> adminCommands.onTabComplete(sender, command, alias, args);
            case "city" -> cityCommands.onTabComplete(sender, command, alias, args);
            case "citizen" -> citizenCommands.onTabComplete(sender, command, alias, args);
            case "siege" -> siegeCommands.onTabComplete(sender, command, alias, args);
            default -> List.of();
        };
    }

    // ==================== GETTERS PÚBLICOS ====================

    /**
     * Obtiene la configuración del plugin
     * Como Settings usa variables estáticas inicializadas por ConfigManager,
     * este método facilita el acceso desde otras clases
     * @return La clase Settings (para acceso estático a configuraciones)
     */
    public Class<Settings> getSettings() {
        return Settings.class;
    }

    /**
     * Obtener la instancia del plugin
     */
    public static cityWars getInstance() {
        return instance;
    }

    /**
     * Verificar si el plugin está completamente habilitado
     */
    public boolean isFullyEnabled() {
        synchronized (initializationLock) {
            return isFullyEnabled;
        }
    }

    // Getters para los gestores
    public ConfigManager getConfigManager() { return configManager; }
    public CityManager getCityManager() { return cityManager; }
    public CitizenManager getCitizenManager() { return citizenManager; }
    public SiegeManager getSiegeManager() { return siegeManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public RegionManager getRegionManager() { return regionManager; }
    public TaxManager getTaxManager() { return taxManager; }

    // Getters para las integraciones
    public GemsEconomyIntegration getGemsEconomyIntegration() { return gemsEconomyIntegration; }
    public ResidenceIntegration getResidenceIntegration() { return residenceIntegration; }
    public ProtectorIntegration getProtectorIntegration() { return protectorIntegration; }

    /**
     * Obtener la API pública del plugin
     */
    public CityWarsAPI getAPI() {
        return new CityWarsAPI() {
            @Override
            public boolean createCity(String name, UUID mayorUuid, Location location) {
                return false;
            }

            @Override
            public boolean deleteCity(String cityName) {
                return false;
            }

            @Override
            public City getCity(String cityName) {
                return null;
            }

            @Override
            public List<City> getAllCities() {
                return List.of();
            }

            @Override
            public boolean cityExists(String cityName) {
                return false;
            }

            @Override
            public City getCityAt(Location location) {
                return null;
            }

            @Override
            public boolean expandCity(String cityName, String direction, int blocks) {
                return false;
            }

            @Override
            public double getCityBankBalance(String cityName) {
                return 0;
            }

            @Override
            public boolean addCitizen(UUID playerUuid, String cityName) {
                return false;
            }

            @Override
            public boolean removeCitizen(UUID playerUuid) {
                return false;
            }

            @Override
            public String getPlayerCity(UUID playerUuid) {
                return "";
            }

            @Override
            public Citizen getCitizen(UUID playerUuid) {
                return null;
            }

            @Override
            public List<Citizen> getCitizens(String cityName) {
                return List.of();
            }

            @Override
            public boolean isCitizen(UUID playerUuid) {
                return false;
            }

            @Override
            public boolean isMayor(UUID playerUuid, String cityName) {
                return false;
            }

            @Override
            public boolean isAssistant(UUID playerUuid, String cityName) {
                return false;
            }

            @Override
            public boolean startSiege(String attackerCity, String defenderCity, Location flagLocation, UUID attackerUuid) {
                return false;
            }

            @Override
            public boolean endSiege(String cityName, String reason) {
                return false;
            }

            @Override
            public SiegeState getSiegeState(String cityName) {
                return null;
            }

            @Override
            public boolean isUnderSiege(String cityName) {
                return false;
            }

            @Override
            public boolean isAttacking(String cityName) {
                return false;
            }

            @Override
            public int getSiegeTimeRemaining(String cityName) {
                return 0;
            }

            @Override
            public boolean hasSiegeCooldown(String attackerCity, String defenderCity) {
                return false;
            }

            @Override
            public int getSiegeCooldownTime(String attackerCity, String defenderCity) {
                return 0;
            }

            @Override
            public double getPlayerBalance(UUID playerUuid) {
                return 0;
            }

            @Override
            public boolean depositToCityBank(String cityName, double amount) {
                return false;
            }

            @Override
            public boolean withdrawFromCityBank(String cityName, double amount) {
                return false;
            }

            @Override
            public double collectCityTaxes(String cityName) {
                return 0;
            }

            @Override
            public double forceGlobalTaxCollection() {
                return 0;
            }

            @Override
            public boolean isProtected(Location location) {
                return false;
            }

            @Override
            public boolean canPerformAction(Player player, Location location, String action) {
                return false;
            }

            @Override
            public boolean isPvPActive(Location location) {
                return false;
            }

            @Override
            public int getTotalCities() {
                return 0;
            }

            @Override
            public int getTotalCitizens() {
                return 0;
            }

            @Override
            public int getActiveSieges() {
                return 0;
            }

            @Override
            public Map<String, Object> getServerStats() {
                return Map.of();
            }

            @Override
            public List<City> getCityRankingByWealth(int limit) {
                return List.of();
            }

            @Override
            public List<City> getCityRankingByPopulation(int limit) {
                return List.of();
            }

            @Override
            public void registerEventListener(Object listener) {

            }

            @Override
            public void unregisterEventListener(Object listener) {

            }

            @Override
            public boolean reloadConfig() {
                return false;
            }

            @Override
            public Object getConfigValue(String path) {
                return null;
            }

            @Override
            public boolean setConfigValue(String path, Object value) {
                return false;
            }

            @Override
            public boolean saveConfig() {
                return false;
            }

            @Override
            public String getAPIVersion() {
                return "";
            }

            @Override
            public boolean isAPIAvailable() {
                return false;
            }
        };
    }

    // Getters para acceder a las tareas desde otros managers (tu código)
    public SiegeCooldownTask getCooldownTask() { return cooldownTask; }
    public TaxCollectionTask getTaxTask() { return taxTask; }

    /**
     * Método para reiniciar el plugin (útil para comandos de admin)
     */
    public CompletableFuture<Boolean> restart() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                getLogger().info("🔄 Reiniciando CityWars...");

                // Deshabilitar componentes
                cancelScheduledTasks();
                saveAllData();

                // Volver a habilitar
                synchronized (initializationLock) {
                    isFullyEnabled = false;
                }

                initializePlugin();

                synchronized (initializationLock) {
                    isFullyEnabled = true;
                }

                getLogger().info("✅ CityWars reiniciado exitosamente");
                return true;

            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "❌ Error durante el reinicio:", e);
                return false;
            }
        });
    }
}
