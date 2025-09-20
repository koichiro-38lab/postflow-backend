ALTER TABLE media
    ADD CONSTRAINT media_storage_key_unique UNIQUE (storage_key);

CREATE INDEX IF NOT EXISTS idx_media_created_by ON media(created_by);
