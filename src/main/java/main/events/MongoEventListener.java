package main.events;

import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MongoEventListener implements CommandListener {
    private final Logger logger = LoggerFactory.getLogger(MongoEventListener.class);


    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
//        logger.info("{} command ran. Took {}ms", event.getCommandName(), event.getElapsedTime(TimeUnit.MILLISECONDS));
    }
}
