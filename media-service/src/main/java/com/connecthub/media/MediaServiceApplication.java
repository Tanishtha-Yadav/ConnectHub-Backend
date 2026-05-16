package com.connecthub.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Media/File Service — handles file uploads and downloads via AWS S3.
 *
 * FEATURES:
 *   - Upload images, documents, and media files
 *   - Generate pre-signed URLs for secure downloads
 *   - Support for avatars, chat attachments, and channel icons
 *   - File size and type validation
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MediaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
