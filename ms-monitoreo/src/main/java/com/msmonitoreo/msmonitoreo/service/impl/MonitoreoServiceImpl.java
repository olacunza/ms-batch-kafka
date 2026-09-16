package com.msmonitoreo.msmonitoreo.service.impl;

import com.msmonitoreo.msmonitoreo.service.IMonitoreoService;
import com.msmonitoreo.msmonitoreo.repository.IMonitoreoRepository;
import com.msmonitoreo.msmonitoreo.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true,timeout=10)
public class MonitoreoServiceImpl implements IMonitoreoService {

    private final IMonitoreoRepository repository;

    @Override
    public CountsResponse summary() {
        return repository.summary();
    }

    @Override
    public PageResponse<ExecutionResponse> executions(int page,int size) {
        return repository.executions(page,size).map(row -> {
            Long duration=row.startTime()!=null && row.endTime()!=null
                    ? Math.max(0,Duration.between(row.startTime(),row.endTime()).toMillis()) : null;
            return new ExecutionResponse(row.jobExecutionId(),row.status(),row.startTime(),row.endTime(),duration);
        });
    }
}
