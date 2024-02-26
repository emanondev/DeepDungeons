package emanondev.deepdungeons.room;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DRInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.door.DoorTypeManager;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import emanondev.deepdungeons.spawner.MonsterSpawnerTypeManager;
import emanondev.deepdungeons.treasure.TreasureType;
import emanondev.deepdungeons.treasure.TreasureTypeManager;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

public abstract class RoomType extends DRegistryElement {

    public RoomType(@NotNull String id) {
        super(id);
    }

    public final @NotNull RoomInstance read(@NotNull String id, @NotNull YMLSection section) {
        RoomInstance room = readImpl(id, section);
        RoomInstanceManager.getInstance().register(room);
        return room;
    }

    public abstract RoomInstanceBuilder getBuilder();

    protected abstract @NotNull RoomInstance readImpl(@NotNull String id, @NotNull YMLSection section);

    public abstract class RoomInstanceBuilder extends DInstance<RoomType> {

        protected RoomInstanceBuilder() {
            super(RoomType.this);
        }

        public final void writeTo(@NotNull YMLSection section) {
            section.set("type", getType().getId());
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);
    }

    public class RoomInstance extends DRInstance<RoomType> {

        private final YMLSection section;
        private final DoorType.DoorInstance entrace;
        private final LinkedHashMap<String, DoorType.DoorInstance> exits = new LinkedHashMap<>();
        private final LinkedHashMap<String, TreasureType.TreasureInstance> treasures = new LinkedHashMap<>();
        private final LinkedHashMap<String, MonsterSpawnerType.MonsterSpawnerInstance> monsterSpawners = new LinkedHashMap<>();
        private final String schematicName;
        private SoftReference<Clipboard> clipboard = null;
        private CompletableFuture<Clipboard> futureClipboard;

        public RoomInstance(@NotNull String id, YMLSection section) {
            super(id, RoomType.this);
            this.section = section;
            this.entrace = DoorTypeManager.getInstance().get(this.section.loadSection("entrace").getString("type"))
                    .read(this, this.section.loadSection("entrace"));
            @NotNull YMLSection tmp = this.section.loadSection("exits");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                exits.put(key, DoorTypeManager.getInstance().get(sub.getString("type"))
                        .read(this, sub));
            }
            tmp = this.section.loadSection("treasures");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                treasures.put(key, TreasureTypeManager.getInstance().get(sub.getString("type"))
                        .read(this, sub));
            }
            tmp = this.section.loadSection("monsterspawners");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                monsterSpawners.put(key, MonsterSpawnerTypeManager.getInstance().get(sub.getString("type"))
                        .read(this, sub));
            }
            this.schematicName = section.getString("schematic");
        }

        public @NotNull DoorType.DoorInstance getEntrance() {
            return this.entrace;
        }

        public @NotNull Collection<DoorType.DoorInstance> getExits() {
            return this.exits.values();
        }

        public @NotNull Collection<MonsterSpawnerType.MonsterSpawnerInstance> getMonsterSpawners() {
            return this.monsterSpawners.values();
        }

        public @NotNull Collection<TreasureType.TreasureInstance> getTreasures() {
            return this.treasures.values();
        }

        public @NotNull File getSchematic() {
            return new File(DeepDungeons.get().getDataFolder(), "schematics" + File.separator + getSchematicName());
        }

        private @NotNull String getSchematicName() {
            return this.schematicName;
        }

        public @NotNull CompletableFuture<Clipboard> getClipboard(boolean async) {
            Clipboard clip = clipboard == null ? null : clipboard.get();
            if (clip != null)
                return CompletableFuture.completedFuture(clip);
            if (futureClipboard != null)
                return futureClipboard;
            CompletableFuture<Clipboard> result = WorldEditUtility.load(getSchematic(), DeepDungeons.get(), async);
            result.thenAccept(value -> this.clipboard = new SoftReference<>(value));
            this.futureClipboard = result;
            result.whenComplete((value, e) -> this.futureClipboard = null);
            return result;
        }

        public @NotNull CompletableFuture<EditSession> paste(@NotNull RoomHandler handler, boolean async) {
            return paste(handler.getLocation(), async);
        }

        public @NotNull CompletableFuture<EditSession> paste(@NotNull Location location, boolean async) {
            return getClipboard(async).thenCompose(value -> WorldEditUtility.paste(location, value, async,
                    DeepDungeons.get(), false, true, true, false));
        }
    }
}
