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
INSERT INTO users (email, password_hash, role, display_name, bio)
VALUES
('admin@example.com',  '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'ADMIN', '神谷 浩一', 'クラウドアーキテクトとして15年の経験を持つシニアエンジニア。マイクロサービスやイベント駆動設計を通じてPostFlowの基盤を主導。チーム横断でコード品質とSRE文化を育て、顧客への価値提供速度を最大化する役割を担う。趣味はサイクリングと技術書執筆で、社内勉強会を毎月開催している。'),
('editor@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'EDITOR', '大橋 涼太', 'バックエンドとインフラの基礎を磨くジュニアエンジニア。認証基盤とCI/CDの保守を担当しながらドメイン駆動設計を学習中。シニア陣のレビューで設計の勘所を吸収し、安定運用と自動化の改善提案を継続している。趣味はガジェット収集とランニング。'),
('author@example.com', '$2a$10$rpcLMbCzLx0QwbS8tCBjkeR6EG8fXL5pYXMYsag794f1YywcrXoNS', 'AUTHOR', '佐藤 美咲', 'Next.jsとデザインシステム構築を得意とするフロントエンドエンジニア。アクセシビリティ指針を軸にUI/UXを改善し、TipTap連携やStorybook整備を推進。ユーザー調査とABテストで管理画面の操作性を磨き、趣味の写真撮影で得た感性をUIに活かしている。');


-- カテゴリー（6件）
WITH seed AS (
SELECT * FROM (VALUES
('Frontend',    'frontend',    0),
('Backend',     'backend',     1),
('Engineering', 'engineering', 2),
('DevOps',      'devops',      3),
('Productivity','productivity',4),
('Cloud',       'cloud',       5)
) AS v(name, slug, sort_order)
)
INSERT INTO categories (name, slug, sort_order)
SELECT name, slug, sort_order FROM seed;

-- フロントエンド配下の子カテゴリ
INSERT INTO categories (name, slug, parent_id, sort_order)
VALUES
('Next.js', 'frontend-nextjs', 1, 0),
('React', 'frontend-react', 1, 1);

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

-- アバター用メディア（ユーザー用の固定画像）
INSERT INTO media (filename, storage_key, mime, width, height, bytes, alt_text, created_by, created_at)
VALUES
('avator_1.webp', 'media/avator_1.webp', 'image/webp', 512, 512, 84256, '神谷 浩一のアバター画像', 1, now()),
('avator_2.webp', 'media/avator_2.webp', 'image/webp', 512, 512, 79200, '大橋 涼太のアバター画像', 2, now()),
('avator_3.webp', 'media/avator_3.webp', 'image/webp', 512, 512, 80640, '佐藤 美咲のアバター画像', 3, now());

UPDATE users u
SET avatar_media_id = m.id
FROM media m
WHERE u.email = 'admin@example.com' AND m.storage_key = 'media/avator_1.webp';

UPDATE users u
SET avatar_media_id = m.id
FROM media m
WHERE u.email = 'editor@example.com' AND m.storage_key = 'media/avator_2.webp';

UPDATE users u
SET avatar_media_id = m.id
FROM media m
WHERE u.email = 'author@example.com' AND m.storage_key = 'media/avator_3.webp';

-- メディア（100件）
WITH media_seed AS (
SELECT gs AS idx,
format('sample-%s.avif', gs) AS filename,
format('media/sample-%s.avif', gs) AS storage_key,
'image/avif' AS mime,
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
    SELECT
        gs AS idx,
        CASE
            WHEN gs % 2 = 1 THEN format('実践ガイド %s: Spring Boot と Next.js で構築する CMS', gs)
            ELSE format('実践ガイド %s: Next.js と React で作る モダンフロントエンド', gs)
        END AS title,
        format('practical-post-%s', to_char(gs, 'FM000')) AS slug,
        CASE
            WHEN gs % 20 = 0 THEN 'ARCHIVED'
            WHEN gs % 15 = 0 THEN 'DRAFT'
            ELSE 'PUBLISHED'
        END AS status,
        CASE
            WHEN gs % 2 = 1 THEN format('実践ガイド %s の概要です。Spring Boot と Next.js を連携させた API / 管理画面の構築手順をまとめました。', gs)
            ELSE format('実践ガイド %s の概要です。App Router と Server Components を活用したモダンフロントエンド開発を整理しました。', gs)
        END AS excerpt,
        CASE
            WHEN gs % 2 = 1 THEN jsonb_build_object('type', 'doc', 'content', jsonb_build_array(
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 2), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'Spring Boot実務ハンドブック')
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'サンプル投稿 ', gs,
            ' では Spring Boot 3 と Next.js 15 を土台にした管理APIの構築を段階的に追体験できるように解説している。',
            ' エントリでは依存注入、例外ハンドリング、DTO設計の実践知に触れ、',
            ' 実務でレビューを通じて蓄積したノウハウを惜しみなく盛り込んでいる。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'プロファイリング結果を踏まえたパフォーマンスチューニング事例も交え、実務で迷子にならない指針を提示する。',
            ' JVM Flight Recorder や async-profiler のログをどう読み解き、ボトルネックを解消したかを具体的なメトリクスとともに解説する。',
            ' コネクションプールやスレッドプールの設定値を見直す際の検証手順と意思決定の根拠も整理し、再現可能な改善プロセスを提示する。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', '負荷試験とパフォーマンス改善')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 37 + 23) % 100) + 1, '.avif'),
        'alt', concat('負荷試験とパフォーマンス改善イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにガベージコレクションログの分析手法や、メモリリーク検出のための具体的な診断フローも詳述し、',
            ' 本番環境で発生しやすいパフォーマンス劣化の兆候を早期に察知するための監視ポイントを複数の視点から整理している。',
            ' 加えて、負荷試験の設計パターンやシナリオ作成の勘所、結果の読み解き方についても実例を交えて丁寧に解説し、',
            ' チーム内でパフォーマンス改善の知見を共有するためのドキュメンテーション手法まで踏み込んで紹介する。',
            ' 実際のインシデント対応で得られた教訓や、事後振り返りで抽出された改善アクションの優先順位付けについても触れ、',
            ' 読者が自身のプロジェクトで同様の課題に直面した際に即座に活用できる実践的なナレッジベースを提供している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'インフラ設計とクラウド運用')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 41 + 29) % 100) + 1, '.avif'),
        'alt', concat('インフラ設計とクラウド運用イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'インフラ編では AWS Fargate / RDS / S3 / CloudWatch を組み合わせた三層構成を題材に、',
            ' サービスディスカバリ、接続プール、バックプレッシャ制御の考え方を整理。',
            ' IaC として Terraform を採用する際のモジュール分割やワークスペース戦略を、',
            ' チーム開発の視点で細かく掘り下げ、',
            ' 監査ログ・メトリクス・分散トレーシングの統合運用に関する Tips を段階的にまとめている。',
            ' ログ保持ルールや匿名化ポリシー、ダッシュボード設計の要点も補足し、運用チームと連携するときの論点を先回りで整理する。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにネットワークセグメンテーションの設計パターンや、セキュリティグループのルール管理ベストプラクティス、',
            ' コスト最適化のための定期的なリソース棚卸し手順についても実例を交えて詳述している。',
            ' バックアップとリストアの運用フロー、ディザスタリカバリ計画の策定プロセス、',
            ' インシデント発生時のエスカレーションパスの設計についても具体的なテンプレートを提供し、',
            ' 読者が自組織の要件に合わせてカスタマイズできるよう配慮した構成となっている。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、マルチリージョン構成への移行戦略や、データレプリケーションの整合性管理、',
            ' フェイルオーバー訓練の実施要領についても段階的に解説し、可用性向上のための実践的な知見を豊富に盛り込んでいる。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'API設計と統合戦略')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 43 + 31) % 100) + 1, '.avif'),
        'alt', concat('API設計と統合戦略イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'API設計編では OpenAPI からコードジェネレーションへと繋げるワークフロー、',
            ' レートリミットや多要素認証を加える際の責務分担を詳述。',
            ' 実際に遭遇した障害事例を元に、',
            ' どこに監視ポイントを置き、アラート閾値をどう調整したかをバッドノウハウも含めて紹介する。',
            ' 実案件の振り返りで得た知見を踏まえた改善サイクルの回し方も丁寧に解説。',
            ' 振り返りで活用した問いかけリストや意思決定ログの残し方まで踏み込み、学びを蓄積する仕組みづくりを提案する。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにバージョニング戦略の選定基準や、後方互換性を維持しながら新機能を段階的にロールアウトする際の具体的な手順、',
            ' クライアント側のエラーハンドリングパターンとリトライポリシーの設計についても実例を交えて詳しく解説している。',
            ' API ドキュメントの自動生成と継続的な更新を実現するためのツールチェーン構築や、',
            ' スキーマ駆動開発を採用する際のチーム内合意形成プロセスについても触れ、',
            ' 開発効率と品質を両立させるためのベストプラクティスを多角的に提示する。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、サードパーティ API との統合時の注意点や、外部依存の障害を切り離すためのサーキットブレーカー設計、',
            ' タイムアウト値の調整方針についても具体的な判断基準を示し、読者が自身のプロジェクトで即座に応用できるよう配慮している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 2), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'システムアーキテクチャ総覧')
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'ドメイン駆動設計の実践')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 7 + 1) % 100) + 1, '.avif'),
        'alt', concat('ドメイン駆動設計の実践イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '開発チームが採択したマイクロサービス分割と境界づけられたコンテキストの整理を、',
            ' コアドメイン、サブドメイン、支援ドメインの観点から丁寧に解説。',
            ' チームトポロジーと結合した責務分担の標準化手順、',
            ' 監査ログを踏まえた権限設計のベストプラクティス、',
            ' 運用監視の導線まで含めた図解を通じて、意思決定プロセスを読者が追体験できるように具体例を豊富に盛り込んだ。',
            ' エスカレーションフローや監視ダッシュボードの設計パターンも併記し、チーム横断で合意形成するための材料を提供する。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにドメインイベントの設計とイベントストーミングの実施手順、サービス間通信のプロトコル選定基準、',
            ' 非同期メッセージングを採用する際のトレードオフと実装パターンについても詳細に解説している。',
            ' コンウェイの法則を意識した組織構造とアーキテクチャの整合性確保、',
            ' チーム自律性を高めるための意思決定権限の委譲戦略、',
            ' サービスオーナーシップの明確化と責任範囲の定義方法についても実例を交えて紹介し、',
            ' 読者が自組織の文化や規模に応じて適用できるよう多様な選択肢を提示している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、技術的負債の可視化と返済計画の策定プロセス、リファクタリングの優先順位付けフレームワーク、',
            ' アーキテクチャ決定記録（ADR）の運用方法についても具体的なテンプレートとともに解説し、',
            ' 継続的なアーキテクチャ改善を実現するための実践的なアプローチを提供している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'トラフィック急増時の耐障害性')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 11 + 3) % 100) + 1, '.avif'),
        'alt', concat('トラフィック急増時の耐障害性イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '突然のアクセス集中に備えたカナリアリリース、',
            ' カスケード障害を避けるためのサーキットブレーカ設定、',
            ' バックプレッシャを活かしたキューイング設計を段階的に紹介。',
            ' SLO 設計とエラーバジェット運用をどのように回したか、',
            ' 具体的な数値と意思決定ログを引用しながら、実務判断の裏側を掘り下げている。',
            ' インシデントレビューで抽出された改善アクションや、フェイルオーバー訓練の記録も取り上げ、実践的な視点を補強する。',
            ' さらにオートスケーリングポリシーの調整方法や、トラフィックパターンに応じたスケーリングメトリクスの選定基準、',
            ' ピーク時のコスト抑制と可用性のバランスを取るための具体的な設定例についても詳述している。',
            ' ロードバランサーの健全性チェック設定やドレイニング戦略、ゼロダウンタイムデプロイを実現するための段階的な手順、',
            ' ロールバック判断の自動化とマニュアル介入のタイミングについても実例を交えて解説し、',
            ' 読者が自身のサービスで同様の耐障害性を実現するための具体的なロードマップを提示している。',
            ' 加えて、分散トレーシングを活用したボトルネック特定の手法や、エラー率の異常検知アルゴリズム、',
            ' アラート疲れを防ぐための通知ルールの最適化についても触れ、運用チームの負担を軽減しながら高い信頼性を維持するための実践的な知見を共有している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'CI/CDと品質管理の統合')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 13 + 5) % 100) + 1, '.avif'),
        'alt', concat('CI/CDと品質管理の統合イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '最後に CI/CD と品質管理について、GitHub Actions と ArgoCD を連携させた運用例を公開。',
            ' スモークテストや契約テストの自動化、',
            ' ブルーグリーンデプロイ時のロールバック戦略など、',
            ' デリバリ効率を上げつつ安定運用を実現した手順を余すところなく書き下ろした。',
            ' 本章を読み終える頃には、読者が自らのプロジェクトで同様の構成を再現するための具体的な Todo リストが手元に残る設計としている。',
            ' 役割ごとの実装チェックリストや移行計画のテンプレートも付し、学びをすぐに現場へ持ち帰れるように構成した。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにパイプラインのセキュリティ強化策として、シークレット管理のベストプラクティスや依存関係の脆弱性スキャン、',
            ' コンテナイメージの署名と検証プロセスについても詳細に解説している。',
            ' テスト環境と本番環境の構成差分を最小化するためのインフラコード管理戦略や、',
            ' 環境変数とシークレットの注入方法、デプロイ承認フローの設計についても実例を交えて紹介し、',
            ' 読者が安全かつ効率的なデリバリパイプラインを構築できるよう具体的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、デプロイ頻度とリードタイムの計測方法、Four Keys メトリクスの可視化と改善サイクル、',
            ' ポストモーテム文化の醸成と学習する組織の構築についても触れ、',
            ' 継続的な改善活動を支える文化と仕組みづくりの重要性を強調している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 2), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', '継続的改善チェックリスト')
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', '振り返りテンプレートの活用')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 17 + 7) % 100) + 1, '.avif'),
        'alt', concat('振り返りテンプレートの活用イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'ふりかえりで定点確認するべきアーキテクチャ・テスト・運用の要点を、',
            ' チーム内で共有するテンプレート付きでまとめた。',
            ' ロードマップの更新周期、セキュリティレビューの頻度、',
            ' KPI と KGI を接続するための指標設計など、',
            ' 読者が自チームに適用できるようワークショップ形式の進め方や質問例を豊富に提示する構成とした。',
            ' 各セッションのタイムボックスや成果物テンプレートも添え、導入準備で迷わないよう実践的なチェックポイントを整理した。',
            ' さらに振り返りファシリテーションのスクリプト例や、議論を収束させるための質問集も収録し、',
            ' チームで合意形成するまでのプロセスを可視化できるよう工夫を凝らしている。',
            ' 改善提案のバックログへの反映方法や優先度の判定フレームワークも示し、',
            ' 継続的に学習サイクルを回すための実践的な運用モデルをステップバイステップで紹介する。',
            ' 加えて、アクションアイテムの追跡テンプレートや、リトロスペクティブ後のフォローアップ会議の進め方にも言及し、',
            ' チーム全員が改善活動を自分ごととして捉えられるような仕組みづくりの要点を丁寧に解説している。',
            ' 最後に定量評価と定性評価をバランスよく組み合わせた振り返り指標の設計例も提供し、',
            ' 読者が現場で即座に活用できる具体的なツールキットを完備した構成としている。'
        ))
    ))
))

            ELSE jsonb_build_object('type', 'doc', 'content', jsonb_build_array(
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 2), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'Next.jsフロント実践メモ')
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'UIコンポーネント設計とアクセシビリティ')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 11 + 3) % 100) + 1, '.avif'),
        'alt', concat('UIコンポーネント設計とアクセシビリティイメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'サンプル投稿 ', gs,
            ' では App Router / Server Components を軸に、',
            ' Next.js 15 の新機能を活かしたUX設計とパフォーマンス最適化の手順を緻密に解説。',
            ' UI コンポーネントの分割方針からアクセシビリティ対応、',
            ' Storybook や Playwright を駆使したビジュアルリグレッションの抑え方まで実践的なノウハウを多層的に盛り込んだ。',
            ' UI レビューを自動化するワークフローとデザインハンドオフのベストプラクティスも丁寧に整理した。',
            ' Linter の拡張やコードモッド活用によってデザインシステムとの乖離を防ぐための運用例も余さず紹介している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにコンポーネントライブラリのバージョン管理戦略や、破壊的変更を段階的に適用するための移行ガイド作成手順、',
            ' デザイントークンの管理と CSS-in-JS / CSS Modules の使い分け基準についても実例を交えて詳述している。',
            ' アクセシビリティ監査の自動化とスクリーンリーダーテストの実施方法、WCAG 準拠を維持するためのチェックリスト運用、',
            ' キーボードナビゲーションとフォーカス管理のベストプラクティスについても具体的なコード例とともに解説し、',
            ' 読者がインクルーシブなユーザー体験を提供できるよう実践的な指針を提示している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、パフォーマンス計測の自動化とContinuous Performance Testingの導入方法、',
            ' Web Vitals の監視とアラート設定、ユーザー体験指標とビジネス KPI の相関分析についても触れ、',
            ' データドリブンな改善サイクルを回すための具体的なフレームワークを提供している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'データフェッチングとキャッシュ戦略')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 13 + 5) % 100) + 1, '.avif'),
        'alt', concat('データフェッチングとキャッシュ戦略イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'データ取得では fetch キャッシュ制御、ISR、Route Handler を使ったサーバーアクション、',
            ' Edge Runtime の活用事例を比較しながら、',
            ' プロダクトの性質に応じた選択肢を豊富なケーススタディで提示。',
            ' API レイヤとの責務分担やオブザーバビリティの仕込み方も読者が自走できるレベルまで踏み込んで解説している。',
            ' API ポリシーのガバナンスや自動テレメトリ導入時の警戒ポイントも取り上げ、運用開始後の継続改善につなげる視点を補強。',
            ' さらにデータフェッチング戦略の選定フローチャートや、リクエストウォーターフォールの最適化手法、',
            ' Suspense と Streaming SSR を活用した段階的なページレンダリングの実装パターンについても詳細に解説している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'クライアントサイドキャッシュとサーバーサイドキャッシュの整合性管理、',
            ' stale-while-revalidate パターンの実装と運用上の注意点、',
            ' キャッシュ無効化戦略とタグベースのキャッシュパージについても実例を交えて紹介し、',
            ' 読者がデータの鮮度とパフォーマンスのバランスを最適化できるよう具体的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、エラーハンドリングとフォールバック UI の設計パターン、楽観的 UI 更新の実装手法、',
            ' リアルタイムデータ同期を実現するための WebSocket / Server-Sent Events の活用についても触れ、',
            ' モダンなフロントエンド開発で求められる多様な要件に対応するための実践的なアプローチを提示している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'デザインシステムとテーマ管理')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 19 + 11) % 100) + 1, '.avif'),
        'alt', concat('デザインシステムとテーマ管理イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'デザインシステム篇では CSS Modules / Tailwind / Radix UI の使い分けを整理し、',
            ' ダークモード・多言語対応・テーマ切り替えを短いサイクルで実装するためのテクニックを紹介。',
            ' コンポーネントテスト、スナップショット、パーシステンステストをどう組み合わせて品質を担保したのかも掘り下げた。',
            ' UI/UX の振り返りを行う際のメトリクスやヒートマップ分析のやり方にも触れ、読者が自分のプロジェクトで再現できるように道筋を示す。',
            ' 定量指標とユーザーインタビューの結果を統合するための分析テンプレートやヒアリング設計のコツも具体的に挙げている。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにデザイントークンの階層構造設計と命名規則、セマンティックトークンとプリミティブトークンの使い分け、',
            ' テーマ切り替え時のトランジション設計とパフォーマンス最適化についても実例を交えて詳述している。',
            ' コンポーネントカタログの運用とドキュメンテーション戦略、デザイナーと開発者の協業フローの最適化、',
            ' Figma / Sketch からのデザイントークン自動生成とコード同期の仕組みづくりについても具体的な手順を提示し、',
            ' 読者がデザインシステムを組織全体で効果的に運用できるよう実践的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、レスポンシブデザインのブレークポイント戦略、モバイルファーストアプローチの実装パターン、',
            ' タッチデバイスとマウス操作の両方に対応したインタラクション設計についても触れ、',
            ' 多様なデバイスとコンテキストで一貫したユーザー体験を提供するための包括的なアプローチを解説している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 2), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'レンダリング戦略総整理')
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'SSR / ISR / SSG / DSG を組み合わせる際の判断基準を、',
            ' ユーザーフローと SEO 期待値を踏まえて体系的に整理。',
            ' LCP / TTFB / INP を改善するための実測データと、',
            ' 具体的なパフォーマンス計測スクリプトの例を紹介し、',
            ' 開発と運用で指標のドリフトを防ぐための振り返りテンプレートも提供している。',
            ' テンプレート運用時のアクションアイテムの優先度付けや、レビューサイクルの最適な間隔についても提案を加えている。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにレンダリング戦略の選定マトリクスや、ページタイプごとの最適化パターン、',
            ' 動的コンテンツと静的コンテンツのハイブリッド構成を実現するための具体的な実装手法についても詳細に解説している。',
            ' プリレンダリングの範囲決定とクロール予算の最適化、構造化データの実装とリッチスニペット対応、',
            ' Open Graph / Twitter Card の動的生成とソーシャルメディア最適化についても実例を交えて紹介し、',
            ' 読者が SEO とパフォーマンスの両方を高いレベルで達成できるよう具体的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、Core Web Vitals の継続的な監視と改善サイクル、ラボデータとフィールドデータの相関分析、',
            ' パフォーマンスバジェットの設定と CI/CD パイプラインへの統合についても触れ、',
            ' データドリブンなパフォーマンス改善を組織文化として定着させるための実践的なアプローチを提示している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'キャッシュ無効化手順')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 23 + 13) % 100) + 1, '.avif'),
        'alt', concat('キャッシュ無効化手順イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'ステージングと本番でキャッシュ層を切り替える際のリスク管理、',
            ' Vary ヘッダーとタグ付きキャッシュの使い分け、',
            ' コンテンツ更新を即時反映するための Webhook / EventBridge 連携の解説を丁寧に収録。',
            ' CDN / Edge / ブラウザキャッシュの整合性を担保するためのモニタリング手法も触れている。',
            ' リリース後に起こりがちなキャッシュ不整合を検知するための統計的手法までフォローし、現場でそのまま活用できる知見をまとめた。',
            ' キャッシュ無効化のための手順や注意点も具体的に示し、運用チームが即座に対応できるよう配慮している。',
            ' 閾値設定サンプルや通知チャネルごとの役割分担も提示し、運用初動を円滑にするための実践的ヒントを付け加えた。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにキャッシュヒット率の監視とボトルネック分析、キャッシュウォーミング戦略の設計と実装、',
            ' パージパターンの自動化とデプロイフックとの連携についても実例を交えて詳述している。',
            ' エッジロケーションごとのキャッシュ分散とレイテンシ最適化、地理的ルーティングとマルチリージョン配信戦略、',
            ' オリジンシールドの活用とバックエンド負荷軽減の手法についても具体的な設定例とともに解説し、',
            ' 読者がグローバルスケールでの高速なコンテンツ配信を実現できるよう実践的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、セキュリティヘッダーの設定とコンテンツセキュリティポリシー、',
            ' DDoS 対策とレート制限の実装、SSL/TLS 最適化とHTTP/3 対応についても触れ、',
            ' パフォーマンスとセキュリティを両立させるための包括的なアプローチを提示している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'マルチクラウド環境の構築')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 29 + 17) % 100) + 1, '.avif'),
        'alt', concat('マルチクラウド環境の構築イメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '最後に運用章として Vercel / AWS / Cloudflare をまたいだデプロイパイプラインと監視体制の構築方法をケーススタディ形式で解説。',
            ' アセット最適化、エラートラッキング、ユーザーフィードバック収集をどう統合したか、',
            ' 実務で得たベストプラクティスを丁寧に記録した。',
            ' 読者は段階的に読み進めるだけで、チーム開発で欠かせないコーディネーションやナレッジ共有の仕組みまで理解できる構成となっている。',
            ' 情報共有テンプレートや意思決定の合意形成プロセスも掲載し、導入時のコミュニケーションコストを抑える工夫を盛り込んだ。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにマルチクラウド環境でのコスト最適化戦略や、各プラットフォームの強みを活かした適材適所のサービス選定基準、',
            ' ベンダーロックイン回避のためのポータビリティ設計についても実例を交えて詳述している。',
            ' デプロイメント戦略の比較検討とリスク評価、カナリアリリースとフィーチャーフラグの組み合わせ運用、',
            ' A/B テストとプログレッシブデリバリーの統合についても具体的な実装手順を提示し、',
            ' 読者が段階的かつ安全にリリースを進められるよう実践的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、リアルユーザーモニタリング（RUM）とシンセティック監視の使い分け、',
            ' エラーレポートの集約と自動トリアージ、インシデント対応のプレイブック作成と訓練についても触れ、',
            ' 運用チームが迅速かつ効果的に問題解決できる体制づくりの要点を丁寧に解説している。'
        ))
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 2), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'プロダクト運用の実例')
    )),
    jsonb_build_object('type', 'heading', 'attrs', jsonb_build_object('level', 3), 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', 'KPI駆動の改善サイクル')
    )),
    jsonb_build_object('type', 'mediaImage', 'attrs', jsonb_build_object(
        'src', concat('media/sample-', ((gs * 31 + 19) % 100) + 1, '.avif'),
        'alt', concat('KPI駆動の改善サイクルイメージ ', gs),
        'link', null,
        'size', 'lg',
        'align', 'center',
        'title', null,
        'caption', null
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'ユーザー調査と AB テストのサイクルをどのように組み合わせ、',
            ' KPI / OKR を更新していったのかをプロダクトフェーズごとに解説。',
            ' 支援チームとの連携方法、プロジェクトライフサイクルに合わせたロードマップの変遷、',
            ' 品質を落とさずにデリバリ速度を引き上げるための儀式設計など、',
            ' 実践で得た学びを余すところなく盛り込み、読者がチーム運営に応用できるよう詳細まで言及している。',
            ' さらにレベニュー指標との結び付け方や経営層へのレポート雛形も解説し、事業インパクトまで踏み込んだ知見を共有する。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            'さらにプロダクトディスカバリーとバリデーションのフレームワーク、仮説検証サイクルの高速化手法、',
            ' 定量データと定性フィードバックの統合分析についても実例を交えて詳述している。',
            ' プロダクトロードマップの優先順位付けとステークホルダー管理、クロスファンクショナルチームの編成と運営、',
            ' デザイン思考とリーンスタートアップの融合アプローチについても具体的な実践例を提示し、',
            ' 読者がプロダクト主導の組織文化を醸成できるよう実践的なガイダンスを提供している。'
        ))
    )),
    jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
        jsonb_build_object('type', 'text', 'text', concat(
            '加えて、カスタマージャーニーマッピングとペルソナ設計、ユーザーストーリーマッピングとバックログリファインメント、',
            ' アジャイルとプロダクトマネジメントの統合運用についても触れ、',
            ' 顧客価値の最大化と継続的なイノベーションを両立させるための包括的なフレームワークを提示している。'
        ))
    ))
))

        END AS content_json,
        CASE
            WHEN gs % 3 = 0 THEN 1
            WHEN gs % 3 = 1 THEN 2
            ELSE 3
        END AS author_id,
        CASE
            WHEN gs % 10 = 0 THEN 7
            WHEN gs % 10 = 5 THEN 8
            ELSE ((gs - 1) % 6) + 1
        END AS category_id,
        CASE
            WHEN gs % 20 = 0 THEN NULL
            WHEN gs % 15 = 0 THEN NULL
            ELSE now() - make_interval(days => 60 - gs, hours => (gs % 24))
        END AS published_at,
        ((gs - 1) % 100) + 4 AS cover_media_id,
        now() - make_interval(days => 70 - gs) AS created_at,
        now() - make_interval(days => 70 - gs, hours => (gs % 12)) AS updated_at
    FROM generate_series(1, 100) AS gs
)
INSERT INTO posts (title, slug, status, excerpt, content_json, author_id, category_id, published_at, cover_media_id, created_at, updated_at)
SELECT title, slug, status, excerpt, content_json, author_id, category_id, published_at, cover_media_id, created_at, updated_at
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
