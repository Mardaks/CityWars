package com.mineglicht.listener;

import com.mineglicht.cityWars;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.SiegeManager;
import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import com.mineglicht.util.MessageUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

/**
 * Listener para eventos relacionados con bloques
 */
public class BlockListener implements Listener {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final SiegeManager siegeManager;

    public BlockListener(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.siegeManager = plugin.getSiegeManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

//    /**
//     * Manejador para la ruptura de bloques en ciudades
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onBlockBreak(BlockBreakEvent event) {
//        Player player = event.getPlayer();
//        Block block = event.getBlock();
//        Location location = block.getLocation();
//
//        // Permitir a los administradores romper bloques sin restricciones
//        if (player.hasPermission("citywars.admin.bypass")) {
//            return;
//        }
//
//        // Verificar si el bloque está en una ciudad
//        City city = cityManager.getCityAtLocation(location);
//        if (city == null) {
//            return; // El bloque no está en una ciudad, permitir romperlo
//        }
//
//        // Verificar si el jugador pertenece a esta ciudad
//        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
//        boolean isInOwnCity = (citizen != null && citizen.getCityId() != null &&
//                citizen.getCityId().equals(city.getId()));
//
//        // Verificar si la ciudad está en fase de saqueo
//        boolean isUnderLoot = siegeManager.isCityInLootPhase(city.getId());
//
//        // Si es miembro de la ciudad y la ciudad no está en fase de saqueo, permitir romper bloques
//        if (isInOwnCity && !isUnderLoot) {
//            return;
//        }
//
//        // Si la ciudad está en fase de saqueo y el jugador es un atacante legítimo
//        if (isUnderLoot && siegeManager.isPlayerLootingCity(player.getUniqueId(), city.getId())) {
//            // Permitir romper bloques durante la fase de saqueo
//            return;
//        }
//
//        // En cualquier otro caso, no permitir romper bloques
//        event.setCancelled(true);
//        player.sendMessage(MessageUtils.formatMessage("city.cannot_break_block"));
//    }
//
//    /**
//     * Manejador para la colocación de bloques en ciudades
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onBlockPlace(BlockPlaceEvent event) {
//        Player player = event.getPlayer();
//        Block block = event.getBlock();
//        Location location = block.getLocation();
//
//        // Permitir a los administradores colocar bloques sin restricciones
//        if (player.hasPermission("citywars.admin.bypass")) {
//            return;
//        }
//
//        // Verificar si el bloque está en una ciudad
//        City city = cityManager.getCityAtLocation(location);
//        if (city == null) {
//            return; // El bloque no está en una ciudad, permitir colocarlo
//        }
//
//        // Verificar si el jugador pertenece a esta ciudad
//        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
//        boolean isInOwnCity = (citizen != null && citizen.getCityId() != null &&
//                citizen.getCityId().equals(city.getId()));
//
//        // Si es miembro de la ciudad, permitir colocar bloques
//        if (isInOwnCity) {
//            return;
//        }
//
//        // Si la ciudad está en fase de saqueo y el jugador es un atacante legítimo
//        if (siegeManager.isCityInLootPhase(city.getId()) &&
//                siegeManager.isPlayerLootingCity(player.getUniqueId(), city.getId())) {
//
//            // Determinar si se permite colocar bloques durante el saqueo (por configuración)
//            if (plugin.getConfig().getBoolean("siege.allow_block_place_during_loot", false)) {
//                return;
//            }
//        }
//
//        // En cualquier otro caso, no permitir colocar bloques
//        event.setCancelled(true);
//        player.sendMessage(MessageUtils.formatMessage("city.cannot_place_block"));
//    }
//
//    /**
//     * Manejador para explosiones de bloques (protección de ciudades)
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onBlockExplode(BlockExplodeEvent event) {
//        handleExplosion(event.blockList());
//    }
//
//    /**
//     * Manejador para explosiones de entidades (protección de ciudades)
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onEntityExplode(EntityExplodeEvent event) {
//        handleExplosion(event.blockList());
//    }
//
//    /**
//     * Método auxiliar para manejar explosiones
//     * @param blocks Lista de bloques afectados por la explosión
//     */
//    private void handleExplosion(List<Block> blocks) {
//        Iterator<Block> it = blocks.iterator();
//
//        while (it.hasNext()) {
//            Block block = it.next();
//            Location location = block.getLocation();
//
//            // Verificar si el bloque está en una ciudad
//            City city = cityManager.getCityAtLocation(location);
//            if (city != null) {
//                // Verificar si la ciudad está en fase de saqueo
//                boolean isUnderLoot = siegeManager.isCityInLootPhase(city.getId());
//
//                // Si la ciudad no está en fase de saqueo, proteger el bloque
//                if (!isUnderLoot) {
//                    it.remove();
//                    continue;
//                }
//
//                // Si es bandera de ciudad o estandarte de asedio, siempre proteger
//                if (cityManager.isCityFlagLocation(location) ||
//                        siegeManager.isSiegeFlagLocation(location)) {
//                    it.remove();
//                    continue;
//                }
//
//                // Durante la fase de saqueo, permitir explosiones excepto de contenedores (a menos que esté configurado)
//                if (block.getState() instanceof Container &&
//                        !plugin.getConfig().getBoolean("siege.allow_container_explosion", false)) {
//                    it.remove();
//                }
//            }
//        }
//    }
}