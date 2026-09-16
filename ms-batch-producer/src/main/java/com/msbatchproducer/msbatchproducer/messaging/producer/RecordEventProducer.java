package com.msbatchproducer.msbatchproducer.messaging.producer;
import com.msbatchproducer.msbatchproducer.model.dto.RecordReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
@Component
public class RecordEventProducer {
    private final KafkaTemplate<String, RecordReadyEvent> template;
    private final String topic;
    public RecordEventProducer(KafkaTemplate<String, RecordReadyEvent> template,
                               @Value("${app.kafka.topic.record-ready}") String topic) {
        this.template = template; this.topic = topic;
    }
    public CompletableFuture<SendResult<String, RecordReadyEvent>> publish(RecordReadyEvent event) {
        return template.send(topic, event.recordId().toString(), event);
    }
}
