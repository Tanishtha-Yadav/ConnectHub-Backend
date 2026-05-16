package com.connecthub.media.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.connecthub.media.validation.MediaUploadValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S3StorageService — upload, download, delete files from AWS S3.
 *
 * FILE NAMING STRATEGY:
 *   Files are stored as: {type}/{userId}/{uuid}.{extension}
 *   Example: avatars/550e8400.../abc123.jpg
 *   This prevents name collisions and organizes files by type.
 */
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner presigner;

    private final String bucketName;

    public S3StorageService(S3Client s3Client, S3Presigner presigner, String bucketName) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucketName = bucketName;
    }



    /**
     * Upload a file to S3.
     * Returns an UploadResult containing the S3 key (path) of the uploaded file and thumbnail (if generated).
     */
    @Override
    public UploadResult uploadFile(MultipartFile file, String userId, String fileType) throws IOException {
        MediaUploadValidator.validate(file);

        // Generate unique key
        String extension = getExtension(file.getOriginalFilename());
        String key = String.format("%s/%s/%s.%s", fileType, userId, UUID.randomUUID(), extension);
        String contentType = MediaUploadValidator.effectiveContentType(file);

        // Upload to S3
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("File uploaded: s3://{}/{}", bucketName, key);

        String thumbnailKey = null;
        if (contentType.startsWith("image/")) {
            try {
                BufferedImage originalImage = ImageIO.read(file.getInputStream());
                if (originalImage != null) {
                    int targetWidth = 200;
                    int targetHeight = (int) (originalImage.getHeight() * ((double) targetWidth / originalImage.getWidth()));
                    BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = thumbnail.createGraphics();
                    g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                    g.dispose();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(thumbnail, "jpg", baos);
                    byte[] thumbBytes = baos.toByteArray();

                    thumbnailKey = String.format("%s/%s/thumb_%s.jpg", fileType, userId, UUID.randomUUID());
                    PutObjectRequest thumbPutRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(thumbnailKey)
                            .contentType("image/jpeg")
                            .contentLength((long) thumbBytes.length)
                            .build();
                    s3Client.putObject(thumbPutRequest, RequestBody.fromBytes(thumbBytes));
                    log.info("Thumbnail uploaded: s3://{}/{}", bucketName, thumbnailKey);
                }
            } catch (Exception e) {
                log.error("Failed to generate thumbnail for {}", key, e);
            }
        }

        return new UploadResult(key, thumbnailKey);
    }

    /**
     * Generate a pre-signed URL for downloading a file.
     * URL is valid for 1 hour.
     *
     * WHY pre-signed URLs?
     *   The file is stored in a private S3 bucket. Instead of streaming
     *   it through our server, we generate a temporary URL that lets
     *   the client download directly from S3. Much more efficient.
     */
    @Override
    public String getPresignedUrl(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    /** Delete a file from S3 */
    @Override
    public void deleteFile(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
        log.info("File deleted: s3://{}/{}", bucketName, key);
    }

    /** Get the public URL of a file (if bucket has public access) */
    @Override
    public String getPublicUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
