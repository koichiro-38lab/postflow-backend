-- filepath: backend/src/main/resources/db/seed/seed_minimal.sql
-- 簡易シード: 初回起動時の最低限データ
-- ユーザー3名・カテゴリ2件・タグ5件・サンプル投稿1件

-- 既存データクリア
TRUNCATE TABLE post_tags, posts, categories, tags, refresh_tokens, users RESTART IDENTITY CASCADE;

-- ユーザー3名（パスワード: password）
INSERT INTO users (email, password_hash, role, display_name, created_at, updated_at)
VALUES
    ('admin@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'ADMIN', '管理者', NOW(), NOW()),
    ('editor@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'EDITOR', '編集者', NOW(), NOW()),
    ('author@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'AUTHOR', '投稿者', NOW(), NOW());

-- カテゴリ2件
INSERT INTO categories (name, slug, created_at, updated_at)
VALUES
    ('テクノロジー', 'technology', NOW(), NOW()),
    ('ビジネス', 'business', NOW(), NOW());

-- タグ5件
INSERT INTO tags (name, slug, created_at, updated_at)
VALUES
    ('Spring Boot', 'spring-boot', NOW(), NOW()),
    ('Next.js', 'nextjs', NOW(), NOW()),
    ('AWS', 'aws', NOW(), NOW()),
    ('Docker', 'docker', NOW(), NOW()),
    ('PostgreSQL', 'postgresql', NOW(), NOW());

-- サンプル投稿1件（admin作成・テクノロジーカテゴリ・公開済み）
INSERT INTO posts (title, slug, excerpt, content_json, status, published_at, author_id, category_id, created_at, updated_at)
VALUES
    (
        'ウェルカム投稿: PostFlowへようこそ',
        'welcome-to-postflow',
        'PostFlowは、Spring BootとNext.jsで構築されたモダンなCMS/ブログシステムです。',
        '{"type":"doc","content":[{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"PostFlowへようこそ"}]},{"type":"paragraph","content":[{"type":"text","text":"このプラットフォームは、以下の技術スタックで構築されています:"}]},{"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"バックエンド: Spring Boot 3.5 + PostgreSQL"}]}]},{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"フロントエンド: Next.js 15 + TailwindCSS"}]}]}]},{"type":"paragraph","content":[{"type":"text","text":"管理画面では、リッチテキストエディタ（TipTap）による執筆、メディアライブラリ管理、タグ・カテゴリ管理が可能です。"}]}]}',
        'PUBLISHED',
        NOW(),
        (SELECT id FROM users WHERE email = 'admin@example.com'),
        (SELECT id FROM categories WHERE slug = 'technology'),
        NOW(),
        NOW()
    );

-- 投稿にタグを関連付け
INSERT INTO post_tags (post_id, tag_id)
VALUES
    ((SELECT id FROM posts WHERE slug = 'welcome-to-postflow'), (SELECT id FROM tags WHERE slug = 'spring-boot')),
    ((SELECT id FROM posts WHERE slug = 'welcome-to-postflow'), (SELECT id FROM tags WHERE slug = 'nextjs')),
    ((SELECT id FROM posts WHERE slug = 'welcome-to-postflow'), (SELECT id FROM tags WHERE slug = 'docker'));
