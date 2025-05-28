package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class City {

    private final UUID id;
    private final String name;
    private final UUID ownerUUID;
    private final World world;
    private Vector minPoint;
    private Vector maxPoint;
    private final Set<UUID> citizens;
    private final Set<UUID> admins;
    private double bankBalance;
    private long lastTaxCollection;
    private int level;
    private Location center;
    private final Map<CityFlag, Boolean> flags;

    // Constructor principal para crear nueva ciudad
    public City(String name, UUID ownerUUID, World world, Vector minPoint, Vector maxPoint) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.citizens = new HashSet<>();
        this.admins = new HashSet<>();
        this.bankBalance = 0.0;
        this.lastTaxCollection = System.currentTimeMillis();
        this.level = 1;
        this.flags = new EnumMap<>(CityFlag.class);
        
        // Calcular centro basado en min y max points
        this.center = calculateCenter();
        
        // Inicializar flags con valores por defecto
        initializeDefaultFlags();

        // Add owner as first citizen
        citizens.add(ownerUUID);
    }

    // Constructor para cargar ciudad desde archivo de configuración
    public City(UUID id, String name, UUID ownerUUID, World world, Vector minPoint, Vector maxPoint, Location center) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.center = center;
        this.citizens = new HashSet<>();
        this.admins = new HashSet<>();
        this.bankBalance = 0.0;
        this.lastTaxCollection = System.currentTimeMillis();
        this.level = 1;
        this.flags = new EnumMap<>(CityFlag.class);
        
        // Inicializar flags con valores por defecto
        initializeDefaultFlags();
        
        // Add owner as first citizen
        citizens.add(ownerUUID);
    }

    private void initializeDefaultFlags() {
        for (CityFlag flag : CityFlag.values()) {
            flags.put(flag, flag.getDefaultValue());
        }
    }

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

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public World getWorld() {
        return world;
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
        if (playerUUID.equals(ownerUUID)) {
            return false;
        }
        return citizens.remove(playerUUID);
    }

    public boolean isCitizen(UUID playerUUID) {
        return citizens.contains(playerUUID);
    }

    public int getCitizenCount() {
        return citizens.size();
    }

    // Métodos para administradores
    public Set<UUID> getAdminIds() {
        return new HashSet<>(admins);
    }

    public void setAdminIds(Set<UUID> adminIds) {
        this.admins.clear();
        this.admins.addAll(adminIds);
    }

    public boolean addAdmin(UUID playerUUID) {
        return admins.add(playerUUID);
    }

    public boolean removeAdmin(UUID playerUUID) {
        return admins.remove(playerUUID);
    }

    public boolean isAdmin(UUID playerUUID) {
        return admins.contains(playerUUID);
    }

    // Métodos para flags
    public boolean hasFlag(CityFlag flag) {
        return flags.getOrDefault(flag, flag.getDefaultValue());
    }

    public void setFlag(CityFlag flag, boolean value) {
        flags.put(flag, value);
    }

    public Map<CityFlag, Boolean> getAllFlags() {
        return new EnumMap<>(flags);
    }

    // Métodos económicos
    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public long getLastTaxCollection() {
        return lastTaxCollection;
    }

    public void setLastTaxCollection(long lastTaxCollection) {
        this.lastTaxCollection = lastTaxCollection;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
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