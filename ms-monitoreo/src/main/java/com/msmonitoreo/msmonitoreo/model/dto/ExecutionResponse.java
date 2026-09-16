package com.msmonitoreo.msmonitoreo.model.dto;

import java.time.LocalDateTime;

public record ExecutionResponse(
        Long jobExecutionId,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs) {}
