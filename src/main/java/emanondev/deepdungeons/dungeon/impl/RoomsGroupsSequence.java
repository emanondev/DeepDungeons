package emanondev.deepdungeons.dungeon.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.*;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.room.RoomInstanceManager;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RoomsGroupsSequence extends DungeonType {
    public RoomsGroupsSequence() {
        super("roomsgroupssequence");
    }

    @Override
    public @NotNull DungeonType.DungeonInstanceBuilder getBuilder(@NotNull String id, @NotNull Player player) {
        return new RoomsGroupsSequenceBuilder(id, player);
    }

    @Override
    protected @NotNull DungeonType.DungeonInstance readImpl(@NotNull String id, @NotNull YMLSection section) {
        return new RoomsGroupsSequenceInstance(id, section);
    }

    public class RoomsGroupsSequenceBuilder extends DungeonInstanceBuilder {

        private final List<RoomsGroupBuilder> groups = new ArrayList<>();

        public RoomsGroupsSequenceBuilder(@NotNull String id, @NotNull Player player) {
            super(id, player);
            groups.add(new RoomsGroupBuilder(1, 1));
            //groups.add(new RoomsGroup(1,1))
        }

        @Override
        protected void setupToolsImpl() {
            Player player = getPlayer();
            Inventory inv = player.getInventory();
            inv.setItem(0, new ItemBuilder(Material.CHISELED_STONE_BRICKS).setGuiProperty()
                    .setDescription(new DMessage(DeepDungeons.get(), player).append("<blue>Configure Rooms Groups")).build());

            inv.setItem(4, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                    .append("<green>Confirm Setup")).build());
        }

        boolean[] resetButtons = new boolean[1];

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            int heldSlot = event.getPlayer().getInventory().getHeldItemSlot();
            switch (heldSlot) {
                case 0 -> {
                    resetButtons[0] = true;
                    PagedMapGui gui = new PagedMapGui(new DMessage(DeepDungeons.get(), event.getPlayer()).append("<blue>Groups"),
                            6, event.getPlayer(), null, DeepDungeons.get()) {

                        @Override
                        public void updateInventory() {
                            if (resetButtons[0]) {
                                resetButtons[0] = false;
                                this.clearButtons();
                                for (int i = 0; i < groups.size(); i++) {
                                    RoomsGroupBuilder group = groups.get(i);
                                    int finalI = i;
                                    this.addButton(new FButton(this, () -> new ItemBuilder(Material.LEATHER_CHESTPLATE).setGuiProperty().setColor(group.color)
                                            .setAmount(Math.max(Math.min(100, group.getRooms().size()), 1))
                                            .setDescription(new DMessage(DeepDungeons.get(), event.getPlayer()).append("<gold>Group #<yellow>" + (finalI + 1))
                                                    .newLine().append("<blue>Selected rooms: <yellow>" + group.getRooms().size())
                                                    .newLine().append("<blue>Length: <yellow>" + group.minLength +
                                                            (group.minLength != group.maxLength ? "</yellow> to <yellow>" + group.maxLength : "")
                                                    ).newLine().newLine()
                                                    .append("<blue>[<white>Click Left/Right</white>] <white>Edit").newLine()
                                                    .append("<blue>[<white>Click Shift Left/Right</white>] <white>Move Left/Right")
                                                    .newLine().append("<blue>[<white>Press <key:key.swapOffhand></white>] <red>Delete"))

                                            .setGuiProperty().build(),
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
                                this.addButton(new FButton(this, () -> new ItemBuilder(Material.LIGHT_BLUE_DYE)
                                        .setDescription(new DMessage(DeepDungeons.get(), getPlayer()).append("<gold>Add a new Group"))
                                        .setGuiProperty().build(),
                                        (event) -> {
                                            groups.add(new RoomsGroupBuilder());
                                            resetButtons[0] = true;
                                            return true;
                                        }));
                            }
                            super.updateInventory();
                        }
                    };

                    gui.open(getPlayer());
                }
                case 4 -> {
                    for (RoomsGroupBuilder group : groups)
                        if (!group.isValid()) {
                            event.getPlayer().sendMessage("Message not implemented (setup incomplete)");
                            return;
                        }
                    this.getCompletableFuture().complete(this);
                }
            }
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            int i = 0;
            for (RoomsGroupBuilder group : groups) {
                i++;
                group.write(section.loadSection("roomgroups." + i));
            }
        }

        public class RoomsGroupBuilder {
            private final HashMap<String, Integer> rooms = new HashMap<>();
            private int minLength = 1;
            private int maxLength = 1;
            private final int MIN_LEN;
            private final int MAX_LEN;
            private final Color color = Color.fromRGB((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
            private int slot;

            public RoomsGroupBuilder() {
                this(0, Integer.MAX_VALUE);
            }

            public RoomsGroupBuilder(int minLimit, int maxLimit) {
                if (minLimit > maxLimit)
                    throw new IllegalArgumentException();
                this.MIN_LEN = Math.max(0, minLimit);
                this.MAX_LEN = Math.max(1, maxLimit);
            }

            public void setMinLength(int value) {
                minLength = Math.max(MIN_LEN, Math.min(MAX_LEN, value));
                if (minLength > maxLength)
                    maxLength = minLength;
            }

            public void setMaxLength(int value) {
                maxLength = Math.max(MIN_LEN, Math.min(MAX_LEN, value));
                if (minLength > maxLength)
                    minLength = maxLength;
            }

            /**
             * add room with a weight of 100
             *
             * @param roomId
             */
            public void setRoom(@NotNull String roomId) {
                rooms.put(roomId, 100);
            }

            /**
             * setting a zero or negative weight remove the item
             *
             * @param roomId
             * @param weight
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
                Player p = RoomsGroupsSequence.RoomsGroupsSequenceBuilder.this.getPlayer();
                this.slot = slot;
                PagedMapGui mapGui = new PagedMapGui(new DMessage(DeepDungeons.get(), getPlayer()).append("Group #" + slot),
                        6, getPlayer(), parent, DeepDungeons.get());
                mapGui.addButton(new ResearchFButton<>(mapGui,
                        () -> {
                            DMessage msg = new DMessage(DeepDungeons.get(), p)
                                    .append("<gold>Room selector").newLine().append("<blue>Selected rooms: <yellow>" + rooms.size());
                            rooms.forEach((id, weight) -> msg.newLine().append("<blue> - <yellow>" + id + " <blue>(weight <yellow>" + weight + "<blue>)"));
                            return new ItemBuilder(Material.BRICKS).setAmount(Math.max(Math.min(100, rooms.size()), 1))
                                    .setDescription(msg).build();
                        },
                        (String text, String roomId) -> {
                            RoomType.RoomInstance inst = RoomInstanceManager.getInstance().get(roomId);
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
                            RoomType.RoomInstance inst = RoomInstanceManager.getInstance().get(roomId);
                            return new ItemBuilder(Material.BRICK).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                            .append("<gold>Room ID: <yellow>" + roomId).newLine()
                                            .append("<blue>Room Type: <yellow>" + (inst == null ? "?" : inst.getType().getId())).newLine().append(
                                                    "<blue>Enabled? " + (rooms.containsKey(roomId) ? "<green>true" : "<red>false"))
                                    ).addEnchantment(Enchantment.DURABILITY, rooms.containsKey(roomId) ? 1 : 0)
                                    .setGuiProperty().build();
                        },
                        () -> RoomInstanceManager.getInstance().getIds()));
                if (MIN_LEN != MAX_LEN) {
                    mapGui.addButton(new NumberEditorFButton<>(mapGui, 1, 1, 10000, () -> minLength, this::setMinLength,
                            () -> new ItemBuilder(Material.REPEATER).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                            .append("<gold>Min: <yellow>" + minLength).newLine()
                                            .append("<blue>How many rooms at <u>least</u> from this group?")
                                    ).setAmount(Math.max(1, Math.min(100, minLength)))
                                    .setGuiProperty().build(), true));
                    mapGui.addButton(new NumberEditorFButton<>(mapGui, 1, 1, 10000, () -> maxLength, this::setMaxLength,
                            () -> new ItemBuilder(Material.REPEATER).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                            .append("<gold>Max: <yellow>" + maxLength).newLine()
                                            .append("<blue>How many rooms at <u>most</u> from this group?")).setAmount(Math.max(1, Math.min(100, maxLength)))
                                    .setGuiProperty().build(), true));
                }
                //TODO change weights
                return mapGui;
            }

            public Set<String> getRooms() {
                return Collections.unmodifiableSet(rooms.keySet());
            }

            public void write(YMLSection section) {
                rooms.forEach((id, value) -> section.set("room_weights.id", value));
                section.set("min_length", minLength);
                section.set("max_length", maxLength);
            }
        }
    }

    public class RoomsGroupsSequenceInstance extends DungeonInstance {

        public RoomsGroupsSequenceInstance(@NotNull String id, @NotNull YMLSection section) {
            super(id, section);
        }
    }


}
