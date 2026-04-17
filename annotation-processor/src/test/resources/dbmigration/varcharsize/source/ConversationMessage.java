package dbmigration.varcharsize;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record ConversationMessage(
        @Id Reference<ConversationMessage> id,
        @Version long version,
        String conversationId,
        int position,
        String role,
        @Size(max = 65535) String content
) {
    public ConversationMessage(String conversationId, int position, String role, String content) {
        this(Reference.create(), 0L, conversationId, position, role, content);
    }
}

