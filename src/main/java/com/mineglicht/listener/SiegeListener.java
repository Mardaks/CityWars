//package com.mineglicht.listener;
//
//import com.mineglicht.cityWars;
//import com.mineglicht.api.event.SiegeStartEvent;
//import com.mineglicht.api.event.SiegeEndEvent;
//import com.mineglicht.models.City;
//import com.mineglicht.models.SiegeState;
//import com.mineglicht.models.SiegeFlag;
//import com.mineglicht.util.ItemUtils;
//import com.mineglicht.util.MessageUtils;
//
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
//import org.bukkit.event.Listener;
//import org.bukkit.event.block.BlockPlaceEvent;
//import org.bukkit.event.block.BlockBreakEvent;
//import org.bukkit.event.entity.EntityDamageByEntityEvent;
//import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.inventory.ItemStack;
//
//import java.util.UUID;
//
///**
// * Listener para eventos relacionados con asedios
// */
//public class SiegeListener implements Listener {
//
//    private final cityWars plugin;
//    private final SiegeManager siegeManager;
//    private final CityManager cityManager;
//    private final EconomyManager economyManager;
//    private final CitizenManager citizenManager;
//
//    public SiegeListener(cityWars plugin) {
//        this.plugin = plugin;
//        this.siegeManager = plugin.getSiegeManager();
//        this.cityManager = plugin.getCityManager();
//        this.economyManager = plugin.getEconomyManager();
//        this.citizenManager = plugin.getCitizenManager();
//        plugin.getServer().getPluginManager().registerEvents(this, plugin);
//    }
//
//    /**
//     * Manejador para la colocación de bloques (estandarte de asedio)
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onBlockPlace(BlockPlaceEvent event) {
//        Player player = event.getPlayer();
//
//        // Verificar si el jugador está intentando colocar un estandarte de asedio
//        if (!siegeManager.isPlayerCarryingSiegeFlag(player)) {
//            return;
//        }
//
//        // Verificar si la ubicación está dentro de una ciudad
//        City targetCity = cityManager.getCityAtLocation(event.getBlock().getLocation());
//        if (targetCity == null) {
//            player.sendMessage(MessageUtils.formatMessage("siege.not_in_city"));
//            event.setCancelled(true);
//            return;
//        }
//
//        // Verificar si el jugador pertenece a la ciudad objetivo
//        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
//        if (playerCity == null) {
//            player.sendMessage(MessageUtils.formatMessage("siege.not_in_any_city"));
//            event.setCancelled(true);
//            return;
//        }
//
//        if (playerCity.getId().equals(targetCity.getId())) {
//            player.sendMessage(MessageUtils.formatMessage("siege.cannot_attack_own_city"));
//            event.setCancelled(true);
//            return;
//        }
//
//        // Verificar si la ciudad objetivo tiene suficientes jugadores online (30%)
//        double onlinePercentage = citizenManager.getOnlineCitizenPercentage(targetCity.getId());
//        if (onlinePercentage < 0.3) {
//            player.sendMessage(MessageUtils.formatMessage("siege.not_enough_defenders",
//                    "%required%", "30%",
//                    "%current%", String.format("%.1f%%", onlinePercentage * 100)));
//            event.setCancelled(true);
//            return;
//        }
//
//        // Verificar si alguna de las ciudades está en asedio
//        if (siegeManager.isCityUnderSiege(targetCity.getId())) {
//            player.sendMessage(MessageUtils.formatMessage("siege.city_already_under_siege"));
//            event.setCancelled(true);
//            return;
//        }
//
//        if (siegeManager.isCityUnderSiege(playerCity.getId())) {
//            player.sendMessage(MessageUtils.formatMessage("siege.your_city_already_attacking"));
//            event.setCancelled(true);
//            return;
//        }
//
//        // Verificar cooldown entre asedios
//        if (siegeManager.isInCooldown(playerCity.getId(), targetCity.getId())) {
//            long remainingTime = siegeManager.getSiegeCooldownRemaining(playerCity.getId(), targetCity.getId());
//            player.sendMessage(MessageUtils.formatMessage("siege.cooldown_active",
//                    "%time%", String.format("%.1f", remainingTime / 60000.0)));
//            event.setCancelled(true);
//            return;
//        }
//
//        // Verificar si el jugador tiene suficiente economía para iniciar el asedio
//        String economyType = plugin.getConfig().getString("siege.economy_type", "jp");
//        double siegeCost = plugin.getConfig().getDouble("siege.cost", 1000.0);
//
//        if (!economyManager.hasCurrency(player, economyType, siegeCost)) {
//            player.sendMessage(MessageUtils.formatMessage("siege.not_enough_funds",
//                    "%amount%", String.valueOf(siegeCost),
//                    "%currency%", economyType));
//            event.setCancelled(true);
//            return;
//        }
//
//        // Descontar fondos al jugador
//        economyManager.withdrawCurrency(player, economyType, siegeCost);
//
//        // Permitir la colocación del estandarte y registrar el asedio
//        Location flagLocation = event.getBlock().getLocation();
//
//        // Iniciar el asedio usando el método correcto del SiegeManager
//        siegeManager.startSiege(player, targetCity, flagLocation);
//    }
//
//    /**
//     * Manejador para la ruptura de bloques (estandarte o bandera de ciudad)
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onBlockBreak(BlockBreakEvent event) {
//        Player player = event.getPlayer();
//        Location location = event.getBlock().getLocation();
//
//        // Verificar si el bloque es un estandarte de asedio
//        if (siegeManager.isSiegeFlagAt(location)) {
//            UUID siegeFlagId = siegeManager.getSiegeFlagIdByCity(cityManager.getCityAtLocation(location).getId());
//            if (siegeFlagId != null) {
//                SiegeFlag siegeFlag = siegeManager.getSiege(siegeFlagId);
//                if (siegeFlag != null) {
//                    // Identificar las ciudades involucradas
//                    City attackingCity = cityManager.getCity(siegeFlag.getAttackingCityId());
//                    City defendingCity = cityManager.getCity(siegeFlag.getDefendingCityId());
//
//                    // Cancelar el asedio
//                    siegeManager.endSiege(siegeFlagId, SiegeState.CANCELLED);
//
//                    // Notificar a ambas ciudades usando métodos correctos
//                    if (attackingCity != null) {
//                        siegeManager.notifyAttackers(attackingCity.getId(),
//                                MessageUtils.formatMessage("siege.your_banner_destroyed",
//                                        "%player%", player.getName()));
//                    }
//
//                    if (defendingCity != null) {
//                        siegeManager.notifyAttackers(defendingCity.getId(),
//                                MessageUtils.formatMessage("siege.enemy_banner_destroyed",
//                                        "%player%", player.getName()));
//                    }
//                }
//            }
//            return;
//        }
//
//        // Verificar si el bloque es una bandera de ciudad
//        // Primero verificamos si estamos en una ciudad
//        City city = cityManager.getCityAtLocation(location);
//        if (city != null) {
//            // Verificamos si el bloque roto es una bandera de ciudad
//            // Asumimos que las banderas de ciudad son del mismo material que los
//            // estandartes de asedio
//            // pero están en el centro/spawn de la ciudad
//            Block block = location.getBlock();
//            String flagMaterialName = plugin.getConfig().getString("siege.flag-material", "WHITE_BANNER");
//            Material flagMaterial;
//            try {
//                flagMaterial = Material.valueOf(flagMaterialName.toUpperCase());
//            } catch (IllegalArgumentException e) {
//                plugin.getLogger().warning("Material de bandera inválido en config: " + flagMaterialName);
//                flagMaterial = Material.WHITE_BANNER;
//            }
//
//            // Si no es el material correcto, no es una bandera de ciudad
//            if (block.getType() != flagMaterial) {
//                city = null;
//            }
//        }
//
//        if (city != null) {
//            // Verificar si hay un asedio activo contra esta ciudad
//            if (siegeManager.isCityUnderSiege(city.getId())) {
//                UUID siegeFlagId = siegeManager.getSiegeFlagIdByCity(city.getId());
//                if (siegeFlagId != null) {
//                    SiegeFlag activeFlag = siegeManager.getSiege(siegeFlagId);
//                    if (activeFlag != null) {
//                        City attackerCity = cityManager.getCity(activeFlag.getAttackingCityId());
//                        if (attackerCity != null) {
//                            // Verificar si el jugador pertenece a la ciudad atacante
//                            City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
//                            if (playerCity != null && playerCity.getId().equals(attackerCity.getId())) {
//                                // El jugador atacante ha capturado la bandera
//                                siegeManager.captureFlag(player, siegeFlagId);
//
//                                // Notificar a ambas ciudades
//                                siegeManager.notifyAttackers(attackerCity.getId(),
//                                        MessageUtils.formatMessage("siege.your_city_captured_flag",
//                                                "%city%", city.getName()));
//
//                                siegeManager.notifyAttackers(city.getId(),
//                                        MessageUtils.formatMessage("siege.your_flag_captured",
//                                                "%city%", attackerCity.getName()));
//
//                                // El resto de la lógica se maneja en el SiegeManager
//                                return;
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Si no es parte de un asedio, no permitir romper la bandera a menos que sea un
//            // admin
//            if (!player.hasPermission("citywars.admin.breakcityflag")) {
//                player.sendMessage(MessageUtils.formatMessage("city.cannot_break_flag"));
//                event.setCancelled(true);
//            }
//        }
//    }
//
//    /**
//     * Manejador para el evento de inicio de asedio
//     */
//    @EventHandler(priority = EventPriority.NORMAL)
//    public void onSiegeStart(SiegeStartEvent event) {
//        City attackerCity = event.getAttackerCity();
//        City defenderCity = event.getDefenderCity();
//
//        // Anunciar inicio de asedio al servidor
//        if (attackerCity != null && defenderCity != null) {
//            plugin.getServer().broadcastMessage(
//                    MessageUtils.formatMessage("siege.server_announce_start",
//                            "%attacker%", attackerCity.getName(),
//                            "%defender%", defenderCity.getName()));
//        }
//    }
//
//    /**
//     * Manejador para el evento de fin de asedio
//     */
//    @EventHandler(priority = EventPriority.NORMAL)
//    public void onSiegeEnd(SiegeEndEvent event) {
//        City attackerCity = event.getAttackingCity();
//        City defenderCity = event.getDefendingCity();
//
//        if (attackerCity == null || defenderCity == null) {
//            return;
//        }
//
//        // Configurar cooldown entre estas ciudades
//        @SuppressWarnings("unused")
//        int cooldownMinutes = plugin.getConfig().getInt("siege.cooldown_minutes", 60); //VARIABLE NO USADA
//        siegeManager.setCooldown(attackerCity.getId(), defenderCity.getId());
//
//        // Anunciar fin de asedio según el motivo final
//        String messageKey;
//        SiegeEndEvent.SiegeEndReason reason = event.getEndReason();
//        switch (reason) {
//            case TIME_EXPIRED:
//                messageKey = "siege.server_announce_timeout";
//                break;
//            case FLAG_CAPTURED:
//                messageKey = "siege.server_announce_captured";
//                break;
//            case ADMIN_STOP:
//            case INITIATOR_DISCONNECTED:
//                messageKey = "siege.server_announce_cancelled";
//                break;
//            default:
//                messageKey = "siege.server_announce_end";
//                break;
//        }
//
//        plugin.getServer().broadcastMessage(
//                MessageUtils.formatMessage(messageKey,
//                        "%attacker%", attackerCity.getName(),
//                        "%defender%", defenderCity.getName()));
//    }
//
//    /**
//     * Manejador para daño entre entidades (protección y jugadores)
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onEntityDamage(EntityDamageByEntityEvent event) {
//        // Verificar si el daño es entre jugadores
//        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
//            Player attacker = (Player) event.getDamager();
//            Player victim = (Player) event.getEntity();
//
//            // Obtener las ciudades de ambos jugadores
//            City attackerCity = citizenManager.getPlayerCity(attacker.getUniqueId());
//            City victimCity = citizenManager.getPlayerCity(victim.getUniqueId());
//
//            // Si ambos jugadores pertenecen a ciudades
//            if (attackerCity != null && victimCity != null) {
//                // Si pertenecen a la misma ciudad, cancelar el daño a menos que esté
//                // configurado lo contrario
//                if (attackerCity.getId().equals(victimCity.getId()) &&
//                        !plugin.getConfig().getBoolean("city.allow_friendly_fire", false)) {
//                    event.setCancelled(true);
//                    attacker.sendMessage(MessageUtils.formatMessage("city.cannot_attack_ally"));
//                    return;
//                }
//
//                // Verificar si hay un asedio activo entre estas ciudades
//                boolean isActiveConflict = (siegeManager.isCityUnderSiege(attackerCity.getId()) ||
//                        siegeManager.isCityUnderSiege(victimCity.getId()));
//
//                // Si no hay un conflicto activo y el PvP está desactivado entre ciudades,
//                // cancelar el daño
//                if (!isActiveConflict && !plugin.getConfig().getBoolean("city.allow_intercity_pvp", false)) {
//                    event.setCancelled(true);
//                    attacker.sendMessage(MessageUtils.formatMessage("city.no_active_conflict"));
//                    return;
//                }
//            }
//        }
//    }
//
//    /**
//     * Manejador para interacción de jugadores (detección de objetos relacionados
//     * con asedio)
//     */
//    @EventHandler(priority = EventPriority.HIGH)
//    public void onPlayerInteract(PlayerInteractEvent event) {
//        Player player = event.getPlayer();
//        ItemStack item = event.getItem();
//
//        // Verificar si el jugador está intentando usar un estandarte de asedio
//        if (ItemUtils.isSiegeFlag(plugin, item)) { // Cambio aquí: agregar el parámetro plugin
//            // Verificar si el jugador tiene permiso para iniciar asedios
//            if (!player.hasPermission("citywars.siege.start")) {
//                player.sendMessage(MessageUtils.formatMessage("siege.no_permission"));
//                event.setCancelled(true);
//                return;
//            }
//            // Verificar si el jugador pertenece a una ciudad
//            City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
//            if (playerCity == null) {
//                player.sendMessage(MessageUtils.formatMessage("siege.must_belong_to_city"));
//                event.setCancelled(true);
//                return;
//            }
//            // Dar información al jugador sobre cómo usar el estandarte
//            player.sendMessage(MessageUtils.formatMessage("siege.banner_instruction"));
//        }
//    }
//}