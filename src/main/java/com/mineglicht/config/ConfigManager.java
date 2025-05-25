package com.mineglicht.config;

import com.mineglicht.cityWars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * Gestor principal de configuración del plugin CityWars
 * Maneja la carga, guardado y recarga de archivos de configuración
 */
public class ConfigManager {

    private final cityWars plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(cityWars plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    /**
     * Carga todas las configuraciones del plugin
     */
    public void loadConfigs() {
        // Cargar config.yml principal
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Cargar messages.yml
        loadMessagesConfig();

        // Inicializar Settings y Messages
        Settings.initialize(config);
        Messages.initialize(messagesConfig);

        plugin.getLogger().info("Configuraciones cargadas correctamente");
    }

    /**
     * Carga el archivo de mensajes
     */
    private void loadMessagesConfig() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Cargar valores por defecto del JAR
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream)
            );
            messagesConfig.setDefaults(defConfig);
        }
    }

    /**
     * Recarga todas las configuraciones
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream)
            );
            messagesConfig.setDefaults(defConfig);
        }

        Settings.initialize(config);
        Messages.initialize(messagesConfig);

        plugin.getLogger().info("Configuraciones recargadas correctamente");
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
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar messages.yml", e);
        }
    }

    /**
     * Guarda todas las configuraciones
     */
    public void saveAllConfigs() {
        saveConfig();
        saveMessagesConfig();
    }

    /**
     * @return Configuración principal del plugin
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * @return Configuración de mensajes
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Crea los archivos de configuración por defecto si no existen
     */
    public void createDefaultConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Crear config.yml por defecto
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultConfig();
        }

        // Crear messages.yml por defecto
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    /**
     * Verifica si las configuraciones son válidas
     * @return true si todas las configuraciones son válidas
     */
    public boolean validateConfigs() {
        try {
            // Validar configuración principal
            if (config == null) {
                plugin.getLogger().severe("config.yml no pudo ser cargado");
                return false;
            }

            // Validar configuración de mensajes
            if (messagesConfig == null) {
                plugin.getLogger().severe("messages.yml no pudo ser cargado");
                return false;
            }

            // Validar configuraciones específicas
            if (!Settings.validateSettings()) {
                plugin.getLogger().severe("Configuraciones en config.yml son inválidas");
                return false;
            }

            if (!Messages.validateMessages()) {
                plugin.getLogger().severe("Configuraciones en messages.yml son inválidas");
                return false;
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validando configuraciones", e);
            return false;
        }
    }
}
