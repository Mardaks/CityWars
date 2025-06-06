package com.mineglicht.task;

import com.mineglicht.cityWars;
import com.mineglicht.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tarea programada para manejar cooldowns entre asedios
 * Controla los períodos de tiempo entre ataques de ciudades
 */
public class SiegeCooldownTask extends BukkitRunnable {
    
    private final cityWars plugin;
    private final SiegeManager siegeManager;
    
    // Mapa para almacenar cooldowns entre ciudades específicas
    // Key: "atacante-defensor", Value: tiempo restante en segundos
    private final Map<String, Integer> cityCooldowns = new ConcurrentHashMap<>();
    
    // Mapa para cooldowns individuales de jugadores
    // Key: UUID del jugador, Value: tiempo restante en segundos
    private final Map<UUID, Integer> playerCooldowns = new ConcurrentHashMap<>();
    
    // Mapa para almacenar información adicional de cooldowns
    private final Map<String, CooldownInfo> cooldownInfo = new ConcurrentHashMap<>();
    
    public SiegeCooldownTask(cityWars plugin) {
        this.plugin = plugin;
        this.siegeManager = plugin.getSiegeManager();
    }
    
    @Override
    public void run() {
        try {
            // Procesar cooldowns entre ciudades
            processCityCooldowns();
            
            // Procesar cooldowns de jugadores
            processPlayerCooldowns();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error en SiegeCooldownTask", e);
        }
    }
    
    /**
     * Procesa y actualiza los cooldowns entre ciudades
     */
    private void processCityCooldowns() {
        cityCooldowns.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            int timeRemaining = entry.getValue() - 1;
            
            if (timeRemaining <= 0) {
                // Cooldown terminado
                CooldownInfo info = cooldownInfo.remove(key);
                if (info != null) {
                    notifyCooldownExpired(info);
                }
                return true; // Remover de la lista
            } else {
                // Actualizar tiempo restante
                entry.setValue(timeRemaining);
                
                // Enviar notificaciones en momentos específicos
                if (shouldNotify(timeRemaining)) {
                    notifyCooldownProgress(key, timeRemaining);
                }
                return false; // Mantener en la lista
            }
        });
    }
    
    /**
     * Procesa y actualiza los cooldowns de jugadores individuales
     */
    private void processPlayerCooldowns() {
        playerCooldowns.entrySet().removeIf(entry -> {
            UUID playerUuid = entry.getKey();
            int timeRemaining = entry.getValue() - 1;
            
            if (timeRemaining <= 0) {
                // Cooldown terminado
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    MessageUtils.sendMessage(player, 
                        "&a¡Tu cooldown de asedio ha expirado! Ya puedes participar en nuevos asedios.");
                }
                return true; // Remover de la lista
            } else {
                // Actualizar tiempo restante
                entry.setValue(timeRemaining);
                return false; // Mantener en la lista
            }
        });
    }
    
    /**
     * Determina si se debe notificar sobre el progreso del cooldown
     */
    private boolean shouldNotify(int timeRemaining) {
        // Notificar en horas específicas y en los últimos minutos
        return timeRemaining == 3600 || // 1 hora
               timeRemaining == 1800 || // 30 minutos
               timeRemaining == 900 ||  // 15 minutos
               timeRemaining == 300 ||  // 5 minutos
               timeRemaining == 60;     // 1 minuto
    }
    
    /**
     * Notifica el progreso del cooldown a las ciudades involucradas
     */
    private void notifyCooldownProgress(String cooldownKey, int timeRemaining) {
        CooldownInfo info = cooldownInfo.get(cooldownKey);
        if (info == null) return;
        
        String timeFormatted = formatTime(timeRemaining);
        String message = String.format(
            "&e¡Cooldown de asedio! &fNo se puede atacar &b%s &fpor &c%s &fmás.",
            info.defenderCity, timeFormatted
        );
        
        // Notificar a los miembros de la ciudad atacante
        notifyCityMembers(info.attackerCity, message);
    }
    
    /**
     * Notifica cuando un cooldown ha expirado
     */
    private void notifyCooldownExpired(CooldownInfo info) {
        String message = String.format(
            "&a¡Cooldown expirado! &fYa puedes atacar a &b%s &fnuevamente.",
            info.defenderCity
        );
        
        // Notificar a los líderes de la ciudad atacante
        notifyCityLeaders(info.attackerCity, message);
        
        plugin.getLogger().info(String.format(
            "Cooldown expirado: %s puede atacar a %s nuevamente",
            info.attackerCity, info.defenderCity
        ));
    }
    
    /**
     * Añade un cooldown entre dos ciudades
     */
    public void addCityCooldown(String attackerCity, String defenderCity) {
        int cooldownMinutes = plugin.getConfig().getInt("siege.cooldown-minutes", 120); // 2 horas por defecto
        int cooldownSeconds = cooldownMinutes * 60;
        
        String key = attackerCity + "-" + defenderCity;
        cityCooldowns.put(key, cooldownSeconds);
        
        CooldownInfo info = new CooldownInfo(attackerCity, defenderCity, System.currentTimeMillis());
        cooldownInfo.put(key, info);
        
        plugin.getLogger().info(String.format(
            "Cooldown de asedio añadido: %s no puede atacar a %s por %d minutos",
            attackerCity, defenderCity, cooldownMinutes
        ));
        
        // Notificar a las ciudades involucradas
        String message = String.format(
            "&c¡Cooldown de asedio activado! &fNo se puede atacar a &b%s &fpor &e%s&f.",
            defenderCity, formatTime(cooldownSeconds)
        );
        notifyCityMembers(attackerCity, message);
    }
    
    /**
     * Añade un cooldown individual a un jugador
     */
    public void addPlayerCooldown(UUID playerUuid, int minutes) {
        int cooldownSeconds = minutes * 60;
        playerCooldowns.put(playerUuid, cooldownSeconds);
        
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            MessageUtils.sendMessage(player, 
                String.format("&c¡Cooldown de asedio! &fNo puedes participar en asedios por &e%s&f.", 
                formatTime(cooldownSeconds)));
        }
        
        plugin.getLogger().info(String.format(
            "Cooldown individual añadido al jugador %s por %d minutos",
            player != null ? player.getName() : playerUuid.toString(), minutes
        ));
    }
    
    /**
     * Verifica si hay cooldown activo entre dos ciudades
     */
    public boolean hasCityCooldown(String attackerCity, String defenderCity) {
        String key = attackerCity + "-" + defenderCity;
        return cityCooldowns.containsKey(key);
    }
    
    /**
     * Verifica si un jugador tiene cooldown activo
     */
    public boolean hasPlayerCooldown(UUID playerUuid) {
        return playerCooldowns.containsKey(playerUuid);
    }
    
    /**
     * Obtiene el tiempo restante de cooldown entre ciudades
     */
    public int getCityCooldownTime(String attackerCity, String defenderCity) {
        String key = attackerCity + "-" + defenderCity;
        return cityCooldowns.getOrDefault(key, 0);
    }
    
    /**
     * Obtiene el tiempo restante de cooldown de un jugador
     */
    public int getPlayerCooldownTime(UUID playerUuid) {
        return playerCooldowns.getOrDefault(playerUuid, 0);
    }
    
    /**
     * Remueve un cooldown entre ciudades (para comandos de admin)
     */
    public boolean removeCityCooldown(String attackerCity, String defenderCity) {
        String key = attackerCity + "-" + defenderCity;
        boolean removed = cityCooldowns.remove(key) != null;
        cooldownInfo.remove(key);
        
        if (removed) {
            plugin.getLogger().info(String.format(
                "Cooldown removido por administrador: %s puede atacar a %s",
                attackerCity, defenderCity
            ));
        }
        
        return removed;
    }
    
    /**
     * Remueve el cooldown de un jugador (para comandos de admin)
     */
    public boolean removePlayerCooldown(UUID playerUuid) {
        boolean removed = playerCooldowns.remove(playerUuid) != null;
        
        if (removed) {
            Player player = Bukkit.getPlayer(playerUuid);
            plugin.getLogger().info(String.format(
                "Cooldown removido por administrador del jugador %s",
                player != null ? player.getName() : playerUuid.toString()
            ));
        }
        
        return removed;
    }
    
    /**
     * Formatea el tiempo en segundos a un string legible
     */
    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return String.format("%dh %dm", hours, minutes);
        } else if (seconds >= 60) {
            int minutes = seconds / 60;
            return String.format("%dm", minutes);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Notifica a todos los miembros de una ciudad
     */
    private void notifyCityMembers(String cityName, String message) {
        try {
            siegeManager.getCityMembers(cityName).forEach(player -> {
                if (player != null && player.isOnline()) {
                    MessageUtils.sendMessage(player, message);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al notificar a los miembros de la ciudad " + cityName, e);
        }
    }
    
    /**
     * Notifica a los líderes de una ciudad
     */
    private void notifyCityLeaders(String cityName, String message) {
        try {
            siegeManager.getCityLeaders(cityName).forEach(player -> {
                if (player != null && player.isOnline()) {
                    MessageUtils.sendMessage(player, message);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al notificar a los líderes de la ciudad " + cityName, e);
        }
    }
    
    /**
     * Obtiene todos los cooldowns activos como un mapa legible
     */
    public Map<String, String> getActiveCooldowns() {
        Map<String, String> activeCooldowns = new HashMap<>();
        
        // Añadir cooldowns entre ciudades
        cityCooldowns.forEach((key, timeRemaining) -> {
            CooldownInfo info = cooldownInfo.get(key);
            if (info != null) {
                String description = String.format("%s → %s", info.attackerCity, info.defenderCity);
                activeCooldowns.put(description, formatTime(timeRemaining));
            }
        });
        
        return activeCooldowns;
    }
    
    /**
     * Obtiene información detallada de un cooldown específico
     */
    public String getCooldownInfo(String attackerCity, String defenderCity) {
        String key = attackerCity + "-" + defenderCity;
        Integer timeRemaining = cityCooldowns.get(key);
        CooldownInfo info = cooldownInfo.get(key);
        
        if (timeRemaining != null && info != null) {
            long startTime = info.startTime;
            long currentTime = System.currentTimeMillis();
            long elapsedTime = (currentTime - startTime) / 1000; // En segundos
            
            return String.format(
                "Cooldown entre %s y %s:\n" +
                "- Tiempo restante: %s\n" +
                "- Tiempo transcurrido: %s\n" +
                "- Iniciado: %s",
                attackerCity, defenderCity,
                formatTime(timeRemaining),
                formatTime((int) elapsedTime),
                new java.util.Date(startTime).toString()
            );
        }
        
        return null;
    }
    
    /**
     * Limpia todos los cooldowns (para comandos de admin o reinicio)
     */
    public void clearAllCooldowns() {
        int cityCount = cityCooldowns.size();
        int playerCount = playerCooldowns.size();
        
        cityCooldowns.clear();
        playerCooldowns.clear();
        cooldownInfo.clear();
        
        plugin.getLogger().info(String.format(
            "Todos los cooldowns han sido limpiados: %d de ciudades, %d de jugadores",
            cityCount, playerCount
        ));
    }
    
    /**
     * Inicia la tarea de cooldown
     */
    public void start() {
        // Ejecutar cada segundo (20 ticks)
        this.runTaskTimer(plugin, 0L, 20L);
        
        plugin.getLogger().info("Tarea de cooldown de asedios iniciada.");
    }
    
    /**
     * Detiene la tarea de cooldown
     */
    public void stop() {
        if (!this.isCancelled()) {
            this.cancel();
            plugin.getLogger().info("Tarea de cooldown de asedios detenida.");
        }
    }
    
    /**
     * Clase interna para almacenar información de cooldowns
     */
    private static class CooldownInfo {
        final String attackerCity;
        final String defenderCity;
        final long startTime;
        
        CooldownInfo(String attackerCity, String defenderCity, long startTime) {
            this.attackerCity = attackerCity;
            this.defenderCity = defenderCity;
            this.startTime = startTime;
        }
    }
}
