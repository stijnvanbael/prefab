package be.appify.prefab.example.pubsub;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class PubSubExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(PubSubExampleApplication.class, args);
    }
}
