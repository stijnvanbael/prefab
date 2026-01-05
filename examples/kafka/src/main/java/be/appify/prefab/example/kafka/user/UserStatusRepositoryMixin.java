package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import org.springframework.data.jdbc.repository.query.Query;

import java.util.List;

@RepositoryMixin(UserStatus.class)
public interface UserStatusRepositoryMixin {
    @Query("""
            SELECT *
            FROM user_status
            WHERE "user" IN (
                SELECT id FROM "user"
                WHERE EXISTS (
                    SELECT 1
                    FROM UNNEST(channel_subscriptions) AS cs
                    WHERE cs = :channel
                )
            )
            """)
    List<UserStatus> findUserStatusesInChannel(Reference<Channel> channel);
}
