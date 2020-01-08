package cn.fxbin.learn.demo;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

/**
 * WebSocketRequestResponseExample  WebSocket-请求-响应流模式
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/1/8 20:43
 */
public class WebSocketRequestResponseExample {

    public static void main(String[] args) {
        RSocketFactory.receive()
                .acceptor((setupPayload, sendingSocket) -> Mono.just(
                        new AbstractRSocket() {
                            @Override
                            public Mono<Payload> requestResponse(Payload payload) {
                                return Mono.just(DefaultPayload.create("ECHO >> " + payload.getDataUtf8()));
                            }
                        }
                ))
                // 指定传输层实现
                .transport(WebsocketServerTransport.create("localhost", 7000))
                // 启动
                .start()
                .subscribe();



        RSocket rSocket = RSocketFactory.connect()
                // 指定传输层实现
                .transport(WebsocketClientTransport.create("localhost", 7000))
                // 启动客户端
                .start()
                .block();

        rSocket.requestResponse(DefaultPayload.create("hello"))
                .map(Payload::getDataUtf8)
                .doOnNext(System.out::println)
                .block();

        rSocket.dispose();
    }

}
