package com.msmonitoreo.msmonitoreo;
import com.msmonitoreo.msmonitoreo.repository.IMonitoreoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional
class MonitoreoRepositoryTest {
    @Autowired IMonitoreoRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Test void emptyTableHasZeroCounts() {
        var result=repository.summary();assertEquals(0,result.total());assertEquals(0,result.pending());assertEquals(0,result.processed());assertEquals(0,result.failed());
    }
    @Test void aggregatesAllStatusesInDatabase() {
        jdbc.update("insert into batch_record(id,status) values(1,'PENDING'),(2,'PROCESSED'),(3,'PROCESSED'),(4,'FAILED')");
        var result=repository.summary();assertEquals(4,result.total());assertEquals(1,result.pending());assertEquals(2,result.processed());assertEquals(1,result.failed());
    }
    @Test void executionsArePagedAndJobZeroIsPreserved() {
        jdbc.update("insert into BATCH_JOB_EXECUTION(JOB_EXECUTION_ID,STATUS) values(0,'COMPLETED'),(1,'STARTED')");
        var first=repository.executions(0,1);assertEquals(2,first.totalElements());assertEquals(2,first.totalPages());assertEquals(1L,first.content().getFirst().jobExecutionId());
        assertEquals(0L,repository.executions(1,1).content().getFirst().jobExecutionId());
        assertTrue(repository.executions(2,1).content().isEmpty());
    }
}
