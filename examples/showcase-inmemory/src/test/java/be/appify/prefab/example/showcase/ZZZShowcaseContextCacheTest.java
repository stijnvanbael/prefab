package be.appify.prefab.example.showcase;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;

import static org.assertj.core.api.Assertions.assertThat;

class ZZZShowcaseContextCacheTest {

    @Test
    void showcaseIntegrationTestsReuseOneApplicationContext() {
        var contextCache = new ExposedContextLoaderDelegate().contextCache();

        assertThat(contextCache.getMissCount())
                .as("Spring application contexts started by the showcase integration tests")
                .isEqualTo(1);
    }

    private static final class ExposedContextLoaderDelegate extends DefaultCacheAwareContextLoaderDelegate {

        private ContextCache contextCache() {
            return getContextCache();
        }
    }
}
