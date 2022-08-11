package com.moonflying.timekiller.executor.core.starter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moonflying.timekiller.executor.annotation.ScheduledTask;
import com.moonflying.timekiller.executor.core.starter.AbstractExecutorStarter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author ffei
 * @Date 2021/11/20 23:26
 * TODO 删除了 DisposableBean，需要自己实现对象销毁后调用stop方法
 */
public class SimpleExecutorStarter extends AbstractExecutorStarter {
    private static final Logger logger = LoggerFactory.getLogger(SimpleExecutorStarter.class);

    private final List<Object> scheduledTasks = new ArrayList<>();

    public void setScheduledTasks(List<Object> tasks) {
        scheduledTasks.addAll(tasks);
    }

    @Override
    public void start() {
        // init job handler from method
        for (Object taskClass : scheduledTasks) {
            Method[] methods = taskClass.getClass().getDeclaredMethods();
            for (Method method : methods) {
                ScheduledTask scheduledTask = method.getAnnotation(ScheduledTask.class);
                if (scheduledTask != null) {
                    super.registerTaskHandler(scheduledTask, taskClass, method);
                }
            }
        }
        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}