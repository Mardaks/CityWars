package com.mineglicht.commands;

import com.mineglicht.cityWars;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.EconomyManager;
import com.mineglicht.manager.RegionManager;
import com.mineglicht.models.City;
import com.mineglicht.models.CityFlag;
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
import java.util.stream.Collectors;

public class CityCommands implements CommandExecutor, TabCompleter {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final EconomyManager economyManager;
    private final RegionManager regionManager;

    // Subcomandos disponibles
    private static final Map<String, String> SUBCOMMANDS = new HashMap<String, String>() {
        {
            put("create", "citywars.city.create");
            put("delete", "citywars.city.delete");
            put("join", "citywars.city.join");
            put("leave", "citywars.city.leave");
            put("invite", "citywars.city.invite");
            put("kick", "citywars.city.kick");
            put("info", "citywars.city.info");
            put("list", "citywars.city.list");
            put("expand", "citywars.city.expand");
            put("flag", "citywars.city.flag");
            put("admin", "citywars.city.admin");
            put("bank", "citywars.city.bank");
            put("tp", "citywars.city.teleport");
            put("help", "citywars.city.help");
        }
    };

    public CityCommands(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.economyManager = plugin.getEconomyManager();
        this.regionManager = plugin.getRegionManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Verificar permisos
        if (!hasPermission(sender, subCommand)) {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return true;
        }

        switch (subCommand) {
            case "create":
                return handleCreateCity(sender, args);
            case "delete":
                return handleDeleteCity(sender, args);
            case "join":
                return handleJoinCity(sender, args);
            case "leave":
                return handleLeaveCity(sender, args);
            case "invite":
                return handleInvitePlayer(sender, args);
            case "kick":
                return handleKickPlayer(sender, args);
            case "info":
                return handleCityInfo(sender, args);
            case "list":
                return handleCityList(sender, args);
            case "expand":
                return handleExpandCity(sender, args);
            case "flag":
                return handleCityFlag(sender, args);
            case "admin":
                return handleCityAdmin(sender, args);
            case "bank":
                return handleCityBank(sender, args);
            case "tp":
                return handleCityTeleport(sender, args);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage(Messages.PREFIX + ChatColor.RED + "Subcomando desconocido. Usa /city help");
                return true;
        }
    }

    private boolean handleCreateCity(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city create <nombre>");
            return true;
        }

        String cityName = args[1];

        // Verificar si el jugador ya está en una ciudad
        if (citizenManager.isInCity(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Ya perteneces a una ciudad. Debes salir primero.");
            return true;
        }

        // Verificar si ya existe una ciudad con ese nombre
        for (City city : cityManager.getAllCities()) {
            if (city.getName().equalsIgnoreCase(cityName)) {
                player.sendMessage(Messages.PREFIX + Messages.CITY_ALREADY_EXISTS);
                return true;
            }
        }

        try {
            City newCity = cityManager.createCity(player, cityName, player.getLocation());
            if (newCity != null) {
                player.sendMessage(Messages.PREFIX + Messages.CITY_CREATED.replace("{city}", cityName));
            } else {
                player.sendMessage(Messages.PREFIX + Messages.CITY_CREATION_FAILED);
            }
        } catch (Exception e) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_CREATION_FAILED);
            plugin.getLogger().severe("Error creando ciudad: " + e.getMessage());
        }

        return true;
    }

    private boolean handleDeleteCity(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // Solo el owner puede eliminar la ciudad
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo el fundador puede eliminar la ciudad.");
            return true;
        }

        // Verificar si la ciudad está bajo asedio
        if (playerCity.isUnderSiege()) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_UNDER_SIEGE);
            return true;
        }

        boolean success = cityManager.deleteCity(playerCity);
        if (success) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_DELETED.replace("{city}", playerCity.getName()));
        } else {
            player.sendMessage(Messages.PREFIX + Messages.CITY_DELETION_FAILED);
        }

        return true;
    }

    private boolean handleJoinCity(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city join <nombre>");
            return true;
        }

        String cityName = args[1];

        // Verificar si ya está en una ciudad
        if (citizenManager.isInCity(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_ALREADY_MEMBER);
            return true;
        }

        // Buscar la ciudad
        City targetCity = null;
        for (City city : cityManager.getAllCities()) {
            if (city.getName().equalsIgnoreCase(cityName)) {
                targetCity = city;
                break;
            }
        }

        if (targetCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_NOT_FOUND);
            return true;
        }

        boolean success = citizenManager.addCitizen(player.getUniqueId(), targetCity);
        if (success) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_JOIN_SUCCESS.replace("{city}", cityName));
        } else {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "No se pudo unir a la ciudad.");
        }

        return true;
    }

    private boolean handleLeaveCity(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // El owner no puede abandonar su ciudad
        if (playerCity.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED
                    + "El fundador no puede abandonar la ciudad. Debes eliminarla o transferir el liderazgo.");
            return true;
        }

        boolean success = citizenManager.removeCitizen(player.getUniqueId());
        if (success) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_LEAVE_SUCCESS.replace("{city}", playerCity.getName()));
        } else {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al abandonar la ciudad.");
        }

        return true;
    }

    private boolean handleInvitePlayer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city invite <jugador>");
            return true;
        }

        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // Verificar permisos (owner o admin)
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId()) && !playerCity.isAdmin(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo los administradores pueden invitar jugadores.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (citizenManager.isInCity(targetPlayer.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "El jugador ya pertenece a una ciudad.");
            return true;
        }

        boolean success = citizenManager.addCitizen(targetPlayer.getUniqueId(), playerCity);
        if (success) {
            player.sendMessage(
                    Messages.PREFIX + ChatColor.GREEN + "Jugador " + targetPlayer.getName() + " añadido a la ciudad.");
            targetPlayer.sendMessage(
                    Messages.PREFIX + ChatColor.GREEN + "Has sido añadido a la ciudad " + playerCity.getName());
        } else {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al añadir el jugador a la ciudad.");
        }

        return true;
    }

    private boolean handleKickPlayer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city kick <jugador>");
            return true;
        }

        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // Verificar permisos
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId()) && !playerCity.isAdmin(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo los administradores pueden expulsar jugadores.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        UUID targetUUID = targetPlayer != null ? targetPlayer.getUniqueId()
                : Bukkit.getOfflinePlayer(args[1]).getUniqueId();

        if (!citizenManager.isInCity(targetUUID, playerCity.getId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "El jugador no pertenece a tu ciudad.");
            return true;
        }

        // No se puede expulsar al owner
        if (playerCity.getOwnerUUID().equals(targetUUID)) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "No puedes expulsar al fundador de la ciudad.");
            return true;
        }

        boolean success = citizenManager.removeCitizen(targetUUID);
        if (success) {
            player.sendMessage(Messages.PREFIX + ChatColor.GREEN + "Jugador " + args[1] + " expulsado de la ciudad.");
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(
                        Messages.PREFIX + ChatColor.RED + "Has sido expulsado de la ciudad " + playerCity.getName());
            }
        } else {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al expulsar al jugador.");
        }

        return true;
    }

    private boolean handleCityInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;
        City targetCity;

        if (args.length >= 2) {
            // Mostrar info de ciudad específica
            String cityName = args[1];
            targetCity = null;
            for (City city : cityManager.getAllCities()) {
                if (city.getName().equalsIgnoreCase(cityName)) {
                    targetCity = city;
                    break;
                }
            }

            if (targetCity == null) {
                player.sendMessage(Messages.PREFIX + Messages.CITY_NOT_FOUND);
                return true;
            }
        } else {
            // Mostrar info de la ciudad del jugador
            targetCity = citizenManager.getPlayerCity(player.getUniqueId());
            if (targetCity == null) {
                player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
                return true;
            }
        }

        sendCityInfo(player, targetCity);
        return true;
    }

    private boolean handleCityList(CommandSender sender, String[] args) {
        Collection<City> cities = cityManager.getAllCities();

        if (cities.isEmpty()) {
            sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "No hay ciudades creadas.");
            return true;
        }

        sender.sendMessage(Messages.PREFIX + ChatColor.GOLD + "=== Lista de Ciudades ===");
        for (City city : cities) {
            int onlineCitizens = citizenManager.getOnlineCitizensInCity(city.getId()).size();
            String status = city.isUnderSiege() ? ChatColor.RED + "[ASEDIO]" : ChatColor.GREEN + "[SEGURA]";

            sender.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + city.getName() +
                    ChatColor.GRAY + " (" + city.getCitizenCount() + " ciudadanos, " +
                    onlineCitizens + " online) " + status);
        }

        return true;
    }

    private boolean handleExpandCity(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city expand <bloques>");
            return true;
        }

        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // Verificar permisos
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId()) && !playerCity.isAdmin(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo los administradores pueden expandir la ciudad.");
            return true;
        }

        int blocks;
        try {
            blocks = Integer.parseInt(args[1]);
            if (blocks <= 0) {
                player.sendMessage(Messages.PREFIX + ChatColor.RED + "El número de bloques debe ser positivo.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Número de bloques inválido.");
            return true;
        }

        boolean success = cityManager.expandCity(playerCity, player, blocks);
        if (success) {
            player.sendMessage(Messages.PREFIX + Messages.CITY_EXPANDED.replace("{blocks}", String.valueOf(blocks)));
        } else {
            player.sendMessage(Messages.PREFIX + Messages.CITY_EXPANSION_FAILED);
        }

        return true;
    }

    private boolean handleCityFlag(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city flag <flag> [true/false]");
            player.sendMessage(Messages.PREFIX + ChatColor.GRAY + "Flags disponibles: " +
                    Arrays.stream(CityFlag.values()).map(Enum::name).collect(Collectors.joining(", ")));
            return true;
        }

        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // Verificar permisos
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId()) && !playerCity.isAdmin(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo los administradores pueden cambiar las flags.");
            return true;
        }

        CityFlag flag;
        try {
            flag = CityFlag.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Flag desconocida. Flags disponibles: " +
                    Arrays.stream(CityFlag.values()).map(Enum::name).collect(Collectors.joining(", ")));
            return true;
        }

        if (args.length >= 3) {
            boolean value = Boolean.parseBoolean(args[2]);
            cityManager.setCityFlag(playerCity, flag, value);
            player.sendMessage(Messages.PREFIX + ChatColor.GREEN + "Flag " + flag.name() + " establecida a: " + value);
        } else {
            boolean currentValue = playerCity.hasFlag(flag);
            player.sendMessage(
                    Messages.PREFIX + ChatColor.YELLOW + "Flag " + flag.name() + " está en: " + currentValue);
        }

        return true;
    }

    private boolean handleCityAdmin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city admin <add/remove> <jugador>");
            return true;
        }

        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        // Solo el owner puede gestionar admins
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo el fundador puede gestionar administradores.");
            return true;
        }

        String action = args[1].toLowerCase();
        Player targetPlayer = Bukkit.getPlayer(args[2]);
        UUID targetUUID = targetPlayer != null ? targetPlayer.getUniqueId()
                : Bukkit.getOfflinePlayer(args[2]).getUniqueId();

        if (!citizenManager.isInCity(targetUUID, playerCity.getId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "El jugador no pertenece a tu ciudad.");
            return true;
        }

        boolean success = false;
        switch (action) {
            case "add":
                success = playerCity.addAdmin(targetUUID);
                if (success) {
                    player.sendMessage(Messages.PREFIX + ChatColor.GREEN + args[2] + " es ahora administrador.");
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(Messages.PREFIX + ChatColor.GREEN + "Eres ahora administrador de "
                                + playerCity.getName());
                    }
                }
                break;
            case "remove":
                success = playerCity.removeAdmin(targetUUID);
                if (success) {
                    player.sendMessage(Messages.PREFIX + ChatColor.GREEN + args[2] + " ya no es administrador.");
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "Ya no eres administrador de "
                                + playerCity.getName());
                    }
                }
                break;
            default:
                player.sendMessage(Messages.PREFIX + ChatColor.RED + "Acción inválida. Usa 'add' o 'remove'.");
                return true;
        }

        if (!success) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al ejecutar la acción.");
        }

        return true;
    }

    private boolean handleCityBank(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        if (args.length < 2) {
            // Mostrar balance del banco
            double balance = economyManager.getCityBankBalance(playerCity);
            player.sendMessage(Messages.PREFIX + ChatColor.GOLD + "Balance del banco: " + ChatColor.WHITE + balance);
            return true;
        }

        String action = args[1].toLowerCase();

        // Verificar permisos para operaciones bancarias
        if (!playerCity.getOwnerUUID().equals(player.getUniqueId()) && !playerCity.isAdmin(player.getUniqueId())) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Solo los administradores pueden gestionar el banco.");
            return true;
        }

        switch (action) {
            case "deposit":
                if (args.length < 3) {
                    player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city bank deposit <cantidad>");
                    return true;
                }
                return handleBankDeposit(player, playerCity, args[2]);

            case "withdraw":
                if (args.length < 3) {
                    player.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city bank withdraw <cantidad>");
                    return true;
                }
                return handleBankWithdraw(player, playerCity, args[2]);

            default:
                player.sendMessage(Messages.PREFIX + ChatColor.RED + "Acciones disponibles: deposit, withdraw");
                return true;
        }
    }

    private boolean handleCityTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PREFIX + Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            player.sendMessage(Messages.PREFIX + Messages.PLAYER_NOT_CITIZEN);
            return true;
        }

        if (playerCity.getCenter() != null) {
            player.teleport(playerCity.getCenter());
            player.sendMessage(
                    Messages.PREFIX + ChatColor.GREEN + "Teletransportado al centro de " + playerCity.getName());
        } else {
            player.sendMessage(
                    Messages.PREFIX + ChatColor.RED + "La ciudad no tiene un punto de teletransporte establecido.");
        }

        return true;
    }

    // Métodos auxiliares

    private boolean hasPermission(CommandSender sender, String subCommand) {
        String permission = SUBCOMMANDS.get(subCommand);
        return permission == null || sender.hasPermission(permission);
    }

    private void sendCityInfo(Player player, City city) {
        player.sendMessage(Messages.PREFIX + Messages.CITY_INFO_HEADER.replace("{city}", city.getName()));

        String owner = Bukkit.getOfflinePlayer(city.getOwnerUUID()).getName();
        int totalCitizens = city.getCitizenCount();
        int onlineCitizens = citizenManager.getOnlineCitizensInCity(city.getId()).size();
        double bankBalance = economyManager.getCityBankBalance(city);
        String status = city.isUnderSiege() ? ChatColor.RED + "BAJO ASEDIO" : ChatColor.GREEN + "SEGURA";

        for (String line : Messages.CITY_INFO_FORMAT) {
            String formattedLine = line
                    .replace("{owner}", owner)
                    .replace("{citizens}", String.valueOf(totalCitizens))
                    .replace("{online}", String.valueOf(onlineCitizens))
                    .replace("{level}", String.valueOf(city.getLevel()))
                    .replace("{bank}", String.valueOf(bankBalance))
                    .replace("{status}", status);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedLine));
        }
    }

    private boolean handleBankDeposit(Player player, City city, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                player.sendMessage(Messages.PREFIX + ChatColor.RED + "La cantidad debe ser positiva.");
                return true;
            }

            if (!economyManager.hasCurrency(player, "default", amount)) {
                player.sendMessage(Messages.PREFIX + Messages.CITY_INSUFFICIENT_FUNDS);
                return true;
            }

            boolean success = economyManager.withdrawCurrency(player, "default", amount) &&
                    economyManager.depositCityBank(city, amount);

            if (success) {
                player.sendMessage(
                        Messages.PREFIX + ChatColor.GREEN + "Depositado " + amount + " al banco de la ciudad.");
            } else {
                player.sendMessage(Messages.PREFIX + ChatColor.RED + "Error al realizar el depósito.");
            }

        } catch (NumberFormatException e) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Cantidad inválida.");
        }

        return true;
    }

    private boolean handleBankWithdraw(Player player, City city, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                player.sendMessage(Messages.PREFIX + ChatColor.RED + "La cantidad debe ser mayor a 0.");
                return true;
            }

            if (economyManager.withdrawCityBank(city, amount)) {
                // Usar la moneda predeterminada del servidor para el depósito
                String defaultCurrency = plugin.getConfig().getString("economy.default-currency", "glitchcoins");
                if (economyManager.depositCurrency(player, defaultCurrency, amount)) {
                    player.sendMessage(Messages.PREFIX + ChatColor.GREEN +
                            "Has retirado $" + amount + " del banco de la ciudad.");
                    return true;
                } else {
                    // Revertir el retiro si no se pudo depositar al jugador
                    economyManager.depositCityBank(city, amount);
                    player.sendMessage(Messages.PREFIX + ChatColor.RED +
                            "Error al depositar el dinero en tu cuenta.");
                }
            } else {
                player.sendMessage(Messages.PREFIX + ChatColor.RED +
                        "No hay suficientes fondos en el banco de la ciudad.");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.PREFIX + ChatColor.RED + "Cantidad inválida.");
        }
        return true;
    }

    private boolean handleBankBalance(Player player, City city) {
        double balance = economyManager.getCityBankBalance(city);
        player.sendMessage(Messages.PREFIX + ChatColor.YELLOW +
                "Balance del banco de " + city.getName() + ": $" + balance);
        return true;
    }

    private boolean handleSetAdmin(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "setadmin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Messages.PREFIX + ChatColor.RED + "Uso: /city setadmin <ciudad> <jugador>");
            return true;
        }

        String cityName = args[1];
        String playerName = args[2];

        City city = getCityByName(cityName);
        Player newAdmin = Bukkit.getPlayer(playerName);

        if (city == null) {
            sender.sendMessage(Messages.CITY_NOT_FOUND);
            return true;
        }

        if (newAdmin == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        // Añadir al jugador como admin si no lo es ya
        if (!city.isAdmin(newAdmin.getUniqueId())) {
            city.addAdmin(newAdmin.getUniqueId());
            cityManager.saveCities();

            sender.sendMessage(Messages.PREFIX + ChatColor.GREEN +
                    "Has establecido a " + newAdmin.getName() + " como administrador de " + city.getName());
        } else {
            sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW +
                    newAdmin.getName() + " ya es administrador de " + city.getName());
        }

        return true;
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

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Messages.PREFIX + ChatColor.YELLOW + "=== Comandos de Ciudad ===");
        sender.sendMessage(ChatColor.GRAY + "/city create <nombre> - Crear una ciudad");
        sender.sendMessage(ChatColor.GRAY + "/city join <nombre> - Unirse a una ciudad");
        sender.sendMessage(ChatColor.GRAY + "/city leave - Abandonar tu ciudad");
        sender.sendMessage(ChatColor.GRAY + "/city invite <jugador> - Invitar a un jugador");
        sender.sendMessage(ChatColor.GRAY + "/city kick <jugador> - Expulsar a un jugador");
        sender.sendMessage(ChatColor.GRAY + "/city info [ciudad] - Ver información de ciudad");
        sender.sendMessage(ChatColor.GRAY + "/city list - Listar todas las ciudades");
        sender.sendMessage(ChatColor.GRAY + "/city expand <bloques> - Expandir ciudad");
        sender.sendMessage(ChatColor.GRAY + "/city flag <flag> <true/false> - Configurar flags");
        sender.sendMessage(ChatColor.GRAY + "/city admin <add/remove> <jugador> - Gestionar admins");
        sender.sendMessage(ChatColor.GRAY + "/city bank <deposit/withdraw/balance> [cantidad] - Gestionar banco");
        sender.sendMessage(ChatColor.GRAY + "/city tp - Teletransportarse al centro de la ciudad");

        if (sender.hasPermission("citywars.admin")) {
            sender.sendMessage(ChatColor.GOLD + "=== Comandos de Admin ===");
            sender.sendMessage(ChatColor.GRAY + "/city delete <ciudad> - Eliminar ciudad");
            sender.sendMessage(ChatColor.GRAY + "/city setadmin <ciudad> <jugador> - Establecer admin");
        }
    }

    private String formatTime(long seconds) {
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

    private boolean isValidCityName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Verificar longitud
        if (name.length() < 3 || name.length() > 16) {
            return false;
        }

        // Verificar caracteres válidos (solo letras, números y algunos símbolos)
        return name.matches("^[a-zA-Z0-9_-]+$");
    }

    private boolean canPlayerManageCity(Player player, City city) {
        return city.getOwnerUUID().equals(player.getUniqueId()) ||
                city.isAdmin(player.getUniqueId()) ||
                player.hasPermission("citywars.admin");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender arg0, @NotNull Command arg1,
            @NotNull String arg2, @NotNull String @NotNull [] arg3) {
        //Auto-generated method stub 
        throw new UnsupportedOperationException("Unimplemented method 'onTabComplete'");
    }
}