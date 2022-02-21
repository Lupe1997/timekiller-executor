package com.moonflying.timekiller.executor.core.starter.impl;

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

/**
 * @Author ffei
 * @Date 2021/11/20 23:26
 */
public class ExecutorStarter extends AbstractExecutorStarter implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorStarter.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(applicationContext);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
    }

    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
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