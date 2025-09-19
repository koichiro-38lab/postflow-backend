BEGIN;

-- 依存関係を考慮しつつ対象テーブルを一旦クリア
TRUNCATE TABLE refresh_tokens,
post_tags,
posts,
media,
tags,
categories,
users
RESTART IDENTITY CASCADE;

-- ユーザー（3件）
INSERT INTO users (email, password_hash, role)
VALUES
('admin@example.com',  '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'ADMIN'),
('editor@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'EDITOR'),
('author@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'AUTHOR');

-- カテゴリー（5件）
WITH seed AS (
SELECT * FROM (VALUES
('Engineering', 'engineering'),
('DevOps',      'devops'),
('Frontend',    'frontend'),
('Backend',     'backend'),
('Productivity','productivity')
) AS v(name, slug)
)
INSERT INTO categories (name, slug)
SELECT name, slug FROM seed;

-- タグ（10件）
WITH seed AS (
SELECT * FROM (VALUES
('Spring Boot', 'spring-boot'),
('Next.js',     'next-js'),
('Terraform',   'terraform'),
('AWS',         'aws'),
('PostgreSQL',  'postgresql'),
('CI/CD',       'ci-cd'),
('Observability','observability'),
('Security',    'security'),
('Testing',     'testing'),
('Architecture','architecture')
) AS v(name, slug)
)
INSERT INTO tags (name, slug)
SELECT name, slug FROM seed;

-- メディア（100件）
WITH media_seed AS (
SELECT gs AS idx,
format('sample-image-%03s.%s', gs,
CASE WHEN gs % 3 = 0 THEN 'png'
WHEN gs % 3 = 1 THEN 'jpg'
ELSE 'jpeg'
END) AS filename,
format('media/sample-image-%03s', gs) AS storage_key,
CASE WHEN gs % 3 = 0 THEN 'image/png'
WHEN gs % 3 = 1 THEN 'image/jpeg'
ELSE 'image/jpeg'
END AS mime,
1200 + ((gs % 5) * 120) AS width,
800 + ((gs % 4) * 90)   AS height,
180000 + gs * 750       AS bytes,
format('サンプルメディア %s の代替テキスト', gs) AS alt_text,
CASE WHEN gs % 3 = 0 THEN 1
WHEN gs % 3 = 1 THEN 2
ELSE 3
END AS created_by,
now() - make_interval(days => 120 - gs) AS created_at
FROM generate_series(1, 100) AS gs
)
INSERT INTO media (filename, storage_key, mime, width, height, bytes, alt_text, created_by, created_at)
SELECT filename, storage_key, mime, width, height, bytes, alt_text, created_by, created_at
FROM media_seed;

-- 投稿（100件）
WITH post_seed AS (
SELECT gs AS idx,
format('実践ガイド %s: Spring Boot と Next.js で構築する CMS', gs) AS title,
format('practical-post-%03s', gs) AS slug,
CASE
WHEN gs % 6 = 0 THEN 'ARCHIVED'
WHEN gs % 5 = 0 THEN 'DRAFT'
ELSE 'PUBLISHED'
END AS status,
format('実践ガイド %s の概要です。AWS 3 層構成と IaC を踏まえた CMS 開発手順をまとめました。', gs) AS excerpt,
jsonb_build_object(
'ops',
jsonb_build_array(
jsonb_build_object('insert',
concat(
'サンプル投稿 ', gs,
' の導入部です。Spring Boot 3 と Next.js 14 を土台に、',
'JWT 認証やロールベースアクセス制御をどう設計するかを解説します。', E'\n\n'
)
),
jsonb_build_object('insert',
concat(
'第2章では AWS (ECS Fargate / RDS / S3 / ALB) を利用した 3 層構成と、',
'Terraform による IaC 管理のポイントを 400 文字程度で詳説しています。', E'\n\n'
)
),
jsonb_build_object('insert',
concat(
'最後に EventBridge によるデイタイム稼働、CloudWatch 監視、',
'本番運用のためのコスト最適化戦略を整理します。', repeat(' 実務の現場でのノウハウを多角的に共有します。', 3), E'\n'
)
)
)
) AS content_json,
CASE
WHEN gs % 3 = 0 THEN 1  -- admin
WHEN gs % 3 = 1 THEN 2  -- editor
ELSE 3                  -- author
END AS author_id,
((gs - 1) % 5) + 1 AS category_id,
CASE
WHEN gs % 5 = 0 THEN NULL  -- draft
ELSE now() - make_interval(days => 60 - gs, hours => (gs % 24))
END AS published_at,
CASE
WHEN gs % 4 = 0 THEN NULL
ELSE ((gs - 1) % 100) + 1
END AS cover_media_id,
now() - make_interval(days => 70 - gs) AS created_at,
now() - make_interval(days => 70 - gs, hours => (gs % 12)) AS updated_at
FROM generate_series(1, 100) gs
)
INSERT INTO posts (title, slug, status, excerpt, content_json, author_id, category_id,
published_at, cover_media_id, created_at, updated_at)
SELECT title, slug, status, excerpt, content_json, author_id, category_id,
published_at, cover_media_id, created_at, updated_at
FROM post_seed;

-- 中間テーブル post_tags（各投稿に 3 タグずつ付与）
WITH post_rn AS (
SELECT id,
row_number() OVER (ORDER BY id) - 1 AS rn
FROM posts
WHERE slug LIKE 'practical-post-%'
),
tag_assign AS (
SELECT p.id AS post_id,
((p.rn + shift) % 10) + 1 AS tag_id
FROM post_rn p
CROSS JOIN LATERAL generate_series(0, 2) AS shift
)
INSERT INTO post_tags (post_id, tag_id)
SELECT a.post_id, t.id
FROM tag_assign a
JOIN tags t ON t.id = a.tag_id;

COMMIT;

-- 実行コマンド例:
-- docker exec -i pf_db psql -U appuser -d appdb < backend/src/test/resources/db/reset_dev_content.sql