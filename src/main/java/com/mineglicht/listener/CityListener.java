//package com.mineglicht.listener;
//
//import com.mineglicht.cityWars;
//import com.mineglicht.api.event.CityCreateEvent;
//import com.mineglicht.api.event.CityDeleteEvent;
//import com.mineglicht.models.City;
//import com.mineglicht.models.Citizen;
//import com.mineglicht.util.MessageUtils;
//
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
//import org.bukkit.event.Listener;
//
//import java.util.UUID;
//
///**
// * Listener para eventos relacionados con las ciudades
// */
//public class CityListener implements Listener {
//
//    private final cityWars plugin;
//    private final CityManager cityManager;
//    private final CitizenManager citizenManager;
//    private final EconomyManager economyManager;
//
//    public CityListener(cityWars plugin) {
//        this.plugin = plugin;
//        this.cityManager = plugin.getCityManager();
//        this.citizenManager = plugin.getCitizenManager();
//        this.economyManager = plugin.getEconomyManager();
//        plugin.getServer().getPluginManager().registerEvents(this, plugin);
//    }
//
//    /**
//     * Manejador para la creación de una ciudad
//     */
//    @EventHandler(priority = EventPriority.NORMAL)
//    public void onCityCreate(CityCreateEvent event) {
//        City city = event.getCreatedCity();
//        Player founder = event.getMayor();
//
//        // Verificar si el evento fue cancelado
//        if (event.isCancelled()) {
//            return;
//        }
//
//        // Crear cuenta bancaria para la ciudad
//        economyManager.createCityBank(city);
//
//        // Anunciar creación de la ciudad a todo el servidor
//        plugin.getServer().broadcastMessage(
//                MessageUtils.formatMessage("city.created",
//                        "%city%", city.getName(),
//                        "%founder%", founder.getName())
//        );
//
//        // Establecer la bandera de protección de la ciudad
//        cityManager.setupCityFlag(city.getId(), location);
//
//        // Actualizar el contador de ciudadanos online
//        city.setOnlineCount(1); // El fundador está online
//        city.setCitizenCount(1); // El fundador es el primer ciudadano
//
//        // Guardar la ciudad
//        cityManager.saveCities();
//    }
//
//    /**
//     * Manejador para la eliminación de una ciudad
//     */
//    @EventHandler(priority = EventPriority.NORMAL)
//    public void onCityDelete(CityDeleteEvent event) {
//        City city = event.getCity();
//        UUID deleterId = event.getDeletedBy();
//
//        // Verificar si el evento fue cancelado
//        if (event.isCancelled()) {
//            return;
//        }
//
//        // Eliminar la cuenta bancaria de la ciudad
//        economyManager.deleteCityAccount(city.getId());
//
//        // Liberar a todos los ciudadanos de la ciudad
//        for (UUID citizenId : cityManager.getCityCitizenIds(city.getId())) {
//            Citizen citizen = citizenManager.getCitizen(citizenId);
//            if (citizen != null) {
//                citizen.setCityId(null);
//                citizen.setRole(null);
//                citizenManager.saveCitizen(citizenId);
//
//                // Notificar al jugador si está online
//                Player player = plugin.getServer().getPlayer(citizenId);
//                if (player != null && player.isOnline()) {
//                    player.sendMessage(MessageUtils.formatMessage("city.you_city_deleted",
//                            "%city%", city.getName()));
//                }
//            }
//        }
//
//        // Eliminar la bandera de la ciudad
//        cityManager.removeCityFlag(city.getId());
//
//        // Anunciar la eliminación de la ciudad a todo el servidor
//        String deleterName = "Consola";
//        if (deleterId != null) {
//            Player deleter = plugin.getServer().getPlayer(deleterId);
//            if (deleter != null) {
//                deleterName = deleter.getName();
//            }
//        }
//
//        plugin.getServer().broadcastMessage(
//                MessageUtils.formatMessage("city.deleted",
//                        "%city%", city.getName(),
//                        "%deleter%", deleterName)
//        );
//
//        // Eliminar la ciudad de la base de datos
//        cityManager.deleteCity(city.getId());
//    }
//}