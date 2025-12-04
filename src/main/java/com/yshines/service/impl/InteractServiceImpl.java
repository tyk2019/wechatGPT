package com.yshines.service.impl;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.yshines.bo.ChatBO;
import com.yshines.exception.ChatException;
import com.yshines.service.InteractService;
import com.yshines.util.BotUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 交互服务impl
 *
 * @author ashinnotfound
 * @date 2022/12/10
 */
@Service
@Slf4j
public class InteractServiceImpl implements InteractService {
    @Override
    public String chat(ChatBO chatBO) throws ChatException {
        // 验证输入参数
        if (chatBO == null || chatBO.getPrompt() == null || chatBO.getPrompt().trim().isEmpty()) {
            log.warn("聊天输入参数无效: {}", chatBO);
            throw new ChatException("请输入有效的问题");
        }

        List<ChatMessage> prompt = BotUtil.buildPrompt(chatBO.getSessionId(), chatBO.getPrompt());

        //向gpt提问
        OpenAiService openAiService = BotUtil.getOpenAiService();
        ChatCompletionRequest.ChatCompletionRequestBuilder completionRequestBuilder = BotUtil.getCompletionRequestBuilder();

        ChatCompletionRequest completionRequest = completionRequestBuilder.messages(prompt).build();
        ChatMessage answer = null;
        try {
            answer = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        }catch (OpenAiHttpException e){
            log.error("向GPT提问失败，提问内容：{}，会话ID：{}，\n原因：{}\n", chatBO.getPrompt(), chatBO.getSessionId(), e.getMessage(), e);
            if (429 == e.statusCode){
                throw new ChatException("提问过于频繁(OpenAI限制接口为20次/分钟)，请稍后再试");
            }else if(400 == e.statusCode || 401 == e.statusCode || 422 == e.statusCode){
                log.warn("API错误响应，状态码：{}，尝试删除较前会话记录并重新提问", e.statusCode);
                //http400/401/422错误，大概率是历史会话太多导致token超出限制或API密钥问题
                if (BotUtil.deleteFirstPrompt(chatBO.getSessionId())){
                    log.info("删除历史会话记录成功，重新提问");
                    return chat(chatBO);
                } else {
                    log.warn("删除历史会话记录失败");
                    throw new ChatException("提问太长或API配置有误，请检查API密钥或缩短问题长度");
                }
            } else {
                throw new ChatException("GPT服务暂时不可用，请稍后再试: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("调用GPT服务时发生未知错误，提问内容：{}，会话ID：{}", chatBO.getPrompt(), chatBO.getSessionId(), e);
            throw new ChatException("GPT服务暂时不可用，请稍后再试");
        }
        
        if (answer == null || answer.getContent() == null) {
            log.warn("GPT返回结果为空，提问内容：{}，会话ID：{}", chatBO.getPrompt(), chatBO.getSessionId());
            throw new ChatException("GPT暂时无法回答，请稍后再试");
        }

        // 更新会话历史
        prompt.add(answer);
        BotUtil.updatePrompt(chatBO.getSessionId(), prompt);

        String response = answer.getContent().trim();
        log.debug("GPT响应成功，会话ID：{}，响应内容：{}", chatBO.getSessionId(), response);
        return response;
    }
}
