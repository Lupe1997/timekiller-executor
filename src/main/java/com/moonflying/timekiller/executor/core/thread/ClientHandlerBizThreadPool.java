package com.moonflying.timekiller.executor.core.thread;

import java.util.concurrent.*;

/**
 * @Author ffei
 * @Date 2022/1/3 16:08
 */
public class ClientHandlerBizThreadPool {
    public static ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
            0,
            200,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(2000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "timekiller-executor, EmbeddedClient bizThreadPool-" + r.hashCode());
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    throw new RuntimeException("timekiller-executor, EmbeddedClient bizThreadPool is EXHAUSTED!");
                }
            });
}
