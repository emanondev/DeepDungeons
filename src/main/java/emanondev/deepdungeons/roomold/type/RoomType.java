package emanondev.deepdungeons.roomold.type;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import emanondev.core.ItemBuilder;
import emanondev.core.MessageBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.FButton;
import emanondev.core.gui.Gui;
import emanondev.core.gui.PagedMapGui;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Offset;
import emanondev.deepdungeons.mob.MobManager;
import emanondev.deepdungeons.mob.MobProvider;
import emanondev.deepdungeons.reward.RewardManager;
import emanondev.deepdungeons.reward.RewardProvider;
import emanondev.deepdungeons.roomold.M;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class RoomType {

    private static final DeepDungeons PLUGIN = DeepDungeons.get();

    public boolean hasEntrace() {
        return true;
    }

    public final boolean hasExits() {
        return maxExits() > 0;
    }

    public int maxExits() {
        return Integer.MAX_VALUE;
    }

    public abstract String getTypeName();

    public abstract RoomBuilder getBuilder(Player p);

    public abstract RoomConfiguration createRoom(YMLSection section);

    public class RoomBuilder implements Listener {

        private final UUID builder;
        private int phase = 0;
        private World w;
        private BlockVector loc1;
        private BlockVector loc2;
        private Offset entraceDoor;
        private HashSet<Offset> exitDoors = new HashSet<>();

        public RoomBuilder(@NotNull Player builder) {
            this.builder = builder.getUniqueId();
        }

        public static void next(Player p) {
            if (!rooms.containsKey(p.getUniqueId()))
                throw new IllegalArgumentException();
            rooms.get(p.getUniqueId()).next();
        }

        public static void abort(Player p) {
            if (!rooms.containsKey(p.getUniqueId()))
                throw new IllegalArgumentException();
            rooms.get(p.getUniqueId()).abort();
        }

        public Player getPlayer() {
            return Bukkit.getPlayer(builder);
        }

        private void onStart() {
            M.BUILDER_START(getPlayer());
            advancePhase();
        }

        protected void incPhase() {
            phase++;
        }

        private void onNextAreaSelected(String[] args) {
            try {
                Region sel = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(getPlayer()))
                        .getSelection(BukkitAdapter.adapt(getPlayer().getWorld()));
                if (sel.getMaximumPoint().getBlockX() - sel.getMinimumPoint().getBlockX() < 5
                        || sel.getMaximumPoint().getBlockY() - sel.getMinimumPoint().getBlockY() < 5
                        || sel.getMaximumPoint().getBlockZ() - sel.getMinimumPoint().getBlockZ() < 5) {
                    new MessageBuilder(PLUGIN, getPlayer())
                            .addFullComponentTranslation("command.DungeonRoom.next.error.small_region",
                                    "&4[&cDungeonRoom&4] &cSelected area is too small",
                                    Arrays.asList("&6The selected area must be greater than 5x5x5", "",
                                            "&9Use worldedit axe &e//wand", "&9or &e//pos1 &9and &e//pos2"),
                                    null, null)
                            .addText("\n")
                            .addFullComponentTranslation("command.DungeonRoom.next.step1.message",
                                    "&9[&bDungeonRoom&9] &bStep1: Seleziona l'area della stanza",
                                    Arrays.asList("&6Clicca per eseguire &e/dungeonroom next",
                                            "&9Seleziona l'area della stanza con worldedit", "",
                                            "&9Includi le pareti e ricorda che le porte",
                                            "&9 per procedere nelle stanze successive",
                                            "&9 si troveranno solo sui bordi della stanza", "&9 e non al suo interno"),
                                    "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                            .send();
                    return;
                }
                w = this.getPlayer().getWorld();
                loc1 = new BlockVector(sel.getMinimumPoint().getX(), sel.getMinimumPoint().getY(),
                        sel.getMinimumPoint().getZ());
                loc2 = new BlockVector(sel.getMaximumPoint().getX(), sel.getMaximumPoint().getY(),
                        sel.getMaximumPoint().getZ());

                phase++;
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.next.step1.success",
                                "&9[&bDungeonRoom&9] &eStep1: Area selezionata",
                                Arrays.asList("&9Mondo: &e%world%", "&9x: &e%x_min% %x_max%", "&9y: &e%y_min% %y_max%",
                                        "&9z: &e%z_min% %z_max%"),
                                null, null, "%world%", w.getName(), "%x_min%", String.valueOf(loc1.getBlockX()),
                                "%y_min%", String.valueOf(loc1.getBlockY()), "%z_min%",
                                String.valueOf(loc1.getBlockZ()), "%x_max%", String.valueOf(loc2.getBlockX()),
                                "%y_max%", String.valueOf(loc2.getBlockY()), "%z_max%",
                                String.valueOf(loc2.getBlockZ()))
                        .addText("\n")
                        .addFullComponentTranslation("command.DungeonRoom.next.step2.message",
                                "&9[&bDungeonRoom&9] &bStep2: Seleziona l'ingresso",
                                Arrays.asList("&6Clicca per eseguire &e/dungeonroom next",
                                        "&9> Posiziona un blocco di smeraldo sulla base dell'ingresso",
                                        "&9> Riposizionalo per correggere l'ingresso",
                                        "&9> Riposizionalo 2 volte nello stesso punto per rimuovere l'ingresso", "",
                                        "&9Puoi procedere allo step successivo senza selezionare",
                                        "&9l'ingresso, in tal caso la stanza sarà una stanza d'inizio dungeon"),
                                "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                        .send();
                // animation
                displayRoomBorders(Material.RED_STAINED_GLASS);
                //

            } catch (IncompleteRegionException e) {
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.next.error.undefined_region",
                                "&4[&cDungeonRoom&4] &cSelected area is invalid",
                                Arrays.asList("&6Be sure to select the worldedit area", "",
                                        "&9Use worldedit axe &e//wand", "&9or &e//pos1 &9and &e//pos2"),
                                null, null)
                        .addText("\n")
                        .addFullComponentTranslation("command.DungeonRoom.next.step1.message",
                                "&9[&bDungeonRoom&9] &bStep1: Seleziona l'area della stanza",
                                Arrays.asList("&6Clicca per eseguire &e/dungeonroom next",
                                        "&9Seleziona l'area della stanza con worldedit", "",
                                        "&9Includi le pareti e ricorda che le porte",
                                        "&9 per procedere nelle stanze successive",
                                        "&9 si troveranno solo sui bordi della stanza", "&9 e non al suo interno"),
                                "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                        .send();
                return;
            }
        }

        public void onNext(String[] args) {
            switch (phase) {
                case 1:
                    onNextAreaSelected(args);
                    return;
                case 2: {

                    MessageBuilder mb = new MessageBuilder(PLUGIN, getPlayer());
                    if (entraceDoor == null)
                        mb.addFullComponentTranslation("command.DungeonRoom.next.step2.no_door",
                                "&9[&bDungeonRoom&9] &eStep2: Skip",
                                Arrays.asList("&9Non hai selezionato una porta, la stanza sarà un'ingresso per dungeon"), null,
                                null);
                    else
                        mb.addFullComponentTranslation("command.DungeonRoom.next.step2.success",
                                "&9[&bDungeonRoom&9] &eStep2: You did set the room entrace",
                                Arrays.asList("&9Hai selezionato la porta la stanza",
                                        "&9con un offset di %x% %y% %z% rispetto alla stanza"),
                                null, null, "%x%", String.valueOf(entraceDoor.getOffset().getBlockX()), "%y%",
                                String.valueOf(entraceDoor.getOffset().getBlockY()), "%z%",
                                String.valueOf(entraceDoor.getOffset().getBlockZ()));
                    mb.addText("\n").addFullComponentTranslation("command.DungeonRoom.next.step3.message",
                            "&9[&bDungeonRoom&9] &bStep3: Seleziona le uscite",
                            Arrays.asList("&6Clicca per eseguire &e/dungeonroom next",
                                    "&9> Posiziona un blocco di redstone sulla base dell'uscita",
                                    "&9> Riposizionalo per aggiungere un'uscita",
                                    "&9> Riposizionalo 2 volte nello stesso punto per rimuovere l'uscita", "",
                                    "&9Puoi procedere allo step successivo senza selezionare uscite",
                                    "&9ma solo se è presente un'ingresso, in tal caso la", "&9stanza sarà la fine del dungeon"),
                            "/dungeonroom next", ClickEvent.Action.RUN_COMMAND).send();

                    phase++;
                    displayDoors();
                    return;
                }
                case 3: {
                    if (entraceDoor == null && exitDoors.isEmpty()) {
                        new MessageBuilder(PLUGIN, getPlayer())
                                .addFullComponentTranslation("command.DungeonRoom.next.step3.no_doors",
                                        "&4[&cDungeonRoom&4] &cYou need at least an exit door",
                                        Arrays.asList("&9The room must have an entrace or an exit"), null, null)
                                .send();
                        return;
                    }
                    MessageBuilder mb = new MessageBuilder(PLUGIN, getPlayer());
                    if (exitDoors.isEmpty())
                        mb.addFullComponentTranslation("command.DungeonRoom.next.step3.no_exits",
                                "&9[&bDungeonRoom&9] &eStep3: No exits, room is a dungeon end",
                                Arrays.asList("&9Non hai selezionato uscite", "&9la stanza è il termine di un dungeon"), null,
                                null);
                    else
                        mb.addFullComponentTranslation("command.DungeonRoom.next.step3.success",
                                "&9[&bDungeonRoom&9] &eStep3: Hai impostato le uscite della stanza",
                                Arrays.asList("&9Hai selezionato uscite"), null, null);
                    mb.addText("\n")
                            .addFullComponentTranslation("command.DungeonRoom.next.step4.message",
                                    "&9[&bDungeonRoom&9] &bStep4: Mobs",
                                    Arrays.asList("&6Clicca per eseguire &e/dungeonroom next",
                                            "&9> Posiziona un armor stand per popolare di",
                                            "&9 mob in una determinata zona della stanza",
                                            "&9> Rimuovi l'armor stand in caso di ripensamento", "",
                                            "&9Puoi procedere allo step successivo senza aggiungere mob"),
                                    "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                            .send();
                    phase++;
                    displayDoors();
                    return;
                }
                case 4: {

                    new MessageBuilder(PLUGIN, getPlayer())
                            .addFullComponentTranslation("command.DungeonRoom.next.step4.success",
                                    "&9[&bDungeonRoom&9] &eStep4: Mobs impostati", (List<String>) null, null, null)
                            .addText("\n")
                            .addFullComponentTranslation("command.DungeonRoom.next.step5.message",
                                    "&9[&bDungeonRoom&9] &bStep5: Seleziona i Reward",
                                    Arrays.asList("&6Clicca per eseguire &e/dungeonroom next",
                                            "&9> Clicca sui contenitori con un lingotto d'oro", "&9> Per aggiungere dei premi",
                                            "", "&9Puoi procedere allo step successivo aggiungere premi"),
                                    "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                            .send();
                    phase++;
                    return;
                }
                default:
                    getPlayer().sendMessage("§cNon implementato");
            }
        }

        private void displayRoomBorders(Material type) {
            final HashSet<Block> coll = new HashSet<>();
            for (int x = loc1.getBlockX(); x <= loc2.getBlockX(); x++)
                for (int y = loc1.getBlockY(); y <= loc2.getBlockY(); y++)
                    for (int z = loc1.getBlockZ(); z <= loc2.getBlockZ(); z++)
                        if (x == loc1.getBlockX() || x == loc2.getBlockX() || y == loc1.getBlockY() || y == loc2.getBlockY()
                                || z == loc1.getBlockZ() || z == loc2.getBlockZ())
                            coll.add(w.getBlockAt(x, y, z));
            new BukkitRunnable() {
                private int counter = 0;

                @SuppressWarnings("deprecation")
                public void run() {
                    counter++;
                    Player p = getPlayer();
                    if (p == null) {
                        this.cancel();
                        return;
                    }
                    if (counter > 5) {
                        this.cancel();
                        for (Block b : coll)
                            getPlayer().sendBlockChange(b.getLocation(), b.getBlockData());
                        return;
                    }
                    if (counter % 2 == 0)
                        for (Block b : coll)
                            if ((b.getX() + b.getY() + b.getZ()) % 2 == 0)
                                p.sendBlockChange(b.getLocation(), type, (byte) 0);
                            else
                                p.sendBlockChange(b.getLocation(), b.getBlockData());
                    else
                        for (Block b : coll)
                            if ((b.getX() + b.getY() + b.getZ()) % 2 != 0)
                                p.sendBlockChange(b.getLocation(), type, (byte) 0);
                            else
                                p.sendBlockChange(b.getLocation(), b.getBlockData());
                }
            }.runTaskTimer(PLUGIN, 0L, 15L);
        }

        private boolean checkDoorLocation(Block b) {
            if (!isInside(b)) {
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.door.error.not_inside",
                                "&4[&cDungeonRoom&4] &cSelected place is not inside of the room",
                                Arrays.asList("&9The door must be on room walls (inside)"), null, null)
                        .send();
                return false;
            }

            if (b.getX() != loc1.getBlockX() && b.getX() != loc2.getBlockX() && b.getZ() != loc1.getBlockZ()
                    && b.getZ() != loc2.getBlockZ()) {
                // not a wall
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.door.error.not_wall",
                                "&4[&cDungeonRoom&4] &cSelected place is not a wall of the room",
                                Arrays.asList("&9The door must be on room walls"), null, null)
                        .send();
                return false;
            }
            if (b.getY() <= loc1.getBlockY() || b.getY() >= loc2.getBlockY() - 1) {
                // y not right
                new MessageBuilder(PLUGIN, getPlayer()).addFullComponentTranslation(
                        "command.DungeonRoom.door.error.wrong_y",
                        "&4[&cDungeonRoom&4] &cSelected place is too low or too high",
                        Arrays.asList("&9Block y must be at least over the", "&9 floor and 3 blocks under the roof"), null,
                        null).send();
                return false;
            }
            int wallCounter = 0;
            try {
                if (checkCorner(b.getZ(), loc1.getBlockZ()))
                    wallCounter++;
                if (checkCorner(b.getX(), loc1.getBlockX()))
                    wallCounter++;
                if (checkCorner(b.getZ(), loc2.getBlockZ()))
                    wallCounter++;
                if (checkCorner(b.getX(), loc2.getBlockX()))
                    wallCounter++;
            } catch (IllegalArgumentException e) {
                // too close to corner
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.door.error.corner_too_close",
                                "&4[&cDungeonRoom&4] &cSelected place is too close to corner",
                                Arrays.asList("&9The door must be not too close", "&9 to corners"), null, null)
                        .send();
                return false;
            }
            if (wallCounter != 1) {
                // not or wall or on corner
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.door.error.on_corner",
                                "&4[&cDungeonRoom&4] &cSelected place is too low or too high",
                                Arrays.asList("&9The door can't be not on corners"), null, null)
                        .send();
                return false;
            }
            return true;
        }

        public void setPlaceRewardProvider(ArmorStand stand) {
            // switch (phase) {
            // case 3:{
            // TODO gui to select provider
            Gui gui = new PagedMapGui("&9Select a Reward provider", 6, getPlayer(), null, DeepDungeons.get());
            for (RewardProvider provider : DeepDungeons.get().getRewardManager().getProviders())
                gui.addButton(new FButton(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                                Arrays.asList("&6" + provider.getId(), "&7[&fClick&7] &9Any &7> &9Select this Reward Provider"))
                        .build(), (e) -> {
                    provider.setupGui(getPlayer(), stand).open(getPlayer());
                    return false;
                }));
            gui.open(getPlayer());
            // }
            // }
        }

        public void editPlaceRewardProvider(ArmorStand stand, RewardProvider provider) {
            if (provider == null)
                throw new NullPointerException();
            provider.setupGui(getPlayer(), stand).open(getPlayer());
        }

        public void editPlaceMobProvider(ArmorStand stand, MobProvider provider) {
            if (provider == null)
                throw new NullPointerException();
            provider.setupGui(getPlayer(), stand).open(getPlayer());
        }

        public void setPlaceMobProvider(ArmorStand stand) {
            // switch (phase) {
            // case 3:{
            // TODO gui to select provider
            Gui gui = new PagedMapGui("&9Select a Mob provider", 6, getPlayer(), null, DeepDungeons.get());
            for (MobProvider provider : DeepDungeons.get().getMobManager().getProviders())
                gui.addButton(new FButton(gui, () -> new ItemBuilder(Material.ZOMBIE_HEAD).setDescription(
                                Arrays.asList("&6" + provider.getId(), "&7[&fClick&7] &9Any &7> &9Select this Mob Provider"))
                        .build(), (e) -> {
                    provider.setupGui(getPlayer(), stand).open(getPlayer());
                    return false;
                }));
            gui.open(getPlayer());
            // }
            // }
        }

        public void setEntraceDoor(Block b) {
            // phase = 1
            if (!checkDoorLocation(b))
                return;
            BlockFace dir = null;
            for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST))
                if (!isInside(b.getRelative(face))) {
                    dir = face.getOppositeFace();
                    break;
                }
            Offset door = new Offset(b.getLocation().toVector().subtract(loc1).toBlockVector(), dir);
            if (entraceDoor == null) {
                entraceDoor = door;
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.entrace.set",
                                Arrays.asList("&2[&aDungeonRoom&2] &eHai impostato l'entrata"),
                                Arrays.asList("&6Clicca per eseguire &e/dungeonroom next", "",
                                        "&9Ripiazza il blocco per annullare", "&9Piazzalo altrove per sostituire"),
                                "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                        .send();
                displayFakeDoorBlocks(Material.EMERALD_BLOCK, entraceDoor);
                return;
            }
            if (entraceDoor.equals(door)) {
                entraceDoor = null;
                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.entrace.unset",
                                Arrays.asList("&2[&aDungeonRoom&2] &eHai rimosso l'entrata"),
                                Arrays.asList("&6Clicca per eseguire &e/dungeonroom next", "",
                                        "&9Ripiazza il blocco per annullare", "&9Piazzalo altrove per sostituire"),
                                "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                        .send();
                displayFakeDoorBlocks(Material.BLACK_STAINED_GLASS, door);
                return;
            }
            new MessageBuilder(PLUGIN, getPlayer())
                    .addFullComponentTranslation("command.DungeonRoom.entrace.replace",
                            Arrays.asList("&2[&aDungeonRoom&2] &eHai cambiato l'entrata"),
                            Arrays.asList("&6Clicca per eseguire &e/dungeonroom next", "",
                                    "&9Ripiazza il blocco per annullare", "&9Piazzalo altrove per sostituire"),
                            "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                    .send();
            entraceDoor = door;
            displayFakeDoorBlocks(Material.EMERALD_BLOCK, entraceDoor);
        }

        public void setExitDoor(Block b) {
            // phase = 2
            if (!checkDoorLocation(b))
                return;
            BlockFace dir = null;
            for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST))
                if (!isInside(b.getRelative(face))) {
                    dir = face;
                    break;
                }
            Offset door = new Offset(b.getLocation().toVector().subtract(loc1).toBlockVector(), dir);
            // not over entrace door
            if (entraceDoor != null && entraceDoor.getDirection().getOppositeFace() == door.getDirection()
                    && ((entraceDoor.getOffset().getX() != door.getOffset().getX()
                    && Math.abs(entraceDoor.getOffset().getX() - door.getOffset().getX()) > DOOR_SIZE + 1)
                    || (entraceDoor.getOffset().getZ() != door.getOffset().getZ()
                    && Math.abs(entraceDoor.getOffset().getZ() - door.getOffset().getZ()) > DOOR_SIZE + 1)
                    || (Math.abs(entraceDoor.getOffset().getY() - door.getOffset().getY()) > DOOR_SIZE + 1))) {
                // too close to entrace

                new MessageBuilder(PLUGIN, getPlayer())
                        .addFullComponentTranslation("command.DungeonRoom.exit.error.near_entrace",
                                "&4[&cDungeonRoom&4] &cSelected place is too close to entrace",
                                Arrays.asList("&9The door can't be so close to entrace"), null, null)
                        .send();
                displayDoors();
                return;
            }
            // not over other exit doors
            for (Offset exit : new HashSet<>(exitDoors))
                if (exit.getDirection() == door.getDirection()) {
                    if (exit.equals(door)) {
                        // same door, remove
                        exitDoors.remove(exit);
                        // TODO
                        new MessageBuilder(PLUGIN, getPlayer()).addFullComponentTranslation(
                                "command.DungeonRoom.exit.unset", Arrays.asList("&2[&aDungeonRoom&2] &eRemoved exit"),
                                Arrays.asList("&6Clicca per eseguire &e/dungeonroom next", "",
                                        "&9Ripiazza il blocco per annullare", "&9Piazzalo altrove per creare altre uscite"),
                                "/dungeonroom next", ClickEvent.Action.RUN_COMMAND).send();
                        displayFakeDoorBlocks(Material.BLACK_STAINED_GLASS, (byte) 15, exit);

                        displayDoors();
                        return;
                    }
                    if ((exit.getOffset().getX() != door.getOffset().getX()
                            && Math.abs(exit.getOffset().getX() - door.getOffset().getX()) > DOOR_SIZE + 1)
                            || (exit.getOffset().getZ() != door.getOffset().getZ()
                            && Math.abs(exit.getOffset().getZ() - door.getOffset().getZ()) > DOOR_SIZE + 1)
                            || (Math.abs(exit.getOffset().getY() - door.getOffset().getY()) > DOOR_SIZE + 1)) {
                        // too close
                        new MessageBuilder(PLUGIN, getPlayer())
                                .addFullComponentTranslation("command.DungeonRoom.exit.error.near_exit",
                                        "&4[&cDungeonRoom&4] &cSelected place is too close to an exit",
                                        Arrays.asList("&9The door can't be so close to another exit"), null, null)
                                .send();
                        displayDoors();
                        return;
                    }
                }
            exitDoors.add(door);
            new MessageBuilder(PLUGIN, getPlayer())
                    .addFullComponentTranslation("command.DungeonRoom.exit.set",
                            Arrays.asList("&2[&aDungeonRoom&2] &eAdded exit"),
                            Arrays.asList("&6Clicca per eseguire &e/dungeonroom next", "",
                                    "&9Ripiazza il blocco per annullare", "&9Piazzalo altrove per creare altre uscite"),
                            "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                    .send();
            displayDoors();
        }

        private void displayFakeDoorBlocks(Material type, Offset loc) {
            displayFakeDoorBlocks(type, (byte) 0, loc);
        }

        private void displayFakeDoorBlocks(Material type, byte byteVal, Offset loc) {
            HashSet<Block> blocks = new HashSet<>();
            Block base = w.getBlockAt(loc1.clone().add(loc.getOffset()).toLocation(w));
            // blocks.add(base);
            for (int i = 0; i < DOOR_SIZE; i++)
                blocks.add(base.getRelative(BlockFace.UP, i));
            switch (loc.getDirection()) {
                case NORTH:
                case SOUTH:
                    for (int i = 1; i <= (DOOR_SIZE - 1) / 2; i++) {
                        Block base1 = base.getRelative(BlockFace.EAST, i);
                        Block base2 = base.getRelative(BlockFace.WEST, i);
                        for (int j = 0; j < DOOR_SIZE; j++) {
                            blocks.add(base1.getRelative(BlockFace.UP, j));
                            blocks.add(base2.getRelative(BlockFace.UP, j));
                        }
                    }
                    break;
                case EAST:
                case WEST:
                    for (int i = 1; i <= (DOOR_SIZE - 1) / 2; i++) {
                        Block base1 = base.getRelative(BlockFace.NORTH, i);
                        Block base2 = base.getRelative(BlockFace.SOUTH, i);
                        for (int j = 0; j < DOOR_SIZE; j++) {
                            blocks.add(base1.getRelative(BlockFace.UP, j));
                            blocks.add(base2.getRelative(BlockFace.UP, j));
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            new BukkitRunnable() {
                @SuppressWarnings("deprecation")
                public void run() {
                    for (Block b : blocks)
                        getPlayer().sendBlockChange(b.getLocation(), type, byteVal);
                }
            }.runTaskLater(PLUGIN, 3L);
            new BukkitRunnable() {

                @Override
                public void run() {
                    Player p = getPlayer();
                    if (p == null)
                        return;
                    for (Block b : blocks)
                        getPlayer().sendBlockChange(b.getLocation(), b.getBlockData());
                }

            }.runTaskLater(PLUGIN, 23L);

        }

        private boolean isInside(Location l) {
            return BoundingBox.of(loc1, loc2.clone().add(new BlockVector(1, 1, 1))).contains(l.toVector());
        }

        private boolean isInside(Block b) {
            return isInside(b.getLocation());
        }

        private boolean checkCorner(int x1, int x2) {
            if (Math.abs(x1 - x2) - ((DOOR_SIZE - 1) / 2) <= 0) {
                if (x1 != x2) {
                    // too close to corner
                    throw new IllegalArgumentException();
                }
                return true;
            }
            return false;
        }

        private void displayDoors() {

            if (entraceDoor != null)
                displayFakeDoorBlocks(Material.EMERALD_BLOCK, entraceDoor);
            for (Offset door : exitDoors)
                displayFakeDoorBlocks(Material.REDSTONE_BLOCK, door);
        }

        private static class RoomBuilderListener implements Listener {

            private RoomBuilderListener() {
                PLUGIN.registerListener(this);
            }

            @EventHandler
            private void event(BlockPlaceEvent event) {
                if (!rooms.containsKey(event.getPlayer().getUniqueId()))
                    return;
                emanondev.deepdungeons.roomold.RoomBuilder rBuilder = rooms.get(event.getPlayer().getUniqueId());
                switch (rBuilder.phase) {
                    case 2:
                        if (event.getBlockPlaced().getType() != Material.EMERALD_BLOCK)
                            return;
                        event.setCancelled(true);
                        rBuilder.setEntraceDoor(event.getBlock());
                        return;
                    case 3:
                        if (event.getBlockPlaced().getType() != Material.REDSTONE_BLOCK)
                            return;
                        event.setCancelled(true);
                        rBuilder.setExitDoor(event.getBlock());
                        return;
                }
            }

            @EventHandler
            private void event(EntityPlaceEvent event) {
                // Bukkit.broadcastMessage(event.getPlayer().getName()+" placed stand");
                if (!rooms.containsKey(event.getPlayer().getUniqueId()))
                    return;
                emanondev.deepdungeons.roomold.RoomBuilder rBuilder = rooms.get(event.getPlayer().getUniqueId());
                // Bukkit.broadcastMessage("Phase "+rBuilder.phase);
                switch (rBuilder.phase) {
                    case 4:
                        if (!(event.getEntity() instanceof ArmorStand))
                            return;
                        // event.setCancelled(true);
                        ArmorStand stand = (ArmorStand) event.getEntity();
                        stand.setGravity(false);
                        stand.setInvulnerable(true);
                        stand.setCustomName(MobManager.NAME);
                        stand.setSmall(true);
                        rBuilder.setPlaceMobProvider(stand);
                        return;
                    /*
                     * case 4: if (event.getBlockPlaced().getType() != Material.REDSTONE_BLOCK)
                     * return; event.setCancelled(true);
                     * rBuilder.setPlaceRewardProvider(event.getBlock()); return;
                     */
                }
            }

            @EventHandler
            private void event(PlayerInteractAtEntityEvent event) {
                if (!rooms.containsKey(event.getPlayer().getUniqueId()))
                    return;
                emanondev.deepdungeons.roomold.RoomBuilder rBuilder = rooms.get(event.getPlayer().getUniqueId());
                if (!(event.getRightClicked() instanceof ArmorStand))
                    return;

                ArmorStand stand = (ArmorStand) event.getRightClicked();
                checkStand(stand, rBuilder, event);
            }

            public void checkStand(ArmorStand stand, emanondev.deepdungeons.roomold.RoomBuilder rBuilder, Cancellable event) {
                switch (rBuilder.phase) {
                    case 4:
                    case 5: {
                        ItemStack item = stand.getEquipment().getItemInMainHand();
                        if (item == null || !item.hasItemMeta())
                            return;
                        ItemMeta meta = item.getItemMeta();
                        if (!meta.hasDisplayName())
                            return;
                        if (meta.getDisplayName().startsWith(MobManager.NAME + " ")) {
                            event.setCancelled(true);
                            MobProvider prov = DeepDungeons.get().getMobManager()
                                    .getProvider(meta.getDisplayName().substring(MobManager.NAME.length() + 1));
                            rBuilder.editPlaceMobProvider(stand, prov);
                        }
                        if (meta.getDisplayName().startsWith(RewardManager.NAME + " ")) {
                            event.setCancelled(true);
                            RewardProvider prov = DeepDungeons.get().getRewardManager()
                                    .getProvider(meta.getDisplayName().substring(RewardManager.NAME.length() + 1));
                            rBuilder.editPlaceRewardProvider(stand, prov);
                        }
                    }
                }
            }

            @EventHandler
            private void event(EntityDamageByEntityEvent event) {
                if (!rooms.containsKey(event.getDamager().getUniqueId()) || (!(event.getDamager() instanceof Player)))
                    return;
                Bukkit.broadcastMessage("PlayerInteractEntityEvent");
                emanondev.deepdungeons.roomold.RoomBuilder rBuilder = rooms.get(event.getDamager().getUniqueId());
                if (!(event.getEntity() instanceof ArmorStand))
                    return;

                ArmorStand stand = (ArmorStand) event.getEntity();
                checkStand(stand, rBuilder, event);
            }

            @EventHandler
            private void event(PlayerInteractEvent event) {
                if (!rooms.containsKey(event.getPlayer().getUniqueId()))
                    return;
                emanondev.deepdungeons.roomold.RoomBuilder rBuilder = rooms.get(event.getPlayer().getUniqueId());
                switch (rBuilder.phase) {
                    case 5:
                        if (event.getItem() == null || event.getItem().getType() != Material.GOLD_INGOT
                                || event.getClickedBlock() == null)
                            return;
                        if (!(event.getClickedBlock().getState() instanceof Container))
                            return;
                        event.setCancelled(true);
                        Collection<Entity> coll = event.getClickedBlock().getWorld().getNearbyEntities(
                                event.getClickedBlock().getBoundingBox(), (e) -> e.getType() == EntityType.ARMOR_STAND);
                        for (Entity e : coll) {
                            if (!e.getLocation().getBlock().equals(event.getClickedBlock()))
                                continue;
                            ArmorStand stand = (ArmorStand) e;
                            ItemStack item = stand.getEquipment().getItemInMainHand();
                            if (!item.hasItemMeta())
                                continue;
                            ItemMeta meta = item.getItemMeta();
                            if (!meta.hasDisplayName() || !meta.getDisplayName().startsWith(RewardManager.NAME + " "))
                                continue;
                            RewardProvider prov = DeepDungeons.get().getRewardManager()
                                    .getProvider(meta.getDisplayName().substring(RewardManager.NAME.length() + 1));
                            rBuilder.editPlaceRewardProvider(stand, prov);
                            return;
                        }
                        ArmorStand stand = (ArmorStand) event.getClickedBlock().getWorld()
                                .spawnEntity(event.getClickedBlock().getLocation().add(0.5, 0D, 0.5), EntityType.ARMOR_STAND);
                        stand.setInvisible(true);
                        rBuilder.setPlaceRewardProvider(stand);
                        return;
                }

            }
        }

    }

}
