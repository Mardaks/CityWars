package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.integration.GemsEconomyIntegration;
import com.mineglicht.models.City;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages all economy-related operations including city bank accounts,
 * tax collection, and integration with GemsEconomy.
 */
public class EconomyManager {
    private final cityWars plugin;
    private final GemsEconomyIntegration gemsEconomy;
    private final Map<UUID, String> cityBankAccounts; // Maps city ID to bank account name
    private final File economyFile;
    private FileConfiguration economyConfig;

    public EconomyManager(cityWars plugin) {
        this.plugin = plugin;
        this.gemsEconomy = new GemsEconomyIntegration(plugin);
        this.cityBankAccounts = new HashMap<>();
        this.economyFile = new File(plugin.getDataFolder(), "economy.yml");
        
        loadEconomyData();
    }
    
    /**
     * Creates a bank account for a city.
     * 
     * @param city The city
     * @return true if bank was created successfully
     */
    public boolean createCityBank(City city) {
        if (city == null) {
            return false;
        }
        
        // Check if bank already exists
        if (cityBankAccounts.containsKey(city.getId())) {
            return true;
        }
        
        // Generate bank account name
        String accountName = "city_" + city.getName().toLowerCase().replace(" ", "_") + "_" + city.getId().toString().substring(0, 8);
        
        // Create bank account in GemsEconomy
        boolean created = gemsEconomy.createBankAccount(accountName);
        if (!created) {
            return false;
        }
        
        // Store bank account reference
        cityBankAccounts.put(city.getId(), accountName);
        saveEconomyData();
        
        return true;
    }
    
    /**
     * Deletes a city's bank account.
     * 
     * @param city The city
     * @return true if deletion was successful
     */
    public boolean deleteCityBank(City city) {
        if (city == null) {
            return false;
        }
        
        String accountName = cityBankAccounts.get(city.getId());
        if (accountName == null) {
            return false;
        }
        
        // Delete bank account in GemsEconomy
        boolean deleted = gemsEconomy.deleteBankAccount(accountName);
        
        // Remove account reference regardless of GemsEconomy result
        cityBankAccounts.remove(city.getId());
        saveEconomyData();
        
        return deleted;
    }
    
    /**
     * Gets the balance of a city bank.
     * 
     * @param city The city
     * @return City bank balance
     */
    public double getCityBankBalance(City city) {
        if (city == null) {
            return 0.0;
        }
        
        String accountName = cityBankAccounts.get(city.getId());
        if (accountName == null) {
            return 0.0;
        }
        
        return gemsEconomy.getBankBalance(accountName);
    }
    
    /**
     * Deposits funds into a city bank.
     * 
     * @param city The city
     * @param amount Amount to deposit
     * @return true if deposit was successful
     */
    public boolean depositCityBank(City city, double amount) {
        if (city == null || amount <= 0) {
            return false;
        }
        
        String accountName = cityBankAccounts.get(city.getId());
        if (accountName == null) {
            return false;
        }
        
        return gemsEconomy.depositBank(accountName, amount);
    }
    
    /**
     * Withdraws funds from a city bank.
     * 
     * @param city The city
     * @param amount Amount to withdraw
     * @return true if withdrawal was successful
     */
    public boolean withdrawCityBank(City city, double amount) {
        if (city == null || amount <= 0) {
            return false;
        }
        
        String accountName = cityBankAccounts.get(city.getId());
        if (accountName == null) {
            return false;
        }
        
        return gemsEconomy.withdrawBank(accountName, amount);
    }
    
    /**
     * Transfers funds from one city bank to another.
     * 
     * @param fromCity Source city
     * @param toCity Destination city
     * @param amount Amount to transfer
     * @return true if transfer was successful
     */
    public boolean transferCityBankFunds(City fromCity, City toCity, double amount) {
        if (fromCity == null || toCity == null || amount <= 0) {
            return false;
        }
        
        String fromAccount = cityBankAccounts.get(fromCity.getId());
        String toAccount = cityBankAccounts.get(toCity.getId());
        
        if (fromAccount == null || toAccount == null) {
            return false;
        }
        
        return gemsEconomy.transferBankToBank(fromAccount, toAccount, amount);
    }
    
    /**
     * Collects tax from a player and deposits it to their city's bank.
     * 
     * @param player The player
     * @param city The player's city
     * @return Amount of tax collected
     */
    public double collectTax(Player player, City city) {
        if (player == null || city == null) {
            return 0.0;
        }
        
        // Calculate 18% tax
        double playerBalance = gemsEconomy.getPlayerBalance(player);
        double taxAmount = playerBalance * 0.18;
        
        // Don't collect tax if amount is negligible
        if (taxAmount < 0.01) {
            return 0.0;
        }
        
        // Withdraw from player
        if (!gemsEconomy.withdrawPlayer(player, taxAmount)) {
            return 0.0;
        }
        
        // Deposit to city bank
        String accountName = cityBankAccounts.get(city.getId());
        if (accountName == null || !gemsEconomy.depositBank(accountName, taxAmount)) {
            // Refund player if deposit fails
            gemsEconomy.depositPlayer(player, taxAmount);
            return 0.0;
        }
        
        return taxAmount;
    }
    
    /**
     * Checks if a player has enough of a specific currency.
     * 
     * @param player The player
     * @param currency Currency name
     * @param amount Required amount
     * @return true if player has enough
     */
    public boolean hasCurrency(Player player, String currency, double amount) {
        if (player == null || currency == null) {
            return false;
        }
        
        return gemsEconomy.hasCurrency(player, currency, amount);
    }
    
    /**
     * Withdraws a specific currency from a player.
     * 
     * @param player The player
     * @param currency Currency name
     * @param amount Amount to withdraw
     * @return true if withdrawal was successful
     */
    public boolean withdrawCurrency(Player player, String currency, double amount) {
        if (player == null || currency == null || amount <= 0) {
            return false;
        }
        
        return gemsEconomy.withdrawCurrency(player, currency, amount);
    }
    
    /**
     * Deposits a specific currency to a player.
     * 
     * @param player The player
     * @param currency Currency name
     * @param amount Amount to deposit
     * @return true if deposit was successful
     */
    public boolean depositCurrency(Player player, String currency, double amount) {
        if (player == null || currency == null || amount <= 0) {
            return false;
        }
        
        return gemsEconomy.depositCurrency(player, currency, amount);
    }
    
    /**
     * Loads economy data from configuration file.
     */
    private void loadEconomyData() {
        cityBankAccounts.clear();
        
        if (!economyFile.exists()) {
            plugin.saveResource("economy.yml", false);
        }
        
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);
        
        if (economyConfig.contains("cityBanks")) {
            for (String cityIdStr : economyConfig.getConfigurationSection("cityBanks").getKeys(false)) {
                try {
                    UUID cityId = UUID.fromString(cityIdStr);
                    String accountName = economyConfig.getString("cityBanks." + cityIdStr);
                    
                    cityBankAccounts.put(cityId, accountName);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load city bank: " + cityIdStr, e);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + cityBankAccounts.size() + " city bank accounts");
    }
    
    /**
     * Saves economy data to configuration file.
     */
    public void saveEconomyData() {
        economyConfig = new YamlConfiguration();
        
        for (Map.Entry<UUID, String> entry : cityBankAccounts.entrySet()) {
            economyConfig.set("cityBanks." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            economyConfig.save(economyFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save economy data", e);
        }
    }
}
