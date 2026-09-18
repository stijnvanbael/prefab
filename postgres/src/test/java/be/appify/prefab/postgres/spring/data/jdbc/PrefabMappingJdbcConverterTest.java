package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.DbDocument;
import be.appify.prefab.core.annotations.Transient;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PrefabMappingJdbcConverterTest {
    private final PrefabMappingJdbcConverter converter = new PrefabMappingJdbcConverter(
            new JdbcMappingContext(),
            mock(RelationResolver.class),
            new JdbcCustomConversions(),
            mock(JdbcTypeFactory.class),
            JsonMapper.builder().build());

    @Test
    void computedFieldsAreNotStoredInJsonbDocuments() {
        var jsonb = (PGobject) converter.writeValue(new Money(BigDecimal.TEN, "EUR"), TypeInformation.of(Money.class));

        assertThat(jsonb.getValue())
                .contains("\"currency\"")
                .doesNotContain("display");
    }

    @Test
    void jsonbDocumentsWithComputedFieldsAreReadIgnoringTheComputedValue() throws Exception {
        var stored = new PGobject();
        stored.setType("jsonb");
        stored.setValue("""
                {"amount":10,"currency":"EUR","display":"tampered"}""");

        var money = converter.readValue(stored, TypeInformation.of(Money.class));

        assertThat(money).isEqualTo(new Money(BigDecimal.valueOf(10), "EUR"));
    }

    @Test
    void transientFieldsAreNotStoredInJsonbDocuments() {
        var jsonb = (PGobject) converter.writeValue(new Snapshot("draft", "preview"), TypeInformation.of(Snapshot.class));

        assertThat(jsonb.getValue())
                .contains("\"status\"")
                .doesNotContain("previewToken");
    }

    @Test
    void jsonbDocumentsWithTransientFieldsAreReadIgnoringTheTransientValue() throws Exception {
        var stored = new PGobject();
        stored.setType("jsonb");
        stored.setValue("""
                {"status":"draft","previewToken":"tampered"}""");

        var snapshot = converter.readValue(stored, TypeInformation.of(Snapshot.class));

        assertThat(snapshot).isEqualTo(new Snapshot("draft", null));
    }

    @DbDocument
    record Money(BigDecimal amount, String currency) {
        @Computed
        public String display() {
            return "%s %s".formatted(amount, currency);
        }
    }

    @DbDocument
    record Snapshot(
            String status,
            @Transient String previewToken
    ) {
    }
}
