package com.msbatchproducer.msbatchproducer.model.dto;
public record BatchLaunchResponse(Long jobExecutionId, String uploadId, String status, String statusUrl) {}
