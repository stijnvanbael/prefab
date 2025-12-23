package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;
import be.appify.prefab.example.pubsub.message.Message;

public record UnreadMessage(Reference<Message> message, Reference<Channel> channel) {
}
