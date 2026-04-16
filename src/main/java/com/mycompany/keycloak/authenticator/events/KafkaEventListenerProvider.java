package com.mycompany.keycloak.authenticator.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import java.io.FileWriter;
import java.io.IOException;

public class KafkaEventListenerProvider implements EventListenerProvider {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    public KafkaEventListenerProvider(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void onEvent(Event event) {

        if (event.getType().name().startsWith("LOGIN")) {

            String payload = String.format(
                    "{\"type\":\"%s\", \"userId\":\"%s\", \"realm\":\"%s\", \"error\":\"%s\", \"timestamp\":\"%d\"}",
                    event.getType(),
                    event.getUserId(),
                    event.getRealmId(),
                    event.getError() != null ? event.getError() : "none",
                    System.currentTimeMillis()
            );

            // Step 1: Always save (source of truth)
            saveToFile(payload);

            //  Step 2: Async Kafka send (non-blocking)
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topic, event.getUserId(), payload);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    // Reduced logging (no spam)
                    System.err.println("Kafka unavailable, using fallback");
                } else {
                    System.out.println("Event sent to Kafka");
                }
            });
        }
    }

    private void saveToFile(String event) {
        try (FileWriter fw = new FileWriter("failed-events.log", true)) {
            fw.write(event + "\n");
        } catch (IOException e) {
            System.err.println("File write failed");
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        //
    }

    @Override
    public void close() {
        //
    }
}