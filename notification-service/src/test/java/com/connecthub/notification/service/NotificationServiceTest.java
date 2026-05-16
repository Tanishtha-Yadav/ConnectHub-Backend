package com.connecthub.notification.service;

import com.connecthub.notification.model.Notification;
import com.connecthub.notification.model.NotificationType;
import com.connecthub.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ChannelTopic topic;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testNotification = new Notification(userId, NotificationType.SYSTEM, "Test Title", "Test Message");
        testNotification.setNotificationId(UUID.randomUUID());
    }

    @Test
    void testGetNotifications_Success() {
        // Arrange
        when(repository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Arrays.asList(testNotification));

        // Act
        List<Notification> result = notificationService.getNotifications(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Title", result.get(0).getTitle());
        verify(repository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void testGetUnreadNotifications_Success() {
        // Arrange
        when(repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)).thenReturn(Arrays.asList(testNotification));

        // Act
        List<Notification> result = notificationService.getUnreadNotifications(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Test
    void testGetUnreadCount_Success() {
        // Arrange
        when(repository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        // Act
        long count = notificationService.getUnreadCount(userId);

        // Assert
        assertEquals(5L, count);
        verify(repository, times(1)).countByUserIdAndIsReadFalse(userId);
    }

    @Test
    void testMarkAsRead_Success() {
        // Arrange
        UUID notificationId = testNotification.getNotificationId();
        when(repository.findById(notificationId)).thenReturn(Optional.of(testNotification));
        when(repository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        assertTrue(testNotification.isRead());
        verify(repository, times(1)).save(testNotification);
    }

    @Test
    void testMarkAllAsRead_Success() {
        // Arrange
        when(repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)).thenReturn(Arrays.asList(testNotification));
        when(repository.saveAll(anyList())).thenReturn(Arrays.asList(testNotification));

        // Act
        notificationService.markAllAsRead(userId);

        // Assert
        assertTrue(testNotification.isRead());
        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void testSendEmail_Success() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        boolean result = notificationService.sendEmail("test@example.com", "Subject", "Body");

        // Assert
        assertTrue(result);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmail_Failure() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        // Act
        boolean result = notificationService.sendEmail("test@example.com", "Subject", "Body");

        // Assert
        assertFalse(result);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
