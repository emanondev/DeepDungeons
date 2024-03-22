package emanondev.deepdungeons.door.impl;

import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
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

public class TimedType extends DoorType {
    public TimedType() {
        super("timed");
    }

    @Override
    @NotNull
    public TimedInstance read(@NotNull RoomInstance room, @NotNull YMLSection section) {
        return new TimedInstance(room, section);
    }

    @Override
    @NotNull
    public TimedBuilder getBuilder(@NotNull RoomBuilder room) {
        return new TimedBuilder(room);
    }

    public final class TimedBuilder extends DoorBuilder {

        private long timeToUnlock = 60;//in seconds
        private boolean completedTimes = false;

        public TimedBuilder(@NotNull RoomBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("timeToUnlock", timeToUnlock);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    MapGui mapGui = new MapGui(
                            CUtils.craftMsg(getPlayer(), "doorbuilder.timed_door_gui_title"),
                            1, getPlayer(), null, DeepDungeons.get());

                    mapGui.setButton(4, new NumberEditorFButton<>(mapGui, 1L, 1L, 10000L, () -> timeToUnlock,
                            (time) -> timeToUnlock = Math.min(Math.max(1, time), 36000),
                            () -> CUtils.createItem(getPlayer(), Material.REPEATER, "doorbuilder.timed_door_gui_item",
                                    "%value%", UtilsString.getTimeStringSeconds(getPlayer(), timeToUnlock),
                                    "%value_raw%", String.valueOf(timeToUnlock)), true));
                    mapGui.open(event.getPlayer());
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
                CUtils.setSlot(player, 0, inv, Material.PAPER, "doorbuilder.timed_door_info");
                CUtils.setSlot(player, 1, inv, Material.CLOCK, "doorbuilder.timed_door_selector",
                        "%value%", UtilsString.getTimeStringSeconds(getPlayer(), timeToUnlock), "%value_raw%", String.valueOf(timeToUnlock));
                CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "doorbuilder.timed_door_confirm");
                return;
            }
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
        }

    }

    public class TimedInstance extends DoorInstance {

        private final long unlockTime;

        public TimedInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            unlockTime = section.getLong("timeToUnlock", 60L);
        }

        @Override
        @NotNull
        public TimedHandler createDoorHandler(@NotNull RoomHandler roomHandler) {
            return new TimedHandler(roomHandler);
        }

        public class TimedHandler extends DoorHandler {

            private long timeAwaited;
            private boolean unlocked = false;
            private ItemDisplay item;
            private TextDisplay text;

            public TimedHandler(@NotNull RoomHandler roomHandler) {
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
            public void onFirstPlayerEnter(@NotNull Player player) {
                //entities.addAll(getRoom().getMonsters());
                World world = getRoomHandler().getDungeonHandler().getWorld();
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
                timeAwaited = System.currentTimeMillis() + unlockTime * 1000;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoomHandler().getDungeonHandler().getState() != DungeonHandler.State.STARTED) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        long now = System.currentTimeMillis();
                        if (timeAwaited < now) {
                            text.remove();
                            item.remove();
                            unlocked = true;
                            this.cancel();
                            return;
                        }
                        //TODO it's not player language specific
                        text.setText(CUtils.craftMsg(null, "door.timed_info",
                                "%current%", UtilsString.getTimeStringSeconds(null, (timeAwaited - now) / 1000 + 1),
                                "%max%", UtilsString.getTimeStringSeconds(null, unlockTime), "%current_raw%",
                                String.valueOf((timeAwaited - now) / 1000 + 1), "%max_raw%", String.valueOf(unlockTime)).toLegacy());
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }
        }
    }
}
