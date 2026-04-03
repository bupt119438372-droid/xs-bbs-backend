ALTER TABLE notification_message
    ADD COLUMN type_code VARCHAR(32) NOT NULL DEFAULT 'SYSTEM';

CREATE INDEX idx_notification_receiver_type_created
    ON notification_message (receiver_id, type_code, created_at DESC);
