package com.connecthub.media.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * StorageService — abstraction over file storage backends.
 * Implementation is chosen at startup:
 *   - S3StorageService when AWS credentials are configured
 *   - LocalStorageService when running locally without AWS
 */
public interface StorageService {

    class UploadResult {
        public String key;
        public String thumbnailKey;
        public UploadResult(String key, String thumbnailKey) {
            this.key = key;
            this.thumbnailKey = thumbnailKey;
        }
    }

    UploadResult uploadFile(MultipartFile file, String userId, String fileType) throws IOException;

    String getPresignedUrl(String key);

    void deleteFile(String key);

    String getPublicUrl(String key);
}
