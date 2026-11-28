package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.TestContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PubSubTestExecutionListenerTest {

    private final PubSubTestExecutionListener listener = new PubSubTestExecutionListener();

    @Test
    void containersMode_resetsPubSubSubscriptionsBeforeEachTest() {
        var pubSubUtil = mock(PubSubUtil.class);
        var context = context(pubSubUtil, "true");

        listener.prepareTestInstance(context);

        verify(pubSubUtil).resetSubscriptions();
    }

    @Test
    void inMemoryMode_doesNotResetAnything() {
        var pubSubUtil = mock(PubSubUtil.class);
        var context = context(pubSubUtil, "false");

        listener.prepareTestInstance(context);

        verify(pubSubUtil, never()).resetSubscriptions();
    }

    @SuppressWarnings("unchecked")
    private static TestContext context(PubSubUtil pubSubUtil, String containers) {
        var environment = new MockEnvironment().withProperty("prefab.test.containers", containers);
        var applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        ObjectProvider<PubSubUtil> provider = mock(ObjectProvider.class);
        doAnswer(invocation -> {
            var consumer = invocation.getArgument(0, java.util.function.Consumer.class);
            consumer.accept(pubSubUtil);
            return null;
        }).when(provider).ifAvailable(any());
        when(applicationContext.getBeanProvider(eq(PubSubUtil.class))).thenReturn(provider);
        var testContext = mock(TestContext.class);
        when(testContext.getApplicationContext()).thenReturn(applicationContext);
        return testContext;
    }
}
