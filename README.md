# SpringBoot RSocket Demo

HTTP: 请求-响应模式
gRPC: 基于HTTP/2,基于 TCP 的协议使用二进制字节流传输，保证了传输的效率
WebSocket: 双向数据传输

## RSocket 介绍

> RSocket 是一个 OSL 七层模型中 5/6 层的协议，是 TCP/IP 之上的应用层协议。RSocket 可以使用不同的底层传输层，包括 TCP、WebSocket 和 Aeron。TCP 适用于分布式系统的各个组件之间交互，WebSocket 适用于浏览器和服务器之间的交互，Aeron 是基于 UDP 协议的传输方式，这就保证了 RSocket 可以适应于不同的场景。使用 RSocket 的应用层实现可以保持不变，只需要根据系统环境、设备能力和性能要求来选择合适的底层传输方式即可。RSocket 作为一个应用层协议，可以很容易在其基础上定义应用自己的协议。此外，RSocket 使用二进制格式，保证了传输的高效，节省带宽。而且，通过基于反应式流语义的流控制，RSocket 保证了消息传输中的双方不会因为请求的压力过大而崩溃。
>
>

## RSocket 交互模式

RSocket 支持四种不同的交互模式, 如下

| 模式 | 说明 |
| - | - |
| 请求-响应（request/response） | 这是最典型也最常见的模式。发送方在发送消息给接收方之后，等待与之对应的响应消息。 |
| 请求-响应流（request/stream） | 发送方的每个请求消息，都对应于接收方的一个消息流作为响应。 |
| 发后不管（fire-and-forget） | 发送方的请求消息没有与之对应的响应。 |
| 通道模式（channel） | 在发送方和接收方之间建立一个双向传输的通道。 |

下面介绍 RSocket 协议的具体内容。

## RSocket 帧

> RSocket 协议在传输时使用帧（frame）来表示单个消息。每个帧中包含的可能是请求内容、响应内容或与协议相关的数据。一个应用消息可能被切分成多个片段（fragment）以包含在一个帧中。根据底层传输协议的不同，一个表示帧长度的字段可能是必须的。由于 TCP 协议没有提供帧支持，所以 RSocket 的帧长度字段是必须的。对于提供了帧支持的传输协议，RSocket 帧只是简单的封装在传输层消息中；对于没有提供帧支持的传输协议，每个 RSocket 帧之前都需要添加一个 24 字节的字段表示帧长度。

## RSocket 帧的内容

> 在每个 RSocket 帧中，最起始的部分是帧头部，包括 31 字节的流标识符，6 字节的帧类型和 10 字节的标志位。RSocket 协议中的流（stream）表示的是一个操作的单元。每个流有自己唯一的标识符。流标识符由发送方生成。值为 0 的流标识符表示与连接相关的操作。客户端的流标识符从 1 开始，每次递增 2；服务器端的流标识符从 2 开始，每次递增 2。在帧头部之后的内容与帧类型相关。

## RSocket 帧的类型

RSocket 中定义了不同类型的帧，如下：

| 帧类型 | 说明 |
| - | - |
| SETUP | 由客户端发送来建立连接，流标识符为 0。 |
| REQUEST_RESPONSE | 请求-响应模式中的请求内容。 |
| REQUEST_FNF | 发后不管模式中的请求内容。 |
| REQUEST_STREAM | 请求-响应流模式中的请求内容。 |
| REQUEST_CHANNEL | 通道模式中的建立通道的请求。发送方只能发一个 REQUEST_CHANNEL 帧。|
| REQUEST_N | 基于反应式流语义的流控制，相当于反应式流中的 request(n)。 |
| PAYLOAD | 表示负载的帧。通过不同的标志位来表示状态。NEXT 标志位表示接收到流中的数据， |
| COMPLETE | 标志位表示流结束。 |
| ERROR | 表示连接层或应用层错误。 |
| CANCEL | 取消当前请求。 |
| KEEPALIVE | 表示连接层或应用层错误。 |
| KEEPALIVE | 启用租约模式。 |
| METADATA_PUSH | 异步元数据推送。 |
| RESUME | 在连接中断后恢复传输。 |
| RESUME_OK | 恢复传输成功。 |
| EXT | 帧类型扩展。 |

RSocket 中的负载分成元数据和数据两种，二者可以使用不同的编码方式。元数据是可选的。帧头部有标志位指示帧中是否包含元数据。某些特定类型的帧可以添加元数据。

## RSocket 帧交互

根据不同的交互模式，发送方和接收方之间有不同的帧交互，下面是几个典型的帧交互示例：

* 在请求-响应模式中，发送方发送 REQUEST_RESPONSE 帧，接收方发送 PAYLOAD 帧并设置 COMPLETE 标志位。
* 在发后不管模式中，发送方发送 REQUEST_FNF 帧。
* 在请求-响应流模式中，发送方发送 REQUEST_STREAM 帧，接收方发送多个 PAYLOAD 帧。设置 COMPLETE 标志位的 PAYLOAD 帧表示流结束。
* 在通道模式中，发送方发送 REQUEST_CHANNEL 帧。发送方和接收方都可以发送 PAYLOAD 帧给对方。设置 COMPLETE 标志位的 PAYLOAD 帧表示其中一方的流结束。

除了发后不管模式之外，其余模式中的接收方都可以通过 ERROR 帧或 CANCEL 帧来结束流。

## 流控制

> RSocket 使用 Reactive Streams 语义来进行流控制（flow control），也就是 request(n)模式。流的发送方通过 REQUEST_N 帧来声明它允许接收方发送的 PAYLOAD 帧的数量。REQUEST_N 帧一旦发出就不能收回，而且所产生的效果是累加的。比如，发送方发送 request(2)和 request(3)帧之后，接收方允许发送 5 个 PAYLOAD 帧。
> 除了基于 Reactive Streams 语义的流程控制之外，RSocket 还可以使用租约模式。租约模式只是限定了在某个时间段内，请求者所能发送的最大请求数量。
>

## Java 实现

> RSocket 提供了不同语言的实现，包括 Java、Kotlin、JavaScript、Go、.NET 和 C++ 等。对 Java 项目来说，只需要添加相应的 Maven 依赖即可。RSocket 的 Java 实现库都在 Maven 分组 io.rsocket 中。其中常用的库包括核心功能库 rsocket-core 和表 3中列出的传输层实现。
> 这里使用的版本是 1.0.0-RC5。

### RSocket 的传输层实现

| Maven实现库名称 | 底层实现 | 支持协议 |
| - | - | - |
| rsocket-transport-netty | Reactor Netty | TCP 和 WebSocket |
| rsocket-transport-akka | Akka | TCP 和 WebSocket |
| rsocket-transport-aeron | Aeron | UDP |

## RSocket 进阶

### 调试

 由于 RSocket 使用二进制协议，所以调试 RSocket 应用消息比 HTTP/1 协议要复杂一些。从 RSocket 帧的二进制内容无法直接得知帧的含义。需要辅助工具来解析二进制格式消息。对 Java 应用来说，只需要把日志记录器 io.rsocket.FrameLogger 设置为 DEBUG 级别，就可以看到每个 RSocket 帧的内容。
 如下给出 REQUEST_RESPONSE 帧的调试信息。除此之外，还可以使用 Wireshark 工具及其 RSocket 插件。

```java
21:06:21.418 [reactor-tcp-nio-2] DEBUG io.rsocket.FrameLogger - receiving -> 
Frame => Stream ID: 1 Type: NEXT_COMPLETE Flags: 0b1100000 Length: 19
Data:
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 45 43 48 4f 20 3e 3e 20 68 65 6c 6c 6f          |ECHO >> hello   |
+--------+-------------------------------------------------+----------------+
ECHO >> hello
```

### 代码零拷贝

在 示例代码 的AbstractRSocket 类的实现中，对于接收的 Payload 对象是直接使用的。这是因为 RSocket 默认对请求的负载进行了拷贝。
这样的做法在实现上虽然简单，但会带来性能上的损失，增加响应时间。为了提高性能，可以通过 ServerRSocketFactory 类或 ClientRSocketFactory 类
的 frameDecoder() 方法来指定 PayloadDecoder 接口的实现。PayloadDecoder.ZERO_COPY 是内置提供的零拷贝实现类。当使用了负载零拷贝之后，
负载的内容不再被拷贝。需要通过 Payload 对象的 release() 方法来手动释放负载对应的内存，否则会造成内存泄漏。如果使用 Spring Boot 提供的 RSocket 支持，
PayloadDecoder.ZERO_COPY 默认已经被启用，并由 Spring 负责相应的内存释放。



## 参考
 
* [RSocket 官网](http://rsocket.io/)
* [SpringBoot features RSocket](https://docs.spring.io/spring-boot/docs/2.2.x/reference/html/spring-boot-features.html#boot-features-rsocket)
