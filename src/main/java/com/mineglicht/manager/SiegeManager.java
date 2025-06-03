package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeFlag;
import com.mineglicht.models.SiegeState;
import com.mineglicht.models.CityFlag;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SiegeManager {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final EconomyManager economyManager;
    private final RegionManager regionManager;
    private final CitizenManager citizenManager;

    // Almacenamiento de asedios activos
    private final Map<UUID, SiegeFlag> activeSieges;
    private final Map<UUID, Long> siegeCooldowns; // cityId -> cooldown end time
    private final Map<UUID, BukkitTask> siegeTasks; // flagId -> task
    private final Map<UUID, BukkitTask> fireworkTasks; // flagId -> firework task
    private final Map<String, Set<UUID>> cityMembersMap = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> cityLeadersMap = new ConcurrentHashMap<>();

    // Configuración
    private FileConfiguration config;
    private File siegeConfigFile;

    // Configuraciones por defecto
    private String siegeCurrency;
    private double siegeCost;
    private int siegeDuration;
    private int fireworkInterval;
    private long siegeCooldownTime;
    private double minimumOnlinePercentage;

    public SiegeManager(cityWars plugin, CityManager cityManager, EconomyManager economyManager,
                        RegionManager regionManager, CitizenManager citizenManager) {
        this.plugin = plugin;
        this.cityManager = cityManager;
        this.economyManager = economyManager;
        this.regionManager = regionManager;
        this.citizenManager = citizenManager;

        this.activeSieges = new ConcurrentHashMap<>();
        this.siegeCooldowns = new ConcurrentHashMap<>();
        this.siegeTasks = new ConcurrentHashMap<>();
        this.fireworkTasks = new ConcurrentHashMap<>();

        loadConfiguration();
        loadSieges();
        startCooldownCleanupTask();
    }

    /**
     * Inicia un asedio
     */
    public boolean startSiege(Player attacker, City defendingCity, Location flagLocation) {
        City attackingCity = citizenManager.getPlayerCity(attacker.getUniqueId());

        // Validaciones previas
        if (!canStartSiege(attacker, attackingCity, defendingCity, flagLocation)) {
            return false;
        }

        // Cobrar el costo del asedio
        if (!economyManager.withdrawCurrency(attacker, siegeCurrency, siegeCost)) {
            attacker.sendMessage(ChatColor.RED + "No tienes suficiente " + siegeCurrency + " para iniciar un asedio.");
            return false;
        }

        // Crear el estandarte de asedio
        SiegeFlag siegeFlag = new SiegeFlag(
                attackingCity.getId(),
                defendingCity.getId(),
                attacker,
                flagLocation,
                siegeDuration);

        // Registrar el asedio
        activeSieges.put(siegeFlag.getId(), siegeFlag);

        // Actualizar estados de las ciudades
        attackingCity.setSiegeState(SiegeState.ACTIVE);
        defendingCity.setSiegeState(SiegeState.ACTIVE);

        // Colocar el bloque del estandarte
        placeSiegeBanner(flagLocation);

        // Activar PvP en la región
        defendingCity.setFlag(CityFlag.PVP, true);
        regionManager.updateCityRegionFlags(defendingCity);

        // Iniciar tareas del asedio
        startSiegeTasks(siegeFlag);

        // Notificar a los jugadores
        notifySiegeStart(attackingCity, defendingCity, attacker);

        return true;
    }

    /**
     * Valida si se puede iniciar un asedio
     */
    private boolean canStartSiege(Player attacker, City attackingCity, City defendingCity, Location flagLocation) {
        // Verificar que el atacante esté en una ciudad
        if (attackingCity == null) {
            attacker.sendMessage(ChatColor.RED + "Debes pertenecer a una ciudad para iniciar un asedio.");
            return false;
        }

        // Verificar que la ubicación esté dentro del territorio enemigo
        if (!defendingCity.isInCity(flagLocation)) {
            attacker.sendMessage(ChatColor.RED + "Debes colocar el estandarte dentro del territorio enemigo.");
            return false;
        }

        // Verificar que la ciudad atacante no esté bajo asedio
        if (attackingCity.isUnderSiege()) {
            attacker.sendMessage(ChatColor.RED + "Tu ciudad está bajo asedio y no puede atacar.");
            return false;
        }

        // Verificar que la ciudad defensora no esté siendo atacada
        if (defendingCity.getSiegeState() != SiegeState.NONE) {
            attacker.sendMessage(ChatColor.RED + "Esta ciudad ya está bajo asedio.");
            return false;
        }

        // Verificar cooldown
        if (isInCooldown(attackingCity.getId(), defendingCity.getId())) {
            attacker.sendMessage(ChatColor.RED + "Debes esperar antes de atacar esta ciudad nuevamente.");
            return false;
        }

        // Verificar porcentaje mínimo de jugadores conectados
        double onlinePercentage = citizenManager.getOnlineCitizenPercentage(defendingCity.getId());
        if (onlinePercentage < minimumOnlinePercentage) {
            attacker.sendMessage(ChatColor.RED + "La ciudad debe tener al menos " +
                    (int) (minimumOnlinePercentage * 100) + "% de sus ciudadanos conectados.");
            return false;
        }

        // Verificar que el atacante tenga la economía suficiente
        if (!economyManager.hasCurrency(attacker, siegeCurrency, siegeCost)) {
            attacker.sendMessage(
                    ChatColor.RED + "Necesitas " + siegeCost + " " + siegeCurrency + " para iniciar un asedio.");
            return false;
        }

        return true;
    }

    /**
     * Captura la bandera enemiga
     */
    public boolean captureFlag(Player player, UUID siegeFlagId) {
        SiegeFlag siegeFlag = activeSieges.get(siegeFlagId);
        if (siegeFlag == null || siegeFlag.getState() != SiegeState.ACTIVE) {
            return false;
        }

        City attackingCity = cityManager.getAllCities().stream()
                .filter(c -> c.getId().equals(siegeFlag.getAttackingCityId()))
                .findFirst().orElse(null);

        if (attackingCity == null || !citizenManager.isInCity(player.getUniqueId(), attackingCity.getId())) {
            return false;
        }

        // Capturar la bandera
        siegeFlag.captureFlag();

        City defendingCity = cityManager.getAllCities().stream()
                .filter(c -> c.getId().equals(siegeFlag.getDefendingCityId()))
                .findFirst().orElse(null);

        if (defendingCity != null) {
            // Desactivar protecciones durante la fase de saqueo
            disableCityProtections(defendingCity);

            // Notificar captura de bandera
            notifyFlagCaptured(attackingCity, defendingCity, player);

            // Iniciar fase de saqueo
            startLootPhase(siegeFlag);
        }

        return true;
    }

    /**
     * Finaliza un asedio
     */
    public void endSiege(UUID siegeFlagId, SiegeState endState) {
        SiegeFlag siegeFlag = activeSieges.get(siegeFlagId);
        if (siegeFlag == null)
            return;

        City attackingCity = cityManager.getAllCities().stream()
                .filter(c -> c.getId().equals(siegeFlag.getAttackingCityId()))
                .findFirst().orElse(null);

        City defendingCity = cityManager.getAllCities().stream()
                .filter(c -> c.getId().equals(siegeFlag.getDefendingCityId()))
                .findFirst().orElse(null);

        // Finalizar el asedio
        siegeFlag.endSiege(endState);

        // Restaurar estados de las ciudades
        if (attackingCity != null) {
            attackingCity.setSiegeState(SiegeState.NONE);
        }

        if (defendingCity != null) {
            defendingCity.setSiegeState(SiegeState.NONE);
            // Restaurar protecciones
            restoreCityProtections(defendingCity);
        }

        // Detener tareas
        stopSiegeTasks(siegeFlagId);

        // Remover estandarte
        removeSiegeBanner(siegeFlag.getLocation());

        // Procesar recompensas
        if (endState == SiegeState.SUCCESSFUL && attackingCity != null && defendingCity != null) {
            processVictoryRewards(attackingCity, defendingCity);
        }

        // Establecer cooldown
        if (attackingCity != null && defendingCity != null) {
            setCooldown(attackingCity.getId(), defendingCity.getId());
        }

        // Remover del registro
        activeSieges.remove(siegeFlagId);

        // Notificar fin del asedio
        notifySiegeEnd(attackingCity, defendingCity, endState);
    }

    /**
     * Cancela un asedio administrativamente
     */
    public boolean cancelSiege(UUID siegeFlagId) {
        if (!activeSieges.containsKey(siegeFlagId)) {
            return false;
        }

        endSiege(siegeFlagId, SiegeState.CANCELLED);
        return true;
    }

    /**
     * Obtiene un asedio por su ID
     */
    public SiegeFlag getSiege(UUID siegeFlagId) {
        return activeSieges.get(siegeFlagId);
    }

    /**
     * Obtiene todos los asedios activos
     */
    public Collection<SiegeFlag> getActiveSieges() {
        return new ArrayList<>(activeSieges.values());
    }

    /**
     * Verifica si una ciudad puede ser atacada
     */
    public boolean canCityBeAttacked(UUID cityId) {
        City city = cityManager.getAllCities().stream()
                .filter(c -> c.getId().equals(cityId))
                .findFirst().orElse(null);

        return city != null && city.canBeAttacked();
    }

    /**
     * Verifica si hay cooldown entre dos ciudades
     */
    public boolean isInCooldown(UUID attackingCityId, UUID defendingCityId) {
        String key = attackingCityId + ":" + defendingCityId;
        Long cooldownEnd = siegeCooldowns.get(UUID.nameUUIDFromBytes(key.getBytes()));
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Registra un participante en el asedio
     */
    public void registerParticipant(Player player, UUID siegeFlagId, boolean isAttacker) {
        SiegeFlag siegeFlag = activeSieges.get(siegeFlagId);
        if (siegeFlag != null) {
            siegeFlag.registerParticipant(player, isAttacker);
        }
    }

    /**
     * Inicia las tareas del asedio
     */
    private void startSiegeTasks(SiegeFlag siegeFlag) {
        // Tarea principal del asedio
        BukkitTask siegeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeSieges.containsKey(siegeFlag.getId())) {
                    cancel();
                    return;
                }

                if (siegeFlag.isExpired()) {
                    endSiege(siegeFlag.getId(), SiegeState.DEFENDED);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Cada segundo

        siegeTasks.put(siegeFlag.getId(), siegeTask);

        // Tarea de fuegos artificiales
        BukkitTask fireworkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeSieges.containsKey(siegeFlag.getId())) {
                    cancel();
                    return;
                }

                spawnFireworks(siegeFlag.getLocation());
            }
        }.runTaskTimer(plugin, 20L, fireworkInterval * 20L);

        fireworkTasks.put(siegeFlag.getId(), fireworkTask);
    }

    /**
     * Detiene las tareas del asedio
     */
    private void stopSiegeTasks(UUID siegeFlagId) {
        BukkitTask siegeTask = siegeTasks.remove(siegeFlagId);
        if (siegeTask != null) {
            siegeTask.cancel();
        }

        BukkitTask fireworkTask = fireworkTasks.remove(siegeFlagId);
        if (fireworkTask != null) {
            fireworkTask.cancel();
        }
    }

    /**
     * Inicia la fase de saqueo
     */
    private void startLootPhase(SiegeFlag siegeFlag) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (siegeFlag.isLootPhaseEnded()) {
                    endSiege(siegeFlag.getId(), SiegeState.SUCCESSFUL);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Desactiva las protecciones de la ciudad
     */
    private void disableCityProtections(City city) {
        city.setFlag(CityFlag.PREVENT_BLOCK_BREAK, false);
        city.setFlag(CityFlag.PREVENT_BLOCK_PLACE, false);
        city.setFlag(CityFlag.PREVENT_INTERACTION, false);
        city.setFlag(CityFlag.PREVENT_EXPLOSION, false);
        regionManager.updateCityRegionFlags(city);

        // Notificar desactivación de defensas
        Set<UUID> citizens = citizenManager.getCitizensInCity(city.getId());
        for (UUID citizenId : citizens) {
            Player citizen = Bukkit.getPlayer(citizenId);
            if (citizen != null && citizen.isOnline()) {
                citizen.sendTitle("", ChatColor.RED + "¡Defensas desactivadas!", 10, 70, 20);
            }
        }
    }

    /**
     * Restaura las protecciones de la ciudad
     */
    private void restoreCityProtections(City city) {
        city.setFlag(CityFlag.PREVENT_BLOCK_BREAK, true);
        city.setFlag(CityFlag.PREVENT_BLOCK_PLACE, true);
        city.setFlag(CityFlag.PREVENT_INTERACTION, true);
        city.setFlag(CityFlag.PREVENT_EXPLOSION, true);
        city.setFlag(CityFlag.PVP, false);
        regionManager.updateCityRegionFlags(city);
    }

    /**
     * Procesa las recompensas de victoria
     */
    private void processVictoryRewards(City attackingCity, City defendingCity) {
        double defendingBalance = economyManager.getCityBankBalance(defendingCity);
        double reward = defendingBalance * 0.5; // 50% del fondo

        // Transferir fondos
        economyManager.transferCityBankFunds(defendingCity, attackingCity, reward);

        // Notificar transferencia
        notifyRewardTransfer(attackingCity, defendingCity, reward);
    }

    /**
     * Establece cooldown entre ciudades
     */
    public void setCooldown(UUID attackingCityId, UUID defendingCityId) {
        String key = attackingCityId + ":" + defendingCityId;
        UUID cooldownId = UUID.nameUUIDFromBytes(key.getBytes());
        long cooldownEnd = System.currentTimeMillis() + siegeCooldownTime;
        siegeCooldowns.put(cooldownId, cooldownEnd);
    }

    /**
     * Coloca el estandarte de asedio
     */
    private void placeSiegeBanner(Location location) {
        Block block = location.getBlock();
        // Aquí iría la lógica para colocar el ítem personalizado definido en config.yml
        // Por ahora uso un bloque temporal
        block.setType(Material.RED_BANNER);
    }

    /**
     * Remueve el estandarte de asedio
     */
    private void removeSiegeBanner(Location location) {
        Block block = location.getBlock();
        block.setType(Material.AIR);
    }

    /**
     * Genera fuegos artificiales
     */
    private void spawnFireworks(Location location) {
        location.getWorld().spawnParticle(Particle.FLAME, location, 50, 2, 2, 2, 0.1);
        location.getWorld().spawnParticle(Particle.SMOKE, location, 20, 1, 1, 1, 0.05);
        location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }

    /**
     * Notifica el inicio del asedio
     */
    private void notifySiegeStart(City attackingCity, City defendingCity, Player attacker) {
        // Notificar a la ciudad defensora
        Set<UUID> defenders = citizenManager.getOnlineCitizensInCity(defendingCity.getId());
        for (UUID defenderId : defenders) {
            Player defender = Bukkit.getPlayer(defenderId);
            if (defender != null && defender.isOnline()) {
                defender.sendTitle("", ChatColor.RED + "¡Estás bajo ataque!", 10, 70, 20);
                defender.sendMessage(
                        ChatColor.RED + "¡Tu ciudad está siendo asediada por " + attackingCity.getName() + "!");
            }
        }

        // Notificar a la ciudad atacante
        Set<UUID> attackers = citizenManager.getOnlineCitizensInCity(attackingCity.getId());
        for (UUID attackerId : attackers) {
            Player attackerPlayer = Bukkit.getPlayer(attackerId);
            if (attackerPlayer != null && attackerPlayer.isOnline()) {
                attackerPlayer.sendMessage(ChatColor.YELLOW + "¡" + attacker.getName()
                        + " ha iniciado un asedio contra " + defendingCity.getName() + "!");
            }
        }
    }

    /**
     * Notifica la captura de bandera
     */
    private void notifyFlagCaptured(City attackingCity, City defendingCity, Player capturer) {
        String message = ChatColor.GOLD + "¡" + capturer.getName()
                + " ha capturado la bandera! ¡Fase de saqueo iniciada!";

        // Notificar a ambas ciudades
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(citizenManager.getOnlineCitizensInCity(attackingCity.getId()));
        allPlayers.addAll(citizenManager.getOnlineCitizensInCity(defendingCity.getId()));

        for (UUID playerId : allPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
                player.sendTitle("", ChatColor.GOLD + "¡BANDERA CAPTURADA!", 10, 70, 20);
            }
        }
    }

    /**
     * Notifica el fin del asedio
     */
    private void notifySiegeEnd(City attackingCity, City defendingCity, SiegeState endState) {
        String message;
        ChatColor color;

        switch (endState) {
            case SUCCESSFUL:
                message = "¡" + attackingCity.getName() + " ha conquistado " + defendingCity.getName() + "!";
                color = ChatColor.GOLD;
                break;
            case DEFENDED:
                message = "¡" + defendingCity.getName() + " ha defendido exitosamente su ciudad!";
                color = ChatColor.GREEN;
                break;
            case CANCELLED:
                message = "El asedio ha sido cancelado por un administrador.";
                color = ChatColor.GRAY;
                break;
            default:
                message = "El asedio ha terminado.";
                color = ChatColor.YELLOW;
        }

        // Notificar a ambas ciudades
        Set<UUID> allPlayers = new HashSet<>();
        if (attackingCity != null) {
            allPlayers.addAll(citizenManager.getOnlineCitizensInCity(attackingCity.getId()));
        }
        if (defendingCity != null) {
            allPlayers.addAll(citizenManager.getOnlineCitizensInCity(defendingCity.getId()));
        }

        for (UUID playerId : allPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(color + message);
            }
        }
    }

    /**
     * Notifica la transferencia de recompensas
     */
    private void notifyRewardTransfer(City attackingCity, City defendingCity, double amount) {
        String message = ChatColor.GOLD + "¡" + attackingCity.getName() + " ha saqueado " + amount + " del tesoro de "
                + defendingCity.getName() + "!";

        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(citizenManager.getOnlineCitizensInCity(attackingCity.getId()));
        allPlayers.addAll(citizenManager.getOnlineCitizensInCity(defendingCity.getId()));

        for (UUID playerId : allPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Inicia la tarea de limpieza de cooldowns
     */
    private void startCooldownCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                siegeCooldowns.entrySet().removeIf(entry -> entry.getValue() < currentTime);
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L, 20L * 60L); // Cada minuto
    }

    /**
     * Carga la configuración
     */
    private void loadConfiguration() {
        siegeConfigFile = new File(plugin.getDataFolder(), "siege-config.yml");
        if (!siegeConfigFile.exists()) {
            plugin.saveResource("siege-config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(siegeConfigFile);

        // Cargar valores de configuración
        siegeCurrency = config.getString("siege.currency", "jp");
        siegeCost = config.getDouble("siege.cost", 1000.0);
        siegeDuration = config.getInt("siege.duration", 1800); // 30 minutos por defecto
        fireworkInterval = config.getInt("siege.firework-interval", 30); // 30 segundos
        config.getInt("siege.loot-phase-duration", 300);
        siegeCooldownTime = config.getLong("siege.cooldown", 3600000L); // 1 hora
        minimumOnlinePercentage = config.getDouble("siege.minimum-online-percentage", 0.3); // 30%
    }

    /**
     * Carga los asedios desde la configuración
     */
    private void loadSieges() {
        // Implementar carga desde archivo si es necesario
        // Por ahora los asedios no persisten entre reinicios
    }

    /**
     * Guarda los asedios
     */
    public void saveSieges() {
        // Implementar guardado si es necesario
        // Por ahora los asedios no persisten entre reinicios
    }

    /**
     * Cierra el manager y limpia recursos
     */
    public void shutdown() {
        // Finalizar todos los asedios activos
        for (UUID siegeId : new HashSet<>(activeSieges.keySet())) {
            endSiege(siegeId, SiegeState.CANCELLED);
        }

        // Cancelar todas las tareas
        siegeTasks.values().forEach(BukkitTask::cancel);
        fireworkTasks.values().forEach(BukkitTask::cancel);

        // Guardar datos
        saveSieges();
    }

    /**
     * Obtiene todos los miembros de una ciudad específica
     *
     * @param cityName Nombre de la ciudad
     * @return Lista de jugadores que son miembros de la ciudad
     */
    public List<Player> getCityMembers(String cityName) {
        List<Player> members = new ArrayList<>();

        // Opción A: Si tienes un mapa de ciudades con sus miembros
        if (cityMembersMap.containsKey(cityName)) {
            Set<UUID> memberUUIDs = cityMembersMap.get(cityName);
            for (UUID uuid : memberUUIDs) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    members.add(player);
                }
            }
        }

        return members;
    }

    /**
     * Obtiene los líderes de una ciudad específica
     *
     * @param cityName Nombre de la ciudad
     * @return Lista de jugadores que son líderes de la ciudad
     */
    public List<Player> getCityLeaders(String cityName) {
        List<Player> leaders = new ArrayList<>();

        // Opción A: Si tienes un mapa de líderes
        if (cityLeadersMap.containsKey(cityName)) {
            Set<UUID> leaderUUIDs = cityLeadersMap.get(cityName);
            for (UUID uuid : leaderUUIDs) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    leaders.add(player);
                }
            }
        }
        return leaders;
    }

    public boolean isSiegeFlagAt(Location location) {
        try {
            Block block = location.getBlock();

            // Obtener el material del estandarte desde config
            String flagMaterialName = plugin.getConfig().getString("siege.flag-material", "WHITE_BANNER");
            Material flagMaterial;

            try {
                flagMaterial = Material.valueOf(flagMaterialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material de estandarte inválido en config: " + flagMaterialName);
                flagMaterial = Material.WHITE_BANNER; // Por defecto
            }

            return block.getType() == flagMaterial;

        } catch (Exception e) {
            plugin.getLogger().warning("Error al verificar estandarte: " + e.getMessage());
            return false;
        }
    }

    public UUID getSiegeFlagIdByCity(UUID cityId) {
        for (Map.Entry<UUID, SiegeFlag> entry : activeSieges.entrySet()) {
            SiegeFlag flag = entry.getValue();
            if (flag.getDefendingCityId().equals(cityId) || flag.getAttackingCityId().equals(cityId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Verifica si una ciudad está bajo asedio
     *
     * @param cityId UUID de la ciudad
     */
    public boolean isCityUnderSiege(UUID cityId) {
        if (cityId == null) {
            return false;
        }

        // Verificar si hay algun asedio activo donde esta cuidad sea la defensora
        for (SiegeFlag siegeFlag : activeSieges.values()) {
            if (siegeFlag.getDefendingCityId().equals(cityId) && siegeFlag.getState() == SiegeState.ACTIVE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si un jugador esta cargando un estandarte de asedio
     *
     * @param player El jugador que esta con el estandarte
     */
    public boolean isPlayerCarryingSiegeFlag(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        // Obtener el material del estandarte desde config
        String flagMaterialName = plugin.getConfig().getString("siege.flag-material", "WHITE_BANNER");
        Material flagMaterial;

        try {
            flagMaterial = Material.valueOf(flagMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material de estandarte invalido en config: " + flagMaterialName);
            flagMaterial = Material.WHITE_BANNER;
        }

        // Verificar si el jugador tiene el estandarte en su inventario
        return player.getInventory().contains(flagMaterial);
    }

    /**
     * Cancela un asedio por UUID del jugador que lleva el estandarte
     *
     * @param playerId UUID del jugador
     * @return true si el jugador es iniciador/portador de un asedio, false en caso contrario
     */
    public boolean cancelSiegeByPlayer(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        // Busca el asedio donde este jugador sea el iniciador/portador
        for (Map.Entry<UUID, SiegeFlag> entry : activeSieges.entrySet()) {
            SiegeFlag siegeFlag = entry.getValue();

            // Verificar si este jugador es quien inicio el asedio
            if (siegeFlag.getAttackerPlayerId() != null && siegeFlag.getAttackerPlayerId().equals(playerId)) {
                return cancelSiege(entry.getKey());
            }
        }
        return false;
    }

    /**
     * Notifica a todos los atacantes de un asedio específico
     *
     * @param cityId  UUID de la ciudad asediada
     * @param message mensaje a los atacantes cuado un defensor muere
     */
    public void notifyAttackers(UUID cityId, String message) {
        if (message == null || message.isEmpty() || cityId == null) {
            return;
        }

        // Buscar el asedio activo para esta ciudad (donde cityId es la ciudad defendida)
        SiegeFlag activeSiege = null;
        for (SiegeFlag siegeFlag : activeSieges.values()) {
            if (siegeFlag.getDefendingCityId().equals(cityId) && siegeFlag.getState() == SiegeState.ACTIVE) {
                activeSiege = siegeFlag;
                break;
            }
        }

        if (activeSiege == null) {
            return;
        }

        // Hacer la variable final para usarlo en el lambda
        final UUID attackingCityId = activeSiege.getAttackingCityId();

        // Obtener la ciudad atacante
        City attackingCity = cityManager.getAllCities().stream()
                .filter(c -> c.getId().equals(attackingCityId))
                .findFirst().orElse(null);

        if (attackingCity == null) {
            return;
        }

        // Notificar a todos los ciudadanos online de la cuidad atacante (osea los atacantes)
        Set<UUID> attackingCitizens = citizenManager.getOnlineCitizensInCity(attackingCity.getId());
        for (UUID citizenId : attackingCitizens) {
            Player citizen = Bukkit.getPlayer(citizenId);
            if (citizen != null && citizen.isOnline()) {
                citizen.sendMessage(message);
            }
        }
    }

    /**
     * Guardar los cooldowns en el archivo cooldown.yml
     */
    public void saveCooldowns() {
        File file = new File(plugin.getDataFolder(), "cooldowns.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, Long> entry : siegeCooldowns.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
            plugin.getLogger().info("Cooldowns guardados correctamente.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar cooldowns: " + e.getMessage());
        }
    }

    /**
     * Cargar los cooldowns desde el archivo cooldowns.yml
     */
    public void loadCooldowns() {
        File file = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID cooldownId = UUID.fromString(key);
                long endTime = config.getLong(key);
                siegeCooldowns.put(cooldownId, endTime);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID inválido en cooldowns.yml: " + key);
            }
        }

        plugin.getLogger().info("Cooldowns cargados correctamente.");
    }
}