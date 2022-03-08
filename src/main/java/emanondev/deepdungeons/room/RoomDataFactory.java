package emanondev.deepdungeons.room;

import java.io.File;

import emanondev.deepdungeons.DeepDungeons;

public class RoomDataFactory {
	
	public RoomData getRoomData(String roomId) {
		return new RoomData(DeepDungeons.get().getConfig("data"+File.separator+"rooms").loadSection(roomId));
	}

}
