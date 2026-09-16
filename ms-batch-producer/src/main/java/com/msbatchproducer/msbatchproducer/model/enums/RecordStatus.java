package com.msbatchproducer.msbatchproducer.model.enums;

public enum RecordStatus {
    PENDING,
    PROCESSED,
    FAILED;

    public boolean isTerminal() {
        return this == PROCESSED || this == FAILED;
    }
}
