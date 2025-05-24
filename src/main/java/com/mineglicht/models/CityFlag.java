package com.mineglicht.models;

/**
 * Representa una bandera de protección para ciudades
 * Estas flags controlan diferentes aspectos de protección y comportamiento en el territorio de una ciudad
 */
public class CityFlag {
    private final String name;
    private boolean value;
    private boolean defaultForVisitors;
    private boolean defaultForCitizens;
    private boolean defaultForOfficers;
    private boolean defaultForOwner;
    private boolean lockedForSiege; // Si está bloqueada durante un asedio

    /**
     * Constructor simple para crear una bandera con un valor por defecto
     *
     * @param name Nombre de la bandera
     * @param value Valor por defecto
     */
    public CityFlag(String name, boolean value) {
        this.name = name;
        this.value = value;
        this.defaultForVisitors = false;
        this.defaultForCitizens = true;
        this.defaultForOfficers = true;
        this.defaultForOwner = true;
        this.lockedForSiege = true;
    }

    /**
     * Constructor completo para crear una bandera con configuración detallada
     *
     * @param name Nombre de la bandera
     * @param value Valor general por defecto
     * @param defaultForVisitors Valor por defecto para visitantes
     * @param defaultForCitizens Valor por defecto para ciudadanos
     * @param defaultForOfficers Valor por defecto para oficiales
     * @param defaultForOwner Valor por defecto para el dueño
     * @param lockedForSiege Si la bandera se bloquea durante un asedio
     */
    public CityFlag(String name, boolean value, boolean defaultForVisitors,
                    boolean defaultForCitizens, boolean defaultForOfficers,
                    boolean defaultForOwner, boolean lockedForSiege) {
        this.name = name;
        this.value = value;
        this.defaultForVisitors = defaultForVisitors;
        this.defaultForCitizens = defaultForCitizens;
        this.defaultForOfficers = defaultForOfficers;
        this.defaultForOwner = defaultForOwner;
        this.lockedForSiege = lockedForSiege;
    }

    /**
     * Determina si se permite una acción para un rol específico dentro de la ciudad
     *
     * @param isVisitor Si el jugador es un visitante
     * @param isCitizen Si el jugador es un ciudadano
     * @param isOfficer Si el jugador es un oficial
     * @param isOwner Si el jugador es el dueño
     * @return true si la acción está permitida, false si no
     */
    public boolean isAllowed(boolean isVisitor, boolean isCitizen, boolean isOfficer, boolean isOwner) {
        if (isOwner) {
            return defaultForOwner;
        }
        if (isOfficer) {
            return defaultForOfficers;
        }
        if (isCitizen) {
            return defaultForCitizens;
        }
        return defaultForVisitors;
    }

    /**
     * Prepara la bandera para el modo asedio si corresponde
     *
     * @param siegeMode Si se está en modo asedio
     * @return El valor anterior de la bandera (para restaurarlo después)
     */
    public boolean prepareSiegeMode(boolean siegeMode) {
        boolean previousValue = value;

        if (lockedForSiege && siegeMode) {
            // Durante el asedio, la bandera se configura según lo definido en la lógica del juego
            // Por ejemplo, PvP se activa en asedio
            if (name.equalsIgnoreCase("pvp")) {
                value = true;
            }
            // Block-break y block-place podrían habilitarse para asediantes durante la captura
        }

        return previousValue;
    }

    // Getters y setters

    public String getName() {
        return name;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean isDefaultForVisitors() {
        return defaultForVisitors;
    }

    public void setDefaultForVisitors(boolean defaultForVisitors) {
        this.defaultForVisitors = defaultForVisitors;
    }

    public boolean isDefaultForCitizens() {
        return defaultForCitizens;
    }

    public void setDefaultForCitizens(boolean defaultForCitizens) {
        this.defaultForCitizens = defaultForCitizens;
    }

    public boolean isDefaultForOfficers() {
        return defaultForOfficers;
    }

    public void setDefaultForOfficers(boolean defaultForOfficers) {
        this.defaultForOfficers = defaultForOfficers;
    }

    public boolean isDefaultForOwner() {
        return defaultForOwner;
    }

    public void setDefaultForOwner(boolean defaultForOwner) {
        this.defaultForOwner = defaultForOwner;
    }

    public boolean isLockedForSiege() {
        return lockedForSiege;
    }

    public void setLockedForSiege(boolean lockedForSiege) {
        this.lockedForSiege = lockedForSiege;
    }
}
