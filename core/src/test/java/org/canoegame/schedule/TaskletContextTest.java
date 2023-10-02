package org.canoegame.schedule;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.logging.Logger;

public class TaskletContextTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
    }

    public void testTestRun() {
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

            }

            @Override
            public void afterCompletion(Tasklet tasklet) {
                Logger.getLogger(getClass().getName()).info("afterCompletion: " + tasklet);
            }
        });
        context.setInterceptors(interceptors);
        context.run(() -> {
            var task = Tasklet.currentTasklet();
            task.set("key", "value");

            assertEquals("value", task.get("key"));
        });
    }
}