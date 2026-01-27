package com.taskify.taskify.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final HandlerInterceptor deprecationInterceptor;

    public WebConfig(DeprecationInterceptor deprecationInterceptor) {
        this.deprecationInterceptor = deprecationInterceptor;
    }

    @Override
    public void addInterceptors(
            @org.springframework.lang.NonNull org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(deprecationInterceptor);
    }
}
