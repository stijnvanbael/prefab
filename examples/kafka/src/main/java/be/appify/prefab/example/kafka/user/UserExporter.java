package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserExporter {
    private final Logger log = LoggerFactory.getLogger(UserExporter.class);
    private final List<Reference<User>> exportedUsers = new ArrayList<>();

    @EventHandler
    public void onUserCreated(UserEvent.Created event) {
        log.info("Exporting new user with ID {}", event.reference());
        exportedUsers.add(event.reference());
        // Export logic here
    }

    public List<Reference<User>> exportedUsers() {
        return exportedUsers;
    }
}
