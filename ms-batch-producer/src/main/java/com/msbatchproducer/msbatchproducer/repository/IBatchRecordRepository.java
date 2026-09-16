package com.msbatchproducer.msbatchproducer.repository;
import com.msbatchproducer.msbatchproducer.model.entity.BatchRecord;
import com.msbatchproducer.msbatchproducer.model.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IBatchRecordRepository extends JpaRepository<BatchRecord, Long> {
    long countByStatus(RecordStatus status);
    long countByUploadId(String uploadId);
    long countByUploadIdAndStatus(String uploadId, RecordStatus status);
}
