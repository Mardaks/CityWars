package com.mineglicht.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilidades para el manejo de items, especialmente el estandarte de asedio
 */
public class ItemUtils {

    // Keys para persistent data
    public static final String SIEGE_FLAG_KEY = "citywars_siege_flag";
    public static final String CITY_FLAG_KEY = "citywars_city_flag";
    public static final String SPECIAL_ITEM_KEY = "citywars_special_item";

    /**
     * Crea un ItemStack básico
     * @param material El material
     * @param amount La cantidad
     * @param displayName El nombre a mostrar
     * @param lore La descripción
     * @return El ItemStack creado
     */
    public static ItemStack createItem(Material material, int amount, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(MessageUtils.colorize(displayName));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> colorizedLore = new ArrayList<>();
                lore.forEach(line -> colorizedLore.add(MessageUtils.colorize(line)));
                meta.setLore(colorizedLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Crea un ItemStack básico con lore variable
     * @param material El material
     * @param amount La cantidad
     * @param displayName El nombre a mostrar
     * @param lore Líneas de lore
     * @return El ItemStack creado
     */
    public static ItemStack createItem(Material material, int amount, String displayName, String... lore) {
        return createItem(material, amount, displayName, Arrays.asList(lore));
    }

    /**
     * Crea el estandarte de asedio
     * @param plugin El plugin principal
     * @param attackerName Nombre del atacante
     * @param targetCity Ciudad objetivo
     * @return El estandarte de asedio
     */
    public static ItemStack createSiegeFlag(Plugin plugin, String attackerName, String targetCity) {
        ItemStack siegeFlag = createItem(
                Material.BLACK_BANNER, 1,
                "&4&l⚔ Estandarte de Asedio ⚔",
                "&7Atacante: &c" + attackerName,
                "&7Objetivo: &e" + targetCity,
                "",
                "&c¡Coloca este estandarte en territorio",
                "&cenemigo para iniciar el asedio!",
                "",
                "&8&o\"La guerra ha comenzado...\""
        );

        // Añadir encantamiento de brillo
        siegeFlag.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);

        ItemMeta meta = siegeFlag.getItemMeta();
        if (meta != null) {
            // Ocultar encantamientos
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Añadir persistent data
            NamespacedKey key = new NamespacedKey(plugin, SIEGE_FLAG_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING,
                    attackerName + ":" + targetCity + ":" + System.currentTimeMillis());

            siegeFlag.setItemMeta(meta);
        }

        return siegeFlag;
    }

    /**
     * Crea una bandera de ciudad
     * @param plugin El plugin principal
     * @param cityName Nombre de la ciudad
     * @param material Material de la bandera
     * @return La bandera de ciudad
     */
    public static ItemStack createCityFlag(Plugin plugin, String cityName, Material material) {
        ItemStack cityFlag = createItem(
                material, 1,
                "&6&l🏴 Bandera de " + cityName + " 🏴",
                "&7Ciudad: &e" + cityName,
                "",
                "&6Esta bandera representa la soberanía",
                "&6de " + cityName + ". ¡Defiéndela con honor!",
                "",
                "&8&o\"Unidos resistimos, divididos caemos\""
        );

        // Añadir encantamiento de brillo
        cityFlag.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);

        ItemMeta meta = cityFlag.getItemMeta();
        if (meta != null) {
            // Ocultar encantamientos
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Añadir persistent data
            NamespacedKey key = new NamespacedKey(plugin, CITY_FLAG_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, cityName);

            cityFlag.setItemMeta(meta);
        }

        return cityFlag;
    }

    /**
     * Verifica si un item es un estandarte de asedio
     * @param plugin El plugin principal
     * @param item El item a verificar
     * @return true si es un estandarte de asedio
     */
    public static boolean isSiegeFlag(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, SIEGE_FLAG_KEY);

        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    /**
     * Verifica si un item es una bandera de ciudad
     * @param plugin El plugin principal
     * @param item El item a verificar
     * @return true si es una bandera de ciudad
     */
    public static boolean isCityFlag(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, CITY_FLAG_KEY);

        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    /**
     * Obtiene los datos del estandarte de asedio
     * @param plugin El plugin principal
     * @param item El estandarte
     * @return Array con [atacante, ciudad_objetivo, timestamp] o null
     */
    public static String[] getSiegeFlagData(Plugin plugin, ItemStack item) {
        if (!isSiegeFlag(plugin, item)) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, SIEGE_FLAG_KEY);
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        return data != null ? data.split(":") : null;
    }

    /**
     * Obtiene el nombre de la ciudad de una bandera
     * @param plugin El plugin principal
     * @param item La bandera
     * @return El nombre de la ciudad o null
     */
    public static String getCityFlagData(Plugin plugin, ItemStack item) {
        if (!isCityFlag(plugin, item)) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, CITY_FLAG_KEY);

        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /**
     * Crea un item especial con datos personalizados
     * @param plugin El plugin principal
     * @param material El material
     * @param displayName El nombre
     * @param lore La descripción
     * @param dataKey Clave de datos
     * @param dataValue Valor de datos
     * @return El item especial
     */
    public static ItemStack createSpecialItem(Plugin plugin, Material material, String displayName,
                                              List<String> lore, String dataKey, String dataValue) {
        ItemStack item = createItem(material, 1, displayName, lore);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, dataKey);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, dataValue);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Verifica si un item tiene datos específicos
     * @param plugin El plugin principal
     * @param item El item
     * @param dataKey La clave de datos
     * @return true si tiene los datos
     */
    public static boolean hasSpecialData(Plugin plugin, ItemStack item, String dataKey) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, dataKey);

        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    /**
     * Obtiene datos especiales de un item
     * @param plugin El plugin principal
     * @param item El item
     * @param dataKey La clave de datos
     * @return El valor de los datos o null
     */
    public static String getSpecialData(Plugin plugin, ItemStack item, String dataKey) {
        if (!hasSpecialData(plugin, item, dataKey)) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, dataKey);

        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /**
     * Añade brillo a un item
     * @param item El item
     * @return El item con brillo
     */
    public static ItemStack addGlow(ItemStack item) {
        if (item == null) return null;

        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Remueve el brillo de un item
     * @param item El item
     * @return El item sin brillo
     */
    public static ItemStack removeGlow(ItemStack item) {
        if (item == null) return null;

        item.removeEnchantment(Enchantment.UNBREAKING);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Clona un ItemStack de forma segura
     * @param item El item a clonar
     * @return El item clonado o null
     */
    public static ItemStack cloneItem(ItemStack item) {
        return item != null ? item.clone() : null;
    }

    /**
     * Compara dos items ignorando la cantida
     * @param item1 Primer item
     * @param item2 Segundo item
     * @return true si son similares
     */
    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;

        return item1.isSimilar(item2);
    }

    /**
     * Verifica si un item es válido (no null y no air)
     * @param item El item
     * @return true si es válido
     */
    public static boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() > 0;
    }

    /**
     * Crea un item de información/GUI
     * @param material El material
     * @param displayName El nombre
     * @param lore La descripción
     * @return El item de información
     */
    public static ItemStack createInfoItem(Material material, String displayName, String... lore) {
        ItemStack item = createItem(material, 1, displayName, lore);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Hacer que el item no se pueda colocar/usar
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Actualiza el lore de un item
     * @param item El item
     * @param newLore El nuevo lore
     * @return El item actualizado
     */
    public static ItemStack updateLore(ItemStack item, List<String> newLore) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> colorizedLore = new ArrayList<>();
            newLore.forEach(line -> colorizedLore.add(MessageUtils.colorize(line)));
            meta.setLore(colorizedLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Actualiza el nombre de un item
     * @param item El item
     * @param newName El nuevo nombre
     * @return El item actualizado
     */
    public static ItemStack updateDisplayName(ItemStack item, String newName) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize(newName));
            item.setItemMeta(meta);
        }

        return item;
    }
}
