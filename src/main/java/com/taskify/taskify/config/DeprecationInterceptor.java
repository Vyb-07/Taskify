package com.taskify.taskify.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DeprecationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DeprecationInterceptor.class);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            ApiDeprecated annotation = handlerMethod.getMethodAnnotation(ApiDeprecated.class);
            if (annotation != null) {
                // 1. Add Deprecation Headers
                response.addHeader("Deprecation", "true");
                response.addHeader("Sunset", annotation.sunsetDate());
                response.addHeader("Link", String.format("<%s>; rel=\"successor-version\"", annotation.successorUrl()));

                // 2. Log Warning
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";

                log.warn("Access to deprecated endpoint: method={}, path={}, user={}",
                        request.getMethod(), request.getRequestURI(), username);
            }
        }
        return true;
    }
}
