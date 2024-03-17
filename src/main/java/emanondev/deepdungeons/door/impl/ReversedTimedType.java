package emanondev.deepdungeons.door.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import emanondev.deepdungeons.room.RoomType.RoomInstanceBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ReversedTimedType extends DoorType {
    public ReversedTimedType() {
        super("reversed_timed");
    }

    @Override
    public @NotNull
    ReversedTimedDoorInstance read(@NotNull RoomInstance room, @NotNull YMLSection section) {
        return new ReversedTimedDoorInstance(room, section);
    }

    @Override
    public @NotNull
    ReversedTimedDoorInstanceBuilder getBuilder(@NotNull RoomInstanceBuilder room) {
        return new ReversedTimedDoorInstanceBuilder(room);
    }

    public final class ReversedTimedDoorInstanceBuilder extends DoorInstanceBuilder {

        private long timeToLock = 60;//in seconds
        private boolean completedTimes = false;
        private boolean locksOpenIfPassThrough = false;

        public ReversedTimedDoorInstanceBuilder(@NotNull RoomInstanceBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("timeToLock", timeToLock);
            section.set("locksOpenIfPassThrough", locksOpenIfPassThrough);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    MapGui mapGui = new MapGui(new DMessage(DeepDungeons.get(), getPlayer()).appendLang("doorbuilder.reversed_timed_door_gui_title"),
                            1, getPlayer(), null, DeepDungeons.get());

                    mapGui.setButton(4, new NumberEditorFButton<>(mapGui, 1L, 1L, 10000L, () -> timeToLock,
                            (time) -> timeToLock = Math.min(Math.max(1, time), 36000),
                            () -> new ItemBuilder(Material.REPEATER).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                    .appendLang("doorbuilder.timed_door_gui_item",
                                            "%value%", UtilsString.getTimeStringSeconds(getPlayer(), timeToLock),
                                            "%value_raw%", String.valueOf(timeToLock))
                            ).setGuiProperty().build(), true));
                    mapGui.open(event.getPlayer());
                }
                case 2 -> {
                    locksOpenIfPassThrough = !locksOpenIfPassThrough;
                    this.setupTools();
                }
                case 6 -> {
                    completedTimes = true;
                    event.getPlayer().getInventory().setHeldItemSlot(0);
                    setupTools();
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            if (!completedTimes) {
                Player player = getPlayer();
                PlayerInventory inv = player.getInventory();
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.reversed_timed_door_info")).build());
                inv.setItem(1, new ItemBuilder(Material.CLOCK).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.reversed_timed_lock_selector",
                                "%value%", String.valueOf(locksOpenIfPassThrough))).build());
                inv.setItem(1, new ItemBuilder(Material.CLOCK).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.reversed_timed_door_selector", "%value%", UtilsString.getTimeStringSeconds(getPlayer(), timeToLock), "%value_raw%", "" + timeToLock)).build());
                inv.setItem(6, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.reversed_timed_door_confirm")).build());
                return;
            }
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
        }

    }

    public class ReversedTimedDoorInstance extends DoorInstance {

        private final long unlockTime;
        private final boolean locksOpenIfPassThrough;

        public ReversedTimedDoorInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            unlockTime = section.getLong("timeToLock", 60L);
            locksOpenIfPassThrough = section.getBoolean("locksOpenIfPassThrough", false);
        }

        @Override
        public @NotNull
        ReversedTimeDoorHandler createDoorHandler(@NotNull RoomHandler roomHandler) {
            return new ReversedTimeDoorHandler(roomHandler);
        }

        public class ReversedTimeDoorHandler extends DoorHandler {

            private long timeWhenLock;
            private boolean unlocked = true;
            private ItemDisplay item;
            private TextDisplay text;
            private boolean passedThrough = false;

            public ReversedTimeDoorHandler(@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            public void setupOffset() {
                super.setupOffset();
            }

            @Override
            public boolean canUse(@NotNull Player player) {
                if (!super.canUse(player))
                    return false;
                return unlocked;
            }

            @Override
            public boolean teleportIn(@NotNull Player player) {
                if (super.teleportIn(player)) {
                    passedThrough = true;
                    return true;
                }
                return false;
            }

            @Override
            public void onFirstPlayerEnter(@NotNull Player player) {
                //entities.addAll(getRoom().getMonsters());
                World world = getRoom().getDungeonHandler().getWorld();
                Vector center = this.getBoundingBox().getCenter();
                item = (ItemDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() + 0.5, center.getZ())
                        .setDirection(getDoorFace().getOppositeFace().getDirection()), EntityType.ITEM_DISPLAY);
                item.setItemStack(new ItemStack(Material.CLOCK));
                Transformation tr = item.getTransformation();
                tr.getScale().mul(1F, 1F, 0.1F);
                item.setTransformation(tr);
                item.setBrightness(new Display.Brightness(15, 15));
                item.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                text = (TextDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() - 0.5, center.getZ())
                        .setDirection(getDoorFace().getDirection()), EntityType.TEXT_DISPLAY);
                text.setBrightness(new Display.Brightness(15, 15));
                timeWhenLock = System.currentTimeMillis() + unlockTime * 1000;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoom().getDungeonHandler().getState() != DungeonType.DungeonInstance.DungeonHandler.State.STARTED) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        if (locksOpenIfPassThrough && passedThrough) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        long now = System.currentTimeMillis();
                        if (timeWhenLock < now) {
                            unlocked = false;
                            text.setText(new DMessage(DeepDungeons.get(), null).appendLang("door.reversed_timed_closed_info").toLegacy());
                            item.setItemStack(new ItemStack(Material.BARRIER));
                            Transformation tr = item.getTransformation();
                            item.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                            tr.getScale().mul(1F, 1F, 1F);
                            item.setTransformation(tr);
                            //text.remove();
                            //item.remove();

                            this.cancel();
                            return;
                        }
                        //TODO it's not player language specific
                        text.setText(new DMessage(DeepDungeons.get(), null).appendLang("door.reversed_timed_open_info",
                                "%current%", UtilsString.getTimeStringSeconds(null, (timeWhenLock - now) / 1000 + 1),
                                "%max%", UtilsString.getTimeStringSeconds(null, unlockTime), "%current_raw%",
                                String.valueOf((timeWhenLock - now) / 1000 + 1), "%max_raw%", String.valueOf(unlockTime)).toLegacy());
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }
        }
    }
}
