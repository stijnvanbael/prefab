package be.appify.prefab.processor;

import org.springframework.test.web.servlet.ResultActions;

public class TestUtil {
    public static String idOf(ResultActions result) {
        var location = result.andReturn().getResponse().getHeader("Location");
        assert location != null;
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
