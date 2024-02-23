package emanondev.deepdungeons.room;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import emanondev.core.YMLSection;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DRInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorInstance;
import emanondev.deepdungeons.spawner.MonsterSpawnerInstance;
import emanondev.deepdungeons.treasure.TreasureInstance;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RoomInstance extends DRInstance<RoomType> {

    private final YMLSection section;

    public RoomInstance(@NotNull String id, @NotNull RoomType type, YMLSection section) {
        super(id,type);
        this.section = section;
    }

    @NotNull DoorInstance getEntrance(){
        return this.entrace;
    }

    @NotNull Collection<DoorInstance> getExits(){
        return this.exits;
    }
    @NotNull Collection<MonsterSpawnerInstance> getMonsterSpawners(){
        return this.monsterSpawners;
    }
    @NotNull Collection<TreasureInstance> getTreasures(){
        return this.treasures;
    }
    public @NotNull File getSchematic(){
        return new File(DeepDungeons.get().getDataFolder(),"schematics"+File.separator+section.getString("schematic"));
    }
    public @NotNull CompletableFuture<Clipboard> getClipboard(boolean async){
        return WorldEditUtility.load(getSchematic(),DeepDungeons.get(),async);
    }
    public CompletableFuture paste(@NotNull RoomHandler handler) throws ExecutionException, InterruptedException {
        return paste(handler.getLocation());
    }
    public CompletableFuture paste(@NotNull Location location) throws ExecutionException, InterruptedException {
        return WorldEditUtility.paste(location,getClipboard(true).get(), true, DeepDungeons.get(), false,true,true,false);
    }
}
