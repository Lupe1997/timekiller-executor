package com.moonflying.timekiller.executor.core.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author ffei
 * @Date 2021/11/23 22:11
 */
public class ScheduledTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskExecutor.class);

    private static final ConcurrentMap<String, ScheduledTaskExecutor> scheduledTaskExecutorRepository = new ConcurrentHashMap<>();

    public static ScheduledTaskExecutor getScheduledTaskHandler(String scheduledTaskName) {
        return scheduledTaskExecutorRepository.get(scheduledTaskName);
    }

    private final Object scheduledTaskBean;
    private final String corn;
    private final Method scheduledTaskMethod;

    public ScheduledTaskExecutor(Object scheduledTaskBean, String corn, Method scheduledTaskMethod) {
        this.scheduledTaskBean = scheduledTaskBean;
        this.corn = corn;
        this.scheduledTaskMethod = scheduledTaskMethod;
    }

    public void execute() throws Exception {
        Class<?>[] paramTypes = scheduledTaskMethod.getParameterTypes();
        if (paramTypes.length > 0) {
            scheduledTaskMethod.invoke(scheduledTaskBean, new Object[paramTypes.length]);       // method-param can not be primitive-types
        } else {
            scheduledTaskMethod.invoke(scheduledTaskBean);
        }
    }

    public static ScheduledTaskExecutor registerScheduledTaskExecutor(String scheduledTaskName, ScheduledTaskExecutor scheduledTaskExecutor) {
        ScheduledTaskExecutor executor = scheduledTaskExecutorRepository.put(scheduledTaskName, scheduledTaskExecutor);
        logger.info(">>>>>>>>>>> time-killer register scheduledTaskExecutor success, name:{}, executor:{}", scheduledTaskName, scheduledTaskExecutor);
        return executor;
    }

    public static ConcurrentMap<String, ScheduledTaskExecutor> getAllScheduledTaskRepository() {
        return scheduledTaskExecutorRepository;
    }

    public String getCorn() {
        return this.corn;
    }
}
