package org.canoegame.schedule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class ScheduledTasklet extends Tasklet implements Comparable<ScheduledTasklet> {
    private long triggerTime;
    private boolean cancelled;

    ScheduledTasklet(String name, Runnable runnable, Actor actor, long delay, TimeUnit unit) {
        super(name, runnable, actor);
        this.assignTriggerTime(delay, unit);
    }

    public static @Nullable ScheduledTasklet currentScheduledTasklet() {
        var current = currentTasklet();
        if (current instanceof ScheduledTasklet) {
            return (ScheduledTasklet) current;
        }

        return null;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    public void cancel() {
        var actor = getActor();
        if (Actor.currentActor() == actor) {
            doCancel();
        } else {
            actor.execute(this::doCancel);
        }
    }

    private void doCancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        getActor().cancel(this);
    }

    public void reset(long delay, TimeUnit unit) {
        var actor = getActor();
        if (Actor.currentActor() == actor) {
            doReset(delay, unit);
        } else {
            actor.execute(() -> {
                doReset(delay, unit);
            });
        }
    }

    private void doReset(long delay, TimeUnit unit) {
        this.cancelled = false;
        this.assignTriggerTime(delay, unit);
        getActor().reset(this);
    }
    @Override
    void run() {
        if (cancelled) {
            return;
        }

        super.run();
    }

    @Override
    public int compareTo(@NotNull ScheduledTasklet o) {
        return Long.compare(triggerTime, o.triggerTime);
    }

    private void assignTriggerTime(long delay, TimeUnit unit) {
        var current = currentScheduledTasklet();
        if (current != null) {
            this.triggerTime = current.triggerTime + unit.toNanos(delay);
        } else {
            this.triggerTime = System.nanoTime() + unit.toNanos(delay);
        }
    }
}
