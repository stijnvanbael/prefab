package be.appify.prefab.example.todo;

import be.appify.prefab.processor.spring.PrefabConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
// TODO: Make sure all of this is imported automatically by the prefab starter
@Import(PrefabConfiguration.class)
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
