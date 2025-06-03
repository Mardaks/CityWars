package com.mineglicht.models;

/**
 * Representa una bandera de protección para ciudades
 * Estas flags controlan diferentes aspectos de protección y comportamiento en el territorio de una ciudad
 */
public enum CityFlag {
    
    // Flags de protección básicas
    PREVENT_MOB_SPAWN("prevent-mob-spawn", true, false, true, true, true, false),
    PREVENT_INTERACTION("prevent-interaction", true, false, true, true, true, false),
    PREVENT_BLOCK_PLACE("prevent-block-place", true, false, true, true, true, false),
    PREVENT_BLOCK_BREAK("prevent-block-break", true, false, true, true, true, false),
    PREVENT_ENDERPEARL("prevent-enderpearl", true, false, true, true, true, false),
    
    // Flags de PvP y combate
    PVP("pvp", false, false, false, false, false, true),
    PREVENT_EXPLOSION("prevent-explosion", true, true, true, true, true, false),
    
    // Flags de acceso y construcción
    ALLOW_VISITORS("allow-visitors", true, true, false, true, true, false),
    PUBLIC_BUILD("public-build", false, true, false, true, true, false),
    
    // Flags económicas
    ALLOW_TRADE("allow-trade", true, true, true, true, true, false),
    TAX_ENABLED("tax-enabled", true, false, true, true, true, false);

    private final String name;
    private final boolean defaultValue;
    private final boolean defaultForVisitors;
    private final boolean defaultForCitizens;
    private final boolean defaultForOfficers;
    private final boolean defaultForOwner;
    private final boolean lockedForSiege;

    /**
     * Constructor para crear una bandera con configuración completa
     *
     * @param name Nombre de la bandera
     * @param defaultValue Valor general por defecto
     * @param defaultForVisitors Valor por defecto para visitantes
     * @param defaultForCitizens Valor por defecto para ciudadanos
     * @param defaultForOfficers Valor por defecto para oficiales
     * @param defaultForOwner Valor por defecto para el dueño
     * @param lockedForSiege Si la bandera se bloquea durante un asedio
     */
    CityFlag(String name, boolean defaultValue, boolean defaultForVisitors,
             boolean defaultForCitizens, boolean defaultForOfficers,
             boolean defaultForOwner, boolean lockedForSiege) {
        this.name = name;
        this.defaultValue = defaultValue;
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
     * Obtiene el valor que debe tener esta flag durante un asedio
     *
     * @return El valor de la flag durante asedio
     */
    public boolean getSiegeValue() {
        if (lockedForSiege) {
            switch (this) {
                case PVP:
                    return true; // PvP se activa durante asedio
                case PREVENT_BLOCK_BREAK:
                case PREVENT_BLOCK_PLACE:
                    return false; // Se permite construcción/destrucción durante asedio
                default:
                    return defaultValue;
            }
        }
        return defaultValue;
    }

    // Getters
    public String getName() {
        return name;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public boolean isDefaultForVisitors() {
        return defaultForVisitors;
    }

    public boolean isDefaultForCitizens() {
        return defaultForCitizens;
    }

    public boolean isDefaultForOfficers() {
        return defaultForOfficers;
    }

    public boolean isDefaultForOwner() {
        return defaultForOwner;
    }

    public boolean isLockedForSiege() {
        return lockedForSiege;
    }
}