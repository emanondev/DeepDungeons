package emanondev.deepdungeons.door.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.room.RoomType;
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
    public @NotNull TimedType.TimedDoorInstance read(@NotNull RoomType.RoomInstance room, @NotNull YMLSection section) {
        return new TimedDoorInstance(room, section);
    }

    @Override
    public @NotNull TimedType.TimedDoorInstanceBuilder getBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
        return new TimedDoorInstanceBuilder(room);
    }

    public final class TimedDoorInstanceBuilder extends DoorInstanceBuilder {

        private long timeToUnlock = 0;//in seconds
        private boolean completedTimes = false;
        public TimedDoorInstanceBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("timeToUnlock", "" + (timeToUnlock));
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    PagedMapGui mapGui = new PagedMapGui(new DMessage(DeepDungeons.get(), getPlayer()).appendLang("doorbuilder.timed_door_gui_title"),
                            6, getPlayer(), null, DeepDungeons.get());

                    mapGui.addButton(new NumberEditorFButton<>(mapGui, 1L, 1L, 10000L, () -> timeToUnlock,
                            (time) -> timeToUnlock = Math.min(Math.max(1, time), 36000),
                            () -> new ItemBuilder(Material.REPEATER).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                            .append("<gold>Time: <yellow>" + UtilsString.getTimeStringSeconds(getPlayer(), timeToUnlock),"%value_raw%",""+timeToUnlock).newLine()
                                            .append("<blue>How much time until door opens?")
                                    )                                    .setGuiProperty().build(), true));
                    mapGui.open(event.getPlayer());
                }
                case 6 -> {
                    if (timeToUnlock != 0) {
                        completedTimes = true;
                        event.getPlayer().getInventory().setHeldItemSlot(0);
                        setupTools();
                    }
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            if (!completedTimes) {
                Player player = getPlayer();
                PlayerInventory inv = player.getInventory();
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.timed_door_info")).build());
                inv.setItem(1, new ItemBuilder(Material.STICK).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.timed_door_selector", "%value%",  UtilsString.getTimeStringSeconds(getPlayer(), timeToUnlock),"%value_raw%",""+timeToUnlock)).build());
                //TODO fare lang
                if (timeToUnlock != 0)
                    inv.setItem(6, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                            .appendLang("doorbuilder.timed_door_confirm")).build());//TODO fare i msg in language
                return;
            }
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
        }

    }

    public class TimedDoorInstance extends DoorInstance {

        private final long unlockTime;

        public TimedDoorInstance(@NotNull RoomType.RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            unlockTime = section.getLong("timeToUnlock", 60L);
        }

        @Override
        public @NotNull TimedType.TimedDoorInstance.TimeDoorHandler createDoorHandler(@NotNull RoomType.RoomInstance.RoomHandler roomHandler) {
            return new TimeDoorHandler(roomHandler);
        }

        public class TimeDoorHandler extends DoorHandler {

            private long timeAwaited;
            private boolean unlocked = false;
            private ItemDisplay item;
            private TextDisplay text;

            public TimeDoorHandler(RoomType.RoomInstance.@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            public void setupOffset() {
                super.setupOffset();
              /*  for (BlockVector vector : TimedDoorInstance.this.pressurePlates)
                    pressurePlates.add(this.getRoom().getLocation().add(vector).getBlock());

               */
            }

            @Override
            public boolean canUse(Player player) {
                if (!super.canUse(player))
                    return false;
                return unlocked;
            }

            @Override
            public void onFirstPlayerEnter(Player player) {
                //entities.addAll(getRoom().getMonsters());
                @NotNull World world = getRoom().getDungeonHandler().getWorld();
                Vector center = this.getBoundingBox().getCenter();
                item = (ItemDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() + 0.5, center.getZ())
                        .setDirection(getDoorFace().getOppositeFace().getDirection()), EntityType.ITEM_DISPLAY);
                item.setItemStack(new ItemStack(Material.CLOCK));
                Transformation tr = item.getTransformation();
                tr.getScale().mul(1.3F, 1.3F, 0.1F);
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
                        if (getRoom().getDungeonHandler().getState() != DungeonType.DungeonInstance.DungeonHandler.State.STARTED) {
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
                        text.setText(new DMessage(DeepDungeons.get(), null).appendLang("door.timed_info",//todo lang da fare
                                "%current%", String.valueOf((timeAwaited - now) / 1000 + 1), "%max%", String.valueOf((unlockTime))).toLegacy());
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }
        }
    }
}
