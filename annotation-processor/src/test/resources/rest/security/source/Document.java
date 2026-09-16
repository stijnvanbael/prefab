package rest.security;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById(security = @Security(role = "READER"))
@GetList(security = @Security(enabled = false))
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Document(
        @Id String id,
        @Version long version,
        @Autocomplete(security = @Security(role = "SEARCHER")) String title,
        @Download(security = @Security(authority = "ROLE_ATTACHMENT_READ")) Binary attachment
) {
    @Create(security = @Security(authority = "ROLE_EDITOR"))
    public Document(String title, Binary attachment) {
        this(UUID.randomUUID().toString(), 0L, title, attachment);
    }

    @Update(security = @Security(role = "EDITOR"))
    public Document rename(String title) {
        return new Document(id, version, title, attachment);
    }

    @Streaming(path = "/updates", security = @Security(authority = "ROLE_STREAM"))
    public Stream<String> updates() {
        return Stream.of(title);
    }
}
