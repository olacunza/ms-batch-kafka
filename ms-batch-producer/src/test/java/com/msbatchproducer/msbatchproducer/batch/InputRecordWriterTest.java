package com.msbatchproducer.msbatchproducer.batch;
import com.msbatchproducer.msbatchproducer.batch.writer.InputRecordWriter;
import com.msbatchproducer.msbatchproducer.model.entity.*;
import com.msbatchproducer.msbatchproducer.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class InputRecordWriterTest {
    @Mock IBatchRecordRepository records;
    @Mock OutboxRepository outbox;
    @Mock EntityManager em;
    @InjectMocks InputRecordWriter writer;
    @Test void savesRecordAndOutboxBeforeFlush() {
        var record = new BatchRecord(); record.setId(7L); record.setBusinessKey(1L); record.setUploadId("u"); record.setStatus(com.msbatchproducer.msbatchproducer.model.enums.RecordStatus.PENDING);
        when(records.saveAll(anyList())).thenReturn(List.of(record));
        writer.write(new Chunk<>(List.of(record)));
        ArgumentCaptor<List<OutboxEvent>> events = ArgumentCaptor.forClass(List.class);
        var order = inOrder(records, outbox, em);
        order.verify(records).saveAll(anyList()); order.verify(outbox).saveAll(events.capture());
        order.verify(em).flush(); order.verify(em).clear();
        assertThat(events.getValue()).singleElement().satisfies(e -> {
            assertThat(e.getRecordId()).isEqualTo(7); assertThat(e.getEventId()).isNotBlank();
        });
    }
    @Test void persistenceFailureDoesNotProduceOutbox() {
        when(records.saveAll(anyList())).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(new BatchRecord())))).hasMessage("db down");
        verifyNoInteractions(outbox);
    }
}
