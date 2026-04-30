package event.handler.multicastcreateorupdate;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import java.util.List;

@RepositoryMixin(Channel.class)
public interface ChannelRepositoryMixin {
    List<Channel> findByChannel(Reference<Channel> channel);
}
