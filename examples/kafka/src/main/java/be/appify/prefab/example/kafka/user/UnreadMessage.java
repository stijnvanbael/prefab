package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import be.appify.prefab.example.kafka.message.Message;

public record UnreadMessage(Reference<Message> message, Reference<Channel> channel) {
}
