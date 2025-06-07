package com.mineglicht.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class Siege {

    private City attackedCity; // Ciudad bajo ataque
    private Set<UUID> attackers; // Jugadores atacantes (usando UUID para identificarlos)
    private Set<UUID> defenders; // Jugadores defensores
    private boolean isActive; // Si el asedio está en curso
    private boolean isLooting; // Si el saqueo está activo (cuando la bandera de protección es destruida)
    private LocalDateTime siegeStartTime; // Hora de inicio del asedio
    private LocalDateTime siegeEndTime; // Hora estimada de finalización
    private ItemStack siegeFlag; // Bandera de asedio (colocada por los atacantes)
    private ItemStack protectionFlag; // Bandera de protección (Estandarte)
    private int siegeDuration; // Duración total del asedio (en segundos)
    private SiegeState state; //
    private int lootingDuration; // Duración del saqueo (en segundos)

    // Constructor
    public Siege(City attackedCity, ItemStack siegeFlag, ItemStack protectionFlag) {
        this.attackedCity = attackedCity;
        this.attackers = new HashSet<>();
        this.defenders = new HashSet<>();
        this.isActive = false;
        this.isLooting = false;
        this.siegeFlag = siegeFlag;
        this.protectionFlag = protectionFlag;
        this.siegeDuration = 600; // Ejemplo: 10 minutos para el asedio
        this.lootingDuration = 300; // Ejemplo: 5 minutos para el saqueo
    }

    public Siege(UUID randomUUID, City attackedCity, Set<UUID> attackers, LocalDateTime siegeStartTime,
            SiegeState state) {
        this.attackedCity = attackedCity;
        this.attackers = attackers;
        this.siegeStartTime = siegeStartTime;
        this.state = state;
    }

    // Métodos de acceso (Getters y Setters)
    public City getAttackedCity() {
        return attackedCity;
    }

    public Set<UUID> getAttackers() {
        return attackers;
    }

    public Set<UUID> getDefenders() {
        return defenders;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isLooting() {
        return isLooting;
    }

    public LocalDateTime getSiegeStartTime() {
        return siegeStartTime;
    }

    public LocalDateTime getSiegeEndTime() {
        return siegeEndTime;
    }

    public ItemStack getSiegeFlag() {
        return siegeFlag;
    }

    public ItemStack getProtectionFlag() {
        return protectionFlag;
    }

    public int getSiegeDuration() {
        return siegeDuration;
    }

    public int getLootingDuration() {
        return lootingDuration;
    }

    // Métodos para actualizar el estado del asedio

    // Inicia el asedio
    public void iniciarAsedio() {
        this.isActive = true;
        this.siegeStartTime = LocalDateTime.now();
        // siegeDuration está en segundos
        this.siegeEndTime = this.siegeStartTime.plusSeconds(siegeDuration);
    }

    // Termina el asedio
    public void terminarAsedio() {
        this.isActive = false;
        this.siegeEndTime = LocalDateTime.now();
    }

    // Activa el modo saqueo
    public void iniciarSaqueo() {
        this.isLooting = true;
    }

    // Finaliza el saqueo
    public void terminarSaqueo() {
        this.isLooting = false;
    }

    // Agregar atacantes y defensores
    public void addAtacante(UUID attackerId) {
        this.attackers.add(attackerId);
    }

    public void addDefensor(UUID defenderId) {
        this.defenders.add(defenderId);
    }

    // Remover atacantes y defensores
    public void removerAtacante(UUID attackerId) {
        this.attackers.remove(attackerId);
    }

    public void removerDefensor(UUID defenderId) {
        this.defenders.remove(defenderId);
    }

    // Verificar si el asedio puede comenzar (por ejemplo, si la ciudad tiene
    // suficiente defensa)
    public boolean puedeIniciarAsedio() {
        return attackers.size() > 0 && defenders.size() > 0; // Se puede ajustar según las reglas
    }

    // Actualizar el estado de las banderas y la protección
    public void checkFlagsStatus() {
        if (protectionFlag == null) { // Si la bandera de protección ha sido destruida
            iniciarSaqueo();
        }
    }
}