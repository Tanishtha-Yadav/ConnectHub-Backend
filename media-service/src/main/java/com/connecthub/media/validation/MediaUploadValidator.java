package com.connecthub.media.validation;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates uploads against allowed MIME types and known browser/OS quirks
 * (e.g. ZIP as {@code application/x-zip-compressed}, JPEG as {@code image/jpg},
 * or {@code application/octet-stream} with a safe extension).
 */
public final class MediaUploadValidator {

    public static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain",
            "video/mp4", "audio/mpeg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/zip", "application/x-zip-compressed", "multipart/x-zip"
    );

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "mp4", "mp3", "mpeg",
            "doc", "docx", "zip"
    );

    private static final Map<String, String> EXT_TO_MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("mpeg", "audio/mpeg"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("zip", "application/zip")
    );

    private MediaUploadValidator() {
    }

    public static void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds 25MB limit");
        }
        if (!isAllowed(file)) {
            throw new IllegalArgumentException("File type not allowed: " + file.getContentType());
        }
    }

    public static boolean isAllowed(MultipartFile file) {
        String raw = file.getContentType();
        String ct = raw != null ? raw.toLowerCase(Locale.ROOT).trim() : null;
        String ext = extension(file.getOriginalFilename());

        if (ct != null && !ct.isEmpty() && ALLOWED_MIME.contains(ct)) {
            return true;
        }
        if (ALLOWED_EXT.contains(ext) && (ct == null || ct.isEmpty()
                || "application/octet-stream".equals(ct)
                || "binary/octet-stream".equals(ct))) {
            return true;
        }
        return false;
    }

    /** For S3 metadata when the client omitted Content-Type. */
    public static String effectiveContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank()) {
            return ct;
        }
        String ext = extension(file.getOriginalFilename());
        return EXT_TO_MIME.getOrDefault(ext, "application/octet-stream");
    }

    public static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
