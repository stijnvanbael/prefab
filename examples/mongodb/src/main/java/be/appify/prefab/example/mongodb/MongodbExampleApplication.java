package be.appify.prefab.example.mongodb;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the MongoDB example application. */
@SpringBootApplication
@EnablePrefab
public class MongodbExampleApplication {

    /** Starts the application. */
    public static void main(String[] args) {
        SpringApplication.run(MongodbExampleApplication.class, args);
    }
}
