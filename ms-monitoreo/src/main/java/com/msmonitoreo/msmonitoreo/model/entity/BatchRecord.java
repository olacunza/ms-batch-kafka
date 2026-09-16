package com.msmonitoreo.msmonitoreo.model.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
@Entity @Table(name="batch_record") @Immutable
public class BatchRecord {

    @Id
    private Long id;

    @Column(name="status",nullable=false,length=20)
    private String status;

    protected BatchRecord() {}

}
