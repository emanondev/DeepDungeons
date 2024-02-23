package emanondev.deepdungeons.door.impl;

import emanondev.deepdungeons.door.DoorInstance;
import emanondev.deepdungeons.door.DoorType;

public class StandardType extends DoorType {
    public StandardType() {
        super("standard");
    }

    public class StandardInstance extends DoorInstance {


        public StandardInstance() {
            super(StandardType.this);
        }
    }
}
