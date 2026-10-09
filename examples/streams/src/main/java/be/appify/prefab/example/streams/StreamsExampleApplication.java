package be.appify.prefab.example.streams;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class StreamsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamsExampleApplication.class, args);
    }
}

