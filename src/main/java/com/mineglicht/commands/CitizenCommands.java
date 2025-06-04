package com.mineglicht.commands;

import com.mineglicht.cityWars;
import com.mineglicht.config.Messages;
import com.mineglicht.manager.CityManager;
import com.mineglicht.manager.CitizenManager;
import com.mineglicht.manager.EconomyManager;
import com.mineglicht.models.City;
import com.mineglicht.util.MessageUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CitizenCommands implements CommandExecutor, TabCompleter {

    private final cityWars plugin;
    private final CitizenManager citizenManager;
    private final CityManager cityManager;
    private final EconomyManager economyManager;
    
    // Cache para los comandos válidos
    private static final Set<String> VALID_COMMANDS = Set.of("help", "info", "join", "leave", "list");
    private static final Set<String> CITY_REQUIRED_COMMANDS = Set.of("join", "list");

    // Constructor público (para compatibilidad)
    public CitizenCommands(cityWars plugin) {
        this.plugin = plugin;
        this.citizenManager = plugin.getCitizenManager();
        this.cityManager = plugin.getCityManager();
        this.economyManager = plugin.getEconomyManager();

        // NO registrar aquí el comando, se hace desde registerCommands()
        // plugin.getCommand("citizen").setExecutor(this);
        // plugin.getCommand("citizen").setTabCompleter(this);
    }

    /**
     * Inicializa el comando CitizenCommands con validaciones completas
     * 
     * @param plugin La instancia principal del plugin CityWars
     * @return La instancia de CitizenCommands creada o null si falla
     */
    public static CitizenCommands initialize(cityWars plugin) {
        // Verificar que el plugin no sea null
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin no puede ser null");
        }
        
        // Verificar que los managers estén inicializados
        if (plugin.getCitizenManager() == null) {
            plugin.getLogger().severe("CitizenManager no está inicializado");
            return null;
        }
        
        if (plugin.getCityManager() == null) {
            plugin.getLogger().severe("CityManager no está inicializado");
            return null;
        }
        
        if (plugin.getEconomyManager() == null) {
            plugin.getLogger().severe("EconomyManager no está inicializado");
            return null;
        }
        
        try {
            // Verificar que el comando esté registrado en plugin.yml
            if (plugin.getCommand("citizen") == null) {
                plugin.getLogger().severe("El comando 'citizen' no está registrado en plugin.yml");
                return null;
            }
            
            // Crear la instancia del comando
            CitizenCommands citizenCommands = new CitizenCommands(plugin);
            
            // Log de inicialización exitosa
            plugin.getLogger().info("CitizenCommands inicializado correctamente");
            
            return citizenCommands;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error al inicializar CitizenCommands: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, Messages.PLAYER_COMMAND_ONLY);
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        // Validación temprana del comando
        if (!VALID_COMMANDS.contains(subCommand)) {
            MessageUtils.sendMessage(player, Messages.INVALID_ARGUMENTS);
            return true;
        }

        // Validación de argumentos para comandos que requieren ciudad
        if (CITY_REQUIRED_COMMANDS.contains(subCommand) && args.length < 2) {
            MessageUtils.sendMessage(player, "&cUsage: /citizen " + subCommand + " <city>");
            return true;
        }

        return switch (subCommand) {
            case "help" -> { sendHelpMessage(player); yield true; }
            case "info" -> { showCitizenInfo(player); yield true; }
            case "leave" -> { leaveCityCommand(player); yield true; }
            case "join" -> { joinCityCommand(player, args[1]); yield true; }
            case "list" -> { listCitizensCommand(player, args[1]); yield true; }
            default -> true; // Ya validado arriba, nunca debería llegar aquí
        };
    }

    private void sendHelpMessage(Player player) {
        String[] helpLines = {
            "&6=== &eCitizenWars Citizen Commands &6===",
            "&e/citizen info &7- Ver información de tu ciudadanía",
            "&e/citizen join <ciudad> &7- Unirse a una ciudad",
            "&e/citizen leave &7- Abandonar tu ciudad actual",
            "&e/citizen list <ciudad> &7- Listar ciudadanos de una ciudad"
        };
        
        Arrays.stream(helpLines).forEach(line -> MessageUtils.sendMessage(player, line));
    }

    private void showCitizenInfo(Player player) {
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            MessageUtils.sendMessage(player, Messages.PLAYER_NOT_CITIZEN);
            return;
        }
    }

    private void leaveCityCommand(Player player) {
        City playerCity = citizenManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            MessageUtils.sendMessage(player, Messages.PLAYER_NOT_CITIZEN);
            return;
        }

        if (!citizenManager.removeCitizen(player.getUniqueId())) {
            MessageUtils.sendMessage(player, Messages.CITIZEN_REMOVE_FAILED);
            return;
        }

        // Éxito al salir
        MessageUtils.sendMessage(player, Messages.CITIZEN_REMOVED.replace("%city%", playerCity.getName()));
        
        // Notificar a otros ciudadanos de forma optimizada
        notifyCitizens(playerCity.getId(), player.getName(), Messages.CITIZEN_REMOVED.replace("%player%", player.getName()));
    }

    private void joinCityCommand(Player player, String cityName) {
        // Verificar si ya está en una ciudad
        City existingCity = citizenManager.getPlayerCity(player.getUniqueId());
        if (existingCity != null) {
            MessageUtils.sendMessage(player, Messages.CITIZEN_ADD_FAILED.replace("%city%", existingCity.getName()));
            return;
        }

        // Verificar si la ciudad existe
        City city = cityManager.getCityByName(cityName);
        if (city == null) {
            MessageUtils.sendMessage(player, Messages.CITY_NOT_FOUND);
            return;
        }

        // Intentar unirse
        if (!citizenManager.addCitizen(player.getUniqueId(), city)) {
            MessageUtils.sendMessage(player, Messages.CITIZEN_ADD_FAILED);
            return;
        }

        // Éxito al unirse
        MessageUtils.sendMessage(player, Messages.CITIZEN_ADDED.replace("%city%", city.getName()));
        
        // Notificar a otros ciudadanos
        notifyCitizens(city.getId(), player.getName(), Messages.CITIZEN_REGISTERED.replace("%player%", player.getName()));
    }

    private void listCitizensCommand(Player player, String cityName) {
        City city = cityManager.getCityByName(cityName);
        if (city == null) {
            MessageUtils.sendMessage(player, Messages.CITY_NOT_FOUND);
            return;
        }

        Set<UUID> citizenIds = citizenManager.getCitizensInCity(city.getId());
        if (citizenIds.isEmpty()) {
            MessageUtils.sendMessage(player, Messages.CITY_NOT_MEMBER);
            return;
        }

        MessageUtils.sendMessage(player, "&6=== &eCiudadanos de " + city.getName() + " &6===");

        // Procesar ciudadanos de forma más eficiente
        Map<Boolean, List<String>> citizensByStatus = citizenIds.stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.partitioningBy(
                Player::isOnline,
                Collectors.mapping(Player::getName, Collectors.toList())
            ));

        // Mostrar online primero
        citizensByStatus.get(true).forEach(name -> 
            MessageUtils.sendMessage(player, "&7- &e" + name + " &aOnline"));
        
        // Luego offline
        citizensByStatus.get(false).forEach(name -> 
            MessageUtils.sendMessage(player, "&7- &e" + name + " &cOffline"));

        MessageUtils.sendMessage(player, "&6Total: &e" + citizenIds.size() + " ciudadanos");
    }

    /**
     * Método optimizado para notificar a todos los ciudadanos de una ciudad
     */
    private void notifyCitizens(UUID cityId, String excludePlayerName, String message) {
        citizenManager.getCitizensInCity(cityId).stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .filter(Player::isOnline)
            .filter(p -> !p.getName().equals(excludePlayerName))
            .forEach(p -> MessageUtils.sendMessage(p, message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length == 0) {
            return Collections.emptyList();
        }

        String lastArg = args[args.length - 1].toLowerCase();

        return switch (args.length) {
            case 1 -> VALID_COMMANDS.stream()
                .filter(cmd -> cmd.startsWith(lastArg))
                .sorted()
                .collect(Collectors.toList());
                
            case 2 -> switch (args[0].toLowerCase()) {
                case "join", "list" -> cityManager.getAllCities().stream()
                    .map(City::getName)
                    .filter(name -> name.toLowerCase().startsWith(lastArg))
                    .sorted()
                    .collect(Collectors.toList());
                default -> Collections.emptyList();
            };
            
            default -> Collections.emptyList();
        };
    }
}