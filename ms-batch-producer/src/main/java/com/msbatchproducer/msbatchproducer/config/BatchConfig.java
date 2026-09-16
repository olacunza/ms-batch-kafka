package com.msbatchproducer.msbatchproducer.config;

import com.msbatchproducer.msbatchproducer.batch.listener.JobCompletionListener;
import com.msbatchproducer.msbatchproducer.batch.processor.InputRecordProcessor;
import com.msbatchproducer.msbatchproducer.batch.reader.XmlArchiveReader;
import com.msbatchproducer.msbatchproducer.batch.writer.InputRecordWriter;
import com.msbatchproducer.msbatchproducer.model.dto.XmlInput;
import com.msbatchproducer.msbatchproducer.model.entity.BatchRecord;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration @EnableScheduling
public class BatchConfig {
    @Bean
    public ThreadPoolTaskExecutor batchExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(2); executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("batch-upload-");
        executor.setWaitForTasksToCompleteOnShutdown(true); executor.setAwaitTerminationSeconds(60);
        return executor;
    }
    @Bean
    public TaskExecutorJobLauncher uploadJobLauncher(JobRepository repository, ThreadPoolTaskExecutor batchExecutor) throws Exception {
        var launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(repository); launcher.setTaskExecutor(batchExecutor); launcher.afterPropertiesSet();
        return launcher;
    }
    @Bean
    public Step processInputFileStep(JobRepository repository, PlatformTransactionManager transactionManager,
            XmlArchiveReader reader, InputRecordProcessor processor, InputRecordWriter writer,
            @Value("${app.batch.chunk-size:100}") int size) {
        return new StepBuilder("processInputFileStep", repository)
            .<XmlInput, BatchRecord>chunk(size, transactionManager)
            .reader(reader).processor(processor).writer(writer).build();
    }
    @Bean
    public Job inputFileJob(JobRepository repository, Step processInputFileStep, JobCompletionListener listener) {
        return new JobBuilder("inputFileJob", repository).listener(listener).start(processInputFileStep).build();
    }
}
