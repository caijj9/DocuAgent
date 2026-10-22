package com.docuagent.docuagent.metrics;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
@Configuration
public class UsageConfig {
    @Bean @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry observationRegistry(UsageObservationHandler handler){var registry=ObservationRegistry.create();registry.observationConfig().observationHandler(handler);return registry;}
}
