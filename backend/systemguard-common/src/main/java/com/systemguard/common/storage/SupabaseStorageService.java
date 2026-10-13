package com.systemguard.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Supabase Storage implementation using Supabase Storage REST API.
 * Communicates with https://<project-ref>.supabase.co/storage/v1
 */
public class SupabaseStorageService implements ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public SupabaseStorageService(String endpoint, String apiKey) {
        // Normalize endpoint to base REST URL: https://<ref>.supabase.co/storage/v1
        String clean = endpoint.trim();
        if (clean.endsWith("/s3")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        this.baseUrl = clean;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("Initialized Supabase Storage at: {}", this.baseUrl);
    }

    @Override
    public void putObject(String bucket, String key, InputStream inputStream, long size, String contentType) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            putObject(bucket, key, bytes, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream for " + bucket + "/" + key, e);
        }
    }

    @Override
    public void putObject(String bucket, String key, byte[] data, String contentType) {
        try {
            ensureBucketExists(bucket);
            // PUT /object/<bucket>/<key> (upsert)
            String url = baseUrl + "/object/" + bucket + "/" + key;
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data));

            if (!apiKey.isEmpty()) {
                req.header("apikey", apiKey);
                req.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> res = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.debug("Stored object in Supabase Storage: {}/{}", bucket, key);
            } else {
                log.warn("Supabase Storage upload warning (status {}): {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.error("Failed to upload object to Supabase: {}/{}", bucket, key, e);
        }
    }

    @Override
    public InputStream getObject(String bucket, String key) {
        try {
            String url = baseUrl + "/object/authenticated/" + bucket + "/" + key;
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

            if (!apiKey.isEmpty()) {
                req.header("apikey", apiKey);
                req.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<InputStream> res = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                return res.body();
            }
            throw new RuntimeException("Failed to get object, status: " + res.statusCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object from Supabase: " + bucket + "/" + key, e);
        }
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        try {
            String url = baseUrl + "/object/info/authenticated/" + bucket + "/" + key;
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

            if (!apiKey.isEmpty()) {
                req.header("apikey", apiKey);
                req.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<Void> res = httpClient.send(req.build(), HttpResponse.BodyHandlers.discarding());
            return res.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try {
            String url = baseUrl + "/object/" + bucket + "/" + key;
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE();

            if (!apiKey.isEmpty()) {
                req.header("apikey", apiKey);
                req.header("Authorization", "Bearer " + apiKey);
            }

            httpClient.send(req.build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.error("Failed to delete object from Supabase: {}/{}", bucket, key, e);
        }
    }

    @Override
    public String getPresignedUrl(String bucket, String key, int expirySeconds) {
        return baseUrl + "/object/public/" + bucket + "/" + key;
    }

    @Override
    public void ensureBucketExists(String bucket) {
        try {
            String url = baseUrl + "/bucket";
            String payload = String.format("{\"id\":\"%s\",\"name\":\"%s\",\"public\":true}", bucket, bucket);
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            if (!apiKey.isEmpty()) {
                req.header("apikey", apiKey);
                req.header("Authorization", "Bearer " + apiKey);
            }

            httpClient.send(req.build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.debug("ensureBucketExists check: {}", e.getMessage());
        }
    }
}
