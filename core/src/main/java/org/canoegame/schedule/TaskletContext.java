package org.canoegame.schedule;

import java.util.List;

public class TaskletContext {
    private List<Interceptor> interceptors;

    public void setInterceptors(List<Interceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public void run(Runnable runnable) {
        var tasklet = new Tasklet(null, runnable, Actor.currentActor());
        run(tasklet);
    }

    void run(Tasklet tasklet) {
        if (Tasklet.currentTasklet() != null) {
            throw new IllegalStateException("Already in a tasklet");
        }

        if (interceptors == null) {
            tasklet.run();
            return;
        }

        try {
            var itr = interceptors.listIterator();
            while (itr.hasNext()) {
                if (!itr.next().beforeRun(tasklet)) {
                    return;
                }
            }
            tasklet.run();
            while (itr.hasPrevious()) {
                itr.previous().afterRun(tasklet);
            }
        } catch (Throwable e) {
            for (Interceptor interceptor : interceptors) {
                interceptor.onException(tasklet, e);
            }
        } finally {
            for (Interceptor interceptor : interceptors) {
                interceptor.afterCompletion(tasklet);
            }
        }
    }
}
