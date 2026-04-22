package org.example.ai;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import org.example.ai.service.ChatService;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.handler.AlgorithmHandler;
import org.example.handler.PromptHandler;
import org.example.mapper.algorithm.BehaviorMapper;
import org.example.pojo.base.PageVo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private AlgorithmHandler algorithmHandler;

    @Resource
    private BehaviorMapper behaviorMapper;

    @Resource
    private PromptHandler promptHandler;

    @PostMapping("/chats")
    public SseEmitter chat(@RequestBody JSONObject req) throws Exception {
        String sessionId = req.getStr("sessionId");
        sessionId = StrUtil.isNotBlank(sessionId) ? sessionId : UUID.randomUUID().toString().replace("-", "");

        //  判断出未登陆
        if (StrUtil.isBlank(SecurityUtils.getUsername())) {
            SseEmitter emitter = new SseEmitter();
            Map<String, Object> answer = new HashMap<>();
            answer.put("content", "暂未登陆，请登陆后再次提问。");
            answer.put("sessionId", sessionId);
            emitter.send(Result.success(answer));
            emitter.complete();
            return emitter;
        }

        //  TODO 直接将行为数据嵌入Prompt
        return chatService.chat(req.getStr("question"), sessionId, SecurityUtils.getLoginUser());
    }

    @PostMapping("/history")
    public Result<PageVo> sessionsHistory(@RequestBody JSONObject req) {
        return Result.success(chatService.sessionsHistory(req));
    }

    @GetMapping("/history/detail")
    public Result<List<JSONObject>> sessionsDetails(@RequestParam("sessionId") String sessionId) {
        return Result.success(chatService.sessionsDetails(sessionId));
    }

    @DeleteMapping("/history/del")
    public Result<Boolean> sessionsDel(@RequestParam("sessionId") String sessionId) {
        return Result.success(chatService.sessionsDel(sessionId));
    }
}
