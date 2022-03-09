package emanondev.deepdungeons.room;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DeepDungeons;
import org.bukkit.Location;

import java.io.File;

public class Room {

    private final static File DIR = new File(DeepDungeons.get().getDataFolder(), "rooms");

    private final String id;
    private final RoomData data;

    Room(String id, RoomData data) {
        if (id == null || data == null)
            throw new NullPointerException();
        this.id = id;
        this.data = data;
    }

    public final String getId() {
        return id;
    }

    public boolean exist() {
        return getFile().exists();
    }

    public File getFile() {
        return new File(DIR, id + ".schematic");
    }

    public boolean delete() {
        return getFile().delete();
    }

    public boolean save(Clipboard clip) {
        getFile().getParentFile().mkdirs();//TODO remove added to api
        return WorldEditUtility.save(getFile(), clip);
    }

    public Clipboard load() {
        return WorldEditUtility.load(getFile());
    }

    public RoomData getData() {
        return data;
    }

    public boolean paste(Location dest, int rotation) {
        if (DeepDungeons.DEBUG)
            DeepDungeons.get().log(this.getClass().getSimpleName() + " -> rotation " + rotation);
        return WorldEditUtility.paste(dest, load(), rotation);
    }

}
