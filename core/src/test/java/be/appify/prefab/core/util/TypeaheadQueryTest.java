package be.appify.prefab.core.util;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TypeaheadQueryTest {

    @Test
    void everyWordOfAPlainTextIsAPrefix() {
        var query = TypeaheadQuery.of("spr  bo G");

        assertThat(query.wholeWords()).isEmpty();
        assertThat(query.prefixes()).containsExactly("spr", "bo", "G");
    }

    @Test
    void singleWordIsOnlyAPrefix() {
        var query = TypeaheadQuery.of("Sprí9");

        assertThat(query.wholeWords()).isEmpty();
        assertThat(query.prefixes()).containsExactly("Sprí9");
    }

    @Test
    void lastWordIsTheOnlyPrefixWhenTheTextUsesWebSearchSyntax() {
        var query = TypeaheadQuery.of("\"query plans\" spr");

        assertThat(query.wholeWords()).isEqualTo("\"query plans\"");
        assertThat(query.prefixes()).containsExactly("spr");
    }

    @Test
    void specialCharactersAreNeverPartOfAPrefix() {
        var query = TypeaheadQuery.of("a:* | b & !c'd");

        assertThat(query.prefixes()).allMatch(prefix -> prefix.matches("[\\p{L}\\p{N}]+"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "!!", "spring -boo", "\"spring boo", "spring OR", "spring or", "spring \"boot\" "})
    void noPrefixWhenTheLastWordIsNotBeingTypedInWebSearchSyntax(String text) {
        var query = TypeaheadQuery.of(text);

        assertThat(query.prefixes()).isEmpty();
        assertThat(query.wholeWords()).isEqualTo(text);
    }

    @Test
    void initialsAreOnlyForASingleWordOfTwoCharactersOrMore() {
        assertThat(TypeaheadQuery.of("SBG").initials()).contains("sbg");
        assertThat(TypeaheadQuery.of("s").initials()).isEmpty();
        assertThat(TypeaheadQuery.of("sb g").initials()).isEmpty();
    }

    @Test
    void valueRegexMatchesWordsInAnyOrderOrInitials() {
        var regex = Pattern.compile(TypeaheadQuery.valueRegex("bo spr"), Pattern.CASE_INSENSITIVE);
        var initials = Pattern.compile(TypeaheadQuery.valueRegex("sbg"), Pattern.CASE_INSENSITIVE);

        assertThat(regex.matcher("Spring Boot guide").find()).isTrue();
        assertThat(regex.matcher("Spring Post").find()).isFalse();
        assertThat(initials.matcher("Spring Boot guide").find()).isTrue();
        assertThat(initials.matcher("Postgres tuning").find()).isFalse();
    }

    @Test
    void nullQueryHasNoPrefix() {
        assertThat(TypeaheadQuery.of(null)).isEqualTo(new TypeaheadQuery("", List.of()));
        assertThat(TypeaheadQuery.valueRegex(null)).isEmpty();
    }
}
