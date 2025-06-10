package com.mineglicht.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase que gestiona las banderas de asedio en CityWars
 * Maneja la validación, colocación, captura y efectos de las banderas de asedio
 */
public class SiegeFlag implements ConfigurationSerializable {
    
    // Constantes para NBT tags
    private static final String SIEGE_FLAG_NBT_KEY = "citywars_siege_flag";
    private static final String EXECUTABLE_ITEMS_NBT = "executableitems";
    
    // Atributos principales
    private String id;
    private Location location;
    private String siegeId;
    private FlagType type;
    private UUID placer;
    private LocalDateTime placementTime;
    
    // Estados de la bandera
    private boolean active;
    private boolean captured;
    private boolean destroyed;
    
    /**
     * Enumeración para los tipos de bandera
     */
    public enum FlagType {
        SIEGE_ATTACK,    // Bandera de ataque (colocada por atacantes)
        SIEGE_DEFENSE,   // Bandera de defensa (bandera de protección de ciudad)
        PROTECTION       // Bandera de protección general
    }
    
    // ==================== CONSTRUCTORES ====================
    
    /**
     * Constructor básico para SiegeFlag
     * @param location Ubicación de la bandera
     * @param siegeId ID del asedio asociado
     * @param type Tipo de bandera
     */
    public SiegeFlag(Location location, String siegeId, FlagType type) {
        this.id = UUID.randomUUID().toString();
        this.location = location;
        this.siegeId = siegeId;
        this.type = type;
        this.placementTime = LocalDateTime.now();
        this.active = true;
        this.captured = false;
        this.destroyed = false;
    }
    
    /**
     * Constructor completo para SiegeFlag
     * @param location Ubicación de la bandera
     * @param siegeId ID del asedio asociado
     * @param type Tipo de bandera
     * @param placer UUID del jugador que colocó la bandera
     */
    public SiegeFlag(Location location, String siegeId, FlagType type, UUID placer) {
        this(location, siegeId, type);
        this.placer = placer;
    }
    
    // ==================== MÉTODOS PRINCIPALES ====================
    
    // Información básica
    
    /**
     * Obtiene el ID único de la bandera
     * @return ID de la bandera
     */
    public String getId() {
        return id;
    }
    
    /**
     * Obtiene la ubicación de la bandera
     * @return Ubicación de la bandera
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Establece la ubicación de la bandera
     * @param location Nueva ubicación
     */
    public void setLocation(Location location) {
        this.location = location;
    }
    
    /**
     * Obtiene el ID del asedio asociado
     * @return ID del asedio
     */
    public String getSiegeId() {
        return siegeId;
    }
    
    /**
     * Obtiene el tipo de bandera
     * @return Tipo de bandera
     */
    public FlagType getType() {
        return type;
    }
    
    /**
     * Obtiene el UUID del jugador que colocó la bandera
     * @return UUID del colocador
     */
    public UUID getPlacer() {
        return placer;
    }
    
    /**
     * Obtiene el tiempo de colocación de la bandera
     * @return Tiempo de colocación
     */
    public LocalDateTime getPlacementTime() {
        return placementTime;
    }
    
    // Estado de la bandera
    
    /**
     * Verifica si la bandera está activa
     * @return true si está activa
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Establece el estado activo de la bandera
     * @param active Estado activo
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Verifica si la bandera ha sido capturada
     * @return true si está capturada
     */
    public boolean isCaptured() {
        return captured;
    }
    
    /**
     * Establece el estado de captura de la bandera
     * @param captured Estado de captura
     */
    public void setCaptured(boolean captured) {
        this.captured = captured;
    }
    
    /**
     * Verifica si la bandera ha sido destruida
     * @return true si está destruida
     */
    public boolean isDestroyed() {
        return destroyed;
    }
    
    /**
     * Establece el estado de destrucción de la bandera
     * @param destroyed Estado de destrucción
     */
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }
    
    // Validaciones NBT
    
    /**
     * Valida si un item es una bandera de asedio válida
     * @param item Item a validar
     * @return true si es una bandera de asedio válida
     */
    public boolean isValidSiegeFlag(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        return hasValidNBT(item) && matchesNBTRequirements(item);
    }
    
    /**
     * Verifica si un item tiene NBT válido para banderas de asedio
     * @param item Item a verificar
     * @return true si tiene NBT válido
     */
    public static boolean hasValidNBT(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Verificar NBT de ExecutableItems
        NamespacedKey executableKey = new NamespacedKey("executableitems", EXECUTABLE_ITEMS_NBT);
        if (meta.getPersistentDataContainer().has(executableKey, PersistentDataType.STRING)) {
            return true;
        }
        
        // Verificar NBT específico de CityWars
        NamespacedKey cityWarsKey = new NamespacedKey("citywars", SIEGE_FLAG_NBT_KEY);
        return meta.getPersistentDataContainer().has(cityWarsKey, PersistentDataType.STRING);
    }
    
    /**
     * Verifica si el item cumple con los requisitos NBT específicos
     * @param item Item a verificar
     * @return true si cumple los requisitos
     */
    public boolean matchesNBTRequirements(ItemStack item) {
        if (!hasValidNBT(item)) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("citywars", SIEGE_FLAG_NBT_KEY);
        
        String nbtValue = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return "siege_banner".equals(nbtValue) || "siege_flag".equals(nbtValue);
    }
    
    // Interacciones
    
    /**
     * Verifica si la bandera puede ser interactuada
     * @return true si puede ser interactuada
     */
    public boolean canBeInteracted() {
        return active && !destroyed && isBlockPresent();
    }
    
    /**
     * Verifica si la bandera puede ser capturada por un jugador específico
     * @param playerId ID del jugador
     * @return true si puede ser capturada
     */
    public boolean canBeCaptured(UUID playerId) {
        if (!canBeInteracted() || captured) {
            return false;
        }
        
        // No puede capturar su propia bandera
        if (Objects.equals(placer, playerId)) {
            return false;
        }
        
        // Verificar si el jugador pertenece al bando contrario
        return true; // Aquí implementarías la lógica específica del asedio
    }
    
    /**
     * Captura la bandera por un jugador específico
     * @param capturer UUID del jugador que captura
     */
    public void capture(UUID capturer) {
        if (!canBeCaptured(capturer)) {
            return;
        }
        
        this.captured = true;
        this.active = false;
        
        // Aquí implementarías la lógica de captura específica
        // Por ejemplo, notificar al sistema de asedio, cambiar protecciones, etc.
    }
    
    /**
     * Destruye la bandera
     */
    public void destroy() {
        this.destroyed = true;
        this.active = false;
        
        // Remover el bloque físico si existe
        if (isBlockPresent()) {
            getBlock().setType(Material.AIR);
        }
        
        removeProtection();
    }
    
    /**
     * Repara la bandera (la restaura a su estado original)
     */
    public void repair() {
        this.destroyed = false;
        this.captured = false;
        this.active = true;
        
        spawnProtection();
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Obtiene el bloque donde está ubicada la bandera
     * @return Bloque de la bandera
     */
    public Block getBlock() {
        return location.getBlock();
    }
    
    /**
     * Verifica si el bloque de la bandera está presente
     * @return true si el bloque existe
     */
    public boolean isBlockPresent() {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        Block block = getBlock();
        return block.getType() != Material.AIR;
    }
    
    /**
     * Genera protección alrededor de la bandera
     */
    public void spawnProtection() {
        // Aquí implementarías la lógica de protección
        // Por ejemplo, crear una región de WorldGuard, establecer flags, etc.
    }
    
    /**
     * Remueve la protección de la bandera
     */
    public void removeProtection() {
        // Aquí implementarías la lógica para remover protección
        // Por ejemplo, eliminar región de WorldGuard, remover flags, etc.
    }
    
    /**
     * Serializa la bandera para almacenamiento
     * @return Mapa con datos serializados
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("location", location);
        data.put("siegeId", siegeId);
        data.put("type", type.name());
        data.put("placer", placer != null ? placer.toString() : null);
        data.put("placementTime", placementTime.toString());
        data.put("active", active);
        data.put("captured", captured);
        data.put("destroyed", destroyed);
        return data;
    }
    
    /**
     * Deserializa datos para crear una SiegeFlag
     * @param data Datos serializados
     * @return Nueva instancia de SiegeFlag
     */
    public static SiegeFlag deserialize(Map<String, Object> data) {
        Location location = (Location) data.get("location");
        String siegeId = (String) data.get("siegeId");
        FlagType type = FlagType.valueOf((String) data.get("type"));
        String placerStr = (String) data.get("placer");
        UUID placer = placerStr != null ? UUID.fromString(placerStr) : null;
        
        SiegeFlag flag = new SiegeFlag(location, siegeId, type, placer);
        flag.id = (String) data.get("id");
        flag.placementTime = LocalDateTime.parse((String) data.get("placementTime"));
        flag.active = (Boolean) data.get("active");
        flag.captured = (Boolean) data.get("captured");
        flag.destroyed = (Boolean) data.get("destroyed");
        
        return flag;
    }
    
    /**
     * Representación en cadena de la bandera
     * @return String descriptivo
     */
    @Override
    public String toString() {
        return String.format("SiegeFlag{id='%s', type=%s, siegeId='%s', active=%s, captured=%s, destroyed=%s}", 
                           id, type, siegeId, active, captured, destroyed);
    }
    
    /**
     * Verifica igualdad con otro objeto
     * @param obj Objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SiegeFlag siegeFlag = (SiegeFlag) obj;
        return Objects.equals(id, siegeFlag.id);
    }
    
    /**
     * Calcula el hash code
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}