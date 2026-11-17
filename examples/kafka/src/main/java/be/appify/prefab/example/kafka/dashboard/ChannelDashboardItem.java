package be.appify.prefab.example.kafka.dashboard;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;

/**
 * Screen-oriented channel summary enriched with the current user's unread count.
 *
 * @param channel the channel reference
 * @param name the channel name
 * @param totalMessages the projected message count
 * @param totalSubscribers the projected subscriber count
 * @param unreadMessages the number of unread messages, or zero when no status projection exists
 * @param unreadStatusAvailable whether the user's status projection was available
 */
public record ChannelDashboardItem(
        Reference<Channel> channel,
        String name,
        int totalMessages,
        int totalSubscribers,
        int unreadMessages,
        boolean unreadStatusAvailable
) {
}
