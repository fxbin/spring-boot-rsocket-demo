package cn.fxbin.learn.demo;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * RequestChannelExample 通道模式
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/1/8 20:19
 */
public class RequestChannelExample {

    public static void main(String[] args) {

        RSocketFactory.receive()
                .acceptor((setupPayload, sendingSocket) -> Mono.just(
                        new AbstractRSocket() {
                            @Override
                            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                                return Flux.from(payloads).flatMap(payload ->
                                        Flux.fromStream(
                                                payload.getDataUtf8().codePoints().mapToObj(msg -> String.valueOf((char) msg))
                                                .map(DefaultPayload::create)));
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

        rSocket.requestChannel(Flux.just("hello", "world", "goodbye").map(DefaultPayload::create))
                .map(Payload::getDataUtf8)
                .doOnNext(System.out::println)
                .blockLast();

        rSocket.dispose();
    }
}
