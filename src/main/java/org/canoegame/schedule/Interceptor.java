package org.canoegame.schedule;

public interface Interceptor {
    boolean beforeRun(Tasklet tasklet);
    void afterRun(Tasklet tasklet);
    void onException(Tasklet tasklet, Throwable throwable);
    void afterCompletion(Tasklet tasklet);
}
