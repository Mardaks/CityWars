package com.mineglicht.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class LocationUtils {

    /**
     * Serializa una Location a un Map para guardar en configuración
     */
    public static Map<String, Object> serializeLocation(Location location) {
        Map<String, Object> map = new HashMap<>();
        if (location == null) return map;

        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());

        return map;
    }

    /**
     * Deserializa una Location desde un Map o ConfigurationSection
     */
    public static Location deserializeLocation(Object obj) {
    if (obj == null) return null;

    Map<String, Object> map;
    if (obj instanceof ConfigurationSection) {
        ConfigurationSection section = (ConfigurationSection) obj;
        map = section.getValues(false);
    } else if (obj instanceof Map<?, ?>) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uncheckedMap = (Map<String, Object>) obj;
            map = uncheckedMap;
        } catch (ClassCastException e) {
            return null;
        }
    } else {
        return null;
    }

    try {
        String worldName = (String) map.get("world");
        double x = getDouble(map.get("x"));
        double y = getDouble(map.get("y"));
        double z = getDouble(map.get("z"));
        float yaw = getFloat(map.get("yaw"));
        float pitch = getFloat(map.get("pitch"));

        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(world, x, y, z, yaw, pitch);
    } catch (Exception e) {
        return null;
    }
}

    /**
     * Calcula la distancia 2D entre dos ubicaciones (ignorando Y)
     */
    public static double distance2D(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return Double.MAX_VALUE;
        if (!loc1.getWorld().equals(loc2.getWorld())) return Double.MAX_VALUE;

        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();

        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calcula la distancia 3D entre dos ubicaciones
     */
    public static double distance3D(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return Double.MAX_VALUE;
        if (!loc1.getWorld().equals(loc2.getWorld())) return Double.MAX_VALUE;

        return loc1.distance(loc2);
    }

    /**
     * Verifica si una ubicación está dentro de un área rectangular
     */
    public static boolean isWithinArea(Location location, Location corner1, Location corner2) {
        if (location == null || corner1 == null || corner2 == null) return false;
        if (!location.getWorld().equals(corner1.getWorld()) ||
                !location.getWorld().equals(corner2.getWorld())) return false;

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return location.getX() >= minX && location.getX() <= maxX &&
                location.getY() >= minY && location.getY() <= maxY &&
                location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    /**
     * Verifica si una ubicación está dentro de un radio circular
     */
    public static boolean isWithinRadius(Location center, Location location, double radius) {
        if (center == null || location == null) return false;
        return distance2D(center, location) <= radius;
    }

    /**
     * Obtiene el centro de un bloque (coordenadas .5)
     */
    public static Location getBlockCenter(Location location) {
        if (location == null) return null;

        Location center = location.clone();
        center.setX(center.getBlockX() + 0.5);
        center.setY(center.getBlockY() + 0.5);
        center.setZ(center.getBlockZ() + 0.5);

        return center;
    }

    /**
     * Obtiene una ubicación segura cerca de la ubicación dada
     */
    public static Location getSafeLocation(Location location) {
        if (location == null) return null;

        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        // Buscar desde Y=255 hacia abajo
        for (int y = 255; y > 0; y--) {
            Location checkLoc = new Location(world, x, y, z);

            // Verificar que el bloque actual y el de arriba sean aire
            if (world.getBlockAt(checkLoc).getType().isAir() &&
                    world.getBlockAt(checkLoc.add(0, 1, 0)).getType().isAir()) {

                // Verificar que el bloque de abajo sea sólido
                Location groundLoc = checkLoc.subtract(0, 1, 0);
                if (world.getBlockAt(groundLoc).getType().isSolid()) {
                    return new Location(world, x + 0.5, groundLoc.getY() + 1, z + 0.5);
                }
            }
        }

        // Si no encontramos ubicación segura, devolver la original
        return getBlockCenter(location);
    }

    /**
     * Convierte una ubicación a string legible
     */
    public static String locationToString(Location location) {
        if (location == null) return "null";

        return String.format("%s: %.1f, %.1f, %.1f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    /**
     * Convierte un string a Location (formato: mundo,x,y,z,yaw,pitch)
     */
    public static Location stringToLocation(String str) {
        if (str == null || str.isEmpty()) return null;

        String[] parts = str.split(",");
        if (parts.length < 4) return null;

        try {
            World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return null;

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtiene todas las ubicaciones en un área rectangular
     */
    public static java.util.List<Location> getLocationsInArea(Location corner1, Location corner2) {
        java.util.List<Location> locations = new java.util.ArrayList<>();

        if (corner1 == null || corner2 == null) return locations;
        if (!corner1.getWorld().equals(corner2.getWorld())) return locations;

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        World world = corner1.getWorld();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    locations.add(new Location(world, x, y, z));
                }
            }
        }

        return locations;
    }

    /**
     * Obtiene la dirección cardinal más cercana basada en el yaw
     */
    public static String getCardinalDirection(float yaw) {
        // Normalizar yaw a 0-360
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) return "Sur";
        else if (yaw >= 45 && yaw < 135) return "Oeste";
        else if (yaw >= 135 && yaw < 225) return "Norte";
        else return "Este";
    }

    /**
     * Calcula el punto medio entre dos ubicaciones
     */
    public static Location getMidpoint(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return null;
        if (!loc1.getWorld().equals(loc2.getWorld())) return null;

        double x = (loc1.getX() + loc2.getX()) / 2;
        double y = (loc1.getY() + loc2.getY()) / 2;
        double z = (loc1.getZ() + loc2.getZ()) / 2;

        return new Location(loc1.getWorld(), x, y, z);
    }

    /**
     * Expande un área por un número de bloques en todas las direcciones
     */
    public static Location[] expandArea(Location corner1, Location corner2, int expansion) {
        if (corner1 == null || corner2 == null) return new Location[]{corner1, corner2};

        double minX = Math.min(corner1.getX(), corner2.getX()) - expansion;
        double maxX = Math.max(corner1.getX(), corner2.getX()) + expansion;
        double minY = Math.min(corner1.getY(), corner2.getY()) - expansion;
        double maxY = Math.max(corner1.getY(), corner2.getY()) + expansion;
        double minZ = Math.min(corner1.getZ(), corner2.getZ()) - expansion;
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + expansion;

        Location newCorner1 = new Location(corner1.getWorld(), minX, minY, minZ);
        Location newCorner2 = new Location(corner1.getWorld(), maxX, maxY, maxZ);

        return new Location[]{newCorner1, newCorner2};
    }

    // Métodos auxiliares para conversión de tipos
    private static double getDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return Double.parseDouble(obj.toString());
    }

    private static float getFloat(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        return Float.parseFloat(obj.toString());
    }


    /**
 * Convierte un Vector a string (formato: x,y,z)
 */
public static String vectorToString(Vector vector) {
    if (vector == null) return "null";
    
    return String.format("%.1f,%.1f,%.1f", 
            vector.getX(), 
            vector.getY(), 
            vector.getZ());
}

/**
 * Convierte un string a Vector (formato: x,y,z)
 */
public static Vector stringToVector(String str) {
    if (str == null || str.isEmpty() || str.equals("null")) return null;
    
    String[] parts = str.split(",");
    if (parts.length < 3) return null;
    
    try {
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        
        return new Vector(x, y, z);
    } catch (NumberFormatException e) {
        return null;
    }
}
}
