package be.appify.prefab.example.kafka.dashboard;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.view.ViewAssembler;
import be.appify.prefab.core.view.ViewContext;
import be.appify.prefab.example.kafka.channelsummary.ChannelSummary;
import be.appify.prefab.example.kafka.user.UnreadMessage;
import be.appify.prefab.example.kafka.user.User;
import be.appify.prefab.example.kafka.user.application.UserStatusRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adds the current user's unread status to one already-paged batch of channel summaries. */
@Component
public class ChannelDashboardAssembler implements ViewAssembler<ChannelSummary, ChannelDashboardItem> {
    private final UserStatusRepository userStatusRepository;

    /**
     * Creates the dashboard view assembler.
     *
     * @param userStatusRepository repository with the batched user-status lookup
     */
    public ChannelDashboardAssembler(UserStatusRepository userStatusRepository) {
        this.userStatusRepository = userStatusRepository;
    }

    @Override
    public List<ChannelDashboardItem> assemble(List<ChannelSummary> page, ViewContext context) {
        var statuses = context.currentUser()
                .map(user -> userStatusRepository.findByUser(Reference.<User>fromId(user.getName())))
                .orElseGet(List::of);
        var unreadByChannel = statuses.stream()
                .flatMap(status -> status.unreadMessages().stream())
                .collect(Collectors.groupingBy(UnreadMessage::channel, Collectors.summingInt(message -> 1)));
        var statusAvailable = !statuses.isEmpty();

        return page.stream()
                .map(summary -> new ChannelDashboardItem(
                        summary.channel(),
                        summary.name(),
                        summary.totalMessages(),
                        summary.totalSubscribers(),
                        unreadByChannel.getOrDefault(summary.channel(), 0),
                        statusAvailable))
                .toList();
    }
}
