package emanondev.deepdungeons.room;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import emanondev.core.YMLConfig;
import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DRInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.door.DoorTypeManager;
import emanondev.deepdungeons.dungeon.DungeonHandler;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import emanondev.deepdungeons.spawner.MonsterSpawnerTypeManager;
import emanondev.deepdungeons.trap.TrapType;
import emanondev.deepdungeons.trap.TrapTypeManager;
import emanondev.deepdungeons.treasure.TreasureType;
import emanondev.deepdungeons.treasure.TreasureTypeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.*;
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

    public abstract @NotNull RoomInstanceBuilder getBuilder(@NotNull String id);

    protected abstract @NotNull RoomInstance readImpl(@NotNull String id, @NotNull YMLSection section);

    public abstract class RoomInstanceBuilder extends DRInstance<RoomType> {

        private final List<DoorType.DoorInstanceBuilder> exits = new ArrayList<>();
        private final List<TreasureType.TreasureInstanceBuilder> treasures = new ArrayList<>();
        private final List<TrapType.TrapInstanceBuilder> traps = new ArrayList<>();
        private final List<MonsterSpawnerType.MonsterSpawnerInstanceBuilder> monsterSpawners = new ArrayList<>();
        private final LinkedHashSet<Material> breakableBlocks = new LinkedHashSet<>();
        private DoorType.DoorInstanceBuilder entrance;
        private String schematicName;
        private Clipboard clipboard;
        private BlockVector size;
        private BlockVector offset;

        public @NotNull UUID getPlayerUUID() {
            return playerUuid;
        }
        public @Nullable Player getPlayer() {
            return Bukkit.getPlayer(playerUuid);
        }

        private final UUID playerUuid;

        public BlockVector getOffset() {
            return offset;
        }

        public void setOffset(BlockVector offset) {
            this.offset = offset;
        }


        protected RoomInstanceBuilder(@NotNull String id,@NotNull Player player) {
            super(id, RoomType.this);
            this.playerUuid = player.getUniqueId();
        }

        public DoorType.DoorInstanceBuilder getEntrance() {
            return entrance;
        }

        public void setEntrance(DoorType.DoorInstanceBuilder entrance) {
            this.entrance = entrance;
        }

        public List<DoorType.DoorInstanceBuilder> getExits() {
            return exits;
        }

        public List<TreasureType.TreasureInstanceBuilder> getTreasures() {
            return treasures;
        }

        public List<TrapType.TrapInstanceBuilder> getTraps() {
            return traps;
        }

        public List<MonsterSpawnerType.MonsterSpawnerInstanceBuilder> getMonsterSpawners() {
            return monsterSpawners;
        }

        public LinkedHashSet<Material> getBreakableBlocks() {
            return breakableBlocks;
        }

        public String getSchematicName() {
            return schematicName;
        }

        public void setSchematicName(String schematicName) {
            this.schematicName = schematicName;
        }

        public Clipboard getClipboard() {
            return clipboard;
        }

        public void setClipboard(Clipboard clipboard) {
            this.clipboard = clipboard;
            this.size = new BlockVector(clipboard.getDimensions().getBlockX(),
                    clipboard.getDimensions().getBlockY(), clipboard.getDimensions().getBlockZ());
        }

        public final void write() {
            YMLSection section = new YMLConfig(DeepDungeons.get(), "rooms" + File.separator + getId());
            section.set("type", getType().getId());
            YMLSection tmp = section.loadSection("entrance");
            entrance.writeTo(tmp);
            tmp = section.loadSection("exits");
            for (int i = 0; i < exits.size(); i++) {
                @NotNull YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                exits.get(i).writeTo(sub);
            }
            tmp = section.loadSection("treasures");
            for (int i = 0; i < treasures.size(); i++) {
                @NotNull YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                treasures.get(i).writeTo(sub);
            }
            tmp = section.loadSection("traps");
            for (int i = 0; i < treasures.size(); i++) {
                @NotNull YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                treasures.get(i).writeTo(sub);
            }
            tmp = section.loadSection("monsterspawners");
            for (int i = 0; i < treasures.size(); i++) {
                @NotNull YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                treasures.get(i).writeTo(sub);
            }
            section.set("schematic", schematicName);
            WorldEditUtility.save(new File(DeepDungeons.get().getDataFolder(), "schematics" + File.separator + schematicName)
                    , clipboard);
            section.setEnumsAsStringList("breakableBlocks", breakableBlocks);
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        public BlockVector getSize() {
            return size;
        }

        public abstract void handleInteract(PlayerInteractEvent event);

        public void setupTools(Player p) {
        }
    }

    public class RoomInstance extends DRInstance<RoomType> {

        private final DoorType.DoorInstance entrance;
        private final List<DoorType.DoorInstance> exits = new ArrayList<>();
        private final List<TreasureType.TreasureInstance> treasures = new ArrayList<>();
        private final List<TrapType.TrapInstance> traps = new ArrayList<>();
        private final List<MonsterSpawnerType.MonsterSpawnerInstance> monsterSpawners = new ArrayList<>();

        private final LinkedHashSet<Material> breakableBlocks = new LinkedHashSet<>();
        private final String schematicName;
        private SoftReference<Clipboard> clipboard = null;
        private CompletableFuture<Clipboard> futureClipboard;

        public RoomInstance(@NotNull String id, YMLSection section) {
            super(id, RoomType.this);
            //this.section = section;
            @NotNull YMLSection tmp = section.loadSection("entrance");
            this.entrance = DoorTypeManager.getInstance().get(tmp.getString("type")).read(this, tmp);
            tmp = section.loadSection("exits");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                exits.add(DoorTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
            }
            tmp = section.loadSection("treasures");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                treasures.add(TreasureTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
            }
            tmp = section.loadSection("traps");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                traps.add(TrapTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
            }
            tmp = section.loadSection("monsterspawners");
            for (String key : tmp.getKeys(false)) {
                @NotNull YMLSection sub = tmp.loadSection(key);
                monsterSpawners.add(MonsterSpawnerTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
            }
            this.schematicName = section.getString("schematic");
            this.breakableBlocks.addAll(section.getMaterialList("breakableBlocks", Collections.emptyList()));
        }

        public Set<Material> getBreakableBlocks() {
            return Collections.unmodifiableSet(breakableBlocks);
        }

        public @NotNull DoorType.DoorInstance getEntrance() {
            return this.entrance;
        }

        public @NotNull List<DoorType.DoorInstance> getExits() {
            return Collections.unmodifiableList(this.exits);
        }

        public @NotNull List<MonsterSpawnerType.MonsterSpawnerInstance> getMonsterSpawners() {
            return Collections.unmodifiableList(this.monsterSpawners);
        }

        public @NotNull List<TreasureType.TreasureInstance> getTreasures() {
            return Collections.unmodifiableList(this.treasures);
        }

        public @NotNull List<TrapType.TrapInstance> getTraps() {
            return Collections.unmodifiableList(this.traps);
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

        private @NotNull CompletableFuture<EditSession> paste(@NotNull RoomHandler handler, boolean async) {
            return paste(handler.getLocation(), async);
        }

        public @NotNull CompletableFuture<EditSession> paste(@NotNull Location location, boolean async) {
            return getClipboard(async).thenCompose(value -> WorldEditUtility.paste(location, value, async,
                    DeepDungeons.get(), false, true, true, false));
        }

        public class RoomHandler {

            private final Location location;
            private final DungeonHandler dungeonHandler;

            public RoomHandler(@NotNull DungeonHandler dungeonHandler, @NotNull Location location) {
                this.location = location;
                this.dungeonHandler = dungeonHandler;
            }

            public @NotNull CompletableFuture<EditSession> paste(boolean async) {
                return RoomInstance.this.paste(this, async);
            }

            public @NotNull DungeonHandler getDungeonHandler() {
                return dungeonHandler;
            }

            public @NotNull Location getLocation() {
                return location;
            }

            public @NotNull RoomType.RoomInstance getRoomInstance() {
                return RoomInstance.this;
            }
        }
    }
}
