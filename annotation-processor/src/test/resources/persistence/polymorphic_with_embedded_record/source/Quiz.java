package persistence.polymorphic_with_embedded_record;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.service.Reference;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public sealed interface Quiz permits Quiz.Assessment {

    record Score(double score, double max) {
    }

    record TimeSpan(Instant start, @Nullable Instant end) {
    }

    record Question(String text) {
    }

    record Assessment(
            @Id Reference<Quiz> id,
            @Version long version,
            Score score,
            @Nullable TimeSpan timeSpan,
            List<Question> questions
    ) implements Quiz {
    }
}

