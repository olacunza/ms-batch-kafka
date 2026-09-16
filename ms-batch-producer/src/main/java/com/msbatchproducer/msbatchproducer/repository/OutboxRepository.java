package com.msbatchproducer.msbatchproducer.repository;
import com.msbatchproducer.msbatchproducer.model.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedAtIsNullOrderByIdAsc(Pageable pageable);
    long countByUploadIdAndPublishedAtIsNull(String uploadId);
}
