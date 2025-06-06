package com.mineglicht.listener;

import com.mineglicht.cityWars;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.SiegeManager;
import com.mineglicht.manager.RegionManager;
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
import java.util.UUID;

/**
 * Listener para eventos relacionados con bloques en CityWars
 * Maneja la protección de bloques en ciudades y durante asedios
 */
public class BlockListener implements Listener {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final SiegeManager siegeManager;
    private final RegionManager regionManager;

    public BlockListener(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.siegeManager = plugin.getSiegeManager();
        this.regionManager = plugin.getRegionManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Manejador para la ruptura de bloques en ciudades
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Permitir a los administradores romper bloques sin restricciones
        if (player.hasPermission("citywars.admin.bypass")) {
            return;
        }

        // Verificar si el bloque está en una ciudad usando RegionManager
        UUID cityId = regionManager.getCityAtLocation(location);
        if (cityId == null) {
            return; // El bloque no está en una ciudad, permitir romperlo
        }
        
        // Obtener el objeto City usando el UUID
        City city = cityManager.getCityById(cityId);

        // Verificar si el jugador pertenece a esta ciudad
        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
        boolean isInOwnCity = false;
        
        if (citizen != null) {
            City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
            isInOwnCity = (playerCity != null && playerCity.getId().equals(city.getId()));
        }

        // Verificar si la ciudad está bajo asedio
        boolean isUnderSiege = siegeManager.isCityUnderSiege(city.getId());

        // Si es miembro de la ciudad y la ciudad no está bajo asedio, permitir romper bloques
        if (isInOwnCity && !isUnderSiege) {
            return;
        }

        // Si la ciudad está bajo asedio, verificar si es un atacante legítimo
        if (isUnderSiege) {
            // Obtener los miembros atacantes del asedio
            List<Player> attackers = siegeManager.getCityMembers("attackers_" + city.getId().toString());
            if (attackers != null && attackers.contains(player)) {
                // Permitir romper bloques durante el asedio si es atacante
                return;
            }
        }

        // En cualquier otro caso, no permitir romper bloques
        event.setCancelled(true);
        MessageUtils.sendMessage(player, "city.cannot_break_block");
    }

    /**
     * Manejador para la colocación de bloques en ciudades
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Permitir a los administradores colocar bloques sin restricciones
        if (player.hasPermission("citywars.admin.bypass")) {
            return;
        }

        // Verificar si el bloque está en una ciudad
        UUID cityId = regionManager.getCityAtLocation(location);
        if (cityId == null) {
            return; // El bloque no está en una ciudad, permitir colocarlo
        }
        
        // Obtener el objeto City usando el UUID
        City city = cityManager.getCityById(cityId);

        // Verificar si el jugador pertenece a esta ciudad
        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
        boolean isInOwnCity = false;
        
        if (citizen != null) {
            City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
            isInOwnCity = (playerCity != null && playerCity.getId().equals(city.getId()));
        }

        // Si es miembro de la ciudad, permitir colocar bloques
        if (isInOwnCity) {
            return;
        }

        // Verificar si la ciudad está bajo asedio y si se permite colocar bloques durante el asedio
        if (siegeManager.isCityUnderSiege(city.getId())) {
            List<Player> attackers = siegeManager.getCityMembers("attackers_" + city.getId().toString());
            if (attackers != null && attackers.contains(player)) {
                // Verificar configuración para permitir colocación durante asedio
                if (plugin.getConfig().getBoolean("siege.allow_block_place_during_siege", false)) {
                    return;
                }
            }
        }

        // En cualquier otro caso, no permitir colocar bloques
        event.setCancelled(true);
        MessageUtils.sendMessage(player, "city.cannot_place_block");
    }

    /**
     * Manejador para explosiones de bloques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList(), event.getBlock().getLocation());
    }

    /**
     * Manejador para explosiones de entidades
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList(), event.getLocation());
    }

    /**
     * Método auxiliar para manejar explosiones
     * @param blocks Lista de bloques afectados por la explosión
     * @param explosionLocation Ubicación de la explosión
     */
    private void handleExplosion(List<Block> blocks, Location explosionLocation) {
        Iterator<Block> it = blocks.iterator();

        while (it.hasNext()) {
            Block block = it.next();
            Location location = block.getLocation();

            // Verificar si el bloque está en una ciudad
            UUID cityId = regionManager.getCityAtLocation(location);
            if (cityId != null) {
                // Obtener el objeto City usando el UUID
                City city = cityManager.getCityById(cityId);
                // Verificar si la ciudad está bajo asedio
                boolean isUnderSiege = siegeManager.isCityUnderSiege(city.getId());

                // Si la ciudad no está bajo asedio, proteger todos los bloques
                if (!isUnderSiege) {
                    it.remove();
                    continue;
                }

                // Durante el asedio, proteger contenedores a menos que esté configurado para permitirlo
                if (block.getState() instanceof Container) {
                    if (!plugin.getConfig().getBoolean("siege.allow_container_explosion", false)) {
                        it.remove();
                        continue;
                    }
                }

                // Proteger bloques especiales siempre
                if (isSpecialBlock(block)) {
                    it.remove();
                    continue;
                }

                // Verificar si el bloque está protegido por banderas de ciudad
                if (isBlockProtectedByFlags(block, city)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Verifica si un bloque es especial y debe estar siempre protegido
     * @param block El bloque a verificar
     * @return true si es un bloque especial
     */
    private boolean isSpecialBlock(Block block) {
        Material type = block.getType();
        
        // Proteger banners y banderas
        if (type.name().contains("BANNER")) {
            return true;
        }
        
        // Proteger lecterns (para libros de ciudad)
        if (type == Material.LECTERN) {
            return true;
        }
        
        // Proteger beacons
        if (type == Material.BEACON) {
            return true;
        }
        
        // Proteger armor stands
        return false; // Los armor stands son entidades, no bloques
    }

    /**
     * Verifica si un bloque está protegido por las banderas de la ciudad
     * @param block El bloque a verificar
     * @param city La ciudad donde está el bloque
     * @return true si el bloque está protegido
     */
    private boolean isBlockProtectedByFlags(Block block, City city) {
        // Verificar protección contra explosiones
        if (plugin.getConfig().getBoolean("city.flags.explosion_protection", true)) {
            return true;
        }
        
        // Verificar protección de contenedores
        if (block.getState() instanceof Container && 
            plugin.getConfig().getBoolean("city.flags.container_protection", true)) {
            return true;
        }
        
        return false;
    }

    /**
     * Verifica si una ubicación contiene una bandera de asedio
     * @param location La ubicación a verificar
     * @return true si hay una bandera de asedio en esa ubicación
     */
    private boolean isSiegeFlagLocation(Location location) {
        return siegeManager.isSiegeFlagAt(location);
    }

    /**
     * Obtiene la ciudad en una ubicación específica
     * @param location La ubicación a verificar
     * @return La ciudad en esa ubicación o null si no hay ninguna
     */
    private City getCityAtLocation(Location location) {
        UUID cityId = regionManager.getCityAtLocation(location);
        if (cityId != null) {
            return cityManager.getCityById(cityId);
        }
        return null;
    }

    /**
     * Verifica si un jugador puede interactuar con bloques en una ciudad
     * @param player El jugador
     * @param city La ciudad
     * @return true si puede interactuar
     */
    private boolean canPlayerInteractInCity(Player player, City city) {
        // Administradores siempre pueden interactuar
        if (player.hasPermission("citywars.admin.bypass")) {
            return true;
        }

        // Verificar si es miembro de la ciudad
        Citizen citizen = citizenManager.getCitizen(player.getUniqueId());
        if (citizen != null) {
            City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
            return (playerCity != null && playerCity.getId().equals(city.getId()));
        }

        return false;
    }
}