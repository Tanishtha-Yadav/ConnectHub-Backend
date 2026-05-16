package com.connecthub.media.config;

import com.connecthub.media.service.LocalStorageService;
import com.connecthub.media.service.S3StorageService;
import com.connecthub.media.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * StorageConfig — wires either S3 or local filesystem storage.
 *
 * If AWS_S3_ACCESS_KEY and AWS_S3_SECRET_KEY are configured (non-placeholder),
 * the S3StorageService is used. Otherwise, LocalStorageService stores files
 * on disk so the app works without AWS credentials.
 */
@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    // Spring @Value does NOT support relaxed binding (AWS_S3_ACCESS_KEY -> aws.s3.access-key).
    // We must explicitly list BOTH the property name and the env-var name as fallbacks.
    @Value("${aws.s3.access-key:${AWS_S3_ACCESS_KEY:}}")
    private String accessKey;

    @Value("${aws.s3.secret-key:${AWS_S3_SECRET_KEY:}}")
    private String secretKey;

    @Value("${aws.s3.region:${AWS_S3_REGION:us-east-1}}")
    private String region;

    @Value("${aws.s3.bucket:${AWS_S3_BUCKET:connecthub-media}}")
    private String bucket;

    @Value("${media.upload-dir:${MEDIA_UPLOAD_DIR:/uploads}}")
    private String uploadDir;

    // IMPORTANT: In Docker, MEDIA_BASE_URL must point to the media-service itself
    // (e.g. http://media-service:8087) so served file URLs are reachable.
    @Value("${media.base-url:${MEDIA_BASE_URL:http://localhost:8087}}")
    private String baseUrl;

    private boolean hasRealAwsCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && !accessKey.startsWith("YOUR_")
                && secretKey != null && !secretKey.isBlank()
                && !secretKey.startsWith("YOUR_");
    }

    @Bean
    public StorageService storageService() {
        if (hasRealAwsCredentials()) {
            log.info("AWS credentials detected — using S3StorageService (bucket: {})", bucket);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            return new S3StorageService(s3Client, presigner, bucket);
        } else {
            log.warn("No valid AWS credentials found — falling back to LocalStorageService (dir: {})", uploadDir);
            return new LocalStorageService(uploadDir, baseUrl);
        }
    }
}
