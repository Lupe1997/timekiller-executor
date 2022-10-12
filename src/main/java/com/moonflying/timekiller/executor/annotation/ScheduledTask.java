package com.moonflying.timekiller.executor.annotation;


import java.lang.annotation.*;

/**
 * @Author ffei
 * @Date 2021/11/20 19:17
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ScheduledTask {
    /**
     * The name and id of the scheduled task.
     */
    String name();

    /**
     * Cron expression.
     */
    String cron();

    /**
     * time zone
     * The data from #ZoneId.getAvailableZoneIds()
     */
    String zone();
}
