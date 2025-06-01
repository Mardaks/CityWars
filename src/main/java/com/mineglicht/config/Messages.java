package com.mineglicht.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Clase que maneja todos los mensajes configurables del plugin
 * Proporciona acceso estático a los mensajes con colores y placeholders
 */
public class Messages {

    private static FileConfiguration config;

    // === MENSAJES GENERALES ===
    public static String PREFIX;
    public static String NO_PERMISSION;
    public static String PLAYER_NOT_FOUND;
    public static String PLAYER_NOT_CITIZEN;
    public static String PLAYER_COMMAND_ONLY;
    public static String NO_CITIZENS;
    public static String COMMAND_ONLY_PLAYER;
    public static String CONFIG_RELOADED;
    public static String INVALID_ARGUMENTS;

    // === MENSAJES GENERALES ===
    public static String CITIZEN_ERROR_REGISTERED;
    public static String CITIZEN_ADDED;
    public static String CITIZEN_ADD_FAILED;
    public static String CITIZEN_REMOVED;
    public static String CITIZEN_REMOVE_FAILED;

    // === MENSAJES DE CIUDAD ===
    public static String CITY_CREATED;
    public static String CITY_CREATION_FAILED;
    public static String CITY_DELETED;
    public static String CITY_DELETION_FAILED;
    public static String CITY_NOT_FOUND;
    public static String CITY_NOT_UNDER_SIEGE;
    public static String CITY_UNDER_SIEGE;
    public static String CITY_ALREADY_EXISTS;
    public static String CITY_JOIN_SUCCESS;
    public static String CITY_LEAVE_SUCCESS;
    public static String CITY_ALREADY_MEMBER;
    public static String CITY_NOT_MEMBER;
    public static String CITY_EXPANDED;
    public static String CITY_EXPANSION_FAILED;
    public static String CITY_INSUFFICIENT_FUNDS;
    public static String CITY_MAX_CITIZENS_REACHED;
    public static String CITY_INFO_HEADER;
    public static List<String> CITY_INFO_FORMAT;

    // === MENSAJES DE ASEDIO ===
    public static String SIEGE_STARTED;
    public static String SIEGE_ENDED;
    public static String SIEGE_ENDED_FAILED;
    public static String SIEGE_VICTORY;
    public static String SIEGE_DEFEAT;
    public static String SIEGE_UNDER_ATTACK;
    public static String SIEGE_PROTECTOR_ATTACKED;
    public static String SIEGE_INSUFFICIENT_DEFENDERS;
    public static String SIEGE_ALREADY_ACTIVE;
    public static String SIEGE_CANNOT_ATTACK_SELF;
    public static String SIEGE_CITY_ATTACKING;
    public static String SIEGE_CITY_UNDER_ATTACK;
    public static String SIEGE_COOLDOWN_ACTIVE;
    public static String SIEGE_FLAG_PLACED;
    public static String SIEGE_FLAG_DESTROYED;
    public static String SIEGE_INSUFFICIENT_FUNDS;
    public static String SIEGE_INVALID_LOCATION;

    // === MENSAJES DE SAQUEO ===
    public static String LOOT_PHASE_STARTED;
    public static String LOOT_PHASE_ENDED;
    public static String LOOT_FUNDS_STOLEN;
    public static String LOOT_CHEST_OPENED;
    public static String LOOT_BLOCK_BROKEN;
    public static String LOOT_PROTECTION_DISABLED;
    public static String LOOT_PROTECTION_RESTORED;

    // === MENSAJES DE IMPUESTOS ===
    public static String TAX_COLLECTED;
    public static String TAX_INSUFFICIENT_BALANCE;
    public static String TAX_COLLECTION_NOTICE;
    public static String TAX_COLLECTION_FAILED;
    public static String TAX_RATE_CHANGED;

    // === MENSAJES DE PROTECCIÓN ===
    public static String PROTECTION_BLOCK_BREAK;
    public static String PROTECTION_BLOCK_PLACE;
    public static String PROTECTION_INTERACT;
    public static String PROTECTION_PVP;
    public static String PROTECTION_ENDERPEARL;
    public static String PROTECTION_MOB_SPAWN;

    // === MENSAJES DE ECONOMÍA ===
    public static String ECONOMY_DEPOSIT_SUCCESS;
    public static String ECONOMY_WITHDRAW_SUCCESS;
    public static String ECONOMY_INSUFFICIENT_FUNDS;
    public static String ECONOMY_BALANCE_SHOW;
    public static String ECONOMY_TRANSACTION_FAILED;

    // === MENSAJES DE COMANDOS ===
    public static String COMMAND_HELP_HEADER;
    public static List<String> COMMAND_HELP_LIST;
    public static String COMMAND_USAGE;

    // === MENSAJES DE EVENTOS ===
    public static String EVENT_CITY_CREATED_BROADCAST;
    public static String EVENT_CITY_DELETED_BROADCAST;
    public static String EVENT_SIEGE_STARTED_BROADCAST;
    public static String EVENT_SIEGE_ENDED_BROADCAST;

    // === SUBTÍTULOS Y TÍTULOS ===
    public static String TITLE_UNDER_ATTACK;
    public static String SUBTITLE_UNDER_ATTACK;
    public static String TITLE_PROTECTOR_ATTACKED;
    public static String SUBTITLE_PROTECTOR_ATTACKED;
    public static String TITLE_SIEGE_VICTORY;
    public static String SUBTITLE_SIEGE_VICTORY;
    public static String TITLE_SIEGE_DEFEAT;
    public static String SUBTITLE_SIEGE_DEFEAT;

    /**
     * Inicializa todos los mensajes desde el archivo messages.yml
     * @param configuration Configuración cargada del archivo
     */
    public static void initialize(FileConfiguration configuration) {
        config = configuration;
        loadAllMessages();
    }

    /**
     * Carga todos los mensajes desde el archivo
     */
    private static void loadAllMessages() {
        // Mensajes generales
        PREFIX = colorize(config.getString("general.prefix", "&8[&6CityWars&8] &r"));
        NO_PERMISSION = colorize(config.getString("general.no-permission", "&cNo tienes permisos para hacer esto."));
        PLAYER_NOT_FOUND = colorize(config.getString("general.player-not-found", "&cJugador no encontrado."));
        COMMAND_ONLY_PLAYER = colorize(config.getString("general.command-only-player", "&cEste comando solo puede ser usado por jugadores."));
        CONFIG_RELOADED = colorize(config.getString("general.config-reloaded", "&aConfiguración recargada correctamente."));
        INVALID_ARGUMENTS = colorize(config.getString("general.invalid-arguments", "&cArgumentos inválidos. Usa: {usage}"));

        // Mensajes de ciudad
        CITY_CREATED = colorize(config.getString("city.created", "&a¡Ciudad '{city}' creada exitosamente!"));
        CITY_DELETED = colorize(config.getString("city.deleted", "&c¡Ciudad '{city}' eliminada!"));
        CITY_NOT_FOUND = colorize(config.getString("city.not-found", "&cLa ciudad '{city}' no existe."));
        CITY_ALREADY_EXISTS = colorize(config.getString("city.already-exists", "&cYa existe una ciudad con ese nombre."));
        CITY_JOIN_SUCCESS = colorize(config.getString("city.join-success", "&a¡Te has unido a la ciudad '{city}'!"));
        CITY_LEAVE_SUCCESS = colorize(config.getString("city.leave-success", "&a¡Has abandonado la ciudad '{city}'!"));
        CITY_ALREADY_MEMBER = colorize(config.getString("city.already-member", "&cYa eres miembro de una ciudad."));
        CITY_NOT_MEMBER = colorize(config.getString("city.not-member", "&cNo eres miembro de ninguna ciudad."));
        CITY_EXPANDED = colorize(config.getString("city.expanded", "&a¡Ciudad expandida {blocks} bloques hacia {direction}!"));
        CITY_EXPANSION_FAILED = colorize(config.getString("city.expansion-failed", "&cNo se pudo expandir la ciudad."));
        CITY_INSUFFICIENT_FUNDS = colorize(config.getString("city.insufficient-funds", "&cLa ciudad no tiene fondos suficientes."));
        CITY_MAX_CITIZENS_REACHED = colorize(config.getString("city.max-citizens-reached", "&cLa ciudad ha alcanzado el máximo de ciudadanos."));
        CITY_INFO_HEADER = colorize(config.getString("city.info-header", "&6=== Información de {city} ==="));
        CITY_INFO_FORMAT = config.getStringList("city.info-format").stream()
                .map(Messages::colorize)
                .collect(Collectors.toList());

        // Mensajes de asedio
        SIEGE_STARTED = colorize(config.getString("siege.started", "&c¡Asedio iniciado! {attacker} vs {defender}"));
        SIEGE_ENDED = colorize(config.getString("siege.ended", "&a¡Asedio terminado! Razón: {reason}"));
        SIEGE_VICTORY = colorize(config.getString("siege.victory", "&a¡Victoria! Has conquistado {city}!"));
        SIEGE_DEFEAT = colorize(config.getString("siege.defeat", "&c¡Derrota! Tu ciudad ha sido conquistada."));
        SIEGE_UNDER_ATTACK = colorize(config.getString("siege.under-attack", "&c¡Estás bajo ataque!"));
        SIEGE_PROTECTOR_ATTACKED = colorize(config.getString("siege.protector-attacked", "&c¡Protector atacado!"));
        SIEGE_INSUFFICIENT_DEFENDERS = colorize(config.getString("siege.insufficient-defenders", "&cNo hay suficientes defensores conectados ({percentage}% requerido)."));
        SIEGE_ALREADY_ACTIVE = colorize(config.getString("siege.already-active", "&cYa hay un asedio activo."));
        SIEGE_CANNOT_ATTACK_SELF = colorize(config.getString("siege.cannot-attack-self", "&cNo puedes atacar tu propia ciudad."));
        SIEGE_CITY_ATTACKING = colorize(config.getString("siege.city-attacking", "&cTu ciudad está atacando y no puede ser atacada."));
        SIEGE_CITY_UNDER_ATTACK = colorize(config.getString("siege.city-under-attack", "&cTu ciudad está bajo ataque y no puede atacar."));
        SIEGE_COOLDOWN_ACTIVE = colorize(config.getString("siege.cooldown-active", "&cHay un cooldown activo entre estas ciudades. Tiempo restante: {time}"));
        SIEGE_FLAG_PLACED = colorize(config.getString("siege.flag-placed", "&c¡Bandera de asedio colocada en {city}!"));
        SIEGE_FLAG_DESTROYED = colorize(config.getString("siege.flag-destroyed", "&a¡Bandera de asedio destruida!"));
        SIEGE_INSUFFICIENT_FUNDS = colorize(config.getString("siege.insufficient-funds", "&cNo tienes suficientes {economy} para iniciar un asedio."));
        SIEGE_INVALID_LOCATION = colorize(config.getString("siege.invalid-location", "&cNo puedes colocar la bandera aquí."));

        // Mensajes de saqueo
        LOOT_PHASE_STARTED = colorize(config.getString("loot.phase-started", "&c¡Fase de saqueo iniciada! Duración: {duration} minutos"));
        LOOT_PHASE_ENDED = colorize(config.getString("loot.phase-ended", "&a¡Fase de saqueo terminada!"));
        LOOT_FUNDS_STOLEN = colorize(config.getString("loot.funds-stolen", "&c¡{amount} {economy} robados del banco de la ciudad!"));
        LOOT_CHEST_OPENED = colorize(config.getString("loot.chest-opened", "&6Cofre saqueado por {player}"));
        LOOT_BLOCK_BROKEN = colorize(config.getString("loot.block-broken", "&6Bloque destruido por {player} durante el saqueo"));
        LOOT_PROTECTION_DISABLED = colorize(config.getString("loot.protection-disabled", "&c¡Protecciones deshabilitadas!"));
        LOOT_PROTECTION_RESTORED = colorize(config.getString("loot.protection-restored", "&a¡Protecciones restauradas!"));

        // Mensajes de impuestos
        TAX_COLLECTED = colorize(config.getString("tax.collected", "&aSe han cobrado {amount} {economy} en impuestos."));
        TAX_INSUFFICIENT_BALANCE = colorize(config.getString("tax.insufficient-balance", "&cNo tienes suficiente dinero para pagar los impuestos."));
        TAX_COLLECTION_NOTICE = colorize(config.getString("tax.collection-notice", "&6¡Recordatorio! Los impuestos se cobrarán en {time}."));
        TAX_COLLECTION_FAILED = colorize(config.getString("tax.collection-failed", "&cError al cobrar impuestos a {player}."));
        TAX_RATE_CHANGED = colorize(config.getString("tax.rate-changed", "&aTasa de impuestos cambiada a {rate}%."));

        // Mensajes de protección
        PROTECTION_BLOCK_BREAK = colorize(config.getString("protection.block-break", "&cNo puedes romper bloques en esta ciudad."));
        PROTECTION_BLOCK_PLACE = colorize(config.getString("protection.block-place", "&cNo puedes colocar bloques en esta ciudad."));
        PROTECTION_INTERACT = colorize(config.getString("protection.interact", "&cNo puedes interactuar en esta ciudad."));
        PROTECTION_PVP = colorize(config.getString("protection.pvp", "&cEl PvP está deshabilitado en esta ciudad."));
        PROTECTION_ENDERPEARL = colorize(config.getString("protection.enderpearl", "&cNo puedes usar enderpearls en esta ciudad."));
        PROTECTION_MOB_SPAWN = colorize(config.getString("protection.mob-spawn", "&cLos mobs no pueden aparecer en esta ciudad."));

        // Mensajes de economía
        ECONOMY_DEPOSIT_SUCCESS = colorize(config.getString("economy.deposit-success", "&a{amount} {economy} depositados en el banco de la ciudad."));
        ECONOMY_WITHDRAW_SUCCESS = colorize(config.getString("economy.withdraw-success", "&a{amount} {economy} retirados del banco de la ciudad."));
        ECONOMY_INSUFFICIENT_FUNDS = colorize(config.getString("economy.insufficient-funds", "&cFondos insuficientes."));
        ECONOMY_BALANCE_SHOW = colorize(config.getString("economy.balance-show", "&6Balance: {amount} {economy}"));
        ECONOMY_TRANSACTION_FAILED = colorize(config.getString("economy.transaction-failed", "&cTransacción fallida."));

        // Mensajes de comandos
        COMMAND_HELP_HEADER = colorize(config.getString("commands.help-header", "&6=== Comandos de CityWars ==="));
        COMMAND_HELP_LIST = config.getStringList("commands.help-list").stream()
                .map(Messages::colorize)
                .collect(Collectors.toList());
        COMMAND_USAGE = colorize(config.getString("commands.usage", "&cUso: {usage}"));

        // Mensajes de eventos
        EVENT_CITY_CREATED_BROADCAST = colorize(config.getString("events.city-created-broadcast", "&a¡Nueva ciudad '{city}' creada por {player}!"));
        EVENT_CITY_DELETED_BROADCAST = colorize(config.getString("events.city-deleted-broadcast", "&c¡Ciudad '{city}' eliminada!"));
        EVENT_SIEGE_STARTED_BROADCAST = colorize(config.getString("events.siege-started-broadcast", "&c¡Guerra! {attacker} está atacando {defender}!"));
        EVENT_SIEGE_ENDED_BROADCAST = colorize(config.getString("events.siege-ended-broadcast", "&a¡Guerra terminada! {winner} vs {loser}"));

        // Títulos y subtítulos
        TITLE_UNDER_ATTACK = colorize(config.getString("titles.under-attack.title", "&c&l¡BAJO ATAQUE!"));
        SUBTITLE_UNDER_ATTACK = colorize(config.getString("titles.under-attack.subtitle", "&fTu ciudad está siendo asediada"));
        TITLE_PROTECTOR_ATTACKED = colorize(config.getString("titles.protector-attacked.title", "&4&l¡PROTECTOR ATACADO!"));
        SUBTITLE_PROTECTOR_ATTACKED = colorize(config.getString("titles.protector-attacked.subtitle", "&fDefiendan al protector"));
        TITLE_SIEGE_VICTORY = colorize(config.getString("titles.siege-victory.title", "&a&l¡VICTORIA!"));
        SUBTITLE_SIEGE_VICTORY = colorize(config.getString("titles.siege-victory.subtitle", "&fHan conquistado {city}"));
        TITLE_SIEGE_DEFEAT = colorize(config.getString("titles.siege-defeat.title", "&c&l¡DERROTA!"));
        SUBTITLE_SIEGE_DEFEAT = colorize(config.getString("titles.siege-defeat.subtitle", "&fSu ciudad ha sido conquistada"));
    }

    /**
     * Convierte códigos de color de Minecraft
     * @param text Texto con códigos de color
     * @return Texto con colores aplicados
     */
    private static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Obtiene un mensaje con el prefijo
     * @param message Mensaje a mostrar
     * @return Mensaje con prefijo
     */
    public static String getWithPrefix(String message) {
        return PREFIX + message;
    }

    /**
     * Reemplaza placeholders en un mensaje
     * @param message Mensaje original
     * @param placeholder Placeholder a reemplazar
     * @param value Valor del placeholder
     * @return Mensaje con placeholder reemplazado
     */
    public static String replacePlaceholder(String message, String placeholder, String value) {
        if (message == null || placeholder == null || value == null) return message;
        return message.replace("{" + placeholder + "}", value);
    }

    /**
     * Reemplaza múltiples placeholders en un mensaje
     * @param message Mensaje original
     * @param placeholders Array de placeholders (placeholder, valor, placeholder, valor...)
     * @return Mensaje con placeholders reemplazados
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (message == null || placeholders == null || placeholders.length % 2 != 0) {
            return message;
        }

        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            result = replacePlaceholder(result, placeholders[i], placeholders[i + 1]);
        }
        return result;
    }

    /**
     * Envía un mensaje a un jugador con prefijo
     * @param player Jugador destinatario
     * @param message Mensaje a enviar
     */
    public static void send(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(getWithPrefix(message));
        }
    }

    /**
     * Envía un mensaje a un jugador con prefijo y placeholders
     * @param player Jugador destinatario
     * @param message Mensaje a enviar
     * @param placeholders Placeholders a reemplazar
     */
    public static void send(Player player, String message, String... placeholders) {
        if (player != null && message != null) {
            String processedMessage = replacePlaceholders(message, placeholders);
            player.sendMessage(getWithPrefix(processedMessage));
        }
    }

    /**
     * Envía un título a un jugador
     * @param player Jugador destinatario
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de permanencia (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            player.sendTitle(
                    title != null ? title : "",
                    subtitle != null ? subtitle : "",
                    fadeIn, stay, fadeOut
            );
        }
    }

    /**
     * Envía un título a un jugador con placeholders
     * @param player Jugador destinatario
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de permanencia (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @param placeholders Placeholders a reemplazar
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut, String... placeholders) {
        if (player != null) {
            String processedTitle = replacePlaceholders(title, placeholders);
            String processedSubtitle = replacePlaceholders(subtitle, placeholders);
            sendTitle(player, processedTitle, processedSubtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Valida que todos los mensajes sean válidos
     * @return true si los mensajes son válidos
     */
    public static boolean validateMessages() {
        try {
            // Verificar que los mensajes esenciales no sean nulos
            return PREFIX != null &&
                    NO_PERMISSION != null &&
                    CITY_CREATED != null &&
                    SIEGE_STARTED != null &&
                    COMMAND_HELP_HEADER != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recarga todos los mensajes
     */
    public static void reload() {
        if (config != null) {
            loadAllMessages();
        }
    }

    /**
     * Obtiene un mensaje personalizado del archivo de configuración
     * @param path Ruta del mensaje
     * @param defaultValue Valor por defecto
     * @return Mensaje con colores aplicados
     */
    public static String getCustomMessage(String path, String defaultValue) {
        if (config == null) return colorize(defaultValue);
        return colorize(config.getString(path, defaultValue));
    }

    /**
     * Obtiene una lista de mensajes personalizada del archivo de configuración
     * @param path Ruta de la lista
     * @return Lista de mensajes con colores aplicados
     */
    public static List<String> getCustomMessageList(String path) {
        if (config == null) return List.of();
        return config.getStringList(path).stream()
                .map(Messages::colorize)
                .collect(Collectors.toList());
    }
}
