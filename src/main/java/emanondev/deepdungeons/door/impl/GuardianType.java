package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
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

import java.util.*;

public class GuardianType extends DoorType {
    public GuardianType() {
        super("guardian");
    }

    @Override
    @NotNull
    public GuardianInstance read(@NotNull RoomInstance room, @NotNull YMLSection section) {
        return new GuardianInstance(room, section);
    }

    @Override
    @NotNull
    public GuardianBuilder getBuilder(@NotNull RoomBuilder room) {
        return new GuardianBuilder(room);
    }

    public final class GuardianBuilder extends DoorBuilder {

        private final HashSet<EntityType> entityTypes = new HashSet<>();
        private boolean completedConfiguration = false;

        public GuardianBuilder(@NotNull RoomBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.setEnumsAsStringList("filtered_types", this.entityTypes);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {

                    MapGui mapGui = new MapGui(CUtils.craftMsg(getPlayer(), "doorbuilder.guardian_door_guititle"),
                            1, getPlayer(), null, DeepDungeons.get());

                    mapGui.setButton(4, new ResearchFButton<>(mapGui,
                            () -> {
                                DMessage msg = CUtils.craftMsg(event.getPlayer(), "doorbuilder.guardian_door_guiselector_base",
                                        "%amount%", String.valueOf(this.entityTypes.isEmpty() ?
                                                EntityType.values().length : this.entityTypes.size()));
                                this.entityTypes.forEach((type) -> msg.newLine().appendLang("doorbuilder.guardian_door_guiselector_line", "%id%"));
                                return CUtils.emptyIBuilder(Material.ZOMBIE_HEAD, this.entityTypes.isEmpty() ?
                                                EntityType.values().length : this.entityTypes.size(), false)
                                        .setDescription(msg).build();
                            },
                            (String text, EntityType type) -> {
                                String[] split = text.split(" ");
                                for (String s : split)
                                    if (!(type.name().contains(s.toUpperCase(Locale.ENGLISH))))
                                        return false;
                                return true;
                            },
                            (evt, type) -> {
                                if (this.entityTypes.contains(type))
                                    this.entityTypes.remove(type);
                                else
                                    this.entityTypes.add(type);
                                return true;
                            },
                            (type) -> CUtils.createItem(getPlayer(), Material.BRICK, 1,
                                    this.entityTypes.isEmpty() || this.entityTypes.contains(type),
                                    "doorbuilder.guardian_door_guiselector_item", "%id%", type.name(),
                                    "%selected%", (this.entityTypes.isEmpty() || this.entityTypes.contains(type) ? "<green>true</green>" : "<red>false</red>")),

                            () -> {
                                List<EntityType> types = new ArrayList<>(Arrays.asList(EntityType.values()));
                                types.removeIf(type -> !type.isSpawnable() && !type.isAlive());
                                types.sort(Comparator.comparing(Enum::name));
                                return types;
                            }));
                    mapGui.open(event.getPlayer());
                }
                case 6 -> {
                    completedConfiguration = true;
                    event.getPlayer().getInventory().setHeldItemSlot(0);
                    setupTools();

                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            if (!completedConfiguration) {
                Player player = getPlayer();
                PlayerInventory inv = player.getInventory();
                CUtils.setSlot(player, 0, inv, Material.PAPER, "doorbuilder.guardian_entitytype_info");
                CUtils.setSlot(player, 1, inv, Material.ZOMBIE_HEAD, "doorbuilder.guardian_entitytype_selector",
                        "%value%", String.valueOf(this.entityTypes.size()));
                CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "doorbuilder.guardian_entitytype_confirm");
                return;
            }
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
        }

    }

    public class GuardianInstance extends DoorInstance {


        private final HashSet<EntityType> entityTypes = new HashSet<>();

        public GuardianInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            this.entityTypes.addAll(section.loadEntityTypeList("filtered_types", Collections.emptyList()));
        }

        @Override
        @NotNull
        public DoorHandler createDoorHandler(@NotNull RoomHandler roomHandler) {
            return new GuardianHandler(roomHandler);
        }

        public class GuardianHandler extends DoorHandler {

            private final List<Entity> entities = new ArrayList<>();
            private ItemDisplay item;
            private TextDisplay text;

            public GuardianHandler(@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            @Override
            public boolean canUse(@NotNull Player player) {
                boolean pr = super.canUse(player);
                if (!pr)
                    return false;
                if (!this.entities.isEmpty()) {
                    this.entities.removeIf(entity -> !entity.isValid() || !getRoomHandler().overlaps(entity));
                }
                return this.entities.isEmpty();
            }

            @Override
            public void onFirstPlayerEnter(@NotNull Player player) {
                this.entities.addAll(getRoomHandler().getMonsters());
                this.entities.removeIf(type -> !GuardianInstance.this.entityTypes.isEmpty() && !GuardianInstance.this.entityTypes.contains(type.getType()));
                World world = getRoomHandler().getDungeonHandler().getWorld();
                Vector center = this.getBoundingBox().getCenter();
                this.item = (ItemDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() + 0.5, center.getZ())
                        .setDirection(getDoorFace().getOppositeFace().getDirection()), EntityType.ITEM_DISPLAY);
                this.item.setItemStack(new ItemStack(Material.SKELETON_SKULL));
                Transformation tr = this.item.getTransformation();
                tr.getScale().mul(1.3F, 1.3F, 0.1F);
                this.item.setTransformation(tr);
                this.item.setBrightness(new Display.Brightness(15, 15));
                this.text = (TextDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() - 0.5, center.getZ())
                        .setDirection(getDoorFace().getDirection()), EntityType.TEXT_DISPLAY);
                this.text.setBrightness(new Display.Brightness(15, 15));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoomHandler().getDungeonHandler().getState() != DungeonHandler.State.STARTED) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        entities.removeIf(entity -> !entity.isValid() || !getRoomHandler().overlaps(entity));
                        if (entities.isEmpty()) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        //TODO it's not player language specific
                        text.setText(CUtils.craftMsg(null, "door.guardian_info",
                                "%value%", String.valueOf(entities.size())).toLegacy());
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }

        }

    }
}
