package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

/** Makes this package the application package that is scanned for repositories. */
@Configuration
@AutoConfigurationPackage
public class FixtureConfiguration {
}
