package com.msmonitoreo.msmonitoreo.model.dto;

public record CountsResponse(
        long total,
        long pending,
        long processed,
        long failed) {}
