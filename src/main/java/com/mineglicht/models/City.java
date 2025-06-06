package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class City {

    // Atributos basicos
    private final UUID id;
    private String name;
    private World world;

    // Limites de la region
    private Vector minPoint;
    private Vector maxPoint;

    // Lista de ciudadanos
    private final Set<UUID> citizens;

    // Datos economicos
    private double bankBalance;
    private double taxRate;

    // Centro de la ciudad (se calcula a partir de minPoint y maxPoint)
    private Location center;

    // Estado de la ciudad
    private SiegeState siegeState;

    // Nivel de la ciudad (inicia en 1 pero va subiendo)
    private int level;

    // Constructor principal para crear nueva ciudad
    public City(String name, World world, Vector minPoint, Vector maxPoint, double taxRate) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.world = world;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.citizens = new HashSet<>();
        this.bankBalance = 0.0;
        this.taxRate = 0.0;
        this.center = calculateCenter(); // Calcular centro basado en min y max points
        this.siegeState = SiegeState.NONE; // Estado inicial sin asedio
        this.level = 1;
    }

    // Constructor para cargar ciudad desde archivo de configuración
    public City(UUID id, String name, World world, Vector minPoint, Vector maxPoint, Location center, double taxRate) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.citizens = new HashSet<>();
        this.bankBalance = 0.0;
        this.taxRate = taxRate;
        this.center = center;
        this.siegeState = SiegeState.NONE; // Estado inicial sin asedio
        this.level = 1;
    }

    /**
     * Calculamos el centro de la ciudad
     *
     * @return Localizacion del centro de la ciudad
     */
    private Location calculateCenter() {
        if (world == null || minPoint == null || maxPoint == null) {
            return null;
        }
        
        double centerX = (minPoint.getX() + maxPoint.getX()) / 2;
        double centerY = (minPoint.getY() + maxPoint.getY()) / 2;
        double centerZ = (minPoint.getZ() + maxPoint.getZ()) / 2;
        
        return new Location(world, centerX, centerY, centerZ);
    }

    // Getters básicos
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Vector getMinPoint() {
        return minPoint;
    }

    public void setMinPoint(Vector minPoint) {
        this.minPoint = minPoint;
        this.center = calculateCenter(); // Recalcular centro
    }

    public Vector getMaxPoint() {
        return maxPoint;
    }

    public void setMaxPoint(Vector maxPoint) {
        this.maxPoint = maxPoint;
        this.center = calculateCenter(); // Recalcular centro
    }

    public double getTaxRate() {
        return this.taxRate;
    }
    
    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    // Métodos para ciudadanos
    public Set<UUID> getCitizens() {
        return new HashSet<>(citizens);
    }

    public boolean addCitizen(UUID playerUUID) {
        return citizens.add(playerUUID);
    }

    public boolean removeCitizen(UUID playerUUID) {
        return citizens.remove(playerUUID);
    }

    public boolean isCitizen(UUID playerUUID) {
        return citizens.contains(playerUUID);
    }

    public int getCitizenCount() {
        return citizens.size();
    }

    // Métodos económicos
    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    // Métodos para estado de asedio
    public SiegeState getSiegeState() {
        return siegeState != null ? siegeState : SiegeState.NONE;
    }

    public void setSiegeState(SiegeState siegeState) {
        this.siegeState = siegeState;
    }

    // Métodos de utilidad para asedio
    public boolean isUnderSiege() {
        return siegeState == SiegeState.ACTIVE || siegeState == SiegeState.FLAG_CAPTURED;
    }

    public boolean canBeAttacked() {
        return siegeState == SiegeState.NONE || siegeState == SiegeState.DEFENDED;
    }

    // Métodos de utilidad
    public boolean isInCity(Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= Math.min(minPoint.getX(), maxPoint.getX()) && x <= Math.max(minPoint.getX(), maxPoint.getX())
                && y >= Math.min(minPoint.getY(), maxPoint.getY()) && y <= Math.max(minPoint.getY(), maxPoint.getY())
                && z >= Math.min(minPoint.getZ(), maxPoint.getZ()) && z <= Math.max(minPoint.getZ(), maxPoint.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        City city = (City) obj;
        return id.equals(city.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}