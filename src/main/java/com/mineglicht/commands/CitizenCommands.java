package com.mineglicht.commands;

import com.mineglicht.cityWars;
import com.mineglicht.config.Messages;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.EconomyManager;
import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import com.mineglicht.util.MessageUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CitizenCommands implements CommandExecutor, TabCompleter {

    private final cityWars plugin;
    private final CitizenManager citizenManager;
    private final CityManager cityManager;
    private final EconomyManager economyManager;

    public CitizenCommands(cityWars plugin) {
        this.plugin = plugin;
        this.citizenManager = plugin.getCitizenManager();
        this.cityManager = plugin.getCityManager();
        this.economyManager = plugin.getEconomyManager();
        
        plugin.getCommand("citizen").setExecutor(this);
        plugin.getCommand("citizen").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(player);
                break;
            case "info":
                showCitizenInfo(player);
                break;
            case "leave":
                leaveCityCommand(player);
                break;
            case "join":
                if (args.length < 2) {
                    MessageUtils.sendMessage(player, "&cUsage: /citizen join <city>");
                    return true;
                }
                joinCityCommand(player, args[1]);
                break;
            case "list":
                if (args.length < 2) {
                    MessageUtils.sendMessage(player, "&cUsage: /citizen list <city>");
                    return true;
                }
                listCitizensCommand(player, args[1]);
                break;
            case "pay":
                if (args.length < 3) {
                    MessageUtils.sendMessage(player, "&cUsage: /citizen pay <player> <amount>");
                    return true;
                }
                payCommand(player, args[1], args[2]);
                break;
            case "balance":
                showBalance(player);
                break;
            default:
                MessageUtils.sendMessage(player, Messages.INVALID_ARGUMENTS);
                break;
        }

        return true;
    }

    private void sendHelpMessage(Player player) {
        MessageUtils.sendMessage(player, "&6=== &eCitizenWars Citizen Commands &6===");
        MessageUtils.sendMessage(player, "&e/citizen info &7- Ver información de tu ciudadanía");
        MessageUtils.sendMessage(player, "&e/citizen join <ciudad> &7- Unirse a una ciudad");
        MessageUtils.sendMessage(player, "&e/citizen leave &7- Abandonar tu ciudad actual");
        MessageUtils.sendMessage(player, "&e/citizen list <ciudad> &7- Listar ciudadanos de una ciudad");
        MessageUtils.sendMessage(player, "&e/citizen pay <jugador> <cantidad> &7- Pagar a otro jugador");
        MessageUtils.sendMessage(player, "&e/citizen balance &7- Ver tu balance económico");
    }

    private void showCitizenInfo(Player player) {
        Citizen citizen = citizenManager.getCitizenByPlayer(player);
        
        if (citizen == null) {
            MessageUtils.sendMessage(player, Messages.PLAYER_NOT_CITIZEN);
            return;
        }
        
        City city = citizen.getCity();
        double taxPercentage = plugin.getConfig().getDouble("tax.daily_percentage", 18.0);
        
        MessageUtils.sendMessage(player, "&6=== &eTu Información de Ciudadano &6===");
        MessageUtils.sendMessage(player, "&7Ciudad: &e" + city.getName());
        MessageUtils.sendMessage(player, "&7Fecha de Ingreso: &e" + citizen.getJoinDate());
        MessageUtils.sendMessage(player, "&7Impuesto Diario: &e" + taxPercentage + "%");
        
        double balance = economyManager.getBalance(player);
        double taxAmount = balance * (taxPercentage / 100);
        
        MessageUtils.sendMessage(player, "&7Tu Balance: &e" + economyManager.formatBalance(balance));
        MessageUtils.sendMessage(player, "&7Próximo Impuesto: &e" + economyManager.formatBalance(taxAmount));
        
        if (citizen.hasRole("owner")) {
            MessageUtils.sendMessage(player, "&7Rol: &cPropietario");
        } else if (citizen.hasRole("admin")) {
            MessageUtils.sendMessage(player, "&7Rol: &6Administrador");
        } else {
            MessageUtils.sendMessage(player, "&7Rol: &eCiudadano");
        }
    }

    private void leaveCityCommand(Player player) {
        Citizen citizen = citizenManager.getCitizenByPlayer(player);
        
        if (citizen == null) {
            MessageUtils.sendMessage(player, Messages.NOT_A_CITIZEN);
            return;
        }
        
        City city = citizen.getCity();
        
        if (citizen.hasRole("owner")) {
            MessageUtils.sendMessage(player, Messages.OWNER_CANNOT_LEAVE);
            return;
        }
        
        boolean success = citizenManager.removeCitizen(citizen);
        
        if (success) {
            MessageUtils.sendMessage(player, Messages.LEFT_CITY.replace("%city%", city.getName()));
            
            // Notificar a los miembros de la ciudad
            for (Citizen otherCitizen : citizenManager.getCitizensInCity(city)) {
                Player otherPlayer = Bukkit.getPlayer(otherCitizen.getUuid());
                if (otherPlayer != null && otherPlayer.isOnline()) {
                    MessageUtils.sendMessage(otherPlayer, Messages.PLAYER_LEFT_CITY
                            .replace("%player%", player.getName()));
                }
            }
        } else {
            MessageUtils.sendMessage(player, Messages.FAILED_TO_LEAVE_CITY);
        }
    }

    private void joinCityCommand(Player player, String cityName) {
        Citizen existingCitizen = citizenManager.getCitizenByPlayer(player);
        
        if (existingCitizen != null) {
            MessageUtils.sendMessage(player, Messages.ALREADY_IN_CITY
                    .replace("%city%", existingCitizen.getCity().getName()));
            return;
        }
        
        City city = cityManager.getCityByName(cityName);
        
        if (city == null) {
            MessageUtils.sendMessage(player, Messages.CITY_NOT_FOUND);
            return;
        }
        
        // Verificar si la ciudad tiene habilitado el auto-ingreso
        if (!city.getAllowsAutoJoin()) {
            MessageUtils.sendMessage(player, Messages.CITY_NO_AUTO_JOIN);
            return;
        }
        
        boolean success = citizenManager.addCitizen(player, city);
        
        if (success) {
            MessageUtils.sendMessage(player, Messages.JOINED_CITY.replace("%city%", city.getName()));
            
            // Notificar a los miembros de la ciudad
            for (Citizen otherCitizen : citizenManager.getCitizensInCity(city)) {
                if (!otherCitizen.getUuid().equals(player.getUniqueId())) {
                    Player otherPlayer = Bukkit.getPlayer(otherCitizen.getUuid());
                    if (otherPlayer != null && otherPlayer.isOnline()) {
                        MessageUtils.sendMessage(otherPlayer, Messages.PLAYER_JOINED_CITY
                                .replace("%player%", player.getName()));
                    }
                }
            }
        } else {
            MessageUtils.sendMessage(player, Messages.FAILED_TO_JOIN_CITY);
        }
    }

    private void listCitizensCommand(Player player, String cityName) {
        City city = cityManager.getCityByName(cityName);
        
        if (city == null) {
            MessageUtils.sendMessage(player, Messages.CITY_NOT_FOUND);
            return;
        }
        
        List<Citizen> citizens = citizenManager.getCitizensInCity(city);
        
        if (citizens.isEmpty()) {
            MessageUtils.sendMessage(player, Messages.NO_CITIZENS_FOUND);
            return;
        }
        
        MessageUtils.sendMessage(player, "&6=== &eCiudadanos de " + city.getName() + " &6===");
        
        // Mostrar propietarios primero
        List<Citizen> owners = citizens.stream()
                .filter(c -> c.hasRole("owner"))
                .collect(Collectors.toList());
        
        if (!owners.isEmpty()) {
            MessageUtils.sendMessage(player, "&c=== Propietarios ===");
            for (Citizen owner : owners) {
                boolean online = Bukkit.getPlayer(owner.getUuid()) != null;
                String status = online ? "&aOnline" : "&cOffline";
                MessageUtils.sendMessage(player, "&7- &e" + owner.getName() + " " + status);
            }
        }
        
        // Luego administradores
        List<Citizen> admins = citizens.stream()
                .filter(c -> c.hasRole("admin") && !c.hasRole("owner"))
                .collect(Collectors.toList());
        
        if (!admins.isEmpty()) {
            MessageUtils.sendMessage(player, "&6=== Administradores ===");
            for (Citizen admin : admins) {
                boolean online = Bukkit.getPlayer(admin.getUuid()) != null;
                String status = online ? "&aOnline" : "&cOffline";
                MessageUtils.sendMessage(player, "&7- &e" + admin.getName() + " " + status);
            }
        }
        
        // Finalmente ciudadanos normales
        List<Citizen> normalCitizens = citizens.stream()
                .filter(c -> !c.hasRole("owner") && !c.hasRole("admin"))
                .collect(Collectors.toList());
        
        if (!normalCitizens.isEmpty()) {
            MessageUtils.sendMessage(player, "&e=== Ciudadanos ===");
            for (Citizen normalCitizen : normalCitizens) {
                boolean online = Bukkit.getPlayer(normalCitizen.getUuid()) != null;
                String status = online ? "&aOnline" : "&cOffline";
                MessageUtils.sendMessage(player, "&7- &e" + normalCitizen.getName() + " " + status);
            }
        }
        
        MessageUtils.sendMessage(player, "&6Total: &e" + citizens.size() + " ciudadanos");
    }

    private void payCommand(Player player, String targetName, String amountStr) {
        Citizen payerCitizen = citizenManager.getCitizenByPlayer(player);
        
        if (payerCitizen == null) {
            MessageUtils.sendMessage(player, Messages.NOT_A_CITIZEN);
            return;
        }
        
        // Verificar que el jugador objetivo existe
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            MessageUtils.sendMessage(player, Messages.PLAYER_NOT_FOUND);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                MessageUtils.sendMessage(player, Messages.INVALID_AMOUNT);
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(player, Messages.INVALID_AMOUNT);
            return;
        }
        
        // Verificar que el pagador tiene suficiente dinero
        if (!economyManager.hasSufficientFunds(player, amount)) {
            MessageUtils.sendMessage(player, Messages.INSUFFICIENT_FUNDS);
            return;
        }
        
        // Realizar la transferencia
        boolean success = economyManager.transferMoney(player, targetPlayer, amount);
        
        if (success) {
            // Notificar a ambos jugadores
            MessageUtils.sendMessage(player, Messages.MONEY_SENT
                    .replace("%amount%", economyManager.formatBalance(amount))
                    .replace("%player%", targetPlayer.getName()));
            
            MessageUtils.sendMessage(targetPlayer, Messages.MONEY_RECEIVED
                    .replace("%amount%", economyManager.formatBalance(amount))
                    .replace("%player%", player.getName()));
        } else {
            MessageUtils.sendMessage(player, Messages.TRANSACTION_FAILED);
        }
    }

    private void showBalance(Player player) {
        double balance = economyManager.getBalance(player);
        MessageUtils.sendMessage(player, Messages.BALANCE_INFO.replace("%balance%", economyManager.formatBalance(balance)));
        
        Citizen citizen = citizenManager.getCitizenByPlayer(player);
        if (citizen != null) {
            double taxPercentage = plugin.getConfig().getDouble("tax.daily_percentage", 18.0);
            double nextTaxAmount = balance * (taxPercentage / 100);
            
            MessageUtils.sendMessage(player, Messages.NEXT_TAX_INFO
                    .replace("%amount%", economyManager.formatBalance(nextTaxAmount))
                    .replace("%percentage%", String.valueOf(taxPercentage)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Completar el primer argumento
            completions.addAll(Arrays.asList("help", "info", "join", "leave", "list", "pay", "balance"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                case "list":
                    // Completar con nombres de ciudades
                    completions.addAll(cityManager.getAllCities().stream()
                            .map(City::getName)
                            .collect(Collectors.toList()));
                    break;
                case "pay":
                    // Completar con nombres de jugadores online
                    Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                    break;
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
