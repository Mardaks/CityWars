package com.mineglicht.task;

import com.mineglicht.cityWars;
import com.mineglicht.manager.SiegeManager;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
import com.mineglicht.util.FireworkUtils;
import com.mineglicht.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.logging.Level;

/**
 * Tarea programada para manejar temporizadores de asedio
 * Controla la duración del asedio, fuegos artificiales y destrucción automática del estandarte
 */
public class SiegeTimerTask extends BukkitRunnable {
    
    private final cityWars plugin;
    private final SiegeManager siegeManager;
    private final String cityName;
    private final Location siegeFlagLocation;
    private int timeRemaining; // En segundos
    private int fireworkInterval; // En segundos
    private int timeSinceLastFirework;
    private boolean siegeEnded = false;
    
    public SiegeTimerTask(cityWars plugin, String cityName, Location siegeFlagLocation) {
        this.plugin = plugin;
        this.siegeManager = plugin.getSiegeManager();
        this.cityName = cityName;
        this.siegeFlagLocation = siegeFlagLocation;
        
        // Obtener configuraciones del archivo config.yml
        this.timeRemaining = plugin.getConfig().getInt("siege.duration-minutes", 30) * 60; // Convertir a segundos
        this.fireworkInterval = plugin.getConfig().getInt("siege.firework-interval-seconds", 60);
        this.timeSinceLastFirework = 0;
    }
    
    @Override
    public void run() {
        try {
            if (siegeEnded) {
                cancel();
                return;
            }
            
            // Verificar si la ciudad aún existe y está bajo asedio
            City city = plugin.getCityManager().getCityByName(cityName);
            if (city == null || city.getSiegeState() != SiegeState.ACTIVE) {
                endSiege("La ciudad ya no está bajo asedio");
                return;
            }
            
            // Verificar si el estandarte aún existe
            if (!siegeManager.isSiegeFlagAt(siegeFlagLocation)) {
                endSiege("El estandarte de asedio ha sido destruido");
                return;
            }
            
            // Actualizar temporizadores
            timeRemaining--;
            timeSinceLastFirework++;
            
            // Lanzar fuegos artificiales si es necesario
            if (timeSinceLastFirework >= fireworkInterval) {
                launchFireworks();
                timeSinceLastFirework = 0;
            }
            
            // Enviar mensajes de advertencia en momentos específicos
            sendWarningMessages();
            
            // Verificar si el tiempo del asedio ha terminado
            if (timeRemaining <= 0) {
                endSiege("El tiempo del asedio ha expirado");
                return;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Error en SiegeTimerTask para la ciudad " + cityName, e);
        }
    }
    
    /**
     * Lanza fuegos artificiales desde la ubicación del estandarte
     */
    private void launchFireworks() {
        try {
            // Lanzar múltiples fuegos artificiales
            int fireworkCount = plugin.getConfig().getInt("siege.firework-count", 3);
            
            for (int i = 0; i < fireworkCount; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    FireworkUtils.createSiegePeriodicFirework(siegeFlagLocation);
                }, i * 10L); // Espaciar los fuegos artificiales
            }
            
            plugin.getLogger().info(String.format(
                "Fuegos artificiales lanzados en el asedio de %s (%d:%02d restante)",
                cityName, timeRemaining / 60, timeRemaining % 60
            ));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al lanzar fuegos artificiales en el asedio de " + cityName, e);
        }
    }
    
    /**
     * Envía mensajes de advertencia a los ciudadanos
     */
    private void sendWarningMessages() {
        // Mensajes en momentos específicos
        if (timeRemaining == 300) { // 5 minutos restantes
            broadcastToCity("&c¡ATENCIÓN! &eQuedan &c5 minutos &epara que termine el asedio!");
        } else if (timeRemaining == 120) { // 2 minutos restantes
            broadcastToCity("&c¡URGENTE! &eQuedan &c2 minutos &epara que termine el asedio!");
        } else if (timeRemaining == 60) { // 1 minuto restante
            broadcastToCity("&c¡ÚLTIMO MINUTO! &eEl asedio terminará en &c60 segundos&e!");
        } else if (timeRemaining <= 10 && timeRemaining > 0) { // Cuenta regresiva final
            broadcastToCity("&c" + timeRemaining + "...");
        }
        
        // Mensaje principal cada 5 minutos
        if (timeRemaining % 300 == 0 && timeRemaining > 300) {
            int minutesLeft = timeRemaining / 60;
            broadcastToCity(String.format("&e¡Estás bajo ataque! Tiempo restante: &c%d minutos", minutesLeft));
        }
    }
    
    /**
     * Envía un mensaje a todos los ciudadanos de la ciudad
     */
    private void broadcastToCity(String message) {
        List<Player> cityMembers = siegeManager.getCityMembers(cityName);
        
        for (Player player : cityMembers) {
            if (player != null && player.isOnline()) {
                MessageUtils.sendTitle(player, "&c¡BAJO ASEDIO!", message, 10, 40, 10);
                MessageUtils.sendMessage(player, message);
            }
        }
    }
    
    /**
     * Termina el asedio por tiempo expirado
     */
    private void endSiege(String reason) {
        if (siegeEnded) return;
        
        siegeEnded = true;
        
        plugin.getLogger().info(String.format("Terminando asedio de %s: %s", cityName, reason));
        
        // Usar SiegeManager para terminar el asedio correctamente
        boolean success = siegeManager.endSiege(cityName, reason);
        
        if (success) {
            // Notificar a todos los jugadores involucrados
            broadcastToCity("&a¡El asedio ha terminado! &e" + reason);
            
            // Notificar a los atacantes también
            List<Player> attackers = siegeManager.getSiegeAttackers(cityName);
            for (Player attacker : attackers) {
                if (attacker != null && attacker.isOnline()) {
                    MessageUtils.sendMessage(attacker, "&c¡El asedio de &e" + cityName + " &cha terminado! &f" + reason);
                }
            }
        }
        
        // Cancelar esta tarea
        cancel();
    }
    
    /**
     * Obtiene el tiempo restante en segundos
     */
    public int getTimeRemaining() {
        return timeRemaining;
    }
    
    /**
     * Obtiene el tiempo restante formateado como string
     */
    public String getFormattedTimeRemaining() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Termina el asedio manualmente (para comandos de admin)
     */
    public void forceEnd(String reason) {
        endSiege("Terminado por administrador: " + reason);
    }
    
    /**
     * Extiende el tiempo del asedio (para comandos de admin)
     */
    public void extendTime(int additionalMinutes) {
        timeRemaining += (additionalMinutes * 60);
        broadcastToCity(String.format("&e¡El asedio ha sido extendido por &c%d minutos &eadicionales!", additionalMinutes));
        
        plugin.getLogger().info(String.format(
            "Asedio de %s extendido por %d minutos. Nuevo tiempo restante: %s",
            cityName, additionalMinutes, getFormattedTimeRemaining()
        ));
    }
    
    /**
     * Inicia la tarea del temporizador de asedio
     */
    public void start() {
        // Ejecutar cada segundo (20 ticks)
        this.runTaskTimer(plugin, 0L, 20L);
        
        plugin.getLogger().info(String.format(
            "Temporizador de asedio iniciado para %s. Duración: %s",
            cityName, getFormattedTimeRemaining()
        ));
        
        // Mensaje inicial
        broadcastToCity("&c¡BAJO ASEDIO! &eTiempo restante: &c" + getFormattedTimeRemaining());
    }
}
