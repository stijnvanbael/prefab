package be.appify.prefab.example.kafka.dashboard;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import be.appify.prefab.example.kafka.channelsummary.ChannelSummary;
import be.appify.prefab.example.kafka.channelsummary.application.ChannelSummaryService;
import be.appify.prefab.example.kafka.user.UnreadMessage;
import be.appify.prefab.example.kafka.user.User;
import be.appify.prefab.example.kafka.user.UserStatus;
import be.appify.prefab.example.kafka.user.application.UserStatusRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class ChannelDashboardServiceTest {
    private final ChannelSummaryService channelSummaryService = mock(ChannelSummaryService.class);
    private final UserStatusRepository userStatusRepository = mock(UserStatusRepository.class);
    private final ChannelDashboardService dashboardService =
            new ChannelDashboardService(channelSummaryService, userStatusRepository);

    @Test
    void getDashboard_composesPagedSummariesWithOneStatusLookup() {
        var user = Reference.<User>fromId("user-1");
        var channelWithUnreadMessages = Reference.<Channel>fromId("channel-1");
        var channelWithoutUnreadMessages = Reference.<Channel>fromId("channel-2");
        var summaries = List.of(
                summary(channelWithUnreadMessages, "General", 4, 2),
                summary(channelWithoutUnreadMessages, "News", 7, 3));
        var sort = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
        when(channelSummaryService.getList(any(PageRequest.class), eq("n")))
                .thenReturn(new PageImpl<>(summaries, PageRequest.of(0, 2, sort), 2));
        when(userStatusRepository.findByUser(user))
                .thenReturn(List.of(userStatus(user, channelWithUnreadMessages)));

        var page = dashboardService.getDashboard(user.id(), 0, 2, "n");

        assertThat(page.getContent()).containsExactly(
                new ChannelDashboardItem(channelWithUnreadMessages, "General", 4, 2, 2, true),
                new ChannelDashboardItem(channelWithoutUnreadMessages, "News", 7, 3, 0, true));
        assertThat(page.getTotalElements()).isEqualTo(2);
        verify(channelSummaryService).getList(PageRequest.of(0, 2, sort), "n");
        verify(userStatusRepository).findByUser(user);
    }

    @Test
    void getDashboard_marksMissingStatusProjectionUnavailable() {
        var user = Reference.<User>fromId("user-1");
        when(channelSummaryService.getList(any(PageRequest.class), eq(null)))
                .thenReturn(new PageImpl<>(List.of(
                        summary(Reference.<Channel>fromId("channel-1"), "General", 1, 1))));
        when(userStatusRepository.findByUser(user)).thenReturn(List.of());

        var item = dashboardService.getDashboard(user.id(), 0, 10, null).getContent().getFirst();

        assertThat(item.unreadMessages()).isZero();
        assertThat(item.unreadStatusAvailable()).isFalse();
    }

    @Test
    void getDashboard_marksEmptyButPresentStatusProjectionAvailable() {
        var user = Reference.<User>fromId("user-1");
        when(channelSummaryService.getList(any(PageRequest.class), eq(null)))
                .thenReturn(new PageImpl<>(List.of(summary(Reference.fromId("channel-1"), "General", 1, 1))));
        when(userStatusRepository.findByUser(user))
                .thenReturn(List.of(new UserStatus(Reference.create(), 0L, user, List.of())));

        var item = dashboardService.getDashboard(user.id(), 0, 10, null).getContent().getFirst();

        assertThat(item.unreadMessages()).isZero();
        assertThat(item.unreadStatusAvailable()).isTrue();
    }

    private static ChannelSummary summary(Reference<Channel> channel, String name, int messages, int subscribers) {
        return new ChannelSummary(Reference.create(), 0L, channel, name, messages, subscribers);
    }

    private static UserStatus userStatus(Reference<User> user, Reference<Channel> channel) {
        return new UserStatus(
                Reference.create(),
                0L,
                user,
                List.of(
                        new UnreadMessage(Reference.create(), channel),
                        new UnreadMessage(Reference.create(), channel)));
    }
}
