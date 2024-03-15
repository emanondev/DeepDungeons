package emanondev.deepdungeons.door.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
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

        private final HashSet<EntityType> entityTypes = new HashSet<>();
        private boolean completedConfiguration = false;

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
                section.setEnumsAsStringList("filthered_types" , entityTypes);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {

                    PagedMapGui mapGui = new PagedMapGui(new DMessage(DeepDungeons.get(), getPlayer()).appendLang("doorbuilder.timed_door_gui_title"),
                            1, getPlayer(), null, DeepDungeons.get());

                    mapGui.setButton(4,new ResearchFButton<>(mapGui,
                            () -> {
                                DMessage msg = new DMessage(DeepDungeons.get(), event.getPlayer())
                                        .append("<gold>EntityType selector").newLine().append(
                                                "<blue>Selected EntityType: <yellow>" + (entityTypes.isEmpty()?EntityType.values().length:entityTypes.size()));
                                entityTypes.forEach((type) -> msg.newLine().append("<blue> - <yellow>" + type ));
                                return new ItemBuilder(Material.ZOMBIE_HEAD).setAmount(Math.max(Math.min(100, (entityTypes.isEmpty()?EntityType.values().length:entityTypes.size())), 1))
                                        .setDescription(msg).build();
                            },
                            (String text, EntityType type) -> {
                                String[] split = text.split(" ");
                                for (String s : split)
                                    if (!(type.name().contains(s.toLowerCase(Locale.ENGLISH))))
                                        return false;
                                return true;
                            },
                            (evt, type) -> {
                                if (entityTypes.contains(type))
                                    entityTypes.remove(type);
                                else
                                    entityTypes.add(type);
                                return true;
                            },
                            (type) -> new ItemBuilder(Material.BRICK).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                            .append("<gold>EntityType: <yellow>" + type.name()).newLine().append(
                                                    "<blue>Enabled? " + (entityTypes.isEmpty()||entityTypes.contains(type) ? "<green>true" : "<red>false"))
                                    ).addEnchantment(Enchantment.DURABILITY, (entityTypes.isEmpty()||entityTypes.contains(type) ? 1 : 0))
                                    .setGuiProperty().build(),
                            () -> {List<EntityType> types = new ArrayList<>(Arrays.asList(EntityType.values()));
                        types.removeIf(type->!type.isSpawnable());
                        types.sort(Comparator.comparing(Enum::name));
                        return types;
                    }));
                    mapGui.open(event.getPlayer());
                }
                case 6 -> {
                    if (entityTypes.size() > 0) {
                        completedConfiguration = true;
                        event.getPlayer().getInventory().setHeldItemSlot(0);
                        setupTools();
                    }
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            if (!completedConfiguration) {
                Player player = getPlayer();
                PlayerInventory inv = player.getInventory();
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.pressure_plates_info")).build());
                inv.setItem(1, new ItemBuilder(Material.STICK).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.pressure_plates_selector", "%value%", String.valueOf(entityTypes.size()))).build());
                if (entityTypes.size() > 0)
                    inv.setItem(6, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                            .appendLang("doorbuilder.pressure_plates_confirm")).build());
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

        public GuardianInstance(@NotNull RoomType.RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            entityTypes.addAll(section.loadEntityTypeList("filthered_types",Collections.emptyList()));
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
                entities.removeIf(type->!entityTypes.isEmpty()&&!entities.contains(type));
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
