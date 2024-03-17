package emanondev.deepdungeons.interfaces;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Class for an object that holds an area in a World
 */
public interface AreaHolder {

    /*@Contract(pure = true, value = "-> new")
    BoundingBox getArea();*/

    /**
     * @return world where this is located
     */
    @NotNull
    World getWorld();

    /**
     * @param vector where
     * @return true when this contains vector coordinates
     */
    boolean contains(@NotNull Vector vector);

    /**
     * @param box where
     * @return true when this overlaps box
     */
    boolean overlaps(@NotNull BoundingBox box);

    /**
     * @param block where
     * @return true when this contains block coordinates
     */
    default boolean contains(@NotNull Block block) {
        return contains(block.getLocation());
    }

    /**
     * @param block where
     * @return true when this contains block coordinates
     */
    default boolean contains(@NotNull BlockState block) {
        return contains(block.getLocation());
    }

    /**
     * @param loc where
     * @return true when this contains loc coordinates
     */
    default boolean contains(@NotNull Location loc) {
        return getWorld().equals(loc.getWorld()) && contains(loc.toVector());
    }

    /**
     * @param entity where
     * @return true when this overlaps entity
     */
    default boolean overlaps(@NotNull Entity entity) {
        return getWorld().equals(entity.getWorld()) && overlaps(entity.getBoundingBox());
    }

}
