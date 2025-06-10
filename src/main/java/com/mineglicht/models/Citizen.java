package com.mineglicht.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Modelo que representa un ciudadano en el sistema de ciudades CityWars
 * No incluye sistema de roles, solo ciudadanos regulares y propietarios de ciudad
 */
public class Citizen {
    // Atributos principales
    private UUID playerId;
    private String cityName;
    private double taxDebt;
    private LocalDateTime joinDate;
    private LocalDateTime lastTaxPayment;
    private boolean isOwner; // Solo propietario de ciudad, sin roles complejos
    
    // Constructor
    public Citizen(UUID playerId, String cityName) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID no puede ser null");
        this.cityName = Objects.requireNonNull(cityName, "City name no puede ser null");
        this.taxDebt = 0.0;
        this.joinDate = LocalDateTime.now();
        this.lastTaxPayment = null;
        this.isOwner = false; // Por defecto es ciudadano regular
    }
    
    // Información básica
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getCityName() {
        return cityName;
    }
    
    public void setCityName(String cityName) {
        this.cityName = Objects.requireNonNull(cityName, "City name no puede ser null");
    }
    
    // Gestión de impuestos
    public double getTaxDebt() {
        return taxDebt;
    }
    
    public void setTaxDebt(double debt) {
        this.taxDebt = Math.max(0, debt);
    }
    
    public void addTaxDebt(double amount) {
        if (amount > 0) {
            this.taxDebt += amount;
        }
    }
    
    /**
     * Permite al ciudadano pagar sus impuestos
     * @param amount cantidad a pagar
     * @return true si el pago fue exitoso, false si no
     */
    public boolean payTaxes(double amount) {
        if (amount <= 0 || amount > taxDebt) {
            return false;
        }
        
        this.taxDebt -= amount;
        this.lastTaxPayment = LocalDateTime.now();
        
        return true;
    }
    
    public void clearTaxDebt() {
        this.taxDebt = 0.0;
        this.lastTaxPayment = LocalDateTime.now();
    }
    
    public boolean hasTaxDebt() {
        return taxDebt > 0;
    }
    
    // Fechas importantes
    public LocalDateTime getJoinDate() {
        return joinDate;
    }
    
    public LocalDateTime getLastTaxPayment() {
        return lastTaxPayment;
    }
    
    public void setLastTaxPayment(LocalDateTime date) {
        this.lastTaxPayment = date;
    }
    
    // Permisos - Cualquier ciudadano puede expandir la ciudad
    /**
     * Cualquier ciudadano puede expandir la ciudad
     * Las expansiones tienen costo, excepto para administradores del servidor
     */
    public boolean canExpandCity() {
        return true; // Cualquier ciudadano puede expandir
    }
    
    public boolean isOwner() {
        return isOwner;
    }
    
    /**
     * No hay sistema de roles de admin, solo propietarios
     * Este método existe para mantener compatibilidad con la interfaz
     */
    public boolean isAdmin() {
        return false; // No hay sistema de admin según los requisitos
    }
    
    // Método para establecer como propietario (usado internamente)
    public void setOwner(boolean owner) {
        this.isOwner = owner;
    }
    
    // Métodos de utilidad
    public Player getPlayer() {
        // Integración con el sistema de jugadores de Bukkit/Spigot
        return org.bukkit.Bukkit.getPlayer(playerId);
    }
    
    public boolean isOnline() {
        Player player = getPlayer();
        return player != null && player.isOnline();
    }
    
    /**
     * Teletransporta al jugador al centro de la ciudad
     * donde está ubicada la bandera de protección
     * 
     * NOTA: Este método podría moverse a una clase de utilidades
     * como CityTeleportManager o CityUtils para mejor separación de responsabilidades
     
    public void teleportToCityCenter() {
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            // Obtener la ciudad y teletransportar al centro donde está la bandera
            City city = CityManager.getCity(cityName);
            if (city != null) {
                player.teleport(city.getCenterLocation());
            }
        }
    }*/
    
    /**
     * Serializa el ciudadano para guardarlo en base de datos o archivos
     */
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", playerId.toString());
        data.put("cityName", cityName);
        data.put("taxDebt", taxDebt);
        data.put("joinDate", joinDate.toString());
        data.put("lastTaxPayment", lastTaxPayment != null ? lastTaxPayment.toString() : null);
        data.put("isOwner", isOwner);
        return data;
    }
    
    /**
     * Deserializa un ciudadano desde datos guardados
     */
    public static Citizen deserialize(Map<String, Object> data) {
        UUID playerId = UUID.fromString((String) data.get("playerId"));
        String cityName = (String) data.get("cityName");
        
        Citizen citizen = new Citizen(playerId, cityName);
        
        // Manejar valores que pueden ser null o de diferentes tipos
        Object taxDebtObj = data.get("taxDebt");
        if (taxDebtObj instanceof Number) {
            citizen.setTaxDebt(((Number) taxDebtObj).doubleValue());
        }
        
        Object joinDateObj = data.get("joinDate");
        if (joinDateObj instanceof String) {
            citizen.joinDate = LocalDateTime.parse((String) joinDateObj);
        }
        
        Object lastPaymentObj = data.get("lastTaxPayment");
        if (lastPaymentObj instanceof String && !((String) lastPaymentObj).isEmpty()) {
            citizen.lastTaxPayment = LocalDateTime.parse((String) lastPaymentObj);
        }
        
        Object isOwnerObj = data.get("isOwner");
        if (isOwnerObj instanceof Boolean) {
            citizen.setOwner((Boolean) isOwnerObj);
        }
        
        return citizen;
    }
    
    @Override
    public String toString() {
        return String.format("Citizen{playerId=%s, cityName='%s', taxDebt=%.2f, isOwner=%s, joinDate=%s}",
                playerId, cityName, taxDebt, isOwner, joinDate);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Citizen citizen = (Citizen) obj;
        return Objects.equals(playerId, citizen.playerId) && 
               Objects.equals(cityName, citizen.cityName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerId, cityName);
    }
}