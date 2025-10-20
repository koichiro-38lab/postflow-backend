package com.example.backend.service.media;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * メディアファイルのストレージ操作を抽象化するインターフェース。
 * <p>
 * S3やMinIO等の外部ストレージ実装に依存せず、
 * アップロード/ダウンロード用の署名付きURL発行・存在確認・削除などを統一APIで提供する。
 * 管理画面・API経由のメディア管理、投稿本文画像の保存・取得用途で利用。
 * </p>
 */
public interface MediaStorage {

    /**
     * 署名付きアップロードURL情報を保持するレコード。
     * <ul>
     * <li>url: PUT先の一時URL</li>
     * <li>headers: 必要な追加ヘッダ（例: Content-Type, x-amz-...）</li>
     * <li>expiresAt: 有効期限（UTC）</li>
     * <li>storageKey: ストレージ内の一意キー</li>
     * </ul>
     */
    record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt, String storageKey) {
    }

    /**
     * 署名付きダウンロードURL情報を保持するレコード。
     * <ul>
     * <li>url: GET用の一時URL</li>
     * <li>expiresAt: 有効期限（UTC）</li>
     * </ul>
     */
    record PresignedDownload(String url, Instant expiresAt) {
    }

    /**
     * 指定キー・コンテンツタイプ・サイズで署名付きアップロードURLを発行する。
     * <p>
     * 主に管理画面やAPI経由のメディア新規登録時に利用。
     * </p>
     * 
     * @param storageKey    ストレージ内の保存先キー（例: media/uuid.jpg）
     * @param contentType   アップロードするファイルのContent-Type
     * @param contentLength バイト長
     * @param ttl           URL有効期間
     * @return PresignedUpload情報
     * @throws StorageException ストレージ連携失敗時
     */
    PresignedUpload createUploadUrl(String storageKey, String contentType, long contentLength, Duration ttl);

    /**
     * 指定キーで署名付きダウンロードURLを発行する。
     * <p>
     * 主にメディア画像の表示・ダウンロード用途で利用。
     * </p>
     * 
     * @param storageKey ストレージ内の保存先キー
     * @param ttl        URL有効期間
     * @return PresignedDownload情報
     * @throws StorageException ストレージ連携失敗時
     */
    PresignedDownload createDownloadUrl(String storageKey, Duration ttl);

    /**
     * 指定キーのオブジェクトがストレージ上に存在するか検証する。
     * <p>
     * 存在しない場合はObjectNotFoundExceptionをスロー。
     * </p>
     * 
     * @param storageKey ストレージ内の保存先キー
     * @throws ObjectNotFoundException オブジェクトが存在しない場合
     * @throws StorageException        ストレージ連携失敗時
     */
    void ensureObjectExists(String storageKey) throws ObjectNotFoundException;

    /**
     * 指定キーのオブジェクトをストレージから削除する。
     * <p>
     * 存在しない場合は例外をスローしない（冪等）。
     * </p>
     * 
     * @param storageKey ストレージ内の保存先キー
     * @throws StorageException ストレージ連携失敗時
     */
    void deleteObject(String storageKey);

    /**
     * ストレージ操作全般の基底例外。
     * <p>
     * 連携先ストレージの障害・認証エラー等で発生。
     * </p>
     */
    class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }

        public StorageException(String message) {
            super(message);
        }
    }

    /**
     * 指定キーのオブジェクトがストレージ上に存在しない場合の例外。
     * <p>
     * 主にensureObjectExistsで利用。
     * </p>
     */
    class ObjectNotFoundException extends StorageException {
        public ObjectNotFoundException(String storageKey, Throwable cause) {
            super("Media object not found for key: " + storageKey, cause);
        }

        public ObjectNotFoundException(String storageKey) {
            super("Media object not found for key: " + storageKey);
        }
    }
}
