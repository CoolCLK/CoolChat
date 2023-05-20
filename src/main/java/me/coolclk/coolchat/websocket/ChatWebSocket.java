package me.coolclk.coolchat.websocket;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import me.coolclk.coolchat.AccountController;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.coolclk.coolchat.Application.LOGGER;

@ServerEndpoint("/ws/chat/{id}")
@RestController
public class ChatWebSocket {
    private static ConcurrentHashMap<String, ChatWebSocket> webSocket = new ConcurrentHashMap<>();
    private String id;
    private Session session;
    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) {
        this.id = id;
        this.session = session;
        webSocket.put(id, this);
        LOGGER.info("[Websocket管理器] " + id + " 接入连接");
        try {
            this.sendMessage("{\"type\":\"chatMessage\",\"id\":\"public\",\"message\":{\"type\":\"prompt\",\"content\":\"欢迎你! \"}}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnClose
    public void onClose() {
        webSocket.remove(this.id);
        LOGGER.info("[Websocket管理器] " + this.id + " 断开连接");
    }

    @OnMessage
    public void onMessage(String message) {
        Map<String, Object> structMessage = new Gson().fromJson(message, new TypeToken<Map<String, Object>>(){}.getType());
        Map<String, Object> objectMessage = (Map<String, Object>) structMessage.get("message");
        objectMessage.put("account", this.id);
        objectMessage.put("nickname", AccountController.getAccountNickname(this.id));
        structMessage.put("message", objectMessage);
        String formattedMessage = new Gson().toJson(structMessage);
        webSocket.keys().asIterator().forEachRemaining(id -> {
            try {
                webSocket.get(id).sendMessage(formattedMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnError
    public void onError(Throwable error) {

    }

    /**
     * 向客户端发送消息
     * @param message 内容
     * @author CoolCLK
     * @exception IOException 在发送数据时发生错误
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}
