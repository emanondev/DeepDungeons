package emanondev.deepdungeons.room;

import emanondev.deepdungeons.DeepDungeons;

import java.io.File;

public class RoomDataFactory {

    public RoomData getRoomData(String roomId) {
        return new RoomData(DeepDungeons.get().getConfig("data" + File.separator + "rooms").loadSection(roomId));
    }

}
