package sns.supertype;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topic.user.name}", platform = Event.Platform.SNS_SQS)
public record UserCreated(String id, String name) implements UserEvent {
}

