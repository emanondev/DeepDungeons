package emanondev.deepdungeons.roomold;

import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.roomold.type.RoomType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class RoomBuilderManager {

    private static final DeepDungeons PLUGIN = DeepDungeons.get();
    @SuppressWarnings("serial")
    private static final HashMap<UUID, RoomType.RoomBuilder> rooms = new HashMap<UUID, RoomType.RoomBuilder>() {
        public void clear() {
            this.values().forEach((val) -> PLUGIN.unregisterListener(val));
            super.clear();
        }

        public RoomType.RoomBuilder put(UUID uid, RoomType.RoomBuilder rbuilder) {
            super.put(uid, rbuilder);
            PLUGIN.registerListener(rbuilder);
            return rbuilder;
        }

        public RoomType.RoomBuilder remove(Object uid) {
            RoomType.RoomBuilder val = super.remove(uid);
            if (val != null)
                PLUGIN.unregisterListener(val);
            return val;
        }
    };
    private static final int DOOR_SIZE = 5;

    public static boolean isBuilding(@NotNull UUID uid) {
        return rooms.containsKey(uid);
    }

    public static boolean isBuilding(@NotNull Player p) {
        return isBuilding(p.getUniqueId());
    }

    public static void removeBuilder(@NotNull Player p) {
        removeBuilder(p.getUniqueId());
    }

    public static void removeBuilder(@NotNull UUID uid) {
        rooms.remove(uid);
    }

    public static void addBuilder(@NotNull Player p, @NotNull RoomType.RoomBuilder builder) {
        addBuilder(p.getUniqueId(), builder);
    }

    public static void addBuilder(@NotNull UUID uid, @NotNull RoomType.RoomBuilder builder) {
        rooms.put(uid, builder);
    }
}
