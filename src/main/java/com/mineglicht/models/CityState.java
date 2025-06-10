package com.mineglicht.models;

/**
 * Enumeración que representa los diferentes estados posibles de un asedio
 */
public enum CityState {
    /**
     * No hay asedio activo
     */
    NONE,

    /**
     * El asedio está activo - el estandarte ha sido colocado
     */
    ACTIVE,

    /**
     * La bandera de la ciudad ha sido capturada - fase de saqueo activa
     */
    FLAG_CAPTURED,

    /**
     * El asedio ha sido defendido - los defensores ganaron
     */
    DEFENDED,

    /**
     * El asedio ha sido exitoso - los atacantes ganaron
     */
    SUCCESSFUL,

    /**
     * El asedio ha sido cancelado administrativamente
     */
    CANCELLED,

    /**
     * En período de cooldown después de un asedio
     */
    COOLDOWN;

    /**
     * Verifica si el estado actual permite un ataque
     *
     * @return true si se puede atacar en este estado, false si no
     */
    public boolean canAttack() {
        return this == NONE;
    }

    /**
     * Verifica si el estado actual permite un saqueo
     *
     * @return true si se puede saquear en este estado, false si no
     */
    public boolean canLoot() {
        return this == FLAG_CAPTURED;
    }

    /**
     * Verifica si el asedio está activo
     *
     * @return true si el asedio está actualmente activo
     */
    public boolean isActive() {
        return this == ACTIVE || this == FLAG_CAPTURED;
    }
}
