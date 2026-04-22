package org.example.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.example.config.security.LoginUser;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.handler.LlmHandler;
import org.example.ai.mapper.ChatMapper;
import org.example.ai.mapper.SessionsMapper;
import org.example.pojo.base.Base;
import org.example.pojo.llm.ChatCompletionChunk;
import org.example.ai.po.Chat;
import org.example.ai.po.Sessions;
import org.example.pojo.base.PageVo;
import org.example.utils.ReqUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    @Resource
    private ChatMapper chatMapper;

    @Resource
    private SessionsMapper sessionsMapper;

    @Resource
    private AsyncChatService asyncChatService;

    @Resource
    private LlmHandler llmHandler;

    @Value("${llm_sk}")
    private String sk;

    private static final String CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    public SseEmitter chat(String question, String sessionId, LoginUser loginUser) {
        SseEmitter emitter = new SseEmitter(5 * 600 * 1000L);

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(50, TimeUnit.SECONDS).readTimeout(10, TimeUnit.MINUTES).build();

        EventSource.Factory factory = EventSources.createFactory(client);

        List<Map<String, Object>> messages = new ArrayList<>(Collections.singletonList(llmHandler.assembleSystem("你是一个优秀的AI助手")));
        messages.add(llmHandler.assembleUser(question));

        Map<String, Object> param = new HashMap<>();
        param.put("model", "qwen-plus");
        param.put("messages", messages);
        param.put("stream", true);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("include_usage", true);
        param.put("stream_options", parameters);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSONUtil.toJsonStr(param));
        Request request = new Request.Builder().url(CHAT_URL).post(body).addHeader("Authorization", sk).addHeader("Content-Type", "application/json").build();

        StringBuffer sb = new StringBuffer();
        Map<String, String> answer = new HashMap<>();
        EventSourceListener eventSourceListener = new EventSourceListener() {

            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                super.onOpen(eventSource, response);
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                super.onEvent(eventSource, id, type, data);
                try {
                    if (data.equals("[DONE]")) {
                        log.info("输出内容终止");
                    } else {
                        ChatCompletionChunk bean = JSONUtil.toBean(data, ChatCompletionChunk.class);
                        if (bean.getChoices() != null && !bean.getChoices().isEmpty()) {
                            if (StrUtil.isNotBlank(bean.getChoices().get(0).getDelta().getContent())) {
                                sb.append(bean.getChoices().get(0).getDelta().getContent());
                                answer.put("content", bean.getChoices().get(0).getDelta().getContent());
                                answer.put("sessionId", sessionId);
                                emitter.send(Result.success(answer));
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                super.onClosed(eventSource);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                try {
                    Chat user = new Chat("user", question, sessionId);
                    chatMapper.insert(user);
                    Thread.sleep(200);
                    Chat assistant = new Chat("assistant", sb.toString(), sessionId);
                    chatMapper.insert(assistant);

                    //  异步存储主题
                    asyncChatService.processChatHistoryAsync(question, sessionId, sb.toString(), loginUser);

                } catch (InterruptedException e) {
                    log.info("对话插入数据失败:{}", e.getMessage());
                }

                emitter.complete();
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                super.onFailure(eventSource, t, response);
                assert t != null;
                log.error("SEE异常关闭连接{},", t.getMessage());
                emitter.completeWithError(t);
            }
        };

        // 创建事件
        factory.newEventSource(request, eventSourceListener);
        return emitter;
    }

    public PageVo sessionsHistory(JSONObject req) {
        Page<Sessions> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));

        String role = SecurityUtils.getRole();
        LambdaQueryWrapper<Sessions> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(!"root".equals(role), Base::getCreateBy, SecurityUtils.getUsername());
        queryWrapper.like(StrUtil.isNotBlank(req.getStr("keyword")), Sessions::getTopic, req.getStr("keyword"));
        queryWrapper.orderByDesc(Sessions::getCreateTime);

        sessionsMapper.selectPage(page, queryWrapper);

        List<JSONObject> records = page.getRecords().stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("id", item.getId().toString());
            jsonObject.putOpt("sessionId", item.getSessionId());
            jsonObject.putOpt("topic", item.getTopic());
            Long record = chatMapper.selectCount(new LambdaQueryWrapper<Chat>().eq(Chat::getSessionId, item.getSessionId()));
            jsonObject.putOpt("count", record.intValue() / 2);
            return jsonObject;
        }).collect(Collectors.toList());

        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(records);
        return answer;
    }

    public List<JSONObject> sessionsDetails(String sessionId) {
        List<Chat> chats = chatMapper.selectList(new LambdaQueryWrapper<Chat>().eq(Chat::getSessionId, sessionId));
        return chats.stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("role", item.getRole());
            jsonObject.putOpt("content", item.getContent());
            jsonObject.putOpt("createTime", item.getCreateTime());
            return jsonObject;
        }).collect(Collectors.toList());
    }

    public Boolean sessionsDel(String sessionId) {
        sessionsMapper.delete(new LambdaQueryWrapper<Sessions>().eq(Sessions::getSessionId, sessionId));
        chatMapper.delete(new LambdaQueryWrapper<Chat>().eq(Chat::getSessionId, sessionId));
        return true;
    }
}
