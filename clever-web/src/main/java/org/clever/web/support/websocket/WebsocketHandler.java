package org.clever.web.support.websocket;

import io.javalin.websocket.*;
import org.clever.core.Ordered;

/**
 * Websocket 请求处理
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/20 20:19 <br/>
 */
public interface WebsocketHandler extends Ordered {
    /**
     * 当 WebSocket 客户端连接时调用处理程序。
     */
    default void onConnect(WsConnectHandler wsConnectHandler) {
    }

    /**
     * 当 WebSocket 客户端发送 String 消息时调用处理程序。
     */
    default void onMessage(WsMessageHandler wsMessageHandler) {
    }

    /**
     * 当 WebSocket 客户端发送二进制消息时调用处理程序
     */
    default void onBinaryMessage(WsBinaryMessageHandler wsBinaryMessageHandler) {
    }

    /**
     * 当 WebSocket 客户端关闭连接时调用处理程序。
     * 处理程序不会在网络问题的情况下被调用，只有当客户端主动关闭连接（或超时）时才会调用。
     */
    default void onClose(WsCloseHandler wsCloseHandler) {
    }

    /**
     * 当检测到错误时调用处理程序
     */
    default void onError(WsErrorHandler wsErrorHandler) {
    }

    @Override
    default double getOrder() {
        return 0;
    }
}
