package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class City {
    
    private final String name;
    private final UUID ownerUUID;
    private final World world;
    private Vector minPoint;
    private Vector maxPoint;
    private final Set<UUID> citizens;
    private double bankBalance;
    private long lastTaxCollection;
    private int level;
    
    public City(String name, UUID ownerUUID, World world, Vector minPoint, Vector maxPoint) {
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.citizens = new HashSet<>();
        this.bankBalance = 0.0;
        this.lastTaxCollection = System.currentTimeMillis();
        this.level = 1;
        
        // Add owner as first citizen
        citizens.add(ownerUUID);
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
    }
    
    public Vector getMaxPoint() {
        return maxPoint;
    }
    
    public void setMaxPoint(Vector maxPoint) {
        this.maxPoint = maxPoint;
    }
    
    public Set<UUID> getCitizens() {
        return new HashSet<>(citizens); // Return a copy to prevent external modification
    }
    
    public boolean addCitizen(UUID playerUUID) {
        return citizens.add(playerUUID);
    }
    
    public boolean removeCitizen(UUID playerUUID) {
        // Cannot remove the owner
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
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        City city = (City) obj;
        return name.equals(city.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}