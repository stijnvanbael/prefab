package be.appify.prefab.example.showcase;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Shop application showcasing Prefab. The same sources run on every showcase variant; only the technology
 * dependencies and configuration differ.
 */
@SpringBootApplication
@EnablePrefab
public class ShowcaseApplication {
    /**
     * Starts the showcase application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ShowcaseApplication.class, args);
    }
}
