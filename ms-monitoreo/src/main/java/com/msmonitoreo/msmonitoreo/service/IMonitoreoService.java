package com.msmonitoreo.msmonitoreo.service;

import com.msmonitoreo.msmonitoreo.model.dto.*;

public interface IMonitoreoService {

    CountsResponse summary();
    PageResponse<ExecutionResponse> executions(int page,int size);

}
