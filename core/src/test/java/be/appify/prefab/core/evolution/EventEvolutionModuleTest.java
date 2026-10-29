package be.appify.prefab.core.evolution;

import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.PreviousName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class EventEvolutionModuleTest {
    private final JsonMapper mapper = JsonMapper.builder().addModule(new EventEvolutionModule()).build();

    enum Status {PENDING, ACTIVE}

    record Added(
            String name,
            @Default("unknown") String label,
            @Default("7") int count,
            @Default("2") Long total,
            @Default("1.5") double ratio,
            @Default("true") boolean active,
            @Default("ACTIVE") Status status,
            @Default("[]") List<String> tags
    ) {
    }

    record Renamed(@PreviousName({"fullName", "title"}) String name, int age) {
    }

    record RenamedWithDefault(@PreviousName("oldName") @Default("fallback") String name) {
    }

    record Plain(String name, Integer count) {
    }

    record Wrapper(Renamed inner, List<Added> items) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    sealed interface Shape permits Circle, Square {
    }

    @PreviousName({"Disc", "be.appify.old.Round"})
    record Circle(@Default("1") int radius) implements Shape {
    }

    record Square(int side) implements Shape {
    }

    @Test
    @DisplayName("a missing field takes its declared default")
    void addedFieldDefault() {
        var added = mapper.readValue("{\"name\":\"a\"}", Added.class);

        assertThat(added).isEqualTo(new Added("a", "unknown", 7, 2L, 1.5, true, Status.ACTIVE, List.of()));
    }

    @Test
    @DisplayName("a present field is not replaced by its default")
    void presentFieldWins() {
        var added = mapper.readValue(
                "{\"name\":\"a\",\"label\":\"x\",\"count\":1,\"total\":3,\"ratio\":0.5,\"active\":false,"
                        + "\"status\":\"PENDING\",\"tags\":[\"t\"]}", Added.class);

        assertThat(added).isEqualTo(new Added("a", "x", 1, 3L, 0.5, false, Status.PENDING, List.of("t")));
    }

    @Test
    @DisplayName("a field is read from its previous name")
    void previousFieldName() {
        assertThat(mapper.readValue("{\"fullName\":\"a\",\"age\":3}", Renamed.class)).isEqualTo(new Renamed("a", 3));
        assertThat(mapper.readValue("{\"title\":\"b\",\"age\":3}", Renamed.class)).isEqualTo(new Renamed("b", 3));
    }

    @Test
    @DisplayName("the current name takes precedence over a previous name")
    void currentNameWins() {
        var renamed = mapper.readValue("{\"name\":\"new\",\"fullName\":\"old\",\"age\":3}", Renamed.class);

        assertThat(renamed.name()).isEqualTo("new");
    }

    @Test
    @DisplayName("a renamed field with a default prefers the old value over the default")
    void renamedFieldWithDefault() {
        assertThat(mapper.readValue("{\"oldName\":\"a\"}", RenamedWithDefault.class).name()).isEqualTo("a");
        assertThat(mapper.readValue("{}", RenamedWithDefault.class).name()).isEqualTo("fallback");
    }

    @Test
    @DisplayName("evolution applies to nested records and list elements")
    void nestedEvolution() {
        var wrapper = mapper.readValue(
                "{\"inner\":{\"fullName\":\"n\",\"age\":1},\"items\":[{\"name\":\"i\"}]}", Wrapper.class);

        assertThat(wrapper.inner()).isEqualTo(new Renamed("n", 1));
        assertThat(wrapper.items().getFirst().label()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("types without declarations behave as before")
    void plainRecordUnchanged() {
        assertThat(mapper.readValue("{\"name\":\"a\"}", Plain.class)).isEqualTo(new Plain("a", null));
        assertThat(mapper.readValue("{\"name\":\"a\",\"count\":2}", Plain.class)).isEqualTo(new Plain("a", 2));
    }

    @Test
    @DisplayName("polymorphic subtypes are read under the current and the previous type id")
    void previousTypeId() {
        assertThat(mapper.readValue("{\"@type\":\"EventEvolutionModuleTest$Circle\",\"radius\":2}", Shape.class)).isEqualTo(new Circle(2));
        assertThat(mapper.readValue("{\"@type\":\"Disc\",\"radius\":2}", Shape.class)).isEqualTo(new Circle(2));
        assertThat(mapper.readValue("{\"@type\":\"be.appify.old.Round\"}", Shape.class)).isEqualTo(new Circle(1));
        assertThat(mapper.readValue("{\"@type\":\"EventEvolutionModuleTest$Square\",\"side\":4}", Shape.class)).isEqualTo(new Square(4));
    }

    @Test
    @DisplayName("an unknown type id is still rejected")
    void unknownTypeId() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> mapper.readValue("{\"@type\":\"Triangle\"}", Shape.class))
                .hasMessageContaining("Triangle");
    }

    @Test
    @DisplayName("events are written under their current names")
    void writesCurrentNames() {
        assertThat(mapper.writeValueAsString(new Renamed("a", 1))).isEqualTo("{\"name\":\"a\",\"age\":1}");
        assertThat(mapper.writeValueAsString(new Circle(2))).doesNotContain("Disc");
    }
}
