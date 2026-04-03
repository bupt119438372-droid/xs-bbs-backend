ALTER TABLE thought_post
    ADD COLUMN allow_recommendation BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE thought_post
    ADD COLUMN public_visible BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE thought_post
SET allow_recommendation = TRUE,
    public_visible = TRUE
WHERE allow_recommendation IS NULL
   OR public_visible IS NULL;

CREATE INDEX idx_thought_public_created ON thought_post (public_visible, created_at DESC);
CREATE INDEX idx_thought_recommend_created ON thought_post (allow_recommendation, created_at DESC);
