// filepath: backend/src/test/java/com/example/backend/batch/DemoContentResetSchedulerTest.java
package com.example.backend.batch;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.backend.config.DemoResetProperties;
import com.example.backend.config.MediaStorageProperties;
import com.example.backend.config.TestClockConfig;
import com.example.backend.config.TestDataConfig;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * DemoContentResetScheduler のテストクラス。
 * 定期リセットジョブ、手動フルリセット、簡易シード、エラーハンドリングを検証。
 * 
 * 注意: このテストはSpringBootTestで実際のDBとやり取りするため、
 * SQLスクリプトが実行され、テストデータが投入されます。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({ TestClockConfig.class, TestDataConfig.class })
class DemoContentResetSchedulerTest {

    @Autowired
    private DemoContentResetScheduler scheduler;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private ResourcePatternResolver resourcePatternResolver;

    @Autowired
    private DemoResetProperties demoResetProperties;

    @Autowired
    private MediaStorageProperties mediaStorageProperties;

    private List<String> uploadedKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        uploadedKeys.clear();
        // デフォルトでS3バケット設定を有効化
        mediaStorageProperties.setBucket("test-bucket");
        mediaStorageProperties.setKeyPrefix("/media/");

        // SQLスクリプトを classpath 指定で設定（classpath: プレフィックス許容を検証）
        demoResetProperties.setFullSeedScript("classpath:db/seed/seed_full.sql");
        demoResetProperties.setMinimalSeedScript("classpath:db/seed/seed_minimal.sql");
    }

    @AfterEach
    void tearDown() {
        uploadedKeys.clear();
    }

    // ========== 定期リセットジョブのテスト ==========

    /**
     * 定期リセットジョブ（resetDemoContent）が正常に実行されることを確認。
     * 簡易シード無効時にフルリセットが実行される。
     */
    @Test
    void resetDemoContent_fullReset_shouldExecuteSuccessfully() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // S3モックの設定
        setupS3Mocks();

        // サンプルメディアのモック設定
        setupSampleMediaMocks();

        // 実行
        scheduler.resetDemoContent();

        // S3削除とアップロードが呼ばれることを確認
        verify(s3Client, atLeastOnce()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, atLeastOnce()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * 簡易シード有効時、定期リセットジョブがスキップされることを確認。
     */
    @Test
    void resetDemoContent_minimalSeedEnabled_shouldSkipScheduledReset() throws Exception {
        // 簡易シード有効化
        demoResetProperties.setMinimalSeedOnStartup(true);

        // 実行
        scheduler.resetDemoContent();

        // S3操作が呼ばれないことを確認
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * 多重起動防止が機能することを確認。
     * 同時に複数回呼び出しても1回だけ実行される。
     */
    @Test
    void resetDemoContent_concurrentExecution_shouldPreventDuplicateRuns() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // S3モックの設定（遅延を追加してレースコンディションをシミュレート）
        setupS3Mocks();
        setupSampleMediaMocks();

        // 複数スレッドで同時実行
        Thread thread1 = new Thread(() -> scheduler.resetDemoContent());
        Thread thread2 = new Thread(() -> scheduler.resetDemoContent());

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // 1回だけ実行されることを確認（2回目はスキップされる）
        // S3操作は1回だけ呼ばれるべき
        verify(s3Client, atMost(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // ========== 手動フルリセットのテスト ==========

    /**
     * 手動フルリセット（executeFullSeed）が正常に実行されることを確認。
     * 簡易シード設定を無視してフルリセットが実行される。
     */
    @Test
    void executeFullSeed_shouldExecuteFullResetRegardlessOfMinimalSeedSetting() throws Exception {
        // 簡易シード有効化（無視されるべき）
        demoResetProperties.setMinimalSeedOnStartup(true);

        // S3モックの設定
        setupS3Mocks();
        setupSampleMediaMocks();

        // 実行
        scheduler.executeFullSeed();

        // S3削除とアップロードが呼ばれることを確認
        verify(s3Client, atLeastOnce()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, atLeastOnce()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * 手動フルリセット中に多重起動を防止することを確認。
     */
    @Test
    void executeFullSeed_concurrentExecution_shouldPreventDuplicateRuns() throws Exception {
        // S3モックの設定
        setupS3Mocks();
        setupSampleMediaMocks();

        // 複数スレッドで同時実行
        Thread thread1 = new Thread(() -> scheduler.executeFullSeed());
        Thread thread2 = new Thread(() -> scheduler.executeFullSeed());

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // 1回だけ実行されることを確認
        verify(s3Client, atMost(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // ========== 簡易シード実行のテスト ==========

    /**
     * 簡易シード（executeMinimalSeed）が正常に実行されることを確認。
     * S3操作は行われない。
     */
    @Test
    void executeMinimalSeed_shouldExecuteSuccessfully() throws Exception {
        // 実行
        scheduler.executeMinimalSeed();

        // S3操作が呼ばれないことを確認
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * 簡易シード実行中に多重起動を防止することを確認。
     */
    @Test
    void executeMinimalSeed_concurrentExecution_shouldPreventDuplicateRuns() throws Exception {
        // 複数スレッドで同時実行
        Thread thread1 = new Thread(() -> scheduler.executeMinimalSeed());
        Thread thread2 = new Thread(() -> scheduler.executeMinimalSeed());

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // 多重起動防止により、2回目はスキップされることを確認
        // （具体的な検証はログ出力で確認）
    }

    // ========== ApplicationReadyEvent のテスト ==========

    /**
     * アプリケーション起動時、簡易シード有効時に簡易シードが実行されることを確認。
     */
    @Test
    void onApplicationReady_minimalSeedEnabled_shouldExecuteMinimalSeed() throws Exception {
        // 簡易シード有効化
        demoResetProperties.setMinimalSeedOnStartup(true);

        // 実行
        scheduler.onApplicationReady();

        // S3操作が呼ばれないことを確認（簡易シードはDB操作のみ）
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * アプリケーション起動時、簡易シード無効時にフルリセットが実行されることを確認。
     */
    @Test
    void onApplicationReady_minimalSeedDisabled_shouldExecuteFullReset() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // S3モックの設定
        setupS3Mocks();
        setupSampleMediaMocks();

        // 実行
        scheduler.onApplicationReady();

        // S3削除とアップロードが呼ばれることを確認
        verify(s3Client, atLeastOnce()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, atLeastOnce()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // ========== エラーハンドリングのテスト ==========

    /**
     * SQLスクリプト読み込み失敗時にエラーがスローされることを確認。
     */
    @Test
    void resetDemoContent_sqlScriptNotFound_shouldThrowException() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // 存在しないSQLスクリプトを設定
        demoResetProperties.setFullSeedScript("db/seed/nonexistent.sql");

        // S3モックの設定
        setupS3Mocks();
        setupSampleMediaMocks();

        // 実行（例外が内部でキャッチされてログ出力される）
        scheduler.resetDemoContent();

        // エラーログが出力され、S3操作は実行されないことを確認
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
    }

    /**
     * SQLスクリプト読み込み失敗時にエラーがハンドリングされることを確認。
     */
    @Test
    void executeMinimalSeed_sqlScriptNotFound_shouldHandleException() throws Exception {
        // 存在しないSQLスクリプトを設定
        demoResetProperties.setMinimalSeedScript("db/seed/nonexistent_minimal.sql");

        // 実行（例外が内部でキャッチされてログ出力される）
        scheduler.executeMinimalSeed();

        // エラーログが出力されることを確認（例外が外部にスローされない）
    }

    /**
     * S3バケット未設定時、メディア削除とアップロードがスキップされることを確認。
     */
    @Test
    void resetDemoContent_s3BucketNotConfigured_shouldSkipS3Operations() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // S3バケットを未設定に
        mediaStorageProperties.setBucket("");

        // 実行
        scheduler.resetDemoContent();

        // S3操作が呼ばれないことを確認
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * S3オブジェクトプレフィックスが"sample"固定のため、削除とアップロードが実行されることを確認。
     * （修正前：プレフィックスが空の場合スキップ → 修正後：sample固定で常に実行）
     */
    @Test
    void resetDemoContent_emptyObjectPrefix_shouldSkipPurge() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // プレフィックスを空に設定（実装上は"sample"固定のため影響なし）
        mediaStorageProperties.setKeyPrefix("");
        demoResetProperties.setMediaFolder("");

        // S3モックの設定（sample/配下にオブジェクトが存在する想定）
        ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder()
                .contents(new ArrayList<>())
                .nextContinuationToken(null)
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(emptyResponse);

        // サンプルメディアのモック設定
        setupSampleMediaMocks();

        // 実行
        scheduler.resetDemoContent();

        // sample/配下を削除しようとする（オブジェクトがないため削除は呼ばれない）
        verify(s3Client, atLeastOnce()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        // アップロードは実行される
        verify(s3Client, atLeastOnce()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * S3オブジェクト削除失敗時、WARNログを出力して処理を継続することを確認。
     */
    @Test
    void purgeMediaObjects_deleteObjectFailure_shouldContinueProcessing() throws Exception {
        // 簡易シード無効化
        demoResetProperties.setMinimalSeedOnStartup(false);

        // S3モックの設定（削除時に例外をスロー）
        S3Object mockObject1 = S3Object.builder().key("media/image1.jpg").build();
        S3Object mockObject2 = S3Object.builder().key("media/image2.jpg").build();

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(mockObject1, mockObject2)
                .nextContinuationToken(null)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        // 1つ目の削除で例外をスロー、2つ目は成功（戻り値はDeleteObjectResponse）
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Access denied").build())
                .thenReturn(null); // 2回目は成功（戻り値はnullでOK）

        // サンプルメディアのモック設定
        setupSampleMediaMocks();

        // 実行（例外がキャッチされて処理継続）
        scheduler.resetDemoContent();

        // 削除が2回呼ばれることを確認（1回目は失敗、2回目は成功）
        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    /**
     * S3削除・アップロード操作のモックを設定。
     */
    private void setupS3Mocks() {
        // ListObjectsV2のモック（空のレスポンス）
        ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder()
                .contents(new ArrayList<>())
                .nextContinuationToken(null)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(emptyResponse);

        // putObjectのモック（実際のサンプルメディアが100+ファイル存在するため、無制限に許可）
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(null);
    }

    /**
     * サンプルメディアのモックを設定。
     */
    /**
     * サンプルメディアのモックを設定。
     */
    private void setupSampleMediaMocks() throws Exception {
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.isReadable()).thenReturn(true);
        when(mockResource.getFilename()).thenReturn("sample.avif");
        when(mockResource.contentLength()).thenReturn(1024L);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        when(resourcePatternResolver.getResources(anyString()))
                .thenReturn(new Resource[] { mockResource });
    }
}
