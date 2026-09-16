package com.msbatchproducer.msbatchproducer.messaging;
import com.msbatchproducer.msbatchproducer.messaging.producer.*;
import com.msbatchproducer.msbatchproducer.service.OutboxStore;
import com.msbatchproducer.msbatchproducer.model.entity.OutboxEvent;
import com.msbatchproducer.msbatchproducer.model.dto.RecordReadyEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {
    @Mock OutboxStore store;
    @Mock RecordEventProducer producer;
    OutboxEvent event() {
        var e = new OutboxEvent(); e.setId(1L); e.setEventId("stable-id"); e.setRecordId(2L);
        e.setBusinessKey(1L); e.setUploadId("u"); e.setCreatedAt(LocalDateTime.now()); return e;
    }
    @Test void marksOnlyAcknowledgedMessagesPublished() {
        when(store.pending(100)).thenReturn(List.of(event()));
        when(producer.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        new OutboxPublisher(store, producer, 100).publishPending();
        verify(store).finish(argThat(m -> m.containsKey(1L) && m.get(1L) == null));
    }
    @Test void failedDeliveryKeepsStableEventIdForRetry() {
        when(store.pending(100)).thenReturn(List.of(event()));
        when(producer.publish(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        var publisher = new OutboxPublisher(store, producer, 100); publisher.publishPending(); publisher.publishPending();
        verify(store, times(2)).finish(argThat(m -> m.get(1L).contains("broker down")));
        ArgumentCaptor<RecordReadyEvent> messages = ArgumentCaptor.forClass(RecordReadyEvent.class);
        verify(producer, times(2)).publish(messages.capture());
        assertThat(messages.getAllValues()).extracting(RecordReadyEvent::eventId).containsExactly("stable-id", "stable-id");
    }
}
