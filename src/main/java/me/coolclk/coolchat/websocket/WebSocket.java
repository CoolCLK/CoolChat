package me.coolclk.coolchat.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/{id}")
@RestController
public class WebSocket {
    private static ConcurrentHashMap<String, WebSocket> webSocket = new ConcurrentHashMap<>();

    private String id;
    private Session session;
    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) throws Exception {
        this.id = id;
        this.session = session;
        webSocket.put(id, this);
        sendMessageToId(this.id, "前端你好，我是后端，我正在通过WebSocket给你发送消息");
        System.out.println(id + "接入连接");
    }

    @OnClose
    public void onClose() {
        webSocket.remove(this.id);
        System.out.println(this.id + "关闭连接");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println(this.id + "发来消息：" + message);
    }

    @OnError
    public void onError(Throwable error) {

    }

    /**
     * 发送消息给客户端
     *
     * @param message 消息
     * @throws IOException 异常
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 给指定的会话发送消息
     *
     * @param id      会话ID
     * @param message 消息
     * @throws IOException 异常
     */
    public void sendMessageToId(String id, String message) throws IOException {
        webSocket.get(id).sendMessage(message);
    }

    /**
     * 群发消息
     *
     * @param message 消息
     * @throws IOException 异常
     */
    public void sendMessageToAll(String message) throws IOException {
        for (String key : webSocket.keySet()) {
            try {
                webSocket.get(key).sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
