package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;

/**
 * Utility class to hold commonly used ClassName instances for code generation.
 */
public class TestClasses {
    private TestClasses() {}

    /**
     * ClassName for org.springframework.test.web.servlet.MockMvc, used for testing Spring MVC controllers.
     */
    public static final ClassName MOCK_MVC = ClassName.get(
            "org.springframework.test.web.servlet",
            "MockMvc");

    /**
     * ClassName for org.springframework.test.web.servlet.setup.MockMvcBuilders, used to build MockMvc instances.
     */
    public static final ClassName MOCK_MVC_BUILDERS = ClassName.get(
            "org.springframework.test.web.servlet.setup",
            "MockMvcBuilders");

    /**
     * ClassName for org.springframework.test.web.servlet.result.MockMvcResultMatchers, used for asserting results of MockMvc tests.
     */
    public static final ClassName MOCK_MVC_RESULT_MATCHERS = ClassName.get(
            "org.springframework.test.web.servlet.result",
            "MockMvcResultMatchers");

    /**
     * ClassName for org.springframework.test.web.servlet.request.MockMvcRequestBuilders, used to create requests for MockMvc tests.
     */
    public static final ClassName MOCK_MVC_REQUEST_BUILDERS = ClassName.get(
            "org.springframework.test.web.servlet.request",
            "MockMvcRequestBuilders");

    /**
     * ClassName for be.appify.prefab.test.TestUtil, a utility class for testing in the prefab project.
     */
    public static final ClassName TEST_UTIL = ClassName.get(
            "be.appify.prefab.test",
            "TestUtil");

    /**
     * ClassName for org.springframework.mock.web.MockPart, used for testing multipart requests in Spring MVC.
     */
    public static final ClassName MOCK_PART = ClassName.get(
            "org.springframework.mock.web",
            "MockPart");

    /**
     * ClassName for org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers, used to configure MockMvc with Spring Security for testing.
     */
    public static final ClassName SECURITY_MOCK_MVC_CONFIGURERS = ClassName.get(
            "org.springframework.security.test.web.servlet.setup",
            "SecurityMockMvcConfigurers");
}
