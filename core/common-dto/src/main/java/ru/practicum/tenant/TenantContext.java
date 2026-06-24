package ru.practicum.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenant) {
        log.debug("Setting tenant: {}", tenant);
        CURRENT_TENANT.set(tenant);
    }

    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        return tenant != null ? tenant : "public";
    }

    public static void clear() {
        log.debug("Clearing tenant context");
        CURRENT_TENANT.remove();
    }

    public static boolean isMultiTenantEnabled() {
        return "true".equals(System.getProperty("MULTI_TENANT_ENABLED", "false"));
    }
}