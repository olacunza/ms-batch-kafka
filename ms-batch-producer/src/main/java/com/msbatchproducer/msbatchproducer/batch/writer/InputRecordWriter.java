package com.msbatchproducer.msbatchproducer.batch.writer;

import com.msbatchproducer.msbatchproducer.model.entity.*;
import com.msbatchproducer.msbatchproducer.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Component @RequiredArgsConstructor
public class InputRecordWriter implements ItemWriter<BatchRecord> {
    private final IBatchRecordRepository records;
    private final OutboxRepository outbox;
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void write(Chunk<? extends BatchRecord> chunk) {
        if (chunk.isEmpty()) return;
        var saved = records.saveAll(new ArrayList<>(chunk.getItems()));
        List<OutboxEvent> events = new ArrayList<>(saved.size());
        for (var record : saved) {
            if (record.getStatus() != com.msbatchproducer.msbatchproducer.model.enums.RecordStatus.PENDING) continue;
            OutboxEvent event = new OutboxEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setRecordId(record.getId()); event.setBusinessKey(record.getBusinessKey());
            event.setUploadId(record.getUploadId()); event.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            events.add(event);
        }
        outbox.saveAll(events);
        entityManager.flush();
        entityManager.clear();
    }
}
