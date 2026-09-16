package com.msbatchproducer.msbatchproducer.service;
import com.msbatchproducer.msbatchproducer.repository.OutboxRepository;
import com.msbatchproducer.msbatchproducer.model.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class OutboxStore {
    private final OutboxRepository repository;
    @Transactional(readOnly = true)
    public List<OutboxEvent> pending(int size) {
        return repository.findByPublishedAtIsNullOrderByIdAsc(PageRequest.of(0, size));
    }
    @Transactional
    public void finish(Map<Long, String> results) {
        var events = repository.findAllById(results.keySet());
        for (var event : events) {
            event.setAttempts(event.getAttempts() + 1);
            String error = results.get(event.getId());
            event.setLastError(error == null ? null : error.substring(0, Math.min(2000, error.length())));
            if (error == null) event.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        repository.flush();
    }
}
