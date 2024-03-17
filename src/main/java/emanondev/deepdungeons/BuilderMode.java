package emanondev.deepdungeons;

import emanondev.core.gui.Gui;
import emanondev.core.message.SimpleMessage;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.room.RoomInstanceManager;
import emanondev.deepdungeons.room.RoomType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class BuilderMode implements Listener {

    private static final BuilderMode instance = new BuilderMode();
    private final PauseListener pauseListener;
    private final HashMap<Player, ActiveBuilder> builderMode = new HashMap<>();//TODO cannot build 2 stuff with same id at same time
    private final HashMap<UUID, ActiveBuilder> paused = new HashMap<>();
    private final HashMap<Player, ItemStack[]> inventoryBackup = new HashMap<>();
    private final HashMap<Player, ItemStack[]> offhandBackup = new HashMap<>();
    private final HashMap<Player, ItemStack[]> equipmentBackup = new HashMap<>();
    private final HashMap<UUID, Long> lastPlayerInteraction = new HashMap<>();
    private BukkitTask timerTask;

    private BuilderMode() {
        this.pauseListener = new PauseListener();
    }

    public static @NotNull BuilderMode getInstance() {
        return instance;
    }

    public boolean isOnEditorMode(@NotNull Player player) {
        return builderMode.containsKey(player);
    }

    public @Nullable ActiveBuilder getBuilderMode(@NotNull Player player) {
        return builderMode.get(player);
    }

    public void exitBuilderMode(@NotNull Player player) {
        if (!builderMode.containsKey(player))
            throw new IllegalArgumentException("not building");
        builderMode.remove(player);
        player.getInventory().setStorageContents(inventoryBackup.remove(player));
        player.getInventory().setExtraContents(offhandBackup.remove(player));
        player.getInventory().setArmorContents(equipmentBackup.remove(player));
        if (DeepDungeons.get().getConfig().loadBoolean("editor.actionbar_reminder", true))
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder().create());

        if (builderMode.isEmpty()) {
            DeepDungeons.get().unregisterListener(this);
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
        }

    }

    public boolean enterBuilderMode(@NotNull Player player, @NotNull ActiveBuilder builder) {
        if (builderMode.containsKey(player) || paused.containsKey(player.getUniqueId()))
            return false;
        if (builder.getCompletableFuture().isDone())
            return false;
        inventoryBackup.put(player, player.getInventory().getStorageContents());
        offhandBackup.put(player, player.getInventory().getExtraContents());
        equipmentBackup.put(player, player.getInventory().getArmorContents());
        player.getInventory().clear();
        builderMode.put(player, builder);
        builder.getCompletableFuture().whenComplete((value, error) -> {
            exitBuilderMode(player);
            if (value != null) {
                try {
                    value.write();
                    if (builder instanceof RoomType.RoomInstanceBuilder) {
                        RoomInstanceManager.getInstance().register(RoomInstanceManager.getInstance().readInstance(
                                new File(RoomInstanceManager.getInstance().getFolder(), builder.getId() + ".yml")));
                    } else if (builder instanceof DungeonType.DungeonInstanceBuilder) {
                        DungeonInstanceManager.getInstance().register(DungeonInstanceManager.getInstance().readInstance(
                                new File(DungeonInstanceManager.getInstance().getFolder(), builder.getId() + ".yml")));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setupTools();
        if (timerTask == null) {
            DeepDungeons.get().registerListener(this);
            timerTask = new BukkitRunnable() {
                private long counter = 0;

                @Override
                public void run() {
                    counter++;
                    builderMode.forEach((p, b) -> {
                        if (!(p.isValid() && p.isOnline()))
                            return;
                        if (DeepDungeons.get().getConfig().loadBoolean("buildermode.actionbar_reminder", true) && counter % 5 == 0)
                            new SimpleMessage(DeepDungeons.get(), "buildermode.actionbar").sendActionBar(p);
                        if (counter % 50 == 0)
                            b.setupTools();
                        b.timerTick();
                    });
                }
            }.runTaskTimer(DeepDungeons.get(), 2L, 2L);
        }
        return true;
    }

    @EventHandler
    public void event(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && isOnEditorMode((Player) event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    public void event(@NotNull PlayerTeleportEvent event) {//if change world
        if (!isOnEditorMode(event.getPlayer()))
            return;
        if (event.getTo() == null || Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) {
            exitBuilderMode(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void event(@NotNull InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && isOnEditorMode((Player) event.getWhoClicked())
                && !(event.getWhoClicked().getOpenInventory().getTopInventory().getHolder() instanceof Gui))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void event(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && isOnEditorMode(p))
            event.setCancelled(true);
        //TODO if EntityDamageByEntity && player notify him
    }

    @EventHandler
    public void event(@NotNull EntityResurrectEvent event) {
        if (event.isCancelled() && event.getEntity() instanceof Player p && isOnEditorMode(p))
            exitBuilderMode(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void event(@NotNull PlayerDropItemEvent event) {
        if (isOnEditorMode(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void event(@NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL)
            return;
        ActiveBuilder builder = getBuilderMode(event.getPlayer());
        if (builder == null)
            return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        //there is a strange bug that fires the event multiple times, this put a cooldown on click for a max output of 10cps
        long nowMs = System.currentTimeMillis();
        long lastMs = this.lastPlayerInteraction.getOrDefault(event.getPlayer().getUniqueId(), nowMs - 150);
        if (lastMs + 100 >= nowMs) //2 tick
            return;
        this.lastPlayerInteraction.put(event.getPlayer().getUniqueId(), nowMs);
        if (event.getPlayer().getInventory().getHeldItemSlot() == 8) {
            BuilderMode.getInstance().exitBuilderMode(event.getPlayer());
            return;
        }
        builder.handleInteract(event);
    }

    @EventHandler
    public void event(@NotNull PlayerQuitEvent event) {
        pauseBuilder(event.getPlayer());
    }

    public boolean isOnPausedEditorMode(@NotNull Player player) {
        return paused.containsKey(player.getUniqueId());
    }

    public boolean pauseBuilder(@NotNull Player player) {
        ActiveBuilder builder = builderMode.get(player);
        if (builder == null)
            return false;
        exitBuilderMode(player);
        if (paused.isEmpty())
            DeepDungeons.get().registerListener(pauseListener);
        paused.put(player.getUniqueId(), builder);
        return true;
    }

    public boolean unpauseBuilder(@NotNull Player player) {
        ActiveBuilder builder = paused.remove(player.getUniqueId());
        if (builder == null)
            return false;
        enterBuilderMode(player, builder);
        if (paused.isEmpty())
            DeepDungeons.get().unregisterListener(pauseListener);
        return true;
    }

    public void disable() {
        for (Player player : new ArrayList<>(builderMode.keySet()))
            exitBuilderMode(player);
    }

    private class PauseListener implements Listener {
        @EventHandler
        public void event(PlayerJoinEvent event) {
            unpauseBuilder(event.getPlayer());
        }
    }
}
