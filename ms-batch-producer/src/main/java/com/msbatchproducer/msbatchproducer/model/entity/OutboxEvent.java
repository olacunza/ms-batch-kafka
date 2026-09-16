package com.msbatchproducer.msbatchproducer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event", indexes = @Index(name = "ix_outbox_pending", columnList = "published_at,id"))
@Getter @Setter @NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_ids")
    @SequenceGenerator(name = "outbox_ids", sequenceName = "outbox_event_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false, unique = true, length = 36)
    private String eventId;
    @Column(nullable = false)
    private Long recordId;
    @Column(nullable = false)
    private Long businessKey;
    @Column(nullable = false, length = 36)
    private String uploadId;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private int attempts;
    @Column(length = 2000)
    private String lastError;
    @Version private long version;
}
