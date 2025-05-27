package com.mineglicht.api.event;

import com.mineglicht.models.City;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Evento que se dispara cuando inicia un asedio
 * Puede ser cancelado por otros plugins
 */
public class SiegeStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final City attackerCity;
    private final City defenderCity;
    private final Player initiator;
    private final UUID initiatorUuid;
    private final Location siegeFlagLocation;
    private final int siegeDurationMinutes;
    private final double economyCost;
    private String cancelReason;
    private int customDuration = -1;

    /**
     * Constructor del evento de inicio de asedio
     * @param attackerCity Ciudad atacante
     * @param defenderCity Ciudad defensora
     * @param initiator Jugador que inicia el asedio
     * @param siegeFlagLocation Ubicación del estandarte de asedio
     * @param siegeDurationMinutes Duración del asedio en minutos
     * @param economyCost Costo económico del asedio
     */
    public SiegeStartEvent(City attackerCity, City defenderCity, Player initiator,
                           Location siegeFlagLocation, int siegeDurationMinutes, double economyCost) {
        this.attackerCity = attackerCity;
        this.defenderCity = defenderCity;
        this.initiator = initiator;
        this.initiatorUuid = initiator.getUniqueId();
        this.siegeFlagLocation = siegeFlagLocation.clone();
        this.siegeDurationMinutes = siegeDurationMinutes;
        this.economyCost = economyCost;
    }

    /**
     * Obtiene la ciudad atacante
     * @return Ciudad atacante
     */
    public City getAttackerCity() {
        return attackerCity;
    }

    /**
     * Obtiene la ciudad defensora
     * @return Ciudad defensora
     */
    public City getDefenderCity() {
        return defenderCity;
    }

    /**
     * Obtiene el jugador que inicia el asedio
     * @return Jugador iniciador
     */
    public Player getInitiator() {
        return initiator;
    }

    /**
     * Obtiene el UUID del jugador que inicia el asedio
     * @return UUID del iniciador
     */
    public UUID getInitiatorUuid() {
        return initiatorUuid;
    }

    /**
     * Obtiene la ubicación del estandarte de asedio
     * @return Ubicación del estandarte
     */
    public Location getSiegeFlagLocation() {
        return siegeFlagLocation.clone();
    }

    /**
     * Obtiene la duración del asedio en minutos
     * @return Duración en minutos
     */
    public int getSiegeDurationMinutes() {
        return customDuration != -1 ? customDuration : siegeDurationMinutes;
    }

    /**
     * Obtiene la duración original del asedio (sin modificaciones)
     * @return Duración original en minutos
     */
    public int getOriginalSiegeDurationMinutes() {
        return siegeDurationMinutes;
    }

    /**
     * Establece una duración personalizada para el asedio
     * @param minutes Nueva duración en minutos
     */
    public void setCustomSiegeDuration(int minutes) {
        this.customDuration = minutes;
    }

    /**
     * Obtiene el costo económico del asedio
     * @return Costo del asedio
     */
    public double getEconomyCost() {
        return economyCost;
    }

    /**
     * Obtiene la razón de cancelación del evento
     * @return Razón de cancelación o null si no fue cancelado
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Establece la razón de cancelación
     * @param reason Razón de la cancelación
     */
    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }

    /**
     * Verifica si las ciudades son válidas para el asedio
     * @return true si ambas ciudades son válidas
     */
    public boolean areCitiesValid() {
        return attackerCity != null && defenderCity != null &&
                !attackerCity.getName().equals(defenderCity.getName());
    }

    /**
     * Obtiene el porcentaje de ciudadanos conectados de la ciudad defensora
     * @return Porcentaje de ciudadanos conectados (0.0 - 1.0)
     */
    public double getDefenderOnlinePercentage() {
        if (defenderCity == null) return 0.0;

        int totalCitizens = defenderCity.getCitizens().size();
        if (totalCitizens == 0) return 0.0;

        long onlineCitizens = defenderCity.getCitizens().stream()
                .mapToLong(citizenUuid -> {
                    Player player = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(String.valueOf(citizenUuid))); // antes era Player player = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(citizenUuid));
                    return (player != null && player.isOnline()) ? 1 : 0;
                })
                .sum();

        return (double) onlineCitizens / totalCitizens;
    }

    /**
     * Verifica si se cumple el requisito mínimo de ciudadanos conectados
     * @param minimumPercentage Porcentaje mínimo requerido (0.0 - 1.0)
     * @return true si se cumple el requisito
     */
    public boolean meetsOnlineRequirement(double minimumPercentage) {
        return getDefenderOnlinePercentage() >= minimumPercentage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Cancela el evento con una razón específica
     * @param cancelled Estado de cancelación
     * @param reason Razón de la cancelación
     */
    public void setCancelled(boolean cancelled, String reason) {
        this.cancelled = cancelled;
        this.cancelReason = reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
