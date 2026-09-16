SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
UPDATE o
SET published_at = NULL, last_error = NULL, version = o.version + 1
FROM dbo.outbox_event o
JOIN dbo.batch_record r ON r.id = o.record_id
WHERE r.status = 'PENDING'
  AND NOT EXISTS (SELECT 1 FROM dbo.consumed_event c WHERE c.event_id = o.event_id);
SELECT @@ROWCOUNT AS events_requeued;
COMMIT TRANSACTION;
GO
