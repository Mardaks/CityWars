package com.mineglicht.api.event;

import com.mineglicht.models.City;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Evento que se dispara cuando se crea una nueva ciudad
 * Puede ser cancelado por otros plugins
 */
public class CityCreateEvent extends Event implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final String cityName;
    private final UUID mayorUuid;
    private final Player mayor;
    private final Location location;
    private final double initialBankBalance;
    private City createdCity;
    private String cancelReason;

    /**
     * Constructor del evento de creación de ciudad
     * @param cityName Nombre de la ciudad a crear
     * @param mayorUuid UUID del alcalde
     * @param mayor Jugador que será alcalde
     * @param location Ubicación central de la ciudad
     * @param initialBankBalance Balance inicial del fondo bancario
     */
    public CityCreateEvent(String cityName, UUID mayorUuid, Player mayor, Location location, double initialBankBalance) {
        this.cityName = cityName;
        this.mayorUuid = mayorUuid;
        this.mayor = mayor;
        this.location = location.clone();
        this.initialBankBalance = initialBankBalance;
    }

    /**
     * Obtiene el nombre de la ciudad
     * @return Nombre de la ciudad
     */
    public String getCityName() {
        return cityName;
    }

    /**
     * Obtiene el UUID del alcalde
     * @return UUID del alcalde
     */
    public UUID getMayorUuid() {
        return mayorUuid;
    }

    /**
     * Obtiene el jugador que será alcalde
     * @return Jugador alcalde
     */
    public Player getMayor() {
        return mayor;
    }

    /**
     * Obtiene la ubicación central de la ciudad
     * @return Ubicación de la ciudad
     */
    public Location getLocation() {
        return location.clone();
    }

    /**
     * Obtiene el balance inicial del fondo bancario
     * @return Balance inicial
     */
    public double getInitialBankBalance() {
        return initialBankBalance;
    }

    /**
     * Obtiene la ciudad creada (disponible solo después de la creación)
     * @return Ciudad creada o null si aún no se ha creado
     */
    public City getCreatedCity() {
        return createdCity;
    }

    /**
     * Establece la ciudad creada (uso interno)
     * @param createdCity Ciudad que fue creada
     */
    public void setCreatedCity(City createdCity) {
        this.createdCity = createdCity;
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
