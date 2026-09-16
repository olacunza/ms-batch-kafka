package com.msbatchproducer.msbatchproducer.service.impl;

import com.msbatchproducer.msbatchproducer.service.*;
import com.msbatchproducer.msbatchproducer.model.dto.*;
import com.msbatchproducer.msbatchproducer.repository.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.NoSuchElementException;

@Service
public class BatchJobServiceImpl implements IBatchJobService {
    private final JobLauncher launcher;
    private final Job job;
    private final UploadStorage storage;
    private final JobExplorer explorer;
    private final IBatchRecordRepository records;
    private final OutboxRepository outbox;
    public BatchJobServiceImpl(@Qualifier("uploadJobLauncher") JobLauncher launcher, Job inputFileJob,
        UploadStorage storage, JobExplorer explorer, IBatchRecordRepository records, OutboxRepository outbox) {
        this.launcher = launcher; this.job = inputFileJob; this.storage = storage;
        this.explorer = explorer; this.records = records; this.outbox = outbox;
    }
    public BatchLaunchResponse launch(MultipartFile file) throws Exception {
        var upload = storage.store(file);
        var params = new JobParametersBuilder().addString("uploadId", upload.id())
            .addString("filePath", upload.path(), false).addString("format", upload.format(), false).toJobParameters();
        JobExecution execution;
        try { execution = launcher.run(job, params); }
        catch (Exception e) { Files.deleteIfExists(Path.of(upload.path())); throw e; }
        return response(execution);
    }
    public BatchLaunchResponse restart(long id) throws Exception {
        var execution = find(id);
        if (execution.getStatus() != BatchStatus.FAILED && execution.getStatus() != BatchStatus.STOPPED)
            throw new IllegalArgumentException("Solo se puede reiniciar un job FAILED o STOPPED");
        return response(launcher.run(job, execution.getJobParameters()));
    }
    public JobStatusResponse status(long id) {
        var e = find(id);
        String upload = e.getJobParameters().getString("uploadId");
        long read = e.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long write = e.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        String error = e.getStatus() == BatchStatus.FAILED ? "La carga falló; consulta el log con jobExecutionId=" + id : null;
        return new JobStatusResponse(e.getId(), upload, e.getStatus().name(), read, write,
            records.countByUploadId(upload), outbox.countByUploadIdAndPublishedAtIsNull(upload),
            records.countByUploadIdAndStatus(upload, com.msbatchproducer.msbatchproducer.model.enums.RecordStatus.FAILED),
            e.getStartTime(), e.getEndTime(), error);
    }
    private JobExecution find(long id) {
        var e = explorer.getJobExecution(id);
        if (e == null) throw new NoSuchElementException("No existe la ejecución " + id);
        return e;
    }
    private BatchLaunchResponse response(JobExecution e) {
        return new BatchLaunchResponse(e.getId(), e.getJobParameters().getString("uploadId"),
            e.getStatus().name(), "/api/batch/jobs/" + e.getId());
    }
}
