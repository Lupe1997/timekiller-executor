package timekiller.executor.annotation;


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
    String value();

    /**
     * Cron expression.
     */
    String cron();

    /**
     * init task, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy task, invoked when JobThread destroy
     */
    String destroy() default "";
}
