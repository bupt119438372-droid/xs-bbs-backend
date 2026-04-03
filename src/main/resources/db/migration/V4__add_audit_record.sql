CREATE TABLE IF NOT EXISTS audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thought_id BIGINT NOT NULL,
    operator_user_id BIGINT NULL,
    source_type VARCHAR(16) NOT NULL,
    previous_status VARCHAR(16) NULL,
    current_status VARCHAR(16) NOT NULL,
    decision_reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_record_thought FOREIGN KEY (thought_id) REFERENCES thought_post (id),
    CONSTRAINT fk_audit_record_operator FOREIGN KEY (operator_user_id) REFERENCES user_profile (id)
);

CREATE INDEX idx_audit_record_thought_created ON audit_record (thought_id, created_at DESC);
CREATE INDEX idx_audit_record_operator_created ON audit_record (operator_user_id, created_at DESC);
