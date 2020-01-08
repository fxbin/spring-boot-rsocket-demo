package cn.fxbin.learn.demo;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

/**
 * FireAndForgetExample 发后不管模式
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/1/8 20:15
 */
public class FireAndForgetExample {

    public static void main(String[] args) {
        RSocketFactory.receive()
                .acceptor((setupPayload, sendingSocket) -> Mono.just(
                        new AbstractRSocket() {
                            @Override
                            public Mono<Void> fireAndForget(Payload payload) {
                                System.out.println("Receive: " + payload.getDataUtf8());
                                return Mono.empty();
                            }
                        }
                ))
                .transport(TcpServerTransport.create("localhost", 7000))
                .start()
                .block();

        RSocket rSocket = RSocketFactory.connect()
                .transport(TcpClientTransport.create("localhost", 7000))
                .start()
                .block();

        rSocket.fireAndForget(DefaultPayload.create("hello")).block();
        rSocket.fireAndForget(DefaultPayload.create("world")).block();

        rSocket.dispose();
    }

}
