package com.msbatchproducer.msbatchproducer.service;
import com.msbatchproducer.msbatchproducer.service.impl.BatchJobServiceImpl;
import com.msbatchproducer.msbatchproducer.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class BatchJobServiceTest {
    @Mock JobLauncher launcher;
    @Mock Job job;
    @Mock UploadStorage storage;
    @Mock JobExplorer explorer;
    @Mock IBatchRecordRepository records;
    @Mock OutboxRepository outbox;
    BatchJobServiceImpl service;
    @BeforeEach void setup() { service = new BatchJobServiceImpl(launcher,job,storage,explorer,records,outbox); }
    JobExecution execution(BatchStatus status) {
        var params = new JobParametersBuilder().addString("uploadId","u").addString("filePath","/tmp/u.xml",false)
            .addString("format","xml",false).toJobParameters();
        var e = new JobExecution(3L,params); e.setStatus(status); return e;
    }
    @Test void launchUsesStoredFileAndReturnsId() throws Exception {
        when(storage.store(any())).thenReturn(new UploadStorage.StoredUpload("u","/tmp/u.xml","xml"));
        when(launcher.run(eq(job), any())).thenReturn(execution(BatchStatus.STARTING));
        var response = service.launch(new MockMultipartFile("file","x.xml","application/xml","<root/>".getBytes()));
        assertThat(response.jobExecutionId()).isEqualTo(3);
        verify(launcher).run(eq(job),argThat(params -> "u".equals(params.getString("uploadId")) && "/tmp/u.xml".equals(params.getString("filePath"))));
    }
    @Test void restartReusesSameParameters() throws Exception {
        var old = execution(BatchStatus.FAILED); when(explorer.getJobExecution(3L)).thenReturn(old);
        when(launcher.run(job,old.getJobParameters())).thenReturn(execution(BatchStatus.STARTING));
        service.restart(3L); verify(launcher).run(job,old.getJobParameters());
    }
    @Test void cannotRestartCompletedJob() {
        when(explorer.getJobExecution(3L)).thenReturn(execution(BatchStatus.COMPLETED));
        assertThatThrownBy(() -> service.restart(3L)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(launcher);
    }
}
