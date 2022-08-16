package com.moonflying.timekiller.executor.core.messenger;

import com.moonflying.timekiller.executor.core.thread.ClientHandlerBizThreadPool;
import com.moonflying.timekiller.executor.core.executor.ScheduledTaskExecutor;
import com.moonflying.timekiller.proto.ScheduledTaskMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @Author ffei
 * @Date 2022/1/3 15:19
 */
public class EmbeddedClientHandler extends SimpleChannelInboundHandler<ScheduledTaskMessage.TaskMessage> {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedClientHandler.class);

    private final String appName;

    public EmbeddedClientHandler(String appName) {
        this.appName = appName;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        ConcurrentMap<String, ScheduledTaskExecutor> scheduledTaskExecutorRepository = new ConcurrentHashMap<>();
//        scheduledTaskExecutorRepository.put("task0", new ScheduledTaskExecutor(null, "1 2 3", null));
//        scheduledTaskExecutorRepository.put("task1", new ScheduledTaskExecutor(null, "4 5 6", null));
//        scheduledTaskExecutorRepository.put("task2", new ScheduledTaskExecutor(null, "7 8 9", null));

        List<ScheduledTaskMessage.ScheduledTask> scheduledTasks = ScheduledTaskExecutor
                .getAllScheduledTaskRepository()
                .entrySet()
                .stream()
                .map(
                        e -> ScheduledTaskMessage.ScheduledTask.newBuilder()
                                .setAppName(this.appName)
                                .setTaskName(e.getKey())
                                .setCorn(((ScheduledTaskExecutor) e.getValue()).getCorn())
                                .build()
                )
                .collect(Collectors.toList());
        ScheduledTaskMessage.TaskMessage taskMessage = ScheduledTaskMessage.TaskMessage.newBuilder()
                .setDataType(ScheduledTaskMessage.TaskMessage.DataType.RegisterRequest)
                .setRegisterRequest(
                        ScheduledTaskMessage.RegisterScheduledTaskRequest.newBuilder()
                                .addAllScheduledTasks(scheduledTasks)
                                .build()
                )
                .build();
        ctx.writeAndFlush(taskMessage);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ScheduledTaskMessage.TaskMessage msg) throws Exception {
        ScheduledTaskMessage.TaskMessage.DataType dataType = msg.getDataType();
        if (dataType == ScheduledTaskMessage.TaskMessage.DataType.ExecuteScheduledTaskRequest) {
            ScheduledTaskMessage.ExecuteScheduledTaskRequest executeRequest = msg.getExecuteRequest();
            ClientHandlerBizThreadPool.bizThreadPool.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ScheduledTaskExecutor.getScheduledTaskHandler(executeRequest.getTaskName()).execute();
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