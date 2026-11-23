package com.example;

import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
