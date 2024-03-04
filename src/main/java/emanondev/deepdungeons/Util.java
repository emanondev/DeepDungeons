package emanondev.deepdungeons;

import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class Util {

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

}
