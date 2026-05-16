package com.connecthub.media.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3StorageService s3StorageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        s3StorageService = new S3StorageService(s3Client, s3Presigner, bucketName);
    }

    @Test
    void testGetPresignedUrl() throws Exception {
        // Arrange
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?signature=123");
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // Act
        String url = s3StorageService.getPresignedUrl("test-key");

        // Assert
        assertEquals("https://test-bucket.s3.amazonaws.com/test-key?signature=123", url);
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void testDeleteFile() {
        // Arrange
        // (Nothing to arrange for void method without complex logic)

        // Act
        s3StorageService.deleteFile("test-key");

        // Assert
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testGetPublicUrl() {
        // Act
        String url = s3StorageService.getPublicUrl("test-key");

        // Assert
        assertEquals("https://test-bucket.s3.amazonaws.com/test-key", url);
    }
}
