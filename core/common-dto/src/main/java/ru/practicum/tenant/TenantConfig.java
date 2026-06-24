package ru.practicum.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (TenantContext.isMultiTenantEnabled()) {
            registry.addInterceptor(tenantInterceptor());
        }
    }

    @Bean
    public TenantInterceptor tenantInterceptor() {
        return new TenantInterceptor();
    }
}