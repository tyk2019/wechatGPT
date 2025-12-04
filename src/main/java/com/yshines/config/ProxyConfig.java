package com.yshines.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代理配置
 *
 * @author ashinnotfound
 * @date 2023/03/04
 */
@Data
@Component
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {
    private String host;
    private String port;
    
    /**
     * 检查是否配置了代理
     */
    public boolean isProxyConfigured() {
        return host != null && !host.trim().isEmpty() && port != null && !port.trim().isEmpty();
    }
}
