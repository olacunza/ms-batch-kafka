package com.msbatchproducer.msbatchproducer.messaging.producer;

import com.msbatchproducer.msbatchproducer.model.dto.RecordReadyEvent;
import com.msbatchproducer.msbatchproducer.service.OutboxStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

@Component @Slf4j
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxStore store;
    private final RecordEventProducer producer;
    private final int batchSize;
    public OutboxPublisher(OutboxStore store, RecordEventProducer producer,
                           @Value("${app.outbox.batch-size:100}") int batchSize) {
        this.store = store; this.producer = producer; this.batchSize = batchSize;
    }
    // A single relay per application; duplicate deliveries across crashes/replicas are allowed.
    @Scheduled(fixedDelayString = "${app.outbox.delay-ms:500}")
    public void publishPending() {
        var events = store.pending(batchSize);
        if (events.isEmpty()) return;
        Map<Long, String> results = Collections.synchronizedMap(new HashMap<>());
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var event : events) {
            try {
                var message = new RecordReadyEvent(event.getEventId(), 2, event.getRecordId(),
                    event.getBusinessKey(), event.getUploadId(), event.getCreatedAt());
                futures.add(producer.publish(message).handle((ok, failure) -> {
                    results.put(event.getId(), failure == null ? null : failure.toString());
                    return null;
                }));
            } catch (Exception e) {
                results.put(event.getId(), e.toString());
                // Metadata/buffer failures can block send(); retry remaining rows next cycle.
                break;
            }
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        store.finish(results);
        long failures = results.values().stream().filter(Objects::nonNull).count();
        if (failures > 0) log.warn("Outbox: {} envíos fallidos; se reintentarán", failures);
    }
}
