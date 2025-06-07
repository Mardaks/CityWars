package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.Citizen;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
import com.mineglicht.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class CitizenManager {

    private final cityWars plugin;
    private final Map<UUID, Citizen> citizens;
    private final Map<UUID, Set<UUID>> cityToCitizens; // Asigna el Id de la cuidad a un conjunto de Id's de cuidadanos
    private final File citizensFile;
    private FileConfiguration citizensConfig;
    private final CityManager cityManager;
    private final Map<UUID, BukkitRunnable> disconnectingPlayers = new HashMap<>(); // Jugadores en proceso de desconexión
    private final Map<UUID, Location> playerLastLocations = new HashMap<>(); // Últimas ubicaciones de jugadores

    public CitizenManager(cityWars plugin, CityManager cityManager) {
        this.plugin = plugin;
        this.cityManager = cityManager;
        this.citizens = new HashMap<>();
        this.cityToCitizens = new HashMap<>();
        this.citizensFile = new File(plugin.getDataFolder(), "citizens.yml");

        loadCitizens();
    }

    /**
     * Agregar un jugador a una ciudad
     *
     * @param playerId UUID de un jugador
     * @param city Ciudad a la que se le va a agregar
     * @return Retorna true si se agrega exitosamente
     */
    public boolean addCitizen(UUID playerId, City city) {
        if (city == null || playerId == null) {
            return false;
        }

        // Verificar si el jugador ya pertenece en una ciudad
        if (isCitizen(playerId)) {
            return false;
        }

        // Crear nuevo ciudadano
        Citizen citizen = new Citizen(playerId, city.getId());
        citizens.put(playerId, citizen);

        // Agregar a la lista de la ciudad a los ciudadanos
        Set<UUID> cityCitizens = cityToCitizens.getOrDefault(city.getId(), new HashSet<>());
        cityCitizens.add(playerId);
        cityToCitizens.put(city.getId(), cityCitizens);

        saveCitizens();
        return true;
    }

    /**
     * Eliminar a un jugador de su ciudad
     *
     * @param playerId UUID de un jugador
     * @return Retorna true si se elimino exitosamente
     */
    public boolean removeCitizen(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return false;
        }
        return removeFromCity(citizen);
    }

    /**
     * Metodo de ayuda para remover a ciudadano de su ciudad
     *
     * @param citizen Ciudadano a remover
     * @return Retorna true si se removio exitosamente
     */
    private boolean removeFromCity(Citizen citizen) {
        UUID cityId = citizen.getCityId();
        UUID playerId = citizen.getCitizenId();

        // Eliminar al ciudadano de la lista
        citizens.remove(playerId);

        // Eliminar de la lista city-to-citizens
        Set<UUID> cityCitizens = cityToCitizens.get(cityId);
        if (cityCitizens != null) {
            cityCitizens.remove(playerId);
            if (cityCitizens.isEmpty()) {
                cityToCitizens.remove(cityId);
            } else {
                cityToCitizens.put(cityId, cityCitizens);
            }
        }

        saveCitizens();
        return true;
    }

    /**
     * Cambiar a jugador de ciudad
     *
     * @param playerId UUID de un jugador
     * @param newCity Ciudad a la que se va a cambiar
     * @return retorna true si se cambio exitosamente
     */
    public boolean changeCitizenCity(UUID playerId, City newCity) {
        // Remover de su ciudad actual
        boolean removed = removeCitizen(playerId);
        if (!removed && getCitizen(playerId) != null) {
            return false;
        }

        // Agregar a la nueva ciudad
        return addCitizen(playerId, newCity);
    }

    /**
     * Obtener un ciudadano por su UUID
     *
     * @param playerId UUID del jugador
     * @return Retorna el ciudadano o null si no se encuentra
     */
    public Citizen getCitizen(UUID playerId) {
        return citizens.get(playerId);
    }

    /**
     * Obtener la ciudad de un jugador
     * @param playerId UUID de un jugador
     * @return Retorna la ciudad a la que pertenece el jugador
     */
    public City getPlayerCity(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return null;
        }
        return cityManager.getCity(citizen.getCityId());
    }

    /**
     * Verifica se un jugador es ciudadano
     *
     * @param playerId UUID de un jugador
     * @return Retorna true si el jugador pertenece a una ciudad
     */
    public boolean isCitizen(UUID playerId) {
        return citizens.containsKey(playerId);
    }

    /**
     * Obtener todos los ciudadanos de una ciudad
     *
     * @param cityId UUID de la ciudad
     * @return Retorna una lista de los ciudadanos de la ciudad
     */
    public Set<UUID> getCitizensInCity(UUID cityId) {
        return Collections.unmodifiableSet(cityToCitizens.getOrDefault(cityId, new HashSet<>()));
    }

    /**
     * Obtener todos los ciudadanos ONLINE de una ciudad
     *
     * @param cityId UUID de la ciudad
     * @return Retorna una lista de los ciudadanos online de la ciudad
     */
    public Set<UUID> getOnlineCitizensInCity(UUID cityId) {
        Set<UUID> allCitizens = getCitizensInCity(cityId);
        Set<UUID> onlineCitizens = new HashSet<>();

        for (UUID citizenId : allCitizens) {
            Player player = Bukkit.getPlayer(citizenId);
            if (player != null && player.isOnline()) {
                onlineCitizens.add(citizenId);
            }
        }

        return onlineCitizens;
    }

    /**
     * Calcula el porcentaje de ciudadanos online en una ciudad
     * @param cityId UUID de la ciudad
     * @return Retorna el porcentaje de ciudadanos online de una ciudad
     */
    public double getOnlineCitizenPercentage(UUID cityId) {
        Set<UUID> allcitizens = getCitizensInCity(cityId);
        if (allcitizens.isEmpty()) {
            return 0.0;
        }

        Set<UUID> onlineCitizens = getOnlineCitizensInCity(cityId);
        return (double) onlineCitizens.size()/allcitizens.size();
    }

    /**
     * Cargar los ciudadanos desde el archivo de configuracion
     */
    public void loadCitizens() {
        citizens.clear();
        cityToCitizens.clear();

        if (!citizensFile.exists()) {
            plugin.saveResource("citizens.yml", false);
        }

        citizensConfig = YamlConfiguration.loadConfiguration(citizensFile);

        for (String playerIdStr : citizensConfig.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                UUID cityId = UUID.fromString(citizensConfig.getString(playerIdStr + ".cityId"));

                // Validar si la ciudad existe
                if (cityManager.getCity(cityId) == null) {
                    plugin.getLogger().warning("Skipping citizen " + playerIdStr + " - city does not exist");
                    continue;
                }

                // Create citizen
                Citizen citizen = new Citizen(playerId, cityId);

                citizens.put(playerId, citizen);

                // Update city-to-citizens mapping
                Set<UUID> cityCitizens = cityToCitizens.getOrDefault(cityId, new HashSet<>());
                cityCitizens.add(playerId);
                cityToCitizens.put(cityId, cityCitizens);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load citizen: " + playerIdStr, e);
            }
        }

        plugin.getLogger().info("Loaded " + citizens.size() + " citizens");
    }

    /**
     * Guardar los ciudadanos en el archivo de configuracion
     */
    public void saveCitizens() {
        citizensConfig = new YamlConfiguration();

        for (Map.Entry<UUID, Citizen> entry : citizens.entrySet()) {
            UUID playerId = entry.getKey();
            Citizen citizen = entry.getValue();
            String playerIdStr = playerId.toString();

            citizensConfig.set(playerIdStr + ".cityId", citizen.getCityId().toString());
        }

        try {
            citizensConfig.save(citizensFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save citizens", e);
        }
    }

    /**
     * Incremente el contador de jugadores online de una ciudad
     *
     * @param cityId UUID de la ciudad
     */
    public void incrementCityOnlineCount(UUID cityId) {
        Set<UUID> onlineCitizens = getOnlineCitizensInCity(cityId);
        plugin.getLogger().info("Ciudad: " + cityId + ", ahora tiene " + onlineCitizens.size() + " cuidadanos conectados");
    }

    /**
     * Decrementa el contador de ciudadanos online para una ciudad.
     *
     * @param cityId UUID de la ciudad
     */
    public void decrementCityOnlineCount(UUID cityId) {
        Set<UUID> onlineCitizens = getOnlineCitizensInCity(cityId);
        plugin.getLogger().info("Cuidad: " + cityId + " ahora tiene " + onlineCitizens.size() + " cuidadanos conectados");
    }

    /**
     * Verifica la viabilidad del asedio de una ciudad cuando un jugador se desconecta
     * Si la ciudad está bajo asedio, el jugador no puede desconectarse inmediatamente.
     * Su personaje permanece en el juego por 2 minutos antes de desaparecer.
     *
     * @param cityId UUID de la ciudad
     * @param player El jugador que intenta desconectarse
     * @return true si el jugador puede desconectarse inmediatamente, false si debe esperar
     */
    public boolean checkCitySiegeViability(UUID cityId, Player player) {
        City city = cityManager.getCity(cityId);
        if (city == null) {
            return true; // Si no hay ciudad, permite la desconexión
        }

        // Verificar si la ciudad está bajo asedio usando el enum SiegeState
        SiegeState siegeState = city.getSiegeState();
        if (!siegeState.isActive()) { // Verifica si es el estado de Asedio es distinto de isActive()
            return true; // Si no está bajo asedio activo, permite desconexión normal
        }

        // La ciudad está bajo asedio - implementar logica de desconexión con delay
        handleSiegeDisconnection(player);
        return false; // No permite desconexión inmediata
    }

    /**
     * Maneja la desconexión de un jugador durante un asedio
     * El personaje permanece en el juego por 2 minutos antes de desaparecer.
     *
     * @param player El jugador que se está desconectando
     */
    private void handleSiegeDisconnection(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancelar cualquier tarea de desconexión previa para este jugador
        BukkitRunnable existingTask = disconnectingPlayers.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Guardar la ubicacion del jugador
        playerLastLocations.put(playerId, player.getLocation());

        // Enviar mensaje al jugador
        player.sendMessage("§c¡Tu ciudad está bajo asedio! Tu personaje permanecerá en el juego por 2 minutos.");

        // Crear tarea para hacer desaparecer al jugador después de 2 minutos
        BukkitRunnable disconnectTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Remover al jugador del mapa de desconexiones
                disconnectingPlayers.remove(playerId);
                playerLastLocations.remove(playerId);

                // Si el jugador sigue online, forzar desconexión
                Player onlinePlayer = Bukkit.getPlayer(playerId);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    onlinePlayer.kickPlayer("§cTu tiempo de permanencia durante el asedio ha terminado.");
                }

                plugin.getLogger().info("Jugador " + player.getName() + " ha sido removido del asedio después de 2 minutos.");
            }
        };

        // Programar la tarea para ejecutarse en 2 minutos (240 ticks - 120 segundos)
        disconnectTask.runTaskLater(plugin, 2400L);
        disconnectingPlayers.put(playerId, disconnectTask);

        // Notificar a los miembros de la ciudad
        Citizen citizen = getCitizen(playerId);
        if (citizen != null) {
            City city = cityManager.getCity(citizen.getCityId());
            if (city != null) {
                MessageUtils.sendToCityMembers(this, city.getId(),
                        MessageUtils.formatMessage("siege.member-disconnect",
                                "%player%", player.getName()));
            }
        }
    }

    /**
     * Verifica si un jugador está en proceso de desconexión durante un asedio.
     *
     * @param playerId UUID del jugador
     * @return true si el jugador está en proceso de desconexión
     */
    public boolean isPlayerDisconnecting(UUID playerId) {
        return disconnectingPlayers.containsKey(playerId);
    }

    /**
     * Cancela el proceso de desconexión de un jugador si se reconecta durante el asedio.
     *
     * @param playerId UUID del jugador
     */
    public void cancelDisconnection(UUID playerId) {
        BukkitRunnable task = disconnectingPlayers.remove(playerId);
        if (task != null) {
            task.cancel();
            playerLastLocations.remove(playerId);
        }
    }

    /**
     * Obtiene la última ubicación guardad de un jugador.
     * Esta ubicación se guarda cuando el jugador se desconecta durante un asedio
     *
     * @param playerId UUID del jugador
     * @return La última ubicación del jugador o null si no hay ninguna guardada
     */
    public Location getPlayerLastLocation (UUID playerId) {
        return playerLastLocations.get(playerId);
    }

    /**
     * Guarda los datos de un ciudadano específico
     *
     * @param playerId UUID del jugador cuyos datos se van a guardar
     */
    public void saveCitizens(UUID playerId) {
        Citizen citizen = getCitizen(playerId);
        if (citizen == null) {
            return;
        }

        // Cargar configuration actual
        if (citizensConfig == null) {
            citizensConfig = YamlConfiguration.loadConfiguration(citizensFile);
        }

        String playerIdStr = playerId.toString();
        citizensConfig.set(playerIdStr + ".cityId", citizen.getCityId().toString());

        try {
            citizensConfig.save(citizensFile);
            plugin.getLogger().info("Datos del ciudadano " + playerId + " guardados correctamente.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar datos del ciudadano: " + playerId, e);
        }
    }
}
