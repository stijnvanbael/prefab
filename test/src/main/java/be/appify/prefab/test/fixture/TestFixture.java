package be.appify.prefab.test.fixture;

import org.springframework.test.web.servlet.ResultActions;

public class TestFixture {
    public static String idOf(ResultActions result) {
        var location = result.andReturn().getResponse().getHeader("Location");
        assert location != null;
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
