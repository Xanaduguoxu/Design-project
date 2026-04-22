package org.example.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.config.security.LoginUser;
import org.example.handler.LlmHandler;
import org.example.ai.mapper.ChatMapper;
import org.example.ai.mapper.SessionsMapper;
import org.example.ai.po.Sessions;
import org.example.ai.po.Chat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AsyncChatService {

    @Resource
    private LlmHandler llmHandler;

    @Resource
    private SessionsMapper sessionsMapper;

    @Resource
    private ChatMapper chatMapper;

    private static final String PROMPT = "你非常善于根据问题总结主题，请根据用户的提问总结主题，不要废话直接输出提问主题";

    @Async
    public void processChatHistoryAsync(String question, String sessionId, String answer, LoginUser loginUser) {
        CompletableFuture.runAsync(() -> {

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                List<Chat> chats = chatMapper.selectList(new LambdaQueryWrapper<Chat>().eq(Chat::getSessionId, sessionId));
                if (chats.size() == 2) {
                    List<Map<String, Object>> messages = new ArrayList<>(Collections.singletonList(llmHandler.assembleSystem(PROMPT)));
                    messages.add(llmHandler.assembleUser(question));
                    String topic = llmHandler.chat(messages);
                    Sessions sessions = new Sessions(sessionId, topic, answer);
                    sessionsMapper.insert(sessions);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.info("error:{}", e.getMessage());
            }
        });
    }
}
