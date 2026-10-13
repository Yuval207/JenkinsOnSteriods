package com.systemguard.config;

import com.systemguard.common.storage.MinioObjectStorageService;
import com.systemguard.common.storage.ObjectStorageService;
import com.systemguard.common.storage.SupabaseStorageService;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AppConfig {

    @Bean
    public ObjectStorageService objectStorageService(
            @Value("${systemguard.s3.endpoint:https://lrfosirsnujasknyehuv.storage.supabase.co/storage/v1/s3}") String endpoint,
            @Value("${systemguard.s3.access-key:}") String accessKey,
            @Value("${systemguard.s3.secret-key:}") String secretKey,
            @Value("${systemguard.s3.region:ap-southeast-1}") String region) {

        if (endpoint != null && endpoint.contains("supabase.co")) {
            return new SupabaseStorageService(endpoint, accessKey);
        }

        MinioClient.Builder builder = MinioClient.builder().endpoint(endpoint);
        if (accessKey != null && !accessKey.isBlank()) {
            builder.credentials(accessKey, secretKey);
        }
        if (region != null && !region.isBlank()) {
            builder.region(region);
        }
        return new MinioObjectStorageService(builder.build());
    }

    @Bean(name = "scanExecutor")
    public Executor scanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);    // Optimized for lightweight machines
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("scan-");
        executor.setAllowCoreThreadTimeOut(true);  // Release idle threads
        executor.initialize();
        return executor;
    }
}
