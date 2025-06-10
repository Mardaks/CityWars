package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa una ciudad en el sistema CityWars
 * Contiene toda la información y lógica relacionada con una ciudad
 */
public class City implements ConfigurationSerializable {
    
    // Campos básicos
    private String name;
    private UUID owner;
    private Location centerLocation;
    private LocalDateTime creationDate;
    
    // Gestión de ciudadanos
    private final Set<UUID> citizens;
    private int maxCitizens;
    
    // Gestión económica
    private double funds;
    private double taxRate;
    private double totalTaxDebt;
    private LocalDateTime lastTaxCollection;
    
    // Sistema de niveles
    private int level;
    private int expansionCount;
    
    // Estado de la ciudad
    private CityState state;
    private boolean protectionEnabled;
    
    // Configuración por defecto
    private static final double DEFAULT_TAX_RATE = 0.05; // 5%
    private static final int DEFAULT_MAX_CITIZENS = 10;
    private static final int BASE_LEVEL = 1;
    
    /**
     * Constructor básico para crear una nueva ciudad
     */
    public City(String name, UUID owner, Location centerLocation) {
        this(name, owner, centerLocation, 0.0);
    }
    
    /**
     * Constructor completo para crear una ciudad con fondos iniciales
     */
    public City(String name, UUID owner, Location centerLocation, double initialFunds) {
        this.name = name;
        this.owner = owner;
        this.centerLocation = centerLocation.clone();
        this.funds = initialFunds;
        this.creationDate = LocalDateTime.now();
        this.citizens = ConcurrentHashMap.newKeySet();
        this.taxRate = DEFAULT_TAX_RATE;
        this.level = BASE_LEVEL;
        this.maxCitizens = DEFAULT_MAX_CITIZENS;
        this.expansionCount = 0;
        this.state = CityState.NONE;
        this.protectionEnabled = true;
        this.totalTaxDebt = 0.0;
        this.lastTaxCollection = LocalDateTime.now();
        
        // El owner es automáticamente ciudadano
        this.citizens.add(owner);
    }
    
    // ==================== MÉTODOS DE GESTIÓN DE CIUDADANOS ====================
    
    /**
     * Añade un ciudadano a la ciudad
     */
    public boolean addCitizen(UUID playerId) {
        if (playerId == null || citizens.contains(playerId)) {
            return false;
        }
        
        if (!canAddCitizen()) {
            return false;
        }
        
        return citizens.add(playerId);
    }
    
    /**
     * Remueve un ciudadano de la ciudad
     */
    public boolean removeCitizen(UUID playerId) {
        if (playerId == null || playerId.equals(owner)) {
            return false; // No se puede remover al owner
        }
        
        return citizens.remove(playerId);
    }
    
    /**
     * Verifica si un jugador es ciudadano
     */
    public boolean isCitizen(UUID playerId) {
        return playerId != null && citizens.contains(playerId);
    }
    
    /**
     * Obtiene la lista de ciudadanos
     */
    public List<UUID> getCitizens() {
        return new ArrayList<>(citizens);
    }
    
    /**
     * Obtiene el número de ciudadanos
     */
    public int getCitizenCount() {
        return citizens.size();
    }
    
    /**
     * Verifica si se puede añadir más ciudadanos
     */
    public boolean canAddCitizen() {
        return citizens.size() < maxCitizens;
    }
    
    // ==================== MÉTODOS DE GESTIÓN DE FONDOS ====================
    
    /**
     * Obtiene los fondos actuales de la ciudad
     */
    public double getFunds() {
        return funds;
    }
    
    /**
     * Establece los fondos de la ciudad
     */
    public void setFunds(double funds) {
        this.funds = Math.max(0, funds);
    }
    
    /**
     * Añade fondos a la ciudad
     */
    public boolean addFunds(double amount) {
        if (amount <= 0) {
            return false;
        }
        
        this.funds += amount;
        return true;
    }
    
    /**
     * Deduce fondos de la ciudad
     */
    public boolean deductFunds(double amount) {
        if (amount <= 0 || !canAfford(amount)) {
            return false;
        }
        
        this.funds -= amount;
        return true;
    }
    
    /**
     * Verifica si la ciudad puede permitirse un gasto
     */
    public boolean canAfford(double amount) {
        return amount >= 0 && funds >= amount;
    }
    
    // ==================== MÉTODOS DE GESTIÓN DE NIVELES ====================
    
    /**
     * Obtiene el nivel actual de la ciudad
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Establece el nivel de la ciudad
     */
    public void setLevel(int level) {
        this.level = Math.max(BASE_LEVEL, level);
        updateMaxCitizensForLevel();
    }
    
    /**
     * Verifica si la ciudad puede subir de nivel
     */
    public boolean canLevelUp() {
        return canAfford(getRequiredFundsForNextLevel());
    }
    
    /**
     * Obtiene los fondos requeridos para el siguiente nivel
     */
    public double getRequiredFundsForNextLevel() {
        return (level + 1) * 1000.0; // Fórmula básica: siguiente nivel * 1000
    }
    
    /**
     * Sube el nivel de la ciudad
     */
    public void levelUp() {
        if (canLevelUp()) {
            deductFunds(getRequiredFundsForNextLevel());
            setLevel(level + 1);
        }
    }
    
    /**
     * Actualiza el máximo de ciudadanos basado en el nivel
     */
    private void updateMaxCitizensForLevel() {
        this.maxCitizens = DEFAULT_MAX_CITIZENS + (level - 1) * 5;
    }

    
    // ==================== MÉTODOS DE INFORMACIÓN BÁSICA ====================
    
    /**
     * Obtiene el nombre de la ciudad
     */
    public String getName() {
        return name;
    }
    
    /**
     * Establece el nombre de la ciudad
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Obtiene el propietario de la ciudad
     */
    public UUID getOwner() {
        return owner;
    }
    
    /**
     * Establece el propietario de la ciudad
     */
    public void setOwner(UUID owner) {
        if (this.owner != null) {
            citizens.remove(this.owner);
        }
        this.owner = owner;
        if (owner != null) {
            citizens.add(owner);
        }
    }
    
    /**
     * Obtiene la ubicación central de la ciudad
     */
    public Location getCenterLocation() {
        return centerLocation.clone();
    }
    
    /**
     * Establece la ubicación central de la ciudad
     */
    public void setCenterLocation(Location location) {
        this.centerLocation = location.clone();
    }
    
    /**
     * Obtiene la fecha de creación
     */
    public LocalDateTime getCreationDate() {
        return creationDate;
    }
    
    // ==================== MÉTODOS DE ESTADO ====================
    
    /**
     * Verifica si la ciudad está bajo asedio
     */
    public CityState getState() {
        return getState();
    }
    
    /**
     * Establece el estado de asedio
     */
    public void setState(CityState state) {
        this.state = state;
    }
    
    /**
     * Verifica si la ciudad está protegida
     */
    public boolean isProtected() {
        return protectionEnabled;
    }
    
    /**
     * Establece el estado de protección
     */
    public void setProtected(boolean protectionEnabled) {
        this.protectionEnabled = protectionEnabled;
    }
    
    // ==================== MÉTODOS DE IMPUESTOS ====================
    
    /**
     * Obtiene la tasa de impuestos
     */
    public double getTaxRate() {
        return taxRate;
    }
    
    /**
     * Establece la tasa de impuestos
     */
    public void setTaxRate(double taxRate) {
        this.taxRate = Math.max(0, Math.min(1.0, taxRate)); // Entre 0% y 100%
    }
    
    /**
     * Obtiene la deuda total de impuestos
     */
    public double getTotalTaxDebt() {
        return totalTaxDebt;
    }
    
    /**
     * Obtiene la fecha de la última recolección de impuestos
     */
    public LocalDateTime getLastTaxCollection() {
        return lastTaxCollection;
    }
    
    /**
     * Establece la fecha de la última recolección de impuestos
     */
    public void setLastTaxCollection(LocalDateTime lastTaxCollection) {
        this.lastTaxCollection = lastTaxCollection;
    }
    
    /**
     * Añade deuda de impuestos
     */
    public void addTaxDebt(double debt) {
        this.totalTaxDebt += Math.max(0, debt);
    }
    
    /**
     * Paga la deuda de impuestos
     */
    public boolean payTaxDebt(double amount) {
        if (amount <= 0 || amount > totalTaxDebt) {
            return false;
        }
        
        this.totalTaxDebt -= amount;
        return true;
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si un jugador es el propietario
     */
    public boolean isOwner(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }
    
    
    /**
     * Serializa la ciudad para almacenamiento
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("owner", owner.toString());
        data.put("centerLocation", centerLocation.serialize());
        data.put("creationDate", creationDate.toString());
        data.put("citizens", citizens.stream().map(UUID::toString).toArray(String[]::new));
        data.put("maxCitizens", maxCitizens);
        data.put("funds", funds);
        data.put("taxRate", taxRate);
        data.put("totalTaxDebt", totalTaxDebt);
        data.put("lastTaxCollection", lastTaxCollection.toString());
        data.put("level", level);
        data.put("expansionCount", expansionCount);
        data.put("state", state);
        data.put("protectionEnabled", protectionEnabled);
        return data;
    }
    
    /**
     * Deserializa una ciudad desde datos almacenados
     */
    public static City deserialize(Map<String, Object> data) {
        String name = (String) data.get("name");
        UUID owner = UUID.fromString((String) data.get("owner"));
        Location centerLocation = Location.deserialize((Map<String, Object>) data.get("centerLocation"));
        double funds = (Double) data.get("funds");
        
        City city = new City(name, owner, centerLocation, funds);
        
        // Restaurar otros campos
        city.creationDate = LocalDateTime.parse((String) data.get("creationDate"));
        city.maxCitizens = (Integer) data.get("maxCitizens");
        city.taxRate = (Double) data.get("taxRate");
        city.totalTaxDebt = (Double) data.get("totalTaxDebt");
        city.lastTaxCollection = LocalDateTime.parse((String) data.get("lastTaxCollection"));
        city.level = (Integer) data.get("level");
        city.expansionCount = (Integer) data.get("expansionCount");
        city.state = (CityState) data.get("underSiege");
        city.protectionEnabled = (Boolean) data.get("protectionEnabled");
        
        // Restaurar ciudadanos
        String[] citizenStrings = (String[]) data.get("citizens");
        for (String citizenString : citizenStrings) {
            city.citizens.add(UUID.fromString(citizenString));
        }
        
        return city;
    }
    
    @Override
    public String toString() {
        return String.format("City{name='%s', owner=%s, level=%d, citizens=%d, funds=%.2f}", 
                           name, owner, level, citizens.size(), funds);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        City city = (City) obj;
        return Objects.equals(name, city.name) && Objects.equals(owner, city.owner);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, owner);
    }
}