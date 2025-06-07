package com.mineglicht.models;

import java.util.UUID;

/**
 * Representa a un ciudadano dentro del sistema CityWars
 */
public class Citizen {

    private final UUID citizenId;
    private String name; // Nombre del ciudadano (opcional)
    private City ciudadania; //Ciudad del jugador
    boolean isActive; // Si el ciudadano está activo o expulsado

    /**
     * Constructor para crear un nuevo ciudadano
     *
     * @param citizenId UUID del jugador
     * @param name   UUID de la ciudad a la que pertenece
     */
    public Citizen(UUID citizenId, String name, City ciudadania) {
        this.citizenId = UUID.randomUUID();
        this.name = name;
        this.ciudadania = ciudadania;
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

    public City getCity() {
        return ciudadania;  // Obtener la ciudad a la que pertenece el ciudadano
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setCity(City city) {
        this.ciudadania = city;  // Actualiza la ciudad del ciudadano
    }

    // Método para comprobar si el ciudadano está en una ciudad específica
    public boolean isInCity(City city) {
        return this.ciudadania.equals(city);
    }

    @Override
    public String toString() {
        return "Citizen{" +
               "citizenId=" + citizenId +
               ", name='" + name + '\'' +
               ", isActive=" + isActive +
               ", city=" + ciudadania.getName() +  // Añadir nombre de la ciudad en la representación
               '}';
    }
}