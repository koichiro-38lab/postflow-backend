-- プロフィール情報
ALTER TABLE users
    ADD COLUMN display_name VARCHAR(100),
    ADD COLUMN bio TEXT,
    ADD COLUMN avatar_media_id BIGINT REFERENCES media(id) ON DELETE SET NULL;

-- アカウント状態管理
ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at TIMESTAMP;

-- セキュリティ強化
ALTER TABLE users
    ADD COLUMN last_login_at TIMESTAMP;

-- ステータス値の制約
ALTER TABLE users ADD CONSTRAINT check_user_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));

-- インデックス追加（パフォーマンス向上）
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_last_login ON users(last_login_at);
CREATE INDEX idx_users_avatar_media ON users(avatar_media_id);
