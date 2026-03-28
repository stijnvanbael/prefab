package be.appify.prefab.core.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the default OpenAPI documentation.
 */
@ConfigurationProperties(prefix = "prefab.openapi")
public class OpenApiProperties {

    private String title = "API Documentation";
    private String description = "";
    private String version = "1.0.0";

    /**
     * Returns the API title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the API title.
     *
     * @param title
     *         the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the API description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the API description.
     *
     * @param description
     *         the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the API version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the API version.
     *
     * @param version
     *         the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
}
