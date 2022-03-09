package emanondev.deepdungeons;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class Offset implements ConfigurationSerializable {

    private final int x;
    private final int y;
    private final int z;
    private final BlockFace direction;

    public Offset(BlockVector offset, BlockFace direction) {
        this(offset.getBlockX(), offset.getBlockY(), offset.getBlockZ(), direction);
    }

    private Offset(int x, int y, int z, BlockFace direction) {
        if (direction == null)
            throw new NullPointerException();
        switch (direction) {
            case NORTH:
            case EAST:
            case WEST:
            case SOUTH:
                break;
            default:
                throw new IllegalArgumentException("invalid direction " + direction.name());
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.direction = direction;
    }

    public BlockVector getOffset() {
        return new BlockVector(x, y, z);
    }

    public BlockFace getDirection() {
        return direction;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + direction.ordinal();
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Offset other = (Offset) obj;
        if (direction != other.direction)
            return false;
        if (x != other.x)
            return false;
        if (z != other.z)
            return false;
        return y == other.y;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("direction", direction.name());
        return map;
    }

    public static Offset deserialize(Map<String, Object> map) {
        return new Offset((int) map.get("x"), (int) map.get("y"), (int) map.get("z"),
                BlockFace.valueOf((String) map.get("direction")));
    }

}
