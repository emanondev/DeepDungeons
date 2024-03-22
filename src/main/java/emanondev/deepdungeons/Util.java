package emanondev.deepdungeons;

import org.bukkit.Location;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

/**
 * Container for generic utility static methods
 */
public class Util {

    private Util() {
        throw new AssertionError();
    }

    @NotNull
    public static String toString(@NotNull BlockVector vector) {
        return vector.getBlockX() + ";" + vector.getBlockY() + ";" + vector.getBlockZ();
    }

    @NotNull
    public static BlockVector toBlockVector(@NotNull String vector) {
        String[] split = vector.replace(",", ".").split(";");
        return new BlockVector(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    @NotNull
    public static String toString(@NotNull Vector vector) {
        DecimalFormat df = new DecimalFormat("###.###");
        return df.format(vector.getX()) + ";" + df.format(vector.getY()) + ";" + df.format(vector.getZ());
    }

    @NotNull
    public static Vector toVector(@NotNull String vector) {
        String[] split = vector.replace(",", ".").split(";");
        return new Vector(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }

    /**
     * @param vector
     * @return
     */
    @NotNull
    public static String toStringNoWorld(@NotNull Location vector) {
        DecimalFormat df = new DecimalFormat("###.###");
        return df.format(vector.getX()) + ";" + df.format(vector.getY()) + ";" + df.format(vector.getZ()) + ";" + df.format(vector.getYaw()) + ";" + df.format(vector.getPitch());
    }

    /**
     * Returned location has null World
     *
     * @param text text to parse
     * @return location obtained parsing input text
     */
    @NotNull
    public static Location toLocationNoWorld(@NotNull String text) {
        String[] split = text.replace(",", ".").split(";");
        return split.length == 3 ? new Location(null, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2])) :
                new Location(null, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Float.parseFloat(split[2]), Float.parseFloat(split[2]));
    }
}
