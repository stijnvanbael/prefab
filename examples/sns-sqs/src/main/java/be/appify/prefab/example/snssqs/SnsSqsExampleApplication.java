package be.appify.prefab.example.snssqs;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class SnsSqsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SnsSqsExampleApplication.class, args);
    }
}
