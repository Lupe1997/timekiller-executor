package timekiller.executor.core.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import timekiller.executor.annotation.ScheduledTask;
import timekiller.executor.core.starter.client.EmbeddedClient;
import timekiller.executor.core.executor.AbstractScheduledTaskExecutor;
import timekiller.executor.core.executor.impl.ScheduledTaskExecutor;
import java.lang.reflect.Method;

/**
 * @Author ffei
 * @Date 2021/11/21 22:49
 */
public abstract class AbstractExecutorStarter {
    private static final Logger logger = LoggerFactory.getLogger(AbstractExecutorStarter.class);

    public void start() throws Exception {

    }

    public void destroy() throws Exception{
        // stop executor server
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected void registerTaskHandler(ScheduledTask scheduledTask, Object bean, Method executeMethod){
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
        AbstractScheduledTaskExecutor.registerScheduledTaskExecutor(name, new ScheduledTaskExecutor(bean, executeMethod, initMethod, destroyMethod));
    }

    // ---------------------- executor-server (rpc customer) ----------------------
    private EmbeddedClient client = null;

    private void initEmbeddedClientServer(String address, int port) throws Exception {
        // generate address
        if (address==null || address.trim().length()==0) {
            throw new RuntimeException("Server address is null!!!");
        }

        // start
        client = new EmbeddedClient();
        client.start(address, port);
    }
}
