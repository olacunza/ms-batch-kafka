package com.msmonitoreo.msmonitoreo;
import com.msmonitoreo.msmonitoreo.controller.MonitoreoController;
import com.msmonitoreo.msmonitoreo.service.IMonitoreoService;
import com.msmonitoreo.msmonitoreo.model.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(MonitoreoController.class)
class MonitoreoControllerTest {
    @Autowired MockMvc mvc;
    @MockBean IMonitoreoService service;
    @Test void summaryReturnsCounts() throws Exception {
        when(service.summary()).thenReturn(new CountsResponse(3,1,1,1));
        mvc.perform(get("/api/monitoreo/resumen")).andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(1));
    }
    @Test void executionsHaveDefaultPagination() throws Exception {
        when(service.executions(0,25)).thenReturn(PageResponse.of(List.of(),0,25,0));
        mvc.perform(get("/api/monitoreo/ejecuciones")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void oversizedPageRejected() throws Exception {
        mvc.perform(get("/api/monitoreo/ejecuciones?size=101")).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void negativePageRejected() throws Exception {
        mvc.perform(get("/api/monitoreo/ejecuciones?page=-1")).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
}
