package be.appify.prefab.example.avro;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class AvroExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvroExampleApplication.class, args);
    }
}
