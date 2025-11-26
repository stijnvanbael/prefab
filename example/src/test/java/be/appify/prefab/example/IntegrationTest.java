package be.appify.prefab.example;

import org.junit.jupiter.api.extension.ExtendWith;

import be.appify.prefab.test.kafka.JsonTestConsumerFactoryAutoConfiguration;
import be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Import({ JsonTestConsumerFactoryAutoConfiguration.class, PubSubTestAutoConfiguration.class })
public @interface IntegrationTest {
}
