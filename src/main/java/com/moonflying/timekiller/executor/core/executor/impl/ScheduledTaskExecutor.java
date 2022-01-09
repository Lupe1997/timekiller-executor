package com.moonflying.timekiller.executor.core.executor.impl;

import com.moonflying.timekiller.executor.core.executor.AbstractScheduledTaskExecutor;
import java.lang.reflect.Method;

/**
 * @Author ffei
 * @Date 2021/11/23 22:11
 */
public class ScheduledTaskExecutor extends AbstractScheduledTaskExecutor {
    private final Object scheduledTaskBean;
    private final String corn;
    private final Method scheduledTaskMethod;
    private final Method initMethod;
    private final Method destroyMethod;

    public ScheduledTaskExecutor(Object scheduledTaskBean, String corn, Method scheduledTaskMethod, Method initMethod, Method destroyMethod) {
        this.scheduledTaskBean = scheduledTaskBean;
        this.corn = corn;
        this.scheduledTaskMethod = scheduledTaskMethod;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public void execute() throws Exception {
        Class<?>[] paramTypes = scheduledTaskMethod.getParameterTypes();
        if (paramTypes.length > 0) {
            scheduledTaskMethod.invoke(scheduledTaskBean, new Object[paramTypes.length]);       // method-param can not be primitive-types
        } else {
            scheduledTaskMethod.invoke(scheduledTaskBean);
        }
    }

    @Override
    public void init() throws Exception {
        if (initMethod != null) initMethod.invoke(scheduledTaskBean);
    }

    @Override
    public void destroy() throws Exception {
        if (destroyMethod != null) destroyMethod.invoke(scheduledTaskBean);
    }

    public String getCorn() {
        return this.corn;
    }
}
