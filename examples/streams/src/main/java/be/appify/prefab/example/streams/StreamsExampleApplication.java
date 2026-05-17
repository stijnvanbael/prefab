package be.appify.prefab.example.streams;

import be.appify.prefab.core.spring.EnablePrefab;
import be.appify.prefab.streams.EnablePrefabStreams;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
@EnablePrefabStreams
public class StreamsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamsExampleApplication.class, args);
    }
}

