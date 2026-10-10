package com.xhr.medsdata.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问日志：每个请求记录 方法、路径、状态码、耗时、来源 IP。
 * 静态资源（css/js/favicon 等）不记录。
 */
@Component
@Order(1)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("ACCESS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            boolean skip = Boolean.TRUE.equals(request.getAttribute(AccessGateFilter.SKIP_LOG_ATTR))
                    || response.getStatus() == HttpServletResponse.SC_NOT_FOUND;
            String uri = request.getRequestURI();
            if (!skip && !isStatic(uri)) {
                long cost = System.currentTimeMillis() - start;
                String query = request.getQueryString();
                log.info("{} {}{} status={} cost={}ms ip={}",
                        request.getMethod(),
                        uri,
                        query == null ? "" : "?" + query,
                        response.getStatus(),
                        cost,
                        clientIp(request));
            }
        }
    }

    private boolean isStatic(String uri) {
        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.equals("/favicon.ico")
                || uri.equals("/favicon.svg")
                || uri.equals("/sw.js");
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
