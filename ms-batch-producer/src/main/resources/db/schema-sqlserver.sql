SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @lockResult INT;
EXEC @lockResult = sp_getapplock @Resource = 'ms-batch-producer-schema', @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 60000;
IF @lockResult < 0 THROW 50002, 'No se pudo bloquear la inicializacion del esquema', 1;
-- Idempotent upgrade: preserve the original IDENTITY table and copy its rows.
IF OBJECT_ID(N'dbo.batch_record', N'U') IS NOT NULL
 AND COLUMNPROPERTY(OBJECT_ID(N'dbo.batch_record'), 'id', 'IsIdentity') = 1
BEGIN
    IF OBJECT_ID(N'dbo.batch_record_legacy', N'U') IS NOT NULL
        THROW 50001, 'Existe batch_record_legacy: revisar migracion antes de continuar', 1;
    EXEC sp_rename 'dbo.batch_record', 'batch_record_legacy';
END;
IF OBJECT_ID(N'dbo.batch_record_seq', N'SO') IS NULL
BEGIN
    DECLARE @start BIGINT = 1;
    IF OBJECT_ID(N'dbo.batch_record_legacy', N'U') IS NOT NULL
        SELECT @start = COALESCE(MAX(id), 0) + 50 FROM dbo.batch_record_legacy;
    EXEC('CREATE SEQUENCE dbo.batch_record_seq AS BIGINT START WITH ' + @start + ' INCREMENT BY 50');
END;
IF OBJECT_ID(N'dbo.outbox_event_seq', N'SO') IS NULL
    CREATE SEQUENCE dbo.outbox_event_seq AS BIGINT START WITH 1 INCREMENT BY 50;
IF OBJECT_ID(N'dbo.batch_record', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.batch_record (
        id BIGINT NOT NULL PRIMARY KEY,
        business_key BIGINT NOT NULL,
        upload_id VARCHAR(36) NOT NULL,
        source_name VARCHAR(512) NOT NULL,
        payload NVARCHAR(MAX) NOT NULL,
        status VARCHAR(20) NOT NULL,
        error_message VARCHAR(2000) NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT uk_record_upload_key UNIQUE(upload_id,business_key),
        CONSTRAINT ck_batch_record_status CHECK(status IN ('PENDING','PROCESSED','FAILED'))
    );
    CREATE INDEX ix_record_upload_status ON dbo.batch_record(upload_id,status);
    IF OBJECT_ID(N'dbo.batch_record_legacy', N'U') IS NOT NULL
        INSERT dbo.batch_record(id,business_key,upload_id,source_name,payload,status,error_message,created_at,updated_at,version)
        SELECT id,business_key,'legacy','legacy','',COALESCE(status,'PENDING'),error_message,
            COALESCE(created_at,SYSUTCDATETIME()),COALESCE(updated_at,SYSUTCDATETIME()),0 FROM dbo.batch_record_legacy;
END;
IF OBJECT_ID(N'dbo.outbox_event', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.outbox_event (
        id BIGINT NOT NULL PRIMARY KEY,
        event_id VARCHAR(36) NOT NULL UNIQUE,
        record_id BIGINT NOT NULL REFERENCES dbo.batch_record(id),
        business_key BIGINT NOT NULL,
        upload_id VARCHAR(36) NOT NULL,
        created_at DATETIME2 NOT NULL,
        published_at DATETIME2 NULL,
        attempts INT NOT NULL DEFAULT 0,
        last_error VARCHAR(2000) NULL,
        version BIGINT NOT NULL DEFAULT 0
    );
    CREATE INDEX ix_outbox_pending ON dbo.outbox_event(published_at,id);
END;



IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.batch_record') AND name='ix_record_status_id')
    CREATE INDEX ix_record_status_id ON dbo.batch_record(status,id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.outbox_event') AND name='ix_outbox_upload_published')
    CREATE INDEX ix_outbox_upload_published ON dbo.outbox_event(upload_id,published_at);
COMMIT TRANSACTION;
^^^
