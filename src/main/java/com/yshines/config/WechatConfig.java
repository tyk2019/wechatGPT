package com.yshines.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置
 *
 * @author ashinnotfound
 * @date 2023/03/19
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {
    private String qrPath = "./"; // 默认二维码保存路径
    
    // 默认会话重置命令
    private String resetCommand = "重置会话";
}
