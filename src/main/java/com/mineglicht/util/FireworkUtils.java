package com.mineglicht.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FireworkUtils {

    private static final Random random = new Random();

    // Colores predefinidos para diferentes tipos de eventos
    private static final List<Color> SIEGE_COLORS = Arrays.asList(
            Color.RED, Color.ORANGE, Color.YELLOW
    );

    private static final List<Color> VICTORY_COLORS = Arrays.asList(
            Color.GREEN, Color.LIME, Color.YELLOW
    );

    private static final List<Color> DEFEAT_COLORS = Arrays.asList(
            Color.RED, Color.MAROON, Color.BLACK
    );

    private static final List<Color> CELEBRATION_COLORS = Arrays.asList(
            Color.BLUE, Color.PURPLE, Color.FUCHSIA, Color.AQUA
    );

    /**
     * Crea fuegos artificiales para el inicio de asedio
     */
    public static void createSiegeStartFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        Firework firework = location.getWorld().spawn(location.add(0, 1, 0), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // Efecto principal - explosión grande roja/naranja
        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(SIEGE_COLORS)
                .withFade(Color.RED)
                .withFlicker()
                .withTrail()
                .build();

        meta.addEffect(effect);
        meta.setPower(2);
        firework.setFireworkMeta(meta);

        // Crear fuegos artificiales adicionales alrededor
        createMultipleFireworks(location, 3, SIEGE_COLORS, FireworkEffect.Type.BURST);
    }

    /**
     * Crea fuegos artificiales para cuando se ataca al protector
     */
    public static void createProtectorAttackedFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        // Fuegos artificiales rojos intensos
        createCustomFirework(location, Arrays.asList(Color.RED, Color.MAROON),
                FireworkEffect.Type.STAR, 1, true, false);
    }

    /**
     * Crea fuegos artificiales para victoria en asedio
     */
    public static void createVictoryFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        // Múltiples fuegos artificiales de celebración
        for (int i = 0; i < 5; i++) {
            Location randomLoc = location.clone().add(
                    random.nextDouble() * 10 - 5,
                    random.nextDouble() * 5,
                    random.nextDouble() * 10 - 5
            );

            createCustomFirework(randomLoc, VICTORY_COLORS,
                    getRandomFireworkType(), random.nextInt(2) + 1, true, true);
        }
    }

    /**
     * Crea fuegos artificiales para derrota en asedio
     */
    public static void createDefeatFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        createCustomFirework(location, DEFEAT_COLORS,
                FireworkEffect.Type.CREEPER, 1, false, false);
    }

    /**
     * Crea fuegos artificiales de celebración general
     */
    public static void createCelebrationFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        for (int i = 0; i < 3; i++) {
            Location offsetLoc = location.clone().add(
                    random.nextDouble() * 6 - 3,
                    random.nextDouble() * 3,
                    random.nextDouble() * 6 - 3
            );

            createCustomFirework(offsetLoc, CELEBRATION_COLORS,
                    getRandomFireworkType(), random.nextInt(3) + 1, true, true);
        }
    }

    /**
     * Crea fuegos artificiales periódicos durante el asedio
     */
    public static void createSiegePeriodicFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        // Fuegos artificiales más simples para uso periódico
        createCustomFirework(location,
                Arrays.asList(SIEGE_COLORS.get(random.nextInt(SIEGE_COLORS.size()))),
                FireworkEffect.Type.BALL, 1, false, true);
    }

    /**
     * Crea un fuego artificial personalizado
     */
    public static void createCustomFirework(Location location, List<Color> colors,
                                            FireworkEffect.Type type, int power,
                                            boolean flicker, boolean trail) {
        if (location == null || location.getWorld() == null) return;

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                .with(type)
                .withColor(colors);

        // Agregar color de desvanecimiento aleatorio
        if (colors.size() > 1) {
            effectBuilder.withFade(colors.get(random.nextInt(colors.size())));
        }

        if (flicker) effectBuilder.withFlicker();
        if (trail) effectBuilder.withTrail();

        meta.addEffect(effectBuilder.build());
        meta.setPower(Math.max(1, Math.min(3, power)));
        firework.setFireworkMeta(meta);
    }

    /**
     * Crea múltiples fuegos artificiales en ubicaciones aleatorias alrededor de un punto
     */
    public static void createMultipleFireworks(Location center, int count,
                                               List<Color> colors, FireworkEffect.Type type) {
        if (center == null || center.getWorld() == null) return;

        for (int i = 0; i < count; i++) {
            // Crear ubicación aleatoria en un radio de 5 bloques
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = random.nextDouble() * 5;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + random.nextDouble() * 3;

            Location fireworkLoc = new Location(center.getWorld(), x, y, z);

            // Delay aleatorio para crear efecto escalonado
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("CityWars"),
                    () -> createCustomFirework(fireworkLoc, colors, type, 1, true, false),
                    random.nextInt(20) // 0-1 segundo de delay
            );
        }
    }

    /**
     * Crea una secuencia de fuegos artificiales (útil para eventos importantes)
     */
    public static void createFireworkSequence(Location location, int duration) {
        if (location == null || location.getWorld() == null) return;

        // Crear fuegos artificiales cada 2 segundos durante la duración especificada
        for (int i = 0; i < duration; i += 2) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("CityWars"),
                    () -> {
                        Location randomLoc = location.clone().add(
                                random.nextDouble() * 8 - 4,
                                random.nextDouble() * 4,
                                random.nextDouble() * 8 - 4
                        );

                        List<Color> randomColors = getRandomColors(2);
                        createCustomFirework(randomLoc, randomColors,
                                getRandomFireworkType(), random.nextInt(2) + 1,
                                random.nextBoolean(), random.nextBoolean());
                    },
                    i * 20L // Convertir segundos a ticks
            );
        }
    }

    /**
     * Crea un espectáculo de fuegos artificiales para eventos muy especiales
     */
    public static void createFireworkSpectacle(Location center, int intensity) {
        if (center == null || center.getWorld() == null) return;

        intensity = Math.max(1, Math.min(10, intensity));

        for (int wave = 0; wave < intensity; wave++) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("CityWars"),
                    () -> {
                        // Crear múltiples fuegos artificiales simultáneos
                        for (int i = 0; i < 5; i++) {
                            double angle = (2 * Math.PI * i) / 5;
                            double radius = 3 + random.nextDouble() * 7;

                            Location fireworkLoc = center.clone().add(
                                    Math.cos(angle) * radius,
                                    random.nextDouble() * 5 + 2,
                                    Math.sin(angle) * radius
                            );

                            createCustomFirework(fireworkLoc, getRandomColors(3),
                                    getRandomFireworkType(), random.nextInt(3) + 1,
                                    true, true);
                        }
                    },
                    wave * 10L // Medio segundo entre ondas
            );
        }
    }

    /**
     * Detona un fuego artificial inmediatamente (útil para testing)
     */
    public static void detonateFirework(Firework firework) {
        if (firework != null && firework.isValid()) {
            firework.detonate();
        }
    }

    /**
     * Obtiene un tipo de fuego artificial aleatorio
     */
    private static FireworkEffect.Type getRandomFireworkType() {
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return types[random.nextInt(types.length)];
    }

    /**
     * Obtiene una lista de colores aleatorios
     */
    private static List<Color> getRandomColors(int count) {
        List<Color> allColors = Arrays.asList(
                Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.ORANGE, Color.PURPLE, Color.LIME,
                Color.AQUA, Color.FUCHSIA, Color.MAROON, Color.NAVY
        );

        List<Color> selectedColors = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            selectedColors.add(allColors.get(random.nextInt(allColors.size())));
        }

        return selectedColors;
    }

    /**
     * Crea un fuego artificial silencioso (sin sonido de explosión)
     */
    public static void createSilentFirework(Location location, List<Color> colors) {
        if (location == null || location.getWorld() == null) return;

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(colors)
                .build();

        meta.addEffect(effect);
        meta.setPower(0); // Power 0 para minimizar sonido
        firework.setFireworkMeta(meta);

        // Detonar inmediatamente para evitar el sonido de lanzamiento
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("CityWars"),
                () -> firework.detonate(),
                1L
        );
    }
}
