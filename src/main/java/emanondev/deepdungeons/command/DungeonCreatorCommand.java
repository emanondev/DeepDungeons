package emanondev.deepdungeons.command;

import emanondev.core.command.CoreCommand;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.party.PartyManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DungeonCreatorCommand extends CoreCommand {
    public DungeonCreatorCommand() {
        super("dungeoncreator", DeepDungeons.get(), Perms.DUNGEONCREATOR_COMMAND);
    }

    //create <id>
    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "create" -> {
                create(sender, label, args);
                return;
            }
            case "start" -> {
                start(sender, label, args);
                return;
            }
            case "test" -> {
                final List<Particle> particles = new ArrayList<>(List.of(Particle.values()));
                particles.sort(Comparator.comparing(Enum::name));
                final Player p = (Player) sender;
                final Location loc = p.getLocation().add(0, 1, 2).setDirection(BlockFace.NORTH.getDirection());
                final Location loc2 = p.getLocation().add(0, 2.5, 0).setDirection(BlockFace.NORTH.getDirection());
                final Location loc3 = p.getLocation().add(0, 4, -2).setDirection(BlockFace.NORTH.getDirection());
                TextDisplay text = (TextDisplay) p.getWorld().spawnEntity(loc.clone().add(0, 0.3, 0), EntityType.TEXT_DISPLAY, false);
                text.setBillboard(Display.Billboard.CENTER);
                text.setLineWidth(text.getLineWidth() * 3);
                Transformation tr = text.getTransformation();
                tr.getScale().mul(0.6F, 0.6F, 0.6F);
                text.setTransformation(tr);
                text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                TextDisplay text2 = (TextDisplay) text.copy(loc2.clone().add(0, 0.3, 0));
                TextDisplay text3 = (TextDisplay) text.copy(loc3.clone().add(0, 0.3, 0));
                new BukkitRunnable() {
                    int counter = 0;
                    boolean even = true;

                    @Override
                    public void run() {
                        counter++;
                        if (counter / 50 >= particles.size()) {
                            text.remove();
                            text2.remove();
                            text3.remove();
                            this.cancel();
                            return;
                        }
                        int tick = counter % 50;
                        if (tick > 10)
                            return;
                        double speed1 = 0.1;
                        double speed2 = 1;
                        double speed3 = 10;
                        Particle part = particles.get(counter / 50);
                        if (tick == 0) {
                            even = !even;
                            text.setText("Particle: " + ChatColor.YELLOW + part.name() + ChatColor.WHITE + " (speed: " + ChatColor.YELLOW + speed1 + ChatColor.WHITE + ")");
                            text2.setText("Particle: " + ChatColor.YELLOW + part.name() + ChatColor.WHITE + " (speed: " + ChatColor.YELLOW + speed2 + ChatColor.WHITE + ")");
                            text3.setText("Particle: " + ChatColor.YELLOW + part.name() + ChatColor.WHITE + " (speed: " + ChatColor.YELLOW + speed3 + ChatColor.WHITE + ")");
                        }
                        Object data = null;
                        if (part.getDataType() == BlockData.class)
                            data = Bukkit.createBlockData(Material.OAK_LOG);
                        if (part.getDataType() == ItemStack.class)
                            data = new ItemStack(Material.OAK_LOG);
                        if (part.getDataType() == MaterialData.class)
                            data = new MaterialData(Material.OAK_LOG);
                        if (part.getDataType() == Particle.DustOptions.class)
                            data = new Particle.DustOptions(Color.RED, 1);
                        if (part.getDataType() == Particle.DustTransition.class)
                            data = new Particle.DustTransition(Color.RED, Color.BLUE, 1);
                        if (part.getDataType() == Float.class)
                            data = 1F;
                        if (part.getDataType() == Integer.class)
                            data = 1;
                        if (part.getDataType() == Vibration.class)
                            data = new Vibration(loc, new Vibration.Destination.BlockDestination(loc
                                    .getBlock().getRelative(BlockFace.NORTH, 3)), 20);
                        try {
                            Vector dir = loc.getDirection().multiply(-1);
                            p.spawnParticle(part, loc, 0, dir.getX(), dir.getY(), dir.getZ(), speed1, data);
                            if (part.getDataType() == Vibration.class)
                                data = new Vibration(loc2, new Vibration.Destination.BlockDestination(loc2
                                        .getBlock().getRelative(BlockFace.NORTH, 3)), 20);
                            p.spawnParticle(part, loc2, 0, dir.getX(), dir.getY(), dir.getZ(), speed2, data);
                            if (part.getDataType() == Vibration.class)
                                data = new Vibration(loc3, new Vibration.Destination.BlockDestination(loc3
                                        .getBlock().getRelative(BlockFace.NORTH, 3)), 20);
                            p.spawnParticle(part, loc3, 0, dir.getX(), dir.getY(), dir.getZ(), speed3, data);
                        } catch (Exception e) {
                            if (tick == 0)
                                p.sendMessage("Cannot spawn");
                        }
                    }
                }.runTaskTimer(DeepDungeons.get(), 100L, 1L);
                return;
            }
        }
        sender.sendMessage("Help Message not implemented");
    }

    private void start(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }
        if (args.length != 2) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        String id = args[1];
        DungeonInstance dungeon = DungeonInstanceManager.getInstance().get(id);
        DungeonHandler handler = AreaManager.getInstance().getReady(dungeon);
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null)
            party = PartyManager.getInstance().createParty(player);
        PartyManager.getInstance().startDungeon(party, handler);
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        String id = args[1];
        DungeonInstance dungeon = DungeonInstanceManager.getInstance().get(id);
        if (dungeon == null) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        DungeonHandler handler = dungeon.createHandler(null);
        Vector loc = handler.getBoundingBox().getCenter();
        new DMessage(DeepDungeons.get(), sender).append("<blue>Dungeon created at " + loc.getBlockX() + " "
                + loc.getBlockY() + " " + loc.getBlockZ() + " ").append("<click:run_command:'/tp "
                + sender.getName() + " " + loc.getBlockX() + " " + (loc.getBlockY() + handler.getBoundingBox().getHeight() * 0.5 + 10) + " " + loc.getBlockZ()
                + "'><hover:show_text:'teleport to " + loc.getBlockX() + " " + (loc.getBlockY() + handler.getBoundingBox().getHeight() * 0.5 + 10) + " "
                + loc.getBlockZ() + "'><yellow>[TP]</yellow></hover></click>").send();
    }

    @Override
    @Nullable
    public List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        return switch (args.length) {
            case 1 -> this.complete(args[0], new String[]{"create", "start"});
            case 2 -> this.complete(args[1], DungeonInstanceManager.getInstance().getIds());
            default -> Collections.emptyList();
        };
    }
}
