package com.moonflying.timekiller.executor;

import com.moonflying.timekiller.executor.core.messenger.EmbeddedClient;

/**
 * @Author ffei
 * @Date 2021/11/20 22:38
 */
public class Test {
    public static void main(String[] args) {
        new EmbeddedClient().start("127.0.0.1", 6668, "local");
    }
}