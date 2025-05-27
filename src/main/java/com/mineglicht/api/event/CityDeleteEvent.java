package com.mineglicht.api.event;

import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * Evento que se dispara cuando se elimina una ciudad
 * Puede ser cancelado por otros plugins
 */
public class CityDeleteEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final City city;
    private final CommandSender deleter;
    private final List<Citizen> affectedCitizens;
    private final double bankBalance;
    private final String deleteReason;
    private String cancelReason;
    private boolean refundBank = true;

    /**
     * Constructor del evento de eliminación de ciudad
     * @param city Ciudad que será eliminada
     * @param deleter Quien elimina la ciudad (jugador o consola)
     * @param affectedCitizens Lista de ciudadanos que serán afectados
     * @param deleteReason Razón de la eliminación
     */
    public CityDeleteEvent(City city, CommandSender deleter, List<Citizen> affectedCitizens, String deleteReason) {
        this.city = city;
        this.deleter = deleter;
        this.affectedCitizens = affectedCitizens;
        this.bankBalance = city.getBankBalance();
        this.deleteReason = deleteReason;
    }

    /**
     * Obtiene la ciudad que será eliminada
     * @return Ciudad a eliminar
     */
    public City getCity() {
        return city;
    }

    /**
     * Obtiene quien está eliminando la ciudad
     * @return CommandSender (jugador o consola)
     */
    public CommandSender getDeleter() {
        return deleter;
    }

    /**
     * Obtiene la lista de ciudadanos afectados
     * @return Lista de ciudadanos
     */
    public List<Citizen> getAffectedCitizens() {
        return affectedCitizens;
    }

    /**
     * Obtiene el balance del fondo bancario de la ciudad
     * @return Balance del fondo bancario
     */
    public double getBankBalance() {
        return bankBalance;
    }

    /**
     * Obtiene la razón de la eliminación
     * @return Razón de eliminación
     */
    public String getDeleteReason() {
        return deleteReason;
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
     * Verifica si el fondo bancario será reembolsado a los ciudadanos
     * @return true si se reembolsará
     */
    public boolean isRefundBank() {
        return refundBank;
    }

    /**
     * Establece si el fondo bancario debe ser reembolsado
     * @param refundBank true para reembolsar, false para no hacerlo
     */
    public void setRefundBank(boolean refundBank) {
        this.refundBank = refundBank;
    }

    /**
     * Obtiene el número de ciudadanos afectados
     * @return Número de ciudadanos
     */
    public int getAffectedCitizensCount() {
        return affectedCitizens.size();
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
