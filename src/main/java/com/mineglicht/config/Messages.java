package com.mineglicht.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clase optimizada que maneja todos los mensajes configurables del plugin
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

    // === MENSAJES DE CIUDADANOS ===
    public static String CITIZEN_REGISTERED;
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
     * 
     * @param configuration Configuración cargada del archivo
     */
    public static void initialize(FileConfiguration configuration) {
        config = configuration;
        loadAllMessages();
    }

    /**
     * Carga todos los mensajes desde el archivo de configuración
     */
    private static void loadAllMessages() {
        loadGeneralMessages();
        loadCitizenMessages();
        loadCityMessages();
        loadSiegeMessages();
        loadLootMessages();
        loadTaxMessages();
        loadProtectionMessages();
        loadEconomyMessages();
        loadCommandMessages();
        loadEventMessages();
        loadTitleMessages();
    }

    /**
     * Carga mensajes generales
     */
    private static void loadGeneralMessages() {
        PREFIX = getConfigString("general.prefix", "&8[&6CityWars&8] &r");
        NO_PERMISSION = getConfigString("general.no-permission", "&cNo tienes permisos para hacer esto.");
        PLAYER_NOT_FOUND = getConfigString("general.player-not-found", "&cJugador no encontrado.");
        PLAYER_NOT_CITIZEN = getConfigString("general.player-not-citizen", "&cEste jugador no es ciudadano.");
        PLAYER_COMMAND_ONLY = getConfigString("general.player-command-only", "&cEste comando solo puede ser usado por jugadores.");
        NO_CITIZENS = getConfigString("general.no-citizens", "&cNo hay ciudadanos registrados.");
        COMMAND_ONLY_PLAYER = getConfigString("general.command-only-player", "&cEste comando solo puede ser usado por jugadores.");
        CONFIG_RELOADED = getConfigString("general.config-reloaded", "&aConfiguración recargada correctamente.");
        INVALID_ARGUMENTS = getConfigString("general.invalid-arguments", "&cArgumentos inválidos. Usa: {usage}");
    }

    /**
     * Carga mensajes de ciudadanos
     */
    private static void loadCitizenMessages() {
        CITIZEN_REGISTERED = getConfigString("citizen.registered", "&a¡Te has registrado como ciudadano!");
        CITIZEN_ERROR_REGISTERED = getConfigString("citizen.error-registered", "&cError al registrarte como ciudadano.");
        CITIZEN_ADDED = getConfigString("citizen.added", "&a¡Ciudadano {player} añadido exitosamente!");
        CITIZEN_ADD_FAILED = getConfigString("citizen.add-failed", "&cNo se pudo añadir al ciudadano.");
        CITIZEN_REMOVED = getConfigString("citizen.removed", "&c¡Ciudadano {player} removido!");
        CITIZEN_REMOVE_FAILED = getConfigString("citizen.remove-failed", "&cNo se pudo remover al ciudadano.");
    }

    /**
     * Carga mensajes de ciudad
     */
    private static void loadCityMessages() {
        CITY_CREATED = getConfigString("city.created", "&a¡Ciudad '{city}' creada exitosamente!");
        CITY_CREATION_FAILED = getConfigString("city.creation-failed", "&cError al crear la ciudad.");
        CITY_DELETED = getConfigString("city.deleted", "&c¡Ciudad '{city}' eliminada!");
        CITY_DELETION_FAILED = getConfigString("city.deletion-failed", "&cError al eliminar la ciudad.");
        CITY_NOT_FOUND = getConfigString("city.not-found", "&cLa ciudad '{city}' no existe.");
        CITY_NOT_UNDER_SIEGE = getConfigString("city.not-under-siege", "&cLa ciudad no está bajo asedio.");
        CITY_UNDER_SIEGE = getConfigString("city.under-siege", "&cLa ciudad está bajo asedio.");
        CITY_ALREADY_EXISTS = getConfigString("city.already-exists", "&cYa existe una ciudad con ese nombre.");
        CITY_JOIN_SUCCESS = getConfigString("city.join-success", "&a¡Te has unido a la ciudad '{city}'!");
        CITY_LEAVE_SUCCESS = getConfigString("city.leave-success", "&a¡Has abandonado la ciudad '{city}'!");
        CITY_ALREADY_MEMBER = getConfigString("city.already-member", "&cYa eres miembro de una ciudad.");
        CITY_NOT_MEMBER = getConfigString("city.not-member", "&cNo eres miembro de ninguna ciudad.");
        CITY_EXPANDED = getConfigString("city.expanded", "&a¡Ciudad expandida {blocks} bloques hacia {direction}!");
        CITY_EXPANSION_FAILED = getConfigString("city.expansion-failed", "&cNo se pudo expandir la ciudad.");
        CITY_INSUFFICIENT_FUNDS = getConfigString("city.insufficient-funds", "&cLa ciudad no tiene fondos suficientes.");
        CITY_MAX_CITIZENS_REACHED = getConfigString("city.max-citizens-reached", "&cLa ciudad ha alcanzado el máximo de ciudadanos.");
        CITY_INFO_HEADER = getConfigString("city.info-header", "&6=== Información de {city} ===");
        CITY_INFO_FORMAT = getConfigStringList("city.info-format");
    }

    /**
     * Carga mensajes de asedio
     */
    private static void loadSiegeMessages() {
        SIEGE_STARTED = getConfigString("siege.started", "&c¡Asedio iniciado! {attacker} vs {defender}");
        SIEGE_ENDED = getConfigString("siege.ended", "&a¡Asedio terminado! Razón: {reason}");
        SIEGE_ENDED_FAILED = getConfigString("siege.ended-failed", "&cError al terminar el asedio.");
        SIEGE_VICTORY = getConfigString("siege.victory", "&a¡Victoria! Has conquistado {city}!");
        SIEGE_DEFEAT = getConfigString("siege.defeat", "&c¡Derrota! Tu ciudad ha sido conquistada.");
        SIEGE_UNDER_ATTACK = getConfigString("siege.under-attack", "&c¡Estás bajo ataque!");
        SIEGE_PROTECTOR_ATTACKED = getConfigString("siege.protector-attacked", "&c¡Protector atacado!");
        SIEGE_INSUFFICIENT_DEFENDERS = getConfigString("siege.insufficient-defenders", "&cNo hay suficientes defensores conectados ({percentage}% requerido).");
        SIEGE_ALREADY_ACTIVE = getConfigString("siege.already-active", "&cYa hay un asedio activo.");
        SIEGE_CANNOT_ATTACK_SELF = getConfigString("siege.cannot-attack-self", "&cNo puedes atacar tu propia ciudad.");
        SIEGE_CITY_ATTACKING = getConfigString("siege.city-attacking", "&cTu ciudad está atacando y no puede ser atacada.");
        SIEGE_CITY_UNDER_ATTACK = getConfigString("siege.city-under-attack", "&cTu ciudad está bajo ataque y no puede atacar.");
        SIEGE_COOLDOWN_ACTIVE = getConfigString("siege.cooldown-active", "&cHay un cooldown activo entre estas ciudades. Tiempo restante: {time}");
        SIEGE_FLAG_PLACED = getConfigString("siege.flag-placed", "&c¡Bandera de asedio colocada en {city}!");
        SIEGE_FLAG_DESTROYED = getConfigString("siege.flag-destroyed", "&a¡Bandera de asedio destruida!");
        SIEGE_INSUFFICIENT_FUNDS = getConfigString("siege.insufficient-funds", "&cNo tienes suficientes {economy} para iniciar un asedio.");
        SIEGE_INVALID_LOCATION = getConfigString("siege.invalid-location", "&cNo puedes colocar la bandera aquí.");
    }

    /**
     * Carga mensajes de saqueo
     */
    private static void loadLootMessages() {
        LOOT_PHASE_STARTED = getConfigString("loot.phase-started", "&c¡Fase de saqueo iniciada! Duración: {duration} minutos");
        LOOT_PHASE_ENDED = getConfigString("loot.phase-ended", "&a¡Fase de saqueo terminada!");
        LOOT_FUNDS_STOLEN = getConfigString("loot.funds-stolen", "&c¡{amount} {economy} robados del banco de la ciudad!");
        LOOT_CHEST_OPENED = getConfigString("loot.chest-opened", "&6Cofre saqueado por {player}");
        LOOT_BLOCK_BROKEN = getConfigString("loot.block-broken", "&6Bloque destruido por {player} durante el saqueo");
        LOOT_PROTECTION_DISABLED = getConfigString("loot.protection-disabled", "&c¡Protecciones deshabilitadas!");
        LOOT_PROTECTION_RESTORED = getConfigString("loot.protection-restored", "&a¡Protecciones restauradas!");
    }

    /**
     * Carga mensajes de impuestos
     */
    private static void loadTaxMessages() {
        TAX_COLLECTED = getConfigString("tax.collected", "&aSe han cobrado {amount} {economy} en impuestos.");
        TAX_INSUFFICIENT_BALANCE = getConfigString("tax.insufficient-balance", "&cNo tienes suficiente dinero para pagar los impuestos.");
        TAX_COLLECTION_NOTICE = getConfigString("tax.collection-notice", "&6¡Recordatorio! Los impuestos se cobrarán en {time}.");
        TAX_COLLECTION_FAILED = getConfigString("tax.collection-failed", "&cError al cobrar impuestos a {player}.");
        TAX_RATE_CHANGED = getConfigString("tax.rate-changed", "&aTasa de impuestos cambiada a {rate}%.");
    }

    /**
     * Carga mensajes de protección
     */
    private static void loadProtectionMessages() {
        PROTECTION_BLOCK_BREAK = getConfigString("protection.block-break", "&cNo puedes romper bloques en esta ciudad.");
        PROTECTION_BLOCK_PLACE = getConfigString("protection.block-place", "&cNo puedes colocar bloques en esta ciudad.");
        PROTECTION_INTERACT = getConfigString("protection.interact", "&cNo puedes interactuar en esta ciudad.");
        PROTECTION_PVP = getConfigString("protection.pvp", "&cEl PvP está deshabilitado en esta ciudad.");
        PROTECTION_ENDERPEARL = getConfigString("protection.enderpearl", "&cNo puedes usar enderpearls en esta ciudad.");
        PROTECTION_MOB_SPAWN = getConfigString("protection.mob-spawn", "&cLos mobs no pueden aparecer en esta ciudad.");
    }

    /**
     * Carga mensajes de economía
     */
    private static void loadEconomyMessages() {
        ECONOMY_DEPOSIT_SUCCESS = getConfigString("economy.deposit-success", "&a{amount} {economy} depositados en el banco de la ciudad.");
        ECONOMY_WITHDRAW_SUCCESS = getConfigString("economy.withdraw-success", "&a{amount} {economy} retirados del banco de la ciudad.");
        ECONOMY_INSUFFICIENT_FUNDS = getConfigString("economy.insufficient-funds", "&cFondos insuficientes.");
        ECONOMY_BALANCE_SHOW = getConfigString("economy.balance-show", "&6Balance: {amount} {economy}");
        ECONOMY_TRANSACTION_FAILED = getConfigString("economy.transaction-failed", "&cTransacción fallida.");
    }

    /**
     * Carga mensajes de comandos
     */
    private static void loadCommandMessages() {
        COMMAND_HELP_HEADER = getConfigString("commands.help-header", "&6=== Comandos de CityWars ===");
        COMMAND_HELP_LIST = getConfigStringList("commands.help-list");
        COMMAND_USAGE = getConfigString("commands.usage", "&cUso: {usage}");
    }

    /**
     * Carga mensajes de eventos
     */
    private static void loadEventMessages() {
        EVENT_CITY_CREATED_BROADCAST = getConfigString("events.city-created-broadcast", "&a¡Nueva ciudad '{city}' creada por {player}!");
        EVENT_CITY_DELETED_BROADCAST = getConfigString("events.city-deleted-broadcast", "&c¡Ciudad '{city}' eliminada!");
        EVENT_SIEGE_STARTED_BROADCAST = getConfigString("events.siege-started-broadcast", "&c¡Guerra! {attacker} está atacando {defender}!");
        EVENT_SIEGE_ENDED_BROADCAST = getConfigString("events.siege-ended-broadcast", "&a¡Guerra terminada! {winner} vs {loser}");
    }

    /**
     * Carga títulos y subtítulos
     */
    private static void loadTitleMessages() {
        TITLE_UNDER_ATTACK = getConfigString("titles.under-attack.title", "&c&l¡BAJO ATAQUE!");
        SUBTITLE_UNDER_ATTACK = getConfigString("titles.under-attack.subtitle", "&fTu ciudad está siendo asediada");
        TITLE_PROTECTOR_ATTACKED = getConfigString("titles.protector-attacked.title", "&4&l¡PROTECTOR ATACADO!");
        SUBTITLE_PROTECTOR_ATTACKED = getConfigString("titles.protector-attacked.subtitle", "&fDefiendan al protector");
        TITLE_SIEGE_VICTORY = getConfigString("titles.siege-victory.title", "&a&l¡VICTORIA!");
        SUBTITLE_SIEGE_VICTORY = getConfigString("titles.siege-victory.subtitle", "&fHan conquistado {city}");
        TITLE_SIEGE_DEFEAT = getConfigString("titles.siege-defeat.title", "&c&l¡DERROTA!");
        SUBTITLE_SIEGE_DEFEAT = getConfigString("titles.siege-defeat.subtitle", "&fSu ciudad ha sido conquistada");
    }

    /**
     * Método auxiliar para obtener strings del config con colorización
     */
    private static String getConfigString(String path, String defaultValue) {
        return colorize(config != null ? config.getString(path, defaultValue) : defaultValue);
    }

    /**
     * Método auxiliar para obtener listas de strings del config con colorización
     */
    private static List<String> getConfigStringList(String path) {
        if (config == null) return List.of();
        return config.getStringList(path).stream()
                .map(Messages::colorize)
                .collect(Collectors.toList());
    }

    /**
     * Convierte códigos de color de Minecraft
     * 
     * @param text Texto con códigos de color
     * @return Texto con colores aplicados
     */
    private static String colorize(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Obtiene un mensaje con el prefijo
     * 
     * @param message Mensaje a mostrar
     * @return Mensaje con prefijo
     */
    public static String getWithPrefix(String message) {
        return PREFIX + message;
    }

    /**
     * Reemplaza placeholders en un mensaje
     * 
     * @param message     Mensaje original
     * @param placeholder Placeholder a reemplazar
     * @param value       Valor del placeholder
     * @return Mensaje con placeholder reemplazado
     */
    public static String replacePlaceholder(String message, String placeholder, String value) {
        if (message == null || placeholder == null || value == null) {
            return message;
        }
        return message.replace("{" + placeholder + "}", value);
    }

    /**
     * Reemplaza múltiples placeholders en un mensaje
     * 
     * @param message      Mensaje original
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
     * 
     * @param player  Jugador destinatario
     * @param message Mensaje a enviar
     */
    public static void send(Player player, String message) {
        if (player != null && player.isOnline() && message != null) {
            player.sendMessage(getWithPrefix(message));
        }
    }

    /**
     * Envía un mensaje a un jugador con prefijo y placeholders
     * 
     * @param player       Jugador destinatario
     * @param message      Mensaje a enviar
     * @param placeholders Placeholders a reemplazar
     */
    public static void send(Player player, String message, String... placeholders) {
        if (player != null && player.isOnline() && message != null) {
            String processedMessage = replacePlaceholders(message, placeholders);
            player.sendMessage(getWithPrefix(processedMessage));
        }
    }

    /**
     * Envía un título a un jugador
     * 
     * @param player   Jugador destinatario
     * @param title    Título principal
     * @param subtitle Subtítulo
     * @param fadeIn   Tiempo de aparición (ticks)
     * @param stay     Tiempo de permanencia (ticks)
     * @param fadeOut  Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null && player.isOnline()) {
            player.sendTitle(
                    title != null ? title : "",
                    subtitle != null ? subtitle : "",
                    fadeIn, stay, fadeOut);
        }
    }

    /**
     * Envía un título a un jugador con placeholders
     * 
     * @param player       Jugador destinatario
     * @param title        Título principal
     * @param subtitle     Subtítulo
     * @param fadeIn       Tiempo de aparición (ticks)
     * @param stay         Tiempo de permanencia (ticks)
     * @param fadeOut      Tiempo de desaparición (ticks)
     * @param placeholders Placeholders a reemplazar
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut,
                                String... placeholders) {
        if (player != null && player.isOnline()) {
            String processedTitle = replacePlaceholders(title, placeholders);
            String processedSubtitle = replacePlaceholders(subtitle, placeholders);
            sendTitle(player, processedTitle, processedSubtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Método para mensajes de impuestos (compatibilidad con código existente)
     * 
     * @param amount Cantidad de impuestos
     * @return Mensaje de impuestos cobrados con placeholder de cantidad reemplazado
     */
    public static String getTaxCollectedMessage(BigDecimal amount) {
        return replacePlaceholder(TAX_COLLECTED, "amount", amount.toString());
    }

    /**
     * Método optimizado para mensajes de impuestos con economía
     * 
     * @param amount   Cantidad de impuestos
     * @param economy  Nombre de la moneda
     * @return Mensaje de impuestos cobrados con placeholders reemplazados
     */
    public static String getTaxCollectedMessage(BigDecimal amount, String economy) {
        return replacePlaceholders(TAX_COLLECTED, 
            "amount", amount.toString(), 
            "economy", economy);
    }

    /**
     * Valida que todos los mensajes esenciales sean válidos
     * 
     * @return true si los mensajes son válidos
     */
    public static boolean validateMessages() {
        try {
            return Stream.of(
                PREFIX, NO_PERMISSION, CITY_CREATED, SIEGE_STARTED, 
                COMMAND_HELP_HEADER, CITY_INFO_HEADER, TAX_COLLECTED,
                ECONOMY_BALANCE_SHOW, PROTECTION_BLOCK_BREAK
            ).allMatch(Objects::nonNull);
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
     * 
     * @param path         Ruta del mensaje
     * @param defaultValue Valor por defecto
     * @return Mensaje con colores aplicados
     */
    public static String getCustomMessage(String path, String defaultValue) {
        return getConfigString(path, defaultValue);
    }

    /**
     * Obtiene una lista de mensajes personalizada del archivo de configuración
     * 
     * @param path Ruta de la lista
     * @return Lista de mensajes con colores aplicados
     */
    public static List<String> getCustomMessageList(String path) {
        return getConfigStringList(path);
    }
}