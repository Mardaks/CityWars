package com.mineglicht.api;

import com.mineglicht.models.City;
import com.mineglicht.models.Citizen;
import com.mineglicht.models.SiegeState;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * API principal de CityWars para integración con otros plugins
 * Proporciona acceso a todas las funcionalidades principales del sistema de ciudades
 */
public interface CityWarsAPI {

    // ================================
    // GESTIÓN DE CIUDADES
    // ================================

    /**
     * Crea una nueva ciudad
     * @param name Nombre de la ciudad
     * @param mayorUuid UUID del alcalde
     * @param location Ubicación central de la ciudad
     * @return true si la ciudad fue creada exitosamente
     */
    boolean createCity(String name, UUID mayorUuid, Location location);

    /**
     * Elimina una ciudad existente
     * @param cityName Nombre de la ciudad
     * @return true si la ciudad fue eliminada exitosamente
     */
    boolean deleteCity(String cityName);

    /**
     * Obtiene una ciudad por su nombre
     * @param cityName Nombre de la ciudad
     * @return Objeto City o null si no existe
     */
    City getCity(String cityName);

    /**
     * Obtiene todas las ciudades existentes
     * @return Lista de todas las ciudades
     */
    List<City> getAllCities();

    /**
     * Verifica si una ciudad existe
     * @param cityName Nombre de la ciudad
     * @return true si la ciudad existe
     */
    boolean cityExists(String cityName);

    /**
     * Obtiene la ciudad en una ubicación específica
     * @param location Ubicación a verificar
     * @return Ciudad en esa ubicación o null
     */
    City getCityAt(Location location);

    /**
     * Expande el territorio de una ciudad
     * @param cityName Nombre de la ciudad
     * @param direction Dirección de expansión
     * @param blocks Cantidad de bloques a expandir
     * @return true si la expansión fue exitosa
     */
    boolean expandCity(String cityName, String direction, int blocks);

    /**
     * Obtiene el balance del fondo bancario de una ciudad
     * @param cityName Nombre de la ciudad
     * @return Balance del fondo bancario
     */
    double getCityBankBalance(String cityName);

    // ================================
    // GESTIÓN DE CIUDADANOS
    // ================================

    /**
     * Añade un jugador a una ciudad
     * @param playerUuid UUID del jugador
     * @param cityName Nombre de la ciudad
     * @return true si el jugador fue añadido exitosamente
     */
    boolean addCitizen(UUID playerUuid, String cityName);

    /**
     * Remueve un jugador de una ciudad
     * @param playerUuid UUID del jugador
     * @return true si el jugador fue removido exitosamente
     */
    boolean removeCitizen(UUID playerUuid);

    /**
     * Obtiene la ciudad de un jugador
     * @param playerUuid UUID del jugador
     * @return Nombre de la ciudad o null si no pertenece a ninguna
     */
    String getPlayerCity(UUID playerUuid);

    /**
     * Obtiene información de un ciudadano
     * @param playerUuid UUID del jugador
     * @return Objeto Citizen o null si no es ciudadano
     */
    Citizen getCitizen(UUID playerUuid);

    /**
     * Obtiene todos los ciudadanos de una ciudad
     * @param cityName Nombre de la ciudad
     * @return Lista de ciudadanos
     */
    List<Citizen> getCitizens(String cityName);

    /**
     * Verifica si un jugador es ciudadano de alguna ciudad
     * @param playerUuid UUID del jugador
     * @return true si es ciudadano
     */
    boolean isCitizen(UUID playerUuid);

    /**
     * Verifica si un jugador es alcalde de una ciudad
     * @param playerUuid UUID del jugador
     * @param cityName Nombre de la ciudad
     * @return true si es alcalde de esa ciudad
     */
    boolean isMayor(UUID playerUuid, String cityName);

    /**
     * Verifica si un jugador es asistente de una ciudad
     * @param playerUuid UUID del jugador
     * @param cityName Nombre de la ciudad
     * @return true si es asistente de esa ciudad
     */
    boolean isAssistant(UUID playerUuid, String cityName);

    // ================================
    // SISTEMA DE ASEDIOS
    // ================================

    /**
     * Inicia un asedio contra una ciudad
     * @param attackerCity Ciudad atacante
     * @param defenderCity Ciudad defensora
     * @param flagLocation Ubicación del estandarte de asedio
     * @param attackerUuid UUID del jugador que inicia el asedio
     * @return true si el asedio fue iniciado exitosamente
     */
    boolean startSiege(String attackerCity, String defenderCity, Location flagLocation, UUID attackerUuid);

    /**
     * Termina un asedio activo
     * @param cityName Nombre de la ciudad bajo asedio
     * @param reason Razón del fin del asedio
     * @return true si el asedio fue terminado exitosamente
     */
    boolean endSiege(String cityName, String reason);

    /**
     * Obtiene el estado de asedio de una ciudad
     * @param cityName Nombre de la ciudad
     * @return Estado de asedio actual
     */
    SiegeState getSiegeState(String cityName);

    /**
     * Verifica si una ciudad está bajo asedio
     * @param cityName Nombre de la ciudad
     * @return true si está bajo asedio
     */
    boolean isUnderSiege(String cityName);

    /**
     * Verifica si una ciudad está atacando a otra
     * @param cityName Nombre de la ciudad
     * @return true si está atacando
     */
    boolean isAttacking(String cityName);

    /**
     * Obtiene el tiempo restante de un asedio
     * @param cityName Nombre de la ciudad bajo asedio
     * @return Tiempo restante en segundos, -1 si no hay asedio activo
     */
    int getSiegeTimeRemaining(String cityName);

    /**
     * Verifica si hay cooldown entre dos ciudades
     * @param attackerCity Ciudad atacante
     * @param defenderCity Ciudad defensora
     * @return true si hay cooldown activo
     */
    boolean hasSiegeCooldown(String attackerCity, String defenderCity);

    /**
     * Obtiene el tiempo de cooldown entre dos ciudades
     * @param attackerCity Ciudad atacante
     * @param defenderCity Ciudad defensora
     * @return Tiempo de cooldown en segundos, 0 si no hay cooldown
     */
    int getSiegeCooldownTime(String attackerCity, String defenderCity);

    // ================================
    // SISTEMA ECONÓMICO
    // ================================

    /**
     * Obtiene el balance de un jugador
     * @param playerUuid UUID del jugador
     * @return Balance del jugador
     */
    double getPlayerBalance(UUID playerUuid);

    /**
     * Deposita dinero en el fondo bancario de una ciudad
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a depositar
     * @return true si el depósito fue exitoso
     */
    boolean depositToCityBank(String cityName, double amount);

    /**
     * Retira dinero del fondo bancario de una ciudad
     * @param cityName Nombre de la ciudad
     * @param amount Cantidad a retirar
     * @return true si el retiro fue exitoso
     */
    boolean withdrawFromCityBank(String cityName, double amount);

    /**
     * Cobra impuestos manualmente a una ciudad
     * @param cityName Nombre de la ciudad
     * @return Cantidad total de impuestos recolectados
     */
    double collectCityTaxes(String cityName);

    /**
     * Fuerza la recolección de impuestos de todas las ciudades
     * @return Cantidad total de impuestos recolectados
     */
    double forceGlobalTaxCollection();

    // ================================
    // SISTEMA DE PROTECCIÓN
    // ================================

    /**
     * Verifica si una ubicación está protegida por una ciudad
     * @param location Ubicación a verificar
     * @return true si está protegida
     */
    boolean isProtected(Location location);

    /**
     * Verifica si un jugador puede realizar una acción en una ubicación
     * @param player Jugador que intenta realizar la acción
     * @param location Ubicación de la acción
     * @param action Tipo de acción (BUILD, BREAK, INTERACT, etc.)
     * @return true si la acción está permitida
     */
    boolean canPerformAction(Player player, Location location, String action);

    /**
     * Verifica si el PvP está activo en una ubicación
     * @param location Ubicación a verificar
     * @return true si el PvP está activo
     */
    boolean isPvPActive(Location location);

    // ================================
    // UTILIDADES Y ESTADÍSTICAS
    // ================================

    /**
     * Obtiene el número total de ciudades
     * @return Número de ciudades
     */
    int getTotalCities();

    /**
     * Obtiene el número total de ciudadanos
     * @return Número de ciudadanos
     */
    int getTotalCitizens();

    /**
     * Obtiene el número de asedios activos
     * @return Número de asedios activos
     */
    int getActiveSieges();

    /**
     * Obtiene estadísticas generales del servidor
     * @return Mapa con estadísticas (ciudades, ciudadanos, asedios, etc.)
     */
    java.util.Map<String, Object> getServerStats();

    /**
     * Obtiene el ranking de ciudades por tamaño de fondo bancario
     * @param limit Número máximo de ciudades a retornar
     * @return Lista ordenada de ciudades por riqueza
     */
    List<City> getCityRankingByWealth(int limit);

    /**
     * Obtiene el ranking de ciudades por número de ciudadanos
     * @param limit Número máximo de ciudades a retornar
     * @return Lista ordenada de ciudades por población
     */
    List<City> getCityRankingByPopulation(int limit);

    // ================================
    // EVENTOS Y CALLBACKS
    // ================================

    /**
     * Registra un listener para eventos de CityWars
     * @param listener Listener a registrar
     */
    void registerEventListener(Object listener);

    /**
     * Desregistra un listener de eventos de CityWars
     * @param listener Listener a desregistrar
     */
    void unregisterEventListener(Object listener);

    // ================================
    // CONFIGURACIÓN Y ADMINISTRACIÓN
    // ================================

    /**
     * Recarga la configuración del plugin
     * @return true si la recarga fue exitosa
     */
    boolean reloadConfig();

    /**
     * Obtiene un valor de configuración
     * @param path Ruta de la configuración
     * @return Valor de la configuración
     */
    Object getConfigValue(String path);

    /**
     * Establece un valor de configuración
     * @param path Ruta de la configuración
     * @param value Nuevo valor
     * @return true si el valor fue establecido exitosamente
     */
    boolean setConfigValue(String path, Object value);

    /**
     * Guarda la configuración actual
     * @return true si se guardó exitosamente
     */
    boolean saveConfig();

    /**
     * Obtiene la versión de la API
     * @return Versión de la API
     */
    String getAPIVersion();

    /**
     * Verifica si la API está disponible y funcionando
     * @return true si la API está disponible
     */
    boolean isAPIAvailable();
}
