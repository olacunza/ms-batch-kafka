package com.msbatchproducer.msbatchproducer.controller;
import com.msbatchproducer.msbatchproducer.service.IBatchJobService;
import com.msbatchproducer.msbatchproducer.model.dto.BatchLaunchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(BatchJobController.class)
class BatchJobControllerTest {
    @Autowired MockMvc mvc;
    @MockBean IBatchJobService service;
    @Test void acceptsFileAndReturnsPollingLocation() throws Exception {
        when(service.launch(any())).thenReturn(new BatchLaunchResponse(1L,"u","STARTING","/api/batch/jobs/1"));
        mvc.perform(multipart("/api/batch/launch").file(new MockMultipartFile("file", "input.xml", "application/xml", "<root/>".getBytes())))
            .andExpect(status().isAccepted()).andExpect(header().string("Location", "/api/batch/jobs/1"))
            .andExpect(jsonPath("$.jobExecutionId").value(1));
    }
    @Test void unknownJobReturns404() throws Exception {
        when(service.status(9)).thenThrow(new java.util.NoSuchElementException("missing"));
        mvc.perform(get("/api/batch/jobs/9")).andExpect(status().isNotFound());
    }
}
