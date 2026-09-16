package com.msmonitoreo.msmonitoreo.repository;

import com.msmonitoreo.msmonitoreo.model.dto.*;

public interface IMonitoreoRepository {

    CountsResponse summary();
    PageResponse<ExecutionRow> executions(int page, int size);

}
