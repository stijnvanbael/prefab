package be.appify.prefab.core.util;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TypeaheadQueryTest {

    @Test
    void lastWordBecomesThePrefix() {
        var query = TypeaheadQuery.of("spring  spri");

        assertThat(query.wholeWords()).isEqualTo("spring");
        assertThat(query.prefix()).contains("spri");
    }

    @Test
    void singleWordIsOnlyAPrefix() {
        var query = TypeaheadQuery.of("Sprí9");

        assertThat(query.wholeWords()).isEmpty();
        assertThat(query.prefix()).contains("Sprí9");
    }

    @Test
    void specialCharactersAreNeverPartOfThePrefix() {
        var query = TypeaheadQuery.of("a:* | b & !c'd");

        assertThat(query.prefix()).contains("d");
        assertThat(query.wholeWords()).isEqualTo("a:* | b & !c'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "spring ", "spring!", "spring -boo", "\"spring boo", "spring OR", "spring or"})
    void noPrefixWhenTheLastWordIsNotBeingTyped(String text) {
        var query = TypeaheadQuery.of(text);

        assertThat(query.prefix()).isEqualTo(Optional.empty());
        assertThat(query.wholeWords()).isEqualTo(text);
    }

    @Test
    void closedQuotesDoNotPreventAPrefix() {
        assertThat(TypeaheadQuery.of("\"query plans\" spr").prefix()).contains("spr");
    }

    @Test
    void nullQueryHasNoPrefix() {
        assertThat(TypeaheadQuery.of(null)).isEqualTo(new TypeaheadQuery("", Optional.empty()));
    }
}
