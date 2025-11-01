// filepath: backend/src/main/java/com/example/backend/batch/DemoContentResetScheduler.java
package com.example.backend.batch;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.context.event.EventListener;

import com.example.backend.config.DemoResetProperties;
import com.example.backend.config.MediaStorageProperties;
import com.example.backend.repository.UserRepository;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * デモ環境用の投稿・メディア初期化バッチ。
 * <ul>
 * <li>DBリセット: SQLスクリプトで投稿・タグ等を初期化</li>
 * <li>メディア削除: S3バケット配下のサンプル画像を一括削除</li>
 * <li>サンプル画像アップロード: 指定ディレクトリからS3へ再投入</li>
 * <li>初回起動時・定期実行（@Scheduled/@EventListener）</li>
 * </ul>
 * 設定は application.properties の app.demo-reset.* で制御。
 * 
 * @see DemoResetProperties
 * @see MediaStorageProperties
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-reset", name = "enabled", havingValue = "true")
public class DemoContentResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemoContentResetScheduler.class);

    private final DataSource dataSource;
    private final S3Client s3Client;
    private final MediaStorageProperties mediaStorageProperties;
    private final DemoResetProperties demoResetProperties;
    private final ResourcePatternResolver resourcePatternResolver;
    private final UserRepository userRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DemoContentResetScheduler(DataSource dataSource, S3Client s3Client,
            MediaStorageProperties mediaStorageProperties, DemoResetProperties demoResetProperties,
            ResourceLoader resourceLoader, UserRepository userRepository) {
        this.dataSource = dataSource;
        this.s3Client = s3Client;
        this.mediaStorageProperties = mediaStorageProperties;
        this.demoResetProperties = demoResetProperties;
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        this.userRepository = userRepository;
    }

    /**
     * デモ環境の投稿・メディアを初期化するバッチ本体（定期実行用）。
     * <ul>
     * <li>DBリセット → メディア削除 → サンプル画像再アップロード</li>
     * <li>多重起動防止のため AtomicBoolean で排他制御</li>
     * <li>簡易シード有効時は定期実行をスキップ</li>
     * </ul>
     */
    @Scheduled(initialDelayString = "${app.demo-reset.initial-delay:PT1M}", fixedDelayString = "${app.demo-reset.fixed-delay:PT30M}")
    public void resetDemoContent() {
        // 簡易シード有効時は定期実行をスキップ
        if (demoResetProperties.isMinimalSeedOnStartup()) {
            log.debug("Minimal seed enabled, skipping scheduled full reset");
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.info("Demo reset already running, skipping this trigger");
            return;
        }
        try {
            log.info("Demo reset: start");
            resetDatabase();
            purgeMediaObjects();
            uploadSampleMedia();
            log.info("Demo reset: completed");
        } catch (Exception ex) {
            log.error("Demo reset: failed", ex);
        } finally {
            running.set(false);
        }
    }

    /**
     * アプリケーション起動時にデモリセットを即時実行。
     * 簡易シードが有効な場合は簡易シードを実行、無効な場合はフルシードを実行。
     * 
     * @param event Spring Boot の ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (demoResetProperties.isMinimalSeedOnStartup()) {
            executeMinimalSeed();
        } else {
            resetDemoContent();
        }
    }

    /**
     * 簡易シードを実行(初回起動時用)。
     * ユーザー3名・カテゴリ2件・タグ5件・サンプル投稿1件のみ投入。
     * メディアアップロードは行わない。
     * 既にユーザーが存在する場合はスキップ（フルシード投入後の再起動対策）。
     */
    public void executeMinimalSeed() {
        if (!running.compareAndSet(false, true)) {
            log.info("Minimal seed already running, skipping this trigger");
            return;
        }
        try {
            // ユーザーが既に存在する場合はスキップ
            if (userRepository.count() > 0) {
                log.info("Minimal seed: users already exist, skipping seed execution");
                return;
            }

            log.info("Minimal seed: start");
            resetDatabaseWithScript(demoResetProperties.getMinimalSeedScript());
            log.info("Minimal seed: completed");
        } catch (Exception ex) {
            log.error("Minimal seed: failed", ex);
        } finally {
            running.set(false);
        }
    }

    /**
     * フルシード実行（手動実行用）。
     * 投稿100件・メディア多数を投入し、定期実行と同じ処理を実行。
     * 簡易シード設定を無視して強制的にフルリセットを実行。
     */
    public void executeFullSeed() {
        if (!running.compareAndSet(false, true)) {
            log.info("Demo reset already running, skipping this trigger");
            return;
        }
        try {
            log.info("Demo reset (manual): start");
            resetDatabase();
            purgeMediaObjects();
            uploadSampleMedia();
            log.info("Demo reset (manual): completed");
        } catch (Exception ex) {
            log.error("Demo reset (manual): failed", ex);
        } finally {
            running.set(false);
        }
    }

    /**
     * SQLスクリプト（seed_full.sql）でDBを初期化。
     * 投稿・タグ・ユーザー等のデモデータを投入。
     */
    private void resetDatabase() {
        resetDatabaseWithScript(demoResetProperties.getFullSeedScript());
    }

    /**
     * 指定されたSQLスクリプトでDBを初期化。
     * 
     * @param scriptPath SQLスクリプトのクラスパス相対パス
     */
    private void resetDatabaseWithScript(String scriptPath) {
        Resource scriptResource = resolveSeedScript(scriptPath);
        try (Connection connection = this.dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, scriptResource);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to reset database using script " + scriptResource.getDescription(), ex);
        }
    }

    private Resource resolveSeedScript(String scriptPath) {
        if (!StringUtils.hasText(scriptPath)) {
            throw new IllegalArgumentException("Seed script path must not be empty");
        }
        Resource resource = resourcePatternResolver.getResource(scriptPath);
        if (!resource.exists()) {
            throw new IllegalStateException("Seed script not found: " + scriptPath);
        }
        return resource;
    }

    /**
     * S3バケット配下のサンプル画像を一括削除。
     * バケット・プレフィックス未設定時は安全のためスキップ。
     */
    private void purgeMediaObjects() {
        String bucket = mediaStorageProperties.getBucket();
        if (!StringUtils.hasText(bucket)) {
            log.warn("Demo reset: S3 bucket not configured, skipping storage purge");
            return;
        }
        String prefix = resolveObjectPrefix();
        if (!StringUtils.hasText(prefix)) {
            log.warn("Demo reset: object prefix empty, skipping purge to avoid deleting entire bucket");
            return;
        }

        log.info("Demo reset: deleting objects under prefix {}/{}", bucket, prefix);
        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix + "/");
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }
            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            if (!response.hasContents() || response.contents().isEmpty()) {
                continuationToken = response.nextContinuationToken();
                continue;
            }
            for (S3Object object : response.contents()) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(object.key())
                            .build());
                } catch (S3Exception ex) {
                    log.warn("Demo reset: failed to delete object {}", object.key(), ex);
                }
            }
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);
    }

    /**
     * サンプル画像ディレクトリからS3へ画像を再アップロード。
     * ファイル名昇順で投入、失敗時はWARNログ。
     * 
     * @throws IOException ファイル読み込み失敗時
     */
    private void uploadSampleMedia() throws IOException {
        String bucket = mediaStorageProperties.getBucket();
        if (!StringUtils.hasText(bucket)) {
            log.warn("Demo reset: S3 bucket not configured, skipping sample upload");
            return;
        }
        Resource[] resources = resourcePatternResolver.getResources(demoResetProperties.getSampleLocation());
        if (resources.length == 0) {
            log.warn("Demo reset: no sample media found for pattern {}", demoResetProperties.getSampleLocation());
            return;
        }
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (Resource resource : resources) {
            if (!resource.exists() || !resource.isReadable()) {
                continue;
            }
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            String storageKey = buildStorageKey(filename);
            try (InputStream inputStream = resource.getInputStream()) {
                long contentLength = resource.contentLength();
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .contentType(demoResetProperties.getContentType())
                        .build();
                s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
                log.debug("Demo reset: uploaded sample {} to {}", filename, storageKey);
            } catch (S3Exception ex) {
                log.warn("Demo reset: failed to upload sample {} to {}", filename, storageKey, ex);
            }
        }
    }

    /**
     * S3保存用のストレージキーを構築。
     * キープレフィックス・メディアフォルダを考慮して連結する。
     * 
     * @param filename
     * @return
     */
    /**
     * S3保存用のストレージキーを構築。
     * sampleディレクトリ配下に格納。
     * 
     * @param filename ファイル名
     * @return S3キー（例: sample/sample-59.avif）
     */
    private String buildStorageKey(String filename) {
        return "sample/" + trimLeadingSlash(Objects.requireNonNull(filename));
    }

    /**
     * S3オブジェクトプレフィックスを構築。
     * キープレフィックス・メディアフォルダを考慮して連結する。
     * 
     * @return
     */
    /**
     * S3オブジェクトプレフィックスを構築。
     * sampleディレクトリ固定。
     * 
     * @return S3プレフィックス（例: sample）
     */
    private String resolveObjectPrefix() {
        return "sample";
    }

    /**
     * 文字列の先頭のスラッシュを削除。
     * 
     * @param value
     * @return
     */
    private String trimLeadingSlash(String value) {
        String v = value;
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        return v;
    }
}
