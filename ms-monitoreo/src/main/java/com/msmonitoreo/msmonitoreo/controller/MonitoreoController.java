package com.msmonitoreo.msmonitoreo.controller;
import com.msmonitoreo.msmonitoreo.service.IMonitoreoService;
import com.msmonitoreo.msmonitoreo.model.dto.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoreo")
@RequiredArgsConstructor
@Validated
public class MonitoreoController {

    private final IMonitoreoService service;

    @GetMapping("/resumen")
    public CountsResponse summary() {
        return service.summary();
    }

    @GetMapping("/ejecuciones")
    public PageResponse<ExecutionResponse> executions(
            @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue="25") @Min(1) @Max(100) int size) {
        return service.executions(page,size);
    }
}
