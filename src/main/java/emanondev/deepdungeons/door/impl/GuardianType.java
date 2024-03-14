package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GuardianType extends DoorType {
    public GuardianType() {
        super("guardian");
    }

    @Override
    public @NotNull GuardianInstance read(@NotNull RoomType.RoomInstance room, @NotNull YMLSection section) {
        return new GuardianInstance(room, section);
    }

    @Override
    public @NotNull GuardianInstanceBuilder getBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
        return new GuardianInstanceBuilder(room);
    }

    public final class GuardianInstanceBuilder extends DoorInstanceBuilder {

        public GuardianInstanceBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {

        }


        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {

        }

        @Override
        protected void setupToolsImpl() {
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {

        }

    }

    public class GuardianInstance extends DoorInstance {

        public GuardianInstance(@NotNull RoomType.RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
        }

        @Override
        public @NotNull DoorHandler createDoorHandler(@NotNull RoomType.RoomInstance.RoomHandler roomHandler) {
            return new GuardianHandler(roomHandler);
        }

        public class GuardianHandler extends DoorHandler {

            private final List<Entity> entities = new ArrayList<>();
            private ItemDisplay item;
            private TextDisplay text;

            public GuardianHandler(RoomType.RoomInstance.@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            @Override
            public boolean canUse(Player player) {
                boolean pr = super.canUse(player);
                if (!pr)
                    return false;
                if (!entities.isEmpty()) {
                    entities.removeIf(entity -> !entity.isValid() || !getRoom().overlaps(entity));
                }
                return entities.isEmpty();
            }

            @Override
            public void onFirstPlayerEnter(Player player) {
                entities.addAll(getRoom().getMonsters());
                @NotNull World world = getRoom().getDungeonHandler().getWorld();
                Vector center = this.getBoundingBox().getCenter();
                item = (ItemDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() + 0.5, center.getZ())
                        .setDirection(getDoorFace().getOppositeFace().getDirection()), EntityType.ITEM_DISPLAY);
                item.setItemStack(new ItemStack(Material.SKELETON_SKULL));
                Transformation tr = item.getTransformation();
                tr.getScale().mul(1.3F, 1.3F, 0.1F);
                item.setTransformation(tr);
                item.setBrightness(new Display.Brightness(15, 15));
                text = (TextDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() - 0.5, center.getZ())
                        .setDirection(getDoorFace().getDirection()), EntityType.TEXT_DISPLAY);
                text.setBrightness(new Display.Brightness(15, 15));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoom().getDungeonHandler().getState() != DungeonType.DungeonInstance.DungeonHandler.State.STARTED) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        entities.removeIf(entity -> !entity.isValid() || !getRoom().overlaps(entity));
                        if (entities.isEmpty()) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        //TODO it's not player language specific
                        text.setText(new DMessage(DeepDungeons.get(), null).appendLang("door.guardian_info",
                                "%value%", String.valueOf(entities.size())).toLegacy());
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }
        }

    }
}
