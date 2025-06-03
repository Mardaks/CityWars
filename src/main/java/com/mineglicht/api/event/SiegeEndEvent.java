package com.mineglicht.api.event;

import com.mineglicht.models.City;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.UUID;

/**
 * Evento que se dispara cuando termina un asedio entre ciudades
 */
public class SiegeEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final City attackingCity;
    private final City defendingCity;
    private final Player siegeInitiator;
    private final Location siegeFlagLocation;
    private final SiegeEndReason endReason;
    private final boolean wasSuccessful;
    private final List<UUID> attackingPlayers;
    private final List<UUID> defendingPlayers;
    private final long siegeDurationMillis;
    private final double stolenFunds;
    private final boolean flagCaptured;
    private final long cooldownEndTime;

    /**
     * Constructor del evento SiegeEndEvent
     *
     * @param attackingCity Ciudad atacante
     * @param defendingCity Ciudad defensora
     * @param siegeInitiator Jugador que inició el asedio
     * @param siegeFlagLocation Ubicación donde se colocó la bandera de asedio
     * @param endReason Razón por la que terminó el asedio
     * @param wasSuccessful Si el asedio fue exitoso para los atacantes
     * @param attackingPlayers Lista de UUIDs de jugadores atacantes
     * @param defendingPlayers Lista de UUIDs de jugadores defensores
     * @param siegeDurationMillis Duración del asedio en milisegundos
     * @param stolenFunds Cantidad de fondos robados (50% del banco de la ciudad)
     * @param flagCaptured Si la bandera fue capturada
     * @param cooldownEndTime Timestamp cuando termina el cooldown entre estas ciudades
     */
    public SiegeEndEvent(City attackingCity, City defendingCity, Player siegeInitiator,
                         Location siegeFlagLocation, SiegeEndReason endReason, boolean wasSuccessful,
                         List<UUID> attackingPlayers, List<UUID> defendingPlayers,
                         long siegeDurationMillis, double stolenFunds, boolean flagCaptured,
                         long cooldownEndTime) {
        this.attackingCity = attackingCity;
        this.defendingCity = defendingCity;
        this.siegeInitiator = siegeInitiator;
        this.siegeFlagLocation = siegeFlagLocation;
        this.endReason = endReason;
        this.wasSuccessful = wasSuccessful;
        this.attackingPlayers = attackingPlayers;
        this.defendingPlayers = defendingPlayers;
        this.siegeDurationMillis = siegeDurationMillis;
        this.stolenFunds = stolenFunds;
        this.flagCaptured = flagCaptured;
        this.cooldownEndTime = cooldownEndTime;
    }

    /**
     * @return Ciudad que atacó
     */
    public City getAttackingCity() {
        return attackingCity;
    }

    /**
     * @return Ciudad que fue atacada
     */
    public City getDefendingCity() {
        return defendingCity;
    }

    /**
     * @return Jugador que inició el asedio
     */
    public Player getSiegeInitiator() {
        return siegeInitiator;
    }

    /**
     * @return Ubicación donde se colocó la bandera de asedio
     */
    public Location getSiegeFlagLocation() {
        return siegeFlagLocation;
    }

    /**
     * @return Razón por la que terminó el asedio
     */
    public SiegeEndReason getEndReason() {
        return endReason;
    }

    /**
     * @return true si el asedio fue exitoso para los atacantes
     */
    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    /**
     * @return Lista de UUIDs de jugadores que participaron en el ataque
     */
    public List<UUID> getAttackingPlayers() {
        return attackingPlayers;
    }

    /**
     * @return Lista de UUIDs de jugadores que participaron en la defensa
     */
    public List<UUID> getDefendingPlayers() {
        return defendingPlayers;
    }

    /**
     * @return Duración del asedio en milisegundos
     */
    public long getSiegeDurationMillis() {
        return siegeDurationMillis;
    }

    /**
     * @return Duración del asedio en segundos
     */
    public long getSiegeDurationSeconds() {
        return siegeDurationMillis / 1000;
    }

    /**
     * @return Cantidad de fondos robados del banco de la ciudad defensora
     */
    public double getStolenFunds() {
        return stolenFunds;
    }

    /**
     * @return true si la bandera de la ciudad fue capturada
     */
    public boolean wasFlagCaptured() {
        return flagCaptured;
    }

    /**
     * @return Timestamp cuando termina el cooldown entre estas ciudades
     */
    public long getCooldownEndTime() {
        return cooldownEndTime;
    }

    /**
     * @return Tiempo restante de cooldown en milisegundos
     */
    public long getRemainingCooldownMillis() {
        return Math.max(0, cooldownEndTime - System.currentTimeMillis());
    }

    /**
     * @return true si aún hay cooldown activo entre estas ciudades
     */
    public boolean hasCooldownActive() {
        return System.currentTimeMillis() < cooldownEndTime;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Enum que define las posibles razones por las que puede terminar un asedio
     */
    public enum SiegeEndReason {
        /**
         * El asedio terminó porque se capturó la bandera enemiga
         */
        FLAG_CAPTURED("La bandera fue capturada"),

        /**
         * El asedio terminó porque el tiempo límite expiró
         */
        TIME_EXPIRED("El tiempo de asedio expiró"),

        /**
         * El asedio fue detenido manualmente por un administrador
         */
        ADMIN_STOP("Detenido por administrador"),

        /**
         * El asedio terminó porque la bandera de asedio fue destruida
         */
        SIEGE_FLAG_DESTROYED("La bandera de asedio fue destruida"),

        /**
         * El asedio terminó porque no había suficientes defensores conectados
         */
        INSUFFICIENT_DEFENDERS("Defensores insuficientes conectados"),

        /**
         * El asedio terminó porque el iniciador se desconectó
         */
        INITIATOR_DISCONNECTED("El iniciador del asedio se desconectó"),

        /**
         * El asedio terminó por error del servidor o plugin
         */
        SERVER_ERROR("Error del servidor");

        private final String description;

        SiegeEndReason(String description) {
            this.description = description;
        }

        /**
         * @return Descripción legible de la razón
         */
        public String getDescription() {
            return description;
        }
    }
}