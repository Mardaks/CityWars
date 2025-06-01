package com.mineglicht.commands;

import com.mineglicht.cityWars;
import com.mineglicht.config.Messages;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.SiegeManager;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final cityWars plugin;
    private final CityManager cityManager;
    private final CitizenManager citizenManager;
    private final SiegeManager siegeManager;

    public AdminCommand(cityWars plugin) {
        this.plugin = plugin;
        this.cityManager = plugin.getCityManager();
        this.citizenManager = plugin.getCitizenManager();
        this.siegeManager = plugin.getSiegeManager();

        plugin.getCommand("cityadmin").setExecutor(this);
        plugin.getCommand("cityadmin").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("citywars.admin")) {
            MessageUtils.sendMessage(sender, Messages.NO_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                break;
            case "reload":
                reloadPlugin(sender);
                break;
            case "city":
                handleCityCommand(sender, args);
                break;
            case "citizen":
                handleCitizenCommand(sender, args);
                break;
            case "siege":
                handleSiegeCommand(sender, args);
                break;
            default:
                MessageUtils.sendMessage(sender, Messages.INVALID_ARGUMENTS);
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&6=== &eCityWars Admin Commands &6===");
        MessageUtils.sendMessage(sender, "&e/cityadmin reload &7- Reload the plugin configuration");
        MessageUtils.sendMessage(sender, "&e/cityadmin city create <name> &7- Create a new city");
        MessageUtils.sendMessage(sender, "&e/cityadmin city delete <name> &7- Delete a city");
        MessageUtils.sendMessage(sender, "&e/cityadmin city list &7- List all cities");
        MessageUtils.sendMessage(sender, "&e/cityadmin citizen add <player> <city> &7- Add player to city");
        MessageUtils.sendMessage(sender, "&e/cityadmin citizen remove <player> &7- Remove player from city");
        MessageUtils.sendMessage(sender, "&e/cityadmin citizen list <city> &7- List all citizens of a city");
        MessageUtils.sendMessage(sender, "&e/cityadmin siege stop <city> &7- Stop an active siege on a city");
        MessageUtils.sendMessage(sender, "&e/cityadmin siege cooldown <city> <minutes> &7- Set siege cooldown");
    }

    private void reloadPlugin(CommandSender sender) {
        plugin.reloadConfig();
        MessageUtils.sendMessage(sender, Messages.CONFIG_RELOADED);
    }

    private void handleCityCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /cityadmin city <create|delete|list>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create":
                if (args.length < 3) {
                    MessageUtils.sendMessage(sender, "&cUsage: /cityadmin city create <name>");
                    return;
                }
                createCity(sender, args[2]);
                break;
            case "delete":
                if (args.length < 3) {
                    MessageUtils.sendMessage(sender, "&cUsage: /cityadmin city delete <name>");
                    return;
                }
                deleteCity(sender, args[2]);
                break;
            case "list":
                listCities(sender);
                break;
            default:
                MessageUtils.sendMessage(sender, "&cUnknown city command. Use /cityadmin help for help.");
                break;
        }
    }

    private void createCity(CommandSender sender, String cityName) {
        if (cityManager.getCityByName(cityName) != null) {
            MessageUtils.sendMessage(sender, Messages.CITY_ALREADY_EXISTS);
            return;
        }

        // Si el sender es un jugador, usa su ubicación
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // CORRECCIÓN: Pasar los 3 parámetros en el orden correcto
            City newCity = cityManager.createCity(player, cityName, player.getLocation());

            if (newCity != null) {
                MessageUtils.sendMessage(sender, Messages.CITY_CREATED.replace("%city%", cityName));
            } else {
                MessageUtils.sendMessage(sender, Messages.CITY_CREATION_FAILED);
            }
        } else {
            MessageUtils.sendMessage(sender, "&cThis command must be executed by a player.");
        }
    }

    private void deleteCity(CommandSender sender, String cityName) {
        City city = cityManager.getCityByName(cityName);
        if (city == null) {
            MessageUtils.sendMessage(sender, Messages.CITY_NOT_FOUND);
            return;
        }

        // Verificar si la ciudad está bajo asedio
        if (city.getSiegeState() != SiegeState.NONE) {
            MessageUtils.sendMessage(sender, Messages.CITY_DELETION_FAILED);
            return;
        }

        boolean success = cityManager.deleteCity(city);
        if (success) {
            MessageUtils.sendMessage(sender, Messages.CITY_DELETED.replace("%city%", cityName));
        } else {
            MessageUtils.sendMessage(sender, Messages.CITY_DELETION_FAILED);
        }
    }

    private void listCities(CommandSender sender) {
        Collection<City> cities = cityManager.getAllCities();

        if (cities.isEmpty()) {
            MessageUtils.sendMessage(sender, Messages.CITY_NOT_FOUND);
            return;
        }

        MessageUtils.sendMessage(sender, "&6=== &eCities List &6===");
        for (City city : cities) {
            String status = city.getSiegeState() != SiegeState.NONE ? "&c(Under Siege)" : "&a(Peaceful)";

            // Cambiar city por city.getId() para obtener el UUID
            MessageUtils.sendMessage(sender, "&e" + city.getName() + " &7- Citizens: &f" +
                    citizenManager.getCitizensInCity(city.getId()).size() + " " + status);
        }
    }

    private void handleCitizenCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /cityadmin citizen <add|remove|list>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add":
                if (args.length < 4) {
                    MessageUtils.sendMessage(sender, "&cUsage: /cityadmin citizen add <player> <city>");
                    return;
                }
                addCitizen(sender, args[2], args[3]);
                break;
            case "remove":
                if (args.length < 3) {
                    MessageUtils.sendMessage(sender, "&cUsage: /cityadmin citizen remove <player>");
                    return;
                }
                removeCitizen(sender, args[2]);
                break;
            case "list":
                if (args.length < 3) {
                    MessageUtils.sendMessage(sender, "&cUsage: /cityadmin citizen list <city>");
                    return;
                }
                listCitizens(sender, args[2]);
                break;
            default:
                MessageUtils.sendMessage(sender, "&cUnknown citizen command. Use /cityadmin help for help.");
                break;
        }
    }

    private void addCitizen(CommandSender sender, String playerName, String cityName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            MessageUtils.sendMessage(sender, Messages.PLAYER_NOT_FOUND);
            return;
        }

        City city = cityManager.getCityByName(cityName);
        if (city == null) {
            MessageUtils.sendMessage(sender, Messages.CITY_NOT_FOUND);
            return;
        }

        if (citizenManager.getPlayerCity(player.getUniqueId()) != null) {
            MessageUtils.sendMessage(sender, Messages.CITIZEN_ERROR_REGISTERED);
            return;
        }

        boolean success = citizenManager.addCitizen(player.getUniqueId(), city);
        if (success) {
            MessageUtils.sendMessage(sender, Messages.CITIZEN_ADDED
                    .replace("%player%", playerName)
                    .replace("%city%", cityName));

            // Notificar al jugador
            MessageUtils.sendMessage(player, Messages.CITIZEN_ADDED.replace("%city%", cityName));
        } else {
            MessageUtils.sendMessage(sender, Messages.CITIZEN_ADD_FAILED);
        }
    }

    private void removeCitizen(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            MessageUtils.sendMessage(sender, Messages.PLAYER_NOT_FOUND);
            return;
        }

        Citizen citizen = citizenManager.getCitizen(player.getUniqueId()); // <-- CAMBIO AQUÍ
        if (citizen == null) {
            MessageUtils.sendMessage(sender, Messages.PLAYER_NOT_CITIZEN);
            return;
        }

        City city = cityManager.getCity(citizen.getCityId()); // <-- CAMBIO AQUÍ
        String cityName = city.getName();

        boolean success = citizenManager.removeCitizen(player.getUniqueId()); // <-- CAMBIO AQUÍ

        if (success) {
            MessageUtils.sendMessage(sender, Messages.CITIZEN_REMOVED
                    .replace("%player%", playerName)
                    .replace("%city%", cityName));

            // Notificar al jugador
            MessageUtils.sendMessage(player, Messages.CITIZEN_REMOVED.replace("%city%", cityName));
        } else {
            MessageUtils.sendMessage(sender, Messages.CITIZEN_REMOVE_FAILED);
        }
    }

    @SuppressWarnings("unused")
    private void listCitizens(CommandSender sender, String cityName) {
        City city = cityManager.getCityByName(cityName);
        if (city == null) {
            MessageUtils.sendMessage(sender, Messages.CITY_NOT_FOUND);
            return;
        }

        Set<UUID> citizenUuids = citizenManager.getCitizensInCity(city.getId()); // <-- CAMBIO AQUÍ
        if (citizenUuids.isEmpty()) {
            MessageUtils.sendMessage(sender, Messages.NO_CITIZENS);
            return;
        }

        MessageUtils.sendMessage(sender, "&6=== &eCitizens in " + city.getName() + " &6===");
        for (UUID citizenUuid : citizenUuids) {
            // Obtener el objeto Citizen por UUID
            Citizen citizen = citizenManager.getCitizen(citizenUuid);
            if (citizen == null)
                continue; // Por seguridad

            Player player = Bukkit.getPlayer(citizenUuid);
            String status = (player != null && player.isOnline()) ? "&aOnline" : "&cOffline";

            String playerName = (player != null) ? player.getName() : Bukkit.getOfflinePlayer(citizenUuid).getName();
        }
    }

    private void handleSiegeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /cityadmin siege <stop|cooldown>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "stop":
                if (args.length < 3) {
                    MessageUtils.sendMessage(sender, "&cUsage: /cityadmin siege stop <city>");
                    return;
                }
                stopSiege(sender, args[2]);
                break;
            case "cooldown":
                // CAMBIO AQUÍ: Ahora requiere 5 argumentos en total
                if (args.length < 5) {
                    MessageUtils.sendMessage(sender,
                            "&cUsage: /cityadmin siege cooldown <attackingCity> <defendingCity> <minutes>");
                    return;
                }
                // CAMBIO AQUÍ: Pasar 3 parámetros (attackingCity, defendingCity, minutes)
                setSiegeCooldown(sender, args[2], args[3], args[4]);
                break;
            default:
                MessageUtils.sendMessage(sender, "&cUnknown siege command. Use /cityadmin help for help.");
                break;
        }
    }

    private void stopSiege(CommandSender sender, String cityName) {
        City city = cityManager.getCityByName(cityName);
        if (city == null) {
            MessageUtils.sendMessage(sender, Messages.CITY_NOT_FOUND);
            return;
        }

        if (city.getSiegeState() == SiegeState.NONE) {
            MessageUtils.sendMessage(sender, Messages.CITY_NOT_UNDER_SIEGE);
            return;
        }

        UUID siegeFlagId = siegeManager.getSiegeFlagIdByCity(city.getId());
        if (siegeFlagId != null) {
            siegeManager.endSiege(siegeFlagId, SiegeState.CANCELLED);
            MessageUtils.sendMessage(sender, Messages.SIEGE_ENDED.replace("%city%", cityName));
        } else {
            MessageUtils.sendMessage(sender, Messages.SIEGE_ENDED);
        }
    }

    private void setSiegeCooldown(CommandSender sender, String attackingCityName, String defendingCityName,
            String minutesStr) {
        // Validar ciudad atacante
        City attackingCity = cityManager.getCityByName(attackingCityName);
        if (attackingCity == null) {
            MessageUtils.sendMessage(sender, "&cCiudad atacante no encontrada: " + attackingCityName);
            return;
        }

        // Validar ciudad defensora
        City defendingCity = cityManager.getCityByName(defendingCityName);
        if (defendingCity == null) {
            MessageUtils.sendMessage(sender, "&cCiudad defensora no encontrada: " + defendingCityName);
            return;
        }

        // Validar que no sean la misma ciudad
        if (attackingCity.getId().equals(defendingCity.getId())) {
            MessageUtils.sendMessage(sender, "&cUna ciudad no puede atacarse a sí misma.");
            return;
        }

        // Validar y parsear minutos
        int minutes;
        try {
            minutes = Integer.parseInt(minutesStr);
            if (minutes < 0) {
                MessageUtils.sendMessage(sender, "&cEl tiempo de cooldown debe ser positivo.");
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cFormato de número inválido. Ingresa un número válido.");
            return;
        }

        // ========================================
        // CORRECCIÓN: Usar setCooldown con las dos ciudades
        // ========================================
        siegeManager.setCooldown(attackingCity.getId(), defendingCity.getId());

        MessageUtils.sendMessage(sender, Messages.SIEGE_COOLDOWN_ACTIVE
                .replace("%city%", attackingCityName + " → " + defendingCityName)
                .replace("%time%", minutesStr));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("citywars.admin")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "reload", "city", "citizen", "siege"));
        } else if (args.length >= 2) {
            switch (args[0].toLowerCase()) {
                case "city":
                    if (args.length == 2) {
                        completions.addAll(Arrays.asList("create", "delete", "list"));
                    } else if (args.length == 3 && args[1].equalsIgnoreCase("delete")) {
                        completions.addAll(cityManager.getAllCities().stream()
                                .map(City::getName)
                                .collect(Collectors.toList()));
                    }
                    break;
                case "citizen":
                    if (args.length == 2) {
                        completions.addAll(Arrays.asList("add", "remove", "list"));
                    } else if (args.length == 3) {
                        if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                        } else if (args[1].equalsIgnoreCase("list")) {
                            completions.addAll(cityManager.getAllCities().stream()
                                    .map(City::getName)
                                    .collect(Collectors.toList()));
                        }
                    } else if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
                        completions.addAll(cityManager.getAllCities().stream()
                                .map(City::getName)
                                .collect(Collectors.toList()));
                    }
                    break;
                case "siege":
                    if (args.length == 2) {
                        completions.addAll(Arrays.asList("stop", "cooldown"));
                    } else if (args.length == 3) {
                        // Para ambos comandos (stop y cooldown), completar con nombres de ciudades
                        completions.addAll(cityManager.getAllCities().stream()
                                .map(City::getName)
                                .collect(Collectors.toList()));
                    } else if (args.length == 4 && args[1].equalsIgnoreCase("cooldown")) {
                        // CAMBIO AQUÍ: Para cooldown, el 4to argumento también es una ciudad
                        completions.addAll(cityManager.getAllCities().stream()
                                .map(City::getName)
                                .collect(Collectors.toList()));
                    } else if (args.length == 5 && args[1].equalsIgnoreCase("cooldown")) {
                        // CAMBIO AQUÍ: Para el 5to argumento (minutos), sugerir algunos valores comunes
                        completions.addAll(Arrays.asList("30", "60", "120", "180"));
                    }
                    break;
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
