package be.appify.prefab.example.kafka.dashboard;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import be.appify.prefab.example.kafka.channelsummary.application.ChannelSummaryService;
import be.appify.prefab.example.kafka.user.UnreadMessage;
import be.appify.prefab.example.kafka.user.User;
import be.appify.prefab.example.kafka.user.application.UserStatusRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Composes the paged channel projection with one batched lookup for a user's unread status.
 */
@Service
public class ChannelDashboardService {
    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.asc("id"));

    private final ChannelSummaryService channelSummaryService;
    private final UserStatusRepository userStatusRepository;

    /**
     * Creates the dashboard adapter.
     *
     * @param channelSummaryService the generated channel-summary query service
     * @param userStatusRepository the generated repository with the handwritten user lookup mixin
     */
    public ChannelDashboardService(
            ChannelSummaryService channelSummaryService,
            UserStatusRepository userStatusRepository
    ) {
        this.channelSummaryService = channelSummaryService;
        this.userStatusRepository = userStatusRepository;
    }

    /**
     * Returns one screen page. The caller must derive {@code userId} from the authenticated identity and authorize
     * access before invoking this method; it must not come from an untrusted request field.
     *
     * @param userId authenticated user's identifier
     * @param page zero-based page index
     * @param pageSize maximum number of channel summaries in the page
     * @param name optional channel-name search
     * @return the composed page, preserving the generated service's total and page metadata
     */
    public Page<ChannelDashboardItem> getDashboard(String userId, int page, int pageSize, String name) {
        var pageable = PageRequest.of(page, pageSize, DEFAULT_SORT);
        var statuses = userStatusRepository.findByUser(Reference.<User>fromId(userId));
        var unreadByChannel = statuses.stream()
                .flatMap(status -> status.unreadMessages().stream())
                .collect(Collectors.groupingBy(UnreadMessage::channel, Collectors.summingInt(message -> 1)));
        var statusAvailable = !statuses.isEmpty();

        return channelSummaryService.getList(pageable, name)
                .map(summary -> new ChannelDashboardItem(
                        summary.channel(),
                        summary.name(),
                        summary.totalMessages(),
                        summary.totalSubscribers(),
                        unreadByChannel.getOrDefault(summary.channel(), 0),
                        statusAvailable));
    }
}
