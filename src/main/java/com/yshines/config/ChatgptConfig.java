package com.yshines.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * chatgpt配置
 *
 * @author ashinnotfound
 * @date 2023/03/04
 */
@Data
@Component
@ConfigurationProperties(prefix = "chatgpt")
public class ChatgptConfig {
    @NotEmpty(message = "ChatGPT API Key不能为空")
    private List<String> apiKey;
    
    // 默认模型配置
    private String model = "gpt-3.5-turbo";
    
    // 默认最大token数
    private Integer maxToken = 2048;
    
    // 默认温度参数
    private Double temperature = 0.8;
}
