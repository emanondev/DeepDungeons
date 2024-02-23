package emanondev.deepdungeons.roomold;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.Offset;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RoomData {

    protected YMLSection section;
    private Offset entrace;
    private final Set<Offset> exits;
    @SuppressWarnings("unused")
    private final Set<Material> breakableBlockTypes = new HashSet<>();

    @SuppressWarnings("unchecked")
    public RoomData(YMLSection section) {
        this.section = section;
        this.entrace = section.get(ENTRACE_PATH, null, Offset.class);
        this.exits = new HashSet<>(
                (List<Offset>) section.getList(EXITS_PATH, new ArrayList<>()));
    }

    private static final String ENTRACE_PATH = "entrace";
    private static final String EXITS_PATH = "exists";

    public Offset getEntrace() {
        return entrace;
    }

    @NotNull
    public Collection<Offset> getExits() {
        return exits;
    }

    @NotNull
    public Collection<Material> getBreakableBlockTypes() {
        //TODO
        return new ArrayList<>();
    }

    @NotNull
    public Collection<BlockVector> getBreakableLocationsOffsets() {
        //TODO
        return new ArrayList<>();
    }

    public void setEntrace(BlockVector vector, BlockFace direction) {
        setEntrace(new Offset(vector, direction));
    }

    public void setEntrace(Offset door) {
        checkEntrace(door);
        this.entrace = door;
    }

    public void checkEntrace(BlockVector vector, BlockFace direction) {
        checkEntrace(new Offset(vector, direction));
    }

    public void checkEntrace(Offset door) {
        if (door == null)
            throw new NullPointerException();
        //TODO check if door is on room border
    }

    public void checkExit(BlockVector vector, BlockFace direction) {
        checkExit(new Offset(vector, direction));
    }

    public void checkExit(Offset door) {
        if (door == null)
            throw new NullPointerException();
        //TODO check if door is on room border
    }

    public void addExit(BlockVector vector, BlockFace direction) {
        addExit(new Offset(vector, direction));
    }

    public void addExit(Offset door) {
        checkExit(door);
        exits.add(door);
    }

    public void removeExit(Offset exit) {
        exits.remove(exit);
    }

    public void clearExits() {
        exits.clear();
    }

    public void addBreakableBlockType(@NotNull Material m) {
        if (m == null)
            throw new NullPointerException();
        if (!m.isBlock())
            throw new IllegalArgumentException();
        //TODO
    }

    public void removeBreakableBlockType(@NotNull Material m) {
        if (m == null)
            throw new NullPointerException();
        if (!m.isBlock())
            throw new IllegalArgumentException();
        //TODO

    }

    public void toggleBreakableBlockType(@NotNull Material m) {
        if (m == null)
            throw new NullPointerException();
        if (!m.isBlock())
            throw new IllegalArgumentException();
        if (getBreakableBlockTypes().contains(m))
            removeBreakableBlockType(m);
        else
            addBreakableBlockType(m);
    }

    public void removeBreakableLocationOffset(@NotNull BlockVector v) {
        if (v == null)
            throw new NullPointerException();
        //TODO

    }

    public void addBreakableLocationOffset(@NotNull BlockVector v) {
        if (v == null)
            throw new NullPointerException();
        //TODO

    }

    public void toggleBreakableLocationOffset(@NotNull BlockVector v) {
        if (v == null)
            throw new NullPointerException();
        //TODO
    }

    public boolean isBreakableLocationOffset(@NotNull BlockVector v) {
        if (v == null)
            throw new NullPointerException();
        //TODO
        return false;
    }

    public void clearBreakableLocationsOffset() {
        //TODO
    }

}
