package com.msbatchproducer.msbatchproducer.batch;
import com.msbatchproducer.msbatchproducer.batch.writer.InputRecordWriter;
import com.msbatchproducer.msbatchproducer.batch.processor.InputRecordProcessor;
import com.msbatchproducer.msbatchproducer.model.dto.XmlInput;
import com.msbatchproducer.msbatchproducer.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.batch.item.Chunk;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties = {"app.outbox.enabled=false", "spring.kafka.admin.auto-create=false"})
class AtomicPersistenceTest {
    @Autowired InputRecordWriter writer;
    @Autowired IBatchRecordRepository records;
    @Autowired OutboxRepository outbox;
    @Autowired PlatformTransactionManager tx;
    @Test void rollbackRemovesBothRecordAndEvent() throws Exception {
        var record = new InputRecordProcessor("rollback-test").process(new XmlInput(1, "x.xml", "<root/>"));
        var transaction = new TransactionTemplate(tx);
        transaction.executeWithoutResult(status -> {
            writer.write(new Chunk<>(List.of(record)));
            assertThat(records.countByUploadId("rollback-test")).isEqualTo(1);
            assertThat(outbox.countByUploadIdAndPublishedAtIsNull("rollback-test")).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(records.countByUploadId("rollback-test")).isZero();
        assertThat(outbox.countByUploadIdAndPublishedAtIsNull("rollback-test")).isZero();
    }
}
