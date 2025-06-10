package com.mineglicht.models;

/**
 * Enumeración que representa los diferentes estados posibles de un asedio
 */
public enum SiegeStatus {
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
    PENDING,

    /**
     * El asedio ha sido defendido - los defensores ganaron
     */
    FINISHED,
    /**
     * El asedio ha sido cancelado administrativamente
     */
    CANCELLED,

    LOOTING;

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
        return this == LOOTING;
    }

    /**
     * Verifica si el asedio está activo
     *
     * @return true si el asedio está actualmente activo
     */
    public boolean isActive() {
        return this == ACTIVE || this == LOOTING || this == PENDING;
    }
}
