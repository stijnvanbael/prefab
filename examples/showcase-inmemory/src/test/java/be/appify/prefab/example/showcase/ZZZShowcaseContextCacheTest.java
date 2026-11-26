package be.appify.prefab.example.showcase;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;

class ZZZShowcaseContextCacheTest {

    @Test
    void showcaseIntegrationTestsReuseOneApplicationContext() throws ClassNotFoundException {
        var showcaseContext = contextFor(ShowcaseSecurityAndDocumentationIntegrationTest.class);
        var orderingContext = contextFor(Class.forName(
                "be.appify.prefab.example.showcase.ordering.OrderOverviewIntegrationTest"));

        assertThat(showcaseContext)
                .as("Regular showcase integration tests should reuse the same Spring application context")
                .isSameAs(orderingContext);
    }

    private static ApplicationContext contextFor(Class<?> testClass) {
        return new TestContextManager(testClass).getTestContext().getApplicationContext();
    }
}
