package com.msmonitoreo.msmonitoreo.model.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;
@Entity @Table(name="BATCH_JOB_EXECUTION") @Immutable
public class JobExecution {

    @Id
    @Column(name="JOB_EXECUTION_ID")
    private Long id;

    @Column(name="STATUS",length=10)
    private String status;

    @Column(name="START_TIME")
    private LocalDateTime startTime;

    @Column(name="END_TIME")
    private LocalDateTime endTime;

    protected JobExecution() {}

}
