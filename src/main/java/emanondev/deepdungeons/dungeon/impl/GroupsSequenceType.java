package emanondev.deepdungeons.dungeon.impl;

import com.sk89q.worldedit.EditSession;
import emanondev.core.RandomItemContainer;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.*;
import emanondev.core.message.DMessage;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.door.DoorType.DoorInstance.DoorHandler;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.room.RoomInstanceManager;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GroupsSequenceType extends DungeonType {
    private static final int MAX_CHUNK_LOAD_PER_TICK = 5;

    public GroupsSequenceType() {
        super("roomsgroupssequence");
    }

    @Override
    @NotNull
    public DungeonBuilder getBuilder(@NotNull String id, @NotNull Player player) {
        return new GroupsSequenceBuilder(id, player);
    }

    @Override
    @NotNull
    protected DungeonInstance readImpl(@NotNull String id, @NotNull YMLSection section) {
        return new GroupsSequenceInstance(id, section);
    }

    public class GroupsSequenceBuilder extends DungeonBuilder {

        private final List<Group> groups = new ArrayList<>();
        boolean[] resetButtons = new boolean[1];

        public GroupsSequenceBuilder(@NotNull String id, @NotNull Player player) {
            super(id, player);
            groups.add(new Group(0, 1, 1));
            //groups.add(new RoomsGroup(1,1))
        }

        @Override
        protected void setupToolsImpl() {
            Player player = getPlayer();
            Inventory inv = player.getInventory();
            CUtils.setSlot(player, 0, inv, Material.PAPER, "dungeonbuilder.rgs_base_info");
            CUtils.setSlot(player, 1, inv, Material.CHISELED_STONE_BRICKS, "dungeonbuilder.rgs_base_gui");
            CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "dungeonbuilder.rgs_base_confirm");
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            int heldSlot = event.getPlayer().getInventory().getHeldItemSlot();
            switch (heldSlot) {
                case 1 -> {
                    resetButtons[0] = true;
                    PagedMapGui gui = new PagedMapGui(CUtils.craftMsg(event.getPlayer(), "dungeonbuilder.rgs_groupsgui_title"),
                            6, event.getPlayer(), null, DeepDungeons.get()) {

                        @Override
                        public void updateInventory() {
                            if (resetButtons[0]) {
                                resetButtons[0] = false;
                                this.clearButtons();
                                HashMap<Integer, GuiButton> buttons = new HashMap<>();
                                for (int i = 0; i < groups.size(); i++) {
                                    Group group = groups.get(i);
                                    int finalI = i;
                                    buttons.put(finalI, new FButton(this, () ->
                                            CUtils.createIBuilder(event.getPlayer(), Material.LEATHER_CHESTPLATE, group.getRooms().size(),
                                                    false, "dungeonbuilder.rgs_groupsgui_groupdesc",
                                                    "%index%", String.valueOf(finalI + 1),
                                                    "%size%", String.valueOf(group.getRooms().size()),
                                                    "%min_len%", String.valueOf(group.minLength),
                                                    "%max_len%", String.valueOf(group.maxLength)).setColor(group.color).build(),
                                            (event) -> switch (event.getClick()) {
                                                case LEFT, RIGHT -> {
                                                    group.createGui(finalI, this).open(event.getWhoClicked());
                                                    yield false;
                                                }
                                                case SHIFT_RIGHT -> {
                                                    if (finalI < groups.size() - 1) {
                                                        groups.add(finalI + 1, groups.remove(finalI));
                                                        resetButtons[0] = true;
                                                        yield true;
                                                    }
                                                    yield false;
                                                }
                                                case SHIFT_LEFT -> {
                                                    if (finalI > 1) {
                                                        groups.add(finalI - 1, groups.remove(finalI));
                                                        resetButtons[0] = true;
                                                        yield true;
                                                    }
                                                    yield false;
                                                }
                                                case SWAP_OFFHAND -> {
                                                    if (finalI >= 1) {
                                                        groups.remove(finalI);
                                                        resetButtons[0] = true;
                                                        yield true;
                                                    }
                                                    yield false;
                                                }
                                                default -> false;
                                            }));
                                }
                                buttons.put(groups.size(), new FButton(this, () ->
                                        CUtils.createItem(getPlayer(), Material.LIGHT_BLUE_DYE, "dungeonbuilder.rgs_groupsgui_groupadd"),
                                        (event) -> {
                                            int[] max = new int[]{0};
                                            groups.forEach(group -> max[0] = Math.max(max[0], group.createIndex));
                                            groups.add(new Group(max[0] + 1));
                                            resetButtons[0] = true;
                                            return true;
                                        }));
                                setButtons(buttons);
                            }
                            super.updateInventory();
                        }
                    };
                    gui.open(getPlayer());
                }
                case 6 -> {
                    for (Group group : groups)
                        if (!group.isValid()) {
                            CUtils.sendMsg(event.getPlayer(), "dungeonbuilder.rgs_msg_setup_incomplete");
                            return;
                        }
                    this.getCompletableFuture().complete(this);
                }
            }
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            int i = 0;
            for (Group group : groups) {
                i++;
                group.write(section.loadSection("groups." + i));
            }
        }

        public class Group {
            private final HashMap<String, Integer> rooms = new HashMap<>();
            private final int MIN_LEN;
            private final int MAX_LEN;
            private final Color color;

            private final int createIndex;
            private int minLength = 1;
            private int maxLength = 1;
            //private int slot;

            public Group(int createIndex) {
                this(createIndex, 0, Integer.MAX_VALUE);
            }

            public Group(int createIndex, int minLimit, int maxLimit) {
                if (minLimit > maxLimit)
                    throw new IllegalArgumentException();
                this.MIN_LEN = Math.max(0, minLimit);
                this.MAX_LEN = Math.max(1, maxLimit);
                this.createIndex = createIndex;
                this.color = CUtils.craftColorRainbow(createIndex);
            }

            public void setMinLength(int value) {
                minLength = Math.max(MIN_LEN, Math.min(MAX_LEN, value));
                if (minLength > maxLength)
                    maxLength = minLength;
            }

            public void setMaxLength(int value) {
                maxLength = Math.max(1, Math.min(MAX_LEN, value));
                if (minLength > maxLength)
                    minLength = maxLength;
            }

            /**
             * add room with a weight of 100
             */
            public void setRoom(@NotNull String roomId) {
                rooms.put(roomId, 100);
            }

            /**
             * setting a zero or negative weight remove the item
             */
            public void setRoom(@NotNull String roomId, int weight) {
                if (weight <= 0)
                    rooms.remove(roomId);
                rooms.put(roomId, weight);
            }

            public void removeRoom(@NotNull String roomId) {
                rooms.remove(roomId);
            }

            public int getRoomWeight(@NotNull String roomId) {
                return rooms.getOrDefault(roomId, 0);
            }

            public boolean isValid() {
                return !rooms.isEmpty();
            }

            public Gui createGui(int slot, @NotNull Gui parent) {
                Player p = GroupsSequenceBuilder.this.getPlayer();
                //this.slot = slot;
                PagedMapGui mapGui = new PagedMapGui(CUtils.craftMsg(p, "dungeonbuilder.rgs_groupgui_title",
                        "%index%", String.valueOf(slot + 1)),
                        6, getPlayer(), parent, DeepDungeons.get());
                mapGui.addButton(new ResearchFButton<>(mapGui,
                        () -> {
                            DMessage msg = CUtils.craftMsg(p, "dungeonbuilder.rgs_groupgui_selectorbase",
                                    "%size%", String.valueOf(rooms.size()));
                            rooms.forEach((id, weight) -> msg.newLine().appendLang("dungeonbuilder.rgs_groupgui_selectorroominfo",
                                    "%id%", id, "%weight%", String.valueOf(weight)));
                            return CUtils.emptyIBuilder(Material.BRICKS, rooms.size(), false)
                                    .setDescription(msg).build();
                        },
                        (String text, String roomId) -> {
                            RoomInstance inst = RoomInstanceManager.getInstance().get(roomId);
                            String[] split = text.split(" ");
                            for (String s : split)
                                if (!(roomId.contains(s.toLowerCase(Locale.ENGLISH)))
                                        && (inst == null || !inst.getType().getId().contains(s.toLowerCase(Locale.ENGLISH))))
                                    return false;
                            return true;
                        },
                        (event, roomId) -> {
                            if (rooms.containsKey(roomId))
                                removeRoom(roomId);
                            else
                                setRoom(roomId);
                            return true;
                        },
                        (roomId) -> {
                            RoomInstance inst = RoomInstanceManager.getInstance().get(roomId);
                            return CUtils.createItem(p, Material.BRICK, 1, rooms.containsKey(roomId), "dungeonbuilder.rgs_selectorgui_roominfo",
                                    "%id%", roomId, "%type%", (inst == null ? "?" : inst.getType().getId()),
                                    "%selected%", rooms.containsKey(roomId) ? "<green>true</green>" : "<red>false</red>");
                        },
                        () -> RoomInstanceManager.getInstance().getIds()));

                if (MIN_LEN != MAX_LEN) {
                    mapGui.addButton(new NumberEditorFButton<>(mapGui, 1, 1, 100, () -> minLength, this::setMinLength,
                            () -> CUtils.createItem(p, Material.REPEATER, minLength, false,
                                    "dungeonbuilder.rgs_groupgui_min", "%min%", String.valueOf(minLength)
                                    , "%max%", String.valueOf(maxLength)), true));
                    mapGui.addButton(new NumberEditorFButton<>(mapGui, 1, 1, 100, () -> maxLength, this::setMaxLength,
                            () -> CUtils.createItem(p, Material.REPEATER, maxLength, false,
                                    "dungeonbuilder.rgs_groupgui_max", "%min%", String.valueOf(minLength)
                                    , "%max%", String.valueOf(maxLength)), true));
                }
                mapGui.addButton(new FButton(mapGui,
                        () -> CUtils.createItem(p, Material.ANVIL, "dungeonbuilder.rgs_groupgui_weighteditor"),
                        (event) -> {
                            PagedMapGui weightGui = new PagedMapGui(
                                    CUtils.craftMsg(p, "dungeonbuilder.rgs_weightgui_title", "%index%", String.valueOf(slot + 1)),
                                    6, getPlayer(), mapGui, DeepDungeons.get());
                            for (String key : rooms.keySet()) {
                                weightGui.addButton(new NumberEditorFButton<>(mapGui, 10, 1, 10000, () ->
                                        rooms.get(key), (value) -> rooms.put(key, Math.max(1, value)),
                                        () -> {
                                            final int[] fullWeight = {0};
                                            rooms.values().forEach((value) -> fullWeight[0] += value);
                                            RoomInstance inst = RoomInstanceManager.getInstance().get(key);
                                            return CUtils.createItem(p, Material.BRICK, "dungeonbuilder.rgs_weightgui_roominfo",
                                                    "%id%", key,
                                                    "%type%", (inst == null ? "?" : inst.getType().getId()),
                                                    "%weight%", String.valueOf(rooms.get(key)),
                                                    "%full_weight%", String.valueOf(fullWeight[0]),
                                                    "%perc_weight%", UtilsString.formatOptional2Digit(rooms.get(key) * 100D / fullWeight[0]));
                                        }, true));
                            }
                            weightGui.open(p);
                            return false;
                        })


                );
                return mapGui;
            }

            @NotNull
            public Set<String> getRooms() {
                return Collections.unmodifiableSet(rooms.keySet());
            }

            public void write(@NotNull YMLSection section) {
                rooms.forEach((id, value) -> section.set("room_weights." + id, value));
                section.set("min_length", minLength);
                section.set("max_length", maxLength);
            }
        }
    }

    public class GroupsSequenceInstance extends DungeonInstance {

        private final List<RoomGroup> groups = new ArrayList<>();

        public GroupsSequenceInstance(@NotNull String id, @NotNull YMLSection section) {
            super(id, section);
            section.getKeys("groups").forEach((key) -> groups.add(new RoomGroup(section.loadSection("groups." + key))));
        }

        @Override
        @Contract(value = "_ -> new", pure = true)
        @NotNull
        public DungeonHandler createHandler(@Nullable World world) {
            return new GroupsSequenceHandler(world);
        }

        private static class RoomGroup {

            private final int min;
            private final int max;
            private final RandomItemContainer<String> rooms = new RandomItemContainer<>();


            public RoomGroup(@NotNull YMLSection section) {
                min = section.loadInteger("min_length", 1);
                max = section.loadInteger("max_length", 1);
                section.getKeys("room_weights").forEach((key) -> rooms.addItem(key, section.loadInteger("room_weights." + key, 100)));
            }
        }

        private class GroupsSequenceHandler extends DungeonHandler {
            private final static int ROOM_MARGIN = 5;
            private final static int DUNGEON_MARGIN = 16;

            private final Location location;
            private final DoorHandler start;
            private final List<RoomHandler> rooms = new ArrayList<>();
            private final BoundingBox boundingBox;
            private State state = State.LOADING;

            public GroupsSequenceHandler(@Nullable World world) {
                super();
                RoomHandler startRoom = RoomInstanceManager.getInstance().get(groups.get(0).rooms.getItem()).createRoomHandler(this);
                rooms.add(startRoom);
                this.start = startRoom.getEntrance();
                //rooms.add(start.getRoom());
                List<DoorHandler> deathEnds = new ArrayList<>(startRoom.getExits());
                for (int i = 1; i < groups.size(); i++) {
                    RoomGroup group = groups.get(i);
                    if (group.min <= 0 && Math.random() > 1D / group.max)
                        continue;
                    RoomHandler groupStart = RoomInstanceManager.getInstance().get(group.rooms.getItem()).createRoomHandler(this);
                    deathEnds.forEach((door) -> door.link(groupStart.getEntrance()));
                    deathEnds.clear();
                    rooms.add(groupStart);
                    generate(group, groupStart, deathEnds, 1);
                }
                //List<BlockVector> offsets = new ArrayList<>();
                List<BoundingBox> boxes = new ArrayList<>();
                for (RoomHandler room : rooms) {
                    BlockVector size = room.getSize();
                    BoundingBox roomBox = new BoundingBox(0D, 0D, 0D, size.getX() + 1, size.getY() + 1, size.getZ() + 1);
                    roomBox.expand(ROOM_MARGIN);
                    boolean added = false;
                    for (int i = 0; i < 10000; i++) {
                        for (int x = 0; x < i; x++) {
                            BoundingBox tmp = roomBox.clone().shift(x * 5, 0, i * 5);
                            boolean fine = true;
                            for (BoundingBox box : boxes) {
                                if (box.overlaps(tmp)) {
                                    fine = false;
                                    break;
                                }
                            }
                            if (fine) {
                                boxes.add(tmp.expand(-ROOM_MARGIN));
                                added = true;
                                break;
                            }
                        }
                        if (added)
                            break;
                        for (int z = 0; z <= i; z++) {
                            BoundingBox tmp = roomBox.clone().shift(i * 5, 0, z * 5);
                            boolean fine = true;
                            for (BoundingBox box : boxes) {
                                if (box.overlaps(tmp)) {
                                    fine = false;
                                    break;
                                }
                            }
                            if (fine) {
                                boxes.add(tmp.expand(-ROOM_MARGIN));
                                added = true;
                                break;
                            }
                        }
                        if (added)
                            break;
                    }
                    if (!added) {
                        throw new IllegalStateException();
                    }
                }
                BoundingBox boxArea = new BoundingBox();
                for (BoundingBox box : boxes) {
                    boxArea.union(box);
                    box.shift(DUNGEON_MARGIN, DUNGEON_MARGIN, DUNGEON_MARGIN);
                    box.expand(0, 0, 0, -0.0001, -0.0001, -0.0001);
                }
                boxArea.expand(DUNGEON_MARGIN);
                boxArea.shift(DUNGEON_MARGIN, DUNGEON_MARGIN, DUNGEON_MARGIN).expand(0, 0, 0, -0.0001, -0.0001, -0.0001);
                this.location = AreaManager.getInstance().findLocation(world, boxArea.clone(), this);
                for (int i = 0; i < rooms.size(); i++) {
                    rooms.get(i).setupOffset(boxes.get(i).getMin());
                }
                this.boundingBox = boxArea.shift(location);
                paste();
            }

            @Override
            @NotNull
            public List<RoomHandler> getRooms() {
                return Collections.unmodifiableList(rooms);
            }

            private void paste() {
                //TODO debug
                DeepDungeons.get().logInfo("Pasting &e" + this.getDungeonInstance().getId() + " &fat &e" + getWorld().getName() + " " + Util.toString(location.toVector().toBlockVector()).replace(";", " "));
                long before = System.currentTimeMillis();

                getLocation().getWorld().getNearbyEntities(getBoundingBox(), (e) -> !(e instanceof Player)).forEach(Entity::remove);
                CompletableFuture<EditSession> future = WorldEditUtility.pasteAir(getBoundingBox(), getLocation().getWorld(), true, DeepDungeons.get());
                List<CompletableFuture<EditSession>> pasting = new ArrayList<>();
                pasting.add(future);
                future.whenComplete((session, t) -> {
                    CompletableFuture<EditSession> removeEntity = new CompletableFuture<>();
                    new BukkitRunnable() {
                        private final List<Chunk> chunks = new ArrayList<>();
                        boolean firstRun = true;

                        @Override
                        public void run() {
                            if (firstRun) {
                                Vector min = getBoundingBox().getMin().multiply(1 / 16);
                                Vector max = getBoundingBox().getMax().multiply(1 / 16);
                                for (int x = min.getBlockX(); x <= max.getBlockX() + 1; x++)
                                    for (int z = min.getBlockZ(); z <= max.getBlockZ() + 1; z++) {
                                        chunks.add(getWorld().getChunkAt(x, z));
                                    }
                                firstRun = false;
                            }
                            int counter = 0;
                            List<Chunk> toRemove = new ArrayList<>();
                            for (Chunk chunk : chunks) {
                                toRemove.add(chunk);
                                if (!chunk.isLoaded()) {
                                    counter++;
                                    chunk.load();
                                }
                                for (Entity en : chunk.getEntities())
                                    if (!(en instanceof Player))
                                        en.remove();
                                if (counter >= MAX_CHUNK_LOAD_PER_TICK)
                                    break;
                            }
                            chunks.removeAll(toRemove);
                            if (chunks.isEmpty()) {
                                removeEntity.complete(null);
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(DeepDungeons.get(), 1L, 1L);
                    pasting.add(removeEntity);
                });
                for (RoomHandler room : rooms) {
                    pasting.get(pasting.size() - 1).whenComplete((session, t) -> pasting.add(room.paste(true)));
                }
                pasting.get(pasting.size() - 1).thenAccept((session) -> {
                    this.state = State.READY;
                    AreaManager.getInstance().flagReady(this);
                    //TODO debug
                    DeepDungeons.get().logInfo("Pasted &e" + this.getDungeonInstance().getId() + " &fat &e" + getWorld().getName() + " " + Util.toString(location.toVector().toBlockVector()).replace(";", " ") + "&f took &e" + (System.currentTimeMillis() - before) + " &fms");
                });
            }

            @Override
            @Contract(pure = true)
            @NotNull
            public DoorHandler getEntrance() {
                return start;
            }

            @Override
            @Contract(value = "-> new", pure = true)
            @NotNull
            public Location getLocation() {
                return location.clone();
            }

            @Override
            @Contract(value = "-> new", pure = true)
            @NotNull
            public BoundingBox getBoundingBox() {
                return boundingBox;
            }

            @Override
            @NotNull
            public State getState() {
                return this.state;
            }

            @Override
            @NotNull
            public World getWorld() {
                return location.getWorld();
            }

            @Override
            public boolean contains(@NotNull Vector vector) {
                return boundingBox.contains(vector);
            }

            @Override
            public boolean overlaps(@NotNull BoundingBox box) {
                return boundingBox.overlaps(box);
            }

            @Override
            protected void startImpl(@NotNull PartyManager.Party party) {
                state = State.STARTED;
            }

            @Override
            public void flagCompleted() {
                state = State.COMPLETED;
            }

            private void generate(RoomGroup group, RoomHandler groupStart, List<DoorHandler> deathEnds, int depth) {
                if (depth >= group.max) {
                    deathEnds.addAll(groupStart.getExits());
                    return;
                }
                for (DoorHandler exit : groupStart.getExits()) {
                    if ((group.min <= depth && Math.random() <= 1D / (group.max - depth))) {
                        deathEnds.add(exit);
                        continue;
                    }
                    RoomHandler next = RoomInstanceManager.getInstance().get(group.rooms.getItem()).createRoomHandler(this);
                    rooms.add(next);
                    exit.link(next.getEntrance());
                    generate(group, next, deathEnds, depth + 1);
                }
            }

        }

    }


}
