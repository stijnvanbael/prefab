package be.appify.prefab.test.asserts;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RestResponseAssertTest {

    @Test
    void idReturnsIdFromLocationHeader() {
        var resultActions = mock(ResultActions.class);
        var assert_ = new RestResponseAssert<>(resultActions, "abc-123", null);

        assertThat(assert_.id()).isEqualTo("abc-123");
    }

    @Test
    void responseReturnsDeserializedBody() {
        var resultActions = mock(ResultActions.class);
        var response = new TestResponse("Alice");
        var assert_ = new RestResponseAssert<>(resultActions, null, response);

        assertThat(assert_.response()).isEqualTo(response);
    }

    @Test
    void resultActionsReturnsWrappedResultActions() {
        var resultActions = mock(ResultActions.class);
        var assert_ = new RestResponseAssert<>(resultActions, "id-1", null);

        assertThat(assert_.resultActions()).isSameAs(resultActions);
    }

    @Test
    void andAssertAppliesConsumerAndReturnsThis() {
        var resultActions = mock(ResultActions.class);
        var assert_ = new RestResponseAssert<>(resultActions, "id-1", new TestResponse("Bob"));

        var returned = assert_.andAssert(ra -> assertThat(ra.id()).isEqualTo("id-1"));

        assertThat(returned).isSameAs(assert_);
    }

    @Test
    void andAssertPropagatesAssertionErrors() {
        var resultActions = mock(ResultActions.class);
        var assert_ = new RestResponseAssert<>(resultActions, "wrong", null);

        assertThatThrownBy(() -> assert_.andAssert(ra -> assertThat(ra.id()).isEqualTo("expected")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void andAssertSupportsMethodChaining() {
        var resultActions = mock(ResultActions.class);
        var assert_ = new RestResponseAssert<>(resultActions, "id-2", new TestResponse("Carol"));

        assert_
                .andAssert(ra -> assertThat(ra.id()).isEqualTo("id-2"))
                .andAssert(ra -> assertThat(ra.response().name()).isEqualTo("Carol"));
    }

    record TestResponse(String name) {
    }
}
