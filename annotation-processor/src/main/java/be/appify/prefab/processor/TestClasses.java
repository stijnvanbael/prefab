package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;

public class TestClasses {
    private TestClasses() {}

    public static final ClassName MOCK_MVC = ClassName.get(
            "org.springframework.test.web.servlet",
            "MockMvc");

    public static final ClassName MOCK_MVC_BUILDERS = ClassName.get(
            "org.springframework.test.web.servlet.setup",
            "MockMvcBuilders");

    public static final ClassName MOCK_MVC_RESULT_MATCHERS = ClassName.get(
            "org.springframework.test.web.servlet.result",
            "MockMvcResultMatchers");

    public static final ClassName MOCK_MVC_REQUEST_BUILDERS = ClassName.get(
            "org.springframework.test.web.servlet.request",
            "MockMvcRequestBuilders");

    public static final ClassName TEST_UTIL = ClassName.get(
            "be.appify.prefab.test",
            "TestUtil");

    public static final ClassName MOCK_PART = ClassName.get(
            "org.springframework.mock.web",
            "MockPart");

    public static final ClassName SECURITY_MOCK_MVC_CONFIGURERS = ClassName.get(
            "org.springframework.security.test.web.servlet.setup",
            "SecurityMockMvcConfigurers");
}
