package com.msmonitoreo.msmonitoreo;
import com.msmonitoreo.msmonitoreo.repository.IMonitoreoRepository;
import com.msmonitoreo.msmonitoreo.service.impl.MonitoreoServiceImpl;
import com.msmonitoreo.msmonitoreo.model.dto.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MonitoreoServiceTest {
    private final IMonitoreoRepository repository=mock(IMonitoreoRepository.class);
    private final MonitoreoServiceImpl service=new MonitoreoServiceImpl(repository);
    @Test void returnsRepositoryCounts() {
        when(repository.summary()).thenReturn(new CountsResponse(10,2,7,1));
        assertEquals(7,service.summary().processed());verify(repository).summary();
    }
    @Test void calculatesCompletedDurationAndKeepsRunningDurationNull() {
        var start=LocalDateTime.of(2026,9,15,12,0);
        when(repository.executions(0,25)).thenReturn(PageResponse.of(List.of(
                new ExecutionRow(0L,"COMPLETED",start,start.plusSeconds(2)),
                new ExecutionRow(1L,"STARTED",start,null),
                new ExecutionRow(2L,"STARTING",null,null)),0,25,3));
        var page=service.executions(0,25);
        assertEquals(2000L,page.content().get(0).durationMs());
        assertNull(page.content().get(1).durationMs());assertNull(page.content().get(2).durationMs());
        assertEquals(3,page.totalElements());
    }
}
