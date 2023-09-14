package org.canoegame.schedule;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ActorTest extends TestCase {
    private final static Logger logger = Logger.getLogger(ActorTest.class.getName());
    private TaskletContext createContext()  {
        TaskletContext context = new TaskletContext();
        var interceptors = new ArrayList<Interceptor>();
        interceptors.add(new Interceptor() {
            @Override
            public boolean beforeRun(Tasklet tasklet) {
                return true;
            }

            @Override
            public void afterRun(Tasklet tasklet) {

            }

            @Override
            public void onException(Tasklet tasklet, Throwable e) {
                logger.info("onException: " + tasklet);
            }

            @Override
            public void afterCompletion(Tasklet tasklet) {
                logger.info("afterCompletion: " + tasklet);
            }
        });
        context.setInterceptors(interceptors);
        return context;
    }

    public void testTestRun() throws InterruptedException {
        var actor = new Actor(new TaskletContext(), "test");
        actor.setExecutor(new ScheduledThreadPoolExecutor(5));
        actor.execute(()->{
            logger.info("hello");
        });
        actor.schedule(()->{
            logger.info("world");
        },
                1,
                TimeUnit.SECONDS);
        Thread.sleep(10000);
    }
}