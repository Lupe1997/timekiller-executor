package com.moonflying.timekiller.executor.core.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moonflying.timekiller.executor.core.messenger.EmbeddedClient;

/**
 * @Author ffei
 * @Date 2021/11/21 22:49
 */
public abstract class AbstractExecutorStarter {
    private static final Logger logger = LoggerFactory.getLogger(AbstractExecutorStarter.class);

    private String dispatcherHost;
    private int dispatcherPort;
    private String appName;

    public void setDispatcherHost(String dispatcherHost) {
        this.dispatcherHost = dispatcherHost;
    }

    public void setDispatcherPort(int dispatcherPort) {
        this.dispatcherPort = dispatcherPort;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void start() throws Exception {
        initEmbeddedClientServer(dispatcherHost, dispatcherPort, appName);
    }

    public void destroy() throws Exception{
        // stop executor server
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    // ---------------------- executor-server (rpc customer) ----------------------
    private EmbeddedClient client = null;

    private void initEmbeddedClientServer(String dispatcherHost, int port, String appName) throws Exception {
        // generate address
        if (dispatcherHost==null || dispatcherHost.trim().length()==0) {
            throw new RuntimeException("Server address is null!!!");
        }

        // start
        client = new EmbeddedClient();
        client.start(dispatcherHost, port, appName);
    }
}
