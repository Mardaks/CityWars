package com.mineglicht.models;

import org.bukkit.Location;

public class Siege {
    private final String attackingCity;
    private final String targetCity;
    private final Location bannerLocation;
    private final long startTime;
    private boolean flagCaptured;

    public Siege(String attackingCity, String targetCity, Location bannerLocation, long startTime) {
        this.attackingCity = attackingCity;
        this.targetCity = targetCity;
        this.bannerLocation = bannerLocation;
        this.startTime = startTime;
        this.flagCaptured = false;
    }

    public String getAttackingCity() {
        return attackingCity;
    }

    public String getTargetCity() {
        return targetCity;
    }

    public Location getBannerLocation() {
        return bannerLocation;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isFlagCaptured() {
        return flagCaptured;
    }

    public void setFlagCaptured(boolean flagCaptured) {
        this.flagCaptured = flagCaptured;
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}
