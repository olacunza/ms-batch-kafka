package com.msbatchproducer.msbatchproducer.service;
import com.msbatchproducer.msbatchproducer.model.dto.*;
import org.springframework.web.multipart.MultipartFile;
public interface IBatchJobService {
    BatchLaunchResponse launch(MultipartFile file) throws Exception;
    JobStatusResponse status(long id);
    BatchLaunchResponse restart(long id) throws Exception;
}
