package org.canoegame.schedule;

import java.time.Instant;

public class DelayedTasklet extends Tasklet {
    private Actor actor;
    private Instant fireTime;

    DelayedTasklet(Runnable runnable) {
        super(runnable);
    }

    DelayedTasklet(String name, Runnable runnable) {
        super(name, runnable);
    }

    void setActor(Actor actor) {
        this.actor = actor;
    }

    public Actor getActor() {
        return actor;
    }

    void setFireTime(Instant fireTime) {
        this.fireTime = fireTime;
    }

    public Instant getFireTime() {
        return fireTime;
    }

}
