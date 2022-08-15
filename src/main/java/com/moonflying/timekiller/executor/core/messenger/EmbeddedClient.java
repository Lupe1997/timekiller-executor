package com.moonflying.timekiller.executor.core.messenger;

import com.moonflying.timekiller.proto.ScheduledTaskMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author ffei
 * @Date 2022/1/2 16:24
 */
public class EmbeddedClient {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedClient.class);

    private Thread thread;

    public void start(final String dispatcherHost, final int port, String appName) {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                NioEventLoopGroup group = new NioEventLoopGroup();
                try {
                    // start client
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(group)
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.TCP_NODELAY, true)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel channel) throws Exception {
                                    channel.pipeline()
//                                            .addLast(new ProtobufVarint32FrameDecoder())
                                            .addLast(new ProtobufDecoder(ScheduledTaskMessage.TaskMessage.getDefaultInstance()))
                                            .addLast(new EmbeddedClientHandler(appName))
//                                            .addLast(new ProtobufVarint32LengthFieldPrepender())
                                            .addLast(new ProtobufEncoder());
                                }
                            });

                    // bind
                    ChannelFuture future = bootstrap.connect(dispatcherHost, port).sync();

                    logger.info(">>>>>>>>>>> executor server start success, netty client = {}, port = {}", EmbeddedClient.class, port);

                    // wait util stop
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    logger.info(">>>>>>>>>>> executor server stop.");
                } finally {
                    // stop
                    try {
                        group.shutdownGracefully();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

            }

        });
        thread.setDaemon(true);	// daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
        thread.start();
    }

    public void stop() throws Exception {
        // destroy server thread
        if (thread!=null && thread.isAlive()) {
            thread.interrupt();
        }

        logger.info(">>>>>>>>>>> executor server destroy success.");
    }
}
