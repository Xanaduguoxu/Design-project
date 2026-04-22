package org.example.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@ServerEndpoint(value = "/webSocket/{username}")
public class WebSocketHandler {

    private static int onlineCount = 0;

    private static CopyOnWriteArraySet<WebSocketHandler> webSocketSet = new CopyOnWriteArraySet<>();

    // 新增：使用用户ID作为key来存储WebSocket连接
    private static ConcurrentHashMap<String, WebSocketHandler> userWebSocketMap = new ConcurrentHashMap<>();

    private Session session;

    // 新增：存储当前连接的用户ID
    private String userId;

    /**
     * 功能描述:连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        this.session = session;
        webSocketSet.add(this);
        addOnlineCount();

        this.userId = username;

        assert this.userId != null;
        userWebSocketMap.put(this.userId, this);

        log.info("用户{}连接成功!当前在线人数为{}", this.userId, getOnlineCount());
        try {
            this.sendMessage("连接成功!");
        } catch (IOException e) {
            log.error("websocket IO异常");
        }
    }

    /**
     * 功能描述: 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        webSocketSet.remove(this);
        userWebSocketMap.remove(this.userId);
        subOnlineCount();

        log.info("用户{}关闭链接!当前在线人数为{}", this.userId, getOnlineCount());
    }

    /**
     * 功能描述:收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("客户端{}发送消息:{}", this.userId, message);

        try {
            JSONObject bean = JSONUtil.toBean(message, JSONObject.class);
            if (bean.getStr("receiver") != null && bean.getStr("content") != null) {
                sendToUser(this.userId, bean.getStr("receiver"), message);
            }
        } catch (Exception e) {
            log.error("消息解析错误: {}", e.getMessage());
        }
    }

    /**
     * 功能描述: 错误处理
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误", error);
    }

    /**
     * 功能描述: 发送单条消息
     */
    public void sendMessage(String message) throws IOException {
        if (this.session.isOpen()) {
            this.session.getBasicRemote().sendText(message);
        }
    }

    /**
     * 功能描述: 向指定用户发送消息
     */
    public void sendToUser(String sender, String receiver, String content) {
        WebSocketHandler handler = userWebSocketMap.get(receiver);
        //  发送消息
        if (handler != null && handler.session.isOpen()) {
            try {
                handler.sendMessage(content);
                log.info("消息发送成功");
            } catch (IOException e) {
                log.error("向用户{}发送消息失败: {}", receiver, e.getMessage());
            }
        } else {
            log.warn("用户{}不在线或连接已关闭", receiver);
        }
    }

    /**
     * 功能描述: 获取当前链接人数
     */
    public synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 功能描述: 链接+1
     */
    public synchronized void addOnlineCount() {
        WebSocketHandler.onlineCount++;
    }

    /**
     * 功能描述: 链接-1
     */
    public synchronized void subOnlineCount() {
        WebSocketHandler.onlineCount--;
    }

}