package emanondev.deepdungeons;

import emanondev.core.gui.Gui;
import emanondev.core.message.DMessage;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class RoomBuilderMode implements Listener {

    private static final RoomBuilderMode instance = new RoomBuilderMode();
    private final PauseListener pauseListener;

    public static RoomBuilderMode getInstance() {
        return instance;
    }

    private RoomBuilderMode() {
        this.pauseListener = new PauseListener();
    }

    private final HashMap<Player, RoomType.RoomInstanceBuilder> builderMode = new HashMap<>();
    private final HashMap<UUID, RoomType.RoomInstanceBuilder> paused = new HashMap<>();
    private final HashMap<Player, ItemStack[]> inventoryBackup = new HashMap<>();
    private final HashMap<Player, ItemStack[]> offhandBackup = new HashMap<>();
    private final HashMap<Player, ItemStack[]> equipmentBackup = new HashMap<>();
    private BukkitTask timerTask;

    public boolean isOnEditorMode(@NotNull Player player) {
        return builderMode.containsKey(player);
    }

    public @Nullable RoomType.RoomInstanceBuilder getBuilderMode(@NotNull Player player) {
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

    public void enterBuilderMode(@NotNull Player player, @NotNull RoomType.RoomInstanceBuilder builder) {
        if (builderMode.containsKey(player))
            throw new IllegalArgumentException("already building");
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
                        if (DeepDungeons.get().getConfig().loadBoolean("editor.actionbar_reminder", true) && counter % 5 == 0)
                            new DMessage(DeepDungeons.get(), p).append("<gold>You are on RoomBuilder Mode").sendActionBar();
                        //TODO configurable DeepDungeons.get().getLanguageConfig(p).getMessage("editor.reminder",
                        if (counter % 50 == 0)
                            b.setupTools();
                        b.timerTick();
                    });
                }
            }.runTaskTimer(DeepDungeons.get(), 2L, 2L);
        }
    }

    private final HashMap<UUID, Long> lastPlayerInteraction = new HashMap<>();

    @EventHandler
    public void event(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && isOnEditorMode((Player) event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    public void event(PlayerTeleportEvent event) {//if change world
        if (!isOnEditorMode(event.getPlayer()))
            return;
        if (event.getTo() == null || Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) {
            exitBuilderMode(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void event(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && isOnEditorMode((Player) event.getWhoClicked())
                && !(event.getWhoClicked().getOpenInventory().getTopInventory().getHolder() instanceof Gui))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void event(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && isOnEditorMode(p))
            event.setCancelled(true);
        //TODO if EntityDamageByEntity && player notify him
    }

    @EventHandler
    public void event(EntityResurrectEvent event) {
        if (event.isCancelled() && event.getEntity() instanceof Player p && isOnEditorMode(p))
            exitBuilderMode(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void event(PlayerDropItemEvent event) {
        if (isOnEditorMode(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void event(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL)
            return;
        RoomType.@Nullable RoomInstanceBuilder builder = getBuilderMode(event.getPlayer());
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

        builder.handleInteract(event);
    }

    @EventHandler
    public void event(PlayerQuitEvent event) {
        RoomType.RoomInstanceBuilder builder = builderMode.get(event.getPlayer());
        if (builder != null) {
            exitBuilderMode(event.getPlayer());
            if (paused.isEmpty())
                DeepDungeons.get().registerListener(pauseListener);
            paused.put(event.getPlayer().getUniqueId(), builder);
        }
    }

    private class PauseListener implements Listener {
        @EventHandler
        public void event(PlayerJoinEvent event) {
            RoomType.RoomInstanceBuilder builder = paused.remove(event.getPlayer().getUniqueId());
            if (builder != null) {
                enterBuilderMode(event.getPlayer(), builder);
                if (paused.isEmpty())
                    DeepDungeons.get().unregisterListener(pauseListener);
            }
        }
    }

    public void disable() {
        for (Player player : new ArrayList<>(builderMode.keySet()))
            exitBuilderMode(player);
    }
}
