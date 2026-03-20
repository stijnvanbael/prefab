package be.appify.prefab.example.snssqs.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.Channel;
import be.appify.prefab.example.snssqs.message.Message;

public record UnreadMessage(Reference<Message> message, Reference<Channel> channel) {
}
