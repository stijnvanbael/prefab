package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.Transient;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrefabJdbcMappingContextTest {

    @Test
    void transientFieldsAreExcludedFromPersistentProperties() {
        var context = new PrefabJdbcMappingContext(new PrefabNamingStrategy());
        var entity = context.getRequiredPersistentEntity(Product.class);
        var propertyNames = new ArrayList<String>();

        entity.doWithProperties(property -> propertyNames.add(property.getName()));

        assertThat(propertyNames)
                .contains("id", "name")
                .doesNotContain("previewToken");
        assertThat(entity.getPersistentProperty("previewToken")).isNull();
    }

    record Product(
            String id,
            String name,
            @Transient String previewToken
    ) {
    }
}
