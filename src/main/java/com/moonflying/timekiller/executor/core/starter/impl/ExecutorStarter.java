package com.moonflying.timekiller.executor.core.starter.impl;

import com.moonflying.timekiller.executor.annotation.ScheduledTaskClass;
import com.moonflying.timekiller.executor.core.executor.AbstractScheduledTaskExecutor;
import com.moonflying.timekiller.executor.core.executor.impl.ScheduledTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moonflying.timekiller.executor.annotation.ScheduledTask;
import com.moonflying.timekiller.executor.core.starter.AbstractExecutorStarter;

import java.lang.reflect.Method;
import java.util.ServiceLoader;

/**
 * @Author ffei
 * @Date 2021/11/20 23:26
 *
 * TODO 删除了Spring，需要自己实现初始化启动代码（删除了SmartInitializingSingleton->afterSingletonsInstantiated）
 * TODO 删除了 DisposableBean，需要自己实现对象销毁后调用stop方法
 */
public class ExecutorStarter extends AbstractExecutorStarter {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorStarter.class);

    public void afterSingletonsInstantiated() {
        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository();

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initJobHandlerMethodRepository() {
        // get task classes by Java SPI
        ServiceLoader<ScheduledTaskClass> taskClasses = ServiceLoader.load(ScheduledTaskClass.class);
        // init job handler from method
        for (ScheduledTaskClass taskClass : taskClasses) {
            Method[] methods = taskClass.getClass().getDeclaredMethods();
            for (Method method : methods) {
                ScheduledTask scheduledTask = method.getAnnotation(ScheduledTask.class);
                if (scheduledTask != null) {
                    registerTaskHandler(scheduledTask, taskClass, method);
                }
            }
        }
    }

    private void registerTaskHandler(ScheduledTask scheduledTask, Object bean, Method executeMethod){
        if (scheduledTask == null) {
            return;
        }

        String name = scheduledTask.value();
        // make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (name.trim().length() == 0)
            throw new RuntimeException("scheduled task name invalid, for[" + clazz + "#" + methodName + "] .");

        if (AbstractScheduledTaskExecutor.getScheduledTaskHandler(name) != null)
            throw new RuntimeException("scheduled task [" + name + "] naming conflicts.");

        executeMethod.setAccessible(true);

        // init and destroy
        Method initMethod = null;
        Method destroyMethod = null;

        if (scheduledTask.init().trim().length() > 0) {
            try {
                initMethod = clazz.getDeclaredMethod(scheduledTask.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("scheduled task initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        if (scheduledTask.destroy().trim().length() > 0) {
            try {
                destroyMethod = clazz.getDeclaredMethod(scheduledTask.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("scheduled task method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        // registry scheduledTaskHandler
        AbstractScheduledTaskExecutor.registerScheduledTaskExecutor(name, new ScheduledTaskExecutor(bean, scheduledTask.cron(), executeMethod, initMethod, destroyMethod));
    }
}