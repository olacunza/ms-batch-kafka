package com.msbatchproducer.msbatchproducer.controller;
import com.msbatchproducer.msbatchproducer.model.dto.*;
import com.msbatchproducer.msbatchproducer.service.IBatchJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
@RestController @RequestMapping("/api/batch") @RequiredArgsConstructor
public class BatchJobController {
    private final IBatchJobService service;
    @PostMapping(value = "/launch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchLaunchResponse> launch(@RequestParam("file") MultipartFile file) throws Exception {
        var response = service.launch(file);
        return ResponseEntity.accepted().location(URI.create(response.statusUrl())).body(response);
    }
    @GetMapping("/jobs/{id}")
    public JobStatusResponse status(@PathVariable long id) { return service.status(id); }
    @PostMapping("/jobs/{id}/restart")
    public ResponseEntity<BatchLaunchResponse> restart(@PathVariable long id) throws Exception {
        var response = service.restart(id);
        return ResponseEntity.accepted().location(URI.create(response.statusUrl())).body(response);
    }
}
