package be.appify.prefab.example.streams;

import be.appify.prefab.core.kafka.GenericKafkaProducer;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import be.appify.prefab.test.asserts.EventAssertions;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@IntegrationTest
class StreamsExampleApplicationTest {
    @Autowired
    GenericKafkaProducer kafkaProducer;
}
