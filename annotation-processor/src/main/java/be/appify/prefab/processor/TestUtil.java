package be.appify.prefab.processor;

import org.springframework.test.web.servlet.ResultActions;

/**
 * Utility class for test-related operations.
 */
public class TestUtil {

    private TestUtil() {
    }

    /**
     * Extracts the ID from the "Location" header of a ResultActions object.
     *
     * @param result the ResultActions object containing the response
     * @return the extracted ID from the "Location" header
     */
    public static String idOf(ResultActions result) {
        var location = result.andReturn().getResponse().getHeader("Location");
        assert location != null;
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
