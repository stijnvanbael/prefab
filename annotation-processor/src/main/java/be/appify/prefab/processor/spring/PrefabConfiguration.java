package be.appify.prefab.processor.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"be.appify.prefab.processor.spring", "be.appify.prefab.processor.problem"})
public class PrefabConfiguration {
}
