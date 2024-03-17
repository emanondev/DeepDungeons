package emanondev.deepdungeons.room;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import emanondev.core.ItemBuilder;
import emanondev.core.YMLConfig;
import emanondev.core.YMLSection;
import emanondev.core.gui.AdvancedResearchFGui;
import emanondev.core.gui.Gui;
import emanondev.core.message.DMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.ActiveBuilder;
import emanondev.deepdungeons.DRInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.door.DoorTypeManager;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.interfaces.MoveListener;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import emanondev.deepdungeons.spawner.MonsterSpawnerTypeManager;
import emanondev.deepdungeons.trap.TrapType;
import emanondev.deepdungeons.trap.TrapTypeManager;
import emanondev.deepdungeons.treasure.TreasureType;
import emanondev.deepdungeons.treasure.TreasureTypeManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
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
        return readImpl(id, section);
    }

    public abstract @NotNull RoomInstanceBuilder getBuilder(@NotNull String id, @NotNull Player player);

    protected abstract @NotNull RoomInstance readImpl(@NotNull String id, @NotNull YMLSection section);


    public abstract class RoomInstanceBuilder extends DRInstance<RoomType> implements ActiveBuilder {

        private final List<DoorType.DoorInstanceBuilder> exits = new ArrayList<>();
        private final List<TreasureType.TreasureInstanceBuilder> treasures = new ArrayList<>();
        private final List<TrapType.TrapInstanceBuilder> traps = new ArrayList<>();
        private final List<MonsterSpawnerType.MonsterSpawnerInstanceBuilder> monsterSpawners = new ArrayList<>();
        private final HashSet<Material> breakableBlocks = new HashSet<>();
        private final HashSet<Material> placeableBlocks = new HashSet<>();
        private final CompletableFuture<RoomType.RoomInstanceBuilder> completableFuture = new CompletableFuture<>();
        private final UUID playerUuid;
        private DoorType.DoorInstanceBuilder entrance;
        private String schematicName;
        //private Clipboard clipboard;
        private World world;
        private BoundingBox area;
        private boolean hasCompletedBreakableMaterials = false;
        private boolean hasCompletedExitsCreation = false;
        private int tickCounter = 0;

        protected RoomInstanceBuilder(@NotNull String id, @NotNull Player player) {
            super(id, RoomType.this);
            this.playerUuid = player.getUniqueId();
            schematicName = this.getId() + ".schem";
        }

        public @NotNull CompletableFuture<RoomInstanceBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public @NotNull UUID getPlayerUUID() {
            return playerUuid;
        }

        public @Nullable Player getPlayer() {
            return Bukkit.getPlayer(playerUuid);
        }

        public DoorType.DoorInstanceBuilder getEntrance() {
            return entrance;
        }

        public void setEntrance(DoorType.DoorInstanceBuilder entrance) {
            this.entrance = entrance;
        }

        public @NotNull List<DoorType.DoorInstanceBuilder> getExits() {
            return exits;
        }

        public @NotNull List<TreasureType.TreasureInstanceBuilder> getTreasures() {
            return treasures;
        }

        public @NotNull List<TrapType.TrapInstanceBuilder> getTraps() {
            return traps;
        }

        public @NotNull List<MonsterSpawnerType.MonsterSpawnerInstanceBuilder> getMonsterSpawners() {
            return monsterSpawners;
        }

        public @NotNull Set<Material> getBreakableBlocks() {
            return breakableBlocks;
        }
        public @NotNull Set<Material> getPlaceableBlocks() {
            return placeableBlocks;
        }

        public String getSchematicName() {
            return schematicName;
        }

        public void setSchematicName(String schematicName) {
            this.schematicName = schematicName;
        }

        public final void write() throws Exception {
            if (!getCompletableFuture().isDone() || getCompletableFuture().isCompletedExceptionally())
                throw new IllegalArgumentException("cannot build a builder not correctly completed");
            if (RoomInstanceManager.getInstance().get(getId()) != null)
                throw new IllegalArgumentException("room id " + getId() + " is already used");

            YMLSection section = new YMLConfig(DeepDungeons.get(), "rooms" + File.separator + getId());
            section.set("type", getType().getId());
            YMLSection tmp = section.loadSection("entrance");
            entrance.writeTo(tmp);
            tmp = section.loadSection("exits");
            for (int i = 0; i < exits.size(); i++) {
                @NotNull YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                exits.get(i).writeTo(sub);
            }
            //remove itemStack & Drops

            HashMap<Block, Inventory> snapshotsInventories = new HashMap<>();
            HashMap<Block, BlockState> snapshotsStates = new HashMap<>();
            HashMap<Block, BlockData> snapshotsBlockData = new HashMap<>();
            HashMap<EntitySnapshot, Location> entitiesSnapshots = new HashMap<>();
            Collection<Entity> entities = getPlayer().getWorld().getNearbyEntities(area, (e) -> !(e instanceof Player));
            BoundingBox smallArea = getArea().expand(0, 0, 0, -1, -1, -1);
            BlockVector min = smallArea.getMin().toBlockVector();
            BlockVector max = smallArea.getMax().toBlockVector();
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++)
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++)
                    for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                        Block b = getPlayer().getWorld().getBlockAt(x, y, z);
                        if (!(b.getState() instanceof Container container))
                            continue;
                        snapshotsInventories.put(b, container.getSnapshotInventory());
                        snapshotsStates.put(b, b.getState());
                        snapshotsBlockData.put(b, b.getBlockData());
                        Inventory inv = container.getInventory();
                        boolean hasTreasures = false;
                        boolean hasMonsters = false;
                        for (int i = 0; i < inv.getSize(); i++) {
                            TreasureType.TreasureInstanceBuilder treasure = TreasureTypeManager
                                    .getInstance().getTreasureInstance(inv.getItem(i));
                            if (treasure != null) {
                                hasTreasures = true;
                                treasure.setOffset(new Vector(x, y, z).subtract(getOffset()).add(new Vector(0.5, 0, 0.5)));
                                treasures.add(treasure);
                                inv.setItem(i, null);
                            } else {
                                MonsterSpawnerType.MonsterSpawnerInstanceBuilder monster = MonsterSpawnerTypeManager
                                        .getInstance().getMonsterSpawnerInstance(inv.getItem(i));
                                if (monster != null) {
                                    hasMonsters = true;
                                    monster.setOffset(new Vector(x, y, z).subtract(getOffset()).add(new Vector(0.5, 0, 0.5)));
                                    if (b.getBlockData() instanceof Directional directional)
                                        monster.setDirection(directional.getFacing().getDirection());
                                    monsterSpawners.add(monster);
                                    inv.setItem(i, null);
                                }
                            }
                        }
                        if (hasMonsters && !hasTreasures) {
                            container.getInventory().clear();
                            b.setType(Material.AIR);
                        }
                        if (!hasMonsters && !hasTreasures) {
                            snapshotsBlockData.remove(b);
                            snapshotsStates.remove(b);
                            snapshotsInventories.remove(b);
                        }
                    }
            for (Entity entity : entities) {
                if (!(entity instanceof Item item))
                    continue;
                TreasureType.TreasureInstanceBuilder treasure = TreasureTypeManager.getInstance()
                        .getTreasureInstance(item.getItemStack());
                if (treasure != null) {
                    treasure.setOffset(item.getLocation().toVector().subtract(getOffset()));
                    treasures.add(treasure);
                    entitiesSnapshots.put(item.createSnapshot(), item.getLocation());
                    item.remove();
                    continue;
                }

                MonsterSpawnerType.MonsterSpawnerInstanceBuilder monster = MonsterSpawnerTypeManager.getInstance()
                        .getMonsterSpawnerInstance(item.getItemStack());
                if (monster != null) {
                    monster.setOffset(item.getLocation().toVector().subtract(getOffset()));
                    monster.setDirection(item.getLocation().getDirection());
                    monsterSpawners.add(monster);
                    entitiesSnapshots.put(item.createSnapshot(), item.getLocation());
                    item.remove();
                }
            }


            tmp = section.loadSection("treasures");
            for ( int i = 0; i < treasures.size(); i++) {
                YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                treasures.get(i).writeTo(sub);
            }

            tmp = section.loadSection("traps");
            for ( int i = 0; i < traps.size(); i++) {
                YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                traps.get(i).writeTo(sub);
            }

            tmp = section.loadSection("monsterspawners");
            for (  int i = 0; i < monsterSpawners.size(); i++) {
                YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                monsterSpawners.get(i).writeTo(sub);
            }
            section.set("schematic", schematicName);

            section.setEnumsAsStringList("breakableBlocks", breakableBlocks);
            section.setEnumsAsStringList("placeableBlocks", placeableBlocks);
            writeToImpl(section);
            section.save();
            WorldEditUtility.copy(smallArea, getPlayer().getWorld(), true, true, true,
                    DeepDungeons.get()).whenComplete((c, e) -> {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                WorldEditUtility.save(new File(DeepDungeons.get().getDataFolder(), "schematics" + File.separator + schematicName), c);
                new BukkitRunnable() {
                    public void run() {
                        entitiesSnapshots.forEach(EntitySnapshot::createEntity);
                        snapshotsStates.forEach((block, blockState) ->
                        {
                            blockState.update(true, false);
                            blockState.setBlockData(snapshotsBlockData.get(block));
                            if (blockState instanceof Container container)
                                container.getInventory().setContents(snapshotsInventories.get(block).getContents());
                        });
                        RoomInstanceManager.getInstance().readInstance(section.getFile());
                    }
                }.runTaskLater(DeepDungeons.get(), 20L);
            });
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        public final @Nullable BlockVector getSize() {
            return area == null ? null : new BlockVector(area.getWidthX(), area.getHeight(), area.getWidthZ());
        }

        public final void handleInteract(@NotNull PlayerInteractEvent event) {
            int heldSlot = event.getPlayer().getInventory().getHeldItemSlot();

            if (getArea() == null) {
                switch (heldSlot) {
                    case 2 -> Bukkit.dispatchCommand(event.getPlayer(),
                            event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ?
                                    "/pos1" : "/pos2");
                    case 6 -> {
                        BoundingBox box = WorldEditUtility.getSelectionBoxExpanded(event.getPlayer());
                        if (box == null) {//TODO lang
                            event.getPlayer().sendMessage("message not implemented: no area selected, use worldedit wand");
                            return;
                        }
                        Vector volume = box.getMax().subtract(box.getMin());
                        if (volume.getX() < 5 || volume.getZ() < 5 || volume.getY() < 4) {//TODO lang
                            event.getPlayer().sendMessage("message not implemented: selected area is too small");
                            return;
                        }
                        setArea(event.getPlayer().getWorld(), box);
                        this.setEntrance(DoorTypeManager.getInstance().getStandard().getBuilder(this));
                        setupTools();
                        getEntrance().getCompletableFuture().whenComplete((b, t) -> {
                            if (t != null) {
                                this.getCompletableFuture().completeExceptionally(t);
                            } else {
                                this.setupTools();
                                getPlayer().getInventory().setHeldItemSlot(0);
                            }
                        });
                        WorldEditUtility.clearSelection(event.getPlayer());
                        getPlayer().getInventory().setHeldItemSlot(0);
                    }
                }
                return;
            }
            if (!getEntrance().getCompletableFuture().isDone()) {
                getEntrance().handleInteract(event);
                return;
            }
            if (!hasCompletedExitsCreation) {
                if (!(exits.isEmpty() || exits.get(exits.size() - 1).getCompletableFuture().isDone())) {
                    exits.get(exits.size() - 1).handleInteract(event);
                    return;
                }
                switch (heldSlot) {
                    case 1 -> {
                        ArrayList<DoorType> types = new ArrayList<>(DoorTypeManager.getInstance().getAll());
                        types.sort(Comparator.comparing(DRegistryElement::getId));
                        new AdvancedResearchFGui<>(//TODO lang
                                new DMessage(DeepDungeons.get(), event.getPlayer()).append("&8Choose an Exit Door type"),
                                event.getPlayer(), null, DeepDungeons.get(),
                                new ItemBuilder(Material.SPRUCE_DOOR).setDescription(new DMessage(
                                                DeepDungeons.get(), event.getPlayer()
                                        ).append(">").newLine().append("<white>Choose door type")//TODO configurable
                                ).build(), (String text, DoorType type) -> {
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(type.getId().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                                (evt, type) -> {
                                    DoorType.DoorInstanceBuilder door = type.getBuilder(this);
                                    exits.add(door);
                                    door.getCompletableFuture().whenComplete((b, t) -> {
                                        if (t != null) {
                                            this.getCompletableFuture().completeExceptionally(t);
                                        } else {
                                            this.setupTools();
                                        }
                                    });
                                    event.getPlayer().closeInventory();
                                    setupTools();
                                    return false;
                                },
                                (type) -> new ItemBuilder(Material.SPRUCE_DOOR).setDescription(
                                        new DMessage(DeepDungeons.get(), event.getPlayer())
                                                .append("<gold><bold>" + type.getId()) //TODO lang
                                                .newLine().append("Click to choose")
                                ).build(),
                                types
                        ).open(event.getPlayer());
                    }
                    case 6 -> {
                        if (!exits.isEmpty()) {
                            hasCompletedExitsCreation = true;
                            setupTools();
                            getPlayer().getInventory().setHeldItemSlot(0);
                        }
                    }
                }
                return;
            }

            if (!hasCompletedBreakableMaterials) {
                switch (heldSlot) {
                    case 1 -> {
                        ArrayList<Material> types = new ArrayList<>(List.of(Material.values()));
                        types.removeIf((m) -> !m.isBlock() || m.isAir());
                        types.sort(Comparator.comparing(Material::name));
                        new AdvancedResearchFGui<>(
                                new DMessage(DeepDungeons.get(), event.getPlayer()).append("&8Choose Breakable blocks"),//TODO lang
                                event.getPlayer(), null, DeepDungeons.get(),
                                new ItemBuilder(Material.SPRUCE_DOOR).setDescription(new DMessage(
                                                DeepDungeons.get(), event.getPlayer()
                                        ).append(">").newLine().append("<white>Choose Blocks")//TODO lang
                                ).build(), (String text, Material type) -> {
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(type.name().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                                (evt, type) -> {
                                    if (breakableBlocks.contains(type))
                                        breakableBlocks.remove(type);
                                    else
                                        breakableBlocks.add(type);
                                    return true;
                                },
                                (type) -> new ItemBuilder(type.isItem() ? type : Material.BARRIER)
                                        .addEnchantment(Enchantment.DURABILITY, breakableBlocks.contains(type) ? 1 : 0)
                                        .setGuiProperty().setDescription(new DMessage(DeepDungeons.get(), event.getPlayer())
                                                .append("<gold><bold>" + type.name()) //TODO lang
                                                .newLine().append("Click to toggle")
                                                .newLine().append("Enabled? " + breakableBlocks.contains(type))).build(),
                                types
                        ).open(event.getPlayer());
                    }
                    case 2 -> {
                        ArrayList<Material> types = new ArrayList<>(List.of(Material.values()));
                        types.removeIf((m) -> !m.isBlock() || m.isAir());
                        types.sort(Comparator.comparing(Material::name));
                        new AdvancedResearchFGui<>(
                                new DMessage(DeepDungeons.get(), event.getPlayer()).append("&8Choose Placeable blocks"),//TODO lang
                                event.getPlayer(), null, DeepDungeons.get(),
                                new ItemBuilder(Material.SPRUCE_DOOR).setDescription(new DMessage(
                                                DeepDungeons.get(), event.getPlayer()
                                        ).append(">").newLine().append("<white>Choose Blocks")//TODO lang
                                ).build(), (String text, Material type) -> {
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(type.name().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                                (evt, type) -> {
                                    if (placeableBlocks.contains(type))
                                        placeableBlocks.remove(type);
                                    else
                                        placeableBlocks.add(type);
                                    return true;
                                },
                                (type) -> new ItemBuilder(type.isItem() ? type : Material.BARRIER)
                                        .addEnchantment(Enchantment.DURABILITY, placeableBlocks.contains(type) ? 1 : 0)
                                        .setGuiProperty().setDescription(new DMessage(DeepDungeons.get(), event.getPlayer())
                                                .append("<gold><bold>" + type.name()) //TODO lang
                                                .newLine().append("Click to toggle")
                                                .newLine().append("Enabled? " + placeableBlocks.contains(type))).build(),
                                types
                        ).open(event.getPlayer());
                    }
                    case 6 -> {
                        hasCompletedBreakableMaterials = true;
                        setupTools();
                        getPlayer().getInventory().setHeldItemSlot(0);
                    }
                }
            }
            //TODO traps
            handleInteractImpl(event);
        }

        public final void setupTools() {
            Player player = getPlayer();
            if (player == null || !player.isValid())
                return;
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Gui)
                return;
            Inventory inv = player.getInventory();
            for (int i = 0; i < 8; i++) //clear
                inv.setItem(i, null);
            inv.setItem(8, new ItemBuilder(Material.BARRIER).setDescription(new DMessage(DeepDungeons.get(), player)
                    .append("Click to exit/abort building")).build());//TODO lang
            if (getArea() == null) {
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_area_info")).build());
                inv.setItem(1, new ItemBuilder(Material.WOODEN_AXE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_area_axe")).build());
                inv.setItem(2, new ItemBuilder(Material.BROWN_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_area_pos")).build());
                inv.setItem(6, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_area_confirm")).build());
                return;
            }
            if (!getEntrance().getCompletableFuture().isDone()) {
                getEntrance().setupTools();
                return;
            }
            if (!hasCompletedExitsCreation) {
                if (exits.isEmpty() || exits.get(exits.size() - 1).getCompletableFuture().isDone()) {
                    inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                            .appendLang("roombuilder.base_exits_info")).build());
                    inv.setItem(1, new ItemBuilder(Material.SPRUCE_DOOR).setDescription(new DMessage(DeepDungeons.get(), player)
                            .appendLang("roombuilder.base_exits_selector")).build());
                    if (!exits.isEmpty())
                        inv.setItem(6, new ItemBuilder(Material.LIGHT_BLUE_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                                .appendLang("roombuilder.base_exits_confirm","%value%",String.valueOf(exits.size()))).build());
                } else {
                    exits.get(exits.size() - 1).setupTools();
                }
                return;
            }

            if (!hasCompletedBreakableMaterials) {
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_commondata_info")).build());
                inv.setItem(1, new ItemBuilder(Material.IRON_PICKAXE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_commondata_break","%value%",String.valueOf(breakableBlocks.size()))).build());
                inv.setItem(2, new ItemBuilder(Material.BRICKS).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_commondata_place","%value%",String.valueOf(placeableBlocks.size()))).build());
                inv.setItem(6, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("roombuilder.base_commondata_confirm")).build());
                return;
            }

            setupToolsImpl();
        }

        public int getTickCounter() {
            return tickCounter;
        }

        public void timerTick() {
            tickCounter++;
            Player player = getPlayer();
            if (player == null)
                return;
            if (tickCounter % 2 == 0) { //reduce particle amount = have a tick 5 time per second instead of 10
                if (area != null)
                    ParticleUtility.spawnParticleBoxFaces(player, (tickCounter) / 6, 8, Particle.REDSTONE, area,
                            new Particle.DustOptions(Color.BLUE, 0.25F));
                else
                    showWEBound(player);

                if (getEntrance() == null)
                    return;
                getEntrance().timerTick(player, Color.LIME);

                if (exits.isEmpty())
                    return;
                for (int i = 0; i < exits.size(); i++) {
                    DoorType.DoorInstanceBuilder exit = exits.get(i);
                    exit.timerTick(player, switch (i % 5) {
                        case 0 -> Color.RED;
                        case 1 -> Color.ORANGE;
                        case 2 -> Color.YELLOW;
                        case 3 -> Color.fromBGR(255, 0, 165);
                        default -> Color.FUCHSIA;
                    });
                }
            }
            timerTickImpl();
        }

        protected abstract void timerTickImpl();

        protected void showWEBound(@NotNull Player player) {
            ParticleUtility.spawnParticleBoxFaces(player, tickCounter / 6 + 6, 4, Particle.REDSTONE, WorldEditUtility.getSelectionBoxExpanded(player),
                    new Particle.DustOptions(Color.WHITE, 0.3F));
        }

        protected abstract void handleInteractImpl(PlayerInteractEvent event);

        protected abstract void setupToolsImpl();

        public boolean contains(@NotNull Location loc) {
            return Objects.equals(loc.getWorld(), world) && area.contains(loc.toVector());
        }

        @Nullable
        public BlockVector getOffset() {
            return area == null ? null : area.getMin().toBlockVector();
        }

        @Nullable
        public BoundingBox getArea() {
            return area == null ? null : area.clone();
        }

        protected void setArea(@NotNull World world, @NotNull BoundingBox box) {
            this.world = world;
            area = box.clone();
        }

        public @Nullable World getWorld() {
            return world;
        }
    }


    public abstract class RoomInstance extends DRInstance<RoomType> {

        private final DoorType.DoorInstance entrance;
        private final List<DoorType.DoorInstance> exits = new ArrayList<>();
        private final List<TreasureType.TreasureInstance> treasures = new ArrayList<>();
        private final List<TrapType.TrapInstance> traps = new ArrayList<>();
        private final List<MonsterSpawnerType.MonsterSpawnerInstance> monsterSpawners = new ArrayList<>();

        private final Set<Material> breakableBlocks = new HashSet<>();
        private final Set<Material> placeableBlocks = new HashSet<>();
        private final String schematicName;
        private final BlockVector size;
        private SoftReference<Clipboard> clipboard = null;
        private CompletableFuture<Clipboard> futureClipboard;

        public RoomInstance(@NotNull String id, @NotNull YMLSection section) {
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
            this.placeableBlocks.addAll(section.getMaterialList("placeableBlocks", Collections.emptyList()));
            BlockVector3 dim;
            try {
                dim = getClipboard(false).join().getDimensions();
            } catch (Throwable e) {
                e.printStackTrace();
                throw new IllegalStateException();
            }
            this.size = new BlockVector(dim.getBlockX(), dim.getBlockY(), dim.getBlockZ());
        }

        public @NotNull BlockVector getSize() {
            return size.clone();
        }

        public @NotNull Set<Material> getBreakableBlocks() {
            return Collections.unmodifiableSet(breakableBlocks);
        }
        public @NotNull Set<Material> getPlaceableBlocks() {
            return Collections.unmodifiableSet(placeableBlocks);
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
                return futureClipboard; //TODO what a mess
            CompletableFuture<Clipboard> result = CompletableFuture.completedFuture(WorldEditUtility.load(getSchematic(), DeepDungeons.get()));//.load(getSchematic(), DeepDungeons.get(), async);
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

        @Contract("_->new")
        public abstract @NotNull RoomHandler createRoomHandler(DungeonType.DungeonInstance.DungeonHandler dungeonHandler);


        public class RoomHandler implements MoveListener {

            private final DungeonType.DungeonInstance.DungeonHandler dungeonHandler;
            private final DoorType.DoorInstance.DoorHandler entranceHandler;
            private final List<DoorType.DoorInstance.DoorHandler> exits = new ArrayList<>();
            private Location location = null;
            private BoundingBox boundingBox = null;
            private final List<Entity> monsters = new ArrayList<>();
            private boolean firstEnter = false;

            public RoomHandler(@NotNull DungeonType.DungeonInstance.DungeonHandler dungeonHandler) {
                this.dungeonHandler = dungeonHandler;
                this.entranceHandler = getRoomInstance().getEntrance().createDoorHandler(this);
                for (DoorType.DoorInstance exit : getRoomInstance().getExits())
                    exits.add(exit.createDoorHandler(this));
            }

            @Contract(pure = true)
            public DoorType.DoorInstance.DoorHandler getEntrance() {
                return entranceHandler;
            }

            @Contract(pure = true)
            public List<DoorType.DoorInstance.DoorHandler> getExits() {
                return Collections.unmodifiableList(exits);
            }

            @Contract(pure = true)
            public @NotNull CompletableFuture<EditSession> paste(boolean async) {
                return RoomInstance.this.paste(this, async);
            }

            @Contract(pure = true)
            public @NotNull DungeonType.DungeonInstance.DungeonHandler getDungeonHandler() {
                return dungeonHandler;
            }

            @Contract(pure = true, value = "-> new")
            public @NotNull Location getLocation() {
                return location.clone();
            }

            @Contract(pure = true, value = "-> new")
            public @NotNull BoundingBox getBoundingBox() {
                return boundingBox.clone();
            }

            @Contract(pure = true)
            public @NotNull RoomType.RoomInstance getRoomInstance() {
                return RoomInstance.this;
            }

            @Contract(pure = true)
            public @NotNull BlockVector getSize() {
                return getRoomInstance().getSize();
            }

            public void setupOffset(@NotNull Vector roomOffset) {
                if (this.location != null)
                    throw new IllegalStateException();
                this.location = getDungeonHandler().getLocation().add(roomOffset);
                this.boundingBox = BoundingBox.of(location.toVector(), location.toVector().add(getSize()));
                getEntrance().setupOffset();
                exits.forEach(DoorType.DoorInstance.DoorHandler::setupOffset);
            }

            public void onPlayerMove(@NotNull PlayerMoveEvent event) {
                if (this.getEntrance().contains(event.getTo())) {
                    getEntrance().onPlayerMove(event);
                    return;
                }
                for (DoorType.DoorInstance.DoorHandler exit : this.getExits())
                    if (exit.contains(event.getTo())) {
                        exit.onPlayerMove(event);
                        return;
                    }
                //TODO traps
            }

            public boolean contains(@NotNull Block block) {
                return contains(block.getLocation());
            }

            public boolean contains(@NotNull BlockState block) {
                return contains(block.getLocation());
            }

            public boolean contains(@NotNull Location loc) {
                return getDungeonHandler().getWorld().equals(loc.getWorld()) && contains(loc.toVector());
            }

            public boolean contains(@NotNull Vector vector) {
                return this.boundingBox.contains(vector);
            }

            public boolean overlaps(@NotNull BoundingBox box) {
                return this.boundingBox.overlaps(box);
            }

            public boolean overlaps(@NotNull Entity box) {
                return overlaps(box.getBoundingBox());
            }

            public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {

            }

            public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
                if (!firstEnter) {
                    firstEnter = true;
                    onFirstPlayerEnter(event.getPlayer());
                }


            }

            protected void onFirstPlayerEnter(@NotNull Player player) {
                //TODO may generate treasures on chest opens instead
                this.getRoomInstance().getTreasures().forEach((treasure -> {
                    Location to = getLocation().add(treasure.getOffset());
                    if (to.getBlock().getState() instanceof Container container) {
                        Inventory inv = container.getInventory();
                        inv.addItem(treasure.getTreasure(new Random(), to, player).toArray(new ItemStack[0]));
                        ItemStack[] stacks = inv.getContents();
                        List<ItemStack> contained = Arrays.asList(stacks);
                        Collections.shuffle(contained);
                        inv.setContents(contained.toArray(stacks));
                    } else
                        treasure.getTreasure(new Random(), to, player).forEach(itemStack -> to.getWorld().dropItem(to, itemStack));
                }));
                this.getRoomInstance().getMonsterSpawners().forEach((monsterSpawner) -> {
                    Location to = getLocation().add(monsterSpawner.getOffset());
                    addMonsters(monsterSpawner.spawnMobs(new Random(), to, player));
                });
                //TODO traps
                this.getEntrance().onFirstPlayerEnter(player);
                this.getExits().forEach(e -> e.onFirstPlayerEnter(player));
            }

            public void onBlockPlace(@NotNull BlockPlaceEvent event) {
                if (!getRoomInstance().getPlaceableBlocks().contains(event.getBlock().getType()))
                    event.setCancelled(true);
            }

            public void onBlockBreak(@NotNull BlockBreakEvent event) {
                if (!getRoomInstance().getBreakableBlocks().contains(event.getBlock().getType()))
                    event.setCancelled(true);
            }

            public void onPlayerBucketFill(@NotNull PlayerBucketFillEvent event) {
                event.setCancelled(true);
            }

            public void onPlayerBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
                event.setCancelled(true);
            }


            public @NotNull List<Entity> getMonsters() {
                monsters.removeIf(entity -> !entity.isValid() || !overlaps(entity));
                return Collections.unmodifiableList(monsters);
            }

            public void addMonsters(@NotNull Collection<Entity> monsters) {
                this.monsters.addAll(monsters);
            }
        }


    }


}
