package com.connecthub.media.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.connecthub.media.validation.MediaUploadValidator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * LocalStorageService — stores uploaded files on the local filesystem.
 *
 * Used as a fallback when AWS S3 credentials are not configured.
 * Files are stored under the configured upload directory (defaults to /uploads).
 * Files are served back via /api/media/files/** endpoint.
 */
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final String uploadDir;
    private final String baseUrl;

    public LocalStorageService(String uploadDir, String baseUrl) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("LocalStorageService initialized. Upload directory: {}", uploadDir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
        }
    }

    @Override
    public UploadResult uploadFile(MultipartFile file, String userId, String fileType) throws IOException {
        MediaUploadValidator.validate(file);

        String extension = getExtension(file.getOriginalFilename());
        String relativePath = fileType + "/" + userId + "/" + UUID.randomUUID() + "." + extension;
        Path targetPath = Paths.get(uploadDir, relativePath);
        Files.createDirectories(targetPath.getParent());

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("File saved locally: {}", targetPath);

        String thumbnailKey = null;
        String contentType = MediaUploadValidator.effectiveContentType(file);
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

                    thumbnailKey = fileType + "/" + userId + "/thumb_" + UUID.randomUUID() + ".jpg";
                    Path thumbPath = Paths.get(uploadDir, thumbnailKey);
                    Files.createDirectories(thumbPath.getParent());
                    Files.write(thumbPath, baos.toByteArray());
                    log.info("Thumbnail saved locally: {}", thumbPath);
                }
            } catch (Exception e) {
                log.error("Failed to generate thumbnail for {}", relativePath, e);
            }
        }

        return new UploadResult(relativePath, thumbnailKey);
    }

    @Override
    public String getPresignedUrl(String key) {
        // Local storage doesn't have pre-signed URLs — just return the direct access URL
        return getPublicUrl(key);
    }

    @Override
    public void deleteFile(String key) {
        try {
            Path filePath = Paths.get(uploadDir, key);
            Files.deleteIfExists(filePath);
            log.info("File deleted locally: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", key, e);
        }
    }

    @Override
    public String getPublicUrl(String key) {
        return baseUrl + "/api/media/files/" + key;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
