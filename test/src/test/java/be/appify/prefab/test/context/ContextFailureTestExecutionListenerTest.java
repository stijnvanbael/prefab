package be.appify.prefab.test.context;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextFailureTestExecutionListenerTest {

    private final ContextFailureTestExecutionListener listener = new ContextFailureTestExecutionListener();

    @Test
    void prepareTestInstance_withLoadedContext_doesNothing() {
        var testContext = mock(TestContext.class);
        when(testContext.getTestClass()).thenAnswer(i -> FirstTest.class);

        listener.prepareTestInstance(testContext);
    }

    @Test
    void prepareTestInstance_afterContextFailure_repeatsOriginalCause() {
        var original = new IllegalStateException("Failed to load ApplicationContext");
        var genericFailure = new IllegalStateException("ApplicationContext failure threshold (1) exceeded");

        var first = failingContext(FirstTest.class, original);
        var second = failingContext(SecondTest.class, genericFailure);

        assertThatThrownBy(() -> listener.prepareTestInstance(first)).isSameAs(original);
        assertThatThrownBy(() -> listener.prepareTestInstance(second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SecondTest.class.getName())
                .hasMessageContaining("Failed to load ApplicationContext")
                .hasCause(original);
    }

    @Test
    void prepareTestInstance_withDifferentContextConfiguration_doesNotReuseFailure() {
        var failure = new IllegalStateException("other failure");

        assertThatThrownBy(() -> listener.prepareTestInstance(failingContext(OtherConfigTest.class, failure)))
                .isSameAs(failure);
    }

    private static TestContext failingContext(Class<?> testClass, RuntimeException failure) {
        var testContext = mock(TestContext.class);
        when(testContext.getTestClass()).thenAnswer(i -> testClass);
        when(testContext.getApplicationContext()).thenThrow(failure);
        return testContext;
    }

    @ContextConfiguration(classes = Object.class)
    static class FirstTest {
    }

    @ContextConfiguration(classes = Object.class)
    static class SecondTest {
    }

    @ContextConfiguration(classes = String.class)
    static class OtherConfigTest {
    }
}
