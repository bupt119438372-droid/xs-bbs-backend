CREATE TABLE IF NOT EXISTS thought_ai_profile (
    thought_id BIGINT PRIMARY KEY,
    summary VARCHAR(255) NOT NULL,
    tags_json TEXT NOT NULL,
    moderation_status VARCHAR(16) NOT NULL,
    moderation_reason VARCHAR(255),
    llm_provider VARCHAR(32) NOT NULL,
    llm_model VARCHAR(64) NOT NULL,
    embedding_provider VARCHAR(32) NOT NULL,
    embedding_model VARCHAR(64) NOT NULL,
    embedding_json TEXT NOT NULL,
    prompt_version VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_thought_ai_profile_thought FOREIGN KEY (thought_id) REFERENCES thought_post (id)
);

CREATE INDEX idx_thought_ai_moderation_status ON thought_ai_profile (moderation_status);
