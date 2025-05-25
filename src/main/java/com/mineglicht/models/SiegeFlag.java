package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa un estandarte de asedio utilizado para iniciar guerras entre ciudades
 */
public class SiegeFlag {

    private final UUID id;
    private final UUID attackingCityId;
    private final UUID defendingCityId;
    private final UUID attackerPlayerId;
    private final long placedTime;
    private final Location location;
    private long expiryTime;
    private SiegeState state;
    private final Set<UUID> attackingPlayers;
    private final Set<UUID> defendingPlayers;
    private boolean protectorDefeated;
    private boolean flagCaptured;
    private long lootPhaseEndTime;

    /**
     * Constructor para crear un nuevo estandarte de asedio
     *
     * @param attackingCityId UUID de la ciudad atacante
     * @param defendingCityId UUID de la ciudad defensora
     * @param attacker Jugador que inició el ataque
     * @param location Ubicación donde se colocó el estandarte
     * @param durationInSeconds Duración del asedio en segundos
     */
    public SiegeFlag(UUID attackingCityId, UUID defendingCityId, Player attacker, Location location, int durationInSeconds) {
        this.id = UUID.randomUUID();
        this.attackingCityId = attackingCityId;
        this.defendingCityId = defendingCityId;
        this.attackerPlayerId = attacker.getUniqueId();
        this.placedTime = System.currentTimeMillis();
        this.location = location;
        this.expiryTime = placedTime + (durationInSeconds * 1000L);
        this.state = SiegeState.ACTIVE;
        this.attackingPlayers = new HashSet<>();
        this.defendingPlayers = new HashSet<>();
        this.protectorDefeated = false;
        this.flagCaptured = false;

        // Registrar al jugador que inició el ataque
        this.attackingPlayers.add(attacker.getUniqueId());
    }

    /**
     * Verifica si el asedio ha expirado
     *
     * @return true si el tiempo ha expirado, false si no
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    /**
     * Registra un jugador participante en el asedio
     *
     * @param player Jugador a registrar
     * @param isAttacker Si el jugador es atacante (true) o defensor (false)
     */
    public void registerParticipant(Player player, boolean isAttacker) {
        UUID playerId = player.getUniqueId();
        if (isAttacker) {
            attackingPlayers.add(playerId);
        } else {
            defendingPlayers.add(playerId);
        }
    }

    /**
     * Marca al protector como derrotado
     */
    public void setProtectorDefeated() {
        this.protectorDefeated = true;
    }

    /**
     * Inicia la fase de captura de bandera
     */
    public void captureFlag() {
        this.flagCaptured = true;
        this.state = SiegeState.FLAG_CAPTURED;

        // Establecer duración de la fase de saqueo (5 minutos = 300,000 ms)
        this.lootPhaseEndTime = System.currentTimeMillis() + 300_000;
    }

    /**
     * Verifica si la fase de saqueo ha terminado
     *
     * @return true si la fase de saqueo ha terminado, false si no
     */
    public boolean isLootPhaseEnded() {
        return flagCaptured && System.currentTimeMillis() > lootPhaseEndTime;
    }

    /**
     * Calcula el tiempo restante para el asedio en segundos
     *
     * @return Tiempo restante en segundos
     */
    public int getRemainingTimeSeconds() {
        long currentTime = System.currentTimeMillis();

        if (state == SiegeState.FLAG_CAPTURED) {
            return (int)((lootPhaseEndTime - currentTime) / 1000);
        } else {
            return (int)((expiryTime - currentTime) / 1000);
        }
    }

    /**
     * Finaliza el asedio con un estado específico
     *
     * @param endState Estado final del asedio
     */
    public void endSiege(SiegeState endState) {
        this.state = endState;
    }

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public UUID getAttackingCityId() {
        return attackingCityId;
    }

    public UUID getDefendingCityId() {
        return defendingCityId;
    }

    public UUID getAttackerPlayerId() {
        return attackerPlayerId;
    }

    public long getPlacedTime() {
        return placedTime;
    }

    public Location getLocation() {
        return location;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public SiegeState getState() {
        return state;
    }

    public void setState(SiegeState state) {
        this.state = state;
    }

    public Set<UUID> getAttackingPlayers() {
        return new HashSet<>(attackingPlayers);
    }

    public Set<UUID> getDefendingPlayers() {
        return new HashSet<>(defendingPlayers);
    }

    public boolean isProtectorDefeated() {
        return protectorDefeated;
    }

    public boolean isFlagCaptured() {
        return flagCaptured;
    }

    public long getLootPhaseEndTime() {
        return lootPhaseEndTime;
    }
}
