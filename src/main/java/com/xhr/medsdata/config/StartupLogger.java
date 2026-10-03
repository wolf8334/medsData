package com.xhr.medsdata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时把监听端口、上下文路径、访问地址写入 app.log，方便排查部署问题。
 */
@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment environment;
    private volatile int port = -1;

    public StartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener
    public void onWebServerReady(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
        log.info("Tomcat 已启动，监听端口: {}", port);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String ctx = environment.getProperty("server.servlet.context-path", "");
        if (ctx == null) {
            ctx = "";
        }
        String profiles = String.join(",", environment.getActiveProfiles());
        log.info("应用启动完成: 访问地址={}, 端口={}, 上下文路径='{}', profiles={}",
                "http://localhost:" + port + ctx + "/", port, ctx,
                profiles.isBlank() ? "default" : profiles);
    }
}
