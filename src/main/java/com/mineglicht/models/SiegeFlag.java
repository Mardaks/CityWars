package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Representa un estandarte de asedio utilizado para iniciar guerras entre ciudades
 */
public class SiegeFlag {

    private UUID ownerUUID;           // UUID del jugador que colocó la bandera
    private Location location;        // Ubicación de la bandera en el mundo
    private boolean isActive;         // Si la bandera está activa o ha sido destruida
    private long placedTime;          // Hora en que la bandera fue colocada
    private int duration;             // Duración en segundos antes de que la bandera se destruya automáticamente
    private ItemStack flagItem;      // El ítem de la bandera de asedio (se crea con ExecutableItems)

    // Constructor
    public SiegeFlag(UUID ownerUUID, Location location, int duration, ItemStack flagItem) {
        this.ownerUUID = ownerUUID;
        this.location = location;
        this.isActive = true;  // La bandera empieza activa
        this.placedTime = System.currentTimeMillis();
        this.duration = duration;  // Duración en segundos
        this.flagItem = flagItem;
    }

    // Getters
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getPlacedTime() {
        return placedTime;
    }

    public int getDuration() {
        return duration;
    }

    public ItemStack getFlagItem() {
        return flagItem;
    }

    // Métodos de actualización y gestión de la bandera

    // Activar o desactivar la bandera (cuando se coloca o destruye)
    public void setestadoBandera(boolean active) {
        this.isActive = active;
    }

    // Verificar si la bandera debe ser destruida (por tiempo de duración)
    public boolean verificaSiExpiro() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - placedTime) >= (duration * 1000);  // Compara el tiempo actual con el tiempo de colocación
    }

    // Establecer la ubicación de la bandera (si se mueve o se reposiciona)
    public void setUbiBandera(Location location) {
        this.location = location;
    }

    // Método para mostrar información básica sobre la bandera
    @Override
    public String toString() {
        return "SiegeFlag{" +
               "ownerUUID=" + ownerUUID +
               ", location=" + location +
               ", isActive=" + isActive +
               ", placedTime=" + placedTime +
               ", duration=" + duration +
               '}';
    }
}