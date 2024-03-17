package emanondev.deepdungeons.interfaces;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public interface AreaHolder {

    /*@Contract(pure = true, value = "-> new")
    BoundingBox getArea();*/

    @NotNull
    World getWorld();

    boolean contains(@NotNull Vector vector);

    boolean overlaps(@NotNull BoundingBox box);

    default boolean contains(@NotNull Block block) {
        return contains(block.getLocation());
    }

    default boolean contains(@NotNull BlockState block) {
        return contains(block.getLocation());
    }

    default boolean contains(@NotNull Location loc) {
        return getWorld().equals(loc.getWorld()) && contains(loc.toVector());
    }

    default boolean overlaps(@NotNull Entity entity) {
        return overlaps(entity.getBoundingBox());
    }

}
