package be.appify.prefab.openapi;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenAPI documentation using SpringDoc.
 * Provides a default {@link OpenAPI} bean when none is defined by the application.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnBean(PrefabCoreConfiguration.class)
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiAutoConfiguration {

    /**
     * Creates a default {@link OpenAPI} bean configured with application properties.
     *
     * @param properties
     *         the OpenAPI properties
     * @return the configured OpenAPI instance
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI(OpenApiProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion()));
    }
}
