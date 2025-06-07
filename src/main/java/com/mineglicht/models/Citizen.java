package com.mineglicht.models;

import java.util.UUID;

/**
 * Representa a un ciudadano dentro del sistema CityWars
 */
public class Citizen {

    private final UUID citizenId;
    private UUID cityId; //Ciudad del jugador
    boolean isActive; // Si el ciudadano está activo o expulsado

    // Constructor para agregarlo
    public Citizen(UUID cityId) {
        this.citizenId = UUID.randomUUID();
        this.cityId = cityId;
        this.isActive = true; // Por defecto, el ciudadano está activo
    }

    // Constructor para cargarlo de un yml
    public Citizen(UUID citizenId, UUID cityId) {
        this.citizenId = citizenId;
        this.cityId = cityId;
        this.isActive = true; // Por defecto, el ciudadano está activo
    }

    //GETTERS Y SETTERS
    public UUID getCitizenId() {
        return citizenId;
    }

    public boolean isActive() {
        return isActive;
    }

    public UUID getCityId() {
        return cityId;  // Obtener la ciudad a la que pertenece el ciudadano
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;  // Actualiza la ciudad del ciudadano
    }
}