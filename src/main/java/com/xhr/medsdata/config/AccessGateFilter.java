package com.xhr.medsdata.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 单入口门禁：访问入口路径（APP_GATE_ENTRY）后写入会话 Cookie，之后携带该 Cookie 才放行；
 * 其余所有请求统一返回 404 空响应且不记日志。未配置入口时门禁关闭。
 * 顺序在 AccessLogFilter 之前。
 */
@Component
@Order(0)
public class AccessGateFilter extends OncePerRequestFilter {

    /** 请求属性：置为 true 时 AccessLogFilter 不记录该请求。 */
    public static final String SKIP_LOG_ATTR = "skipAccessLog";

    @Value("${app.gate.entry:}")
    private String gateEntry;

    @Value("${app.gate.cookie-name:meds_gate}")
    private String cookieName;

    /** 空闲有效期（分钟）：每次通过门禁的请求都会续期，超过该时长无操作即失效。 */
    @Value("${app.gate.session-minutes:10}")
    private long sessionMinutes;

    private static final Logger log = LoggerFactory.getLogger(AccessGateFilter.class);

    @PostConstruct
    public void logState() {
        String entry = gateEntry == null ? "" : gateEntry.trim();
        if (entry.isEmpty()) {
            log.info("门禁未启用：APP_GATE_ENTRY 为空，所有请求放行");
        } else {
            log.info("门禁已启用：入口路径 {}，空闲有效期 {} 分钟", normalize(entry), sessionMinutes);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entry = gateEntry == null ? "" : gateEntry.trim();
        if (entry.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String entryPath = normalize(entry);
        String expected = tokenOf(entry);

        if ("GET".equalsIgnoreCase(request.getMethod()) && entryPath.equals(request.getRequestURI())) {
            request.setAttribute(SKIP_LOG_ATTR, Boolean.TRUE);
            issueCookie(request, response, expected);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/");
            return;
        }

        if (hasValidToken(request, expected)) {
            issueCookie(request, response, expected);
            chain.doFilter(request, response);
            return;
        }

        request.setAttribute(SKIP_LOG_ATTR, Boolean.TRUE);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    /** 写入/续期会话 Cookie：Max-Age = 配置的分钟数，每次请求都会刷新。 */
    private void issueCookie(HttpServletRequest request, HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(cookieName, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) Math.max(1, sessionMinutes) * 60);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private boolean hasValidToken(HttpServletRequest request, String expected) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && constantTimeEquals(expected, cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String entry) {
        String e = entry.trim();
        while (e.startsWith("/")) {
            e = e.substring(1);
        }
        while (e.endsWith("/")) {
            e = e.substring(0, e.length() - 1);
        }
        return "/" + e;
    }

    private String tokenOf(String entry) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(entry.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法计算门禁 token", e);
        }
    }
}
