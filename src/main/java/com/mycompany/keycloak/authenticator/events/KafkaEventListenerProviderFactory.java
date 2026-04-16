package com.mycompany.keycloak.authenticator.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.util.Properties;

public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    private KafkaProducer<String, String> producer;
    private final String topic = "keycloak-audit-events";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new KafkaEventListenerProvider(producer, topic);
    }

    @Override
    public void init(Config.Scope config) {

        Properties props = new Properties();

        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        //  Reliability
        props.put("acks", "all");
        props.put("retries", 1);

        //  Reduce retry spam
        props.put("retry.backoff.ms", 5000);
        props.put("reconnect.backoff.ms", 5000);
        props.put("reconnect.backoff.max.ms", 10000);

        //
        //  Fast failure (no delay)
        props.put("request.timeout.ms", 2000);
        props.put("delivery.timeout.ms", 3000);
        props.put("max.block.ms", 1000);

        producer = new KafkaProducer<>(props);
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {}

    @Override
    public void close() {
        if (producer != null) {
            producer.close(); // only here ✔
        }
    }

    @Override
    public String getId() {
        return "kafka-event-listener";
    }
}