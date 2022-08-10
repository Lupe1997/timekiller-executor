package com.moonflying.timekiller.executor.core.starter.impl;

import com.moonflying.timekiller.executor.annotation.ScheduledTaskClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moonflying.timekiller.executor.annotation.ScheduledTask;
import com.moonflying.timekiller.executor.core.starter.AbstractExecutorStarter;

import java.lang.reflect.Method;
import java.util.ServiceLoader;

/**
 * @Author ffei
 * @Date 2021/11/20 23:26
 * Get scheduled tasks by Java SPI
 * TODO 删除了Spring，需要自己实现初始化启动代码（删除了SmartInitializingSingleton->afterSingletonsInstantiated）
 * TODO 删除了 DisposableBean，需要自己实现对象销毁后调用stop方法
 */
public class SPIExecutorStarter extends AbstractExecutorStarter {
    private static final Logger logger = LoggerFactory.getLogger(SPIExecutorStarter.class);

    @Override
    public void start() {
        // get task classes by Java SPI
        ServiceLoader<ScheduledTaskClass> taskClasses = ServiceLoader.load(ScheduledTaskClass.class);
        // init job handler from method
        for (ScheduledTaskClass taskClass : taskClasses) {
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