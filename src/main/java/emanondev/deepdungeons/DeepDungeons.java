package emanondev.deepdungeons;

import emanondev.core.CorePlugin;
import emanondev.deepdungeons.command.DeepDungeonsCommand;
import emanondev.deepdungeons.command.DungeonRoomCommand;
import emanondev.deepdungeons.mob.MobManager;
import emanondev.deepdungeons.reward.RewardManager;
import emanondev.deepdungeons.roomold.RoomDataFactory;
import emanondev.deepdungeons.roomold.RoomManager;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

public class DeepDungeons extends CorePlugin {

    public static final boolean DEBUG = true;
    private static DeepDungeons instance;
    private RoomManager roomManager;
    private RoomDataFactory roomDataFactory;
    private MobManager mobManager;
    private RewardManager rewardManager;

    public static DeepDungeons get() {
        return instance;
    }

    @Override
    protected boolean registerReloadCommand() {
        return false;
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
    }

    @Override
    public void enable() {
        ConfigurationSerialization.registerClass(Offset.class);
        this.roomManager = new RoomManager();
        this.roomDataFactory = new RoomDataFactory();
        this.registerCommand(new DeepDungeonsCommand());
        this.registerCommand(new DungeonRoomCommand());
        mobManager = new MobManager();
        rewardManager = new RewardManager();
    }

    @Override
    public void load() {
        instance = this;
    }

    public RoomDataFactory getRoomDataFactory() {
        return roomDataFactory;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

}
