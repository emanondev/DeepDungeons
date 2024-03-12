package emanondev.deepdungeons;

import emanondev.core.CorePlugin;
import emanondev.deepdungeons.command.*;
import emanondev.deepdungeons.door.DoorTypeManager;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonTypeManager;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.room.RoomInstanceManager;
import emanondev.deepdungeons.room.RoomTypeManager;
import emanondev.deepdungeons.spawner.MonsterSpawnerTypeManager;
import emanondev.deepdungeons.trap.TrapTypeManager;
import emanondev.deepdungeons.treasure.TreasureTypeManager;

public class DeepDungeons extends CorePlugin {

    //public static final boolean DEBUG = true;
    private static DeepDungeons instance;
    /*private RoomManager roomManager;
    private RoomDataFactory roomDataFactory;
    private MobManager mobManager;
    private RewardManager rewardManager;*/

    public static DeepDungeons get() {
        return instance;
    }

    @Override
    protected boolean registerReloadCommand() {
        return false;
    }

    @Override
    public void disable() {
        BuilderMode.getInstance().disable();
    }

    @Override
    public void reload() {

    }

    @Override
    public void enable() {

        TreasureTypeManager.getInstance().load();
        TrapTypeManager.getInstance().load();
        MonsterSpawnerTypeManager.getInstance().load();
        DoorTypeManager.getInstance().load();
        RoomTypeManager.getInstance().load();
        RoomInstanceManager.getInstance().load();
        DungeonTypeManager.getInstance().load();
        DungeonInstanceManager.getInstance().load();
        PartyManager.getInstance().load();
        BuilderMode.getInstance();
        this.registerCommand(new DungeonTreasureCommand());
        this.registerCommand(new DungeonMonsterSpawnerCommand());
        this.registerCommand(new DungeonRoomBuilderCommand());
        this.registerCommand(new DungeonDungeonBuilderCommand());
        this.registerCommand(new DungeonCreatorCommand());
    }

    @Override
    public void load() {
        instance = this;
    }

    /*
        ConfigurationSerialization.registerClass(Offset.class);
        this.roomManager = new RoomManager();
        this.roomDataFactory = new RoomDataFactory();
        this.registerCommand(new DeepDungeonsCommand());
        this.registerCommand(new DungeonRoomCommand());
        mobManager = new MobManager();
        rewardManager = new RewardManager();
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
    }*/

}
