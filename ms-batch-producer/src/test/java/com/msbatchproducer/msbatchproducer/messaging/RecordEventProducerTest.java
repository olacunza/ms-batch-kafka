package com.msbatchproducer.msbatchproducer.messaging;
import com.msbatchproducer.msbatchproducer.messaging.producer.RecordEventProducer;
import com.msbatchproducer.msbatchproducer.model.dto.RecordReadyEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class RecordEventProducerTest {
    @Test void sendsObjectWithRecordIdKeyAndExposesFailure() {
        KafkaTemplate<String, RecordReadyEvent> kafka = mock(KafkaTemplate.class);
        var event = new RecordReadyEvent("e",2,42L,1L,"u",LocalDateTime.now());
        when(kafka.send("topic", "42", event)).thenReturn(CompletableFuture.failedFuture(new RuntimeException("offline")));
        var future = new RecordEventProducer(kafka,"topic").publish(event);
        assertThat(future).isCompletedExceptionally();
        verify(kafka).send("topic","42",event);
    }
}
