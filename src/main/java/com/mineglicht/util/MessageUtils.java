package com.mineglicht.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades para el manejo de mensajes, títulos y subtítulos
 */
public class MessageUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Colorea un mensaje usando códigos de color legacy y hex
     * @param message El mensaje a colorear
     * @return El mensaje coloreado
     */
    public static String colorize(String message) {
        if (message == null) return "";

        // Procesar colores hex (&#RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        // Procesar códigos de color legacy (&)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Envía un mensaje normal a un jugador
     * @param player El jugador
     * @param message El mensaje
     */
    public static void sendMessage(Player player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(colorize(message));
        }
    }

    /**
     * Envía múltiples mensajes a un jugador
     * @param player El jugador
     * @param messages Lista de mensajes
     */
    public static void sendMessages(Player player, List<String> messages) {
        if (player != null && messages != null) {
            messages.forEach(msg -> sendMessage(player, msg));
        }
    }

    /**
     * Envía un mensaje con prefijo a un jugador
     * @param player El jugador
     * @param message El mensaje
     * @param prefix El prefijo
     */
    public static void sendPrefixedMessage(Player player, String message, String prefix) {
        sendMessage(player, prefix + " " + message);
    }

    /**
     * Envía un título y subtítulo a un jugador
     * @param player El jugador
     * @param title El título principal
     * @param subtitle El subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de permanencia (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            player.sendTitle(
                    colorize(title != null ? title : ""),
                    colorize(subtitle != null ? subtitle : ""),
                    fadeIn, stay, fadeOut
            );
        }
    }

    /**
     * Envía un título y subtítulo con valores por defecto
     * @param player El jugador
     * @param title El título principal
     * @param subtitle El subtítulo
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    /**
     * Envía solo un subtítulo
     * @param player El jugador
     * @param subtitle El subtítulo
     */
    public static void sendSubtitle(Player player, String subtitle) {
        sendTitle(player, "", subtitle);
    }

    /**
     * Envía un mensaje en la action bar
     * @param player El jugador
     * @param message El mensaje
     */
    public static void sendActionBar(Player player, String message) {
        if (player != null && message != null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(colorize(message)));
        }
    }

    /**
     * Broadcast a todos los jugadores online
     * @param message El mensaje
     */
    public static void broadcast(String message) {
        Bukkit.broadcastMessage(colorize(message));
    }

    /**
     * Broadcast a todos los jugadores online con permiso
     * @param message El mensaje
     * @param permission El permiso requerido
     */
    public static void broadcastWithPermission(String message, String permission) {
        Bukkit.broadcast(colorize(message), permission);
    }

    /**
     * Envía un mensaje a todos los ciudadanos de una ciudad
     * @param citizens Lista de jugadores (ciudadanos)
     * @param message El mensaje
     */
    public static void sendToCitizens(List<Player> citizens, String message) {
        if (citizens != null && message != null) {
            citizens.forEach(citizen -> sendMessage(citizen, message));
        }
    }

    /**
     * Envía un subtítulo a todos los ciudadanos de una ciudad
     * @param citizens Lista de jugadores (ciudadanos)
     * @param subtitle El subtítulo
     */
    public static void sendSubtitleToCitizens(List<Player> citizens, String subtitle) {
        if (citizens != null && subtitle != null) {
            citizens.forEach(citizen -> sendSubtitle(citizen, subtitle));
        }
    }

    /**
     * Envía un título de asedio bajo ataque
     * @param player El jugador
     */
    public static void sendSiegeUnderAttackTitle(Player player) {
        sendSubtitle(player, "&c&l¡Estás bajo ataque!");
    }

    /**
     * Envía un título de protector atacado
     * @param player El jugador
     */
    public static void sendProtectorAttackedTitle(Player player) {
        sendSubtitle(player, "&4&l¡Protector atacado!");
    }

    /**
     * Envía mensajes de asedio a todos los ciudadanos
     * @param citizens Lista de jugadores (ciudadanos)
     * @param attackerName Nombre del atacante
     * @param cityName Nombre de la ciudad
     */
    public static void sendSiegeStartMessages(List<Player> citizens, String attackerName, String cityName) {
        String message = "&c&l¡ASEDIO INICIADO! &r&7" + attackerName + " está atacando " + cityName;
        sendToCitizens(citizens, message);
        sendSubtitleToCitizens(citizens, "&c&l¡Estás bajo ataque!");
    }

    /**
     * Envía mensajes de fin de asedio
     * @param citizens Lista de jugadores (ciudadanos)
     * @param cityName Nombre de la ciudad
     * @param victory Si fue victoria o derrota
     */
    public static void sendSiegeEndMessages(List<Player> citizens, String cityName, boolean victory) {
        String message = victory ?
                "&a&l¡VICTORIA! &r&7" + cityName + " ha defendido exitosamente su territorio" :
                "&c&l¡DERROTA! &r&7" + cityName + " ha sido saqueada";

        String subtitle = victory ? "&a&l¡Victoria!" : "&c&l¡Derrota!";

        sendToCitizens(citizens, message);
        sendSubtitleToCitizens(citizens, subtitle);
    }

    /**
     * Envía un mensaje de consola
     * @param plugin El plugin
     * @param message El mensaje
     * @param level El nivel (INFO, WARNING, SEVERE)
     */
    public static void sendConsoleMessage(Plugin plugin, String message, java.util.logging.Level level) {
        plugin.getLogger().log(level, message);
    }

    /**
     * Envía un mensaje de información a consola
     * @param plugin El plugin
     * @param message El mensaje
     */
    public static void sendConsoleInfo(Plugin plugin, String message) {
        sendConsoleMessage(plugin, message, java.util.logging.Level.INFO);
    }

    /**
     * Envía un mensaje de advertencia a consola
     * @param plugin El plugin
     * @param message El mensaje
     */
    public static void sendConsoleWarning(Plugin plugin, String message) {
        sendConsoleMessage(plugin, message, java.util.logging.Level.WARNING);
    }

    /**
     * Envía un mensaje de error a consola
     * @param plugin El plugin
     * @param message El mensaje
     */
    public static void sendConsoleError(Plugin plugin, String message) {
        sendConsoleMessage(plugin, message, java.util.logging.Level.SEVERE);
    }

    /**
     * Reemplaza placeholders en un mensaje
     * @param message El mensaje con placeholders
     * @param placeholders Array de placeholders y valores (placeholder, valor, placeholder, valor...)
     * @return El mensaje con placeholders reemplazados
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (message == null) return "";

        String result = message;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }
        return result;
    }

    /**
     * Centra un mensaje en el chat
     * @param message El mensaje a centrar
     * @return El mensaje centrado
     */
    public static String centerMessage(String message) {
        if (message == null) return "";

        int centerPixel = 154; // Ancho promedio del chat
        int messagePixel = getStringWidth(ChatColor.stripColor(message));
        int spaces = (centerPixel - messagePixel) / 4; // Ancho promedio de un espacio

        StringBuilder centered = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            centered.append(" ");
        }
        centered.append(message);

        return centered.toString();
    }

    /**
     * Calcula el ancho de una cadena en píxeles
     * @param text El texto
     * @return El ancho en píxeles
     */
    private static int getStringWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            switch (c) {
                case 'i': case 'l': case '|': case ':': case ';': case '\'': case '!':
                    width += 2; break;
                case 'I': case '[': case ']': case 't':
                    width += 3; break;
                case 'f': case 'k':
                    width += 4; break;
                case ' ':
                    width += 3; break;
                default:
                    width += 5; break;
            }
        }
        return width;
    }

    /**
     * Crea una línea decorativa
     * @param character El carácter a usar
     * @param length La longitud de la línea
     * @param color El color de la línea
     * @return La línea decorativa
     */
    public static String createLine(char character, int length, String color) {
        StringBuilder line = new StringBuilder(color);
        for (int i = 0; i < length; i++) {
            line.append(character);
        }
        return line.toString();
    }

    /**
     * Crea una línea decorativa por defecto
     * @return La línea decorativa
     */
    public static String createDefaultLine() {
        return createLine('-', 50, "&7&m");
    }
}
