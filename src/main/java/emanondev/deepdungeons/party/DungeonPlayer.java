package emanondev.deepdungeons.party;

import emanondev.core.PlayerSnapshot;
import emanondev.deepdungeons.door.DoorType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class DungeonPlayer {
    private PlayerSnapshot preEnterSnapshot = null;
    private PlayerSnapshot logoutSnapshot = null;
    private final HashMap<DoorType.DoorInstance.DoorHandler, DoorType.DoorInstance.DoorHandler> history = new HashMap<>();

    public void setPreEnterSnapshot(@NotNull Player player) {
        setPreEnterSnapshot(new PlayerSnapshot(player, PlayerSnapshot.FieldType.LOCATION));
    }

    public void setPreEnterSnapshot(@Nullable PlayerSnapshot snapshot) {
        preEnterSnapshot = snapshot == null ? null : snapshot.clone();
    }

    public @Nullable PlayerSnapshot getPreEnterSnapshot() {
        return preEnterSnapshot;
    }
    public @Nullable PlayerSnapshot getAndDeletePreEnterSnapshot() {
        PlayerSnapshot value = preEnterSnapshot;
        preEnterSnapshot = null;
        return value;
    }

    public void setLogoutSnapshot(@NotNull Player player) {
        setLogoutSnapshot(new PlayerSnapshot(player, PlayerSnapshot.FieldType.LOCATION));
    }

    public void setLogoutSnapshot(@Nullable PlayerSnapshot snapshot) {
        logoutSnapshot = snapshot == null ? null : snapshot.clone();
    }

    public @Nullable PlayerSnapshot getLogoutSnapshot() {
        return logoutSnapshot;
    }
    public @Nullable PlayerSnapshot getAndDeleteLogoutSnapshot() {
        PlayerSnapshot value = logoutSnapshot;
        logoutSnapshot = null;
        return value;
    }

    public boolean hasPreEnterSnapshot() {
        return preEnterSnapshot != null;
    }

    public boolean hasLogoutSnapshot() {
        return logoutSnapshot != null;
    }

    public void clearDungeonData() {
        preEnterSnapshot = null;
        logoutSnapshot = null;
        history.clear();
    }

    public void addDoorHistory(@NotNull DoorType.DoorInstance.DoorHandler from, @NotNull DoorType.DoorInstance.DoorHandler to) {
        history.put(to,from);
    }

    public @Nullable DoorType.DoorInstance.DoorHandler getBackRoute(@NotNull DoorType.DoorInstance.DoorHandler from){
        return history.get(from);
    }
}
