package com.mineglicht.integration;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.DyeColor;
import org.bukkit.block.Banner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Clase encargada de la integración con ExecutableItems para gestionar
 * las banderas de asedio y protección en el sistema CityWars.
 */
public class ExecutableItemsIntegration {
    
    // Claves para identificar las banderas mediante PersistentDataContainer
    private static final NamespacedKey SIEGE_FLAG_KEY = new NamespacedKey("citywars", "siege_flag");
    private static final NamespacedKey PROTECTION_FLAG_KEY = new NamespacedKey("citywars", "protection_flag");
    private static final NamespacedKey OWNER_UUID_KEY = new NamespacedKey("citywars", "owner_uuid");
    
    // Nombres y descripciones de las banderas
    private static final String SIEGE_FLAG_NAME = "§c§lBandera de Asedio";
    private static final String PROTECTION_FLAG_NAME = "§a§lEstandarte de Protección";
    
    private static final List<String> SIEGE_FLAG_LORE = Arrays.asList(
        "§7Coloca esta bandera dentro de una ciudad",
        "§7enemiga para iniciar un asedio.",
        "§c§lADVERTENCIA: §7Esto activará el PvP",
        "§7en la ciudad objetivo.",
        "",
        "§8Propietario: §7{owner}"
    );
    
    private static final List<String> PROTECTION_FLAG_LORE = Arrays.asList(
        "§7Esta bandera protege tu ciudad de",
        "§7ataques y saqueos enemigos.",
        "§a§lIMPORTANTE: §7Si es destruida,",
        "§7la ciudad quedará vulnerable.",
        "",
        "§8Propietario: §7{owner}"
    );
    
    /**
     * Crea una bandera de asedio con el UUID del propietario.
     * 
     * @param ownerUUID UUID del propietario de la bandera
     * @return ItemStack de la bandera de asedio
     */
    public ItemStack createSiegeFlag(UUID ownerUUID) {
        ItemStack siegeFlag = new ItemStack(Material.RED_BANNER);
        BannerMeta meta = (BannerMeta) siegeFlag.getItemMeta();
        
        if (meta != null) {
            // Configurar nombre y lore
            meta.setDisplayName(SIEGE_FLAG_NAME);
            List<String> lore = SIEGE_FLAG_LORE;
            lore.replaceAll(line -> line.replace("{owner}", ownerUUID.toString().substring(0, 8) + "..."));
            meta.setLore(lore);
            
            // Agregar patrones decorativos para la bandera de asedio
            meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            meta.addPattern(new Pattern(DyeColor.RED, PatternType.STRIPE_MIDDLE));
            meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.CROSS));
            
            // Marcar como bandera de asedio en PersistentDataContainer
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(SIEGE_FLAG_KEY, PersistentDataType.BOOLEAN, true);
            container.set(OWNER_UUID_KEY, PersistentDataType.STRING, ownerUUID.toString());
            
            siegeFlag.setItemMeta(meta);
        }
        
        return siegeFlag;
    }
    
    /**
     * Crea una bandera de protección (Estandarte) con el UUID del propietario.
     * 
     * @param ownerUUID UUID del propietario de la bandera
     * @return ItemStack de la bandera de protección
     */
    public ItemStack createProtectionFlag(UUID ownerUUID) {
        ItemStack protectionFlag = new ItemStack(Material.GREEN_BANNER);
        BannerMeta meta = (BannerMeta) protectionFlag.getItemMeta();
        
        if (meta != null) {
            // Configurar nombre y lore
            meta.setDisplayName(PROTECTION_FLAG_NAME);
            List<String> lore = PROTECTION_FLAG_LORE;
            lore.replaceAll(line -> line.replace("{owner}", ownerUUID.toString().substring(0, 8) + "..."));
            meta.setLore(lore);
            
            // Agregar patrones decorativos para la bandera de protección
            meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BASE));
            meta.addPattern(new Pattern(DyeColor.GREEN, PatternType.BORDER));
            meta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.FLOWER));
            meta.addPattern(new Pattern(DyeColor.GREEN, PatternType.GRADIENT_UP));
            
            // Marcar como bandera de protección en PersistentDataContainer
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(PROTECTION_FLAG_KEY, PersistentDataType.BOOLEAN, true);
            container.set(OWNER_UUID_KEY, PersistentDataType.STRING, ownerUUID.toString());
            
            protectionFlag.setItemMeta(meta);
        }
        
        return protectionFlag;
    }
    
    /**
     * Verifica si un ítem es una bandera de asedio.
     * 
     * @param item ItemStack a verificar
     * @return true si es una bandera de asedio, false en caso contrario
     */
    public boolean isSiegeFlag(ItemStack item) {
        if (item == null || item.getType() != Material.RED_BANNER) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(SIEGE_FLAG_KEY, PersistentDataType.BOOLEAN) &&
               Boolean.TRUE.equals(container.get(SIEGE_FLAG_KEY, PersistentDataType.BOOLEAN));
    }
    
    /**
     * Verifica si un ítem es una bandera de protección.
     * 
     * @param item ItemStack a verificar
     * @return true si es una bandera de protección, false en caso contrario
     */
    public boolean isProtectionFlag(ItemStack item) {
        if (item == null || item.getType() != Material.GREEN_BANNER) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(PROTECTION_FLAG_KEY, PersistentDataType.BOOLEAN) &&
               Boolean.TRUE.equals(container.get(PROTECTION_FLAG_KEY, PersistentDataType.BOOLEAN));
    }
    
    /**
     * Obtiene el UUID del propietario de la bandera.
     * 
     * @param item ItemStack de la bandera
     * @return UUID del propietario o null si no se encuentra
     */
    public UUID getOwnerUUID(ItemStack item) {
        if (item == null || (!isSiegeFlag(item) && !isProtectionFlag(item))) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String uuidString = container.get(OWNER_UUID_KEY, PersistentDataType.STRING);
        
        if (uuidString != null) {
            try {
                return UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                // UUID inválido
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Destruye la bandera eliminándola del mundo en la ubicación especificada.
     * 
     * @param location Ubicación donde se encuentra la bandera a destruir
     * @return true si la bandera fue destruida exitosamente
     */
    public boolean destroyFlag(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        Block block = location.getBlock();
        
        // Verificar que el bloque sea una bandera de CityWars
        if (!isCityWarsFlag(block)) {
            return false;
        }
        
        // Eliminar el bloque del mundo
        block.setType(Material.AIR);
        return true;
    }
    
    /**
     * Destruye la bandera eliminándola del mundo basándose en el bloque.
     * 
     * @param block Bloque de la bandera a destruir
     * @return true si la bandera fue destruida exitosamente
     */
    public boolean destroyFlag(Block block) {
        if (block == null || !isCityWarsFlag(block)) {
            return false;
        }
        
        // Eliminar el bloque del mundo
        block.setType(Material.AIR);
        return true;
    }
    
    /**
     * Coloca la bandera en el mundo en la ubicación especificada.
     * 
     * @param item ItemStack de la bandera a colocar
     * @param location Ubicación donde colocar la bandera
     * @return true si la bandera fue colocada exitosamente
     */
    public boolean placeFlagInWorld(ItemStack item, Location location) {
        if (item == null || location == null || location.getWorld() == null) {
            return false;
        }
        
        if (!isSiegeFlag(item) && !isProtectionFlag(item)) {
            return false;
        }
        
        Block targetBlock = location.getBlock();
        
        // Verificar que el bloque esté disponible para colocación
        if (targetBlock.getType() != Material.AIR) {
            return false;
        }
        
        // Determinar el tipo de bandera y colocarla
        Material bannerType = isSiegeFlag(item) ? Material.RED_BANNER : Material.GREEN_BANNER;
        targetBlock.setType(bannerType);
        
        // Configurar los datos del bloque banner
        BlockState state = targetBlock.getState();
        if (state instanceof Banner) {
            Banner banner = (Banner) state;
            
            // Transferir los patrones del ítem al bloque
            BannerMeta itemMeta = (BannerMeta) item.getItemMeta();
            if (itemMeta != null) {
                banner.setPatterns(itemMeta.getPatterns());
            }
            
            banner.update();
            return true;
        }
        
        return false;
    }
    
    /**
     * Verifica si un bloque en el mundo es una bandera de asedio o protección.
     * 
     * @param block Bloque a verificar
     * @return true si el bloque es una bandera de CityWars
     */
    public boolean isCityWarsFlag(Block block) {
        if (block == null) {
            return false;
        }
        
        Material type = block.getType();
        return type == Material.RED_BANNER || type == Material.GREEN_BANNER;
    }
    
    /**
     * Obtiene el tipo de bandera de un bloque en el mundo.
     * 
     * @param block Bloque a verificar
     * @return "siege" si es bandera de asedio, "protection" si es de protección, null si no es bandera
     */
    public String getFlagType(Block block) {
        if (block == null) {
            return null;
        }
        
        if (block.getType() == Material.RED_BANNER) {
            return "siege";
        } else if (block.getType() == Material.GREEN_BANNER) {
            return "protection";
        }
        
        return null;
    }
}