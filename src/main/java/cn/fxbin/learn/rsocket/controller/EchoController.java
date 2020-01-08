package cn.fxbin.learn.rsocket.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * EchoController
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/1/8 20:26
 */
@Controller
public class EchoController {

    /**
     * 请求-响应模式
     */
    @MessageMapping("echo")
    public Mono<String> echo(String input) {
        return Mono.just("ECHO >> " + input);
    }

    @MessageMapping("stringSplit")
    public Flux<String> stringSplit(String input) {
        return Flux.fromStream(input.codePoints().mapToObj(msg -> String.valueOf((char) msg)));
    }

}
