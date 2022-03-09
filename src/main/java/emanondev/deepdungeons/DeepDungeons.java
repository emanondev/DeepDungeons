package emanondev.deepdungeons;

import org.bukkit.configuration.serialization.ConfigurationSerialization;

import emanondev.core.CorePlugin;
import emanondev.deepdungeons.command.DeepDungeonsCommand;
import emanondev.deepdungeons.command.DungeonRoomCommand;
import emanondev.deepdungeons.mob.MobManager;
import emanondev.deepdungeons.reward.RewardManager;
import emanondev.deepdungeons.room.RoomDataFactory;
import emanondev.deepdungeons.room.RoomManager;

public class DeepDungeons extends CorePlugin {

	private static DeepDungeons instance;
	
	public static final boolean DEBUG = true;

	public static DeepDungeons get() {
		return instance;
	}
	
	private RoomManager roomManager;
	private RoomDataFactory roomDataFactory;

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
	
	private MobManager mobManager;

	public MobManager getMobManager() {
		return mobManager;
	}
	private RewardManager rewardManager;

	public RewardManager getRewardManager() {
		return rewardManager;
	}

}
