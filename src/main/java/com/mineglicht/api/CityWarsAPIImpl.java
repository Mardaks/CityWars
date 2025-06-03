package com.mineglicht.api;

import com.mineglicht.models.Citizen;
import com.mineglicht.models.City;
import com.mineglicht.models.SiegeState;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CityWarsAPIImpl implements CityWarsAPI {
    @Override
    public boolean createCity(String name, UUID mayorUuid, Location location) {
        return false;
    }

    @Override
    public boolean deleteCity(String cityName) {
        return false;
    }

    @Override
    public City getCity(String cityName) {
        return null;
    }

    @Override
    public List<City> getAllCities() {
        return List.of();
    }

    @Override
    public boolean cityExists(String cityName) {
        return false;
    }

    @Override
    public City getCityAt(Location location) {
        return null;
    }

    @Override
    public boolean expandCity(String cityName, String direction, int blocks) {
        return false;
    }

    @Override
    public double getCityBankBalance(String cityName) {
        return 0;
    }

    @Override
    public boolean addCitizen(UUID playerUuid, String cityName) {
        return false;
    }

    @Override
    public boolean removeCitizen(UUID playerUuid) {
        return false;
    }

    @Override
    public String getPlayerCity(UUID playerUuid) {
        return "";
    }

    @Override
    public Citizen getCitizen(UUID playerUuid) {
        return null;
    }

    @Override
    public List<Citizen> getCitizens(String cityName) {
        return List.of();
    }

    @Override
    public boolean isCitizen(UUID playerUuid) {
        return false;
    }

    @Override
    public boolean isMayor(UUID playerUuid, String cityName) {
        return false;
    }

    @Override
    public boolean isAssistant(UUID playerUuid, String cityName) {
        return false;
    }

    @Override
    public boolean startSiege(String attackerCity, String defenderCity, Location flagLocation, UUID attackerUuid) {
        return false;
    }

    @Override
    public boolean endSiege(String cityName, String reason) {
        return false;
    }

    @Override
    public SiegeState getSiegeState(String cityName) {
        return null;
    }

    @Override
    public boolean isUnderSiege(String cityName) {
        return false;
    }

    @Override
    public boolean isAttacking(String cityName) {
        return false;
    }

    @Override
    public int getSiegeTimeRemaining(String cityName) {
        return 0;
    }

    @Override
    public boolean hasSiegeCooldown(String attackerCity, String defenderCity) {
        return false;
    }

    @Override
    public int getSiegeCooldownTime(String attackerCity, String defenderCity) {
        return 0;
    }

    @Override
    public double getPlayerBalance(UUID playerUuid) {
        return 0;
    }

    @Override
    public boolean depositToCityBank(String cityName, double amount) {
        return false;
    }

    @Override
    public boolean withdrawFromCityBank(String cityName, double amount) {
        return false;
    }

    @Override
    public double collectCityTaxes(String cityName) {
        return 0;
    }

    @Override
    public double forceGlobalTaxCollection() {
        return 0;
    }

    @Override
    public boolean isProtected(Location location) {
        return false;
    }

    @Override
    public boolean canPerformAction(Player player, Location location, String action) {
        return false;
    }

    @Override
    public boolean isPvPActive(Location location) {
        return false;
    }

    @Override
    public int getTotalCities() {
        return 0;
    }

    @Override
    public int getTotalCitizens() {
        return 0;
    }

    @Override
    public int getActiveSieges() {
        return 0;
    }

    @Override
    public Map<String, Object> getServerStats() {
        return Map.of();
    }

    @Override
    public List<City> getCityRankingByWealth(int limit) {
        return List.of();
    }

    @Override
    public List<City> getCityRankingByPopulation(int limit) {
        return List.of();
    }

    @Override
    public void registerEventListener(Object listener) {

    }

    @Override
    public void unregisterEventListener(Object listener) {

    }

    @Override
    public boolean reloadConfig() {
        return false;
    }

    @Override
    public Object getConfigValue(String path) {
        return null;
    }

    @Override
    public boolean setConfigValue(String path, Object value) {
        return false;
    }

    @Override
    public boolean saveConfig() {
        return false;
    }

    @Override
    public String getAPIVersion() {
        return "";
    }

    @Override
    public boolean isAPIAvailable() {
        return false;
    }
}
