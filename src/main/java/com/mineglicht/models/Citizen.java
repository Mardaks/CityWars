package com.mineglicht.models;

import java.util.UUID;

/**
 * Representa a un ciudadano dentro del sistema CityWars
 */
public class Citizen {

    private final UUID citizenId;
    String name; // Nombre del ciudadano (opcional)
    boolean isActive; // Si el ciudadano está activo o expulsado

    /**
     * Constructor para crear un nuevo ciudadano
     *
     * @param citizenId UUID del jugador
     * @param cityId   UUID de la ciudad a la que pertenece
     */
    public Citizen(UUID citizenId, String name) {
        this.citizenId = UUID.randomUUID();
        this.name = name;
        this.isActive = true; // Por defecto, el ciudadano está activo}
    }

    //GETTERS Y SETTERS
    public UUID getCitizenId() {
        return citizenId;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return isActive;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}