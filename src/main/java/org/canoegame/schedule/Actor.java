package org.canoegame.schedule;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private long nextScheduledTime;

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

    public Tasklet execute(Runnable runnable) {
        return execute(null, runnable);
    }
    public Tasklet execute(String name, Runnable runnable) {
        var tasklet = new Tasklet(name, runnable, this);
        executionQueue.add(tasklet);
        switchToReady();
        return tasklet;
    }

    public ScheduledTasklet schedule(Runnable runnable, long delay, TimeUnit unit) {
        return schedule(null, runnable, delay, unit);
    }

    public ScheduledTasklet schedule(String name, Runnable runnable, long delay, TimeUnit unit) {
        var t = new ScheduledTasklet(name, runnable, this, delay, unit);
        execute(() -> scheduledQueue.add(t));
        return t;
    }

    void cancel(ScheduledTasklet tasklet) {
        scheduledQueue.remove(tasklet);
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

            logger.info("Actor " + name + " hasMoreDelayed: " + hasMoreDelayed + ", executionQueue: " + executionQueue.size() + ", scheduledQueue: " + scheduledQueue.size());
            if (hasMoreDelayed || !executionQueue.isEmpty()) {
                switchToReady();
            }
        }
    }
    private boolean doRun() {
        var total = BATCH_SIZE_ASYNC;
        while (!executionQueue.isEmpty()) {
            // 如果系统退出，则执行完所有任务
            if (!executor.isShutdown() &&  total -- <= 0) {
                break;
            }

            context.run(executionQueue.poll());
        }

        total = BATCH_SIZE_DELAYED;

        while (!scheduledQueue.isEmpty()) {
            var tasklet  = scheduledQueue.peek();
            if (shutDownOrInFuture(tasklet)) {
                break;
            }

            if (total -- <= 0) {
                return true;
            }

            scheduledQueue.remove();
            context.run(tasklet);
        }

        return false;
    }

    private boolean shutDownOrInFuture(ScheduledTasklet tasklet) {
        var now = System.nanoTime();
        var fireTime = tasklet.getTriggerTime();
        if (fireTime <= now) {
            return false;
        }

        if (executor.isShutdown()) {
            if (nextScheduledFeature != null) {
                nextScheduledFeature.cancel(false);
            }

            return true;
        }

        if (nextScheduledFeature != null) {
            if (nextScheduledTime == fireTime) {
                return true;
            }

            nextScheduledFeature.cancel(false);
        }

        nextScheduledTime = fireTime;
        nextScheduledFeature = executor.schedule(
                this,
                fireTime - now,
                TimeUnit.NANOSECONDS);

        return true;
    }
}
