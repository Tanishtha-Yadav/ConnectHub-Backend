package com.connecthub.media.controller;

import com.connecthub.media.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final StorageService storageService;

    @Value("${media.upload-dir:/uploads}")
    private String uploadDir;

    @Value("${jwt.secret:ConnectHub_JWT_Secret_Key_2026!@#$%^&*_SuperSecure_abcdefghijklmnopqrstuvwxyz_ABCDEF}")
    private String jwtSecret;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * POST /api/media/upload — Upload a file.
     * Routes to S3 or local disk depending on configured StorageService.
     *
     * WHY we extract userId from both Authentication and the header:
     * If the JWT filter fails to populate SecurityContextHolder (e.g. token edge cases),
     * auth.getName() would throw NPE. We fall back to parsing the header directly.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "attachment") String type,
            Authentication auth,
            jakarta.servlet.http.HttpServletRequest request) throws IOException {

        // Resolve userId: prefer SecurityContext (populated by JWT filter), fallback to header
        String userId = (auth != null) ? auth.getName() : null;
        if (userId == null || userId.isBlank()) {
            // Manually extract from Authorization header as fallback
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                            jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    userId = claims.getSubject();
                } catch (Exception e) {
                    // token invalid
                }
            }
        }
        if (userId == null || userId.isBlank()) {
            Map<String, String> err = new java.util.HashMap<>();
            err.put("error", "Unauthorized: no valid user identity");
            return ResponseEntity.status(401).body(err);
        }

        StorageService.UploadResult result = storageService.uploadFile(file, userId, type);

        Map<String, String> response = new HashMap<>();
        response.put("key", result.key);
        response.put("url", storageService.getPublicUrl(result.key));
        response.put("presignedUrl", storageService.getPresignedUrl(result.key));
        if (result.thumbnailKey != null) {
            response.put("thumbnailUrl", storageService.getPresignedUrl(result.thumbnailKey));
        }
        response.put("message", "File uploaded successfully");
        return ResponseEntity.ok(response);
    }

    /** GET /api/media/download?key=... — Get a download URL */
    @GetMapping("/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@RequestParam String key) {
        Map<String, String> response = new HashMap<>();
        response.put("url", storageService.getPresignedUrl(key));
        return ResponseEntity.ok(response);
    }

    /** DELETE /api/media/delete?key=... — Delete a file */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam String key) {
        storageService.deleteFile(key);
        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/media/files/** — Serve locally stored files.
     * Only active when LocalStorageService is used (no S3 credentials).
     */
    @GetMapping("/files/**")
    public ResponseEntity<Resource> serveFile(jakarta.servlet.http.HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String fileKey = requestUri.replaceFirst(".*/api/media/files/", "");
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileKey).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** GET /api/media/health */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> r = new HashMap<>();
        r.put("status", "Media Service is running");
        r.put("storage", storageService.getClass().getSimpleName());
        return ResponseEntity.ok(r);
    }
}
