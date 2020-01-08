package cn.fxbin.learn;

import io.rsocket.transport.netty.client.TcpClientTransport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Resource;

@SpringBootTest
class SpringBootRSocketDemoApplicationTests extends AbstractTest {

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Test echo server")
    void testEcho() {
        RSocketRequester requester = createRSocketRequester();
        Mono<String> result = requester
                // 指定消息目的地
                .route("echo")
                // 指定负载中数据
                .data("hello")
                // 发送请求
                .retrieveMono(String.class);

        StepVerifier.create(result)
                .expectNext("ECHO >> hello")
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Test string split")
    void testStringSplit() {
        RSocketRequester requester = createRSocketRequester();
        Flux<String> result = requester
                // 指定消息目的地
                .route("stringSplit")
                // 指定负载中数据
                .data("hello")
                // 发送请求
                .retrieveFlux(String.class);

        StepVerifier.create(result)
                .expectNext("h", "e", "l", "l", "o")
                .expectComplete()
                .verify();
    }
}

abstract class AbstractTest {

    @Value("${spring.rsocket.server.port}")
    private int serverPort;

    @Resource
    private RSocketRequester.Builder builder;

    RSocketRequester createRSocketRequester() {

        return builder
                // 指定负载数据的MIME类型
                .dataMimeType(MimeTypeUtils.TEXT_PLAIN)
                // 指定传输层实现
                .connect(TcpClientTransport.create(serverPort))
                .block();
    }
}
