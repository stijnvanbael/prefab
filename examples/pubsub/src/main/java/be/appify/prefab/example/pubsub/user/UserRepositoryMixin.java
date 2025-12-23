package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;
import org.springframework.data.jdbc.repository.query.Query;

import java.util.List;

@RepositoryMixin(User.class)
public interface UserRepositoryMixin {
    @Query("""
            SELECT *
            FROM "user"
            WHERE EXISTS (
                SELECT 1
                FROM UNNEST(channel_subscriptions) AS cs
                WHERE cs = :channel
            )
            """)
    List<User> findUsersInChannel(Reference<Channel> channel);
}
