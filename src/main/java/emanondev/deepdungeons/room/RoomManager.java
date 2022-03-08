package emanondev.deepdungeons.room;

import java.util.*;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

import emanondev.deepdungeons.DeepDungeons;

public class RoomManager {
	
	private final Map<String,Room> rooms = new HashMap<>();
	
	public Collection<Room> getRooms(){
		return Collections.unmodifiableCollection(rooms.values());
	}
	public Map<String,Room> getRoomsMap(){
		return Collections.unmodifiableMap(rooms);
	}
	
	public Room getRoom(String id) {
		return rooms.get(id);
	}
	
	public Room createRoom(String id,Clipboard clip) {
		if (id==null || clip==null)
			throw new NullPointerException();
		if (rooms.containsKey(id) )
			throw new IllegalArgumentException();
		Room room = new Room(id,DeepDungeons.get().getRoomDataFactory().getRoomData(id));
		rooms.put(room.getId(), room);
		room.save(clip);
		return room;
	}
	
	public boolean deleteRoom(String id) {
		Room val = rooms.remove(id);
		if (val==null)
			return false;
		return val.delete();
	}

}
