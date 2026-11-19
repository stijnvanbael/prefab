package be.appify.prefab.example.kafka.dashboard;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.view.ViewContext;
import be.appify.prefab.example.kafka.channel.Channel;
import be.appify.prefab.example.kafka.channelsummary.ChannelSummary;
import be.appify.prefab.example.kafka.user.UnreadMessage;
import be.appify.prefab.example.kafka.user.User;
import be.appify.prefab.example.kafka.user.UserStatus;
import be.appify.prefab.example.kafka.user.application.UserStatusRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

class ChannelDashboardAssemblerTest {
    private final UserStatusRepository userStatusRepository = mock(UserStatusRepository.class);
    private final ChannelDashboardAssembler assembler = new ChannelDashboardAssembler(userStatusRepository);

    @Test
    void assemble_composesOneBatchWithOneStatusLookup() {
        var user = Reference.<User>fromId("user-1");
        var channelWithUnreadMessages = Reference.<Channel>fromId("channel-1");
        var channelWithoutUnreadMessages = Reference.<Channel>fromId("channel-2");
        var summaries = List.of(
                summary(channelWithUnreadMessages, "General", 4, 2),
                summary(channelWithoutUnreadMessages, "News", 7, 3));
        when(userStatusRepository.findByUser(user))
                .thenReturn(List.of(userStatus(user, channelWithUnreadMessages)));

        var items = assembler.assemble(summaries, ViewContext.of(() -> user.id(), null));

        assertThat(items).containsExactly(
                new ChannelDashboardItem(channelWithUnreadMessages, "General", 4, 2, 2, true),
                new ChannelDashboardItem(channelWithoutUnreadMessages, "News", 7, 3, 0, true));
        verify(userStatusRepository).findByUser(user);
    }

    @Test
    void assemble_marksMissingStatusProjectionUnavailable() {
        var user = Reference.<User>fromId("user-1");
        when(userStatusRepository.findByUser(user)).thenReturn(List.of());

        var item = assembler.assemble(
                List.of(summary(Reference.<Channel>fromId("channel-1"), "General", 1, 1)),
                ViewContext.of(() -> user.id(), null)).getFirst();

        assertThat(item.unreadMessages()).isZero();
        assertThat(item.unreadStatusAvailable()).isFalse();
    }

    @Test
    void assemble_marksEmptyButPresentStatusProjectionAvailable() {
        var user = Reference.<User>fromId("user-1");
        when(userStatusRepository.findByUser(user))
                .thenReturn(List.of(new UserStatus(Reference.create(), 0L, user, List.of())));

        var item = assembler.assemble(
                List.of(summary(Reference.fromId("channel-1"), "General", 1, 1)),
                ViewContext.of(() -> user.id(), null)).getFirst();

        assertThat(item.unreadMessages()).isZero();
        assertThat(item.unreadStatusAvailable()).isTrue();
    }

    @Test
    void assemble_withoutPrincipalDoesNotQueryStatus() {
        var item = assembler.assemble(
                List.of(summary(Reference.<Channel>fromId("channel-1"), "General", 1, 1)),
                ViewContext.of(null, null)).getFirst();

        assertThat(item.unreadMessages()).isZero();
        assertThat(item.unreadStatusAvailable()).isFalse();
        verify(userStatusRepository, never()).findByUser(org.mockito.ArgumentMatchers.any());
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
