package com.msmonitoreo.msmonitoreo.model.dto;

import java.time.LocalDateTime;

public record ExecutionRow(
        Long jobExecutionId,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime) {}
