package com.mineglicht.listener;

import com.mineglicht.cityWars;
import com.mineglicht.config.Messages;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import com.mineglicht.util.MessageUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Listener para eventos relacionados con jugadores
 */
public class PlayerListener implements Listener {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;

    public PlayerListener(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Manejador para cuando un jugador entra al servidor
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Cargar datos del ciudadano si existe
        if (!citizenManager.loadOrCreateCitizen(playerId)) {
            // Sí es nuevo, crear un nuevo ciudadano
            citizenManager.createNewCitizen(player.getUniqueId(), player.getName());
        }

        // Verificar si el jugador estaba en proceso de desconexión durante un asedio
        if (citizenManager.isPlayerDisconnecting(playerId)) {
            // Cancelar el proceso de desconexión
            citizenManager.cancelDisconnection(playerId);

            // Tepear al jugador a su última ubicación conocida
            Location lastLocation = citizenManager.getPlayerLastLocation(playerId);
            if (lastLocation != null) {
                player.teleport(lastLocation);
            }

            // Envia mensaje de bienvenida
            player.sendMessage("§a¡Te has reconectado durante el asedio! Tu proceso de desconexión ha sido cancelado.");
        }

        // Verificar si el jugador pertenece a una ciudad
        Citizen citizen = citizenManager.getCitizen(playerId);
        if (citizen != null && citizen.getCityId() != null) {
            City city = cityManager.getCity(citizen.getCityId());

            if (city != null) {
                // Actualizar contador de ciudadanos online
                citizenManager.incrementCityOnlineCount(city.getId());

                // Determinar el mensaje apropiado basado en sí se reconectó durante el asedio
                String messageKey;
                if (citizenManager.isPlayerDisconnecting(playerId)) {
                    // Si estaba desconectándose durante un asedio
                    messageKey = "siege.member-reconnected-siege";
                } else {
                    // Conexión normal
                    messageKey = "siege.member-joined";
                }

                // Notificar a miembros de la ciudad que un jugador ha entrado
                MessageUtils.sendToCityMembers(citizenManager, city.getId(),
                        MessageUtils.formatMessage(messageKey,
                                "%player%", player.getName()));
            }
        }
    }

    /**
     * Manejador para cuando un jugador sale del servidor
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Verificar si el jugador pertenece a una ciudad
        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
        if (citizen != null && citizen.getCityId() != null) {
            City city = cityManager.getCity(citizen.getCityId());
            if (city != null) {
                // Decrementar contador de ciudadanos online
                citizenManager.decrementCityOnlineCount(city.getId());

                // Notificar a miembros de la ciudad que un jugador ha salido
                MessageUtils.sendToCityMembers(citizenManager, city.getId(),
                        MessageUtils.formatMessage("city.member-left",
                                "%player%", player.getName()));

                // Verificar si la ciudad está bajo asedio y si hay suficientes jugadores conectados
                citizenManager.checkCitySiegeViability(city.getId(), player);
            }
        }

        // Guardar datos del ciudadano
        citizenManager.saveCitizens(player.getUniqueId());
    }

    /**
     * Manejador para cuando un jugador se mueve
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Solo procesar si hubo un cambio de bloque (optimización)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getTo();

        // Verificar si el jugador está entrando a una ciudad
        City fromCity = cityManager.getCityAtLocation(event.getFrom());
        City toCity = cityManager.getCityAtLocation(location);

        // Si entró a una nueva ciudad
        if (fromCity != toCity && toCity != null) {
            player.sendTitle(
                    MessageUtils.formatMessage(Messages.CITY_ENTER_TITLE, "%city%", toCity.getName()),
                    MessageUtils.formatMessage(Messages.CITY_ENTER_SUBTITLE, "%city%", toCity.getName()),
                    10, 70, 20);

            // Verificar si la ciudad está bajo asedio
            if (plugin.getSiegeManager().isCityUnderSiege(toCity.getId())) {
                player.sendMessage(MessageUtils.formatMessage("siege.entering_city_under_siege"));
            }
        }

        // Si salió de una ciudad
        if (fromCity != toCity && fromCity != null) {
            player.sendTitle(
                    MessageUtils.formatMessage(Messages.CITY_EXIT_TITLE, "%city%", fromCity.getName()),
                    MessageUtils.formatMessage(Messages.CITY_EXIT_SUBTITLE, "%city%", fromCity.getName()),
                    10, 70, 20);
        }

        // Si el jugador está en una ciudad, verificar si está intentando colocar un estandarte de asedio
        if (plugin.getSiegeManager().isPlayerCarryingSiegeFlag(player) && toCity != null) {
            Citizen citizen = citizenManager.getCitizen(player.getUniqueId());

            // Si pertenece a otra ciudad e intenta colocar el estandarte
            if (citizen != null && citizen.getCityId() != null &&
                    !citizen.getCityId().equals(toCity.getId())) {

                // Verificar si la ciudad tiene suficientes jugadores para ser asediada
                double onlinePercentage = citizenManager.getOnlineCitizenPercentage(toCity.getId());
                if (onlinePercentage < 0.3) { // Menos del 30% de los ciudadanos están online
                    player.sendMessage(MessageUtils.formatMessage("siege.not_enough_defenders"));
                }
            }
        }
    }

    /**
     * Manejador para cuando un jugador muere
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Si el jugador tenía un estandarte de asedio, cancelar el asedio
        if (plugin.getSiegeManager().isPlayerCarryingSiegeFlag(player)) {
            plugin.getSiegeManager().cancelSiegeByPlayer(player.getUniqueId());

            if (player.getKiller() != null) {
                Player killer = player.getKiller();
                plugin.getServer().broadcastMessage(
                        MessageUtils.formatMessage("siege.carrier_killed",
                                "%player%", player.getName(),
                                "%killer%", killer.getName()));
            }
        }

        // Verificar si el jugador estaba defendiendo una ciudad bajo asedio
        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
        if (citizen != null && citizen.getCityId() != null) {
            City city = cityManager.getCity(citizen.getCityId());
            if (city != null && plugin.getSiegeManager().isCityUnderSiege(city.getId())) {
                // Notificar a los atacantes
                plugin.getSiegeManager().notifyAttackers(city.getId(),
                        MessageUtils.formatMessage("siege.defender_killed",
                                "%player%", player.getName()));
            }
        }
    }

    /**
     * Manejador para cuando un jugador reaparece
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Verificar si el jugador pertenece a una ciudad
        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
        if (citizen != null && citizen.getCityId() != null) {
            City city = cityManager.getCity(citizen.getCityId());
            if (city != null && city.getSpawnLocation(city) != null) {
                // Si la ciudad tiene una ubicación de spawn configurada,
                // y no está bajo asedio, reaparecerá allí
                if (!plugin.getSiegeManager().isCityUnderSiege(city.getId())) {
                    event.setRespawnLocation(city.getSpawnLocation(city));
                }
            }
        }
    }
}