package com.mycompany.keycloak.authenticator.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

public class KafkaEventListenerProvider implements EventListenerProvider {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    public KafkaEventListenerProvider(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.LOGIN || event.getType() == EventType.LOGIN_ERROR) {

            String payload = String.format(
                    "{\"type\":\"%s\", \"userId\":\"%s\", \"realm\":\"%s\", \"error\":\"%s\"}",
                    event.getType(),
                    event.getUserId(),
                    event.getRealmId(),
                    event.getError() != null ? event.getError() : "none"
            );

            producer.send(new ProducerRecord<>(topic, event.getUserId(), payload));
        }
    }

    // ✅ Correct method name
    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // You can ignore or log admin events
    }

    @Override
    public void close() {
        // No-op
    }
}