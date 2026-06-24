package ru.practicum.featureflags;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// @Service
public class FeatureFlagService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, Map<String, Boolean>> cache = new ConcurrentHashMap<>();
    private static final String FLAG_KEY_PREFIX = "feature:flag:";

    public FeatureFlagService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isEnabled(String feature, String tenant) {
        String key = FLAG_KEY_PREFIX + feature + ":" + tenant;

        Map<String, Boolean> tenantFlags = cache.computeIfAbsent(tenant, k -> new ConcurrentHashMap<>());

        if (tenantFlags.containsKey(feature)) {
            return tenantFlags.get(feature);
        }

        try {
            String value = redisTemplate.opsForValue().get(key);
            boolean enabled = "true".equals(value);
            tenantFlags.put(feature, enabled);
            log.debug("Feature {} for tenant {} is {}", feature, tenant, enabled);
            return enabled;
        } catch (Exception e) {
            log.warn("Failed to get feature flag from Redis: {}", e.getMessage());
            return false;
        }
    }

    public void setFlag(String feature, String tenant, boolean enabled) {
        String key = FLAG_KEY_PREFIX + feature + ":" + tenant;
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(enabled));
            cache.computeIfAbsent(tenant, k -> new ConcurrentHashMap<>()).put(feature, enabled);
            log.info("Feature {} for tenant {} set to {}", feature, tenant, enabled);
        } catch (Exception e) {
            log.error("Failed to set feature flag: {}", e.getMessage());
        }
    }

    public void enableFeature(String feature, String tenant) {
        setFlag(feature, tenant, true);
    }

    public void disableFeature(String feature, String tenant) {
        setFlag(feature, tenant, false);
    }
}