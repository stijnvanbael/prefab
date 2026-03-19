package be.appify.prefab.example.sns;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class SnsSqsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SnsSqsApplication.class, args);
    }
}
