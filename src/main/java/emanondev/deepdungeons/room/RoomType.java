package emanondev.deepdungeons.room;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import emanondev.core.YMLConfig;
import emanondev.core.YMLSection;
import emanondev.core.gui.AdvancedResearchFGui;
import emanondev.core.gui.Gui;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.ActiveBuilder;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DRInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.door.DoorType.DoorBuilder;
import emanondev.deepdungeons.door.DoorType.DoorInstance;
import emanondev.deepdungeons.door.DoorType.DoorInstance.DoorHandler;
import emanondev.deepdungeons.door.DoorTypeManager;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.interfaces.*;
import emanondev.deepdungeons.interfaces.PaperPopulatorType.PaperPopulatorBuilder;
import emanondev.deepdungeons.interfaces.PopulatorType.PopulatorBuilder;
import emanondev.deepdungeons.interfaces.PopulatorType.PopulatorInstance;
import emanondev.deepdungeons.populator.PopulatorTypeManager;
import emanondev.deepdungeons.trap.TrapType;
import emanondev.deepdungeons.trap.TrapType.TrapBuilder;
import emanondev.deepdungeons.trap.TrapType.TrapInstance;
import emanondev.deepdungeons.trap.TrapType.TrapInstance.TrapHandler;
import emanondev.deepdungeons.trap.TrapTypeManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
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

    @NotNull
    public final RoomInstance read(@NotNull String id, @NotNull YMLSection section) {
        return readImpl(id, section);
    }

    @NotNull
    public abstract RoomBuilder getBuilder(@NotNull String id, @NotNull Player player);

    @NotNull
    protected abstract RoomInstance readImpl(@NotNull String id, @NotNull YMLSection section);


    public abstract class RoomBuilder extends DRInstance<RoomType> implements ActiveBuilder {

        private final List<DoorBuilder> exits = new ArrayList<>();
        private final List<TrapBuilder> traps = new ArrayList<>();
        private final List<PopulatorType.PopulatorBuilder> populatorBuilders = new ArrayList<>();
        private final HashSet<Material> breakableBlocks = new HashSet<>();
        private final HashSet<Material> placeableBlocks = new HashSet<>();
        private final CompletableFuture<RoomBuilder> completableFuture = new CompletableFuture<>();
        private final UUID playerUuid;
        private final String schematicName;
        private DoorBuilder entrance;
        private World world;
        private BoundingBox area;
        private boolean hasCompletedBreakableMaterials = false;
        private boolean hasCompletedExitsCreation = false;
        private boolean hasCompletedTrapsCreation = false;
        private int tickCounter = 0;
        private boolean hasCompletedPopulatorCreation = false;

        protected RoomBuilder(@NotNull String id, @NotNull Player player) {
            super(id, RoomType.this);
            this.playerUuid = player.getUniqueId();
            schematicName = this.getId() + ".schem";
        }

        @NotNull
        public CompletableFuture<RoomBuilder> getCompletableFuture() {
            return completableFuture;
        }

        @NotNull
        public final UUID getPlayerUUID() {
            return playerUuid;
        }

        @Nullable
        public final Player getPlayer() {
            return Bukkit.getPlayer(playerUuid);
        }


        public void setEntrance(DoorBuilder entrance) {
            this.entrance = entrance;
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
                YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                exits.get(i).writeTo(sub);
            }

            HashMap<Block, Inventory> snapshotsInventories = new HashMap<>();
            HashMap<Block, BlockState> snapshotsStates = new HashMap<>();
            HashMap<Block, BlockData> snapshotsBlockData = new HashMap<>();
            HashMap<EntitySnapshot, Location> entitiesSnapshots = new HashMap<>();
            Collection<Entity> entities = getPlayer().getWorld().getNearbyEntities(area, (e) -> !(e instanceof Player));
            BoundingBox smallArea = getArea().expand(0, 0, 0, -1, -1, -1);
            BlockVector min = smallArea.getMin().toBlockVector();
            BlockVector max = smallArea.getMax().toBlockVector();


            List<PaperPopulatorBuilder> paperPopulators = new ArrayList<>();

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
                        boolean preserveContainer = false;
                        boolean hasSomething = false;
                        for (int i = 0; i < inv.getSize(); i++) {
                            PaperPopulatorBuilder populator = PopulatorTypeManager
                                    .getInstance().getPaperPopulatorBuilder(inv.getItem(i));
                            if (populator != null) {
                                preserveContainer |= populator.preserveContainer();
                                hasSomething = true;
                                Location offset = new Location(null, x, y, z);
                                if (b.getBlockData() instanceof Directional directional) {
                                    offset.setDirection(directional.getFacing().getDirection());
                                }
                                populator.setOffset(offset.subtract(getOffset()).add(new Vector(0.5, 0, 0.5)));
                                paperPopulators.add(populator);
                                inv.setItem(i, null);
                            }
                        }
                        if (hasSomething && !preserveContainer) {
                            container.getInventory().clear();
                            b.setType(Material.AIR);
                        }
                        if (!hasSomething) {
                            snapshotsBlockData.remove(b);
                            snapshotsStates.remove(b);
                            snapshotsInventories.remove(b);
                        }
                    }
            for (Entity entity : entities) {
                if (!(entity instanceof Item item))
                    continue;
                PaperPopulatorBuilder populator = PopulatorTypeManager
                        .getInstance().getPaperPopulatorBuilder(item.getItemStack());
                if (populator != null) {
                    populator.setOffset(item.getLocation().subtract(getOffset()));
                    paperPopulators.add(populator);
                    entitiesSnapshots.put(item.createSnapshot(), item.getLocation());
                    item.remove();
                }
            }


            tmp = section.loadSection("populators");
            int index = 0;
            for (; index < this.populatorBuilders.size(); index++) {
                YMLSection sub = tmp.loadSection(String.valueOf(index + 1));
                try {
                    this.populatorBuilders.get(index).writeTo(sub);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            for (; index < paperPopulators.size(); index++) {
                YMLSection sub = tmp.loadSection(String.valueOf(index + 1));
                try {
                    paperPopulators.get(index).writeTo(sub);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            tmp = section.loadSection("traps");
            for (int i = 0; i < traps.size(); i++) {
                YMLSection sub = tmp.loadSection(String.valueOf(i + 1));
                try {
                    traps.get(i).writeTo(sub);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
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

        @Nullable
        public final BlockVector getSize() {
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
                        if (box == null) {
                            CUtils.sendMsg(event.getPlayer(), "roombuilder.base_msg_must_set_area");
                            return;
                        }
                        Vector volume = box.getMax().subtract(box.getMin());
                        if (volume.getX() < 5 || volume.getZ() < 5 || volume.getY() < 4) {
                            CUtils.sendMsg(event.getPlayer(), "roombuilder.base_msg_too_small");
                            return;
                        }
                        setArea(event.getPlayer().getWorld(), box);
                        this.setEntrance(DoorTypeManager.getInstance().getStandard().getBuilder(this));
                        setupTools();
                        entrance.getCompletableFuture().whenComplete((b, t) -> {
                            if (t != null) {
                                this.getCompletableFuture().completeExceptionally(t);
                            } else {
                                getPlayer().getInventory().setHeldItemSlot(0);
                                this.setupTools();
                            }
                        });
                        WorldEditUtility.clearSelection(event.getPlayer());
                        getPlayer().getInventory().setHeldItemSlot(0);
                    }
                }
                return;
            }
            if (!entrance.getCompletableFuture().isDone()) {
                entrance.handleInteract(event);
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
                        new AdvancedResearchFGui<>(
                                CUtils.craftMsg(event.getPlayer(), "roombuilder.base_exits_guititle"),
                                event.getPlayer(), null, DeepDungeons.get(),
                                CUtils.emptyIBuilder(Material.SPRUCE_DOOR).setDescription(CUtils.emptyMsg(event.getPlayer())
                                        .append(">").newLine().appendLang("roombuilder.base_exits_guihelp")
                                ).build(), (String text, DoorType type) -> {
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(type.getId().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                                (evt, type) -> {
                                    DoorBuilder door = type.getBuilder(this);
                                    exits.add(door);
                                    door.getCompletableFuture().whenComplete((b, t) -> {
                                        if (t != null) {
                                            this.getCompletableFuture().completeExceptionally(t);
                                        } else {
                                            this.getPlayer().getInventory().setHeldItemSlot(0);
                                            this.setupTools();
                                        }
                                    });
                                    event.getPlayer().closeInventory();
                                    setupTools();
                                    return false;
                                },
                                (type) -> CUtils.createItem(event.getPlayer(), Material.SPRUCE_DOOR,
                                        "roombuilder.base_exits_guiitem", "%id%", type.getId()),
                                types
                        ).open(event.getPlayer());
                    }
                    case 6 -> {
                        if (!exits.isEmpty()) {
                            hasCompletedExitsCreation = true;
                            getPlayer().getInventory().setHeldItemSlot(0);
                            setupTools();
                        }
                    }
                }
                return;
            }
            if (!hasCompletedTrapsCreation) {
                if (!(traps.isEmpty() || traps.get(traps.size() - 1).getCompletableFuture().isDone())) {
                    traps.get(traps.size() - 1).handleInteract(event);
                    return;
                }
                switch (heldSlot) {
                    case 1 -> {
                        ArrayList<TrapType> types = new ArrayList<>(TrapTypeManager.getInstance().getAll());
                        types.sort(Comparator.comparing(DRegistryElement::getId));
                        new AdvancedResearchFGui<>(
                                CUtils.craftMsg(event.getPlayer(), "roombuilder.base_traps_guititle"),
                                event.getPlayer(), null, DeepDungeons.get(),
                                CUtils.emptyIBuilder(Material.TRIPWIRE_HOOK).setDescription(CUtils.emptyMsg(event.getPlayer())
                                        .append(">").newLine().appendLang("roombuilder.base_traps_guihelp")
                                ).build(), (String text, TrapType type) -> {
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(type.getId().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                                (evt, type) -> {
                                    TrapBuilder trap = type.getBuilder(this);
                                    traps.add(trap);
                                    trap.getCompletableFuture().whenComplete((b, t) -> {
                                        if (t != null) {
                                            this.getCompletableFuture().completeExceptionally(t);
                                        } else {
                                            this.getPlayer().getInventory().setHeldItemSlot(0);
                                            this.setupTools();
                                        }
                                    });
                                    event.getPlayer().closeInventory();
                                    setupTools();
                                    return false;
                                },
                                (type) -> CUtils.createItem(event.getPlayer(), Material.TRIPWIRE_HOOK,
                                        "roombuilder.base_traps_guiitem", "%id%", type.getId()),
                                types
                        ).open(event.getPlayer());
                    }
                    case 6 -> {
                        if (!traps.isEmpty()) {
                            hasCompletedTrapsCreation = true;
                            getPlayer().getInventory().setHeldItemSlot(0);
                            setupTools();
                        }
                    }
                }
                return;
            }
            if (!this.hasCompletedPopulatorCreation) {
                if (!(populatorBuilders.isEmpty() || populatorBuilders.get(populatorBuilders.size() - 1).getCompletableFuture().isDone())) {
                    populatorBuilders.get(populatorBuilders.size() - 1).handleInteract(event);
                    return;
                }
                switch (heldSlot) {
                    case 1 -> {
                        ArrayList<PopulatorType> types = new ArrayList<>(PopulatorTypeManager.getInstance().getAll());
                        types.sort(Comparator.comparing(PopulatorType::getId));
                        new AdvancedResearchFGui<>(
                                CUtils.craftMsg(event.getPlayer(), "roombuilder.base_populators_guititle"),
                                event.getPlayer(), null, DeepDungeons.get(),
                                CUtils.emptyIBuilder(Material.TURTLE_EGG).setDescription(CUtils.emptyMsg(event.getPlayer())
                                        .append(">").newLine().appendLang("roombuilder.base_populators_guihelp")
                                ).build(), (String text, PopulatorType type) -> {
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(type.getId().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                                (evt, type) -> {
                                    PopulatorBuilder pop = type.getBuilder(this);
                                    populatorBuilders.add(pop);
                                    pop.getCompletableFuture().whenComplete((b, t) -> {
                                        if (t != null) {
                                            this.getCompletableFuture().completeExceptionally(t);
                                        } else {
                                            this.getPlayer().getInventory().setHeldItemSlot(0);
                                            this.setupTools();
                                        }
                                    });
                                    event.getPlayer().closeInventory();
                                    setupTools();
                                    return false;
                                },
                                (type) -> CUtils.createItem(event.getPlayer(), Material.TRIPWIRE_HOOK,
                                        "roombuilder.base_populators_guiitem", "%id%", type.getId()),
                                types
                        ).open(event.getPlayer());
                    }
                    case 6 -> {
                        if (!traps.isEmpty()) {
                            hasCompletedPopulatorCreation = true;
                            getPlayer().getInventory().setHeldItemSlot(0);
                            setupTools();
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
                                CUtils.craftMsg(event.getPlayer(), "roombuilder.base_commondata_guibreaktitle"),
                                event.getPlayer(), null, DeepDungeons.get(),
                                CUtils.emptyIBuilder(Material.SPRUCE_DOOR).setDescription(CUtils.emptyMsg(event.getPlayer())
                                        .append(">").newLine().appendLang("roombuilder.base_commondata_guibreakhelp")
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
                                (type) -> CUtils.createItem(event.getPlayer(), type.isItem() ? type : Material.BARRIER, 1,
                                        breakableBlocks.contains(type), "roombuilder.base_commondata_guibreaktitle",
                                        "%id%" + type.name(),
                                        "%selected%", breakableBlocks.contains(type) ? ("<green>true</green>") : ("<red>false</red>")),
                                types
                        ).open(event.getPlayer());
                    }
                    case 2 -> {
                        ArrayList<Material> types = new ArrayList<>(List.of(Material.values()));
                        types.removeIf((m) -> !m.isBlock() || m.isAir());
                        types.sort(Comparator.comparing(Material::name));
                        new AdvancedResearchFGui<>(
                                CUtils.craftMsg(event.getPlayer(), "roombuilder.base_commondata_guiplacetitle"),
                                event.getPlayer(), null, DeepDungeons.get(),
                                CUtils.emptyIBuilder(Material.SPRUCE_DOOR).setDescription(
                                        CUtils.emptyMsg(event.getPlayer()).append(">").newLine()
                                                .appendLang("roombuilder.base_commondata_guiplacehelp")
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
                                (type) -> CUtils.createItem(event.getPlayer(), type.isItem() ? type : Material.BARRIER,
                                        1, placeableBlocks.contains(type),
                                        "roombuilder.base_commondata_guiplacetitle", "%id%" + type.name(),
                                        "%selected%", placeableBlocks.contains(type) ? ("<green>true</green>") : ("<red>false</red>")),
                                types
                        ).open(event.getPlayer());
                    }
                    case 6 -> {
                        hasCompletedBreakableMaterials = true;
                        getPlayer().getInventory().setHeldItemSlot(0);
                        setupTools();
                    }
                }
            }
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
            CUtils.setSlot(player, 8, inv, Material.BARRIER, "roombuilder.exit_mode");
            if (getArea() == null) {
                CUtils.setSlot(player, 0, inv, Material.PAPER, "roombuilder.base_area_info");
                CUtils.setSlot(player, 1, inv, Material.WOODEN_AXE, "roombuilder.base_area_axe");
                CUtils.setSlot(player, 2, inv, Material.BROWN_DYE, "roombuilder.base_area_pos");
                CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "roombuilder.base_area_confirm");
                return;
            }
            if (!entrance.getCompletableFuture().isDone()) {
                entrance.setupTools();
                return;
            }
            if (!hasCompletedExitsCreation) {
                if (exits.isEmpty() || exits.get(exits.size() - 1).getCompletableFuture().isDone()) {
                    CUtils.setSlot(player, 0, inv, Material.PAPER, "roombuilder.base_exits_info");
                    CUtils.setSlot(player, 1, inv, Material.SPRUCE_DOOR, "roombuilder.base_exits_selector");
                    if (!exits.isEmpty())
                        CUtils.setSlot(player, 6, inv, Material.LIGHT_BLUE_DYE, "roombuilder.base_exits_confirm",
                                "%value%", String.valueOf(exits.size()));
                } else {
                    exits.get(exits.size() - 1).setupTools();
                }
                return;
            }
            if (!hasCompletedTrapsCreation) {
                if (traps.isEmpty() || traps.get(traps.size() - 1).getCompletableFuture().isDone()) {
                    CUtils.setSlot(player, 0, inv, Material.PAPER, "roombuilder.base_traps_info");
                    CUtils.setSlot(player, 1, inv, Material.SPRUCE_DOOR, "roombuilder.base_traps_selector");
                    CUtils.setSlot(player, 6, inv, Material.LIGHT_BLUE_DYE, "roombuilder.base_traps_confirm",
                            "%value%", String.valueOf(traps.size()));
                } else {
                    traps.get(traps.size() - 1).setupTools();
                }
                return;
            }

            if (!hasCompletedPopulatorCreation) {
                if (populatorBuilders.isEmpty() || populatorBuilders.get(populatorBuilders.size() - 1).getCompletableFuture().isDone()) {
                    CUtils.setSlot(player, 0, inv, Material.PAPER, "roombuilder.base_populators_info");
                    CUtils.setSlot(player, 1, inv, Material.SPRUCE_DOOR, "roombuilder.base_populators_selector");
                    CUtils.setSlot(player, 6, inv, Material.LIGHT_BLUE_DYE, "roombuilder.base_populators_confirm",
                            "%value%", String.valueOf(populatorBuilders.size()));
                } else {
                    populatorBuilders.get(populatorBuilders.size() - 1).setupTools();
                }
                return;
            }

            if (!hasCompletedBreakableMaterials) {
                CUtils.setSlot(player, 0, inv, Material.PAPER, "roombuilder.base_commondata_info");
                CUtils.setSlot(player, 1, inv, Material.IRON_PICKAXE, "roombuilder.base_commondata_break",
                        "%value%", String.valueOf(breakableBlocks.size()));
                CUtils.setSlot(player, 2, inv, Material.BRICKS, "roombuilder.base_commondata_place",
                        "%value%", String.valueOf(placeableBlocks.size()));
                CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "roombuilder.base_commondata_confirm");
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
                    CUtils.showWEBound(player, getTickCounter());

                if (!hasCompletedExitsCreation) {
                    if (entrance == null)
                        return;
                    entrance.timerTick(player, Color.LIME);

                    if (exits.isEmpty())
                        return;
                    for (int i = 0; i < exits.size(); i++) {
                        DoorBuilder exit = exits.get(i);
                        exit.timerTick(player, CUtils.craftColorRedSpectrum(i));
                    }
                }
                if (!hasCompletedTrapsCreation) {
                    for (int i = 0; i < traps.size(); i++) {
                        TrapBuilder trap = traps.get(i);
                        trap.timerTick(player, CUtils.craftColorRedSpectrum(i));
                    }
                }
                if (!hasCompletedPopulatorCreation) {
                    for (int i = 0; i < populatorBuilders.size(); i++) {
                        PopulatorBuilder pop = populatorBuilders.get(i);
                        pop.timerTick(player, CUtils.craftColorRedSpectrum(i));
                    }
                }
            }
            timerTickImpl();
        }

        protected abstract void timerTickImpl();

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

        @Nullable
        public World getWorld() {
            return world;
        }
    }


    public abstract class RoomInstance extends DRInstance<RoomType> {

        private final DoorInstance entrance;
        private final List<DoorInstance> exits = new ArrayList<>();
        private final List<TrapInstance> traps = new ArrayList<>();
        private final Set<Material> breakableBlocks = new HashSet<>();
        private final Set<Material> placeableBlocks = new HashSet<>();
        private final String schematicName;
        private final BlockVector size;
        private final List<PopulatorInstance> populators = new ArrayList<>();
        private SoftReference<Clipboard> clipboard = null;
        private CompletableFuture<Clipboard> futureClipboard;

        public RoomInstance(@NotNull String id, @NotNull YMLSection section) {
            super(id, RoomType.this);
            YMLSection tmp = section.loadSection("entrance");
            this.entrance = DoorTypeManager.getInstance().get(tmp.getString("type")).read(this, tmp);
            tmp = section.loadSection("exits");
            for (String key : tmp.getKeys(false)) {
                YMLSection sub = tmp.loadSection(key);
                exits.add(DoorTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
            }
            tmp = section.loadSection("populators");
            List<PopulatorInstance> pops = new ArrayList<>();
            for (String key : tmp.getKeys(false)) {
                YMLSection sub = tmp.loadSection(key);
                pops.add(PopulatorTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
            }
            this.addPopulators(pops);
            tmp = section.loadSection("traps");
            for (String key : tmp.getKeys(false)) {
                YMLSection sub = tmp.loadSection(key);
                traps.add(TrapTypeManager.getInstance().get(sub.getString("type")).read(this, sub));
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

        @NotNull
        public BlockVector getSize() {
            return size.clone();
        }

        @NotNull
        public Set<Material> getBreakableBlocks() {
            return Collections.unmodifiableSet(breakableBlocks);
        }

        @NotNull
        public Set<Material> getPlaceableBlocks() {
            return Collections.unmodifiableSet(placeableBlocks);
        }

        @NotNull
        public DoorInstance getEntrance() {
            return this.entrance;
        }

        @NotNull
        public List<DoorInstance> getExits() {
            return Collections.unmodifiableList(this.exits);
        }

        @NotNull
        public List<TrapInstance> getTraps() {
            return Collections.unmodifiableList(this.traps);
        }

        @NotNull
        public File getSchematic() {
            return new File(DeepDungeons.get().getDataFolder(), "schematics" + File.separator + getSchematicName());
        }

        @NotNull
        private String getSchematicName() {
            return this.schematicName;
        }

        @NotNull
        public CompletableFuture<Clipboard> getClipboard(boolean async) {
            Clipboard clip = clipboard == null ? null : clipboard.get();
            if (clip != null)
                return CompletableFuture.completedFuture(clip);
            if (futureClipboard != null)
                return futureClipboard; //TODO what a mess
            CompletableFuture<Clipboard> result = CompletableFuture.completedFuture(WorldEditUtility
                    .load(getSchematic(), DeepDungeons.get()));
            result.thenAccept(value -> this.clipboard = new SoftReference<>(value));
            this.futureClipboard = result;
            result.whenComplete((value, e) -> this.futureClipboard = null);
            return result;
        }

        @NotNull
        private CompletableFuture<EditSession> paste(@NotNull RoomHandler handler, boolean async) {
            return paste(handler.getLocation(), async);
        }

        @NotNull
        public CompletableFuture<EditSession> paste(@NotNull Location location, boolean async) {
            return getClipboard(async).thenCompose(value -> WorldEditUtility.paste(location, value, async,
                    DeepDungeons.get(), false, true, true, false));
        }

        @Contract("_->new")
        @NotNull
        public abstract RoomHandler createRoomHandler(@NotNull DungeonHandler dungeonHandler);

        public void addPopulator(@NotNull PopulatorInstance populator) {
            this.populators.add(populator);
            this.populators.sort(Comparator.comparingInt(p -> p.getPriority().ordinal()));
        }

        public void addPopulators(@NotNull Collection<PopulatorType.PopulatorInstance> populators) {
            this.populators.addAll(populators);
            this.populators.sort(Comparator.comparingInt(p -> p.getPriority().ordinal()));
        }

        private List<PopulatorInstance> getPopulators() {
            return Collections.unmodifiableList(populators);
        }

        public class RoomHandler implements MoveListener, InteractListener, InteractEntityListener,
                BlockPlaceListener, BlockBreakListener, AreaHolder {

            private final DungeonHandler dungeonHandler;
            private final DoorHandler entranceHandler;
            private final List<DoorHandler> exits = new ArrayList<>();
            private final List<TrapHandler> traps = new ArrayList<>();
            private final List<Entity> monsters = new ArrayList<>();
            private Location location = null;
            private BoundingBox boundingBox = null;
            private boolean firstEnter = false;

            public RoomHandler(@NotNull DungeonHandler dungeonHandler) {
                this.dungeonHandler = dungeonHandler;
                this.entranceHandler = getRoomInstance().getEntrance().createDoorHandler(this);
                for (DoorInstance exit : getRoomInstance().getExits())
                    this.exits.add(exit.createDoorHandler(this));
                for (TrapInstance trap : getRoomInstance().getTraps())
                    this.traps.add(trap.createTrapHandler(this));
            }

            @Contract(pure = true)
            @NotNull
            public final DoorHandler getEntrance() {
                return entranceHandler;
            }

            @Contract(pure = true)
            @NotNull
            public final List<DoorHandler> getExits() {
                return Collections.unmodifiableList(exits);
            }

            @Contract(pure = true)
            @NotNull
            public final List<TrapHandler> getTraps() {
                return Collections.unmodifiableList(traps);
            }

            @Contract(pure = true)
            @NotNull
            public CompletableFuture<EditSession> paste(boolean async) {
                return RoomInstance.this.paste(this, async);
            }

            @Contract(pure = true)
            @NotNull
            public final DungeonHandler getDungeonHandler() {
                return dungeonHandler;
            }

            @Contract(pure = true, value = "-> new")
            @NotNull
            public Location getLocation() {
                return location.clone();
            }

            @Contract(pure = true, value = "-> new")
            @NotNull
            public BoundingBox getBoundingBox() {
                return boundingBox.clone();
            }

            @Contract(pure = true)
            @NotNull
            public RoomInstance getRoomInstance() {
                return RoomInstance.this;
            }

            @Contract(pure = true)
            @NotNull
            public BlockVector getSize() {
                return getRoomInstance().getSize();
            }

            public void setupOffset(@NotNull Vector roomOffset) {
                if (this.location != null)
                    throw new IllegalStateException();
                this.location = getDungeonHandler().getLocation().add(roomOffset);
                this.boundingBox = BoundingBox.of(location.toVector(), location.toVector().add(getSize()));
                getEntrance().setupOffset();
                this.exits.forEach(DoorHandler::setupOffset);
                this.traps.forEach(TrapHandler::setupOffset);
            }

            public void onPlayerMove(@NotNull PlayerMoveEvent event) {
                if (this.entranceHandler.contains(event.getTo())) {
                    entranceHandler.onPlayerMove(event);
                    return;
                }
                for (DoorHandler exit : this.exits)
                    if (exit.contains(event.getTo())) {
                        exit.onPlayerMove(event);
                        break;
                    }
                for (TrapHandler trap : this.traps)
                    if (trap instanceof MoveListener iTrap)
                        iTrap.onPlayerMove(event);
            }

            @NotNull
            public World getWorld() {
                return this.dungeonHandler.getWorld();
            }

            public boolean contains(@NotNull Vector vector) {
                return this.boundingBox.contains(vector);
            }

            public boolean overlaps(@NotNull BoundingBox box) {
                return this.boundingBox.overlaps(box);
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
                //TODO event
                this.getRoomInstance().getPopulators().forEach(pop -> pop.populate(this, player));
                this.entranceHandler.onFirstPlayerEnter(player);
                this.exits.forEach(e -> e.onFirstPlayerEnter(player));
                this.traps.forEach(e -> e.onFirstPlayerEnter(player));
            }

            public void onBlockPlace(@NotNull BlockPlaceEvent event) {
                for (TrapHandler trap : this.traps)
                    if (trap instanceof BlockPlaceListener iTrap)
                        iTrap.onBlockPlace(event);
                if (!event.isCancelled() && !getRoomInstance().getPlaceableBlocks().contains(event.getBlock().getType()))
                    event.setCancelled(true);
            }

            public void onBlockBreak(@NotNull BlockBreakEvent event) {
                for (TrapHandler trap : this.traps)
                    if (trap instanceof BlockBreakListener iTrap)
                        iTrap.onBlockBreak(event);
                if (!event.isCancelled() && !getRoomInstance().getBreakableBlocks().contains(event.getBlock().getType()))
                    event.setCancelled(true);
            }

            public void onPlayerBucketFill(@NotNull PlayerBucketFillEvent event) {
                event.setCancelled(true);
            }

            public void onPlayerBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
                event.setCancelled(true);
            }

            @NotNull
            public List<Entity> getMonsters() {
                monsters.removeIf(entity -> !entity.isValid() || !overlaps(entity));
                return Collections.unmodifiableList(monsters);
            }

            public void addGuardians(@NotNull Collection<Entity> guardians) {
                this.monsters.addAll(guardians);
            }

            @Override
            public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
                for (TrapHandler trap : this.traps)
                    if (trap instanceof InteractEntityListener iTrap)
                        iTrap.onPlayerInteractEntity(event);
            }

            @Override
            public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
                for (TrapHandler trap : this.traps)
                    if (trap instanceof InteractListener iTrap)
                        iTrap.onPlayerInteract(event);
            }
        }
    }


}
