package ru.practicum.feature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FeatureFlagService {

    @Value("${feature.recommendations.enabled:true}")
    private boolean recommendationsEnabled;

    @Value("${feature.new-ui.enabled:false}")
    private boolean newUiEnabled;

    @Value("${feature.comments-moderation.enabled:true}")
    private boolean commentsModerationEnabled;

    @Value("${feature.analytics.enabled:true}")
    private boolean analyticsEnabled;

    private final Map<String, Boolean> overrides = new ConcurrentHashMap<>();

    public boolean isRecommendationsEnabled() {
        return overrides.getOrDefault("recommendations", recommendationsEnabled);
    }

    public boolean isNewUiEnabled() {
        return overrides.getOrDefault("new-ui", newUiEnabled);
    }

    public boolean isCommentsModerationEnabled() {
        return overrides.getOrDefault("comments-moderation", commentsModerationEnabled);
    }

    public boolean isAnalyticsEnabled() {
        return overrides.getOrDefault("analytics", analyticsEnabled);
    }

    public void setFeatureFlag(String feature, boolean enabled) {
        overrides.put(feature, enabled);
        log.info("Feature flag {} set to {}", feature, enabled);
    }

    public void clearFeatureFlag(String feature) {
        overrides.remove(feature);
        log.info("Feature flag {} cleared", feature);
    }

    public boolean isFeatureEnabled(String feature) {
        switch (feature) {
            case "recommendations":
                return isRecommendationsEnabled();
            case "new-ui":
                return isNewUiEnabled();
            case "comments-moderation":
                return isCommentsModerationEnabled();
            case "analytics":
                return isAnalyticsEnabled();
            default:
                log.warn("Unknown feature flag: {}", feature);
                return false;
        }
    }
}