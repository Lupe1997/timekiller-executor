package timekiller.executor.core.starter.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import timekiller.executor.core.Thread.ClientHandlerBizThreadPool;

/**
 * @Author ffei
 * @Date 2022/1/3 15:19
 */
public class EmbeddedClientHandler extends SimpleChannelInboundHandler {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedClientHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        super.channelActive(ctx);

    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, Object msg) throws Exception {
        // TODO 使用probuf

        // request parse
        //final byte[] requestBytes = ByteBufUtil.getBytes(msg.content());    // byteBuf.toString(io.netty.util.CharsetUtil.UTF_8);
        String requestData = msg.content().toString(CharsetUtil.UTF_8);
        String uri = msg.uri();
        boolean keepAlive = HttpUtil.isKeepAlive(msg);
        String accessTokenReq = msg.headers().get(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);

        // invoke
        ClientHandlerBizThreadPool.bizThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                // do invoke
                Object responseObj = process(httpMethod, uri, requestData, accessTokenReq);

                // to json
                String responseJson = GsonTool.toJson(responseObj);

                // write response
                writeResponse(ctx, keepAlive, responseJson);
            }
        });
    }

    private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq) {

        // valid
        if (HttpMethod.POST != httpMethod) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri==null || uri.trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        if (accessToken!=null
                && accessToken.trim().length()>0
                && !accessToken.equals(accessTokenReq)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }

        // services mapping
        try {
            if ("/beat".equals(uri)) {
                return executorBiz.beat();
            } else if ("/idleBeat".equals(uri)) {
                IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                return executorBiz.idleBeat(idleBeatParam);
            } else if ("/run".equals(uri)) {
                TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                return executorBiz.run(triggerParam);
            } else if ("/kill".equals(uri)) {
                KillParam killParam = GsonTool.fromJson(requestData, KillParam.class);
                return executorBiz.kill(killParam);
            } else if ("/log".equals(uri)) {
                LogParam logParam = GsonTool.fromJson(requestData, LogParam.class);
                return executorBiz.log(logParam);
            } else {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping("+ uri +") not found.");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "request error:" + ThrowableUtil.toString(e));
        }
    }

    /**
     * write response
     */
    private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
        // write response
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));   //  Unpooled.wrappedBuffer(responseJson)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");       // HttpHeaderValues.TEXT_PLAIN.toString()
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response);
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