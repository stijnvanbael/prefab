package be.appify.prefab.example.showcase;

import be.appify.prefab.example.showcase.catalog.Category;
import be.appify.prefab.example.showcase.catalog.application.CategoryRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@AutoConfiguration
@Profile("local-demo")
public class ShowcaseLocalDemoData {

    /** Constructs the showcase's test-scoped local demo configuration. */
    public ShowcaseLocalDemoData() {
    }

    @Bean
    ApplicationRunner seedDemoCategory(CategoryRepository categories) {
        return arguments -> {
            if (categories.count() == 0) {
                categories.save(new Category("Demo"));
            }
        };
    }
}
