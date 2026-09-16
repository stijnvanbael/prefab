package be.appify.prefab.test;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SpringSecurityTestSupportTest {

    @Test
    void mockUserPostProcessorIsUsableWithPrefabTestClasspath() {
        assertDoesNotThrow(() -> SecurityMockMvcRequestPostProcessors.user("test"));
    }
}
