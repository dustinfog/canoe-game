package org.canoegame.schedule;

import java.util.HashMap;
import java.util.Map;

public class Tasklet {
    private static final ThreadLocal<Tasklet> current = new ThreadLocal<>();

    private final Runnable runnable;
    private final Map<TaskletLocal<?>, Object> storage;
    private final String name;
    private final Actor actor;

    Tasklet(String name, Runnable runnable, Actor actor) {
        if (name == null) {
            name = runnable.getClass().getName();
        }
        this.name = name;
        this.runnable = runnable;
        this.actor = actor;
        storage = new HashMap<>();
    }

    public static Tasklet currentTasklet() {
        return current.get();
    }

    public String getName() {
        return name;
    }

    <T> void localSet(TaskletLocal<T> local, T value) {
        storage.put(local, value);
    }

    <T> T localGet(TaskletLocal<T> local) {
        return (T)storage.get(local);
    }

    void localRemove(TaskletLocal<?> local) {
        storage.remove(local);
    }

    public Actor getActor() {
        return actor;
    }

    @Override
    public String toString() {
        return name + "@" + getId();
    }

    public int getId() {
        return hashCode();
    }

    void run() {
        current.set(this);

        try {
            runnable.run();
        } finally {
            current.remove();
        }
    }
}
