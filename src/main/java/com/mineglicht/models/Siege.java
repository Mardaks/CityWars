package com.mineglicht.models;

import org.bukkit.Location;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Modelo que representa un asedio entre ciudades en CityWars
 * Gestiona todo el ciclo de vida del asedio: declaración, combate, saqueo y finalización
 */
public class Siege {
    
    // Atributos principales
    private String attackerCity;
    private String defenderCity;
    private Location siegeLocation; // Ubicación del estandarte de asedio
    private Location defenderFlagLocation; // Ubicación de la bandera defensora
    
    // Estado y tiempo
    private SiegeStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationInMinutes;
    private SiegeResult result;
    
    // Participantes
    private Set<UUID> attackers;
    private Set<UUID> defenders;
    
    // Fase de saqueo
    private boolean inLootPhase;
    private LocalDateTime lootStartTime;
    private int lootDurationMinutes;
    private double lootAmount;
    private boolean rewardsDistributed;
    
    // Validación
    private String invalidReason;
    
    // Constructores
    public Siege(String attackerCity, String defenderCity, Location siegeLocation) {
        this(attackerCity, defenderCity, siegeLocation, 60); // 60 minutos por defecto
    }
    
    public Siege(String attackerCity, String defenderCity, Location siegeLocation, int duration) {
        this.attackerCity = Objects.requireNonNull(attackerCity, "Attacker city no puede ser null");
        this.defenderCity = Objects.requireNonNull(defenderCity, "Defender city no puede ser null");
        this.siegeLocation = Objects.requireNonNull(siegeLocation, "Siege location no puede ser null");
        this.durationInMinutes = duration;
        
        // Inicialización de estado
        this.status = SiegeStatus.PENDING;
        this.startTime = LocalDateTime.now();
        this.result = null;
        
        // Inicialización de participantes
        this.attackers = new HashSet<>();
        this.defenders = new HashSet<>();
        
        // Inicialización de saqueo
        this.inLootPhase = false;
        this.lootDurationMinutes = 30; // 30 minutos de saqueo por defecto
        this.lootAmount = 0.0;
        this.rewardsDistributed = false;
        
        this.invalidReason = "";
    }
    
    // Métodos de Estado del Asedio
    public SiegeStatus getStatus() {
        return status;
    }
    
    public void setStatus(SiegeStatus status) {
        this.status = Objects.requireNonNull(status, "Status no puede ser null");
    }
    
    public boolean isActive() {
        return status == SiegeStatus.ACTIVE;
    }
    
    public boolean isFinished() {
        return status == SiegeStatus.FINISHED || status == SiegeStatus.CANCELLED;
    }
    
    public boolean isInLootPhase() {
        return inLootPhase;
    }
    
    // Información de ciudades
    public String getAttackerCity() {
        return attackerCity;
    }
    
    public String getDefenderCity() {
        return defenderCity;
    }
    
    public Location getSiegeLocation() {
        return siegeLocation;
    }
    
    public Location getDefenderFlagLocation() {
        return defenderFlagLocation;
    }
    
    public void setDefenderFlagLocation(Location location) {
        this.defenderFlagLocation = location;
    }
    
    // Gestión de tiempo
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public long getDurationInMinutes() {
        return durationInMinutes;
    }
    
    public long getRemainingTimeInMinutes() {
        if (endTime == null) {
            LocalDateTime calculatedEndTime = startTime.plusMinutes(durationInMinutes);
            return ChronoUnit.MINUTES.between(LocalDateTime.now(), calculatedEndTime);
        }
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), endTime);
    }
    
    public boolean hasExpired() {
        return getRemainingTimeInMinutes() <= 0;
    }
    
    // Métodos de Participantes
    public Set<UUID> getAttackers() {
        return new HashSet<>(attackers);
    }
    
    public Set<UUID> getDefenders() {
        return new HashSet<>(defenders);
    }
    
    public boolean addAttacker(UUID playerId) {
        if (playerId == null || defenders.contains(playerId)) {
            return false;
        }
        return attackers.add(playerId);
    }
    
    public boolean addDefender(UUID playerId) {
        if (playerId == null || attackers.contains(playerId)) {
            return false;
        }
        return defenders.add(playerId);
    }
    
    public boolean removeParticipant(UUID playerId) {
        return attackers.remove(playerId) || defenders.remove(playerId);
    }
    
    public boolean isParticipant(UUID playerId) {
        return attackers.contains(playerId) || defenders.contains(playerId);
    }
    
    public boolean isAttacker(UUID playerId) {
        return attackers.contains(playerId);
    }
    
    public boolean isDefender(UUID playerId) {
        return defenders.contains(playerId);
    }
    
    public int getAttackerCount() {
        return attackers.size();
    }
    
    public int getDefenderCount() {
        return defenders.size();
    }
    
    // Métodos de Saqueo
    public void startLootPhase() {
        this.inLootPhase = true;
        this.lootStartTime = LocalDateTime.now();
        this.status = SiegeStatus.LOOTING;
    }
    
    public void endLootPhase() {
        this.inLootPhase = false;
        this.status = SiegeStatus.FINISHED;
        this.endTime = LocalDateTime.now();
    }
    
    public long getLootTimeRemaining() {
        if (!inLootPhase || lootStartTime == null) {
            return 0;
        }
        LocalDateTime lootEndTime = lootStartTime.plusMinutes(lootDurationMinutes);
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), lootEndTime);
    }
    
    public void setLootAmount(double amount) {
        this.lootAmount = Math.max(0, amount);
    }
    
    public boolean canLoot() {
        return inLootPhase && getLootTimeRemaining() > 0;
    }
    
    public String getStatusMessage() {
        switch (status) {
            case PENDING:
                return "Asedio pendiente de iniciar";
            case ACTIVE:
                return String.format("Asedio activo - %d minutos restantes", getRemainingTimeInMinutes());
            case LOOTING:
                return String.format("Fase de saqueo - %d minutos restantes", getLootTimeRemaining());
            case FINISHED:
                return "Asedio finalizado - " + (result != null ? result.getDisplayName() : "Sin resultado");
            case CANCELLED:
                return "Asedio cancelado";
            default:
                return "Estado desconocido";
        }
    }
    
    @Override
    public String toString() {
        return String.format("Siege{attacker='%s', defender='%s', status=%s, participants=%d}",
                attackerCity, defenderCity, status, getAttackerCount() + getDefenderCount());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Siege siege = (Siege) obj;
        return Objects.equals(attackerCity, siege.attackerCity) &&
               Objects.equals(defenderCity, siege.defenderCity) &&
               Objects.equals(startTime, siege.startTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(attackerCity, defenderCity, startTime);
    }
}