package com.mineglicht.manager;

import com.mineglicht.cityWars;
import com.mineglicht.models.City;
import com.mineglicht.manager.CitizenManager;
import me.xanium.gemseconomy.api.GemsEconomyAPI;
import me.xanium.gemseconomy.economy.Economy;
import me.xanium.gemseconomy.account.Account;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Gestor de economía para el sistema CityWars.
 * Maneja todas las transacciones económicas entre ciudadanos, ciudades y fondos.
 */
public class EconomyManager {
    
    private final cityWars plugin;
    private final Logger logger;
    private final GemsEconomyAPI gemsAPI;
    private final CitizenManager citizenManager;
    
    // Prefijo para identificar cuentas de ciudades
    private static final String CITY_ACCOUNT_PREFIX = "city_";
    
    public EconomyManager(Plugin plugin, CitizenManager citizenManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.citizenManager = citizenManager;
        this.gemsAPI = new GemsEconomyAPI();
        
        // Verificar que GemsEconomy esté disponible
        if (!Bukkit.getPluginManager().isPluginEnabled("GemsEconomy")) {
            throw new IllegalStateException("GemsEconomy plugin no está disponible!");
        }
        
        logger.info("EconomyManager inicializado correctamente con GemsEconomy");
    }
    
    /**
     * Crea el fondo inicial de la ciudad.
     * 
     * @param city Ciudad para la cual crear el fondo
     * @param initialBalance Balance inicial del fondo
     * @return true si el fondo fue creado exitosamente
     */
    public boolean createCityFund(City city, double initialBalance) {
        if (city == null || initialBalance < 0) {
            logger.warning("Parámetros inválidos para crear fondo de ciudad");
            return false;
        }
        
        try {
            String accountName = CITY_ACCOUNT_PREFIX + city.getName().toLowerCase();
            Economy economy = gemsAPI.getEconomy("gems"); // Usar la economía por defecto "gems"
            
            if (economy == null) {
                logger.severe("No se pudo obtener la economía de GemsEconomy");
                return false;
            }
            
            // Crear cuenta para la ciudad si no existe
            Account cityAccount = economy.getAccount(accountName);
            if (cityAccount == null) {
                cityAccount = economy.createAccount(accountName);
                if (cityAccount == null) {
                    logger.severe("No se pudo crear la cuenta para la ciudad: " + city.getName());
                    return false;
                }
            }
            
            // Establecer el balance inicial
            cityAccount.setBalance(initialBalance);
            
            logger.info("Fondo creado para la ciudad " + city.getName() + " con balance inicial: " + initialBalance);
            return true;
            
        } catch (Exception e) {
            logger.severe("Error al crear fondo para la ciudad " + city.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cobra impuestos a los ciudadanos y los deposita en el fondo de la ciudad.
     * 
     * @param city Ciudad que recibe los impuestos
     * @param taxRate Tasa de impuestos (como decimal, ej: 0.18 para 18%)
     * @return true si los impuestos fueron cobrados exitosamente
     */
    public boolean collectTaxes(City city, double taxRate) {
        if (city == null || taxRate < 0 || taxRate > 1) {
            logger.warning("Parámetros inválidos para cobrar impuestos");
            return false;
        }
        
        try {
            Set<UUID> citizens = citizenManager.getCitizens(city);
            double totalTaxesCollected = 0.0;
            int citizensTaxed = 0;
            
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) {
                logger.severe("No se pudo obtener la economía de GemsEconomy");
                return false;
            }
            
            // Cobrar impuestos a cada ciudadano
            for (UUID citizenId : citizens) {
                Player player = Bukkit.getPlayer(citizenId);
                if (player == null) continue; // Solo cobrar a jugadores conectados
                
                Account citizenAccount = economy.getAccount(player.getName());
                if (citizenAccount == null) continue;
                
                double currentBalance = citizenAccount.getBalance();
                double taxAmount = currentBalance * taxRate;
                
                if (taxAmount > 0 && citizenAccount.withdraw(taxAmount)) {
                    totalTaxesCollected += taxAmount;
                    citizensTaxed++;
                    
                    // Notificar al jugador
                    player.sendMessage("§e[CityWars] §7Se han cobrado §c$" + String.format("%.2f", taxAmount) + 
                                     " §7en impuestos para la ciudad §a" + city.getName());
                }
            }
            
            // Depositar impuestos en el fondo de la ciudad
            if (totalTaxesCollected > 0) {
                String cityAccountName = CITY_ACCOUNT_PREFIX + city.getName().toLowerCase();
                Account cityAccount = economy.getAccount(cityAccountName);
                
                if (cityAccount != null) {
                    cityAccount.deposit(totalTaxesCollected);
                    logger.info("Impuestos cobrados para " + city.getName() + ": $" + 
                              String.format("%.2f", totalTaxesCollected) + " de " + citizensTaxed + " ciudadanos");
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.severe("Error al cobrar impuestos para la ciudad " + city.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Transfiere fondos de un jugador a la ciudad.
     * 
     * @param citizenId UUID del ciudadano
     * @param city Ciudad destino
     * @param amount Cantidad a transferir
     * @return true si la transferencia fue exitosa
     */
    public boolean transferFundsToCity(UUID citizenId, City city, double amount) {
        if (citizenId == null || city == null || amount <= 0) {
            return false;
        }
        
        try {
            Player player = Bukkit.getPlayer(citizenId);
            if (player == null) return false;
            
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) return false;
            
            Account citizenAccount = economy.getAccount(player.getName());
            String cityAccountName = CITY_ACCOUNT_PREFIX + city.getName().toLowerCase();
            Account cityAccount = economy.getAccount(cityAccountName);
            
            if (citizenAccount == null || cityAccount == null) {
                return false;
            }
            
            if (citizenAccount.getBalance() >= amount && citizenAccount.withdraw(amount)) {
                cityAccount.deposit(amount);
                
                player.sendMessage("§e[CityWars] §7Has transferido §a$" + String.format("%.2f", amount) + 
                                 " §7a la ciudad §a" + city.getName());
                return true;
            }
            
        } catch (Exception e) {
            logger.severe("Error al transferir fondos a la ciudad: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Distribuye el porcentaje del fondo de la ciudad a los atacantes después de un asedio.
     * 
     * @param city Ciudad derrotada
     * @param attackers Set de UUIDs de los atacantes
     * @param percentage Porcentaje del fondo a distribuir (como decimal, ej: 0.5 para 50%)
     * @return true si la distribución fue exitosa
     */
    public boolean distributeFundsAfterSiege(City city, Set<UUID> attackers, double percentage) {
        if (city == null || attackers == null || attackers.isEmpty() || percentage <= 0 || percentage > 1) {
            return false;
        }
        
        try {
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) return false;
            
            String cityAccountName = CITY_ACCOUNT_PREFIX + city.getName().toLowerCase();
            Account cityAccount = economy.getAccount(cityAccountName);
            
            if (cityAccount == null) return false;
            
            double cityBalance = cityAccount.getBalance();
            double totalToDistribute = cityBalance * percentage;
            double amountPerAttacker = totalToDistribute / attackers.size();
            
            if (totalToDistribute > 0 && cityAccount.withdraw(totalToDistribute)) {
                int successfulDistributions = 0;
                
                for (UUID attackerId : attackers) {
                    Player attacker = Bukkit.getPlayer(attackerId);
                    if (attacker == null) continue;
                    
                    Account attackerAccount = economy.getAccount(attacker.getName());
                    if (attackerAccount != null) {
                        attackerAccount.deposit(amountPerAttacker);
                        successfulDistributions++;
                        
                        attacker.sendMessage("§e[CityWars] §7Has recibido §a$" + String.format("%.2f", amountPerAttacker) + 
                                           " §7como botín del asedio a §c" + city.getName());
                    }
                }
                
                logger.info("Distribuidos $" + String.format("%.2f", totalToDistribute) + 
                          " del fondo de " + city.getName() + " entre " + successfulDistributions + " atacantes");
                return true;
            }
            
        } catch (Exception e) {
            logger.severe("Error al distribuir fondos después del asedio: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Obtiene el balance de un ciudadano.
     * 
     * @param citizenId UUID del ciudadano
     * @return Balance del ciudadano o -1 si hay error
     */
    public double getCitizenBalance(UUID citizenId) {
        if (citizenId == null) return -1;
        
        try {
            Player player = Bukkit.getPlayer(citizenId);
            if (player == null) return -1;
            
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) return -1;
            
            Account account = economy.getAccount(player.getName());
            return account != null ? account.getBalance() : -1;
            
        } catch (Exception e) {
            logger.severe("Error al obtener balance del ciudadano: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Obtiene el balance total de la ciudad.
     * 
     * @param city Ciudad a consultar
     * @return Balance de la ciudad o -1 si hay error
     */
    public double getCityBalance(City city) {
        if (city == null) return -1;
        
        try {
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) return -1;
            
            String cityAccountName = CITY_ACCOUNT_PREFIX + city.getName().toLowerCase();
            Account cityAccount = economy.getAccount(cityAccountName);
            
            return cityAccount != null ? cityAccount.getBalance() : -1;
            
        } catch (Exception e) {
            logger.severe("Error al obtener balance de la ciudad: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Establece el nuevo balance de un ciudadano.
     * 
     * @param citizenId UUID del ciudadano
     * @param newBalance Nuevo balance a establecer
     * @return true si el balance fue establecido exitosamente
     */
    public boolean setCitizenBalance(UUID citizenId, double newBalance) {
        if (citizenId == null || newBalance < 0) return false;
        
        try {
            Player player = Bukkit.getPlayer(citizenId);
            if (player == null) return false;
            
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) return false;
            
            Account account = economy.getAccount(player.getName());
            if (account != null) {
                account.setBalance(newBalance);
                return true;
            }
            
        } catch (Exception e) {
            logger.severe("Error al establecer balance del ciudadano: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Transfiere fondos entre dos ciudades.
     * 
     * @param fromCity Ciudad origen
     * @param toCity Ciudad destino
     * @param amount Cantidad a transferir
     * @return true si la transferencia fue exitosa
     */
    public boolean transferFundsBetweenCities(City fromCity, City toCity, double amount) {
        if (fromCity == null || toCity == null || amount <= 0) {
            return false;
        }
        
        try {
            Economy economy = gemsAPI.getEconomy("gems");
            if (economy == null) return false;
            
            String fromAccountName = CITY_ACCOUNT_PREFIX + fromCity.getName().toLowerCase();
            String toAccountName = CITY_ACCOUNT_PREFIX + toCity.getName().toLowerCase();
            
            Account fromAccount = economy.getAccount(fromAccountName);
            Account toAccount = economy.getAccount(toAccountName);
            
            if (fromAccount == null || toAccount == null) {
                return false;
            }
            
            if (fromAccount.getBalance() >= amount && fromAccount.withdraw(amount)) {
                toAccount.deposit(amount);
                
                logger.info("Transferidos $" + String.format("%.2f", amount) + 
                          " de " + fromCity.getName() + " a " + toCity.getName());
                return true;
            }
            
        } catch (Exception e) {
            logger.severe("Error al transferir fondos entre ciudades: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Verifica si GemsEconomy está disponible y funcionando.
     * 
     * @return true si GemsEconomy está disponible
     */
    public boolean isEconomyAvailable() {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("GemsEconomy") && 
                   gemsAPI.getEconomy("gems") != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtiene información detallada del balance de una ciudad.
     * 
     * @param city Ciudad a consultar
     * @return String con información detallada o null si hay error
     */
    public String getCityBalanceInfo(City city) {
        if (city == null) return null;
        
        double balance = getCityBalance(city);
        if (balance >= 0) {
            return "§e[CityWars] §7Balance de §a" + city.getName() + "§7: §a$" + String.format("%.2f", balance);
        }
        
        return null;
    }
}
