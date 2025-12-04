package com.yshines.util;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.yshines.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
//chatbot工具类


@Component
@Slf4j
/**
 *
 * 功能描述:
 *
 * @param:
 * @return:
 * @auther: Y-Shines
 * @date: 2023/4/1 16:00
 */
public class BotUtil {
    @Resource
    public void setAccountConfig(BotConfig botConfig) {
        BotUtil.botConfig = botConfig;
    }

    private static BotConfig botConfig;

    // 使用线程安全的ConcurrentHashMap存储会话历史
    private static final Map<String, List<ChatMessage>> PROMPT_MAP = new ConcurrentHashMap<>();
    // 使用线程安全的ConcurrentHashMap跟踪API服务使用次数
    private static final Map<OpenAiService, Integer> COUNT_FOR_OPEN_AI_SERVICE = new ConcurrentHashMap<>();
    private static ChatCompletionRequest.ChatCompletionRequestBuilder completionRequestBuilder;

    @PostConstruct
    public void init() {
        completionRequestBuilder = ChatCompletionRequest.builder()
                .model(botConfig.getModel())
                .temperature(botConfig.getTemperature())
                .maxTokens(botConfig.getMaxToken());
        
        for (OpenAiService openAiService : botConfig.getOpenAiServiceList()) {
            COUNT_FOR_OPEN_AI_SERVICE.put(openAiService, 0);
        }
        
        log.info("BotUtil初始化完成，共配置了{}个API服务", botConfig.getOpenAiServiceList().size());
    }


    public static OpenAiService getOpenAiService() {
        // 获取使用次数最小的openAiService 否则获取map中的第一个
        Optional<Map.Entry<OpenAiService, Integer>> minEntry = COUNT_FOR_OPEN_AI_SERVICE.entrySet().stream()
                .min(Map.Entry.comparingByValue());
        
        if (minEntry.isPresent()) {
            OpenAiService service = minEntry.get().getKey();
            COUNT_FOR_OPEN_AI_SERVICE.compute(service, (key, value) -> (value == null) ? 1 : value + 1);
            log.debug("选择API服务，当前使用次数: {}", COUNT_FOR_OPEN_AI_SERVICE.get(service));
            return service;
        } else {
            // 如果没有找到最小值，返回第一个可用的服务
            OpenAiService firstService = COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next();
            COUNT_FOR_OPEN_AI_SERVICE.compute(firstService, (key, value) -> (value == null) ? 1 : value + 1);
            log.warn("未找到使用次数最小的服务，使用第一个服务，当前使用次数: {}", COUNT_FOR_OPEN_AI_SERVICE.get(firstService));
            return firstService;
        }
    }

    public static ChatCompletionRequest.ChatCompletionRequestBuilder getCompletionRequestBuilder() {
        return completionRequestBuilder;
    }

    public static List<ChatMessage> buildPrompt(String sessionId, String newPrompt) {
        if (!PROMPT_MAP.containsKey(sessionId)) {
            if (null != botConfig.getBasicPrompt()) {
                List<ChatMessage> promptList = new ArrayList<>();
                promptList.add(botConfig.getBasicPrompt());
                PROMPT_MAP.put(sessionId, promptList);
            }
        }
        List<ChatMessage> promptList = PROMPT_MAP.getOrDefault(sessionId, new ArrayList<>());
        promptList.add(new ChatMessage("user", newPrompt));
        return promptList;
    }

    public static void updatePrompt(String sessionId, List<ChatMessage> promptList) {
        if (promptList != null && !promptList.isEmpty()) {
            PROMPT_MAP.put(sessionId, promptList);
        } else {
            log.warn("尝试更新空的会话历史，会话ID: {}", sessionId);
        }
    }

    public static boolean isPromptEmpty(String sessionId) {
        if (!PROMPT_MAP.containsKey(sessionId)) {
            return true;
        }
        List<ChatMessage> promptList = PROMPT_MAP.get(sessionId);
        if (null != botConfig.getBasicPrompt()) {
            return promptList.size() <= 1; // 基本提示也算作一个消息
        } else {
            return promptList.size() == 0;
        }
    }

    public static boolean deleteFirstPrompt(String sessionId) {
        if (!isPromptEmpty(sessionId)) {
            int index = null != botConfig.getBasicPrompt() ? 1 : 0;
            List<ChatMessage> promptList = PROMPT_MAP.get(sessionId);
            //问
            promptList.remove(index);
            //答
            promptList.remove(index);
            updatePrompt(sessionId, promptList);
            return true;
        }
        return false;
    }

    //清空token
    public static void resetPrompt(String sessionId) {
        PROMPT_MAP.remove(sessionId);
    }

}
