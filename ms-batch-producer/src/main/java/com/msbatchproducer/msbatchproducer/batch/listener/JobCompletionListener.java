package com.msbatchproducer.msbatchproducer.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job started. id={}, name={}",
                jobExecution.getId(), jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED) {
            String file = jobExecution.getJobParameters().getString("filePath");
            if (file != null) try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(file)); }
            catch (java.io.IOException e) { log.warn("No se pudo borrar la carga completada del job {}", jobExecution.getId(), e); }
        }
        ExitStatus exitStatus = jobExecution.getExitStatus();
        log.info("Job finished. id={}, status={}, exitCode={}",
                jobExecution.getId(), jobExecution.getStatus(), exitStatus.getExitCode());
    }

}
