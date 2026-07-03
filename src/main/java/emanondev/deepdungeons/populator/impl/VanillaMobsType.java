package emanondev.deepdungeons.populator.impl;

import emanondev.core.YMLSection;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.MobPopulator;
import emanondev.deepdungeons.populator.APaperPopulatorType;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VanillaMobsType extends APaperPopulatorType {

    public VanillaMobsType() {
        super("vanillamobs");
    }

    @Override
    @NotNull
    public VanillaMobsInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new VanillaMobsInstance(room, sub);
    }

    @NotNull
    @Override
    public APopulatorBuilder getBuilder(@NotNull RoomType.RoomBuilder room) {
        return new VanillaMobsBuilder(room);
    }

    @Override
    @NotNull
    public VanillaMobsPaperBuilder getPaperBuilder() {
        return new VanillaMobsPaperBuilder();
    }


    public class VanillaMobsBuilder extends APopulatorBuilder {

        private final List<Location> offsets = new ArrayList<>();
        private EntityType type = EntityType.ZOMBIE;
        @Getter
        private int min = 1;
        @Getter
        private int max = 1;

        public VanillaMobsBuilder(@NotNull RoomType.RoomBuilder room) {
            super(room);
        }


        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            if (offsets.isEmpty())
                throw new Exception("Location not set");
            section.set("mobtype", type.name());
            section.set("min", min);
            section.set("max", max);
            List<String> offsetsString = new ArrayList<>();
            offsets.forEach(off -> offsetsString.add(Util.toStringNoWorld(off)));
            section.set("offsets", offsetsString);
        }

        public void toggleOffset(@NotNull Location location) {
            location = location.clone();
            location.setWorld(null);
            location.subtract(getRoomOffset());
            location.setX(location.getBlockX() + 0.5D);
            location.setY(location.getBlockY());
            location.setZ(location.getBlockZ() + 0.5D);
            Location finalLocation = location;
            if (!offsets.removeIf(loc -> CUtils.isEqual(loc, finalLocation)))
                offsets.add(location);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    Location loc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
                    loc.setYaw(event.getPlayer().getLocation().getYaw() + 180);
                    this.toggleOffset(loc);
                    this.getRoomBuilder().setupTools();
                }
                case 6 -> {
                    if (offsets.isEmpty()) {
                        CUtils.sendMsg(event.getPlayer(), "populatorbuilder.msg_uncompleted_settings");
                        return;
                    }
                    this.complete();
                    this.getRoomBuilder().setupTools();
                }
            }
        }

        @Override
        protected void setupToolsImpl(@NotNull PlayerInventory inv, @NotNull Player player) {
            inv.setItem(0, CUtils.createItem(player, Material.PAPER, "populatorbuilder.vanillamobs_info"));
            inv.setItem(1, CUtils.createItem(player, Material.STICK, offsets.size(), false, "populatorbuilder.vanillamobs_selector"));
            inv.setItem(6, CUtils.createItem(player, Material.LIME_DYE, "populatorbuilder.base_confirm"));
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            offsets.forEach(loc -> CUtils.markBlock(player, loc.toVector().add(getRoomOffset()).toBlockVector(), color));
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            createButtons(gui, player, () -> type, this::setEntityType, this::getMin, this::setMin, this::getMax, this::setMax);
        }

        public void setMin(int min) {
            if (min < 0)
                min = 0;
            if (min > 100)
                min = 100;
            if (min > max)
                this.max = min;
            this.min = min;
        }

        public void setMax(int max) {
            if (max < 1)
                max = 1;
            if (max > 100)
                max = 100;
            if (max < min)
                this.min = max;
            this.max = max;
        }

        @NotNull
        public EntityType getEntityType() {
            return type;
        }

        public void setEntityType(@NotNull EntityType type) {
            this.type = type;
        }
    }

    private static void createButtons(PagedMapGui gui, Player player, Supplier<EntityType> getMob, Consumer<EntityType> setMob,
                                      Supplier<Integer> getMin, Consumer<Integer> setMin,
                                      Supplier<Integer> getMax, Consumer<Integer> setMax) {
        gui.addButton(new ResearchFButton<>(gui, () ->
                CUtils.createItem(player, Material.SPAWNER, "mythicmob_guimobselector", "%type%",
                        (getMob.get().getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                getMob.get().getKey().toString().substring(10) : getMob.get().getKey().toString())),
                (String text, EntityType type) -> {
                    String[] split = text.split(" ");
                    for (String s : split) {
                        if (!(type.name().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))
                                || type.getKey().toString().contains(s.toLowerCase(Locale.ENGLISH))))
                            return false;
                    }
                    return true;
                },
                (InventoryClickEvent event, EntityType type) -> {
                    setMob.accept(type);
                    gui.open(player);
                    return false;
                },
                (EntityType type) ->
                        CUtils.createItem(player, Material.SPAWNER, "mythicmob_guimobitem", "%name%",type.name(),"%type%",
                                (type.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                        type.getKey().toString().substring(10) : type.getKey().toString())),
                () -> {
                    ArrayList<EntityType> list = new ArrayList<>(Arrays.asList(EntityType.values()));
                    list.removeIf((type) -> !type.isSpawnable() || !type.isAlive());
                    list.sort(Comparator.comparing(Enum::name));
                    return list;
                }));
        gui.addButton(new NumberEditorFButton<>(
                gui, 1, 1, 100, getMin, setMin, () ->
                CUtils.createItem(player, Material.REPEATER, getMin.get(), false, "mythicmob_guimobmin",
                        "%min%", String.valueOf(getMin.get()),
                        "%max%", String.valueOf(getMax.get())), true
        ));
        gui.addButton(new NumberEditorFButton<>(
                gui, 1, 1, 100, getMax, setMax, () ->
                CUtils.createItem(player, Material.REPEATER, getMax.get(), false, "mythicmob_guimobmax",
                        "%min%", String.valueOf(getMin.get()),
                        "%max%", String.valueOf(getMax.get())), true));
    }

    private class VanillaMobsPaperBuilder extends APaperPopulatorBuilder {

        private EntityType type = EntityType.ZOMBIE;
        @Getter
        private int min = 1;
        @Getter
        private int max = 1;

        @Override
        public boolean preserveContainer() {
            return false;
        }

        @Override
        @NotNull
        protected List<String> toItemLinesImpl() {
            List<String> list = new ArrayList<>();
            list.add("&9MobType:&6 " + type.name());
            list.add("&9Min:&6 " + min);
            list.add("&9Max:&6 " + max);
            return list;
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("mobtype", type.name());
            section.set("min", min);
            section.set("max", max);
            section.set("offsets", List.of(Util.toStringNoWorld(getOffset())));
        }

        @Override
        public void fromItemLinesImpl(@NotNull List<String> lines) {
            type = EntityType.valueOf(lines.getFirst().split(" ")[1]);
            min = Integer.parseInt(lines.get(1).split(" ")[1]);
            max = Integer.parseInt(lines.get(2).split(" ")[1]);
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            createButtons(gui, player, () -> type, this::setEntityType, this::getMin, this::setMin, this::getMax, this::setMax);
        }

        public void setMin(int min) {
            if (min < 0)
                min = 0;
            if (min > 100)
                min = 100;
            if (min > max)
                this.max = min;
            this.min = min;
        }

        public void setMax(int max) {
            if (max < 1)
                max = 1;
            if (max > 100)
                max = 100;
            if (max < min)
                this.min = max;
            this.max = max;
        }

        @NotNull
        public EntityType getEntityType() {
            return type;
        }

        public void setEntityType(@NotNull EntityType type) {
            this.type = type;
        }
    }

    private class VanillaMobsInstance extends APopulatorInstance implements MobPopulator {

        private final List<Location> offsets = new ArrayList<>();
        @Getter
        private final EntityType entityType;
        @Getter
        private final int min;
        @Getter
        private final int max;

        public VanillaMobsInstance(RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            entityType = section.getEntityType("mobtype", EntityType.ZOMBIE);
            min = section.getInt("min");
            max = section.getInt("max");
            section.getStringList("offsets", Collections.emptyList()).forEach(val -> offsets.add(Util.toLocationNoWorld(val)));
        }

        @NotNull
        @Override
        public Collection<Entity> spawnMobs(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
            List<Location> randomPick = new ArrayList<>(offsets.size());
            if (offsets.isEmpty())
                return Collections.emptyList();

            int amount = random.nextInt() % (max - min + 1) + min;
            List<Entity> entities = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                if (randomPick.isEmpty()) {
                    randomPick.addAll(offsets);
                    Collections.shuffle(randomPick, random);
                }
                Location location = CUtils.sum(handler.getLocation(), randomPick.removeLast());
                Entity entity = location.getWorld().spawnEntity(location, entityType, true);
                entity.setPersistent(true);
                if (entity.isValid())
                    entities.add(entity);
                else {
                    DeepDungeons.get().logIssue("Failed to spawn monster at &e" + location.getWorld() + " "
                            + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                    //TODO more info
                }
            }
            return entities;
        }

        @Override
        public boolean spawnGuardians() {
            return true;
        }
    }
}
