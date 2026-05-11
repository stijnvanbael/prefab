package event.avsc.dependencygeneratedconsumer;

import be.appify.prefab.core.annotations.EventHandler;
import event.avsc.dependency.DependencyAvscEvent;

public class DependencyGeneratedConsumer {

    @EventHandler
    public void on(DependencyAvscEvent event) {
        // no-op
    }
}

