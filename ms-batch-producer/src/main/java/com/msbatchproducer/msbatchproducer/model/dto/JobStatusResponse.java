package com.msbatchproducer.msbatchproducer.model.dto;
import java.time.LocalDateTime;
public record JobStatusResponse(Long jobExecutionId, String uploadId, String status,
    long readCount, long writeCount, long committedRecords, long pendingEvents, long failedRecords,
    LocalDateTime startTime, LocalDateTime endTime, String error) {}
