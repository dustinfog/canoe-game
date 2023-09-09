package org.canoegame.schedule;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Actor implements Runnable{
    private static final ThreadLocal<Actor> current = new ThreadLocal<>();
    private final TaskletContext context;
    private final String name;
    private final ReadWriteLock lock;
    private final PriorityQueue<DelayedTasklet> delayedQueue;
    private final AsyncQueue asyncQueue;
    private ActorState state;
    private ScheduledThreadPoolExecutor executor;

    public Actor(TaskletContext context, String name) {
        this.name = name;
        this.context = context;

        lock = new ReentrantReadWriteLock();
        delayedQueue = new PriorityQueue<>();
        asyncQueue = new AsyncQueue();
    }

    public static Actor currentActor() {
        return current.get();
    }

    public String getName() {
        return name;
    }

    public void execute(Runnable runnable) {
        var locker = lock.writeLock();
        locker.lock();
        try {
            asyncQueue.add(new Tasklet(runnable));
            if (state == ActorState.WAIT) {
                state = ActorState.READY;
                executor.execute(this);
            }
        } finally {
            locker.unlock();
        }
    }

    public void run() {
        current.set(this);
        try {
            doRun();
        } finally {
            current.remove();
        }
    }

    private void doRun() {
        lock.writeLock().lock();
        ArrayList<Tasklet> runningChunk;
        try {
            runningChunk = asyncQueue.popChunk();
        } finally {
            lock.writeLock().unlock();
        }

        if (runningChunk != null) {
            for (var tasklet : runningChunk) {
                context.run(tasklet);
            }
        }

        var now = Instant.now();
        while (!delayedQueue.isEmpty()) {
            var tasklet  = delayedQueue.peek();
            if (tasklet.getFireTime().isAfter(now)) {
                break;
            }

            delayedQueue.remove();
            context.run(tasklet);
        }
    }

    private static class AsyncQueue extends LinkedList<ArrayList<Tasklet>> {
        public final int chunkSize = 256;

        public void add(Tasklet tasklet) {
            var last = getLast();
            if (last == null || last.size() >= chunkSize) {
                last = new ArrayList<>(chunkSize);
                add(last);
            }

            last.add(tasklet);
        }

        public ArrayList<Tasklet> popChunk() {
            if (isEmpty() || getFirst().isEmpty()) {
                return null;
            }

            return removeFirst();
        }
    }
}
