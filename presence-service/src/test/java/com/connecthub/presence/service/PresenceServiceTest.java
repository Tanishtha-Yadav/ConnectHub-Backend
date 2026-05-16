package com.connecthub.presence.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PresenceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private PresenceService presenceService;

    private String userId = "user-123";

    @BeforeEach
    void setUp() {
        // We only mock the ops when needed in specific tests, but we can set lenient here if we wanted.
        // Or we just rely on the tests to mock what they need.
    }

    @Test
    void testSetOnline() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        presenceService.setOnline(userId);

        verify(setOperations, times(1)).add("presence:online", userId);
        verify(valueOperations, times(1)).set(eq("presence:user:" + userId), eq("online"), any(Duration.class));
    }

    @Test
    void testSetOffline() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        presenceService.setOffline(userId);

        verify(setOperations, times(1)).remove("presence:online", userId);
        verify(redisTemplate, times(1)).delete("presence:user:" + userId);
        verify(valueOperations, times(1)).set(eq("presence:lastseen:" + userId), anyString());
    }

    @Test
    void testIsOnline_True() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        when(setOperations.isMember("presence:online", userId)).thenReturn(true);
        when(valueOperations.get("presence:user:" + userId)).thenReturn("online");

        boolean result = presenceService.isOnline(userId);

        assertTrue(result);
    }

    @Test
    void testIsOnline_False_StaleEntry() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        when(setOperations.isMember("presence:online", userId)).thenReturn(true);
        when(valueOperations.get("presence:user:" + userId)).thenReturn(null); // Stale

        boolean result = presenceService.isOnline(userId);

        assertFalse(result);
        verify(setOperations, times(1)).remove("presence:online", userId);
    }

    @Test
    void testGetStatus() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("presence:user:" + userId)).thenReturn("away");

        String status = presenceService.getStatus(userId);

        assertEquals("away", status);
    }

    @Test
    void testGetOnlineUsers() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("presence:online")).thenReturn(Set.of("user-1", "user-2"));

        Set<String> users = presenceService.getOnlineUsers();

        assertEquals(2, users.size());
        assertTrue(users.contains("user-1"));
    }

    @Test
    void testGetLastSeen() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("presence:lastseen:" + userId)).thenReturn("2023-10-10T10:00:00");

        String lastSeen = presenceService.getLastSeen(userId);

        assertEquals("2023-10-10T10:00:00", lastSeen);
    }
}
