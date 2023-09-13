package org.canoegame.schedule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

public class ScheduledTasklet extends Tasklet implements Comparable<ScheduledTasklet> {
    private final Actor actor;
    private Instant fireTime;
    private boolean cancelled;

    ScheduledTasklet(Runnable runnable, Actor actor, Instant fireTime) {
        super(runnable);
        this.actor = actor;
        this.fireTime = fireTime;
    }

    ScheduledTasklet(String name, Runnable runnable, Actor actor, Instant fireTime) {
        super(name, runnable);
        this.actor = actor;
        this.fireTime = fireTime;
    }

    public static @Nullable ScheduledTasklet currentScheduledTasklet() {
        var current = currentTasklet();
        if (current instanceof ScheduledTasklet) {
            return (ScheduledTasklet) current;
        }

        return null;
    }

    public Actor getActor() {
        return actor;
    }

    public Instant getFireTime() {
        return fireTime;
    }

    public boolean cancel() {
        if (cancelled) {
            return false;
        }
        cancelled = true;
        actor.cancel(this);
        return true;
    }

    public void reset(Instant fireTime) {
        if (Actor.currentActor() == actor) {
            doReset(fireTime);
        } else {
            actor.execute(() -> {
                doReset(fireTime);
            });
        }
    }

    public void reset(TemporalAmount delay) {
        reset(Instant.now().plus(delay));
    }

    private void doReset(Instant fireTime) {
        this.cancelled = false;
        this.fireTime = fireTime;
        actor.reset(this);
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
        return fireTime.compareTo(o.fireTime);
    }
}
