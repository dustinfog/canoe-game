package org.canoegame.schedule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class Actor implements Runnable{
    private final static Logger logger = Logger.getLogger(Actor.class.getName());
    public static final int BATCH_SIZE_ASYNC = 256;
    public static final int BATCH_SIZE_DELAYED = 64; // n*lg(n) < 256
    private static final ThreadLocal<Actor> current = new ThreadLocal<>();

    private final TaskletContext context;
    private final String name;
    private final PriorityQueue<ScheduledTasklet> scheduledQueue;
    private final ConcurrentLinkedQueue<Tasklet> executionQueue;
    private final AtomicReference<ActorState> state;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> nextScheduledFeature;
    private Instant nextScheduledTime;

    public Actor(TaskletContext context, String name) {
        this.name = name;
        this.context = context;
        state = new AtomicReference<>(ActorState.WAIT);
        scheduledQueue = new PriorityQueue<>();
        executionQueue = new ConcurrentLinkedQueue<>();
    }

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public static Actor currentActor() {
        return current.get();
    }

    public String getName() {
        return name;
    }

    public void execute(Runnable runnable) {
        executionQueue.add(new Tasklet(runnable));
        switchToReady();
    }
    public void execute(String name, Runnable runnable) {
        executionQueue.add(new Tasklet(name, runnable));
        switchToReady();
    }

    public ScheduledTasklet schedule(Runnable runnable, Instant fireTime) {
        var t = new ScheduledTasklet(runnable, this, fireTime);
        execute(() -> scheduledQueue.add(t));
        return t;
    }

    public ScheduledTasklet schedule(String name, Runnable runnable, Instant fireTime) {
        var t = new ScheduledTasklet(name, runnable, this, fireTime);
        execute(() -> scheduledQueue.add(t));
        return t;
    }

    void cancel(ScheduledTasklet tasklet) {
        if (current.get() == this) {
            scheduledQueue.remove(tasklet);
        } else {
            execute(() -> scheduledQueue.remove(tasklet));
        }
    }

    void reset(ScheduledTasklet tasklet) {
        scheduledQueue.remove(tasklet);
        scheduledQueue.add(tasklet);
    }

    private void switchToReady() {
        if (state.compareAndSet(ActorState.WAIT, ActorState.READY)) {
            executor.execute(this);
        }
    }

    public void run() {
        if (state.getAndSet(ActorState.RUNNING) == ActorState.RUNNING) {
            return;
        }

        logger.info("Actor " + name + " is running");

        current.set(this);
        boolean hasMoreDelayed = false;
        try {
            hasMoreDelayed = doRun();
        } catch (Throwable e) {
            logger.warning("Actor " + name + " got exception: " + e);
        }finally {
            state.set(ActorState.WAIT);
            current.remove();
            if (hasMoreDelayed || !executionQueue.isEmpty()) {
                switchToReady();
            }
        }
    }
    private boolean doRun() {
        for (var total = BATCH_SIZE_ASYNC; total -- > 0 && !executionQueue.isEmpty(); ) {
            context.run(executionQueue.poll());
        }

        var now = Instant.now();
        var total = BATCH_SIZE_DELAYED;
        while (!scheduledQueue.isEmpty()) {
            var tasklet  = scheduledQueue.peek();
            var fireTime = tasklet.getFireTime();
            var until = now.until(fireTime, ChronoUnit.MILLIS);

            if (until > 0) {
                if (nextScheduledTime != null && nextScheduledTime.compareTo(fireTime) != 0) {
                    nextScheduledFeature.cancel(false);
                }

                nextScheduledTime = fireTime;
                nextScheduledFeature = executor.schedule(
                        this,
                        until,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                );
                break;
            }

            if (total <= 0) {
                return true;
            }

            scheduledQueue.remove();
            context.run(tasklet);
            total --;
        }

        return false;
    }
}
