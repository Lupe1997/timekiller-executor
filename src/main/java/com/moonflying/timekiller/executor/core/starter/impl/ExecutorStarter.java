package com.moonflying.timekiller.executor.core.starter.impl;

import com.moonflying.timekiller.executor.annotation.ScheduledTaskClass;
import com.moonflying.timekiller.executor.core.executor.AbstractScheduledTaskExecutor;
import com.moonflying.timekiller.executor.core.executor.impl.ScheduledTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import com.moonflying.timekiller.executor.annotation.ScheduledTask;
import com.moonflying.timekiller.executor.core.starter.AbstractExecutorStarter;

import java.lang.reflect.Method;
import java.util.Map;
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
        ServiceLoader<ScheduledTaskClass> taskClasses = ServiceLoader.load(ScheduledTaskClass.class);


        // init job handler from method
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);

            Map<Method, ScheduledTask> annotatedMethods;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(
                        bean.getClass(),
                        new MethodIntrospector.MetadataLookup<ScheduledTask>() {
                            @Override
                            public ScheduledTask inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, ScheduledTask.class);
                            }
                        }
                );
            } catch (Throwable ex) {
                logger.error("Occur an exception when resolve ScheduledTask annotatedMethods for bean[" + beanName + "].", ex);
                throw ex;
            }

            if (annotatedMethods.isEmpty()) continue;

            for (Map.Entry<Method, ScheduledTask> methodScheduledTaskEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodScheduledTaskEntry.getKey();
                if (executeMethod.getParameterCount() > 0)
                    throw new RuntimeException("The method: " + executeMethod.getName() + " of scheduled task shouldn't have parameters");
                ScheduledTask scheduledTask = methodScheduledTaskEntry.getValue();

                // register
                registerTaskHandler(scheduledTask, bean, executeMethod);
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