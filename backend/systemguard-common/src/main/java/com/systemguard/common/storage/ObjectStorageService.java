package com.systemguard.common.storage;

import java.io.InputStream;

/**
 * Abstraction for S3-compatible object storage.
 * Implementations: MinIO for local dev, AWS S3 for production.
 */
public interface ObjectStorageService {

    /**
     * Store an object in the given bucket with the given key.
     */
    void putObject(String bucket, String key, InputStream inputStream, long size, String contentType);

    /**
     * Store a byte array.
     */
    void putObject(String bucket, String key, byte[] data, String contentType);

    /**
     * Retrieve an object.
     */
    InputStream getObject(String bucket, String key);

    /**
     * Check if an object exists.
     */
    boolean objectExists(String bucket, String key);

    /**
     * Delete an object.
     */
    void deleteObject(String bucket, String key);

    /**
     * Generate a pre-signed URL for temporary access.
     */
    String getPresignedUrl(String bucket, String key, int expirySeconds);

    /**
     * Ensure a bucket exists, creating it if necessary.
     */
    void ensureBucketExists(String bucket);
}
