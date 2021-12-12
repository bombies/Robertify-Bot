package main.events;

import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;

public class MongoEventListener implements CommandListener {

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        //TODO
    }
}
