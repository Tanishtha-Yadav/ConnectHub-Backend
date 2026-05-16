package com.connecthub.presence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class PresenceService {
    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private static final String ONLINE_USERS_KEY = "presence:online";
    private static final String USER_KEY_PREFIX = "presence:user:";
    private static final Duration USER_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getLastSeenKey(String userId) {
        return "presence:lastseen:" + userId;
    }

    public void setOnline(String userId) {
        setStatus(userId, "online");
    }

    public void setStatus(String userId, String status) {
        try {
            if ("offline".equalsIgnoreCase(status) || "invisible".equalsIgnoreCase(status)) {
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
                // Treat offline/invisible as "not present": record a last-seen timestamp
                // so UIs can always display "Last seen ..." even if the client explicitly
                // switches status (vs a WebSocket disconnect).
                redisTemplate.opsForValue().set(getLastSeenKey(userId), java.time.LocalDateTime.now().toString());
            } else {
                redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
            }
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + userId, status.toLowerCase(), USER_TTL);
            log.info("User {} is now {}", userId, status.toUpperCase());
        } catch (Exception e) {
            log.warn("Redis unavailable for presence tracking: {}", e.getMessage());
        }
    }

    public void setOffline(String userId) {
        try {
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
            redisTemplate.delete(USER_KEY_PREFIX + userId);
            redisTemplate.opsForValue().set(getLastSeenKey(userId), java.time.LocalDateTime.now().toString());
            log.info("User {} is now OFFLINE", userId);
        } catch (Exception e) {
            log.warn("Redis unavailable for presence tracking: {}", e.getMessage());
        }
    }

    public boolean isOnline(String userId) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
            if (!Boolean.TRUE.equals(isMember)) return false;

            // Cross-check: verify the TTL-bound status key still exists.
            // If it expired (WebSocket disconnect event was missed / service restart),
            // the set entry is stale — clean it up and report offline.
            String status = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
            if (status == null || "offline".equalsIgnoreCase(status)) {
                log.debug("Stale online-set entry for userId={} — removing and marking offline", userId);
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
                if (status == null) {
                    redisTemplate.opsForValue().set(
                        getLastSeenKey(userId), java.time.LocalDateTime.now().toString());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis unavailable: {}", e.getMessage());
            return false;
        }
    }

    public String getStatus(String userId) {
        try {
            String status = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
            return status != null ? status : "offline";
        } catch (Exception e) {
            log.warn("Redis unavailable: {}", e.getMessage());
            return "offline";
        }
    }

    public Set<String> getOnlineUsers() {
        try {
            return redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        } catch (Exception e) {
            log.warn("Redis unavailable: {}", e.getMessage());
            return Set.of();
        }
    }

    public String getLastSeen(String userId) {
        try {
            return redisTemplate.opsForValue().get(getLastSeenKey(userId));
        } catch (Exception e) {
            log.warn("Redis unavailable: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns last-seen if present; if absent, stores and returns current timestamp.
     * This guarantees UI can render a value for offline users even when a disconnect
     * event was missed (browser crash/network drop).
     */
    public String getOrCreateLastSeen(String userId) {
        try {
            String key = getLastSeenKey(userId);
            String lastSeen = redisTemplate.opsForValue().get(key);
            if (lastSeen == null) {
                lastSeen = java.time.LocalDateTime.now().toString();
                redisTemplate.opsForValue().set(key, lastSeen);
            }
            return lastSeen;
        } catch (Exception e) {
            log.warn("Redis unavailable: {}", e.getMessage());
            return java.time.LocalDateTime.now().toString();
        }
    }
}
