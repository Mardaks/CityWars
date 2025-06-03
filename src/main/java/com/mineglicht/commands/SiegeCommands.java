package com.mineglicht.commands;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeFlag;
import com.mineglicht.models.SiegeState;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.EconomyManager;
import com.mineglicht.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SiegeCommands implements CommandExecutor, @Nullable TabCompleter {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final EconomyManager economyManager;

    // Almacenamiento de asedios activos (UUID = SiegeFlag ID)
    private final Map<UUID, SiegeFlag> activeSieges;
    // Cooldowns entre ciudades (String = "cityId1_cityId2")
    private final Map<String, Long> siegeCooldowns;

    public SiegeCommands(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.economyManager = plugin.getEconomyManager();
        this.activeSieges = new ConcurrentHashMap<>();
        this.siegeCooldowns = new ConcurrentHashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendSiegeHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                return handleStartSiege(sender, args);
            case "cancel":
                return handleCancelSiege(sender, args);
            case "info":
                return handleSiegeInfo(sender, args);
            case "list":
                return handleSiegeList(sender, args);
            case "cooldown":
                return handleSiegeCooldown(sender, args);
            case "give":
                return handleGiveBanner(sender, args);
            default:
                sendSiegeHelp(sender);
                return true;
        }
    }

    private boolean handleStartSiege(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.COMMAND_ONLY_PLAYER);
            return true;
        }

        Player player = (Player) sender;

        if (!hasPermission(sender, "start")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /siege start <ciudad_objetivo>");
            return true;
        }

        String targetCityName = args[1];

        // Obtener ciudad del atacante
        City attackingCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (attackingCity == null) {
            player.sendMessage(
                    Messages.PREFIX + ChatColor.RED + "Debes pertenecer a una ciudad para iniciar un asedio.");
            return true;
        }

        // Obtener ciudad objetivo
        City targetCity = getCityByName(targetCityName);
        if (targetCity == null) {
            player.sendMessage(Messages.CITY_NOT_FOUND);
            return true;
        }

        // Validaciones
        if (!canCityAttack(attackingCity, player)) {
            return true;
        }

        if (!canCityBeAttacked(targetCity)) {
            return true;
        }

        if (!hasRequiredCurrency(player)) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "No tienes suficiente dinero para iniciar un asedio.");
            return true;
        }

        if (!hasMinimumOnlineCitizens(targetCity)) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED
                    + "La ciudad objetivo debe tener al menos 30% de ciudadanos conectados.");
            return true;
        }

        if (isCooldownActive(attackingCity, targetCity)) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Hay un cooldown activo entre estas ciudades.");
            return true;
        }

        // Verificar que el jugador esté en territorio enemigo
        if (!isInEnemyTerritory(player, targetCity)) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED
                    + "Debes estar dentro del territorio enemigo para iniciar el asedio.");
            return true;
        }

        // Cobrar la economía requerida
        String requiredCurrency = plugin.getConfig().getString("siege.required-currency", "jp");
        double requiredAmount = plugin.getConfig().getDouble("siege.required-amount", 1000.0);

        if (!economyManager.withdrawCurrency(player, requiredCurrency, requiredAmount)) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al cobrar el costo del asedio.");
            return true;
        }

        // Crear el asedio
        int siegeDuration = plugin.getConfig().getInt("siege.duration-seconds", 3600); // 1 hora por defecto
        SiegeFlag siegeFlag = new SiegeFlag(
                attackingCity.getId(),
                targetCity.getId(),
                player,
                player.getLocation(),
                siegeDuration);

        // Registrar el asedio
        activeSieges.put(siegeFlag.getId(), siegeFlag);
        attackingCity.setSiegeState(SiegeState.ACTIVE);
        targetCity.setSiegeState(SiegeState.ACTIVE);

        // Dar el estandarte al jugador
        giveSiegeBanner(player);

        // Notificar a las ciudades
        notifyAllCityMembers(attackingCity, ChatColor.GREEN + "¡" + player.getName() + " ha iniciado un asedio contra "
                + targetCity.getName() + "!");
        notifyAllCityMembers(targetCity, ChatColor.RED + "¡Estás bajo ataque por " + attackingCity.getName() + "!");

        player.sendMessage(
                Messages.PREFIX + ChatColor.GREEN + "¡Asedio iniciado! Coloca el estandarte en territorio enemigo.");

        return true;
    }

    private boolean handleCancelSiege(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "cancel")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /siege cancel <ciudad>");
            return true;
        }

        String cityName = args[1];
        City city = getCityByName(cityName);

        if (city == null) {
            sender.sendMessage(Messages.CITY_NOT_FOUND);
            return true;
        }

        // Buscar asedio activo de esta ciudad
        SiegeFlag siegeToCancel = null;
        for (SiegeFlag siege : activeSieges.values()) {
            if (siege.getAttackingCityId().equals(city.getId()) ||
                    siege.getDefendingCityId().equals(city.getId())) {
                siegeToCancel = siege;
                break;
            }
        }

        if (siegeToCancel == null) {
            sender.sendMessage(Messages.PREFIX + ChatColor.RED + "No se encontró un asedio activo para esta ciudad.");
            return true;
        }

        // Cancelar el asedio
        endSiege(siegeToCancel, SiegeState.CANCELLED);
        sender.sendMessage(Messages.PREFIX + ChatColor.GREEN + "Asedio cancelado exitosamente.");

        return true;
    }

    private boolean handleSiegeInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Mostrar todos los asedios activos
            if (activeSieges.isEmpty()) {
                sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "No hay asedios activos.");
                return true;
            }

            sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "=== Asedios Activos ===");
            for (SiegeFlag siege : activeSieges.values()) {
                displaySiegeDetails(sender, siege);
            }
            return true;
        }

        String cityName = args[1];
        City city = getCityByName(cityName);

        if (city == null) {
            sender.sendMessage(Messages.CITY_NOT_FOUND);
            return true;
        }

        // Buscar asedio de esta ciudad
        SiegeFlag citySeige = null;
        for (SiegeFlag siege : activeSieges.values()) {
            if (siege.getAttackingCityId().equals(city.getId()) ||
                    siege.getDefendingCityId().equals(city.getId())) {
                citySeige = siege;
                break;
            }
        }

        if (citySeige == null) {
            sender.sendMessage(Messages.PREFIX + ChatColor.RED + "No hay asedio activo para esta ciudad.");
            return true;
        }

        displaySiegeDetails(sender, citySeige);
        return true;
    }

    private boolean handleSiegeList(CommandSender sender, String[] args) {
        if (activeSieges.isEmpty()) {
            sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "No hay asedios activos.");
            return true;
        }

        sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "=== Lista de Asedios Activos ===");
        int count = 1;

        for (SiegeFlag siege : activeSieges.values()) {
            City attackingCity = cityManager.getCityById(siege.getAttackingCityId());
            City defendingCity = cityManager.getCityById(siege.getDefendingCityId());

            if (attackingCity != null && defendingCity != null) {
                String timeRemaining = formatSiegeTime(siege.getRemainingTimeSeconds());
                sender.sendMessage(
                        ChatColor.GRAY + String.valueOf(count) + ". " + ChatColor.RED + attackingCity.getName() +
                                ChatColor.WHITE + " vs " + ChatColor.BLUE + defendingCity.getName() +
                                ChatColor.GRAY + " (" + timeRemaining + ")");
                count++;
            }
        }

        return true;
    }

    private boolean handleSiegeCooldown(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /siege cooldown <ciudad1> <ciudad2>");
            return true;
        }

        City city1 = getCityByName(args[1]);
        City city2 = getCityByName(args[2]);

        if (city1 == null || city2 == null) {
            sender.sendMessage(Messages.CITY_NOT_FOUND);
            return true;
        }

        String cooldownKey = getCooldownKey(city1.getId(), city2.getId());
        Long cooldownEnd = siegeCooldowns.get(cooldownKey);

        if (cooldownEnd == null || System.currentTimeMillis() > cooldownEnd) {
            sender.sendMessage(Messages.PREFIX + ChatColor.GREEN + "No hay cooldown activo entre estas ciudades.");
        } else {
            long remainingTime = (cooldownEnd - System.currentTimeMillis()) / 1000;
            sender.sendMessage(
                    Messages.PREFIX + ChatColor.YELLOW + "Cooldown restante: " + formatSiegeTime(remainingTime));
        }

        return true;
    }

    private boolean handleGiveBanner(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "give")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.COMMAND_ONLY_PLAYER);
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                player = target;
            }
        }

        if (giveSiegeBanner(player)) {
            sender.sendMessage(
                    Messages.PREFIX + ChatColor.GREEN + "Estandarte de asedio entregado a " + player.getName());
        } else {
            sender.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al entregar el estandarte.");
        }

        return true;
    }

    // === MÉTODOS DE VALIDACIÓN ===

    private boolean canCityAttack(City attackingCity, Player attacker) {
        if (attackingCity.getSiegeState() != SiegeState.NONE) {
            attacker.sendMessage(Messages.PREFIX + ChatColor.RED + "Tu ciudad ya está involucrada en un asedio.");
            return false;
        }

        if (!canManageCitySieges(attacker, attackingCity)) {
            attacker.sendMessage(
                    Messages.PREFIX + ChatColor.RED + "No tienes permisos para iniciar asedios por esta ciudad.");
            return false;
        }

        return true;
    }

    private boolean canCityBeAttacked(City defendingCity) {
        return defendingCity.getSiegeState() == SiegeState.NONE;
    }

    private boolean hasRequiredCurrency(Player player) {
        String requiredCurrency = plugin.getConfig().getString("siege.required-currency", "jp");
        double requiredAmount = plugin.getConfig().getDouble("siege.required-amount", 1000.0);

        return economyManager.hasCurrency(player, requiredCurrency, requiredAmount);
    }

    private boolean hasMinimumOnlineCitizens(City city) {
        double onlinePercentage = citizenManager.getOnlineCitizenPercentage(city.getId());
        double requiredPercentage = plugin.getConfig().getDouble("siege.min-online-percentage", 0.30);

        return onlinePercentage >= requiredPercentage;
    }

    private boolean isCooldownActive(City city1, City city2) {
        String cooldownKey = getCooldownKey(city1.getId(), city2.getId());
        Long cooldownEnd = siegeCooldowns.get(cooldownKey);

        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    private boolean isInEnemyTerritory(Player player, City enemyCity) {
        return enemyCity.isInCity(player.getLocation());
    }

    // === MÉTODOS AUXILIARES ===

    private boolean giveSiegeBanner(Player player) {
        try {
            // Usar ExecutableItems API para dar el estandarte
            String bannerItemId = plugin.getConfig().getString("siege.banner-item-id", "siege_banner");

            // Comando ExecutableItems para dar el ítem
            String command = "executableitems give " + player.getName() + " " + bannerItemId + " 1";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al dar estandarte de asedio: " + e.getMessage());
            return false;
        }
    }

    private void notifyAllCityMembers(City city, String message) {
        Set<UUID> citizens = citizenManager.getOnlineCitizensInCity(city.getId());

        for (UUID citizenId : citizens) {
            Player citizen = Bukkit.getPlayer(citizenId);
            if (citizen != null && citizen.isOnline()) {
                citizen.sendMessage(Messages.PREFIX + message);
                // Enviar subtitle
                citizen.sendTitle("", message, 10, 70, 20);
            }
        }
    }

    private void endSiege(SiegeFlag siegeFlag, SiegeState endState) {
        // Obtener ciudades
        City attackingCity = cityManager.getCityById(siegeFlag.getAttackingCityId());
        City defendingCity = cityManager.getCityById(siegeFlag.getDefendingCityId());

        if (attackingCity != null) {
            attackingCity.setSiegeState(SiegeState.NONE);
        }

        if (defendingCity != null) {
            defendingCity.setSiegeState(SiegeState.NONE);
        }

        // Finalizar el asedio
        siegeFlag.endSiege(endState);

        // Remover de asedios activos
        activeSieges.remove(siegeFlag.getId());

        // Establecer cooldown
        if (attackingCity != null && defendingCity != null) {
            long cooldownDuration = plugin.getConfig().getLong("siege.cooldown-hours", 24) * 60 * 60 * 1000; // en
                                                                                                             // milisegundos
            String cooldownKey = getCooldownKey(attackingCity.getId(), defendingCity.getId());
            siegeCooldowns.put(cooldownKey, System.currentTimeMillis() + cooldownDuration);
        }

        // Notificar resultado
        if (attackingCity != null && defendingCity != null) {
            String resultMessage = getEndSiegeMessage(endState, attackingCity.getName(), defendingCity.getName());
            notifyAllCityMembers(attackingCity, resultMessage);
            notifyAllCityMembers(defendingCity, resultMessage);
        }
    }

    private String getCooldownKey(UUID cityId1, UUID cityId2) {
        // Crear clave ordenada para que city1_city2 sea igual a city2_city1
        String id1 = cityId1.toString();
        String id2 = cityId2.toString();

        if (id1.compareTo(id2) < 0) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }

    private String getEndSiegeMessage(SiegeState endState, String attackingCityName, String defendingCityName) {
        switch (endState) {
            case SUCCESSFUL:
                return ChatColor.GREEN + "¡" + attackingCityName + " ha conquistado " + defendingCityName + "!";
            case DEFENDED:
                return ChatColor.BLUE + "¡" + defendingCityName + " ha defendido exitosamente contra "
                        + attackingCityName + "!";
            case CANCELLED:
                return ChatColor.GRAY + "El asedio entre " + attackingCityName + " y " + defendingCityName
                        + " ha sido cancelado.";
            default:
                return ChatColor.GRAY + "El asedio entre " + attackingCityName + " y " + defendingCityName
                        + " ha terminado.";
        }
    }

    private City getCityByName(String name) {
        Collection<City> cities = cityManager.getAllCities();
        for (City city : cities) {
            if (city.getName().equalsIgnoreCase(name)) {
                return city;
            }
        }
        return null;
    }

    private String formatSiegeTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
    }

    private void displaySiegeDetails(CommandSender sender, SiegeFlag siegeFlag) {
        City attackingCity = cityManager.getCityById(siegeFlag.getAttackingCityId());
        City defendingCity = cityManager.getCityById(siegeFlag.getDefendingCityId());

        if (attackingCity == null || defendingCity == null) {
            return;
        }

        String timeRemaining = formatSiegeTime(siegeFlag.getRemainingTimeSeconds());
        Player attacker = Bukkit.getPlayer(siegeFlag.getAttackerPlayerId());
        String attackerName = (attacker != null) ? attacker.getName() : "Desconocido";

        sender.sendMessage(ChatColor.YELLOW + "=== Información del Asedio ===");
        sender.sendMessage(ChatColor.GRAY + "Atacante: " + ChatColor.RED + attackingCity.getName());
        sender.sendMessage(ChatColor.GRAY + "Defensor: " + ChatColor.BLUE + defendingCity.getName());
        sender.sendMessage(ChatColor.GRAY + "Iniciado por: " + ChatColor.WHITE + attackerName);
        sender.sendMessage(ChatColor.GRAY + "Tiempo restante: " + ChatColor.YELLOW + timeRemaining);
        sender.sendMessage(ChatColor.GRAY + "Estado: " + ChatColor.WHITE + siegeFlag.getState().name());

        if (siegeFlag.isProtectorDefeated()) {
            sender.sendMessage(ChatColor.RED + "¡Protector derrotado!");
        }

        if (siegeFlag.isFlagCaptured()) {
            sender.sendMessage(ChatColor.RED + "¡Bandera capturada! Fase de saqueo activa.");
        }
    }

    private void sendSiegeHelp(CommandSender sender) {
        sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "=== Comandos de Asedio ===");
        sender.sendMessage(ChatColor.GRAY + "/siege start <ciudad> - Iniciar asedio contra una ciudad");
        sender.sendMessage(ChatColor.GRAY + "/siege info [ciudad] - Ver información de asedios");
        sender.sendMessage(ChatColor.GRAY + "/siege list - Listar todos los asedios activos");
        sender.sendMessage(ChatColor.GRAY + "/siege cooldown <ciudad1> <ciudad2> - Ver cooldown entre ciudades");

        if (sender.hasPermission("citywars.siege.admin")) {
            sender.sendMessage(ChatColor.GOLD + "=== Comandos de Admin ===");
            sender.sendMessage(ChatColor.GRAY + "/siege cancel <ciudad> - Cancelar asedio");
            sender.sendMessage(ChatColor.GRAY + "/siege give [jugador] - Dar estandarte de asedio");
        }
    }

    private boolean hasPermission(CommandSender sender, String subCommand) {
        String permission = "citywars.siege." + subCommand;
        return sender.hasPermission(permission) || sender.hasPermission("citywars.siege.*")
                || sender.hasPermission("citywars.*");
    }

    private boolean canManageCitySieges(Player player, City city) {
        return city.getOwnerUUID().equals(player.getUniqueId()) ||
                city.isAdmin(player.getUniqueId()) ||
                player.hasPermission("citywars.siege.admin");
    }

    // === MÉTODOS PÚBLICOS PARA OTROS SISTEMAS ===

    public Map<UUID, SiegeFlag> getActiveSieges() {
        return new HashMap<>(activeSieges);
    }

    public SiegeFlag getSiegeByCity(UUID cityId) {
        for (SiegeFlag siege : activeSieges.values()) {
            if (siege.getAttackingCityId().equals(cityId) || siege.getDefendingCityId().equals(cityId)) {
                return siege;
            }
        }
        return null;
    }

    public void addActiveSiege(SiegeFlag siegeFlag) {
        activeSieges.put(siegeFlag.getId(), siegeFlag);
    }

    public boolean removeSiege(UUID siegeId) {
        return activeSieges.remove(siegeId) != null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender arg0, @NotNull Command arg1,
            @NotNull String arg2, @NotNull String @NotNull [] arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onTabComplete'");
    }
}
