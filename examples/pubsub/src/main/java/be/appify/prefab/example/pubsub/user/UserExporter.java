package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserExporter {
    private final Logger log = LoggerFactory.getLogger(UserExporter.class);
    private final List<String> exportedUserIds = new ArrayList<>();

    @EventHandler
    public void onUserCreated(UserEvent.Created event) {
        log.info("Exporting new user with ID {}", event.id());
        exportedUserIds.add(event.id());
        // Export logic here
    }

    public List<String> exportedUserIds() {
        return exportedUserIds;
    }
}
