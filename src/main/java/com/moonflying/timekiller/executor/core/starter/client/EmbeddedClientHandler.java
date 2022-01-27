package com.moonflying.timekiller.executor.core.starter.client;

import com.moonflying.timekiller.executor.core.thread.ClientHandlerBizThreadPool;
import com.moonflying.timekiller.executor.core.executor.AbstractScheduledTaskExecutor;
import com.moonflying.timekiller.executor.core.executor.impl.ScheduledTaskExecutor;
import com.moonflying.timekiller.proto.ScheduledTaskProtoBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author ffei
 * @Date 2022/1/3 15:19
 */
public class EmbeddedClientHandler extends SimpleChannelInboundHandler<ScheduledTaskProtoBuf.TaskMessage> {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedClientHandler.class);

    private final String appName;

    public EmbeddedClientHandler(String appName) {
        this.appName = appName;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        List<ScheduledTaskProtoBuf.ScheduledTask> scheduledTasks = AbstractScheduledTaskExecutor
                .getAllScheduledTaskRepository()
                .entrySet()
                .stream()
                .map(
                        e -> ScheduledTaskProtoBuf.ScheduledTask.newBuilder()
                                .setAppName(this.appName)
                                .setTaskName(e.getKey())
                                .setCorn(((ScheduledTaskExecutor) e.getValue()).getCorn())
                                .build()
                )
                .collect(Collectors.toList());
        ScheduledTaskProtoBuf.TaskMessage taskMessage = ScheduledTaskProtoBuf.TaskMessage.newBuilder()
                .setDataType(ScheduledTaskProtoBuf.TaskMessage.DataType.RegisterRequest)
                .setRegisterRequest(
                        ScheduledTaskProtoBuf.RegisterScheduledTaskRequest.newBuilder()
                                .addAllScheduledTasks(scheduledTasks)
                                .build()
                )
                .build();
        ctx.writeAndFlush(taskMessage);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ScheduledTaskProtoBuf.TaskMessage msg) throws Exception {
        ScheduledTaskProtoBuf.TaskMessage.DataType dataType = msg.getDataType();
        if (dataType == ScheduledTaskProtoBuf.TaskMessage.DataType.ExecuteScheduledTaskRequest) {
            ScheduledTaskProtoBuf.ExecuteScheduledTaskRequest executeRequest = msg.getExecuteRequest();
            ClientHandlerBizThreadPool.bizThreadPool.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AbstractScheduledTaskExecutor.getScheduledTaskHandler(executeRequest.getTaskName()).execute();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(">>>>>>>>>>> timekiller executor caught exception", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();      // beat 3N, close if idle
            logger.debug(">>>>>>>>>>> xxl-job provider netty_http server close an idle channel.");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}