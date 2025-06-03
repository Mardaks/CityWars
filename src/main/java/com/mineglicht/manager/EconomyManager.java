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
    private final Map<UUID, UUID> cityBankAccounts; // Maps city ID to bank account UUID
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
            plugin.getLogger().warning("Cannot create bank for null city");
            return false;
        }
        
        // Check if bank already exists
        if (cityBankAccounts.containsKey(city.getId())) {
            plugin.getLogger().info("Bank account already exists for city: " + city.getName());
            return true;
        }
        
        // Verify GemsEconomy integration is available
        if (gemsEconomy == null || !gemsEconomy.isEnabled()) {
            plugin.getLogger().severe("GemsEconomy integration is not available!");
            return false;
        }
        
        try {
            // Create bank account in GemsEconomy - this returns a UUID
            UUID bankAccountUUID = gemsEconomy.createCityBankAccount(city.getName());
            if (bankAccountUUID == null) {
                plugin.getLogger().warning("Failed to create bank account in GemsEconomy for city: " + city.getName());
                return false;
            }
            
            // Store bank account reference
            cityBankAccounts.put(city.getId(), bankAccountUUID);
            saveEconomyData();
            
            plugin.getLogger().info("Successfully created bank account for city: " + city.getName() + " (Account UUID: " + bankAccountUUID + ")");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Exception occurred while creating bank account for city: " + city.getName());
            plugin.getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        
        UUID accountUUID = cityBankAccounts.get(city.getId());
        if (accountUUID == null) {
            return false;
        }
        
        // Note: GemsEconomy doesn't typically require explicit deletion of UUID accounts
        // as they're virtual. We just remove our reference.
        cityBankAccounts.remove(city.getId());
        saveEconomyData();
        
        plugin.getLogger().info("Removed bank account reference for city: " + city.getName());
        return true;
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
        
        UUID accountUUID = cityBankAccounts.get(city.getId());
        if (accountUUID == null) {
            return 0.0;
        }
        
        return gemsEconomy.getCityBankBalance(accountUUID);
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
        
        UUID accountUUID = cityBankAccounts.get(city.getId());
        if (accountUUID == null) {
            return false;
        }
        
        return gemsEconomy.depositToCityBank(accountUUID, amount);
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
        
        UUID accountUUID = cityBankAccounts.get(city.getId());
        if (accountUUID == null) {
            return false;
        }
        
        return gemsEconomy.withdrawFromCityBank(accountUUID, amount);
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
        
        UUID fromAccount = cityBankAccounts.get(fromCity.getId());
        UUID toAccount = cityBankAccounts.get(toCity.getId());
        
        if (fromAccount == null || toAccount == null) {
            return false;
        }
        
        return gemsEconomy.transferMoney(fromAccount, toAccount, gemsEconomy.getTaxCurrency(), amount);
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
        
        UUID cityAccountUUID = cityBankAccounts.get(city.getId());
        if (cityAccountUUID == null) {
            plugin.getLogger().warning("No bank account found for city: " + city.getName());
            return 0.0;
        }
        
        // Use the GemsEconomy integration's built-in tax collection
        double taxAmount = gemsEconomy.collectTax(player.getUniqueId());
        
        if (taxAmount > 0) {
            // Deposit the collected tax to the city bank
            if (gemsEconomy.depositToCityBank(cityAccountUUID, taxAmount)) {
                plugin.getLogger().info("Collected " + taxAmount + " in taxes from " + player.getName() + " for city " + city.getName());
                return taxAmount;
            } else {
                // If deposit fails, refund the player
                gemsEconomy.addBalance(player.getUniqueId(), gemsEconomy.getTaxCurrency(), taxAmount);
                plugin.getLogger().warning("Failed to deposit tax to city bank, refunded player");
            }
        }
        
        return 0.0;
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
        
        return gemsEconomy.hasBalance(player.getUniqueId(), currency, amount);
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
        
        return gemsEconomy.removeBalance(player.getUniqueId(), currency, amount);
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
        
        return gemsEconomy.addBalance(player.getUniqueId(), currency, amount);
    }
    
    /**
     * Gets a player's balance in a specific currency.
     * 
     * @param player The player
     * @param currency Currency name
     * @return Player's balance
     */
    public double getPlayerBalance(Player player, String currency) {
        if (player == null || currency == null) {
            return 0.0;
        }
        
        return gemsEconomy.getBalance(player.getUniqueId(), currency);
    }
    
    /**
     * Gets a player's balance in the default tax currency.
     * 
     * @param player The player
     * @return Player's balance
     */
    public double getPlayerBalance(Player player) {
        return getPlayerBalance(player, gemsEconomy.getTaxCurrency());
    }
    
    /**
     * Formats money with currency symbol.
     * 
     * @param amount Amount to format
     * @param currency Currency name
     * @return Formatted string
     */
    public String formatMoney(double amount, String currency) {
        return gemsEconomy.formatMoney(amount, currency);
    }
    
    /**
     * Loads economy data from configuration file.
     */
    private void loadEconomyData() {
        cityBankAccounts.clear();
        
        if (!economyFile.exists()) {
            try {
                economyFile.getParentFile().mkdirs();
                economyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create economy.yml file", e);
                return;
            }
        }
        
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);
        
        if (economyConfig.contains("cityBanks")) {
            for (String cityIdStr : economyConfig.getConfigurationSection("cityBanks").getKeys(false)) {
                try {
                    UUID cityId = UUID.fromString(cityIdStr);
                    String accountUUIDStr = economyConfig.getString("cityBanks." + cityIdStr);
                    UUID accountUUID = UUID.fromString(accountUUIDStr);
                    
                    cityBankAccounts.put(cityId, accountUUID);
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
        if (economyConfig == null) {
            economyConfig = new YamlConfiguration();
        }
        
        // Clear existing data
        economyConfig.set("cityBanks", null);
        
        for (Map.Entry<UUID, UUID> entry : cityBankAccounts.entrySet()) {
            economyConfig.set("cityBanks." + entry.getKey().toString(), entry.getValue().toString());
        }
        
        try {
            economyConfig.save(economyFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save economy data", e);
        }
    }
    
    /**
     * Gets the GemsEconomy integration instance.
     * 
     * @return GemsEconomyIntegration instance
     */
    public GemsEconomyIntegration getGemsEconomy() {
        return gemsEconomy;
    }
}