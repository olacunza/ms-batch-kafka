package com.msmonitoreo.msmonitoreo.repository.impl;

import com.msmonitoreo.msmonitoreo.model.dto.*;
import com.msmonitoreo.msmonitoreo.repository.IMonitoreoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MonitoreoRepositoryImpl implements IMonitoreoRepository {

    private final EntityManager em;
    private static final String DTO="com.msmonitoreo.msmonitoreo.model.dto.";

    private <T> TypedQuery<T> query(String hql,Class<T> type) {
        return em.createQuery(hql,type).setHint("jakarta.persistence.query.timeout",5000);
    }

    @Override
    public CountsResponse summary() {
        return query("select new "+DTO+"CountsResponse(count(r), "
                +"coalesce(sum(case when r.status='PENDING' then 1L else 0L end),0L), "
                +"coalesce(sum(case when r.status='PROCESSED' then 1L else 0L end),0L), "
                +"coalesce(sum(case when r.status='FAILED' then 1L else 0L end),0L)) from BatchRecord r",
                CountsResponse.class).getSingleResult();
    }

    @Override
    public PageResponse<ExecutionRow> executions(int page,int size) {
        long count=query("select count(e) from JobExecution e",Long.class).getSingleResult();
        var rows=query("select new "+DTO+"ExecutionRow(e.id,e.status,e.startTime,e.endTime) from JobExecution e order by e.id desc",ExecutionRow.class)
                .setFirstResult(page*size).setMaxResults(size).getResultList();
        return PageResponse.of(rows,page,size,count);
    }
}
