package stream.pull;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Streaming;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record RecordSession(
        @Id String id,
        @Version long version,
        String title) {

    public RecordSession(String title) {
        this(UUID.randomUUID().toString(), 0L, title);
    }

    @Streaming(path = "/tokens", event = "token")
    public Stream<TokenItem> streamTokenItems() {
        return List.of(new TokenItem("hello"), new TokenItem("world")).stream();
    }
}

