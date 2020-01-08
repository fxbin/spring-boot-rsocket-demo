package cn.fxbin.learn.rsocket.config;

import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import org.springframework.boot.web.embedded.netty.NettyRouteProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * Config
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/1/8 20:45
 */
@EnableWebFlux
@Configuration
public class Config {

    /**
     * RSocket 有自己的二进制协议，在浏览器端的实现需要使用 RSocket 提供的 JavaScript 客户端与服务器端交互。
     * 在 Web 应用中使用 RSocket 提供的 NodeJS 模块 rsocket-websocket-client
     */

    @Bean
    RSocketWebSocketNettyRouteProvider rSocketWebSocketNettyRouteProvider(RSocketMessageHandler messageHandler) {
        return new RSocketWebSocketNettyRouteProvider("/ws", messageHandler.responder());
    }

    static class RSocketWebSocketNettyRouteProvider implements NettyRouteProvider {

        private final String mappingPath;

        private final SocketAcceptor socketAcceptor;

        RSocketWebSocketNettyRouteProvider(String mappingPath, SocketAcceptor  socketAcceptor) {
            this.mappingPath = mappingPath;
            this.socketAcceptor = socketAcceptor;
        }

        @Override
        public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
            ServerTransport.ConnectionAcceptor acceptor = RSocketFactory.receive()
                    .acceptor(this.socketAcceptor)
                    .toConnectionAcceptor();
            return httpServerRoutes.ws(this.mappingPath, WebsocketRouteTransport.newHandler(acceptor));
        }
    }

}
