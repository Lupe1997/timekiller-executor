package com.moonflying.timekiller.executor.core.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author ffei
 * @Date 2021/11/21 22:52
 */
public abstract class AbstractScheduledTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(AbstractScheduledTaskExecutor.class);

    private static final ConcurrentMap<String, AbstractScheduledTaskExecutor> scheduledTaskExecutorRepository = new ConcurrentHashMap<>();

    public static AbstractScheduledTaskExecutor getScheduledTaskHandler(String scheduledTaskName) {
        return scheduledTaskExecutorRepository.get(scheduledTaskName);
    }

    public static AbstractScheduledTaskExecutor registerScheduledTaskExecutor(String scheduledTaskName, AbstractScheduledTaskExecutor scheduledTaskExecutor) {
        AbstractScheduledTaskExecutor executor = scheduledTaskExecutorRepository.put(scheduledTaskName, scheduledTaskExecutor);
        logger.info(">>>>>>>>>>> time-killer register scheduledTaskExecutor success, name:{}, executor:{}", scheduledTaskName, scheduledTaskExecutor);
        return executor;
    }

    public static ConcurrentMap<String, AbstractScheduledTaskExecutor> getAllScheduledTaskRepository() {
        return scheduledTaskExecutorRepository;
    }

    /**
     * execute scheduled task when the executor receives a scheduling request
     *
     * @throws Exception
     */
    public abstract void execute() throws Exception;

    /**
     * execute before scheduled task executes
     *
     * @throws Exception
     */
    public abstract void init() throws Exception;

    /**
     * execute after scheduled task executes
     *
     * @throws Exception
     */
    public abstract void destroy() throws Exception;

}
